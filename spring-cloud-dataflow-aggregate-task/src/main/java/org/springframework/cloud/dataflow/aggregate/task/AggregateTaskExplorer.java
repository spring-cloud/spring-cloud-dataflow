/*
 * Copyright 2023-2023 the original author or authors.
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
package org.springframework.cloud.dataflow.aggregate.task;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Provides for exploring tasks for multiple schema targets.
 *
 * @author Corneil du Plessis
 */
public interface AggregateTaskExplorer {
	/**
	 * find a task execution given an execution id and schema target.
	 *
	 * @param executionId  the task execution id
	 * @param schemaTarget the schema target
	 * @return the task execution
	 */
	AggregateTaskExecution getTaskExecution(long executionId, String schemaTarget);

	/**
	 * find a task execution given an external execution id and platform name.
	 *
	 * @param externalExecutionId the external execution id
	 * @param platform            the platform name
	 * @return the task execution
	 */
	AggregateTaskExecution getTaskExecutionByExternalExecutionId(String externalExecutionId, String platform);

	List<AggregateTaskExecution> findChildTaskExecutions(long executionId, String schemaTarget);

	List<AggregateTaskExecution> findChildTaskExecutions(Collection<Long> parentIds, String schemaTarget);

	/**
	 * Retrieve a collection of taskExecutions that have the task name provided.
	 *
	 * @param taskName the name of the task
	 * @param pageable the constraints for the search
	 * @return the set of running executions for tasks with the specified name
	 */
	Page<AggregateTaskExecution> findRunningTaskExecutions(String taskName, Pageable pageable);

	/**
	 * Retrieve a list of available task names.
	 *
	 * @return the task names that have been executed
	 */
	List<String> getTaskNames();

	/**
	 * Get number of executions for a taskName.
	 *
	 * @param taskName the name of the task to be searched
	 * @return the number of running tasks that have the taskname specified
	 */
	long getTaskExecutionCountByTaskName(String taskName);

	/**
	 * Retrieves current number of task executions.
	 *
	 * @return current number of task executions.
	 */
	long getTaskExecutionCount();

	/**
	 * Retrieves current number of running task executions.
	 *
	 * @return current number of running task executions.
	 */
	long getRunningTaskExecutionCount();

	/**
	 * Get a list of executions for a task by name and completion status.
	 *
	 * @param taskName  the name of the task to be searched
	 * @param onlyCompleted whether to include only completed tasks
	 * @return list of task executions
	 */
	List<AggregateTaskExecution> findTaskExecutions(String taskName, boolean onlyCompleted);

	/**
	 * Get a list of executions for a task by name, completion status and end time.
	 *
	 * @param taskName  the name of the task to be searched
	 * @param endTime the tasks that ended before the endTime
	 * @return list of task executions
	 * @since 2.11.0
	 */
	List<AggregateTaskExecution> findTaskExecutionsBeforeEndTime(String taskName, Date endTime);

	/**
	 * Get a collection/page of executions.
	 *
	 * @param taskName the name of the task to be searched
	 * @param pageable the constraints for the search
	 * @return list of task executions
	 */
	Page<AggregateTaskExecution> findTaskExecutionsByName(String taskName, Pageable pageable);

	/**
	 * Retrieves all the task executions within the pageable constraints sorted by start
	 * date descending, taskExecution id descending.
	 *
	 * @param pageable the constraints for the search
	 * @return page containing the results from the search
	 */
	Page<AggregateTaskExecution> findAll(Pageable pageable);

	/**
	 * Retrieves all the task executions within the pageable constraints sorted by start
	 * date descending, taskExecution id descending.
	 *
	 * @param pageable the constraints for the search
	 * @param thinResults Indicated if arguments will be populated
	 * @return page containing the results from the search
	 */
	Page<AggregateTaskExecution> findAll(Pageable pageable, boolean thinResults);
	/**
	 * Returns the id of the TaskExecution that the requested Spring Batch job execution
	 * was executed within the context of. Returns null if none were found.
	 *
	 * @param jobExecutionId the id of the JobExecution
	 * @param schemaTarget the schema target
	 * @return the id of the {@link TaskExecution}
	 */
	Long getTaskExecutionIdByJobExecutionId(long jobExecutionId, String schemaTarget);

	/**
	 * Returns a Set of JobExecution ids for the jobs that were executed within the scope
	 * of the requested task.
	 *
	 * @param taskExecutionId id of the {@link TaskExecution}
	 * @param schemaTarget the schema target
	 * @return a <code>Set</code> of the ids of the job executions executed within the
	 * task.
	 */
	Set<Long> getJobExecutionIdsByTaskExecutionId(long taskExecutionId, String schemaTarget);

	/**
	 * Returns a {@link List} of the latest {@link TaskExecution} for 1 or more task
	 * names.
	 * <p>
	 * Latest is defined by the most recent start time. A {@link TaskExecution} does not
	 * have to be finished (The results may including pending {@link TaskExecution}s).
	 * <p>
	 * It is theoretically possible that a {@link TaskExecution} with the same name to
	 * have more than 1 {@link TaskExecution} for the exact same start time. In that case
	 * the {@link TaskExecution} with the highest Task Execution ID is returned.
	 * <p>
	 * This method will not consider end times in its calculations. Thus, when a task
	 * execution {@code A} starts after task execution {@code B} but finishes BEFORE task
	 * execution {@code A}, then task execution {@code B} is being returned.
	 *
	 * @param taskNames At least 1 task name must be provided
	 * @return List of TaskExecutions. May be empty but never null.
	 */
	List<AggregateTaskExecution> getLatestTaskExecutionsByTaskNames(String... taskNames);

	/**
	 * Returns the latest task execution for a given task name. Will ultimately apply the
	 * same algorithm underneath as {@link #getLatestTaskExecutionsByTaskNames(String...)}
	 * but will only return a single result.
	 *
	 * @param taskName Must not be null or empty
	 * @return The latest Task Execution or null
	 * @see #getLatestTaskExecutionsByTaskNames(String...)
	 */
	AggregateTaskExecution getLatestTaskExecutionForTaskName(String taskName);
}
