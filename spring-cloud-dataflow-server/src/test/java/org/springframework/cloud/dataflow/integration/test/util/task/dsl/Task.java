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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.rest.client.JobOperations;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.JobInstanceResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionStatus;

/**
 * @author Christian Tzolov
 */
public class Task implements AutoCloseable {
	private final TaskOperations taskOperations;
	private final String taskName;
	private JobOperations jobOperations;

	Task(String taskName, TaskOperations taskOperations, JobOperations jobOperations) {
		this.taskOperations = taskOperations;
		this.taskName = taskName;
		this.jobOperations = jobOperations;
	}

	public boolean isComposed() {
		return this.definitionResource().isComposed();
	}

	public long launch() {
		return this.taskOperations.launch(this.taskName, Collections.EMPTY_MAP, Collections.EMPTY_LIST, null);
	}

	public long launch(List<String> arguments) {
		return this.taskOperations.launch(this.taskName, Collections.EMPTY_MAP, arguments, null);
	}

	public void destroy() {
		this.taskOperations.destroy(this.taskName);
	}

	public Collection<TaskExecutionResource> executions() {
		return taskOperations.executionListByTaskName(this.taskName).getContent();
	}


	public Optional<TaskExecutionResource> execution(long executionId) {
		return this.executions().stream()
				.filter(Objects::nonNull)
				.filter(e -> e.getExecutionId() == executionId)
				.findFirst();
	}

	public Optional<TaskExecutionResource> executionByParentExecutionId(long parentExecutionId) {
		return this.executions().stream()
				.filter(Objects::nonNull)
				.filter(e -> e.getParentExecutionId() == parentExecutionId)
				.findFirst();
	}

	public TaskExecutionStatus executionStatus(long executionId) {
		return this.execution(executionId)
				.map(e -> e.getTaskExecutionStatus())
				.orElse(TaskExecutionStatus.UNKNOWN);
	}

	public List<Task> children() {
		return !isComposed() ?
				new ArrayList<>() :
				this.taskOperations.list().getContent().stream()
						.filter(Objects::nonNull)
						.filter(t -> t.getName().startsWith(this.taskName + "-"))
						.map(t -> new Task(t.getName(), this.taskOperations, this.jobOperations))
						.collect(Collectors.toList());
	}

	public Collection<JobExecutionResource> jobExecutionResources() {
		return this.jobOperations.executionListByJobName(this.taskName).getContent();
	}

	public Collection<JobInstanceResource> jobInstanceResources() {
		return this.jobOperations.instanceList(this.taskName).getContent();
	}

	private TaskDefinitionResource definitionResource() {
		return this.taskOperations.list().getContent().stream()
				.filter(Objects::nonNull)
				.filter(t -> t.getName().equals(this.taskName))
				.findFirst().get();
	}

	public String getTaskName() {
		return taskName;
	}

	@Override
	public void close() {
		destroy();
	}
}
