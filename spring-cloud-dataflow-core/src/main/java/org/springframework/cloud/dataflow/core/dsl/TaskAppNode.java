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

import java.util.List;

/**
 * Common AST base class for nodes representing job definitions or job references.
 *
 * @author Andy Clement
 */
public class TaskAppNode extends LabelledComposedTaskNode {

	private Token taskName;
	
	private List<TransitionNode> transitions;

	TaskAppNode(Token taskName, List<TransitionNode> transitions) {
		super(taskName.startPos, (transitions == null || transitions.isEmpty())?taskName.endPos:transitions.get(transitions.size()-1).endPos);
		this.taskName = taskName;
		this.transitions = transitions;
	}

	void setTransitions(List<TransitionNode> transitions) {
		this.transitions = transitions;
	}

	@Override
	public String toString() {
		return "TaskApp: " + stringify(true);
	}

	@Override
	public final boolean isTaskApp() {
		return true;
	}

	public List<TransitionNode> getTransitions() {
		return transitions;
	}

	public boolean hasTransitions() {
		return transitions != null && !transitions.isEmpty();
	}

	public String getName() {
		return taskName.stringValue();
	}

	@Override
	public String stringify(boolean includePositionInfo) {
		StringBuilder s = new StringBuilder();
		if (hasLabel()) {
			s.append(getLabelString()).append(": ");
		}
		s.append(getName());
		s.append(" ");
		for (int i=0;i<transitions.size();i++) {
			TransitionNode t = transitions.get(i);
			if (i > 0) {
				s.append(" ");
			}
			s.append(t.stringify(includePositionInfo));
		}
		return s.toString().trim();
	}

	@Override
	public void accept(ComposedTaskVisitor visitor) {
		boolean cont = visitor.preVisit(this);
		if (!cont) {
			return;
		}
		visitor.visit(this);
		for (TransitionNode transition: transitions) {
			transition.accept(visitor);
		}
		visitor.postVisit(this);
	}

}
