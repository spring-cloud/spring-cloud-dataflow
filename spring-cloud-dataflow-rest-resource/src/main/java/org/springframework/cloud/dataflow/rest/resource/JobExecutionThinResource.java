/*
 * Copyright 2018-2023 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource;

import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.support.JobUtils;
import org.springframework.cloud.dataflow.rest.job.support.TimeUtils;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.util.Assert;

/**
 * A HATEOAS representation of a JobExecution without the StepExecutions.
 *
 * @author Glenn Renfro
 * @author Corneil du Plessis
 *
 * @since 2.0
 */
public class JobExecutionThinResource extends RepresentationModel<JobExecutionThinResource> {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private static final DateFormat dateFormat = TimeUtils.getDefaultDateFormat();

	private static final DateFormat timeFormat = TimeUtils.getDefaultTimeFormat();

	private static final DateFormat durationFormat = TimeUtils.getDefaultDurationFormat();

	private Long executionId;

	private int stepExecutionCount;

	private Long jobId;

	private Long taskExecutionId;

	private Long instanceId;

	private String name;

	private String startDate = "";

	private String startTime = "";

	private Date startDateTime = null;

	private String duration = "";

	private Properties jobParameters;

	private String jobParametersString;

	private boolean restartable = false;

	private boolean abandonable = false;

	private boolean stoppable = false;

	private boolean defined;

	private JobParametersConverter converter = new DefaultJobParametersConverter();

	private TimeZone timeZone;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	private BatchStatus status;

	private String schemaTarget;

	/**
	 * Default constructor to be used by Jackson.
	 */
	@SuppressWarnings("unused")
	private JobExecutionThinResource() {

	}

	public JobExecutionThinResource(TaskJobExecution taskJobExecution, TimeZone timeZone) {
		Assert.notNull(taskJobExecution, "taskJobExecution must not be null");
		this.taskExecutionId = taskJobExecution.getTaskId();
		JobExecution jobExecution = taskJobExecution.getJobExecution();
		this.timeZone = timeZone;
		this.executionId = jobExecution.getId();
		this.jobId = jobExecution.getJobId();
		this.schemaTarget = taskJobExecution.getSchemaTarget();
		this.stepExecutionCount = taskJobExecution.getStepExecutionCount();
		this.jobParameters =converter.getProperties(jobExecution.getJobParameters());
		this.jobParametersString = fromJobParameters(
				this.argumentSanitizer.sanitizeJobParameters(jobExecution.getJobParameters()));
		this.defined = taskJobExecution.isTaskDefined();
		JobInstance jobInstance = jobExecution.getJobInstance();
		this.status = taskJobExecution.getJobExecution().getStatus();
		if (jobInstance != null) {
			this.name = jobInstance.getJobName();
			this.restartable = JobUtils.isJobExecutionRestartable(jobExecution);
			this.abandonable = JobUtils.isJobExecutionAbandonable(jobExecution);
			this.stoppable = JobUtils.isJobExecutionStoppable(jobExecution);
			this.instanceId = jobExecution.getJobInstance().getInstanceId();
		}
		else {
			this.name = "?";
		}

		// Duration is always in GMT
		durationFormat.setTimeZone(TimeUtils.getJvmTimeZone());
		// The others can be localized
		timeFormat.setTimeZone(timeZone);
		dateFormat.setTimeZone(timeZone);
		if (jobExecution.getStartTime() != null) {
			this.startDate = dateFormat.format(jobExecution.getStartTime());
			this.startTime = timeFormat.format(jobExecution.getStartTime());
			Date endTime = jobExecution.getEndTime() != null ? jobExecution.getEndTime() : new Date();
			this.duration = durationFormat.format(new Date(endTime.getTime() - jobExecution.getStartTime().getTime()));
			this.startDateTime = jobExecution.getStartTime();
		}

	}

	public TimeZone getTimeZone() {
		return timeZone;
	}

	public String getName() {
		return name;
	}

	public Long getExecutionId() {
		return executionId;
	}

	public int getStepExecutionCount() {
		return stepExecutionCount;
	}

	public Long getJobId() {
		return jobId;
	}

	public String getStartDate() {
		return startDate;
	}

	public String getStartTime() {
		return startTime;
	}

	public String getDuration() {
		return duration;
	}

	public Long getInstanceId() {
		return instanceId;
	}

	public boolean isRestartable() {
		return restartable;
	}

	public boolean isAbandonable() {
		return abandonable;
	}

	public boolean isStoppable() {
		return stoppable;
	}

	public String getJobParametersString() {
		return jobParametersString;
	}

	public Properties getJobParameters() {
		return jobParameters;
	}

	public BatchStatus getStatus() {
		return status;
	}

	public Date getStartDateTime() {
		return startDateTime;
	}

	public long getTaskExecutionId() {
		return taskExecutionId;
	}

	public boolean isDefined() {
		return defined;
	}

	public String getSchemaTarget() {
		return schemaTarget;
	}

	/**
	 * @param oldParameters the latest job parameters
	 * @return a String representation for rendering the job parameters from the last
	 * instance
	 */
	private String fromJobParameters(JobParameters oldParameters) {

		String properties = PropertiesConverter.propertiesToString(converter.getProperties(oldParameters));
		if (properties.startsWith("#")) {
			properties = properties.substring(properties.indexOf(LINE_SEPARATOR) + LINE_SEPARATOR.length());
		}
		properties = properties.replace("\\:", ":");
		return properties;

	}

	public static class Page extends PagedModel<JobExecutionThinResource> {
	}
}
