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

package org.springframework.cloud.dataflow.rest.resource;

import java.text.DateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.TimeZone;

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
 * A HATEOAS representation of a JobExecution.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
public class JobExecutionResource extends RepresentationModel<JobExecutionResource> {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;

	private DateTimeFormatter timeFormat = DateTimeFormatter.ISO_LOCAL_TIME;

	private Long executionId;

	private int stepExecutionCount;

	private Long jobId;

	private Long taskExecutionId;

	private String name;

	private String startDate = "";

	private String startTime = "";

	private String duration = "";

	private JobExecution jobExecution;

	private Properties jobParameters;

	private String jobParametersString;

	private boolean restartable = false;

	private boolean abandonable = false;

	private boolean stoppable = false;

	private boolean defined;

	private JobParametersConverter converter = new DefaultJobParametersConverter();

	private TimeZone timeZone;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	/**
	 * Default constructor to be used by Jackson.
	 */
	@SuppressWarnings("unused")
	private JobExecutionResource() {

	}

	public JobExecutionResource(TaskJobExecution taskJobExecution, TimeZone timeZone) {
		Assert.notNull(taskJobExecution, "taskJobExecution must not be null");
		this.taskExecutionId = taskJobExecution.getTaskId();
		this.jobExecution = taskJobExecution.getJobExecution();
		this.timeZone = timeZone;
		this.executionId = jobExecution.getId();
		this.jobId = jobExecution.getJobId();
		this.stepExecutionCount = taskJobExecution.getStepExecutionCount();
		this.jobParameters = converter.getProperties(jobExecution.getJobParameters());
		this.jobParametersString = fromJobParameters(
				this.argumentSanitizer.sanitizeJobParameters(jobExecution.getJobParameters()));
		this.defined = taskJobExecution.isTaskDefined();
		JobInstance jobInstance = jobExecution.getJobInstance();
		if (jobInstance != null) {
			this.name = jobInstance.getJobName();
			this.restartable = JobUtils.isJobExecutionRestartable(jobExecution);
			this.abandonable = JobUtils.isJobExecutionAbandonable(jobExecution);
			this.stoppable = JobUtils.isJobExecutionStoppable(jobExecution);
		} else {
			this.name = "?";
		}

		if (jobExecution.getStartTime() != null) {
			// We assume the startTime is date time from current timezone.
			// if the timezone provided is different from the current we have to assume we need a representation in that timezone.
			this.startDate = dateFormat.format(ZonedDateTime.of(jobExecution.getStartTime(), TimeZone.getDefault().toZoneId()).withZoneSameInstant(timeZone.toZoneId()));
			this.startTime = timeFormat.format(ZonedDateTime.of(jobExecution.getStartTime(), TimeZone.getDefault().toZoneId()).withZoneSameInstant(timeZone.toZoneId()));
			// We assume start time and end time are from current timezone.
			LocalDateTime endTime = jobExecution.getEndTime() != null ? jobExecution.getEndTime() : LocalDateTime.now();
			this.duration = String.valueOf(Duration.between(jobExecution.getStartTime(), endTime));
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

	public JobExecution getJobExecution() {
		return jobExecution;
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

	public long getTaskExecutionId() {
		return taskExecutionId;
	}

	public boolean isDefined() {
		return defined;
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

	public static class Page extends PagedModel<JobExecutionResource> {
	}
}
