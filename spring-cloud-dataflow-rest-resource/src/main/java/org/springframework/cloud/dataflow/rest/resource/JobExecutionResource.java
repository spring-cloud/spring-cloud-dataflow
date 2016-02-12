/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.rest.resource;

import java.util.Date;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;

/**
 * A HATEOAS representation of a JobExecution.
 *
 * @author Glenn Renfro
 */
public class JobExecutionResource extends ResourceSupport {

	/**
	 * The unique id  associated with the task execution.
	 */
	private final long taskExecutionId;

	/**
	 * The unique id  associated with the job execution.
	 */
	private final long jobExecutionId;


	/**
	 * The recorded batch status for the Job execution.
	 */
	private final BatchStatus batchStatus;

	/**
	 * Time of when the Job was started.
	 */
	private final Date startTime;

	/**
	 * Timestamp of when the Job was completed/terminated.
	 */
	private final Date endTime;

	/**
	 * ExitStatus returned from the Job or stacktrace.parameters.
	 */
	private final ExitStatus exitStatus;

	/**
	 * The parameters that were used for this job execution.
	 */
	private final JobParameters parameters;

	/**
	 * The name associated with the job.
	 */
	private final String jobName;

	public JobExecutionResource(long taskExecutionId, JobExecution jobExecution) {
		Assert.notNull(jobExecution, "jobExecution must not be null");
		this.taskExecutionId = taskExecutionId;
		this.jobExecutionId = jobExecution.getId();
		this.batchStatus = jobExecution.getStatus();
		this.startTime = jobExecution.getStartTime();
		this.endTime = jobExecution.getEndTime();
		this.exitStatus = jobExecution.getExitStatus();
		this.parameters = jobExecution.getJobParameters();
		this.jobName = jobExecution.getJobInstance().getJobName();
	}

	public JobExecutionResource(String jobName, TaskJobExecution taskJobExecution) {
		Assert.notNull(taskJobExecution, "taskJobExecution must not be null");
		this.taskExecutionId = taskJobExecution.getTaskId();
		this.jobExecutionId = taskJobExecution.getJobExecution().getId();
		this.batchStatus = taskJobExecution.getJobExecution().getStatus();
		this.startTime = taskJobExecution.getJobExecution().getStartTime();
		this.endTime = taskJobExecution.getJobExecution().getEndTime();
		this.exitStatus = taskJobExecution.getJobExecution().getExitStatus();
		this.parameters = taskJobExecution.getJobExecution().getJobParameters();
		this.jobName = jobName;
	}

	public JobParameters getParameters() {
		return parameters;
	}

	public BatchStatus getBatchStatus() {
		return batchStatus;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public ExitStatus getExitStatus() {
		return exitStatus;
	}

	public long getTaskExecutionId() {
		return taskExecutionId;
	}

	public long getJobExecutionId() {
		return jobExecutionId;
	}

	public String getJobName() {
		return jobName;
	}
}
