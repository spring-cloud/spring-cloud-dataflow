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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.rest.job.JobInstanceExecutions;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.support.JobUtils;
import org.springframework.cloud.dataflow.server.batch.JobExecutionWithStepCount;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.springframework.cloud.dataflow.server.job.support.JobNotRestartableException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskBatchException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
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
@Transactional
public class DefaultTaskJobService implements TaskJobService {

	private static final Logger logger = LoggerFactory.getLogger(DefaultTaskJobService.class);

	private final TaskExecutionService taskExecutionService;

	private TaskExplorer taskExplorer;

	private JobService jobService;

	private TaskDefinitionRepository taskDefinitionRepository;

	public DefaultTaskJobService(JobService jobService, TaskExplorer taskExplorer,
			TaskDefinitionRepository taskDefinitionRepository, TaskExecutionService taskExecutionService) {
		Assert.notNull(jobService, "jobService must not be null");
		Assert.notNull(taskExplorer, "taskExplorer must not be null");
		Assert.notNull(taskDefinitionRepository, "taskDefinitionRepository must not be null");
		Assert.notNull(taskExecutionService, "taskExecutionService must not be null");
		this.jobService = jobService;
		this.taskExplorer = taskExplorer;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskExecutionService = taskExecutionService;
	}

	@Override
	public List<TaskJobExecution> listJobExecutions(Pageable pageable) throws NoSuchJobExecutionException {
		Assert.notNull(pageable, "pageable must not be null");
		List<JobExecution> jobExecutions = new ArrayList<>(
				jobService.listJobExecutions(getPageOffset(pageable), pageable.getPageSize()));
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

	@Override
	public List<TaskJobExecution> listJobExecutionsWithStepCount(Pageable pageable) {
		Assert.notNull(pageable, "pageable must not be null");
		List<JobExecutionWithStepCount> jobExecutions = new ArrayList<>(
				jobService.listJobExecutionsWithStepCount(getPageOffset(pageable), pageable.getPageSize()));
		return getTaskJobExecutionsWithStepCountForList(jobExecutions);
	}

	@Override
	public List<TaskJobExecution> listJobExecutionsForJob(Pageable pageable, String jobName, BatchStatus status) throws NoSuchJobException {
		Assert.notNull(pageable, "pageable must not be null");
		return getTaskJobExecutionsForList(
				jobService.listJobExecutionsForJob(jobName, status, getPageOffset(pageable), pageable.getPageSize()));
	}

	@Override
	public List<TaskJobExecution> listJobExecutionsForJobWithStepCount(Pageable pageable,
			Date fromDate, Date toDate) {
		Assert.notNull(pageable, "pageable must not be null");
		return getTaskJobExecutionsWithStepCountForList(
				jobService.listJobExecutionsForJobWithStepCount(fromDate, toDate, getPageOffset(pageable),
						pageable.getPageSize()));
	}

	@Override
	public List<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByJobInstanceId(
			Pageable pageable, int jobInstanceId) throws NoSuchJobException {
		Assert.notNull(pageable, "pageable must not be null");
		return getTaskJobExecutionsWithStepCountForList(
				jobService.listJobExecutionsForJobWithStepCountFilteredByJobInstanceId(jobInstanceId, getPageOffset(pageable),
						pageable.getPageSize()));
	}

	@Override
	public List<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByTaskExecutionId(
			Pageable pageable, int taskExecutionId) throws NoSuchJobException {
		Assert.notNull(pageable, "pageable must not be null");
		return getTaskJobExecutionsWithStepCountForList(
				jobService.listJobExecutionsForJobWithStepCountFilteredByTaskExecutionId(taskExecutionId, getPageOffset(pageable),
						pageable.getPageSize()));
	}

	@Override
	public List<TaskJobExecution> listJobExecutionsForJobWithStepCount(Pageable pageable, String jobName) throws NoSuchJobException {
		Assert.notNull(pageable, "pageable must not be null");
		return getTaskJobExecutionsWithStepCountForList(
				jobService.listJobExecutionsForJobWithStepCount(jobName, getPageOffset(pageable), pageable.getPageSize()));
	}

	@Override
	public TaskJobExecution getJobExecution(long id) throws NoSuchJobExecutionException {
		JobExecution jobExecution = jobService.getJobExecution(id);
		return getTaskJobExecution(jobExecution);
	}

	@Override
	public List<JobInstanceExecutions> listTaskJobInstancesForJobName(Pageable pageable, String jobName)
			throws NoSuchJobException {
		Assert.notNull(pageable, "pageable must not be null");
		Assert.notNull(jobName, "jobName must not be null");
		List<JobInstanceExecutions> taskJobInstances = new ArrayList<>();
		for (JobInstance jobInstance : jobService.listJobInstances(jobName, getPageOffset(pageable),
				pageable.getPageSize())) {
			taskJobInstances.add(getJobInstanceExecution(jobInstance));
		}
		return taskJobInstances;
	}

	@Override
	public JobInstanceExecutions getJobInstance(long id) throws NoSuchJobInstanceException, NoSuchJobException {
		return getJobInstanceExecution(jobService.getJobInstance(id));
	}

	@Override
	public int countJobInstances(String jobName) throws NoSuchJobException {
		Assert.notNull(jobName, "jobName must not be null");
		return jobService.countJobInstances(jobName);
	}

	@Override
	public int countJobExecutions() {
		return jobService.countJobExecutions();
	}

	@Override
	public int countJobExecutionsForJob(String jobName, BatchStatus status) throws NoSuchJobException {
		return jobService.countJobExecutionsForJob(jobName, status);
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
		TaskManifest taskManifest = this.taskExecutionService.findTaskManifestById(taskExecution.getExecutionId());
		TaskDefinition taskDefinition = this.taskDefinitionRepository.findById(taskExecution.getTaskName())
				.orElseThrow(() -> new NoSuchTaskDefinitionException(taskExecution.getTaskName()));
		String platformName = taskManifest.getPlatformName();
		if (platformName != null) {
			Map<String, String> deploymentProperties = new HashMap<>();
			deploymentProperties.put(DefaultTaskExecutionService.TASK_PLATFORM_NAME, platformName);
			taskExecutionService.executeTask(taskDefinition.getName(), deploymentProperties,
					restartExecutionArgs(taskExecution.getArguments(),
							taskJobExecution.getJobExecution().getJobParameters()));
		} else {
			throw new IllegalStateException(String.format("Did not find platform for taskName=[%s] , taskId=[%s]",
					taskExecution.getTaskName(),taskJobExecution.getTaskId()));
		}

	}

	/**
	 * Apply identifying job parameters to arguments.  There are cases (incrementers)
	 * that add parameters to a job and thus must be added for each restart so that the
	 * JobInstanceId does not change.
	 * @param taskExecutionArgs original set of task execution arguments
	 * @param jobParameters for the job to be restarted.
	 * @return deduped list of arguments that contains the original arguments and any
	 * identifying job parameters not in the original task execution arguments.
	 */
	private List<String>restartExecutionArgs(List<String> taskExecutionArgs, JobParameters jobParameters) {
		List<String> result = new ArrayList<>(taskExecutionArgs);
		Map<String, JobParameter> jobParametersMap = jobParameters.getParameters();
		for (String key : jobParametersMap.keySet()) {
			if (!key.startsWith("-")) {
				boolean existsFlag = false;
				for(String arg : taskExecutionArgs) {
					if(arg.startsWith(key)) {
						existsFlag = true;
						break;
					}
				}
				if(!existsFlag) {
					result.add(String.format("%s(%s)=%s", key,
							jobParametersMap.get(key).getType().toString().toLowerCase(),
							jobParameters.getString(key)));
				}
			}
		}
		return result;
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
		return new TaskJobExecution(getTaskExecutionId(jobExecution), jobExecution,
				isTaskDefined(jobExecution), jobExecution.getStepExecutions().size());
	}

	private List<TaskJobExecution> getTaskJobExecutionsWithStepCountForList(Collection<JobExecutionWithStepCount> jobExecutions) {
		Assert.notNull(jobExecutions, "jobExecutions must not be null");
		List<TaskJobExecution> taskJobExecutions = new ArrayList<>();
		for (JobExecutionWithStepCount jobExecution : jobExecutions) {
			taskJobExecutions.add(getTaskJobExecutionWithStepCount(jobExecution));
		}
		return taskJobExecutions;
	}

	private TaskJobExecution getTaskJobExecutionWithStepCount(JobExecutionWithStepCount jobExecutionWithStepCount) {
		return new TaskJobExecution(getTaskExecutionId(jobExecutionWithStepCount), jobExecutionWithStepCount,
				isTaskDefined(jobExecutionWithStepCount), jobExecutionWithStepCount.getStepCount());
	}

	private Long getTaskExecutionId(JobExecution jobExecution) {
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
		return taskExecutionId;
	}


	private int getPageOffset(Pageable pageable) {
		if(pageable.getOffset() > (long)Integer.MAX_VALUE) {
			throw new OffsetOutOfBoundsException("The pageable offset requested for this query is greater than MAX_INT.") ;
		}
		return (int)pageable.getOffset();
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
		return taskDefinitionRepository.findById(taskExecution.getTaskName()).isPresent();
	}
}
