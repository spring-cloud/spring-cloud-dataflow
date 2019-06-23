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
	 *
	 * @return
	 */
	int deleteUnusedBatchJobInstances();

	/**
	 *
	 * @param jobInstanceIds
	 * @return
	 */
	int deleteBatchJobExecutionByJobInstanceIds(Set<Long> jobInstanceIds);

	/**
	 *
	 * @param jobExecutionIds
	 * @return
	 */
	int deleteBatchJobExecutionByJobExecitionIds(Set<Long> jobExecutionIds);

	/**
	 *
	 * @param jobExecutionIds
	 * @return
	 */
	int deleteBatchJobExecutionParamsByJobExecitionIds(Set<Long> jobExecutionIds);

	/**
	 *
	 * @param jobExecutionIds
	 * @return
	 */
	int deleteBatchJobExecutionContextByJobExecitionIds(Set<Long> jobExecutionIds);

	/**
	 *
	 * @param jobExecutionIds
	 * @return
	 */
	int deleteBatchStepExecutionsByJobExecitionIds(Set<Long> jobExecutionIds);

	/**
	 *
	 * @param jobExecutionIds
	 * @return
	 */
	int deleteBatchStepExecutionContextByJobExecitionIds(Set<Long> jobExecutionIds);
}
