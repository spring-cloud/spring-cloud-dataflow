/*
 * Copyright 2015-2019 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;

import org.springframework.cloud.dataflow.core.dsl.ArgumentNode;
import org.springframework.cloud.dataflow.core.dsl.TaskAppNode;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * @author Michael Minella
 * @author Mark Fisher
 * @author Glenn Renfro
 * @author Andy Clement
 */
@Entity
@Table(name = "TASK_DEFINITIONS")
public class TaskDefinition extends DataFlowAppDefinition {

	public static final String SPRING_CLOUD_TASK_NAME = "spring.cloud.task.name";

	/**
	 * Name of task.
	 */
	@Id
	@Column(name = "DEFINITION_NAME")
	private String taskName;

	/**
	 * DSL definition for stream.
	 */
	@Column(name = "DEFINITION")
	@Lob
	private String dslText;

	/**
	 * Description of the definition (Optional).
	 */
	@Column(name = "DESCRIPTION")
	private String description;

	public TaskDefinition() {
	}

	TaskDefinition(String registeredAppName, String label, Map<String, String> properties) {
		super(registeredAppName, label, ApplicationType.task, properties);
		this.taskName = registeredAppName;
		this.dslText = "";
	}

	/**
	 * Construct a {@code TaskDefinition}
	 *
	 * @param name task definition name
	 * @param registeredAppName the application name
	 * @param label the label associated with the definition
	 * @param properties the properties for the definition
	 * @param dsl task definition DSL expression
	 * @since 2.3
	 */
	TaskDefinition(String name, String registeredAppName, String label, Map<String, String> properties, String dsl) {
		super(registeredAppName, label, ApplicationType.task, properties);
		this.taskName = name;
		this.dslText = dsl;
	}

	/**
	 * Construct a {@code TaskDefinition}
	 *
	 * @param name task definition name
	 * @param dsl task definition DSL expression
	 */
	public TaskDefinition(String name, String dsl) {
		this.taskName = name;
		this.dslText = dsl;
		Map<String, String> properties = new LinkedHashMap<>();
		TaskNode taskNode = new TaskParser(name, dsl, true, true).parse();
		if (taskNode.isComposed()) {
			setRegisteredAppName(name);
		}
		else {
			TaskAppNode singleTaskApp = taskNode.getTaskApp();
			setRegisteredAppName(singleTaskApp.getName());
			if (singleTaskApp.hasArguments()) {
				for (ArgumentNode argumentNode : singleTaskApp.getArguments()) {
					properties.put(argumentNode.getName(), argumentNode.getValue());
				}
			}
		}
		properties.put(SPRING_CLOUD_TASK_NAME, name);
		this.appDefinition = new AppDefinition(name, properties);
	}

	/**
	 * Construct a {@code TaskDefinition}
	 *
	 * @param name task definition name
	 * @param dsl task definition DSL expression
	 * @param description description of the definition
	 */
	public TaskDefinition(String name, String dsl, String description) {
		this(name, dsl);
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public String getTaskName() {
		return this.taskName;
	}

	public String getDslText() {
		return dslText;
	}

	@PostLoad
	public void initialize() {
		Map<String, String> properties = new HashMap<>();
		TaskNode taskNode = new TaskParser(this.taskName, this.dslText, true, true).parse();
		if (taskNode.isComposed()) {
			setRegisteredAppName(this.taskName);
		}
		else {
			TaskAppNode singleTaskApp = taskNode.getTaskApp();
			setRegisteredAppName(singleTaskApp.getName());
			if (singleTaskApp.hasArguments()) {
				for (ArgumentNode argumentNode : singleTaskApp.getArguments()) {
					properties.put(argumentNode.getName(), argumentNode.getValue());
				}
			}
		}
		properties.put(SPRING_CLOUD_TASK_NAME, this.taskName);
		this.appDefinition = new AppDefinition(this.taskName, properties);
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("dslText", this.dslText).append("appDefinition", this.appDefinition)
				.toString();
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
		}
		else if (!dslText.equals(other.dslText))
			return false;
		return true;
	}

	/**
	 * Builder object for {@code TaskDefinition}. This object is mutable to allow for
	 * flexibility in specifying application fields/properties during parsing.
	 */
	public static class TaskDefinitionBuilder {

		/**
		 * @see AppDefinition#getProperties()
		 */
		private final Map<String, String> properties = new HashMap<String, String>();

		/**
		 * @see DataFlowAppDefinition#registeredAppName
		 */
		private String registeredAppName;

		/**
		 * @see AppDefinition#getName()
		 */
		private String label;

		private String dslText;

		private String taskName;

		/**
		 * Create a new builder that is initialized with properties of the given
		 * definition. Useful for "mutating" a definition by building a slightly different
		 * copy.
		 *
		 * @param definition the DataFlowAppDefinition to use when creating the builder
		 * @return a task definition builder
		 */
		public static TaskDefinitionBuilder from(DataFlowAppDefinition definition) {
			TaskDefinitionBuilder builder = new TaskDefinitionBuilder();
			builder.setRegisteredAppName(definition.getRegisteredAppName()).setLabel(definition.getName())
					.addProperties(definition.getProperties());
			return builder;
		}

		/**
		 * Set an app property.
		 *
		 * @param name property name
		 * @param value property value
		 * @return this builder object
		 * @see AppDefinition#getProperties()
		 */
		public TaskDefinitionBuilder setProperty(String name, String value) {
			this.properties.put(name, value);
			return this;
		}

		/**
		 * Establish the DSL Text for a task definition.
		 *
		 * @param dslText the dsl to be used by the TaskDefinition
		 * @return this builder object
		 *
		 * @since 2.3
		 */
		public TaskDefinitionBuilder setDslText(String dslText) {
			this.dslText = dslText;
			return this;
		}

		/**
		 * Establish the task name for a task definition.
		 *
		 * @param taskName the name to be used by the TaskDefinition
		 * @return this builder object
		 * @see AppDefinition#getProperties()
		 * @since 2.3
		 */
		public TaskDefinitionBuilder setTaskName(String taskName) {
			this.taskName = taskName;
			return this;
		}

		/**
		 * Add the contents of the provided map to the map of app properties.
		 *
		 * @param properties app properties
		 * @return this builder object
		 * @see AppDefinition#getProperties()
		 */
		public TaskDefinitionBuilder addProperties(Map<String, String> properties) {
			this.properties.putAll(properties);
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
		 * Set the name of the app in the registry.
		 *
		 * @param registeredAppName name of app in registry
		 * @return this builder object
		 * @see DataFlowAppDefinition#registeredAppName
		 */
		public TaskDefinitionBuilder setRegisteredAppName(String registeredAppName) {
			this.registeredAppName = registeredAppName;
			return this;
		}

		/**
		 * Return symbolic name of a task. If not provided, it will be the same as the
		 * task name.
		 *
		 * @return app label
		 */
		public String getLabel() {
			return label;
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
		 * Return properties for the task. Note that the contents of this map are
		 * <b>mutable</b>.
		 *
		 * @return map of app properties
		 */
		public Map<String, String> getProperties() {
			return properties;
		}

		/**
		 * Sets the contents of the provided map as the map of app properties.
		 *
		 * @param properties app properties
		 * @return this builder object
		 * @see AppDefinition#getProperties()
		 */
		public TaskDefinitionBuilder setProperties(Map<String, String> properties) {
			Assert.notNull(properties, "properties must not be null");
			this.properties.clear();
			this.addProperties(properties);
			return this;
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
			return new TaskDefinition(this.taskName, this.registeredAppName, this.label, this.properties, this.dslText);
		}
	}
}
