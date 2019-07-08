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
	 * Deletes 1 or more Task Executions
	 * @param taskExecutionIds Must contain at least 1 taskExecutionId
	 */
	int deleteTaskExecutionsByTaskExecutionIds(Set<Long> taskExecutionIds);

	/**
	 * Deletes 1 or more Task Executions
	 * @param taskExecutionIds Must contain at least 1 taskExecutionId
	 */
	int deleteTaskExecutionParamsByTaskExecutionIds(Set<Long> taskExecutionIds);

	/**
	 * Deletes 1 or more Task Executions
	 * @param taskExecutionIds Must contain at least 1 taskExecutionId
	 */
	int deleteTaskTaskBatchRelationshipsByTaskExecutionIds(Set<Long> taskExecutionIds);

	/**
	 *
	 * @param taskExecutionId
	 * @return
	 */
	Set<Long> findChildTaskExecutionIds(Set<Long> taskExecutionIds);

}
