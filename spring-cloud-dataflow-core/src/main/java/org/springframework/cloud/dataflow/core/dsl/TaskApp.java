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

import java.util.Map;

/**
 * Represents the use of a task application in a DSL string. These are specific to the
 * composed task definition they were contained in - the name of that composed task
 * definition is used to compute the unique name see {@link #getExecutableDSLName()}.
 *
 * @author Andy Clement
 */
public class TaskApp {

	private final static String PREFIX = "_";
	
	private String composedTaskName;
	
	private String name;
	
	private Map<String,String> arguments;
	
	private String label;

	TaskApp(String composedTaskName, TaskAppNode taskAppNode) {
		this.composedTaskName = composedTaskName;
		this.name = taskAppNode.getName();
		this.arguments = taskAppNode.getArgumentsAsMap();
		this.label = taskAppNode.getLabelString();
	}
	
	public String getComposedTaskName() {
		return composedTaskName;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getArguments() {
		return arguments;
	}

	public String getLabel() {
		return label;
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (label!=null) {
			s.append(label).append(": ");
		}
		s.append(name);
		if (arguments.size()!=0) {
			s.append(" ");
			for (Map.Entry<String,String> argument: arguments.entrySet()) {
				s.append("--").append(argument.getKey()).append("=").append(argument.getValue());
			}
		}
		return s.toString();
	}

	public String getExecutableDSLName() {
		StringBuilder s = new StringBuilder();
		s.append(PREFIX).append(composedTaskName).append(".");
		s.append(label==null?name:label);
		return s.toString();
	}
	
}
