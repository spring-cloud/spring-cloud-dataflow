/*
 * Copyright 2017-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.composedtaskrunner;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.FlowJobBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.core.dsl.FlowNode;
import org.springframework.cloud.dataflow.core.dsl.LabelledTaskNode;
import org.springframework.cloud.dataflow.core.dsl.SplitNode;
import org.springframework.cloud.dataflow.core.dsl.TaskAppNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.core.dsl.TransitionNode;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * Genererates a Composed Task Job Flow.
 *
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public class ComposedRunnerJobFactory implements FactoryBean<Job> {

	private static final String WILD_CARD = "*";

	private static String CTR_KEY = "ctr.id";

	@Autowired
	private ApplicationContext context;

	@Autowired
	private TaskExecutor taskExecutor;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private TaskNameResolver taskNameResolver;

	private final ComposedTaskProperties composedTaskProperties;

	private FlowBuilder<Flow> flowBuilder;

	private Map<String, Integer> taskBeanSuffixes = new HashMap<>();

	private Deque<Flow> jobDeque = new LinkedList<>();

	private Deque<LabelledTaskNode> visitorDeque;

	private Deque<Flow> executionDeque = new LinkedList<>();

	private String dsl;

	private int nestedSplits;

	public ComposedRunnerJobFactory(ComposedTaskProperties properties) {
		this.composedTaskProperties = properties;
		Assert.notNull(properties.getGraph(), "The DSL must not be null");
		this.dsl = properties.getGraph();
		this.flowBuilder = new FlowBuilder<>(UUID.randomUUID().toString());
	}

	@Override
	public Job getObject() throws Exception {
		ComposedRunnerVisitor composedRunnerVisitor = new ComposedRunnerVisitor();

		TaskParser taskParser = new TaskParser("composed-task-runner",
				this.dsl,false,true);
		taskParser.parse().accept(composedRunnerVisitor);

		this.visitorDeque = composedRunnerVisitor.getFlow();
		JobBuilder jobBuilder = new JobBuilder(this.taskNameResolver.getTaskName(), jobRepository);
		FlowJobBuilder builder = jobBuilder
				.start(this.flowBuilder
						.start(createFlow())
						.end())
				.end();
		if (this.composedTaskProperties.isUuidInstanceEnabled()) {
			builder.incrementer(new UuidIncrementer());
		}
		return builder.build();
	}

	@Override
	public Class<?> getObjectType() {
		return Job.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private Flow createFlow() {

		while (!this.visitorDeque.isEmpty()) {

			if (this.visitorDeque.peek() instanceof TaskAppNode) {
				TaskAppNode taskAppNode = (TaskAppNode) this.visitorDeque.pop();

				if (taskAppNode.hasTransitions()) {
					handleTransition(this.executionDeque, taskAppNode);
				}
				else {
					this.executionDeque.push(getTaskAppFlow(taskAppNode));
				}
			}
			//When end marker of a split is found, process the split
			else if (this.visitorDeque.peek() instanceof SplitNode) {
				Deque<LabelledTaskNode> splitNodeDeque = new LinkedList<>();
				SplitNode splitNode = (SplitNode) this.visitorDeque.pop();
				splitNodeDeque.push(splitNode);
				while (!this.visitorDeque.isEmpty() && !this.visitorDeque.peek().equals(splitNode)) {
					splitNodeDeque.push(this.visitorDeque.pop());
				}
				this.nestedSplits = 0;
				splitNodeDeque.push(this.visitorDeque.pop());
				handleSplit(splitNodeDeque, splitNode);

				int threadCorePoolSize = this.composedTaskProperties.getSplitThreadCorePoolSize();
				Assert.isTrue(threadCorePoolSize >= this.nestedSplits,
						"Split thread core pool size " + threadCorePoolSize + " should be equal or greater "
								+ "than the depth of split flows " + this.nestedSplits + "."
								+ " Try setting the composed task property `splitThreadCorePoolSize`");
			}
			//When start marker of a DSL flow is found, process it.
			else if (this.visitorDeque.peek() instanceof FlowNode) {
				handleFlow(this.executionDeque);
			}
		}

		return this.jobDeque.pop();
	}

	private void handleFlow(Deque<Flow> executionDeque) {
		if(!executionDeque.isEmpty()) {
			this.flowBuilder.start(executionDeque.pop());
		}

		while (!executionDeque.isEmpty()) {
				this.flowBuilder.next(executionDeque.pop());
		}

		this.visitorDeque.pop();
		this.jobDeque.push(this.flowBuilder.end());
	}

	private void handleSplit(Deque<LabelledTaskNode> visitorDeque, SplitNode splitNode) {
		this.executionDeque.push(processSplitNode(visitorDeque, splitNode));
	}

	private Flow processSplitNode(Deque<LabelledTaskNode> visitorDeque, SplitNode splitNode) {
		this.nestedSplits++;
		Deque<Flow> flows = new LinkedList<>();
		//For each node in the split process it as a DSL flow.
		for (LabelledTaskNode taskNode : splitNode.getSeries()) {
			Deque<Flow> resultFlowDeque = new LinkedList<>();
			flows.addAll(processSplitFlow(taskNode, resultFlowDeque));
		}
		removeProcessedNodes(visitorDeque, splitNode);
		Flow nestedSplitFlow = new FlowBuilder.SplitBuilder<>(
				new FlowBuilder<Flow>("Split" + UUID.randomUUID().toString()),
				taskExecutor)
				.add(flows.toArray(new Flow[flows.size()]))
				.build();
		FlowBuilder<Flow> taskAppFlowBuilder =
				new FlowBuilder<>("Flow" + UUID.randomUUID().toString());

		return taskAppFlowBuilder.start(nestedSplitFlow).end();
	}

	private void removeProcessedNodes(Deque<LabelledTaskNode> visitorDeque, SplitNode splitNode) {
		//remove the nodes of the split since it has already been processed
		while (visitorDeque.peek() != null && !(visitorDeque.peek().equals(splitNode))) {
			visitorDeque.pop();
		}
		// pop the SplitNode that marks the beginning of the split from the deque
		if (visitorDeque.peek() != null) {
			visitorDeque.pop();
		}
	}

	/**
	 * Processes each node in split as a  DSL Flow.
	 * @param node represents a single node in the split.
	 * @return Deque of Job Flows that was obtained from the Node.
	 */
	private Deque<Flow> processSplitFlow(LabelledTaskNode node, Deque<Flow> resultFlowDeque) {
		TaskParser taskParser = new TaskParser("split_flow" + UUID.randomUUID().toString(), node.stringify(),
				false, true);
		ComposedRunnerVisitor splitElementVisitor = new ComposedRunnerVisitor();
		taskParser.parse().accept(splitElementVisitor);

		Deque<LabelledTaskNode> splitElementDeque = splitElementVisitor.getFlow();
		Deque<Flow> elementFlowDeque = new LinkedList<>();

		while (!splitElementDeque.isEmpty()) {

			if (splitElementDeque.peek() instanceof TaskAppNode) {

				TaskAppNode taskAppNode = (TaskAppNode) splitElementDeque.pop();

				if (taskAppNode.hasTransitions()) {
					handleTransition(elementFlowDeque, taskAppNode);
				}
				else {
					elementFlowDeque.push(
							getTaskAppFlow(taskAppNode));
				}
			}
			else if (splitElementDeque.peek() instanceof FlowNode) {
				resultFlowDeque.push(handleFlowForSegment(elementFlowDeque));
				splitElementDeque.pop();
			}
			else if (splitElementDeque.peek() instanceof SplitNode) {
				Deque<LabelledTaskNode> splitNodeDeque = new LinkedList<>();
				SplitNode splitNode = (SplitNode) splitElementDeque.pop();
				splitNodeDeque.push(splitNode);
				while (!splitElementDeque.isEmpty() && !splitElementDeque.peek().equals(splitNode)) {
					splitNodeDeque.push(splitElementDeque.pop());
				}
				splitNodeDeque.push(splitElementDeque.pop());
				elementFlowDeque.push(processSplitNode(splitNodeDeque, splitNode));
			}
		}
		return resultFlowDeque;
	}

	private Flow handleFlowForSegment(Deque<Flow> resultFlowDeque) {
		FlowBuilder<Flow> localTaskAppFlowBuilder =
				new FlowBuilder<>("Flow" + UUID.randomUUID().toString());

		if(!resultFlowDeque.isEmpty()) {
			localTaskAppFlowBuilder.start(resultFlowDeque.pop());

		}

		while (!resultFlowDeque.isEmpty()) {
			localTaskAppFlowBuilder.next(resultFlowDeque.pop());
		}

		return localTaskAppFlowBuilder.end();
	}

	private void handleTransition(Deque<Flow> resultFlowDeque,
			TaskAppNode taskAppNode) {
		String beanName = getBeanName(taskAppNode);
		Step currentStep = this.context.getBean(beanName, Step.class);
		FlowBuilder<Flow> builder = new FlowBuilder<Flow>(beanName)
				.from(currentStep);

		boolean wildCardPresent = false;

		for (TransitionNode transitionNode : taskAppNode.getTransitions()) {
			String transitionBeanName = getBeanName(transitionNode);

			wildCardPresent = transitionNode.getStatusToCheck().equals(WILD_CARD);

			Step transitionStep = this.context.getBean(transitionBeanName,
					Step.class);
			builder.on(transitionNode.getStatusToCheck()).to(transitionStep)
					.from(currentStep);
		}

		if (wildCardPresent && !resultFlowDeque.isEmpty()) {
			throw new IllegalStateException(
					"Invalid flow following '*' specifier.");
		}
		else {
			//if there are nodes are in the execution Deque.  Make sure that
			//they are processed as a target of the wildcard instead of the
			//whole transition.
			if (!resultFlowDeque.isEmpty()) {
				builder.on(WILD_CARD).to(handleFlowForSegment(resultFlowDeque)).from(currentStep);
			}
		}

		resultFlowDeque.push(builder.end());
	}

	private String getBeanName(TransitionNode transition) {
		if (transition.getTargetLabel() != null) {
			return transition.getTargetLabel();
		}

		return getBeanName(transition.getTargetApp());
	}


	private String getBeanName(TaskAppNode taskApp) {
		if (taskApp.getLabel() != null) {
			return taskApp.getLabel().stringValue();
		}

		String taskName = taskApp.getName();

		if (taskName.contains("->")) {
			taskName = taskName.substring(taskName.indexOf("->") + 2);
		}

		return getBeanName(taskName);
	}

	private String getBeanName(String taskName) {
		int taskSuffix = 0;

		if (this.taskBeanSuffixes.containsKey(taskName)) {
			taskSuffix = this.taskBeanSuffixes.get(taskName);
		}

		String result = String.format("%s_%s", taskName, taskSuffix++);
		this.taskBeanSuffixes.put(taskName, taskSuffix);

		return result;
	}

	private Flow getTaskAppFlow(TaskAppNode taskApp) {
		String beanName = getBeanName(taskApp);
		Step currentStep = this.context.getBean(beanName, Step.class);

		return new FlowBuilder<Flow>(beanName).from(currentStep).end();
	}

	public static class UuidIncrementer implements JobParametersIncrementer {

		@Override
		public JobParameters getNext(JobParameters parameters) {
			JobParameters params = (parameters == null) ? new JobParameters() : parameters;
			return new JobParametersBuilder(params).addString(CTR_KEY, UUID.randomUUID().toString()).toJobParameters();
		}
	}
}
