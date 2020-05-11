/*
 * Copyright 2017-2020 the original author or authors.
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
package org.springframework.cloud.dataflow.server.repository;

import java.util.Set;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;

/**
 * Repository to access {@link TaskExecution}s. Mirrors the {@link TaskExecutionDao}
 * but contains Spring Cloud Data Flow specific operations. This functionality might
 * be migrated to Spring Cloud Task itself.
 *
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public interface DataflowTaskExecutionDao {

	/**
	 * Deletes 1 or more task execution parameter records.
	 *
	 * @param taskExecutionIds Must contain at least 1 taskExecutionId
	 * @return The number of affected records
	 */
	int deleteTaskExecutionParamsByTaskExecutionIds(Set<Long> taskExecutionIds);

	/**
	 * Deletes 1 or more task executions
	 *
	 * @param taskExecutionIds Must contain at least 1 taskExecutionId
	 * @return The number of affected records
	 */
	int deleteTaskExecutionsByTaskExecutionIds(Set<Long> taskExecutionIds);

	/**
	 * Deletes 1 or more task batch relationship records that links batch executions
	 * to task executions.
	 *
	 * @param taskExecutionIds Must contain at least 1 taskExecutionId
	 * @return The number of affected records
	 */
	int deleteTaskTaskBatchRelationshipsByTaskExecutionIds(Set<Long> taskExecutionIds);

	/**
	 * Finds all the child tasks for the taskExecution that is provided.
	 * @param taskExecutionIds The ids to the{@link TaskExecution}s to be searched
	 * @return a list of child {@link TaskExecution}s that correspond to the {@link TaskExecution}s passed.
	 */
	Set<Long> findChildTaskExecutionIds(Set<Long> taskExecutionIds);

	/**
	 * Returns the Set of task execution IDs by the given task name.
	 * @param taskName the task name
	 * @return the Set of task execution IDs
	 */
	Set<Long> getTaskExecutionIdsByTaskName(String taskName);
}
