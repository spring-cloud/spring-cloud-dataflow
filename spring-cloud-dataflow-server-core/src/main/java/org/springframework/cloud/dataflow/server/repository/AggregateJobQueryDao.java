/*
 * Copyright 2023 the original author or authors.
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


import java.util.Collection;
import java.util.Date;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.cloud.dataflow.rest.job.JobInstanceExecutions;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Provides for reading job execution data for Batch 4 and 5 schema versions.
 *
 * @author Corneil du Plessis
 * @since 2.11.0
 */
public interface AggregateJobQueryDao {
	Page<JobInstanceExecutions> listJobInstances(String jobName, Pageable pageable) throws NoSuchJobException;

	Page<TaskJobExecution> listJobExecutions(String jobName, BatchStatus status, Pageable pageable) throws NoSuchJobExecutionException;

	Page<TaskJobExecution> listJobExecutionsBetween(Date fromDate, Date toDate, Pageable pageable);

	Page<TaskJobExecution> listJobExecutionsWithSteps(Pageable pageable);

	Page<TaskJobExecution> listJobExecutionsWithStepCount(Pageable pageable);

	Page<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByJobInstanceId(int jobInstanceId, String schemaTarget, Pageable pageable);

	Page<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByTaskExecutionId(int taskExecutionId, String schemaTarget, Pageable pageable);

	Page<TaskJobExecution> listJobExecutionsForJobWithStepCount(String jobName, Pageable pageable) throws NoSuchJobException;

	TaskJobExecution getJobExecution(long id, String schemaTarget) throws NoSuchJobExecutionException;

	JobInstanceExecutions getJobInstanceExecution(String jobName, long instanceId);

	JobInstanceExecutions getJobInstanceExecutions(long id, String schemaTarget);

	JobInstance getJobInstance(long id, String schemaTarget) throws NoSuchJobInstanceException;

	void populateCtrStatus(Collection<AggregateTaskExecution> aggregateTaskExecutions);

}
