/*
 * Copyright 2009-2019 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.batch.operations.JobOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.ListableJobLocator;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

/**
 * Implementation of {@link JobService} that delegates most of its work to other
 * off-the-shelf components.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Glenn Renfro
 *
 */
public class SimpleJobService implements JobService, DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(SimpleJobService.class);

	// 60 seconds
	private static final int DEFAULT_SHUTDOWN_TIMEOUT = 60 * 1000;

	private final SearchableJobInstanceDao jobInstanceDao;

	private final SearchableJobExecutionDao jobExecutionDao;

	private final JobRepository jobRepository;

	private final JobLauncher jobLauncher;

	private final ListableJobLocator jobLocator;

	private final SearchableStepExecutionDao stepExecutionDao;

	private final ExecutionContextDao executionContextDao;

	private Collection<JobExecution> activeExecutions = Collections.synchronizedList(new ArrayList<JobExecution>());

	private JobOperator jsrJobOperator;

	private int shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;

	/**
	 * Timeout for shutdown waiting for jobs to finish processing.
	 *
	 * @param shutdownTimeout in milliseconds (default 60 secs)
	 */
	public void setShutdownTimeout(int shutdownTimeout) {
		this.shutdownTimeout = shutdownTimeout;
	}

	public SimpleJobService(SearchableJobInstanceDao jobInstanceDao, SearchableJobExecutionDao jobExecutionDao,
			SearchableStepExecutionDao stepExecutionDao, JobRepository jobRepository, JobLauncher jobLauncher,
			ListableJobLocator jobLocator, ExecutionContextDao executionContextDao) {
		this(jobInstanceDao, jobExecutionDao, stepExecutionDao, jobRepository, jobLauncher, jobLocator, executionContextDao, null);
	}

	public SimpleJobService(SearchableJobInstanceDao jobInstanceDao, SearchableJobExecutionDao jobExecutionDao,
			SearchableStepExecutionDao stepExecutionDao, JobRepository jobRepository, JobLauncher jobLauncher,
			ListableJobLocator jobLocator, ExecutionContextDao executionContextDao, JobOperator jsrJobOperator) {
		super();
		this.jobInstanceDao = jobInstanceDao;
		this.jobExecutionDao = jobExecutionDao;
		this.stepExecutionDao = stepExecutionDao;
		this.jobRepository = jobRepository;
		this.jobLauncher = jobLauncher;
		this.jobLocator = jobLocator;
		this.executionContextDao = executionContextDao;

		if(jsrJobOperator == null) {
			logger.warn("No JobOperator compatible with JSR-352 was provided.");
		}
		else {
			this.jsrJobOperator = jsrJobOperator;
		}
	}

	@Override
	public Collection<StepExecution> getStepExecutions(Long jobExecutionId) throws NoSuchJobExecutionException {

		JobExecution jobExecution = jobExecutionDao.getJobExecution(jobExecutionId);
		if (jobExecution == null) {
			throw new NoSuchJobExecutionException("No JobExecution with id=" + jobExecutionId);
		}

		stepExecutionDao.addStepExecutions(jobExecution);

		String jobName = jobExecution.getJobInstance() == null ? jobInstanceDao.getJobInstance(jobExecution).getJobName() : jobExecution.getJobInstance().getJobName();
		Collection<String> missingStepNames = new LinkedHashSet<String>();

		if (jobName != null) {
			missingStepNames.addAll(stepExecutionDao.findStepNamesForJobExecution(jobName, "*:partition*"));
			logger.debug("Found step executions in repository: " + missingStepNames);
		}

		Job job = null;
		try {
			job = jobLocator.getJob(jobName);
		}
		catch (NoSuchJobException e) {
			// expected
		}
		if (job instanceof StepLocator) {
			Collection<String> stepNames = ((StepLocator) job).getStepNames();
			missingStepNames.addAll(stepNames);
			logger.debug("Added step executions from job: " + missingStepNames);
		}

		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			String stepName = stepExecution.getStepName();
			if (missingStepNames.contains(stepName)) {
				missingStepNames.remove(stepName);
			}
			logger.debug("Removed step executions from job execution: " + missingStepNames);
		}

		for (String stepName : missingStepNames) {
			StepExecution stepExecution = jobExecution.createStepExecution(stepName);
			stepExecution.setStatus(BatchStatus.UNKNOWN);
		}

		return jobExecution.getStepExecutions();

	}

	@Override
	public boolean isLaunchable(String jobName) {
		return jobLocator.getJobNames().contains(jobName) || getJsrJobNames().contains(jobName);
	}

	@Override
	public boolean isIncrementable(String jobName) {
		try {
			return jobLocator.getJobNames().contains(jobName)
					&& jobLocator.getJob(jobName).getJobParametersIncrementer() != null;
		}
		catch (NoSuchJobException e) {
			// Should not happen
			throw new IllegalStateException("Unexpected non-existent job: " + jobName);
		}
	}

	/**
	 * Delegates launching to {@link org.springframework.cloud.dataflow.server.batch.SimpleJobService#restart(Long, org.springframework.batch.core.JobParameters)}
	 *
	 * @param jobExecutionId the job execution to restart
	 * @return Instance of {@link JobExecution} associated with the restart.

	 * @throws NoSuchJobExecutionException thrown if job execution specified does not exist
	 * @throws JobExecutionAlreadyRunningException thrown if job is already running
	 * @throws JobRestartException thrown if job can not be restarted
	 * @throws JobInstanceAlreadyCompleteException thrown if job is already complete
	 * @throws NoSuchJobException thrown if job does not exist
	 * @throws JobParametersInvalidException the parameters specified are invalid
	 */
	@Override
	public JobExecution restart(Long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, NoSuchJobException, JobParametersInvalidException {
		return restart(jobExecutionId, null);
	}

	@Override
	public JobExecution restart(Long jobExecutionId, JobParameters params) throws NoSuchJobExecutionException,
	JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException,
	NoSuchJobException, JobParametersInvalidException {

		JobExecution jobExecution = null;

		JobExecution target = getJobExecution(jobExecutionId);
		JobInstance lastInstance = target.getJobInstance();

		if(jobLocator.getJobNames().contains(lastInstance.getJobName())) {
			Job job = jobLocator.getJob(lastInstance.getJobName());

			jobExecution = jobLauncher.run(job, target.getJobParameters());

			if (jobExecution.isRunning()) {
				activeExecutions.add(jobExecution);
			}
		}
		else {
			if(jsrJobOperator != null) {
				if(params != null) {
					jobExecution = new JobExecution(jsrJobOperator.restart(jobExecutionId, params.toProperties()));
				}
				else {
					jobExecution = new JobExecution(jsrJobOperator.restart(jobExecutionId, new Properties()));
				}
			}
			else {
				throw new NoSuchJobException(String.format("Can't find job associated with job execution id %s to restart",
						String.valueOf(jobExecutionId)));
			}
		}

		return jobExecution;
	}

	@Override
	public JobExecution launch(String jobName, JobParameters jobParameters) throws NoSuchJobException,
	JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException,
	JobParametersInvalidException {

		JobExecution jobExecution = null;

		if(jobLocator.getJobNames().contains(jobName)) {
			Job job = jobLocator.getJob(jobName);

			JobExecution lastJobExecution = jobRepository.getLastJobExecution(jobName, jobParameters);
			boolean restart = false;
			if (lastJobExecution != null) {
				BatchStatus status = lastJobExecution.getStatus();
				if (status.isUnsuccessful() && status!=BatchStatus.ABANDONED) {
					restart = true;
				}
			}

			if (job.getJobParametersIncrementer() != null && !restart) {
				jobParameters = job.getJobParametersIncrementer().getNext(jobParameters);
			}

			jobExecution = jobLauncher.run(job, jobParameters);

			if (jobExecution.isRunning()) {
				activeExecutions.add(jobExecution);
			}
		}
		else {
			if(jsrJobOperator != null) {
				jobExecution = new JobExecution(jsrJobOperator.start(jobName, jobParameters.toProperties()));
			}
			else {
				throw new NoSuchJobException(String.format("Unable to find job %s to launch",
						String.valueOf(jobName)));
			}
		}

		return jobExecution;
	}

	@Override
	public JobParameters getLastJobParameters(String jobName) throws NoSuchJobException {

		Collection<JobExecution> executions = jobExecutionDao.getJobExecutions(jobName, 0, 1);

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
		Collection<String> jobNames = new LinkedHashSet<String>(jobLocator.getJobNames());
		jobNames.addAll(getJsrJobNames());
		if (start + count > jobNames.size()) {
			jobNames.addAll(jobInstanceDao.getJobNames());
		}
		if (start >= jobNames.size()) {
			start = jobNames.size();
		}
		if (start + count >= jobNames.size()) {
			count = jobNames.size() - start;
		}
		return new ArrayList<String>(jobNames).subList(start, start + count);
	}

	private Collection<String> getJsrJobNames() {

		Set<String> jsr352JobNames = new HashSet<String>();

		try {
			PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
			Resource[] resources = pathMatchingResourcePatternResolver.getResources("classpath*:/META-INF/batch-jobs/**/*.xml");

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
		Collection<String> names = new HashSet<String>(jobLocator.getJobNames());
		names.addAll(jobInstanceDao.getJobNames());
		return names.size();
	}

	@Override
	public int stopAll() {
		Collection<JobExecution> result = jobExecutionDao.getRunningJobExecutions();
		Collection<String> jsrJobNames = getJsrJobNames();

		for (JobExecution jobExecution : result) {
			if(jsrJobOperator != null && jsrJobNames.contains(jobExecution.getJobInstance().getJobName())) {
				jsrJobOperator.stop(jobExecution.getId());
			}
			else {
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

		if(jsrJobOperator != null && jsrJobNames.contains(jobExecution.getJobInstance().getJobName())) {
			jsrJobOperator.stop(jobExecutionId);
			jobExecution = getJobExecution(jobExecutionId);
		}
		else {
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
		if(jsrJobOperator != null && jsrJobNames.contains(jobInstance.getJobName())) {
			jsrJobOperator.abandon(jobExecutionId);
			jobExecution = getJobExecution(jobExecutionId);
		}
		else {
			jobExecution.upgradeStatus(BatchStatus.ABANDONED);
			jobExecution.setEndTime(new Date());
			jobRepository.update(jobExecution);
		}

		return jobExecution;

	}

	@Override
	public int countJobExecutionsForJob(String name) throws NoSuchJobException {
		checkJobExists(name);
		return jobExecutionDao.countJobExecutions(name);
	}

	@Override
	public int countJobInstances(String name) throws NoSuchJobException {
		return jobInstanceDao.countJobInstances(name);
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
		List<JobExecution> jobExecutions = jobExecutionDao.findJobExecutions(jobInstanceDao
				.getJobInstance(jobInstanceId));
		for (JobExecution jobExecution : jobExecutions) {
			stepExecutionDao.addStepExecutions(jobExecution);
		}
		return jobExecutions;
	}

	@Override
	public StepExecution getStepExecution(Long jobExecutionId, Long stepExecutionId)
			throws NoSuchJobExecutionException, NoSuchStepExecutionException {
		JobExecution jobExecution = getJobExecution(jobExecutionId);
		StepExecution stepExecution = stepExecutionDao.getStepExecution(jobExecution, stepExecutionId);
		if (stepExecution == null) {
			throw new NoSuchStepExecutionException("There is no StepExecution with jobExecutionId=" + jobExecutionId
					+ " and id=" + stepExecutionId);
		}
		try {
			stepExecution.setExecutionContext(executionContextDao.getExecutionContext(stepExecution));
		}
		catch (Exception e) {
			logger.info("Cannot load execution context for step execution: " + stepExecution);
		}
		return stepExecution;
	}

	@Override
	public Collection<JobExecution> listJobExecutionsForJob(String jobName, int start, int count)
			throws NoSuchJobException {
		checkJobExists(jobName);
		List<JobExecution> jobExecutions = jobExecutionDao.getJobExecutions(jobName, start, count);
		for (JobExecution jobExecution : jobExecutions) {
			stepExecutionDao.addStepExecutions(jobExecution);
		}
		return jobExecutions;
	}

	@Override
	public Collection<JobExecutionWithStepCount> listJobExecutionsForJobWithStepCount(String jobName, int start, int count)
			throws NoSuchJobException {
		checkJobExists(jobName);
		List<JobExecutionWithStepCount> jobExecutions = jobExecutionDao.getJobExecutionsWithStepCount(jobName, start, count);
		return jobExecutions;
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
	public Collection<String> getStepNamesForJob(String jobName) throws NoSuchJobException {
		try {
			Job job = jobLocator.getJob(jobName);
			if (job instanceof StepLocator) {
				return ((StepLocator) job).getStepNames();
			}
		}
		catch (NoSuchJobException e) {
			// ignore
		}
		Collection<String> stepNames = new LinkedHashSet<String>();
		for (JobExecution jobExecution : listJobExecutionsForJob(jobName, 0, 100)) {
			for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
				stepNames.add(stepExecution.getStepName());
			}
		}
		return Collections.unmodifiableList(new ArrayList<String>(stepNames));
	}

	private void checkJobExists(String jobName) throws NoSuchJobException {
		if(getJsrJobNames().contains(jobName)) {
			return;
		}
		if (jobLocator.getJobNames().contains(jobName)) {
			return;
		}
		if (jobInstanceDao.countJobInstances(jobName) > 0) {
			return;
		}
		throw new NoSuchJobException("No Job with that name either current or historic: [" + jobName + "]");
	}

	/**
	 * Stop all the active jobs and wait for them (up to a time out) to finish
	 * processing.
	 */
	@Override
	public void destroy() throws Exception {

		Exception firstException = null;

		for (JobExecution jobExecution : activeExecutions) {
			try {
				if (jobExecution.isRunning()) {
					stop(jobExecution.getId());
				}
			}
			catch (JobExecutionNotRunningException e) {
				logger.info("JobExecution is not running so it cannot be stopped");
			}
			catch (Exception e) {
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
	 * Check all the active executions and see if they are still actually
	 * running. Remove the ones that have completed.
	 */
	@Scheduled(fixedDelay = 60000)
	public void removeInactiveExecutions() {

		for (Iterator<JobExecution> iterator = activeExecutions.iterator(); iterator.hasNext();) {
			JobExecution jobExecution = iterator.next();
			try {
				jobExecution = getJobExecution(jobExecution.getId());
			}
			catch (NoSuchJobExecutionException e) {
				logger.error("Unexpected exception loading JobExecution", e);
			}
			if (!jobExecution.isRunning()) {
				iterator.remove();
			}
		}

	}

}
