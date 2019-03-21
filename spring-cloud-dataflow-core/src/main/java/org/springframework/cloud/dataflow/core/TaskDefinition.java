/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.core.dsl.ArgumentNode;
import org.springframework.cloud.dataflow.core.dsl.AppNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * @author Michael Minella
 * @author Mark Fisher
 */
public class TaskDefinition extends DataFlowAppDefinition {

	/**
	 * DSL text for the module.
	 */
	private final String dslText;

	public TaskDefinition(String name, String dsl) {
		this.dslText = dsl;
		AppNode taskNode = new TaskParser(name, dsl).parse();
		setRegisteredAppName(taskNode.getName());
		Map<String, String> properties = new HashMap<>();
		if (taskNode.hasArguments()) {
			for (ArgumentNode argumentNode : taskNode.getArguments()) {
				properties.put(argumentNode.getName(), argumentNode.getValue());
			}
		}
		properties.put("spring.cloud.task.name", name);
		this.appDefinition = new AppDefinition(name, properties);
	}

	TaskDefinition(String registeredAppName, String label, Map<String, String> properties) {
		super(registeredAppName, label, properties);
		this.dslText = "";
		properties.put("spring.cloud.task.name", registeredAppName);
	}

	public String getDslText() {
		return dslText;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("dslText", this.dslText)
				.append("appDefinition", this.appDefinition).toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dslText == null) ? 0 : dslText.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TaskDefinition other = (TaskDefinition) obj;
		if (dslText == null) {
			if (other.dslText != null)
				return false;
		} else if (!dslText.equals(other.dslText))
			return false;
		return true;
	}

	/**
	 * Builder object for {@code TaskDefinition}.
	 * This object is mutable to allow for flexibility in specifying application
	 * fields/properties during parsing.
	 */
	public static class TaskDefinitionBuilder {

		/**
		 * @see DataFlowAppDefinition#registeredAppName
		 */
		private String registeredAppName;

		/**
		 * @see AppDefinition#getName()
		 */
		private String label;

		/**
		 * @see AppDefinition#getProperties()
		 */
		private final Map<String, String> properties = new HashMap<String, String>();

		/**
		 * Create a new builder that is initialized with properties of the given definition.
		 * Useful for "mutating" a definition by building a slightly different copy.
		 */
		public static TaskDefinitionBuilder from(DataFlowAppDefinition definition) {
			TaskDefinitionBuilder builder = new TaskDefinitionBuilder();
			builder.setRegisteredAppName(definition.getRegisteredAppName())
				.setLabel(definition.getName())
				.addProperties(definition.getProperties());
			return builder;
		}


		/**
		 * Set the name of the app in the registry.
		 *
		 * @param registeredAppName name of app in registry
		 * @return this builder object
		 *
		 * @see DataFlowAppDefinition#registeredAppName
		 */
		public TaskDefinitionBuilder setRegisteredAppName(String registeredAppName) {
			this.registeredAppName = registeredAppName;
			return this;
		}

		/**
		 * Set the app label.
		 *
		 * @param label name of app label
		 * @return this builder object
		 */
		public TaskDefinitionBuilder setLabel(String label) {
			this.label = label;
			return this;
		}

		/**
		 * Set an app property.
		 *
		 * @param name property name
		 * @param value property value
		 * @return this builder object
		 *
		 * @see AppDefinition#getProperties()
		 */
		public TaskDefinitionBuilder setProperty(String name, String value) {
			this.properties.put(name, value);
			return this;
		}

		/**
		 * Add the contents of the provided map to the map of app properties.
		 *
		 * @param properties app properties
		 * @return this builder object
		 *
		 * @see AppDefinition#getProperties()
		 */
		public TaskDefinitionBuilder addProperties(Map<String, String> properties) {
			this.properties.putAll(properties);
			return this;
		}

		/**
		 * Sets the contents of the provided map as the map of app properties.
		 *
		 * @param properties app properties
		 * @return this builder object
		 *
		 * @see AppDefinition#getProperties()
		 */
		public TaskDefinitionBuilder setProperties(Map<String, String> properties) {
			Assert.notNull(properties, "properties must not be null");
			this.properties.clear();
			this.addProperties(properties);
			return this;
		}

		/**
		 * Return name of task app in registry.
		 *
		 * @return task app name in registry
		 */
		public String getRegisteredAppName() {
			return registeredAppName;
		}

		/**
		 * Return symbolic name of a task. If not provided, it will be the same as the task name.
		 *
		 * @return app label
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * Return properties for the task.
		 * Note that the contents of this map are <b>mutable</b>.
		 *
		 * @return map of app properties
		 */
		public Map<String, String> getProperties() {
			return properties;
		}

		/**
		 * Return a new instance of {@link TaskDefinition}.
		 *
		 * @return new instance of {@code TaskDefinition}
		 */
		public TaskDefinition build() {
			if (this.label == null) {
				this.label = this.registeredAppName;
			}
			return new TaskDefinition(this.registeredAppName, this.label, this.properties);
		}
	}
}
