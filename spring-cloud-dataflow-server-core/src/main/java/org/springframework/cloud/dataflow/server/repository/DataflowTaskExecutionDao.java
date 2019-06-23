/*
 * Copyright 2019 the original author or authors.
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
	 * Find all child task execution ids for the provided task execution ids.
	 *
	 * @param taskExecutionId Must contain at least 1 taskExecutionId
	 * @return A set of task execution or an empty collection, but never null
	 */
	Set<Long> findChildTaskExecutionIds(Set<Long> taskExecutionIds);

}
