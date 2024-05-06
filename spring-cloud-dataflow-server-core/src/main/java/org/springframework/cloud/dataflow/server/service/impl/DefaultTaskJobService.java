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
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.AggregateTaskExplorer;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.rest.job.JobInstanceExecutions;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.support.JobUtils;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersion;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.server.batch.JobExecutionWithStepCount;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.job.support.JobNotRestartableException;
import org.springframework.cloud.dataflow.server.repository.AggregateJobQueryDao;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskBatchException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.JobServiceContainer;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

	private final AggregateTaskExplorer taskExplorer;

	private final JobServiceContainer jobServiceContainer;

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final LauncherRepository launcherRepository;

	private final AggregateExecutionSupport aggregateExecutionSupport;

	private final AggregateJobQueryDao aggregateJobQueryDao;

	private final TaskDefinitionReader taskDefinitionReader;

	public DefaultTaskJobService(
			JobServiceContainer jobServiceContainer,
			AggregateTaskExplorer taskExplorer,
			TaskDefinitionRepository taskDefinitionRepository,
			TaskExecutionService taskExecutionService,
			LauncherRepository launcherRepository,
			AggregateExecutionSupport aggregateExecutionSupport,
			AggregateJobQueryDao aggregateJobQueryDao,
			TaskDefinitionReader taskDefinitionReader) {
		this.aggregateJobQueryDao = aggregateJobQueryDao;
		Assert.notNull(jobServiceContainer, "jobService must not be null");
		Assert.notNull(taskExplorer, "taskExplorer must not be null");
		Assert.notNull(taskDefinitionRepository, "taskDefinitionRepository must not be null");
		Assert.notNull(taskDefinitionReader, "taskDefinitionReader must not be null");
		Assert.notNull(taskExecutionService, "taskExecutionService must not be null");
		Assert.notNull(launcherRepository, "launcherRepository must not be null");
		Assert.notNull(aggregateExecutionSupport, "CompositeExecutionSupport must not be null");
		this.jobServiceContainer = jobServiceContainer;
		this.taskExplorer = taskExplorer;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskDefinitionReader = taskDefinitionReader;
		this.taskExecutionService = taskExecutionService;
		this.launcherRepository = launcherRepository;
		this.aggregateExecutionSupport = aggregateExecutionSupport;
	}

	@Override
	public Page<TaskJobExecution> listJobExecutions(Pageable pageable) throws NoSuchJobExecutionException {
		Assert.notNull(pageable, "pageable must not be null");
		return aggregateJobQueryDao.listJobExecutionsWithSteps(pageable);
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsWithStepCount(Pageable pageable) {
		Assert.notNull(pageable, "pageable must not be null");
		return aggregateJobQueryDao.listJobExecutionsWithStepCount(pageable);
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsForJob(Pageable pageable, String jobName, BatchStatus status) throws NoSuchJobException, NoSuchJobExecutionException {
		Assert.notNull(pageable, "pageable must not be null");
		if(status != null) {
			return aggregateJobQueryDao.listJobExecutions(jobName, status, pageable);
		} else if(StringUtils.hasText(jobName)) {
			return aggregateJobQueryDao.listJobExecutionsForJobWithStepCount(jobName, pageable);
		} else {
			return aggregateJobQueryDao.listJobExecutionsWithSteps(pageable);
		}
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsForJobWithStepCount(Pageable pageable, Date fromDate, Date toDate) {
		Assert.notNull(pageable, "pageable must not be null");
		return aggregateJobQueryDao.listJobExecutionsBetween(fromDate, toDate, pageable);
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByJobInstanceId(
			Pageable pageable,
			int jobInstanceId,
			String schemaTarget
	) {
		Assert.notNull(pageable, "pageable must not be null");
		return aggregateJobQueryDao.listJobExecutionsForJobWithStepCountFilteredByJobInstanceId(jobInstanceId, schemaTarget, pageable);
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByTaskExecutionId(
			Pageable pageable,
			int taskExecutionId,
			String schemaTarget
	) {
		Assert.notNull(pageable, "pageable must not be null");
		return aggregateJobQueryDao.listJobExecutionsForJobWithStepCountFilteredByTaskExecutionId(taskExecutionId, schemaTarget, pageable);
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsForJobWithStepCount(Pageable pageable, String jobName) throws NoSuchJobException {
		Assert.notNull(pageable, "pageable must not be null");
		return aggregateJobQueryDao.listJobExecutionsForJobWithStepCount(jobName, pageable);
	}

	@Override
	public TaskJobExecution getJobExecution(long id, String schemaTarget) throws NoSuchJobExecutionException {
		logger.info("getJobExecution:{}:{}", id, schemaTarget);
		if (!StringUtils.hasText(schemaTarget)) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		return aggregateJobQueryDao.getJobExecution(id, schemaTarget);
	}

	@Override
	public Page<JobInstanceExecutions> listTaskJobInstancesForJobName(Pageable pageable, String jobName) throws NoSuchJobException {
		Assert.notNull(pageable, "pageable must not be null");
		Assert.notNull(jobName, "jobName must not be null");
		return aggregateJobQueryDao.listJobInstances(jobName, pageable);
	}

	@Override
	public JobInstanceExecutions getJobInstance(long id, String schemaTarget) throws NoSuchJobInstanceException, NoSuchJobException {
		return aggregateJobQueryDao.getJobInstanceExecutions(id, schemaTarget);
	}

	@Override
	public Map<Long, Set<Long>> getJobExecutionIdsByTaskExecutionIds(Collection<Long> taskExecutionIds, String schemaTarget) {
		JobService jobService = this.jobServiceContainer.get(schemaTarget);
		Assert.notNull(jobService, ()->"Expected JobService for " + schemaTarget);
		return jobService.getJobExecutionIdsByTaskExecutionIds(taskExecutionIds);
	}

	@Override
	public void restartJobExecution(long jobExecutionId, String schemaTarget) throws NoSuchJobExecutionException {
		logger.info("restarting job:{}:{}", jobExecutionId, schemaTarget);
		final TaskJobExecution taskJobExecution = this.getJobExecution(jobExecutionId, schemaTarget);
		final JobExecution jobExecution = taskJobExecution.getJobExecution();

		if (!JobUtils.isJobExecutionRestartable(taskJobExecution.getJobExecution())) {
			throw new JobNotRestartableException(
					String.format("JobExecution with Id '%s' and state '%s' is not " + "restartable.",
							jobExecution.getId(), taskJobExecution.getJobExecution().getStatus()));
		}

		AggregateTaskExecution taskExecution = this.taskExplorer.getTaskExecution(taskJobExecution.getTaskId(), taskJobExecution.getSchemaTarget());
		TaskManifest taskManifest = this.taskExecutionService.findTaskManifestById(taskExecution.getExecutionId(), taskExecution.getSchemaTarget());
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
							taskJobExecution.getJobExecution().getJobParameters(), schemaTarget));
		} else {
			throw new IllegalStateException(String.format("Did not find platform for taskName=[%s] , taskId=[%s]",
					taskExecution.getTaskName(), taskJobExecution.getTaskId()));
		}

	}

	/**
	 * Apply identifying job parameters to arguments.  There are cases (incrementers)
	 * that add parameters to a job and thus must be added for each restart so that the
	 * JobInstanceId does not change.
	 *
	 * @param taskExecutionArgs original set of task execution arguments
	 * @param jobParameters     for the job to be restarted.
	 * @return deduped list of arguments that contains the original arguments and any
	 * identifying job parameters not in the original task execution arguments.
	 */
	private List<String> restartExecutionArgs(List<String> taskExecutionArgs, JobParameters jobParameters, String schemaTarget) {
		List<String> result = new ArrayList<>(taskExecutionArgs);
		String boot3Version = SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT3).getName();
		String type;
		Map<String, JobParameter> jobParametersMap = jobParameters.getParameters();
		for (String key : jobParametersMap.keySet()) {
			if (!key.startsWith("-")) {
				boolean existsFlag = false;
				for (String arg : taskExecutionArgs) {
					if (arg.startsWith(key)) {
						existsFlag = true;
						break;
					}
				}
				if (!existsFlag) {
					String param;
					if (boot3Version.equals(schemaTarget)) {
						if (JobParameter.ParameterType.LONG.equals(jobParametersMap.get(key).getType())) {
							type = Long.class.getCanonicalName();
						} else if (JobParameter.ParameterType.DATE.equals(jobParametersMap.get(key).getType())) {
							type = Date.class.getCanonicalName();
						} else if (JobParameter.ParameterType.DOUBLE.equals(jobParametersMap.get(key).getType())) {
							type = Double.class.getCanonicalName();
						} else if (JobParameter.ParameterType.STRING.equals(jobParametersMap.get(key).getType())) {
							type = String.class.getCanonicalName();
						} else  {
							throw new IllegalArgumentException("Unable to convert " +
								jobParametersMap.get(key).getType() + " to known type of JobParameters");
						}
						param = String.format("%s=%s,%s", key, jobParametersMap.get(key).getValue(), type);
					} else {
						param = String.format("%s(%s)=%s", key,
							jobParametersMap.get(key).getType().toString().toLowerCase(),
							jobParameters.getString(key));
					}
					result.add(param);
				}
			}
		}
		return result;
	}

	@Override
	public void stopJobExecution(long jobExecutionId, String schemaTarget) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		if (!StringUtils.hasText(schemaTarget)) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		JobService jobService = jobServiceContainer.get(schemaTarget);
		BatchStatus status = jobService.stop(jobExecutionId).getStatus();
		logger.info("stopped:{}:{}:status={}", jobExecutionId, schemaTarget, status);
	}

	@Override
	public void populateComposeTaskRunnerStatus(Collection<AggregateTaskExecution> taskExecutions) {
		aggregateJobQueryDao.populateCtrStatus(taskExecutions);
	}

	private TaskJobExecution getTaskJobExecution(JobExecution jobExecution, String schemaTarget) {
		return new TaskJobExecution(
				getTaskExecutionId(jobExecution, schemaTarget),
				jobExecution,
				isTaskDefined(jobExecution),
				jobExecution.getStepExecutions().size(),
				schemaTarget
		);
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
		SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget(jobExecutionWithStepCount.getJobConfigurationName(), taskDefinitionReader);
		return new TaskJobExecution(
				getTaskExecutionId(jobExecutionWithStepCount, schemaVersionTarget.getName()),
				jobExecutionWithStepCount,
				isTaskDefined(jobExecutionWithStepCount),
				jobExecutionWithStepCount.getStepCount(),
				schemaVersionTarget.getName()
		);
	}

	private Long getTaskExecutionId(JobExecution jobExecution, String schemaTarget) {
		Assert.notNull(jobExecution, "jobExecution must not be null");

		Long taskExecutionId = taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecution.getId(), schemaTarget);
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

	private JobInstanceExecutions getJobInstanceExecution(JobInstance jobInstance) {
		Assert.notNull(jobInstance, "jobInstance must not be null");
		return aggregateJobQueryDao.getJobInstanceExecution(jobInstance.getJobName(), jobInstance.getInstanceId());
	}

	private boolean isTaskDefined(JobExecution jobExecution) {
		SchemaVersionTarget versionTarget = aggregateExecutionSupport.findSchemaVersionTarget(jobExecution.getJobInstance().getJobName(), taskDefinitionReader);
		Long executionId = taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecution.getId(), versionTarget.getName());
		AggregateTaskExecution taskExecution = taskExplorer.getTaskExecution(executionId, versionTarget.getName());
		return taskDefinitionRepository.findById(taskExecution.getTaskName()).isPresent();
	}
}
