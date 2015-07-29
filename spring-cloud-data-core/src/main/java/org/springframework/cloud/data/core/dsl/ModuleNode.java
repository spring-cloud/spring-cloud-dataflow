/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.data.core.dsl;

import java.util.Arrays;
import java.util.Properties;

/**
 * @author Andy Clement
 */
public class ModuleNode extends AstNode {

	private static final ArgumentNode[] NO_ARGUMENTS = new ArgumentNode[0];

	private LabelNode label;

	private final String moduleName;

	private ArgumentNode[] arguments;

	public ModuleNode(LabelNode label, String moduleName, int startPos, int endPos, ArgumentNode[] arguments) {
		super(startPos, endPos);
		this.label = label;
		this.moduleName = moduleName;
		if (arguments != null) {
			this.arguments = Arrays.copyOf(arguments, arguments.length);
			// adjust end pos for module node to end of final argument
			super.endPos = this.arguments[this.arguments.length - 1].endPos;
		}
		else {
			this.arguments = NO_ARGUMENTS;
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (label != null) {
			s.append(label.toString());
			s.append(" ");
		}
		s.append(moduleName);
		if (arguments != null) {
			for (int a = 0; a < arguments.length; a++) {
				s.append(" --").append(arguments[a].getName()).append("=").append(arguments[a].getValue());
			}
		}
		return s.toString();
	}

	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append("(");
		if (label != null) {
			s.append(label.stringify(includePositionalInfo));
			s.append(" ");
		}
		s.append("ModuleNode:").append(moduleName);
		if (arguments != null) {
			for (int a = 0; a < arguments.length; a++) {
				s.append(" --").append(arguments[a].getName()).append("=").append(arguments[a].getValue());
			}
		}
		if (includePositionalInfo) {
			s.append(":");
			s.append(getStartPos()).append(">").append(getEndPos());
		}
		s.append(")");
		return s.toString();
	}

	public String getName() {
		return moduleName;
	}

	public ArgumentNode[] getArguments() {
		return arguments;
	}

	public boolean hasArguments() {
		return arguments != null;
	}

	/**
	 * Return the label for this module, that is:
	 * <ul>
	 * <li>an explicit label if provided</li>
	 * <li>the module name if no label was provided</li>
	 * </ul>
	 */
	public String getLabelName() {
		return (label != null) ? label.getLabelName() : moduleName;
	}

	/**
	 * @return Retrieve the module arguments as a simple {@link java.util.Properties} object.
	 */
	public Properties getArgumentsAsProperties() {
		Properties props = new Properties();
		if (arguments != null) {
			for (ArgumentNode argumentNode : arguments) {
				props.put(argumentNode.getName(), argumentNode.getValue());
			}
		}
		return props;
	}

}
