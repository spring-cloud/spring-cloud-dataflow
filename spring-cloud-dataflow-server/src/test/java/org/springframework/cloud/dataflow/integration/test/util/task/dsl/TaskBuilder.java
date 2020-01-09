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
package org.springframework.cloud.dataflow.integration.test.util.task.dsl;

import org.springframework.cloud.dataflow.rest.client.JobOperations;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;

/**
 * @author Christian Tzolov
 */
public class TaskBuilder {
	private TaskOperations taskOperations;
	private JobOperations jobOperations;

	TaskBuilder(TaskOperations taskOperations, JobOperations jobOperations) {
		this.taskOperations = taskOperations;
		this.jobOperations = jobOperations;
	}

	private String taskDefinitionName;
	private String taskDefinition;
	private String taskDescription;

	public TaskBuilder name(String name) {
		this.taskDefinitionName = name;
		return this;
	}

	public TaskBuilder definition(String taskDefinition) {
		this.taskDefinition = taskDefinition;
		return this;
	}

	public TaskBuilder description(String taskDescription) {
		this.taskDescription = taskDescription;
		return this;
	}

	public Task create() {
		TaskDefinitionResource td = this.taskOperations.create(this.taskDefinitionName, this.taskDefinition, this.taskDescription);
		return new Task(td.getName(), this.taskOperations, this.jobOperations);
	}
}
