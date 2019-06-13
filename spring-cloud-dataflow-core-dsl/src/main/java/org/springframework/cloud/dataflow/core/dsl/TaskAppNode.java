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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents either a task application or task definition reference. Task application
 * references can have arguments.
 *
 * @author Andy Clement
 */
public class TaskAppNode extends LabelledTaskNode {

	private Token taskName;

	private ArgumentNode[] arguments;

	private Map<String, String> argumentsMap;

	private List<TransitionNode> transitions;

	TaskAppNode(Token taskName, ArgumentNode[] arguments, List<TransitionNode> transitions) {
		super(taskName.startPos,
				(transitions == null || transitions.isEmpty())
						? arguments == null || arguments.length == 0 ? taskName.endPos
								: arguments[arguments.length - 1].endPos
						: transitions.get(transitions.size() - 1).endPos);
		this.taskName = taskName;
		this.arguments = arguments;
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

	void setTransitions(List<TransitionNode> transitions) {
		this.transitions = transitions;
	}

	public boolean hasTransitions() {
		return transitions != null && !transitions.isEmpty();
	}

	public String getName() {
		return taskName.stringValue();
	}

	public boolean hasArguments() {
		return arguments != null && arguments.length != 0;
	}

	public ArgumentNode[] getArguments() {
		return arguments;
	}

	public Map<String, String> getArgumentsAsMap() {
		if (argumentsMap == null) {
			if (arguments == null || arguments.length == 0) {
				argumentsMap = Collections.emptyMap();
			}
			else {
				argumentsMap = new LinkedHashMap<String, String>();
				for (ArgumentNode argument : arguments) {
					argumentsMap.put(argument.getName(), argument.getValue());
				}
			}
		}
		return argumentsMap;
	}

	@Override
	public String stringify(boolean includePositionInfo) {
		StringBuilder s = new StringBuilder();
		if (hasLabel()) {
			s.append(getLabelString()).append(": ");
		}
		s.append(getName());
		if (arguments != null) {
			for (ArgumentNode argument : arguments) {
				s.append(" ").append("--").append(argument.getName()).append("=").append(argument.getValue());
			}
		}
		if (includePositionInfo) {
			s.append(":").append(startPos).append(">").append(endPos);
		}
		s.append(" ");
		for (int i = 0; i < transitions.size(); i++) {
			TransitionNode t = transitions.get(i);
			if (i > 0) {
				s.append(" ");
			}
			s.append(t.stringify(includePositionInfo));
		}
		return s.toString().trim();
	}

	@Override
	public void accept(TaskVisitor visitor) {
		boolean cont = visitor.preVisit(this);
		if (!cont) {
			return;
		}
		visitor.visit(this);
		for (TransitionNode transition : transitions) {
			transition.accept(visitor);
		}
		visitor.postVisit(this);
	}

	public String toDslText() {
		StringBuilder s = new StringBuilder();
		if (getLabel() != null) {
			s.append(getLabelString()).append(": ");
		}
		s.append(taskName.stringValue());
		if (getArgumentsAsMap().size() != 0) {
			s.append(" ");
			for (Map.Entry<String, String> argument : argumentsMap.entrySet()) {
				s.append("--").append(argument.getKey()).append("=").append(argument.getValue());
			}
		}
		return s.toString();
	}

}
