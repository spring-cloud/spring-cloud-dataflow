/*
 * Copyright 2016-2021 the original author or authors.
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.JobExecution;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecutionRel;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A HATEOAS representation of a TaskExecution.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
public class TaskExecutionResource extends RepresentationModel<TaskExecutionResource> {

	/**
	 * The unique id associated with the task execution.
	 */
	private long executionId;

	/**
	 * The recorded exit code for the task.
	 */
	private Integer exitCode;

	/**
	 * User defined name for the task.
	 */
	private String taskName;

	/**
	 * Time of when the task was started.
	 */
	private LocalDateTime startTime;

	/**
	 * Timestamp of when the task was completed/terminated.
	 */
	private LocalDateTime endTime;

	/**
	 * Message returned from the task.
	 */
	private String exitMessage;

	/**
	 * The command line arguments that were used for this task execution.
	 */
	private List<String> arguments;

	/**
	 * List of {@link JobExecution}s that are associated with this task.
	 */
	private List<Long> jobExecutionIds;

	/**
	 * Error Message returned from a task execution.
	 */
	private String errorMessage;

	/**
	 * Task Execution ID that is set from an external source. i.e. CloudFoundry Deployment
	 * Id.
	 */
	private String externalExecutionId;

	/**
	 * This Task Execution might be a child task as part of a composed
	 * task and have a parent task execution id. This property can be null.
	 */
	private Long parentExecutionId;

	/**
	 * The resource that defines the task.
	 */
	private String resourceUrl;

	/**
	 * The application arguments, typically set via SPRING_APPLICATION_JSON env-var on deployment
	 */
	private Map<String, String> appProperties;

	private Map<String, String> deploymentProperties;

	/**
	 * The platform for which the task was executed.
	 */
	private String platformName;

	private String taskExecutionStatus;

	private String composedTaskJobExecutionStatus;

	public TaskExecutionResource() {
		arguments = new ArrayList<>();
	}

	/**
	 * Constructor to initialize the TaskExecutionResource using
	 * {@link TaskJobExecutionRel}.
	 *
	 * @param taskJobExecutionRel contains the {@link TaskExecution} but also a list of
	 * the Job ExecutionIds that were associated with this task if applicable.
	 */
	public TaskExecutionResource(TaskJobExecutionRel taskJobExecutionRel) {
		Assert.notNull(taskJobExecutionRel, "taskJobExecutionDTO must not be null");
		this.executionId = taskJobExecutionRel.getTaskExecution().getExecutionId();
		this.parentExecutionId = taskJobExecutionRel.getTaskExecution().getParentExecutionId();
		this.exitCode = taskJobExecutionRel.getTaskExecution().getExitCode();
		this.taskName = taskJobExecutionRel.getTaskExecution().getTaskName();
		this.exitMessage = taskJobExecutionRel.getTaskExecution().getExitMessage();
		this.arguments = Collections.unmodifiableList(taskJobExecutionRel.getTaskExecution().getArguments());
		this.startTime = taskJobExecutionRel.getTaskExecution().getStartTime();
		this.endTime = taskJobExecutionRel.getTaskExecution().getEndTime();
		this.errorMessage = taskJobExecutionRel.getTaskExecution().getErrorMessage();
		this.externalExecutionId = taskJobExecutionRel.getTaskExecution().getExternalExecutionId();
		if(taskJobExecutionRel.getTaskManifest() != null) {
			this.platformName = taskJobExecutionRel.getTaskManifest().getPlatformName();
		}
		if (taskJobExecutionRel.getJobExecutionIds() == null) {
			this.jobExecutionIds = Collections.emptyList();
		}
		else {
			this.jobExecutionIds = Collections
					.unmodifiableList(new ArrayList<>(taskJobExecutionRel.getJobExecutionIds()));
		}
		if (taskJobExecutionRel.getTaskManifest() != null) {
			this.resourceUrl = taskJobExecutionRel.getTaskManifest().getTaskDeploymentRequest().getResource().toString();
			this.appProperties = taskJobExecutionRel.getTaskManifest().getTaskDeploymentRequest().getDefinition().getProperties();
			this.deploymentProperties = taskJobExecutionRel.getTaskManifest().getTaskDeploymentRequest().getDeploymentProperties();
		}
		if(taskJobExecutionRel.getTaskManifest() != null) {
			this.platformName = taskJobExecutionRel.getTaskManifest().getPlatformName();
		}
		this.composedTaskJobExecutionStatus = (taskJobExecutionRel.getComposedTaskJobExecution() != null) ?
				taskJobExecutionRel.getComposedTaskJobExecution().getJobExecution().getExitStatus().getExitCode() :
				null;
	}

	/**
	 * Constructor to initialize the TaskExecutionResource using a
	 * {@link TaskExecution}.
	 *
	 * @param taskExecution contains the {@link TaskExecution}
	 * @param composedTaskJobExecution the optional composed task execution.
	 */
	public TaskExecutionResource(TaskExecution taskExecution, TaskJobExecution composedTaskJobExecution) {
		Assert.notNull(taskExecution, "taskExecution must not be null");
		this.executionId = taskExecution.getExecutionId();
		this.exitCode = taskExecution.getExitCode();
		this.taskName = taskExecution.getTaskName();
		this.exitMessage = taskExecution.getExitMessage();
		this.arguments = Collections.unmodifiableList(taskExecution.getArguments());
		this.startTime = taskExecution.getStartTime();
		this.endTime = taskExecution.getEndTime();
		this.errorMessage = taskExecution.getErrorMessage();
		this.externalExecutionId = taskExecution.getExternalExecutionId();
		this.composedTaskJobExecutionStatus = (composedTaskJobExecution != null) ?
				composedTaskJobExecution.getJobExecution().getExitStatus().getExitCode() :
				null;
	}

	/**
	 * Constructor to initialize the TaskExecutionResource using a
	 * {@link TaskExecution} and {@link TaskManifest}.
	 *
	 * @param taskExecution contains the {@link TaskExecution}
	 * @param taskManifest contains the (@link TaskManifest}
	 * @param composedTaskJobExecution The optional composed task execution.
	 */
	public TaskExecutionResource(TaskExecution taskExecution, TaskManifest taskManifest, TaskJobExecution composedTaskJobExecution) {
		Assert.notNull(taskExecution, "taskExecution must not be null");
		Assert.notNull(taskManifest, "taskManifest must not be null");
		this.executionId = taskExecution.getExecutionId();
		this.exitCode = taskExecution.getExitCode();
		this.taskName = taskExecution.getTaskName();
		this.exitMessage = taskExecution.getExitMessage();
		this.arguments = Collections.unmodifiableList(taskExecution.getArguments());
		this.startTime = taskExecution.getStartTime();
		this.endTime = taskExecution.getEndTime();
		this.errorMessage = taskExecution.getErrorMessage();
		this.externalExecutionId = taskExecution.getExternalExecutionId();
		this.resourceUrl = taskManifest.getTaskDeploymentRequest().getResource().toString();
		this.appProperties = taskManifest.getTaskDeploymentRequest().getDefinition().getProperties();
		this.deploymentProperties = taskManifest.getTaskDeploymentRequest().getDeploymentProperties();
		this.composedTaskJobExecutionStatus = (composedTaskJobExecution != null) ?
				composedTaskJobExecution.getJobExecution().getExitStatus().getExitCode() :
				null;
	}

	public long getExecutionId() {
		return executionId;
	}

	/**
	 * @return the int containing the exit code of the task application upon completion.
	 * Default is 0.
	 */
	public Integer getExitCode() {
		return exitCode;
	}

	public String getTaskName() {
		return taskName;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public String getExitMessage() {
		return exitMessage;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public List<Long> getJobExecutionIds() {
		return jobExecutionIds;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public String getExternalExecutionId() {
		return externalExecutionId;
	}

	public Long getParentExecutionId() {
		return parentExecutionId;
	}

	public String getResourceUrl() {
		return resourceUrl;
	}

	public Map<String, String> getAppProperties() {
		return appProperties;
	}

	public Map<String, String> getDeploymentProperties() {
		return deploymentProperties;
	}

	public String getPlatformName() {
		return platformName;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	public void setTaskExecutionStatus(String taskExecutionStatus) {
		this.taskExecutionStatus = taskExecutionStatus;
	}

	/**
	 * Returns the calculated status of this {@link TaskExecution}.
	 *
	 * If {@link #startTime} is
	 * null, the {@link TaskExecution} is considered to be not running (never executed).
	 *
	 * If {@link #endTime} is
	 * null, the {@link TaskExecution} is considered to be still running:
	 * {@link TaskExecutionStatus#RUNNING}. If the {@link #endTime} is defined and the
	 * {@link #exitCode} is non-zero, an status of {@link TaskExecutionStatus#ERROR} is assumed,
	 * if {@link #exitCode} is zero, {@link TaskExecutionStatus#COMPLETE} is returned.
	 *
	 * @return TaskExecutionStatus, never null
	 */
	public TaskExecutionStatus getTaskExecutionStatus() {
		if (StringUtils.hasText(this.taskExecutionStatus)) {
			return TaskExecutionStatus.valueOf(this.taskExecutionStatus);
		}
		if (this.startTime == null) {
			return TaskExecutionStatus.UNKNOWN;
		}
		if (this.endTime == null) {
			return TaskExecutionStatus.RUNNING;
		}
		else {
			if (this.composedTaskJobExecutionStatus != null) {
				return (this.composedTaskJobExecutionStatus.equals("ABANDONED") ||
						this.composedTaskJobExecutionStatus.equals("FAILED") ||
						this.composedTaskJobExecutionStatus.equals("STOPPED")) ?
						TaskExecutionStatus.ERROR : TaskExecutionStatus.COMPLETE;
			}

			return (this.exitCode == null) ? TaskExecutionStatus.RUNNING :
					((this.exitCode == 0) ? TaskExecutionStatus.COMPLETE : TaskExecutionStatus.ERROR);
		}
	}

	public static class Page extends PagedModel<TaskExecutionResource> {
	}
}
