/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.cloud.dataflow.schema;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.util.Assert;

/**
 * Contains the attributes of a {@link TaskExecution} as well as the name of the {@link SchemaVersionTarget}.
 *
 * @author Corneil du Plessis
 */
public class AggregateTaskExecution {
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

	/**
	 * Id assigned to the task by the platform.
	 *
	 * @since 1.1.0
	 */
	private String externalExecutionId;

	/**
	 * Error information available upon the failure of a task.
	 *
	 * @since 1.1.0
	 */
	private String errorMessage;

	private String schemaTarget;

	private String platformName;

	private String ctrTaskStatus;
	/**
	 * The arguments that were used for this task execution.
	 */
	private List<String> arguments;

	public AggregateTaskExecution() {
		this.arguments = new ArrayList<>();
	}

	public AggregateTaskExecution(long executionId, Integer exitCode, String taskName,
								  Date startTime, Date endTime, String exitMessage, List<String> arguments,
								  String errorMessage, String externalExecutionId, Long parentExecutionId, String platformName,
								  String ctrTaskStatus, String schemaTarget) {

		Assert.notNull(arguments, "arguments must not be null");
		this.executionId = executionId;
		this.exitCode = exitCode;
		this.taskName = taskName;
		this.exitMessage = exitMessage;
		this.arguments = new ArrayList<>(arguments);
		this.startTime = (startTime != null) ? (Date) startTime.clone() : null;
		this.endTime = (endTime != null) ? (Date) endTime.clone() : null;
		this.errorMessage = errorMessage;
		this.externalExecutionId = externalExecutionId;
		this.parentExecutionId = parentExecutionId;
		this.schemaTarget = schemaTarget;
		this.platformName = platformName;
		this.ctrTaskStatus = ctrTaskStatus;
	}

	public AggregateTaskExecution(long executionId, Integer exitCode, String taskName,
								  Date startTime, Date endTime, String exitMessage, List<String> arguments,
								  String errorMessage, String externalExecutionId, String platformName, String ctrTaskStatus, String schemaTarget) {

		this(executionId, exitCode, taskName, startTime, endTime, exitMessage, arguments,
				errorMessage, externalExecutionId, null, platformName, ctrTaskStatus, schemaTarget);
	}

	public long getExecutionId() {
		return this.executionId;
	}

	public Integer getExitCode() {
		return this.exitCode;
	}

	public void setExitCode(Integer exitCode) {
		this.exitCode = exitCode;
	}

	public String getTaskName() {
		return this.taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public Date getStartTime() {
		return (this.startTime != null) ? (Date) this.startTime.clone() : null;
	}

	public void setStartTime(Date startTime) {
		this.startTime = (startTime != null) ? (Date) startTime.clone() : null;
	}

	public Date getEndTime() {
		return (this.endTime != null) ? (Date) this.endTime.clone() : null;
	}

	public void setEndTime(Date endTime) {
		this.endTime = (endTime != null) ? (Date) endTime.clone() : null;
	}

	public String getExitMessage() {
		return this.exitMessage;
	}

	public void setExitMessage(String exitMessage) {
		this.exitMessage = exitMessage;
	}

	public List<String> getArguments() {
		return this.arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = new ArrayList<>(arguments);
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getExternalExecutionId() {
		return this.externalExecutionId;
	}

	public void setExternalExecutionId(String externalExecutionId) {
		this.externalExecutionId = externalExecutionId;
	}

	public Long getParentExecutionId() {
		return this.parentExecutionId;
	}

	public void setParentExecutionId(Long parentExecutionId) {
		this.parentExecutionId = parentExecutionId;
	}

	public String getSchemaTarget() {
		return schemaTarget;
	}

	public void setSchemaTarget(String schemaTarget) {
		this.schemaTarget = schemaTarget;
	}

	public String getPlatformName() {
		return platformName;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	public String getCtrTaskStatus() {
		return ctrTaskStatus;
	}

	public void setCtrTaskStatus(String ctrTaskStatus) {
		this.ctrTaskStatus = ctrTaskStatus;
	}

	@Override
	public String toString() {
		return "AggregateTaskExecution{" +
			"executionId=" + executionId +
			", parentExecutionId=" + parentExecutionId +
			", exitCode=" + exitCode +
			", taskName='" + taskName + '\'' +
			", startTime=" + startTime +
			", endTime=" + endTime +
			", exitMessage='" + exitMessage + '\'' +
			", externalExecutionId='" + externalExecutionId + '\'' +
			", errorMessage='" + errorMessage + '\'' +
			", schemaTarget='" + schemaTarget + '\'' +
			", platformName='" + platformName + '\'' +
			", ctrTaskStatus='" + ctrTaskStatus + '\'' +
			", arguments=" + arguments +
			'}';
	}

	public TaskExecution toTaskExecution() {
		return new TaskExecution(executionId,
				exitCode,
				taskName,
				startTime,
				endTime,
				exitMessage,
				arguments,
				errorMessage,
				externalExecutionId,
				parentExecutionId
		);
	}
}
