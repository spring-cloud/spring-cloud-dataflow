/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.cloud.dataflow.rest.client.dsl.task;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;

/**
 * Represents the entry point into the Task DSL to set the name and the definition of the task.
 *
 * @author Christian Tzolov
 */
public class TaskBuilder {

	private final DataFlowOperations dataFlowOperations;

	private String taskDefinitionName;
	private String taskDefinition;
	private String taskDescription;

	TaskBuilder(DataFlowOperations dataFlowOperations) {
		this.dataFlowOperations = dataFlowOperations;
	}

	/**
	 * Fluent API method to set the name for the task definition.
	 * @param definitionName - definition name of a Task with the server. Note that the actual task name is computed
	 *                          form the definition name, task definition and task description.
	 * @return A {@link TaskBuilder} that provides the next navigation step in the DSL.
	 */
	public TaskBuilder name(String definitionName) {
		this.taskDefinitionName = definitionName;
		return this;
	}

	/**
	 * Fluent API method to set the task definition.
	 * @param taskDefinition - The task definition to use.
	 * @return A {@link TaskBuilder} that provides the next navigation step in the DSL.
	 */
	public TaskBuilder definition(String taskDefinition) {
		this.taskDefinition = taskDefinition;
		return this;
	}

	/**
	 * @param taskDescription Task description.
	 * @return A {@link TaskBuilder} that provides the next navigation step in the DSL.
	 */
	public TaskBuilder description(String taskDescription) {
		this.taskDescription = taskDescription;
		return this;
	}

	/**
	 * Create new Task instance.
	 * @return Task instance.
	 */
	public Task build() {
		// Use a TaskDefinitionResource instance to generate a compliant Task name.
		TaskDefinitionResource taskDefinitionResource = this.dataFlowOperations.taskOperations().create(
				this.taskDefinitionName, this.taskDefinition, this.taskDescription);
		return new Task(taskDefinitionResource.getName(), this.dataFlowOperations);
	}

	/**
	 * @return All tasks defined currently in Data Flow.
	 */
	public List<Task> allTasks() {
		return this.dataFlowOperations.taskOperations().list().getContent().stream()
				.map(td -> new Task(td.getName(), this.dataFlowOperations))
				.collect(Collectors.toList());
	}

	/**
	 * @param taskName existing Task name used to retrieve the task.
	 * @return Task instance identified by the taskName.
	 */
	public Optional<Task> findByName(String taskName) {
		return allTasks().stream()
				.filter(task -> task.getTaskName().equalsIgnoreCase(taskName))
				.findFirst();
	}

}
