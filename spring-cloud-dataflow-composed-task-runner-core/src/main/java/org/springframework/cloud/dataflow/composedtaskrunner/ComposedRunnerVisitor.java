/*
 * Copyright 2017-2020 the original author or authors.
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
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.core.dsl.FlowNode;
import org.springframework.cloud.dataflow.core.dsl.LabelledTaskNode;
import org.springframework.cloud.dataflow.core.dsl.SplitNode;
import org.springframework.cloud.dataflow.core.dsl.TaskAppNode;
import org.springframework.cloud.dataflow.core.dsl.TaskVisitor;

/**
 * Creates a stack of task executions from a composed task DSL.
 *
 * @author Glenn Renfro.
 */
public class ComposedRunnerVisitor extends TaskVisitor {

	private Deque<LabelledTaskNode> flowDeque = new LinkedList<>();

	private static final Log logger = LogFactory.getLog(org.springframework.cloud.dataflow.composedtaskrunner.ComposedRunnerVisitor.class);

	/**
	 * Push the flow node on the stack to record the beginning of the flow.
	 *
	 * @param flow the flow which represents things to execute in sequence
	 * @return false to skip visiting this flow
	 */
	public boolean preVisit(FlowNode flow) {
		logger.debug("Pre Visit Flow:  " + flow);
		this.flowDeque.push(flow);
		return true;
	}

	/**
	 * Push the split node on the stack to record the beginning of the split.
	 *
	 * @param split the information pertaining to the elements contained in the
	 *              split.
	 */
	public void visit(SplitNode split) {
		logger.debug("Visit Split:  " + split);
		this.flowDeque.push(split);
	}

	/**
	 * Push the split node on the stack to record the Ending of the split.
	 *
	 * @param split the information pertaining to the elements contained in the
	 *              split.
	 */
	public void postVisit(SplitNode split) {
		logger.debug("Post Visit Split:  " + split);
		this.flowDeque.push(split);
	}

	/**
	 * Push the task app node on the stack to record the task app.
	 *
	 * @param taskApp the information pertaining to the taskAppNode contained
	 *                in the flow.
	 */
	public void visit(TaskAppNode taskApp) {
		logger.debug("Visit taskApp:  " + taskApp);
		this.flowDeque.push(taskApp);
	}

	public Deque<LabelledTaskNode> getFlow() {
		return this.flowDeque;
	}

}
