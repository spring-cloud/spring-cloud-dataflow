/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.data.core.dsl.ArgumentNode;
import org.springframework.cloud.data.core.dsl.ModuleNode;
import org.springframework.cloud.data.core.dsl.TaskDslParser;
import org.springframework.core.style.ToStringCreator;

/**
 * @author Michael Minella
 * @author Mark Fisher
 */
public class TaskDefinition {

	/**
	 * Name of module.
	 */
	private final String taskName;

	/**
	 * Symbolic taskName of a module. This may be generated to a default
	 * value or specified in the DSL string.
	 */
	private final String task;

	/**
	 * Parameters for module. This is specific to the type of module - for
	 * instance an http module would include a port number as a parameter.
	 */
	private final Map<String, String> parameters;


	public TaskDefinition(String name, String dsl) {
		ModuleNode taskNode = new TaskDslParser(name, dsl).parse();
		this.taskName = name;
		this.task = taskNode.getLabelName();
		HashMap<String, String> arguments = new HashMap<>();
		if (taskNode.hasArguments()) {
			for (ArgumentNode argument : taskNode.getArguments()) {
				arguments.put(argument.getName(), argument.getValue());
			}
		}
		this.parameters = Collections.unmodifiableMap(new HashMap<>(arguments));
	}

	public String getTaskName() {
		return taskName;
	}

	public String getTask() {
		return task;
	}

	/**
	 * Return parameters for module. This is specific to the type of module - for
	 * instance a filejdbc module would contain a file name as a parameter.
	 *
	 * @return read-only map of module parameters
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("taskName", this.taskName)
				.append("task", this.task)
				.append("parameters", this.parameters).toString();
	}
}
