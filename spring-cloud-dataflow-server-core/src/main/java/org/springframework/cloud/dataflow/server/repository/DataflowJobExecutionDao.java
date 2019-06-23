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

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.repository.dao.JobExecutionDao;

/**
 * Repository to access {@link JobExecution}s. Mirrors the {@link JobExecutionDao}
 * but contains Spring Cloud Data Flow specific operations. This functionality might
 * be migrated to Spring Batch itself eventually.
 *
 * @author Gunnar Hillert
 */
public interface DataflowJobExecutionDao {

	/**
	 * Delete the batch job execution records from the persistence store for
	 * the provided job execution ids.
	 *
	 * @param jobExecutionIds Must contain at least 1 value
	 * @return The number of affected records
	 */
	int deleteBatchJobExecutionByJobExecutionIds(Set<Long> jobExecutionIds);

	/**
	 * Delete the batch job execution context records from the persistence store for
	 * the provided job execution ids.
	 *
	 * @param jobExecutionIds Must contain at least 1 value
	 * @return The number of affected records
	 */
	int deleteBatchJobExecutionContextByJobExecutionIds(Set<Long> jobExecutionIds);

	/**
	 * Delete the batch job execution parameter records from the persistence store for
	 * the provided job execution ids.
	 *
	 * @param jobExecutionIds Must contain at least 1 value
	 * @return The number of affected records
	 */
	int deleteBatchJobExecutionParamsByJobExecutionIds(Set<Long> jobExecutionIds);

	/**
	 * Delete the batch step execution context records from the persistence store for
	 * the provided job execution ids.
	 *
	 * @param jobExecutionIds Must contain at least 1 value
	 * @return The number of affected records
	 */
	int deleteBatchStepExecutionContextByJobExecutionIds(Set<Long> jobExecutionIds);

	/**
	 *
	 * @param jobExecutionIds Must contain at least 1 value
	 * @return The number of affected records
	 */
	int deleteBatchStepExecutionsByJobExecutionIds(Set<Long> jobExecutionIds);

	/**
	 * Will delete any unused job instance records from the persistence store.
	 *
	 * @return The number of affected records
	 */
	int deleteUnusedBatchJobInstances();
}
