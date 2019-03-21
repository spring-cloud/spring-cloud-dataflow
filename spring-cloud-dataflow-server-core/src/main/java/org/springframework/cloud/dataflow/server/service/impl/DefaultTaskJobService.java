/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.rest.job.JobInstanceExecutions;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.support.JobUtils;
import org.springframework.cloud.dataflow.server.job.support.JobNotRestartableException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskBatchException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * Repository that retrieves Tasks and JobExecutions/Instances and the associations
 * between them.
 *
 * @author Glenn Renfro.
 * @author Gunnar Hillert
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class DefaultTaskJobService implements TaskJobService {

	private static final Logger logger = LoggerFactory.getLogger(DefaultTaskJobService.class);

	private final TaskService taskService;

	private TaskExplorer taskExplorer;

	private JobService jobService;

	private TaskDefinitionRepository taskDefinitionRepository;

	public DefaultTaskJobService(JobService jobService, TaskExplorer taskExplorer,
			TaskDefinitionRepository taskDefinitionRepository, TaskService taskService) {
		Assert.notNull(jobService, "jobService must not be null");
		Assert.notNull(taskExplorer, "taskExplorer must not be null");
		Assert.notNull(taskDefinitionRepository, "taskDefinitionRepository must not be null");
		Assert.notNull(taskService, "taskService must not be null");
		this.jobService = jobService;
		this.taskExplorer = taskExplorer;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskService = taskService;
	}

	/**
	 * Retrieves Pageable list of {@link JobExecution}s from the JobRepository and matches the
	 * data with a task id.
	 *
	 * @param pageable enumerates the data to be returned.
	 * @return List containing {@link TaskJobExecution}s.
	 */
	@Override
	public List<TaskJobExecution> listJobExecutions(Pageable pageable) throws NoSuchJobExecutionException {
		Assert.notNull(pageable, "pageable must not be null");
		List<JobExecution> jobExecutions = new ArrayList<>(
				jobService.listJobExecutions(pageable.getOffset(), pageable.getPageSize()));
		for (JobExecution jobExecution : jobExecutions) {
			Collection<StepExecution> stepExecutions = jobService.getStepExecutions(jobExecution.getId());
			List<StepExecution> validStepExecutions = new ArrayList<>();
			for (StepExecution stepExecution : stepExecutions) {
				if (stepExecution.getId() != null) {
					validStepExecutions.add(stepExecution);
				}
			}
			jobExecution.addStepExecutions(validStepExecutions);
		}
		return getTaskJobExecutionsForList(jobExecutions);
	}

	/**
	 * Retrieves Pageable list of {@link JobExecution} from the JobRepository with a specific
	 * jobName and matches the data with a task id.
	 *
	 * @param pageable enumerates the data to be returned.
	 * @param jobName the name of the job for which to search.
	 * @return List containing {@link TaskJobExecution}s.
	 */
	@Override
	public List<TaskJobExecution> listJobExecutionsForJob(Pageable pageable, String jobName) throws NoSuchJobException {
		Assert.notNull(pageable, "pageable must not be null");
		Assert.notNull(jobName, "jobName must not be null");
		return getTaskJobExecutionsForList(
				jobService.listJobExecutionsForJob(jobName, pageable.getOffset(), pageable.getPageSize()));
	}

	/**
	 * Retrieves a JobExecution from the JobRepository and matches it with a task id.
	 *
	 * @param id the id of the {@link JobExecution}
	 * @return the {@link TaskJobExecution}s associated with the id.
	 */
	@Override
	public TaskJobExecution getJobExecution(long id) throws NoSuchJobExecutionException {
		JobExecution jobExecution = jobService.getJobExecution(id);
		return getTaskJobExecution(jobExecution);
	}

	/**
	 * Retrieves Pageable list of {@link JobInstanceExecutions} from the JobRepository with a
	 * specific jobName and matches the data with the associated JobExecutions.
	 *
	 * @param pageable enumerates the data to be returned.
	 * @param jobName the name of the job for which to search.
	 * @return List containing {@link JobInstanceExecutions}.
	 */
	@Override
	public List<JobInstanceExecutions> listTaskJobInstancesForJobName(Pageable pageable, String jobName)
			throws NoSuchJobException {
		Assert.notNull(pageable, "pageable must not be null");
		Assert.notNull(jobName, "jobName must not be null");
		List<JobInstanceExecutions> taskJobInstances = new ArrayList<>();
		for (JobInstance jobInstance : jobService.listJobInstances(jobName, pageable.getOffset(),
				pageable.getPageSize())) {
			taskJobInstances.add(getJobInstanceExecution(jobInstance));
		}
		return taskJobInstances;
	}

	/**
	 * Retrieves a {@link JobInstance} from the JobRepository and matches it with the
	 * associated {@link JobExecution}s.
	 *
	 * @param id the id of the {@link JobInstance}
	 * @return the {@link JobInstanceExecutions} associated with the id.
	 */
	@Override
	public JobInstanceExecutions getJobInstance(long id) throws NoSuchJobInstanceException, NoSuchJobException {
		return getJobInstanceExecution(jobService.getJobInstance(id));
	}

	/**
	 * Retrieves the total number of job instances for a job name.
	 *
	 * @param jobName the name of the job instance.
	 */
	@Override
	public int countJobInstances(String jobName) throws NoSuchJobException {
		Assert.notNull(jobName, "jobName must not be null");
		return jobService.countJobInstances(jobName);
	}

	/**
	 * Retrieves the total number of the job executions.
	 */
	@Override
	public int countJobExecutions() {
		return jobService.countJobExecutions();
	}

	/**
	 * Retrieves the total number {@link JobExecution} that match a specific job name.
	 *
	 * @param jobName the job name to search.
	 * @return the number of {@link JobExecution}s that match the job name.
	 * @throws NoSuchJobException if the job with the given name is not available
	 */
	@Override
	public int countJobExecutionsForJob(String jobName) throws NoSuchJobException {
		Assert.notNull(jobName, "jobName must not be null");
		return jobService.countJobExecutionsForJob(jobName);
	}

	@Override
	public void restartJobExecution(long jobExecutionId) throws NoSuchJobExecutionException {
		logger.info("Restarting Job with Id " + jobExecutionId);

		final TaskJobExecution taskJobExecution = this.getJobExecution(jobExecutionId);
		final JobExecution jobExecution = taskJobExecution.getJobExecution();

		if (!JobUtils.isJobExecutionRestartable(taskJobExecution.getJobExecution())) {
			throw new JobNotRestartableException(
					String.format("JobExecution with Id '%s' and state '%s' is not " + "restartable.",
							jobExecution.getId(), taskJobExecution.getJobExecution().getStatus()));
		}

		TaskExecution taskExecution = this.taskExplorer.getTaskExecution(taskJobExecution.getTaskId());

		TaskDefinition taskDefinition = this.taskDefinitionRepository.findOne(taskExecution.getTaskName());

		if (taskDefinition == null) {
			throw new NoSuchTaskDefinitionException(taskExecution.getTaskName());
		}

		taskService.executeTask(taskDefinition.getName(), taskDefinition.getProperties(), taskExecution.getArguments());
	}

	@Override
	public void stopJobExecution(long jobExecutionId)
			throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		this.jobService.stop(jobExecutionId).getStatus();
	}

	private List<TaskJobExecution> getTaskJobExecutionsForList(Collection<JobExecution> jobExecutions) {
		Assert.notNull(jobExecutions, "jobExecutions must not be null");
		List<TaskJobExecution> taskJobExecutions = new ArrayList<>();
		for (JobExecution jobExecution : jobExecutions) {
			taskJobExecutions.add(getTaskJobExecution(jobExecution));
		}
		return taskJobExecutions;
	}

	private TaskJobExecution getTaskJobExecution(JobExecution jobExecution) {
		Assert.notNull(jobExecution, "jobExecution must not be null");
		Long taskExecutionId = taskExplorer.getTaskExecutionIdByJobExecutionId(
				jobExecution.getId());
		if(taskExecutionId == null) {
			String message = String.format("No corresponding taskExecutionId " +
					"for jobExecutionId %s.  This indicates that Spring " +
					"Batch application has been executed that is not a Spring " +
					"Cloud Task.", jobExecution.getId());
			logger.warn(message);
			throw new NoSuchTaskBatchException(message);
		}
		return new TaskJobExecution(taskExecutionId, jobExecution,
				isTaskDefined(jobExecution));
	}

	private JobInstanceExecutions getJobInstanceExecution(JobInstance jobInstance) throws NoSuchJobException {
		Assert.notNull(jobInstance, "jobInstance must not be null");
		List<JobExecution> jobExecutions = new ArrayList<>(
				jobService.getJobExecutionsForJobInstance(jobInstance.getJobName(), jobInstance.getInstanceId()));
		return new JobInstanceExecutions(jobInstance, getTaskJobExecutionsForList(jobExecutions));
	}

	private boolean isTaskDefined(JobExecution jobExecution) {
		TaskExecution taskExecution = taskExplorer
				.getTaskExecution(taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecution.getId()));
		TaskDefinition definition = taskDefinitionRepository.findOne(taskExecution.getTaskName());
		return (definition != null);
	}
}
