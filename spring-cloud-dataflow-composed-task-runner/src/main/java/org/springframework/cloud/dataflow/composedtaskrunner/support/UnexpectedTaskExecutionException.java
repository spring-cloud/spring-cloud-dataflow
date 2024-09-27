/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner.support;

import java.time.LocalDateTime;

import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.cloud.task.repository.TaskExecution;

/**
 * Creates a {@link UnexpectedTaskExecutionException} which extends {@link UnsupportedOperationException}, but
 * also contains the exitCode as information.
 *
 * @author Tobias Soloschenko
 */
public class UnexpectedTaskExecutionException extends UnexpectedJobExecutionException implements ExitCodeGenerator {

	private static final long serialVersionUID = 1080992679855603656L;

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
	private Integer exitCode = -1;

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

	/**
	 * Id assigned to the task by the platform.
	 */
	private String externalExecutionId;

	/**
	 * Error information available upon the failure of a task.
	 */
	private String errorMessage;

	/**
	 * Constructs an UnexpectedTaskExecutionException with the specified
	 * detail message.
	 *
	 * @param message the detail message
	 */
	public UnexpectedTaskExecutionException(String message) {
		super(message);
	}

	/**
	 * Constructs an UnexpectedTaskExecutionException with the specified
	 * detail message, cause and exitCode.
	 *
	 * @param message the detail message
	 * @param cause   the cause which leads to this exception
	 */
	public UnexpectedTaskExecutionException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs an UnexpectedTaskExecutionException with the specified
	 * detail message and taskExecution.
	 *
	 * @param message       the detail message
	 * @param taskExecution the taskExecution of the task
	 */
	public UnexpectedTaskExecutionException(String message, TaskExecution taskExecution) {
		this(message);
		assignTaskExecutionFields(taskExecution);
	}

	/**
	 * Constructs an UnexpectedTaskExecutionException with the specified
	 * detail message, cause and taskExecution.
	 *
	 * @param message       the detail message
	 * @param cause         the cause which leads to this exception
	 * @param taskExecution the taskExecution of the task
	 */
	public UnexpectedTaskExecutionException(String message, Throwable cause, TaskExecution taskExecution) {
		this(message, cause);
		assignTaskExecutionFields(taskExecution);
	}

	/**
	 * Assigns the task execution fields to this exception.
	 *
	 * @param taskExecution the task execution of which the fields should be assigned to this exception
	 */
	private void assignTaskExecutionFields(TaskExecution taskExecution) {
		if(taskExecution != null) {
			executionId = taskExecution.getExecutionId();
			parentExecutionId = taskExecution.getParentExecutionId();
			exitCode = taskExecution.getExitCode();
			taskName = taskExecution.getTaskName();
			startTime = taskExecution.getStartTime();
			endTime = taskExecution.getEndTime();
			externalExecutionId = taskExecution.getExternalExecutionId();
			errorMessage = taskExecution.getErrorMessage();
			exitMessage = taskExecution.getExitMessage();
		}
	}

	public long getExecutionId() {
		return this.executionId;
	}

	/**
	 * Returns the exit code of the task.
	 *
	 * @return the exit code or -1 if the exit code couldn't be determined
	 */
	@Override
	public int getExitCode() {
		return this.exitCode;
	}

	public String getTaskName() {
		return this.taskName;
	}

	public LocalDateTime getStartTime() {
		return (this.startTime != null) ? this.startTime: null;
	}

	public LocalDateTime getEndTime() {
		return (this.endTime != null) ? this.endTime : null;
	}

	public String getExitMessage() {
		return this.exitMessage;
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	public String getExternalExecutionId() {
		return this.externalExecutionId;
	}

	public Long getParentExecutionId() {
		return this.parentExecutionId;
	}

}