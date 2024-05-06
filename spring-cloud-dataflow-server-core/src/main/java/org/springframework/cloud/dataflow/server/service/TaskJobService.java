/*
 * Copyright 2016 the original author or authors.
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.cloud.dataflow.rest.job.JobInstanceExecutions;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.server.batch.JobExecutionWithStepCount;
import org.springframework.cloud.dataflow.server.job.support.JobNotRestartableException;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repository that retrieves Tasks and JobExecutions/Instances and the associations
 * between them.
 *
 * @author Glenn Renfro.
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
public interface TaskJobService {

	/**
	 * Retrieves Pageable list of {@link JobExecution}s from the JobRepository and matches the
	 * data with a task id.
	 *
	 * @param pageable enumerates the data to be returned.
	 * @return List containing {@link TaskJobExecution}s.
	 * @throws NoSuchJobExecutionException in the event that a job execution id specified is
	 *                                     not present when looking up stepExecutions for the result.
	 */
	Page<TaskJobExecution> listJobExecutions(Pageable pageable) throws NoSuchJobExecutionException;

	/**
	 * Retrieves Pageable list of {@link JobExecutionWithStepCount} from the JobRepository
	 * with a specific jobName and matches the data with a task id.
	 *
	 * @param pageable enumerates the data to be returned.
	 * @param jobName  the name of the job for which to findByTaskNameContains.
	 * @return List containing {@link JobExecutionWithStepCount}s.
	 * @throws NoSuchJobException if the job with the given name does not exist.
	 */
	Page<TaskJobExecution> listJobExecutionsForJobWithStepCount(Pageable pageable, String jobName) throws NoSuchJobException;

	/**
	 * Retrieves a JobExecution from the JobRepository and matches it with a task id.
	 *
	 * @param id           the id of the {@link JobExecution}
	 * @param schemaTarget the schema target of the task job execution.
	 * @return the {@link TaskJobExecution}s associated with the id.
	 * @throws NoSuchJobExecutionException if the specified job execution for the id does not
	 *                                     exist.
	 */
	TaskJobExecution getJobExecution(long id, String schemaTarget) throws NoSuchJobExecutionException;

	/**
	 * Retrieves Pageable list of {@link JobInstanceExecutions} from the JobRepository with a
	 * specific jobName and matches the data with the associated JobExecutions.
	 *
	 * @param pageable enumerates the data to be returned.
	 * @param jobName  the name of the job for which to findByTaskNameContains.
	 * @return List containing {@link JobInstanceExecutions}.
	 * @throws NoSuchJobException if the job for the jobName specified does not exist.
	 */
	Page<JobInstanceExecutions> listTaskJobInstancesForJobName(Pageable pageable, String jobName) throws NoSuchJobException;

	/**
	 * Retrieves a {@link JobInstance} from the JobRepository and matches it with the
	 * associated {@link JobExecution}s.
	 *
	 * @param id           the id of the {@link JobInstance}
	 * @param schemaTarget the schema target of the job instance.
	 * @return the {@link JobInstanceExecutions} associated with the id.
	 * @throws NoSuchJobInstanceException if job instance id does not exist.
	 * @throws NoSuchJobException         if the job for the job instance does not exist.
	 */
	JobInstanceExecutions getJobInstance(long id, String schemaTarget) throws NoSuchJobInstanceException, NoSuchJobException;

	/**
	 * Restarts a {@link JobExecution} IF the respective {@link JobExecution} is actually
	 * deemed restartable. Otherwise a {@link JobNotRestartableException} is being thrown.
	 *
	 * @param jobExecutionId The id of the JobExecution to restart.
	 * @param schemaTarget   the schema target of the job execution.
	 * @throws NoSuchJobExecutionException if the JobExecution for the provided id does not
	 *                                     exist.
	 */
	void restartJobExecution(long jobExecutionId, String schemaTarget) throws NoSuchJobExecutionException;

	/**
	 * Requests a {@link JobExecution} to stop.
	 * <p>
	 * Please remember, that calling this method only requests a job execution to stop
	 * processing. This method does not guarantee a {@link JobExecution} to stop. It is the
	 * responsibility of the implementor of the {@link Job} to react to that request.
	 * Furthermore, this method does not interfere with the associated {@link TaskExecution}.
	 *
	 * @param jobExecutionId The id of the {@link JobExecution} to stop.
	 * @param schemaTarget   the schema target of the job execution.
	 * @throws NoSuchJobExecutionException     thrown if no job execution exists for the
	 *                                         jobExecutionId.
	 * @throws JobExecutionNotRunningException thrown if a stop is requested on a job that is
	 *                                         not running.
	 * @see org.springframework.cloud.dataflow.server.batch.JobService#stop(Long)
	 */
	void stopJobExecution(long jobExecutionId, String schemaTarget) throws NoSuchJobExecutionException, JobExecutionNotRunningException;

	/**
	 * Retrieves Pageable list of {@link JobExecutionWithStepCount}s from the JobRepository
	 * and matches the data with a task id but excludes the step executions.
	 *
	 * @param pageable enumerates the data to be returned.
	 * @return List containing {@link TaskJobExecution}s.
	 * @throws NoSuchJobExecutionException thrown if the job execution specified does not
	 *                                     exist.
	 */
	Page<TaskJobExecution> listJobExecutionsWithStepCount(Pageable pageable) throws NoSuchJobExecutionException;

	/**
	 * Retrieves Pageable list of {@link JobExecution} from the JobRepository with a specific
	 * jobName, status and matches the data with a task id.
	 *
	 * @param pageable enumerates the data to be returned.
	 * @param jobName  the name of the job for which to findByTaskNameContains.
	 * @param status   the BatchStatus of the job execution.
	 * @return List containing {@link TaskJobExecution}s.
	 * @throws NoSuchJobException          if the job with the given name does not exist.
	 * @throws NoSuchJobExecutionException the job execution with the given name doesn't exist.
	 */
	Page<TaskJobExecution> listJobExecutionsForJob(
		Pageable pageable,
		String jobName,
		BatchStatus status
	) throws NoSuchJobException, NoSuchJobExecutionException;

	/**
	 * Retrieves Pageable list of {@link JobExecutionWithStepCount} from the JobRepository
	 * filtered by the date range.
	 *
	 * @param pageable enumerates the data to be returned.
	 * @param fromDate the date which start date must be greater than.
	 * @param toDate   the date which start date must be less than.
	 * @return List containing {@link JobExecutionWithStepCount}s.
	 * @throws NoSuchJobException if the job with the given name does not exist.
	 */
	Page<TaskJobExecution> listJobExecutionsForJobWithStepCount(Pageable pageable, Date fromDate, Date toDate) throws NoSuchJobException;

	/**
	 * Retrieves Pageable list of {@link JobExecutionWithStepCount} from the JobRepository
	 * filtered by the job instance id.
	 *
	 * @param pageable      enumerates the data to be returned.
	 * @param jobInstanceId the job instance id associated with the execution.
	 * @param schemaTarget  the schema target of the job instance.
	 * @return List containing {@link JobExecutionWithStepCount}s.
	 * @throws NoSuchJobException if the job with the given name does not exist.
	 */
	Page<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByJobInstanceId(
		Pageable pageable,
		int jobInstanceId,
		String schemaTarget
	) throws NoSuchJobException;

	/**
	 * Retrieves Pageable list of {@link JobExecutionWithStepCount} from the JobRepository
	 * filtered by the task execution id.
	 *
	 * @param pageable        enumerates the data to be returned.
	 * @param taskExecutionId the task execution id associated with the execution.
	 * @param schemaTarget    the schema target of the task execution.
	 * @return List containing {@link JobExecutionWithStepCount}s.
	 * @throws NoSuchJobException if the job with the given name does not exist.
	 */
	Page<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByTaskExecutionId(
		Pageable pageable,
		int taskExecutionId,
		String schemaTarget
	) throws NoSuchJobException;

	Map<Long, Set<Long>> getJobExecutionIdsByTaskExecutionIds(Collection<Long> taskExecutionIds, String schemaTarget);

	void populateComposeTaskRunnerStatus(Collection<AggregateTaskExecution> taskExecutions);

}
