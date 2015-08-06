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

import org.springframework.cloud.data.core.parser.TaskDefinitionParser;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * @author Michael Minella
 */
public class TaskDefinition {

	private static final TaskDefinitionParser parser = new TaskDefinitionParser();

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


	/**
	 * Construct a {@code TaskDefinition}. This constructor is private; use
	 * {@link TaskDefinition.Builder} or
	 * {@link TaskDefinition#getInstance(String, String)} to create a new instance.
	 *
	 * @param taskName taskName of task
	 * @param task task to be used
	 * @param parameters module parameters; may be {@code null}
	 */
	private TaskDefinition(String taskName, String task, Map<String, String> parameters) {
		Assert.notNull(taskName, "taskName must not be null");
		Assert.notNull(task, "task must not be null");
		this.taskName = taskName;
		this.task = task;
		this.parameters = parameters == null
				? Collections.<String, String>emptyMap()
				: Collections.unmodifiableMap(new HashMap<>(parameters));
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

	public static TaskDefinition getInstance(String name, String dsl) {
		return parser.parse(name, dsl);
	}


	/**
	 * Builder object for {@code TaskDefinition}.
	 * This object is mutable to allow for flexibility in specifying module
	 * fields/parameters during parsing.
	 */
	public static class Builder {

		/**
		 * @see TaskDefinition#taskName
		 */
		private String taskName;

		/**
		 * @see TaskDefinition#task
		 */
		private String task;

		/**
		 * @see TaskDefinition#parameters
		 */
		private final Map<String, String> parameters = new HashMap<String, String>();


		/**
		 * Set the taskName.
		 *
		 * @param taskName of task
		 * @return this builder object
		 *
		 * @see TaskDefinition#taskName
		 */
		public Builder setTaskName(String taskName) {
			this.taskName = taskName;
			return this;
		}

		/**
		 * Set the task.
		 *
		 * @param task task to be executed
		 * @return this builder object
		 *
		 * @see TaskDefinition#task
		 */
		public Builder setTask(String task) {
			this.task = task;
			return this;
		}

		/**
		 * Set a module parameter.
		 *
		 * @param name parameter taskName
		 * @param value parameter value
		 * @return this builder object
		 *
		 * @see TaskDefinition#parameters
		 */
		public Builder setParameter(String name, String value) {
			this.parameters.put(name, value);
			return this;
		}

		/**
		 * Add the contents of the provided map to the map of module parameters.
		 *
		 * @param parameters module parameters
		 * @return this builder object
		 *
		 * @see TaskDefinition#parameters
		 */
		public Builder addParameters(Map<String, String> parameters) {
			if(parameters != null) {
				this.parameters.putAll(parameters);
			}
			return this;
		}

		/**
		 * Return taskName of module.
		 *
		 * @return module taskName
		 */
		public String getTaskName() {
			return taskName;
		}

		/**
		 * Return symbolic taskName of a module. This will be explicitly specified in
		 * the DSL string.
		 *
		 * @return task task module
		 */
		public String getTask() {
			return task;
		}

		/**
		 * Return parameters for task. This is specific to the type of task -
		 * for instance an filejdbc task would include a file name as a parameter.
		 * <br />
		 * Note that the contents of this map are <b>mutable</b>.
		 *
		 * @return map of module parameters
		 */
		public Map<String, String> getParameters() {
			return parameters;
		}

		/**
		 * Return a new instance of {@link TaskDefinition}.
		 *
		 * @return new instance of {@code TaskDefinition}
		 */
		public TaskDefinition build() {
			if (this.task == null) {
				this.task = this.taskName;
			}
			return new TaskDefinition(this.taskName, this.task, this.parameters);
		}
	}
}
