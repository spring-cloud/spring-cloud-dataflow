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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link JobService} that delegates most of its work to other
 * off-the-shelf components.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
public class SimpleJobService implements JobService {

	private static final Logger logger = LoggerFactory.getLogger(SimpleJobService.class);

	// 60 seconds
	private static final int DEFAULT_SHUTDOWN_TIMEOUT = 60 * 1000;

	private final SearchableJobInstanceDao jobInstanceDao;

	private final SearchableJobExecutionDao jobExecutionDao;

	private final JobRepository jobRepository;

	private final SearchableStepExecutionDao stepExecutionDao;

	private final ExecutionContextDao executionContextDao;

	private JobOperator jobOperator;


	private int shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;

	public SimpleJobService(SearchableJobInstanceDao jobInstanceDao, SearchableJobExecutionDao jobExecutionDao,
							SearchableStepExecutionDao stepExecutionDao, JobRepository jobRepository,
							ExecutionContextDao executionContextDao, JobOperator jobOperator) {
		super();
		this.jobInstanceDao = jobInstanceDao;
		this.jobExecutionDao = jobExecutionDao;
		this.stepExecutionDao = stepExecutionDao;
		this.jobRepository = jobRepository;
		this.executionContextDao = executionContextDao;
		this.jobOperator = Objects.requireNonNull(jobOperator, "jobOperator must not be null");
	}

	/**
	 * Timeout for shutdown waiting for jobs to finish processing.
	 *
	 * @param shutdownTimeout in milliseconds (default 60 secs)
	 */
	public void setShutdownTimeout(int shutdownTimeout) {
		this.shutdownTimeout = shutdownTimeout;
	}

	@Override
	public Collection<StepExecution> getStepExecutions(Long jobExecutionId) throws NoSuchJobExecutionException {

		JobExecution jobExecution = jobExecutionDao.getJobExecution(jobExecutionId);
		if (jobExecution == null) {
			throw new NoSuchJobExecutionException("No JobExecution with id=" + jobExecutionId);
		}
		return getStepExecutions(jobExecution);

	}

	@Override
	public Collection<StepExecution> getStepExecutions(JobExecution jobExecution) {
		Assert.notNull(jobExecution, "jobExecution required");
		stepExecutionDao.addStepExecutions(jobExecution);
		return jobExecution.getStepExecutions();
	}

	@Override
	public void addStepExecutions(JobExecution jobExecution) {
		stepExecutionDao.addStepExecutions(jobExecution);
	}

	@Override
	public JobExecution restart(Long jobExecutionId, JobParameters params) throws NoSuchJobException {

		JobExecution jobExecution;

        try {
            jobExecution = new JobExecution(jobOperator.restart(jobExecutionId.longValue()));
        }
		  catch (Exception e) {
			throw new JobRestartRuntimeException(jobExecutionId, e);
        }

        return jobExecution;
	}

	@Override
	public Collection<JobExecution> listJobExecutions(int start, int count) {
		return jobExecutionDao.getJobExecutions(start, count);
	}

	@Override
	public Collection<JobExecutionWithStepCount> listJobExecutionsWithStepCount(int start, int count) {
		return jobExecutionDao.getJobExecutionsWithStepCount(start, count);
	}

	@Override
	public int countJobExecutions() {
		return jobExecutionDao.countJobExecutions();
	}


	@Override
	public JobExecution stop(Long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		return stopJobExecution(getJobExecution(jobExecutionId));
	}

	private JobExecution stopJobExecution(JobExecution jobExecution) throws JobExecutionNotRunningException{
		if (!jobExecution.isRunning()) {
			throw new JobExecutionNotRunningException("JobExecution is not running and therefore cannot be stopped");
		}
		// Indicate the execution should be stopped by setting it's status to
		// 'STOPPING'. It is assumed that
		// the step implementation will check this status at chunk boundaries.
		logger.info("Stopping job execution: {}", jobExecution);
		jobExecution.getStepExecutions().forEach(StepExecution::setTerminateOnly);
		jobExecution.setStatus(BatchStatus.STOPPING);
		jobRepository.update(jobExecution);
		return jobExecution;

	}

	@Override
	public int countJobExecutionsForJob(String name, BatchStatus status) throws NoSuchJobException {
		return countJobExecutions(name, status);
	}

	private int countJobExecutions(String jobName, BatchStatus status) throws NoSuchJobException {
		if (!StringUtils.hasText(jobName)) {
			if (status == null) {
				throw new IllegalArgumentException("One of jobName or status must be specified");
			}
			return jobExecutionDao.countJobExecutions(status);
		}
		checkJobExists(jobName);
		return (status != null) ?
				jobExecutionDao.countJobExecutions(jobName, status) :
				jobExecutionDao.countJobExecutions(jobName);
	}

	@Override
	public JobExecution getJobExecution(Long jobExecutionId) throws NoSuchJobExecutionException {
		JobExecution jobExecution = jobExecutionDao.getJobExecution(jobExecutionId);
		if (jobExecution == null) {
			throw new NoSuchJobExecutionException("There is no JobExecution with id=" + jobExecutionId);
		}
		jobExecution.setJobInstance(jobInstanceDao.getJobInstance(jobExecution));
		try {
			jobExecution.setExecutionContext(executionContextDao.getExecutionContext(jobExecution));
		}
		catch (Exception e) {
			logger.info("Cannot load execution context for job execution: " + jobExecution);
		}
		stepExecutionDao.addStepExecutions(jobExecution);
		return jobExecution;
	}

	@Override
	public Collection<JobExecution> getJobExecutionsForJobInstance(String name, Long jobInstanceId)
			throws NoSuchJobException {
		checkJobExists(name);
		List<JobExecution> jobExecutions = jobExecutionDao.findJobExecutions(Objects.requireNonNull(jobInstanceDao.getJobInstance(jobInstanceId)));
		for (JobExecution jobExecution : jobExecutions) {
			stepExecutionDao.addStepExecutions(jobExecution);
		}
		return jobExecutions;
	}

	@Override
	public StepExecution getStepExecution(Long jobExecutionId, Long stepExecutionId)
			throws NoSuchJobExecutionException, NoSuchStepExecutionException {
		JobExecution jobExecution = getJobExecution(jobExecutionId);
		return getStepExecution(jobExecution, stepExecutionId);
	}

	@Override
	public StepExecution getStepExecution(JobExecution jobExecution, Long stepExecutionId) throws NoSuchStepExecutionException {
		StepExecution stepExecution = stepExecutionDao.getStepExecution(jobExecution, stepExecutionId);
		if (stepExecution == null) {
			throw new NoSuchStepExecutionException("There is no StepExecution with jobExecutionId=" + jobExecution.getId()
					+ " and id=" + stepExecutionId);
		}
		try {
			stepExecution.setExecutionContext(executionContextDao.getExecutionContext(stepExecution));
		} catch (Exception e) {
			logger.info("Cannot load execution context for step execution: " + stepExecution);
		}
		return stepExecution;
	}

	@Override
	public Collection<JobExecutionWithStepCount> listJobExecutionsForJobWithStepCount(String jobName, int start,
																					  int count)
			throws NoSuchJobException {
		checkJobExists(jobName);
		return jobExecutionDao.getJobExecutionsWithStepCount(jobName, start, count);
	}

	@Override
	public Collection<StepExecution> listStepExecutionsForStep(String jobName, String stepName, int start, int count)
			throws NoSuchStepException {
		if (stepExecutionDao.countStepExecutions(jobName, stepName) == 0) {
			throw new NoSuchStepException("No step executions exist with this step name: " + stepName);
		}
		return stepExecutionDao.findStepExecutions(jobName, stepName, start, count);
	}

	@Override
	public int countStepExecutionsForStep(String jobName, String stepName) throws NoSuchStepException {
		return stepExecutionDao.countStepExecutions(jobName, stepName);
	}

	@Override
	public JobInstance getJobInstance(long jobInstanceId) throws NoSuchJobInstanceException {
		JobInstance jobInstance = jobInstanceDao.getJobInstance(jobInstanceId);
		if (jobInstance == null) {
			throw new NoSuchJobInstanceException("JobInstance with id=" + jobInstanceId + " does not exist");
		}
		return jobInstance;
	}

	@Override
	public Collection<JobInstance> listJobInstances(String jobName, int start, int count) throws NoSuchJobException {
		checkJobExists(jobName);
		return jobInstanceDao.getJobInstances(jobName, start, count);
	}

	@Override
	public Collection<JobExecution> listJobExecutionsForJob(String jobName, BatchStatus status, int pageOffset,
															int pageSize) {
		List<JobExecution> jobExecutions = getJobExecutions(jobName, status, pageOffset, pageSize);

		for (JobExecution jobExecution : jobExecutions) {
			stepExecutionDao.addStepExecutions(jobExecution);
		}

		return jobExecutions;
	}

	@Override
	public Collection<JobExecutionWithStepCount> listJobExecutionsForJobWithStepCount(Date fromDate,
																					  Date toDate, int start, int count) {
		return jobExecutionDao.getJobExecutionsWithStepCount(fromDate, toDate, start, count);
	}

	@Override
	public Collection<JobExecutionWithStepCount> listJobExecutionsForJobWithStepCountFilteredByJobInstanceId(
			int jobInstanceId, int start, int count) {
		return jobExecutionDao.getJobExecutionsWithStepCountFilteredByJobInstanceId(jobInstanceId, start, count);
	}

	@Override
	public Collection<JobExecutionWithStepCount> listJobExecutionsForJobWithStepCountFilteredByTaskExecutionId(
			int taskExecutionId, int start, int count) {
		return jobExecutionDao.getJobExecutionsWithStepCountFilteredByTaskExecutionId(taskExecutionId, start, count);
	}

	@Override
	public Map<Long, Set<Long>> getJobExecutionIdsByTaskExecutionIds(Collection<Long> taskExecutionIds) {
		return this.jobExecutionDao.getJobExecutionsByTaskIds(taskExecutionIds);
	}

	private List<JobExecution> getJobExecutions(String jobName, BatchStatus status, int pageOffset, int pageSize) {
		if (StringUtils.isEmpty(jobName)) {
			if (status != null) {
				return jobExecutionDao.getJobExecutions(status, pageOffset, pageSize);
			}
		} else {
			if (status != null) {
				return jobExecutionDao.getJobExecutions(jobName, status, pageOffset, pageSize);
			}
		}

		return jobExecutionDao.getJobExecutions(jobName, pageOffset, pageSize);
	}

	private void checkJobExists(String jobName) throws NoSuchJobException {
		if (jobInstanceDao.countJobInstances(jobName) <= 0) {
			throw new NoSuchJobException("No Job with that name either current or historic: [" + jobName + "]");
		}
	}
}
