/*
 * Copyright 2017-2021 the original author or authors.
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

import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

/**
 * Repository to access {@link TaskExecution}s. Mirrors the {@link TaskExecutionDao}
 * but contains Spring Cloud Data Flow specific operations. This functionality might
 * be migrated to Spring Cloud Task itself.
 *
 * @author Corneil du Plessis
 * @since 2.11.0
 */
public interface DataflowTaskExecutionQueryDao {
	/**
	 * Retrieves a task execution from the task repository.
	 *
	 * @param executionId the id associated with the task execution.
	 * @param schemaTarget the schema target.
	 * @return a fully qualified TaskExecution instance.
	 */
	AggregateTaskExecution getTaskExecution(long executionId, String schemaTarget);

	/**
	 * Retrieves a list of task executions where the provided execution id and schemaTarget represents the parent of task execution.
	 *
	 * @param executionId  parent task execution id
	 * @param schemaTarget parent task schema target
	 * @return the task executions
	 */
	List<AggregateTaskExecution> findChildTaskExecutions(long executionId, String schemaTarget);

	/**
	 * Retrieves a list of task executions where the provided execution ids and schemaTarget represents the parents of task executions.
	 *
	 * @param parentIds    parent task execution ids
	 * @param schemaTarget parent task schema target
	 * @return the task executions
	 */
	List<AggregateTaskExecution> findChildTaskExecutions(Collection<Long> parentIds, String schemaTarget);

	/**
	 * Find task executions by task name and completion status.
	 *
	 * @param taskName  the name of the task to search for in the repository.
	 * @param completed whether to include only completed task executions.
	 * @return list of task executions
	 */
	List<AggregateTaskExecution> findTaskExecutions(String taskName, boolean completed);

	/**
	 * Find task executions by task name whose end date is before the specified date.
	 *
	 * @param taskName  the name of the task to search for in the repository.
	 * @param endTime the time before the task ended.
	 * @return list of task executions.
	 */
	List<AggregateTaskExecution> findTaskExecutionsBeforeEndTime(String taskName, @NonNull Date endTime);

	/**
	 * Retrieves current number of task executions for a taskName.
	 *
	 * @param taskName the name of the task
	 * @return current number of task executions for the taskName.
	 */
	long getTaskExecutionCountByTaskName(String taskName);

	/**
	 * Retrieves current number of task executions for a taskName and with a non-null endTime before the specified date.
	 *
	 * @param taskName the name of the task
	 * @param endTime the time before task ended
	 * @return the number of completed task executions
	 */
	long getCompletedTaskExecutionCountByTaskNameAndBeforeDate(String taskName, @NonNull Date endTime);

	/**
	 * Retrieves current number of task executions for a taskName and with a non-null endTime.
	 *
	 * @param taskName the name of the task
	 * @return the number of completed task executions
	 */
	long getCompletedTaskExecutionCountByTaskName(String taskName);

	/**
	 * Retrieves current number of task executions for a taskName and with an endTime of
	 * null.
	 *
	 * @param taskName the name of the task to search for in the repository.
	 * @return the number of running task executions
	 */
	long getRunningTaskExecutionCountByTaskName(String taskName);

	/**
	 * Retrieves current number of task executions with an endTime of null.
	 *
	 * @return current number of task executions.
	 */
	long getRunningTaskExecutionCount();

	/**
	 * Retrieves current number of task executions.
	 *
	 * @return current number of task executions.
	 */
	long getTaskExecutionCount();

	/**
	 * Retrieves a set of task executions that are running for a taskName.
	 *
	 * @param taskName the name of the task to search for in the repository.
	 * @param pageable the constraints for the search.
	 * @return set of running task executions.
	 */
	Page<AggregateTaskExecution> findRunningTaskExecutions(String taskName, Pageable pageable);

	/**
	 * Retrieves a subset of task executions by task name, start location and size.
	 *
	 * @param taskName the name of the task to search for in the repository.
	 * @param pageable the constraints for the search.
	 * @return a list that contains task executions from the query bound by the start
	 * position and count specified by the user.
	 */
	Page<AggregateTaskExecution> findTaskExecutionsByName(String taskName, Pageable pageable);

	/**
	 * Retrieves a sorted list of distinct task names for the task executions.
	 *
	 * @return a list of distinct task names from the task repository..
	 */
	List<String> getTaskNames();

	/**
	 * Retrieves all the task executions within the pageable constraints.
	 *
	 * @param pageable the constraints for the search
	 * @return page containing the results from the search
	 */

	Page<AggregateTaskExecution> findAll(Pageable pageable);

	/**
	 * Retrieves all the task executions within the pageable constraints.
	 * @param pageable the constraints for the search
	 * @param thinResults Indicated if arguments will be populated
	 * @return page containing the results from the search
	 */

	Page<AggregateTaskExecution> findAll(Pageable pageable, boolean thinResults);

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

	AggregateTaskExecution geTaskExecutionByExecutionId(String executionId, String taskName);

}
