/*
 * Copyright 2015-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.Properties;

/**
 * @author Andy Clement
 * @author Mark Fisher
 */
public class AppNode extends AstNode {

	private static final ArgumentNode[] NO_ARGUMENTS = new ArgumentNode[0];

	protected final String appName;

	protected LabelNode label;

	protected ArgumentNode[] arguments;
	
	protected boolean isUnboundStreamApp = true;
	
	public AppNode(LabelNode label, String appName, int startPos, int endPos, ArgumentNode[] arguments) {
		super(startPos, endPos);
		this.label = label;
		this.appName = appName;
		if (arguments != null) {
			this.arguments = Arrays.copyOf(arguments, arguments.length);
			// adjust end pos for app node to end of final argument
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
		s.append(appName);
		if (arguments != null) {
			for (ArgumentNode argumentNode : arguments) {
				s.append(" --").append(argumentNode.getName()).append("=").append(argumentNode.getValue());
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
		s.append("AppNode:").append(appName);
		if (arguments != null) {
			for (ArgumentNode argumentNode : arguments) {
				s.append(" --").append(argumentNode.getName()).append("=").append(argumentNode.getValue());
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
		return appName;
	}

	public ArgumentNode[] getArguments() {
		return arguments;
	}

	public boolean hasArguments() {
		return arguments != null;
	}

	/**
	 * Return the label for this app, that is:
	 * <ul>
	 * <li>an explicit label if provided</li>
	 * <li>the app name if no label was provided</li>
	 * </ul>
	 * @return String containing the label
	 */
	public String getLabelName() {
		return (label != null) ? label.getLabelName() : appName;
	}

	/**
	 * @return Retrieve the app arguments as a simple {@link java.util.Properties} object.
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

	public void setUnboundStreamApp(boolean b) {
		isUnboundStreamApp = b;
	}
	
	public boolean isUnboundStreamApp() {
		return isUnboundStreamApp;
	}

}
