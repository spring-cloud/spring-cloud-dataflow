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

import java.time.LocalDateTime;

import org.springframework.cloud.dataflow.core.ThinTaskExecution;
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
	private LocalDateTime startTime;

	/**
	 * Timestamp of when the task was completed/terminated.
	 */
	private LocalDateTime endTime;

	/**
	 * Message returned from the task or stacktrace.
	 */
	private String exitMessage;

	private String externalExecutionId;


	private String errorMessage;

	private String composedTaskJobExecutionStatus;

	public TaskExecutionThinResource() {
	}

	public TaskExecutionThinResource(ThinTaskExecution taskExecution) {
		this.executionId = taskExecution.getExecutionId();
		this.taskName = taskExecution.getTaskName();
		this.externalExecutionId = taskExecution.getExternalExecutionId();
		this.parentExecutionId =taskExecution.getParentExecutionId();
		this.startTime = taskExecution.getStartTime();
		this.endTime = taskExecution.getEndTime();
		this.exitCode = taskExecution.getExitCode();
		this.exitMessage = taskExecution.getExitMessage();
		this.errorMessage = taskExecution.getErrorMessage();
		this.composedTaskJobExecutionStatus = taskExecution.getCtrTaskStatus();
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

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
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

	public String getComposedTaskJobExecutionStatus() {
		return composedTaskJobExecutionStatus;
	}

	public void setComposedTaskJobExecutionStatus(String composedTaskJobExecutionStatus) {
		this.composedTaskJobExecutionStatus = composedTaskJobExecutionStatus;
	}

	public TaskExecutionStatus getTaskExecutionStatus() {
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
