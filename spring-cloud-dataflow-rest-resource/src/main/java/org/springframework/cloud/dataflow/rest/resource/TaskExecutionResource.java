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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;

/**
 * A HATEOAS representation of a TaskExecution.
 *
 * @author Glenn Renfro
 */
public class TaskExecutionResource extends ResourceSupport {

	/**
	 * The unique id  associated with the task execution.
	 */
	private long executionId;

	/**
	 * Id provided by an external system for the given task execution.
	 */
	private String externalExecutionID;

	/**
	 * The recorded exit code for the task.
	 */
	private int exitCode;

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
	 * The status code associated with the task execution.
	 */
	private String statusCode;

	/**
	 * Message returned from the task or stacktrace.parameters.
	 */
	private String exitMessage;

	/**
	 * The parameters that were used for this task execution.
	 */
	private List<String> parameters;

	public TaskExecutionResource() {
		parameters = new ArrayList<>();
	}

	public TaskExecutionResource(long executionId, int exitCode, String taskName,
								 Date startTime, Date endTime, String statusCode,
								 String exitMessage, List<String> parameters,
								 String externalExecutionID) {

		Assert.notNull(parameters, "parameters must not be null");
		Assert.notNull(startTime, "startTime must not be null");
		this.executionId = executionId;
		this.externalExecutionID = externalExecutionID;
		this.exitCode = exitCode;
		this.taskName = taskName;
		this.statusCode = statusCode;
		this.exitMessage = exitMessage;
		this.parameters = parameters;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public static class Page extends PagedResources<TaskExecutionResource> {}

	public long getExecutionId() {
		return executionId;
	}

	public void setExecutionId(long executionId) {
		this.executionId = executionId;
	}

	public String getExternalExecutionID() {
		return externalExecutionID;
	}

	public void setExternalExecutionID(String externalExecutionID) {
		this.externalExecutionID = externalExecutionID;
	}

	public int getExitCode() {
		return exitCode;
	}

	public String getTaskName() {
		return taskName;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public String getExitMessage() {
		return exitMessage;
	}

	public List<String> getParameters() {
		return parameters;
	}

}
