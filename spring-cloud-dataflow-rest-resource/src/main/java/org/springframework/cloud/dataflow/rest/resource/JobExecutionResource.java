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
import java.util.Date;
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

	private DateFormat dateFormat = TimeUtils.getDefaultDateFormat();

	private DateFormat timeFormat = TimeUtils.getDefaultTimeFormat();

	private DateFormat durationFormat = TimeUtils.getDefaultDurationFormat();

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

	private String schemaTarget;

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
		this.schemaTarget = taskJobExecution.getSchemaTarget();
		this.stepExecutionCount = taskJobExecution.getStepExecutionCount();
		this.jobParameters = converter.getProperties(jobExecution.getJobParameters());
		this.jobParametersString = fromJobParameters(
				this.argumentSanitizer.sanitizeJobParameters(jobExecution.getJobParameters()));
		this.defined = taskJobExecution.isTaskDefined();
		this.schemaTarget = taskJobExecution.getSchemaTarget();
		JobInstance jobInstance = jobExecution.getJobInstance();
		if (jobInstance != null) {
			this.name = jobInstance.getJobName();
			this.restartable = JobUtils.isJobExecutionRestartable(jobExecution);
			this.abandonable = JobUtils.isJobExecutionAbandonable(jobExecution);
			this.stoppable = JobUtils.isJobExecutionStoppable(jobExecution);
		} else {
			this.name = "?";
		}

		durationFormat.setTimeZone(TimeUtils.getJvmTimeZone());

		// The others can be localized
		timeFormat.setTimeZone(timeZone);
		dateFormat.setTimeZone(timeZone);
		if (jobExecution.getStartTime() != null) {
			this.startDate = dateFormat.format(jobExecution.getStartTime());
			this.startTime = timeFormat.format(jobExecution.getStartTime());
			Date endTime = jobExecution.getEndTime() != null ? jobExecution.getEndTime() : new Date();
			this.duration = durationFormat.format(new Date(endTime.getTime() - jobExecution.getStartTime().getTime()));
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

	public static class Page extends PagedModel<JobExecutionResource> {
	}
}
