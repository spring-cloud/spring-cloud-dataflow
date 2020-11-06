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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.JobOperations;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.JobInstanceResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionStatus;
import org.springframework.util.StringUtils;

/**
 * Represents a Task defined on DataFlow server. New Task can be defined with the help of a fluent style builder
 * pattern or use the {@link Task} static utility methods to retrieve existing tasks already defined in DataFlow.
 *
 * For for instance you can define a new task like this:
 * <pre>
 *     {@code
 *     Task task = Task.builder(dataflowOperations)
 *              .name("myComposedTask")
 *              .definition("a: timestamp && b:timestamp")
 *              .description("My Composed Task")
 *              .build();
 *     }
 * </pre>
 *
 * Next you can launch the task and inspect the executions result. Mind that the task is run asynchronously.
 * <pre>
 *     import org.awaitility.Awaitility;
 *
 *     {@code
 *          long launchId = task.launch();
 *
 *          // Leverage Awaitility to wait until task completion.
 *          Awaitility.await().until(() -> task.executionStatus(launchId) == TaskExecutionStatus.COMPLETE);
 *
 *          // Check the executions
 *          task.executions().forEach( execution -> System.out.println(execution.getExitCode()));
 *     }
 * </pre>
 *
 * Use <pre>{@code close()}</pre> to destroy the task manually. Since tasks are auto-closable you can use the
 * Java try block instead:
 * <pre>
 *
 *     {@code
 *          try (Task task = Task.builder(dataFlowOperations)
 *               .name("myTask")
 *               .definition("timestamp")
 *               .description("Test timestamp task")
 *               .build()) {
 *
 *                 long launchId1 = task.launch();
 *                 // Do something
 *          } // Task is destroyed.
 *     }
 * </pre>
 *
 * Use the {@link TaskBuilder#allTasks()} and {@link TaskBuilder#findByName(String)}
 * static helper methods to list or retrieve existing tasks defined in DataFlow.
 *
 * @author Christian Tzolov
 */
public class Task implements AutoCloseable {
	private final String taskName;
	private final TaskOperations taskOperations;
	private final JobOperations jobOperations;
	private final DataFlowOperations dataFlowOperations;

	Task(String taskName, DataFlowOperations dataFlowOperations) {
		this.taskName = taskName;
		this.dataFlowOperations = dataFlowOperations;
		this.taskOperations = dataFlowOperations.taskOperations();
		this.jobOperations = dataFlowOperations.jobOperations();
	}

	//--------------------------------------------------------------------------------------------------------
	//                     Build new or Retrieve an existing Task
	//--------------------------------------------------------------------------------------------------------

	/**
	 * Fluent API method to create a {@link TaskBuilder}.
	 * @param dataFlowOperations {@link DataFlowOperations} Data Flow Rest client instance.
	 * @return A fluent style builder to create tasks.
	 */
	public static TaskBuilder builder(DataFlowOperations dataFlowOperations) {
		return new TaskBuilder(dataFlowOperations);
	}
	
	/**
	 * Launch a task without properties or arguments.
	 * @return long containing the TaskExecutionId
	 */
	public long launch() {
		return this.launch(Collections.EMPTY_LIST);
	}

	/**
	 * Launch a task with command line arguments.
	 * @param arguments the command line arguments.
	 * @return long containing the TaskExecutionId
	 */
	public long launch(List<String> arguments) {
		return this.launch(Collections.EMPTY_MAP, arguments);
	}

	/**
	 * Launch a task with deployment properties and command line arguments.
	 * @param properties the deployment properties.
	 * @param arguments the command line arguments.
	 * @return long containing the TaskExecutionId
	 */
	public long launch(Map<String, String> properties, List<String> arguments) {
		if (properties == null) {
			throw new IllegalArgumentException("Task properties can't be null!");
		}
		return this.taskOperations.launch(this.taskName, properties, arguments, null);
	}

	/**
	 * Stop all Tasks' running {@link org.springframework.cloud.task.repository.TaskExecution}s.
	 *
	 * Note: this functionality is platform dependent! It works for local platform but does nothing on K8s!
	 */
	public void stop() {
		String commaSeparatedIds = executions().stream()
				.filter(Objects::nonNull)
				.filter(e -> e.getTaskExecutionStatus() == TaskExecutionStatus.RUNNING)
				.map(TaskExecutionResource::getExecutionId)
				.map(String::valueOf)
				.collect(Collectors.joining(","));
		if (StringUtils.hasText(commaSeparatedIds)) {
			this.taskOperations.stop(commaSeparatedIds);
		}
	}

	/**
	 * Stop a list of {@link org.springframework.cloud.task.repository.TaskExecution}s.
	 * @param taskExecutionIds List of {@link org.springframework.cloud.task.repository.TaskExecution} ids to stop.
	 *
	 * Note: this functionality is platform dependent! It works for local platform but does nothing on K8s!
	 */
	public void stop(long... taskExecutionIds) {
		String commaSeparatedIds = Stream.of(taskExecutionIds)
				.map(String::valueOf)
				.collect(Collectors.joining(","));
		if (StringUtils.hasText(commaSeparatedIds)) {
			this.taskOperations.stop(commaSeparatedIds);
		}
	}

	/**
	 * Destroy the task.
	 */
	public void destroy() {
		this.taskOperations.destroy(this.taskName);
	}

	//--------------------------------------------------------------------------------------------------------
	//                                    TASK EXECUTIONS
	//--------------------------------------------------------------------------------------------------------

	/**
	 * List task executions for this task.
	 * @return List of task executions for the given task.
	 */
	public Collection<TaskExecutionResource> executions() {
		return taskOperations.executionListByTaskName(this.taskName).getContent();
	}

	/**
	 * Retrieve task execution by Id.
	 * @param executionId Task execution Id
	 * @return Task executions for the given task execution id.
	 */
	public Optional<TaskExecutionResource> execution(long executionId) {
		return this.executions().stream()
				.filter(Objects::nonNull)
				.filter(e -> e.getExecutionId() == executionId)
				.findFirst();
	}

	/**
	 * Find {@link TaskExecutionResource} by a parent execution id.
	 * @param parentExecutionId parent task execution id.
	 * @return Return TaskExecutionResource
	 */
	public Optional<TaskExecutionResource> executionByParentExecutionId(long parentExecutionId) {
		return this.executions().stream()
				.filter(Objects::nonNull)
				.filter(e -> e.getParentExecutionId() == parentExecutionId)
				.findFirst();
	}

	/**
	 * Task execution status
	 * @param executionId execution Id
	 * @return returns the task execution status.
	 */
	public TaskExecutionStatus executionStatus(long executionId) {
		return this.execution(executionId)
				.map(TaskExecutionResource::getTaskExecutionStatus)
				.orElse(TaskExecutionStatus.UNKNOWN);
	}

	/**
	 * @return Return true if composed task or false otherwise.
	 */
	public boolean isComposed() {
		return this.definitionResource().map(TaskDefinitionResource::isComposed).orElse(false);
	}

	/**
	 * @return If a composed task return the list of children sub-tasks. Returns empty list if not composed task.
	 */
	public List<Task> composedTaskChildTasks() {
		return !isComposed() ?
				new ArrayList<>() :
				this.taskOperations.list().getContent().stream()
						.filter(Objects::nonNull)
						.filter(t -> t.getName().startsWith(this.taskName + "-"))
						.map(t -> new Task(t.getName(), this.dataFlowOperations))
						.collect(Collectors.toList());
	}

	//--------------------------------------------------------------------------------------------------------
	//                                    TASK JOBS
	//--------------------------------------------------------------------------------------------------------

	/**
	 * @return Returns list of {@link JobExecutionResource} belonging to the task.
	 */
	public Collection<JobExecutionResource> jobExecutionResources() {
		return this.jobOperations.executionListByJobName(this.taskName).getContent();
	}

	/**
	 * @return Returns list of {@link JobInstanceResource} belonging to this task.
	 */
	public Collection<JobInstanceResource> jobInstanceResources() {
		return this.jobOperations.instanceList(this.taskName).getContent();
	}

	private Optional<TaskDefinitionResource> definitionResource() {
		return this.taskOperations.list().getContent().stream()
				.filter(Objects::nonNull)
				.filter(t -> t.getName().equals(this.taskName))
				.findFirst();
	}

	/**
	 * @return Returns the task name.
	 */
	public String getTaskName() {
		return taskName;
	}

	@Override
	public void close() {
		destroy();
	}
}
