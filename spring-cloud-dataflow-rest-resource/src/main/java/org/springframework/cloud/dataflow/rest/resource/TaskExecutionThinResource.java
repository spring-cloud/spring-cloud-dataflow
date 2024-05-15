/*
 * Copyright 2024 the original author or authors.
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

import java.util.Date;

import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.util.StringUtils;


/**
 * This resource is a match for AggregateTaskExecution and should satisfy UI paging.
 * @author Corneil du Plessis
 */
public class TaskExecutionThinResource extends RepresentationModel<TaskExecutionThinResource> {
	/**
	 * The unique id associated with the task execution.
	 */
	private long executionId;

	/**
	 * The parent task execution id.
	 */
	private Long parentExecutionId;

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
	private Date startTime;

	/**
	 * Timestamp of when the task was completed/terminated.
	 */
	private Date endTime;

	/**
	 * Message returned from the task or stacktrace.
	 */
	private String exitMessage;

	private String externalExecutionId;


	private String errorMessage;

	private String taskExecutionStatus;

	private String composedTaskJobExecutionStatus;

	/**
	 * @since 2.11.0
	 */

	private String schemaTarget;


	public TaskExecutionThinResource() {
	}

	public TaskExecutionThinResource(AggregateTaskExecution aggregateTaskExecution) {
		this.executionId = aggregateTaskExecution.getExecutionId();
		this.schemaTarget = aggregateTaskExecution.getSchemaTarget();
		this.taskName = aggregateTaskExecution.getTaskName();
		this.externalExecutionId = aggregateTaskExecution.getExternalExecutionId();
		this.parentExecutionId =aggregateTaskExecution.getParentExecutionId();
		this.startTime = aggregateTaskExecution.getStartTime();
		this.endTime = aggregateTaskExecution.getEndTime();
		this.exitCode = aggregateTaskExecution.getExitCode();
		this.exitMessage = aggregateTaskExecution.getExitMessage();
		this.errorMessage = aggregateTaskExecution.getErrorMessage();
		this.composedTaskJobExecutionStatus = aggregateTaskExecution.getCtrTaskStatus();
	}

	public long getExecutionId() {
		return executionId;
	}

	public void setExecutionId(long executionId) {
		this.executionId = executionId;
	}

	public Long getParentExecutionId() {
		return parentExecutionId;
	}

	public void setParentExecutionId(Long parentExecutionId) {
		this.parentExecutionId = parentExecutionId;
	}

	public Integer getExitCode() {
		return exitCode;
	}

	public void setExitCode(Integer exitCode) {
		this.exitCode = exitCode;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public String getExitMessage() {
		return exitMessage;
	}

	public void setExitMessage(String exitMessage) {
		this.exitMessage = exitMessage;
	}

	public String getExternalExecutionId() {
		return externalExecutionId;
	}

	public void setExternalExecutionId(String externalExecutionId) {
		this.externalExecutionId = externalExecutionId;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getSchemaTarget() {
		return schemaTarget;
	}

	public void setSchemaTarget(String schemaTarget) {
		this.schemaTarget = schemaTarget;
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
		if (this.composedTaskJobExecutionStatus != null) {
			return (this.composedTaskJobExecutionStatus.equals("ABANDONED") ||
				this.composedTaskJobExecutionStatus.equals("FAILED") ||
				this.composedTaskJobExecutionStatus.equals("STOPPED")) ?
				TaskExecutionStatus.ERROR : TaskExecutionStatus.COMPLETE;
		}
		return (this.exitCode == null) ? TaskExecutionStatus.RUNNING :
				((this.exitCode == 0) ? TaskExecutionStatus.COMPLETE : TaskExecutionStatus.ERROR);
	}

	public static class Page extends PagedModel<TaskExecutionThinResource> {
	}
}
