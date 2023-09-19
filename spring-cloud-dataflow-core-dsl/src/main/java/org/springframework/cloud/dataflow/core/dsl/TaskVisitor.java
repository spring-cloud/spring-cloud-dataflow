/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.core.dsl;

/**
 * Basic visitor pattern for a parsed task. Provide a concrete implementation to
 * participate in the visit and pass it to a parsed TaskNode. A simple task only has one
 * sequence, for example: {@code appA && appB && appC}. In this situation
 * <tt> preVisit(int)</tt> and <tt>postVisit(int)</tt> will only be called with 0. A more
 * complex situation would be:
 *
 * <pre>
 * {@code
 * appA && appB 0->:foo *->appC
 * foo: appD && appE
 * }
 * </pre>
 *
 * This includes two sequences - as in two separate definitions. The primary definition
 * references other definitions where it would be too messy to inline them. In this case
 * {@link #preVisit(FlowNode)} would be called.
 *
 * @author Andy Clement
 */
public abstract class TaskVisitor {

	/**
	 * The first call made to a visitor.
	 *
	 * @param taskName the name of the task definition
	 * @param taskDsl the textual definition of the AST being visited
	 */
	public void startVisit(String taskName, String taskDsl) {
	}

	/**
	 * The last call made to a visitor.
	 */
	public void endVisit() {
	}

	/**
	 * @param firstNode the first node in the sequence
	 * @param sequenceNumber the sequence number, where the primary sequence is 0
	 * @return false to skip visiting the specified sequence
	 */
	public boolean preVisitSequence(LabelledTaskNode firstNode, int sequenceNumber) {
		return true;
	}

	public void postVisitSequence(LabelledTaskNode firstNode, int sequenceNumber) {
	}

	/**
	 * @param flow the flow which represents things to execute in sequence
	 * @return false to skip visiting this flow
	 */
	public boolean preVisit(FlowNode flow) {
		return true;
	}

	public void visit(FlowNode flow) {
	}

	public void postVisit(FlowNode flow) {
	}

	/**
	 * @param split the split which represents things to execute in parallel
	 * @return false to skip visiting this split
	 */
	public boolean preVisit(SplitNode split) {
		return true;
	}

	public void visit(SplitNode split) {
	}

	public void postVisit(SplitNode split) {
	}

	/**
	 * <b>This preVisit/visit/postVisit sequence for taskApp is not invoked for inlined
	 * references to apps in transitions, for example: {@code appA 0->:foo 1->appB}. The
	 * reference to {@code appB} would be seen in the transition visit below.</b>
	 *
	 * @param taskApp the use of a task app in a task dsl
	 * @return false to skip visiting this taskApp
	 */
	public boolean preVisit(TaskAppNode taskApp) {
		return true;
	}

	public void visit(TaskAppNode taskApp) {
	}

	public void postVisit(TaskAppNode taskApp) {
	}

	/**
	 * After {@link #visit(TaskAppNode)} and before {@link #postVisit(TaskAppNode)} the
	 * transitions (if there are any) are visited for that task app.
	 *
	 * @param transition the transition
	 * @return false to skip visiting this transition
	 */
	public boolean preVisit(TransitionNode transition) {
		return true;
	}

	public void visit(TransitionNode transition) {
	}

	public void postVisit(TransitionNode transition) {
	}

}
