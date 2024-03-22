/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.naming.OperationNotSupportedException;

import org.springframework.cloud.dataflow.rest.resource.CurrentTaskExecutionsResource;
import org.springframework.cloud.dataflow.rest.resource.LaunchResponseResource;
import org.springframework.cloud.dataflow.rest.resource.LauncherResource;
import org.springframework.cloud.dataflow.rest.resource.TaskAppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionThinResource;
import org.springframework.hateoas.PagedModel;

/**
 * Interface defining operations available against tasks.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Gunnar Hillert
 * @author David Turanski
 * @author Corneil du Plessis
 */
public interface TaskOperations {

	/**
	 * @return the list tasks known to the system.
	 */
	PagedModel<TaskDefinitionResource> list();


	/**
	 * @return the list of platform accounts for tasks.
	 */
	PagedModel<LauncherResource> listPlatforms();

	/**
	 * Create a new task definition
	 *
	 * @param name        the name of the task
	 * @param definition  the task definition DSL
	 * @param description the description of the task definition
	 * @return the task definition
	 */
	TaskDefinitionResource create(String name, String definition, String description);

	/**
	 * Launch an already created task.
	 *
	 * @param name       the name of the task
	 * @param properties the deployment properties
	 * @param arguments  the command line arguments
	 * @return long containing the TaskExecutionId
	 */
	LaunchResponseResource launch(String name, Map<String, String> properties, List<String> arguments);

	/**
	 * Request the stop of a group {@link org.springframework.cloud.task.repository.TaskExecution}s.
	 *
	 * @param ids          comma delimited set of {@link org.springframework.cloud.task.repository.TaskExecution} ids to stop.
	 * @param schemaTarget the schema target of the task execution.
	 */
	void stop(String ids, String schemaTarget);

	/**
	 * Request the stop of a group {@link org.springframework.cloud.task.repository.TaskExecution}s.
	 *
	 * @param ids          comma delimited set of {@link org.springframework.cloud.task.repository.TaskExecution} ids to stop.
	 * @param schemaTarget the schema target of the task execution.
	 * @param platform     the platform name where the task is executing.
	 */
	void stop(String ids, String schemaTarget, String platform);

	/**
	 * Destroy an existing task.
	 *
	 * @param name the name of the task
	 */
	void destroy(String name);

	/**
	 * Destroy an existing task with the flag to cleanup task resources.
	 *
	 * @param name    the name of the task
	 * @param cleanup flag indicates task execution cleanup
	 */
	void destroy(String name, boolean cleanup);

	/**
	 * @return the list task executions known to the system.
	 */
	PagedModel<TaskExecutionResource> executionList();

	/**
	 * @return the list of thin task executions known to the system.
	 */
	PagedModel<TaskExecutionThinResource> thinExecutionList();

	/**
	 * List task executions known to the system filtered by task name.
	 *
	 * @param taskName of the executions.
	 * @return the paged list of task executions for the given task name
	 */
	PagedModel<TaskExecutionResource> executionListByTaskName(String taskName);

	/**
	 * Return the {@link TaskExecutionResource} for the id specified.
	 *
	 * @param id           identifier of the task execution
	 * @param schemaTarget the schema target of the task execution.
	 * @return {@link TaskExecutionResource}
	 */
	TaskExecutionResource taskExecutionStatus(long id, String schemaTarget);

	/**
	 * Return the task execution log.  The platform from which to retrieve the log will be set to {@code default}.
	 *
	 * @param externalExecutionId the external execution identifier of the task execution.
	 * @return {@link String} containing the log.
	 */
	String taskExecutionLog(String externalExecutionId);

	/**
	 * Return the task execution log.
	 *
	 * @param externalExecutionId the external execution identifier of the task execution.
	 * @param platform            the platform from which to obtain the log.
	 * @return {@link String} containing the log.
	 */
	String taskExecutionLog(String externalExecutionId, String platform);

	/**
	 * Return information including the count of currently executing tasks and task execution
	 * limits.
	 *
	 * @return Collection of {@link CurrentTaskExecutionsResource}
	 */
	Collection<CurrentTaskExecutionsResource> currentTaskExecutions();

	/**
	 * Cleanup any resources associated with the execution for the id specified.
	 *
	 * @param id           identifier of the task execution
	 * @param schemaTarget the schema target of the task execution.
	 */
	void cleanup(long id, String schemaTarget);

	/**
	 * Cleanup any resources associated with the execution for the id specified.
	 *
	 * @param id           identifier of the task execution
	 * @param schemaTarget the schema target of the task execution.
	 * @param removeData   delete the history of the execution
	 */
	void cleanup(long id, String schemaTarget, boolean removeData);


	/**
	 * Cleanup any resources associated with the matching task executions.
	 *
	 * @param completed cleanup only completed task executions
	 * @param taskName  the name of the task to cleanup, if null then all the tasks are considered.
	 */
	void cleanupAllTaskExecutions(boolean completed, String taskName);

	/**
	 * Get the task executions count with the option to filter only the completed task executions.
	 *
	 * @param completed cleanup only completed task executions
	 * @param taskName  the name of the task to cleanup, if null then all the tasks are considered.
	 * @return the number of task executions.
	 */
	Integer getAllTaskExecutionsCount(boolean completed, String taskName);

	/**
	 * Return the validation status for the tasks in an definition.
	 *
	 * @param taskDefinitionName The name of the task definition to be validated.
	 * @return {@link TaskAppStatusResource} containing the task app statuses.
	 * @throws OperationNotSupportedException if the server does not support task validation
	 */
	TaskAppStatusResource validateTaskDefinition(String taskDefinitionName) throws OperationNotSupportedException;

	/**
	 * Destroy all existing tasks.
	 */
	void destroyAll();
}
