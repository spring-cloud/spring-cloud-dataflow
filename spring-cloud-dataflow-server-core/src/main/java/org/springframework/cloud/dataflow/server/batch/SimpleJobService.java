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

import javax.batch.operations.JobOperator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
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
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.server.repository.AggregateJobQueryDao;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
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
public class SimpleJobService implements JobService, DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(SimpleJobService.class);

	// 60 seconds
	private static final int DEFAULT_SHUTDOWN_TIMEOUT = 60 * 1000;

	private final SearchableJobInstanceDao jobInstanceDao;

	private final SearchableJobExecutionDao jobExecutionDao;

	private final JobRepository jobRepository;

	private final SearchableStepExecutionDao stepExecutionDao;

	private final ExecutionContextDao executionContextDao;

	private Collection<JobExecution> activeExecutions = Collections.synchronizedList(new ArrayList<JobExecution>());

	private JobOperator jsrJobOperator;

	private final AggregateJobQueryDao aggregateJobQueryDao;

	private int shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;

	private final SchemaVersionTarget schemaVersionTarget;

	public SimpleJobService(SearchableJobInstanceDao jobInstanceDao, SearchableJobExecutionDao jobExecutionDao,
							SearchableStepExecutionDao stepExecutionDao, JobRepository jobRepository,
							ExecutionContextDao executionContextDao, JobOperator jsrJobOperator, AggregateJobQueryDao aggregateJobQueryDao,
							SchemaVersionTarget schemaVersionTarget) {
		super();
		this.jobInstanceDao = jobInstanceDao;
		this.jobExecutionDao = jobExecutionDao;
		this.stepExecutionDao = stepExecutionDao;
		this.jobRepository = jobRepository;
		this.executionContextDao = executionContextDao;
		this.aggregateJobQueryDao = aggregateJobQueryDao;
		this.schemaVersionTarget = schemaVersionTarget;

		if (jsrJobOperator == null) {
			logger.warn("No JobOperator compatible with JSR-352 was provided.");
		} else {
			this.jsrJobOperator = jsrJobOperator;
		}
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

	/**
	 * Delegates launching to
	 * {@link org.springframework.cloud.dataflow.server.batch.SimpleJobService#restart(Long, org.springframework.batch.core.JobParameters)}
	 *
	 * @param jobExecutionId the job execution to restart
	 * @return Instance of {@link JobExecution} associated with the restart.
	 * @throws NoSuchJobException thrown if job does not exist
	 */
	@Override
	public JobExecution restart(Long jobExecutionId)
			throws NoSuchJobException {
		return restart(jobExecutionId, null);
	}

	@Override
	public JobExecution restart(Long jobExecutionId, JobParameters params) throws NoSuchJobException {

		JobExecution jobExecution;

		if (jsrJobOperator != null) {
			if (params != null) {
				jobExecution = new JobExecution(jsrJobOperator.restart(jobExecutionId, params.toProperties()));
			} else {
				jobExecution = new JobExecution(jsrJobOperator.restart(jobExecutionId, new Properties()));
			}
		} else {
			throw new NoSuchJobException(String.format("Can't find job associated with job execution id %s to restart",
					String.valueOf(jobExecutionId)));
		}

		return jobExecution;
	}

	@Override
	public JobExecution launch(String jobName, JobParameters jobParameters) throws NoSuchJobException {
		JobExecution jobExecution;

		if (jsrJobOperator != null) {
			jobExecution = new JobExecution(jsrJobOperator.start(jobName, jobParameters.toProperties()));
		} else {
			throw new NoSuchJobException(String.format("Unable to find job %s to launch",
					String.valueOf(jobName)));
		}

		return jobExecution;
	}

	@Override
	public JobParameters getLastJobParameters(String jobName) {
		Collection<JobExecution> executions = jobExecutionDao.getJobExecutions(jobName, null, 0, 1);

		JobExecution lastExecution = null;
		if (!CollectionUtils.isEmpty(executions)) {
			lastExecution = executions.iterator().next();
		}

		JobParameters oldParameters = new JobParameters();
		if (lastExecution != null) {
			oldParameters = lastExecution.getJobParameters();
		}

		return oldParameters;

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
	public Collection<String> listJobs(int start, int count) {
		Collection<String> jobNames = jobInstanceDao.getJobNames();
		return new ArrayList<>(jobNames).subList(start, start + count);
	}

	private Collection<String> getJsrJobNames() {
		Set<String> jsr352JobNames = new HashSet<String>();

		try {
			PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
			Resource[] resources = pathMatchingResourcePatternResolver
					.getResources("classpath*:/META-INF/batch-jobs/**/*.xml");

			for (Resource resource : resources) {
				String jobXmlFileName = resource.getFilename();
				jsr352JobNames.add(jobXmlFileName.substring(0, jobXmlFileName.length() - 4));
			}
		} catch (IOException e) {
			logger.debug("Unable to list JSR-352 batch jobs", e);
		}

		return jsr352JobNames;
	}

	@Override
	public int countJobs() {
		return jobInstanceDao.getJobNames().size();
	}

	@Override
	public int stopAll() {
		Collection<JobExecution> result = jobExecutionDao.getRunningJobExecutions();
		Collection<String> jsrJobNames = getJsrJobNames();

		for (JobExecution jobExecution : result) {
			if (jsrJobOperator != null && jsrJobNames.contains(jobExecution.getJobInstance().getJobName())) {
				jsrJobOperator.stop(jobExecution.getId());
			} else {
				jobExecution.stop();
				jobRepository.update(jobExecution);
			}
		}

		return result.size();
	}

	@Override
	public JobExecution stop(Long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		JobExecution jobExecution = getJobExecution(jobExecutionId);
		if (!jobExecution.isRunning()) {
			throw new JobExecutionNotRunningException("JobExecution is not running and therefore cannot be stopped");
		}

		logger.info("Stopping job execution: " + jobExecution);

		Collection<String> jsrJobNames = getJsrJobNames();

		if (jsrJobOperator != null && jsrJobNames.contains(jobExecution.getJobInstance().getJobName())) {
			jsrJobOperator.stop(jobExecutionId);
			jobExecution = getJobExecution(jobExecutionId);
		} else {
			jobExecution.stop();
			jobRepository.update(jobExecution);
		}
		return jobExecution;

	}

	@Override
	public JobExecution abandon(Long jobExecutionId) throws NoSuchJobExecutionException,
			JobExecutionAlreadyRunningException {

		JobExecution jobExecution = getJobExecution(jobExecutionId);
		if (jobExecution.getStatus().isLessThan(BatchStatus.STOPPING)) {
			throw new JobExecutionAlreadyRunningException(
					"JobExecution is running or complete and therefore cannot be aborted");
		}

		logger.info("Aborting job execution: " + jobExecution);

		Collection<String> jsrJobNames = getJsrJobNames();

		JobInstance jobInstance = jobExecution.getJobInstance();
		if (jsrJobOperator != null && jsrJobNames.contains(jobInstance.getJobName())) {
			jsrJobOperator.abandon(jobExecutionId);
			jobExecution = getJobExecution(jobExecutionId);
		} else {
			jobExecution.upgradeStatus(BatchStatus.ABANDONED);
			jobExecution.setEndTime(new Date());
			jobRepository.update(jobExecution);
		}

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
	public int countJobInstances(String name) {
		return jobInstanceDao.countJobInstances(name);
	}

	@Override
	public JobExecution getJobExecution(Long jobExecutionId) throws NoSuchJobExecutionException {
		JobExecution jobExecution =  this.aggregateJobQueryDao.getJobExecution(jobExecutionId, this.schemaVersionTarget.getName()).getJobExecution();
		jobExecution.setJobInstance(Objects.requireNonNull(this.jobInstanceDao.getJobInstance(jobExecution)));
		try {
			jobExecution.setExecutionContext(this.executionContextDao.getExecutionContext(jobExecution));
		} catch (Exception e) {
			this.logger.info("Cannot load execution context for job execution: " + jobExecution);
		}
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
	public int countStepExecutionsForJobExecution(long jobExecutionId) {
		return stepExecutionDao.countStepExecutionsForJobExecution(jobExecutionId);
	}

	@Override
	public JobInstance getJobInstance(long jobInstanceId) throws NoSuchJobInstanceException {
		JobInstance jobInstance = this.aggregateJobQueryDao.getJobInstance(jobInstanceId, this.schemaVersionTarget.getName());
		return jobInstance;
	}

	@Override
	public Collection<JobInstance> listJobInstances(String jobName, int start, int count) throws NoSuchJobException {
		checkJobExists(jobName);
		return jobInstanceDao.getJobInstances(jobName, start, count);
	}

	@Override
	public Collection<String> getStepNamesForJob(String jobName) throws NoSuchJobException {
		Collection<String> stepNames = new LinkedHashSet<>();
		for (JobExecution jobExecution : listJobExecutionsForJob(jobName, null, 0, 100)) {
			for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
				stepNames.add(stepExecution.getStepName());
			}
		}
		return Collections.unmodifiableList(new ArrayList<>(stepNames));
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
		if (getJsrJobNames().stream().anyMatch(e -> e.contains(jobName)) ||
				jobInstanceDao.countJobInstances(jobName) > 0) {
			return;
		}
		throw new NoSuchJobException("No Job with that name either current or historic: [" + jobName + "]");
	}

	/**
	 * Stop all the active jobs and wait for them (up to a time out) to finish processing.
	 */
	@Override
	public void destroy() throws Exception {

		Exception firstException = null;

		for (JobExecution jobExecution : activeExecutions) {
			try {
				if (jobExecution.isRunning()) {
					stop(jobExecution.getId());
				}
			} catch (JobExecutionNotRunningException e) {
				logger.info("JobExecution is not running so it cannot be stopped");
			} catch (Exception e) {
				logger.error("Unexpected exception stopping JobExecution", e);
				if (firstException == null) {
					firstException = e;
				}
			}
		}

		int count = 0;
		int maxCount = (shutdownTimeout + 1000) / 1000;
		while (!activeExecutions.isEmpty() && ++count < maxCount) {
			logger.error("Waiting for " + activeExecutions.size() + " active executions to complete");
			removeInactiveExecutions();
			Thread.sleep(1000L);
		}

		if (firstException != null) {
			throw firstException;
		}

	}

	/**
	 * Check all the active executions and see if they are still actually running. Remove the
	 * ones that have completed.
	 */
	@Scheduled(fixedDelay = 60000)
	public void removeInactiveExecutions() {

		for (Iterator<JobExecution> iterator = activeExecutions.iterator(); iterator.hasNext(); ) {
			JobExecution jobExecution = iterator.next();
			try {
				jobExecution = getJobExecution(jobExecution.getId());
			} catch (NoSuchJobExecutionException e) {
				logger.error("Unexpected exception loading JobExecution", e);
			}
			if (!jobExecution.isRunning()) {
				iterator.remove();
			}
		}

	}
}
