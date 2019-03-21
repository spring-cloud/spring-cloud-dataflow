/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
 * @author Andy Clement
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public class DestinationNode extends AstNode {

	private List<String> nameComponents;

	private ArgumentNode[] arguments;

	public DestinationNode(int startPos, int endPos, List<String> nameComponents, ArgumentNode[] arguments) {
		super(startPos, endPos);
		this.nameComponents = nameComponents;
		this.arguments = arguments;
	}

	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append("(");
		s.append(getDestinationName());
		if (includePositionalInfo) {
			s.append(":");
			s.append(getStartPos()).append(">").append(getEndPos());
		}
		if (arguments != null) {
			for (ArgumentNode argumentNode : arguments) {
				s.append(" --").append(argumentNode.getName()).append("=").append(argumentNode.getValue());
			}
		}
		s.append(")");
		return s.toString();
	}

	@Override
	public String toString() {
		return getDestinationName();
	}

	String getDestinationName() {
		StringBuilder s = new StringBuilder();
		for (int t = 0, max = nameComponents.size(); t < max; t++) {
			if (t != 0) {
				s.append(".");
			}
			s.append(nameComponents.get(t));
		}
		return s.toString();
	}

	public DestinationNode copyOf() {
		return new DestinationNode(super.startPos, super.endPos, nameComponents, arguments);
	}

	ArgumentNode[] getArguments() {
		return this.arguments;
	}

}
