/*
 * Copyright 2009-2023 the original author or authors.
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
package org.springframework.cloud.dataflow.server.batch;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.batch.core.step.tasklet.Tasklet;

/**
 * Interface for general purpose monitoring and management of Batch jobs. The features
 * here can generally be composed from existing Spring Batch interfaces (although for
 * performance reasons, implementations might choose special-purpose optimisations via a
 * relation database, for instance).
 * 
 * @author Dave Syer
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
public interface JobService {

	/**
	 * Launch a job with the parameters provided. If an instance with the parameters provided
	 * has already failed (and is not abandoned) it will be restarted.
	 * 
	 * @param jobName the job name
	 * @param params the {@link JobParameters}
	 * @return the resulting {@link JobExecution} if successful
	 * 
	 * @throws NoSuchJobException thrown if job specified does not exist
	 * @throws JobExecutionAlreadyRunningException thrown if job is already executing
	 * @throws JobRestartException thrown if job failed to restart
	 * @throws JobInstanceAlreadyCompleteException thrown if job was already complete
	 * @throws JobParametersInvalidException thrown if job parameters are invalid
	 */
	@Deprecated
	JobExecution launch(String jobName, JobParameters params) throws NoSuchJobException,
			JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException,
			JobParametersInvalidException;

	/**
	 * Get the last {@link JobParameters} used to execute a job successfully.
	 * 
	 * @param jobName the name of the job
	 * @return the last parameters used to execute this job or empty if there are none
	 * 
	 * @throws NoSuchJobException thrown if job specified does not exist
	 */
	@Deprecated
	JobParameters getLastJobParameters(String jobName) throws NoSuchJobException;

	/**
	 * Launch a job with the parameters provided.
	 * 
	 * @param jobExecutionId the job execution to restart
	 * @return the resulting {@link JobExecution} if successful
	 * 
	 * @throws NoSuchJobExecutionException thrown if job execution specified does not exist
	 * @throws NoSuchJobException thrown if job specified does not exist
	 * @throws JobExecutionAlreadyRunningException thrown if job is already executing
	 * @throws JobRestartException thrown if job failed to restart
	 * @throws JobInstanceAlreadyCompleteException thrown if job was already complete
	 * @throws JobParametersInvalidException thrown if job parameters are invalid
	 */
	@Deprecated
	JobExecution restart(Long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException, NoSuchJobException, JobParametersInvalidException;

	/**
	 * Launch a job with the parameters provided. JSR-352 supports restarting of jobs with a
	 * new set of parameters. This method exposes this functionality
	 *
	 * @param jobExecutionId the job execution to restart
	 * @param params the job parameters to use in the restart
	 * @return the resulting {@link JobExecution} if successful
	 *
	 * @throws NoSuchJobExecutionException thrown if job execution specified does not exist
	 * @throws NoSuchJobException thrown if job specified does not exist
	 * @throws JobExecutionAlreadyRunningException thrown if job is already executing
	 * @throws JobRestartException thrown if job failed to restart
	 * @throws JobInstanceAlreadyCompleteException thrown if job was already complete
	 * @throws JobParametersInvalidException thrown if job parameters are invalid
	 */
	@Deprecated
	JobExecution restart(Long jobExecutionId, JobParameters params)
			throws NoSuchJobExecutionException, JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException, NoSuchJobException, JobParametersInvalidException;

	/**
	 * Send a signal to a job execution to stop processing. This method does not guarantee
	 * that the processing will stop, only that the signal will be delivered. It is up to the
	 * individual {@link Job} and {@link Step} implementations to ensure that the signal is
	 * obeyed. In particular, if users provide a custom {@link Tasklet} to a {@link Step} it
	 * must check the signal in the {@link JobExecution} itself.
	 * 
	 * @param jobExecutionId the job execution id to stop
	 * @return the {@link JobExecution} that was stopped
	 * @throws NoSuchJobExecutionException thrown if job execution specified does not exist
	 * @throws JobExecutionNotRunningException thrown if the job execution specified is not
	 *     running
	 */
	JobExecution stop(Long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException;

	/**
	 * Mark the {@link JobExecution} as ABANDONED. If a stop signal is ignored because the
	 * process died this is the best way to mark a job as finished with (as opposed to
	 * STOPPED). An abandoned job execution can be restarted, but a stopping one cannot.
	 * 
	 * @param jobExecutionId the job execution id to abort
	 * @return the {@link JobExecution} that was aborted
	 * @throws NoSuchJobExecutionException thrown if job execution specified does not exist
	 * @throws JobExecutionAlreadyRunningException thrown if the job is running (it should be
	 *     stopped first)
	 */
	@Deprecated
	JobExecution abandon(Long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionAlreadyRunningException;

	/**
	 * Query the job names in the system, either launchable or not. If not launchable, then
	 * there must be a history of the job having been launched previously in the
	 * {@link JobRepository}.
	 * 
	 * @param start the start index of the job names to return
	 * @param count the maximum number of job names to return
	 * @return a collection of job names
	 */
	@Deprecated
	Collection<String> listJobs(int start, int count);

	/**
	 * Count the total number of jobs that can be returned by {@link #listJobs(int, int)}.
	 * 
	 * @return the total number of jobs
	 */
	@Deprecated
	int countJobs();

	/**
	 * Get a {@link JobInstance job instance} by id.
	 * 
	 * @param jobInstanceId the id of the instance
	 * @return a {@link JobInstance job instance}
	 * @throws NoSuchJobInstanceException thrown if the job instance specified does not exist
	 */
	JobInstance getJobInstance(long jobInstanceId) throws NoSuchJobInstanceException;

	/**
	 * List the {@link JobInstance job instances} in descending order of creation (usually
	 * close to order of execution).
	 * 
	 * @param jobName the name of the job
	 * @param start the index of the first to return
	 * @param count the maximum number of instances to return
	 * @return a collection of {@link JobInstance job instances}
	 * @throws NoSuchJobException thrown if job specified does not exist
	 */

	@Deprecated
	Collection<JobInstance> listJobInstances(String jobName, int start, int count) throws NoSuchJobException;

	/**
	 * Count the number of {@link JobInstance job instances} in the repository for a given job
	 * name.
	 * 
	 * @param jobName the name of the job
	 * @return the number of job instances available
	 * @throws NoSuchJobException thrown if job specified does not exist
	 */
	@Deprecated
	int countJobInstances(String jobName) throws NoSuchJobException;

	/**
	 * List the {@link JobExecutionWithStepCount job executions} for a job in descending order
	 * of creation (usually close to execution order).
	 *
	 * @param jobName the job name
	 * @param start the start index of the first job execution
	 * @param count the maximum number of executions to return
	 * @return a collection of {@link JobExecutionWithStepCount}
	 * @throws NoSuchJobException thrown if job specified does not exist
	 */
	@Deprecated
	Collection<JobExecutionWithStepCount> listJobExecutionsForJobWithStepCount(String jobName, int start, int count)
			throws NoSuchJobException;

	/**
	 * Count the job executions in the repository for a job.
	 * 
	 * @param jobName the job name
	 * @param status the status of the job
	 * @return the number of executions
	 * @throws NoSuchJobException thrown if job specified does not exist
	 */
	@Deprecated
	int countJobExecutionsForJob(String jobName, BatchStatus status) throws NoSuchJobException;

	/**
	 * Get all the job executions for a given job instance. On a sunny day there would be only
	 * one. If there have been failures and restarts there may be many, and they will be
	 * listed in reverse order of primary key.
	 * 
	 * @param jobName the name of the job
	 * @param jobInstanceId the id of the job instance
	 * @return all the job executions
	 * @throws NoSuchJobException thrown if job specified does not exist
	 */
	@Deprecated
	Collection<JobExecution> getJobExecutionsForJobInstance(String jobName, Long jobInstanceId)
			throws NoSuchJobException;

	/**
	 * List the {@link JobExecution job executions} in descending order of creation (usually
	 * close to execution order).
	 * 
	 * @param start the index of the first execution to return
	 * @param count the maximum number of executions
	 * @return a collection of {@link JobExecution}
	 */
	@Deprecated
	Collection<JobExecution> listJobExecutions(int start, int count);

	/**
	 * List the {@link JobExecutionWithStepCount JobExecutions} in descending order of
	 * creation (usually close to execution order) without step execution data.
	 *
	 * @param start the index of the first execution to return
	 * @param count the maximum number of executions
	 * @return a collection of {@link JobExecutionWithStepCount}
	 */
	@Deprecated
	Collection<JobExecutionWithStepCount> listJobExecutionsWithStepCount(int start, int count);

	/**
	 * Count the maximum number of executions that could be returned by
	 * {@link #listJobExecutions(int, int)}.
	 * 
	 * @return the number of job executions in the job repository
	 */
	@Deprecated
	int countJobExecutions();

	/**
	 * Get a {@link JobExecution} by id.
	 * 
	 * @param jobExecutionId the job execution id
	 * @return the {@link JobExecution}
	 * 
	 * @throws NoSuchJobExecutionException thrown if job execution specified does not exist
	 */
	JobExecution getJobExecution(Long jobExecutionId) throws NoSuchJobExecutionException;

	/**
	 * Get the {@link StepExecution step executions} for a given job execution (by id).
	 * 
	 * @param jobExecutionId the parent job execution id
	 * @return the step executions for the job execution
	 * 
	 * @throws NoSuchJobExecutionException thrown if job execution specified does not exist
	 */
	Collection<StepExecution> getStepExecutions(Long jobExecutionId) throws NoSuchJobExecutionException;
	Collection<StepExecution> getStepExecutions(JobExecution jobExecution) throws NoSuchJobExecutionException;
	void addStepExecutions(JobExecution jobExecution);
	/**
	 * List the {@link StepExecution step executions} for a step in descending order of
	 * creation (usually close to execution order).
	 * @param jobName the name of the job associated with the step (or a pattern with
	 *     wildcards)
	 * @param stepName the step name (or a pattern with wildcards)
	 * @param start the start index of the first execution
	 * @param count the maximum number of executions to return
	 * 
	 * @return a collection of {@link StepExecution}
	 * @throws NoSuchStepException thrown if step specified does not exist
	 */
	Collection<StepExecution> listStepExecutionsForStep(String jobName, String stepName, int start, int count)
			throws NoSuchStepException;

	/**
	 * Count the step executions in the repository for a given step name (or pattern).
	 * @param jobName the job name (or a pattern with wildcards)
	 * @param stepName the step name (or a pattern with wildcards)
	 * 
	 * @return the number of executions
	 * @throws NoSuchStepException thrown if step specified does not exist
	 */
	int countStepExecutionsForStep(String jobName, String stepName) throws NoSuchStepException;

	/**
	 * Count the step executions in the repository for a given job execution.
	 * @param jobExecutionId the id of the job execution.
	 *
	 * @return the number of executions.
	 */
	@Deprecated
	int countStepExecutionsForJobExecution(long jobExecutionId);

	/**
	 * Locate a {@link StepExecution} from its id and that of its parent {@link JobExecution}.
	 * 
	 * @param jobExecutionId the job execution id
	 * @param stepExecutionId the step execution id
	 * @return the {@link StepExecution}
	 * 
	 * @throws NoSuchStepExecutionException thrown if the step execution specified does not
	 *     exist
	 * @throws NoSuchJobExecutionException thrown if job execution specified does not exist
	 */
	StepExecution getStepExecution(Long jobExecutionId, Long stepExecutionId) throws NoSuchStepExecutionException,
			NoSuchJobExecutionException;
	StepExecution getStepExecution(JobExecution jobExecution, Long stepExecutionId) throws NoSuchStepExecutionException;

	/**
	 * Send a stop signal to all running job executions.
	 * 
	 * @return the number of executions affected
	 */
	@Deprecated
	int stopAll();

	/**
	 * Get the names of the steps in a job (or a historical list of recent execution names if
	 * the Job is not launchable).
	 * 
	 * @param jobName the name of the job
	 * @return {@link Collection} of step names.
	 * @throws NoSuchJobException thrown if the job name cannot be located
	 */
	@Deprecated
	Collection<String> getStepNamesForJob(String jobName) throws NoSuchJobException;

	/**
	 * List the {@link JobExecution job executions} for a job in descending order of creation
	 * (usually close to execution order).
	 *
	 * @param jobName the job name
	 * @param status the status of the job execution
	 * @param pageOffset the start index of the first job execution
	 * @param pageSize the maximum number of executions to return
	 * @return a collection of {@link JobExecution}
	 * @throws NoSuchJobException thrown if job specified does not exist
	 */
	Collection<JobExecution> listJobExecutionsForJob(String jobName, BatchStatus status, int pageOffset, int pageSize)
			throws NoSuchJobException;

	/**
	 * List the {@link JobExecutionWithStepCount job executions} filtered by date range in
	 * descending order of creation (usually close to execution order).
	 *
	 * @param fromDate the date which start date must be greater than.
	 * @param toDate   the date which start date must be less than.
	 * @param start    the start index of the first job execution
	 * @param count    the maximum number of executions to return
	 * @return a collection of {@link JobExecutionWithStepCount}
	 */
	@Deprecated
	Collection<JobExecutionWithStepCount> listJobExecutionsForJobWithStepCount(Date fromDate,
			Date toDate, int start, int count);

	/**
	 * List the {@link JobExecutionWithStepCount job executions} filtered by job instance id in
	 * descending order of creation (usually close to execution order).
	 *
	 * @param jobInstanceId the job instance id associated with the execution.
	 * @param start    the start index of the first job execution
	 * @param count    the maximum number of executions to return
	 * @return a collection of {@link JobExecutionWithStepCount}
	 */
	@Deprecated
	Collection<JobExecutionWithStepCount> listJobExecutionsForJobWithStepCountFilteredByJobInstanceId(int jobInstanceId, int start, int count);

	/**
	 * List the {@link JobExecutionWithStepCount job executions} filtered by task execution id in
	 * descending order of creation (usually close to execution order).
	 *
	 * @param taskExecutionId the task execution id associated with the execution.
	 * @param start    the start index of the first job execution
	 * @param count    the maximum number of executions to return
	 * @return a collection of {@link JobExecutionWithStepCount}
	 */
	@Deprecated
	Collection<JobExecutionWithStepCount> listJobExecutionsForJobWithStepCountFilteredByTaskExecutionId(int taskExecutionId, int start, int count);

	/**
	 * Returns a collection job execution ids given a collection of task execution ids that is mapped by id.
	 * @param taskExecutionId
	 * @return
	 */
	Map<Long, Set<Long>> getJobExecutionIdsByTaskExecutionIds(Collection<Long> taskExecutionId);
}
