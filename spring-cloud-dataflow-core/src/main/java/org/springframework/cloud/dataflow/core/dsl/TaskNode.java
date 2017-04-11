/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.core.dsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.springframework.cloud.dataflow.core.dsl.graph.Graph;
import org.springframework.util.Assert;

/**
 * The root AST node for any entity parsed from task DSL.
 *
 * @author Andy Clement
 * @author Thomas Risberg
 */
public class TaskNode extends AstNode {

	/**
	 * The name of the task.
	 */
	private String name;

	/**
	 * The DSL text that was parsed to create this TaskNode.
	 */
	private String taskDSL;

	/**
	 * The sequence of LabelledNodes parsed from the dsl.
	 */
	private List<LabelledTaskNode> sequences;

	/**
	 * All the apps mentioned in the task dsl.
	 */
	private List<TaskApp> taskApps;

	TaskNode(String name, String taskDSL, List<LabelledTaskNode> sequences, boolean inAppMode) {
		super((sequences.size() == 0) ? 0 : sequences.get(0).getStartPos(),
				(sequences.size() == 0) ? 0 : sequences.get(sequences.size() - 1).getEndPos());
		this.name = name;
		this.taskDSL = taskDSL;
		this.sequences = sequences;
		// TODO use inAppMode to police what can be called on this node?
	}

	@Override
	public String stringify(boolean includePositionInfo) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < sequences.size(); i++) {
			if (i > 0) {
				s.append("\n");
			}
			s.append(sequences.get(i).stringify(includePositionInfo));
		}
		return s.toString();
	}

	/**
	 * @return all the apps referenced in the task.
	 */
	public List<TaskApp> getTaskApps() {
		if (taskApps == null) {
			TaskAppsCollector collector = new TaskAppsCollector();
			accept(collector);
			taskApps = collector.getTaskApps();
		}
		return taskApps;
	}

	/**
	 * Walk the AST for the parsed task, calling the visitor for each element of interest.
	 * See the {@link TaskVisitor} for all the events that can be received, since that is an
	 * abstract class it can be extended and subclasses only need override methods to receive the
	 * events of interest.
	 *
	 * @param visitor a visitor to be called as the AST is walked
	 */
	public void accept(TaskVisitor visitor) {
		Assert.notNull(visitor, "visitor expected to be non-null");
		visitor.startVisit(this.name, this.taskDSL);
		int sequenceNumber = 0;
		for (LabelledTaskNode ctn : sequences) {
			if (visitor.preVisitSequence(ctn, sequenceNumber)) {
				ctn.accept(visitor);
				visitor.postVisitSequence(ctn, sequenceNumber);
			}
			sequenceNumber++;
		}
		visitor.endVisit();
	}

	/**
	 * @return this AST converted to a Graph form for display by Flo
	 */
	public Graph toGraph() {
		GraphGeneratorVisitor ggv = new GraphGeneratorVisitor();
		this.accept(ggv);
		return ggv.getGraph();
	}

	/**
	 * @return a list of problems found during validation, empty if no problems
	 */
	public List<TaskValidationProblem> validate() {
		TaskValidatorVisitor validator = new TaskValidatorVisitor();
		this.accept(validator);
		return validator.getProblems();
	}

	/**
	 * Simple visitor that discovers all the tasks in use in the task definition.
	 */
	class TaskAppsCollector extends TaskVisitor {

		private String taskName;

		private List<TaskApp> taskApps = new ArrayList<>();

		@Override
		public void startVisit(String taskName, String taskDSL) {
			this.taskName = taskName;
		}

		@Override
		public void visit(TaskAppNode taskApp) {
			taskApps.add(new TaskApp(taskName, taskApp));
		}

		@Override
		public void visit(TransitionNode transition) {
			if (transition.isTargetApp()) {
				taskApps.add(new TaskApp(taskName, transition.getTargetApp()));
			}
		}

		public List<TaskApp> getTaskApps() {
			return taskApps;
		}

	}

	public String getName() {
		return this.name;
	}

	public String getTaskText() {
		return this.taskDSL;
	}

	/**
	 * Shortcut to return the first node in the first sequence. Many tasks
	 * will have just one sequence so do not force the consumer to dig through that
	 * sequence.
	 *
	 * @return the first node in the first sequence.
	 */
	public LabelledTaskNode getStart() {
		if (sequences.size() == 0) {
			return null;
		}
		else {
			return sequences.get(0);
		}
	}

	public List<LabelledTaskNode> getSequences() {
		return Collections.unmodifiableList(sequences);
	}

	/**
	 * Find the sequence with the specified label and return it.
	 *
	 * @param label the label to search for
	 * @return the sequence with that label or null if there isn't one
	 */
	public LabelledTaskNode getSequenceWithLabel(String label) {
		Assert.hasText(label, "label is required");
		for (LabelledTaskNode ctn : sequences) {
			if (ctn.hasLabel() && ctn.getLabelString().equals(label)) {
				return ctn;
			}
		}
		return null;
	}

	/**
	 * @return the DSL representation of this task Ast
	 */
	public String toDSL() {
		return stringify(false);
	}

	public String toExecutableDSL() {
		ExecutableDSLVisitor v = new ExecutableDSLVisitor();
		accept(v);
		return v.getDSL();
	}

	static class ExecutableDSLVisitor extends TaskVisitor {

		private final String EXECUTABLE_DSL_JOIN_CHAR = "-";
		private final static int START_OF_FLOW = 0;
		private final static int START_OF_SPLIT = 1;
		private final static int IN_FLOW = 2;
		private final static int IN_SPLIT = 3;

		private StringBuilder dsl = new StringBuilder();

		private String taskName;

		private Stack<Integer> state = new Stack<>();

		@Override
		public void startVisit(String taskName, String taskDSL) {
			this.taskName = taskName;
		}

		@Override
		public boolean preVisit(FlowNode flow) {
			if (!state.isEmpty() && state.peek() == IN_SPLIT) {
				dsl.append(" || ");
			}
			state.push(START_OF_FLOW);
			return true;
		}

		@Override
		public void postVisit(FlowNode flow) {
			state.pop();
			// If we were at the start of a split, we are now further along the split
			if (!state.isEmpty() && state.peek() == START_OF_SPLIT) {
				state.pop();
				state.push(IN_SPLIT);
			}
		}

		@Override
		public boolean preVisit(SplitNode split) {
			// Hitting a split at the start of a flow should also flip it from START to IN
			if (state.peek() == START_OF_FLOW) {
				state.pop();
				state.push(IN_FLOW);
			}
			else if (state.peek() == IN_FLOW) {
				dsl.append(" && ");
			}
			state.push(START_OF_SPLIT);
			dsl.append("<");
			return true;
		}

		@Override
		public void postVisit(SplitNode split) {
			dsl.append(">");
			state.pop();
		}

		@Override
		public void visit(TaskAppNode taskApp) {
			int currentState = state.peek();
			if (currentState == START_OF_FLOW) {
				state.pop();
				state.push(IN_FLOW);
			}
			else if (currentState == IN_FLOW) {
				dsl.append(" && ");
			}
			else {
				throw new IllegalStateException("" + state.peek());
			}
			dsl.append(toExecutableDSLTaskName(taskApp));

			List<TransitionNode> transitions = taskApp.getTransitions();
			for (TransitionNode transition : transitions) {
				dsl.append(" ");
				dsl.append(transition.getStatusToCheckInDSLForm());
				dsl.append("->");
				if (transition.isTargetApp()) {
					dsl.append(toExecutableDSLTaskName(transition.getTargetApp()));
				}
				else {
					dsl.append(":").append(transition.getTargetLabel());
				}
			}
		}

		private String toExecutableDSLTaskName(TaskAppNode taskApp) {
			StringBuilder taskDefName = new StringBuilder();
			taskDefName.append(taskName).append(EXECUTABLE_DSL_JOIN_CHAR);
			if (taskApp.hasLabel()) {
				taskDefName.append(taskApp.getLabelString());
			}
			else {
				taskDefName.append(taskApp.getName());
			}
			return taskDefName.toString();
		}

		public String getDSL() {
			return dsl.toString();
		}

	}

	/**
	 * @return true if the dsl contained more than just a single task app/reference or a single
	 * task has transitions
	 */
	public boolean isComposed() {
		// Is there just one task
		boolean isOneTask = (sequences.size() == 1 && sequences.get(0).isFlow() &&
				((FlowNode) sequences.get(0)).getSeriesLength() == 1 &&
				((FlowNode) sequences.get(0)).getSeriesElement(0).isTaskApp());
		if (!isOneTask) {
			return true;
		}
		// Does the one task have transitions?
		TaskAppNode singleNode = (TaskAppNode) (((FlowNode) sequences.get(0)).getSeriesElement(0));
		return singleNode.hasTransitions();
	}

	/**
	 * @return the single task contained in the dsl (for non composed task definitions), or
	 * null if it is a composed task
	 */
	public TaskAppNode getTaskApp() {
		return (isComposed() ? null : (TaskAppNode) (((FlowNode) sequences.get(0)).getSeriesElement(0)));
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("TaskNode for ").append(taskDSL.replaceAll("\n", ";"));
		s.append("\n").append(sequences);
		return s.toString();
	}

}
