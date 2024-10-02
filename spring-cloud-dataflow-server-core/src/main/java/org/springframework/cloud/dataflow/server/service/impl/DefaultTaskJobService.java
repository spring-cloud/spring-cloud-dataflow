/*
 * Copyright 2016-2023 the original author or authors.
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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.rest.job.JobInstanceExecutions;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.support.JobUtils;
import org.springframework.cloud.dataflow.server.batch.JobExecutionWithStepCount;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.job.support.JobNotRestartableException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskBatchException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorer;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
 * @author Corneil du Plessis
 */
@Transactional
public class DefaultTaskJobService implements TaskJobService {

	private static final Logger logger = LoggerFactory.getLogger(DefaultTaskJobService.class);

	private final TaskExecutionService taskExecutionService;

	private final DataflowTaskExplorer taskExplorer;

	private final JobService jobService;

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final LauncherRepository launcherRepository;

	private final TaskConfigurationProperties taskConfigurationProperties;


	public DefaultTaskJobService(
		JobService jobService,
		DataflowTaskExplorer taskExplorer,
		TaskDefinitionRepository taskDefinitionRepository,
		TaskExecutionService taskExecutionService,
		LauncherRepository launcherRepository,
		TaskConfigurationProperties taskConfigurationProperties) {
		Assert.notNull(jobService, "jobService must not be null");
		Assert.notNull(taskExplorer, "taskExplorer must not be null");
		Assert.notNull(taskDefinitionRepository, "taskDefinitionRepository must not be null");
		Assert.notNull(taskExecutionService, "taskExecutionService must not be null");
		Assert.notNull(launcherRepository, "launcherRepository must not be null");
		this.jobService = jobService;
		this.taskExplorer = taskExplorer;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskExecutionService = taskExecutionService;
		this.launcherRepository = launcherRepository;
		this.taskConfigurationProperties = taskConfigurationProperties;
	}

	@Override
	public Page<TaskJobExecution> listJobExecutions(Pageable pageable) throws NoSuchJobExecutionException {
		Assert.notNull(pageable, "pageable must not be null");
		long total = jobService.countJobExecutions();
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
		return new PageImpl<>(getTaskJobExecutionsForList(jobExecutions), pageable, total);
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsWithStepCount(Pageable pageable) {
		Assert.notNull(pageable, "pageable must not be null");
		List<JobExecutionWithStepCount> jobExecutions = new ArrayList<>(
			jobService.listJobExecutionsWithStepCount(getPageOffset(pageable), pageable.getPageSize()));
		List<TaskJobExecution> taskJobExecutions = getTaskJobExecutionsWithStepCountForList(jobExecutions);
		return new PageImpl<>(taskJobExecutions, pageable, jobService.countJobExecutions());
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsForJob(Pageable pageable, String jobName, BatchStatus status) throws NoSuchJobException, NoSuchJobExecutionException {
		Assert.notNull(pageable, "pageable must not be null");
		long total = jobService.countJobExecutionsForJob(jobName, status);
		List<TaskJobExecution> taskJobExecutions = getTaskJobExecutionsForList(
			jobService.listJobExecutionsForJob(jobName, status, getPageOffset(pageable), pageable.getPageSize()));
		return new PageImpl<>(taskJobExecutions, pageable, total);
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsForJobWithStepCount(Pageable pageable, Date fromDate, Date toDate) {
		Assert.notNull(pageable, "pageable must not be null");

		List<TaskJobExecution> taskJobExecutions =  getTaskJobExecutionsWithStepCountForList(
			jobService.listJobExecutionsForJobWithStepCount(fromDate, toDate, getPageOffset(pageable),
				pageable.getPageSize()));
		return new PageImpl<>(taskJobExecutions, pageable, taskJobExecutions.size());
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByJobInstanceId(
			Pageable pageable,
			int jobInstanceId) {
		Assert.notNull(pageable, "pageable must not be null");
		List <TaskJobExecution> jobExecutions =  getTaskJobExecutionsWithStepCountForList(
			jobService.listJobExecutionsForJobWithStepCountFilteredByJobInstanceId(jobInstanceId, getPageOffset(pageable),
				pageable.getPageSize()));
		long total = 0;
        try {
            JobInstance jobInstance = jobService.getJobInstance(jobInstanceId);
			total = jobService.getJobExecutionsForJobInstance(jobInstance.getJobName(), jobInstance.getInstanceId()).size();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return new PageImpl<>(jobExecutions, pageable, total);
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByTaskExecutionId(
			Pageable pageable,
			int taskExecutionId
	) {
		Assert.notNull(pageable, "pageable must not be null");
		List<TaskJobExecution> taskJobExecutions = getTaskJobExecutionsWithStepCountForList(
			jobService.listJobExecutionsForJobWithStepCountFilteredByTaskExecutionId(taskExecutionId, getPageOffset(pageable),
				pageable.getPageSize()));
		return new PageImpl<>(taskJobExecutions, pageable, taskJobExecutions.size());
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsForJobWithStepCount(Pageable pageable, String jobName) throws NoSuchJobException {
		Assert.notNull(pageable, "pageable must not be null");
		List<TaskJobExecution> taskJobExecutions = getTaskJobExecutionsWithStepCountForList(
			jobService.listJobExecutionsForJobWithStepCount(jobName, getPageOffset(pageable), pageable.getPageSize()));
		return new PageImpl<>(taskJobExecutions, pageable, jobService.countJobExecutionsForJob(jobName, null));
	}

	@Override
	public TaskJobExecution getJobExecution(long id) throws NoSuchJobExecutionException {
		logger.debug("getJobExecution:{}", id);
		JobExecution jobExecution = jobService.getJobExecution(id);
		return getTaskJobExecution(jobExecution);
	}

	@Override
	public Page<JobInstanceExecutions> listTaskJobInstancesForJobName(Pageable pageable, String jobName) throws NoSuchJobException {
		Assert.notNull(pageable, "pageable must not be null");
		Assert.notNull(jobName, "jobName must not be null");
		long total = jobService.countJobExecutionsForJob(jobName, null);
		if (total == 0) {
			throw new NoSuchJobException("No Job with that name either current or historic: [" + jobName + "]");
		}
		List<JobInstanceExecutions> taskJobInstances = new ArrayList<>();
		for (JobInstance jobInstance : jobService.listJobInstances(jobName, getPageOffset(pageable),
			pageable.getPageSize())) {
			taskJobInstances.add(getJobInstanceExecution(jobInstance));
		}
		return new PageImpl<>(taskJobInstances, pageable, total);
	}

	@Override
	public JobInstanceExecutions getJobInstance(long id) throws NoSuchJobInstanceException, NoSuchJobException {
		return getJobInstanceExecution(jobService.getJobInstance(id));
	}

	@Override
	public Map<Long, Set<Long>> getJobExecutionIdsByTaskExecutionIds(Collection<Long> taskExecutionIds) {
		return jobService.getJobExecutionIdsByTaskExecutionIds(taskExecutionIds);
	}

	@Override
	public void restartJobExecution(long jobExecutionId) throws NoSuchJobExecutionException {
		restartJobExecution(jobExecutionId, null);
	}

	@Override
	public void restartJobExecution(long jobExecutionId, Boolean useJsonJobParameters) throws NoSuchJobExecutionException {
		logger.info("restarting job:{}", jobExecutionId);
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
		String platformName = null;
		// if task was launched outside of dataflow there will be no task manifest.
		// in this scenario, if there is only one platform, then use that platform.
		// else throw exception.
		if (taskManifest == null) {
			Iterable<Launcher> launchers = this.launcherRepository.findAll();
			List<Launcher> launcherList = new ArrayList<>();
			launchers.forEach(launcherList::add);
			if (launcherList.size() == 1) {
				platformName = launcherList.get(0).getName();
				logger.info(String.format("No task manifest found for task execution " +
						"associated with this job.  Using %s platform.", platformName));
			}
		} else {
			platformName = taskManifest.getPlatformName();
		}
		if (platformName != null) {
			Map<String, String> deploymentProperties = new HashMap<>();
			deploymentProperties.put(DefaultTaskExecutionService.TASK_PLATFORM_NAME, platformName);
			taskExecutionService.executeTask(taskDefinition.getName(), deploymentProperties,
					restartExecutionArgs(taskExecution.getArguments(),
							taskJobExecution.getJobExecution().getJobParameters(), useJsonJobParameters));
		} else {
			throw new IllegalStateException(String.format("Did not find platform for taskName=[%s] , taskId=[%s]",
					taskExecution.getTaskName(), taskJobExecution.getTaskId()));
		}

	}

	//TODO: Boot3x followup Verify usage job params work with Batch 5.x
	/**
	 * Apply identifying job parameters to arguments.  There are cases (incrementers)
	 * that add parameters to a job and thus must be added for each restart so that the
	 * JobInstanceId does not change.
	 *
	 * @param taskExecutionArgs original set of task execution arguments
	 * @param jobParameters     for the job to be restarted.
	 * @param useJsonJobParameters determine what converter to use to serialize the job parameter to the command line arguments.
	 * @return deduped list of arguments that contains the original arguments and any
	 * identifying job parameters not in the original task execution arguments.
	 */
	private List<String> restartExecutionArgs(List<String> taskExecutionArgs, JobParameters jobParameters,
		Boolean useJsonJobParameters) {
		if (useJsonJobParameters == null) {
			useJsonJobParameters = taskConfigurationProperties.isUseJsonJobParameters();
		}
		var jobParamsConverter = useJsonJobParameters ? new ScdfJsonJobParametersConverter()
			: new ScdfDefaultJobParametersConverter();
		List<String> result = new ArrayList<>(taskExecutionArgs);
		jobParameters.getParameters().entrySet().stream()
			.filter((e) -> !e.getKey().startsWith("-"))
			.filter((e) -> taskExecutionArgs.stream().noneMatch((arg) -> arg.startsWith(e.getKey())))
			.map((e) -> e.getKey() + "=" + jobParamsConverter.deserializeJobParameter(e.getValue()))
			.forEach(result::add);
		return result;
	}

	@Override
	public void stopJobExecution(long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		BatchStatus status = jobService.stop(jobExecutionId).getStatus();
		logger.info("stopped:{}:status={}", jobExecutionId, status);
	}

	private TaskJobExecution getTaskJobExecution(JobExecution jobExecution) {
		return new TaskJobExecution(
				getTaskExecutionId(jobExecution),
				jobExecution,
				isTaskDefined(jobExecution),
				jobExecution.getStepExecutions().size());
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
		return new TaskJobExecution(
				getTaskExecutionId(jobExecutionWithStepCount),
				jobExecutionWithStepCount,
				isTaskDefined(jobExecutionWithStepCount),
				jobExecutionWithStepCount.getStepCount());
	}

	private Long getTaskExecutionId(JobExecution jobExecution) {
		Assert.notNull(jobExecution, "jobExecution must not be null");

		Long taskExecutionId = taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecution.getId());
		if (taskExecutionId == null) {
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
		if (pageable.getOffset() > (long) Integer.MAX_VALUE) {
			throw new OffsetOutOfBoundsException("The pageable offset requested for this query is greater than MAX_INT.");
		}
		return (int) pageable.getOffset();
	}

	private List<TaskJobExecution> getTaskJobExecutionsForList(Collection<JobExecution> jobExecutions) {
		Assert.notNull(jobExecutions, "jobExecutions must not be null");
		List<TaskJobExecution> taskJobExecutions = new ArrayList<>();
		for (JobExecution jobExecution : jobExecutions) {
			taskJobExecutions.add(getTaskJobExecution(jobExecution));
		}
		return taskJobExecutions;
	}

	private JobInstanceExecutions getJobInstanceExecution(JobInstance jobInstance) throws NoSuchJobException {
		Assert.notNull(jobInstance, "jobInstance must not be null");
		List<JobExecution> jobExecutions = new ArrayList<>(
			jobService.getJobExecutionsForJobInstance(jobInstance.getJobName(), jobInstance.getInstanceId()));
		return new JobInstanceExecutions(jobInstance, getTaskJobExecutionsForList(jobExecutions));
	}

	private boolean isTaskDefined(JobExecution jobExecution) {
		Long executionId = taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecution.getId());
		TaskExecution taskExecution = taskExplorer.getTaskExecution(executionId);
		return taskDefinitionRepository.findById(taskExecution.getTaskName()).isPresent();
	}
}
