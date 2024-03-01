/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service;

import java.util.Set;

import org.springframework.cloud.dataflow.server.controller.support.TaskExecutionControllerDeleteAction;

/**
 * Provides task deletion services.
 *
 * @author Daniel Serleg
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
public interface TaskDeleteService {
	/**
	 * Cleanup the resources that resulted from running the task with the given execution id.
	 *
	 * @param id the execution id
	 */
	void cleanupExecution(long id);

	/**
	 *  Cleanup the resources that resulted from running the task with the given execution
	 *  ids and actions.
	 *
	 * @param actionsAsSet the actions
	 * @param ids the id's
	 */
	void cleanupExecutions(Set<TaskExecutionControllerDeleteAction> actionsAsSet, Set<Long> ids);

	/**
	 * Clean up the resources that resulted from running the task with the given name.
	 *
	 * @param actionsAsSet the actions to perform
	 * @param taskName the task name
	 * @param onlyCompleted whether to include only completed tasks
	 */
	void cleanupExecutions(Set<TaskExecutionControllerDeleteAction> actionsAsSet, String taskName, boolean onlyCompleted);

	/**
	 * Clean up the resources that resulted from running the task with the given name.
	 *
	 * @param actionsAsSet the actions to perform
	 * @param taskName the task name
	 * @param onlyCompleted whether to include only completed tasks (ignored when {@code includeTasksEndedMinDaysAgo} is specified)
	 * @param includeTasksEndedMinDaysAgo only include tasks that have ended at least this many days ago
	 * @since 2.11.0
	 */
	void cleanupExecutions(Set<TaskExecutionControllerDeleteAction> actionsAsSet, String taskName, boolean onlyCompleted, Integer includeTasksEndedMinDaysAgo);

	/**
	 * Delete one or more Task executions.
	 *
	 * @param ids Collection of task execution ids to delete. Must contain at least 1 id.
	 */
	void deleteTaskExecutions(Set<Long> ids);

	/**
	 * Delete task executions by name and execution state.
	 * @param taskName the name of the task executions
	 * @param onlyCompleted indicator to delete only completed tasks
	 */
	void deleteTaskExecutions(String taskName, boolean onlyCompleted);

	/**
	 * Destroy the task definition. If it is a Composed Task then the task definitions
	 * required for a ComposedTaskRunner task are also destroyed.
	 *
	 * @param name The name of the task.
	 */
	void deleteTaskDefinition(String name);

	/**
	 * Destroy the task definition. If it is a Composed Task then the task definitions
	 * required for a ComposedTaskRunner task are also destroyed.
	 * If the cleanup flag is set to true, delete all the task executions associated with the task definition.
	 *
	 * @param name The name of the task.
	 * @param cleanup the flag to indicate the cleanup of the task executions for the task definition.
	 */
	void deleteTaskDefinition(String name, boolean cleanup);

	/**
	 * Destroy all task definitions. If it is a Composed Task then the task definitions
	 * required for a ComposedTaskRunner tasks are also destroyed.
	 */
	void deleteAll();

}
