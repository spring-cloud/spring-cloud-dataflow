/*
 * Copyright 2018 the original author or authors.
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

import org.springframework.hateoas.ResourceSupport;

/**
 *  A HATEOAS representation of a the currently running task executions and server limits.
 *
 * @author David Turanski
 **/
public class CurrentTaskExecutionsResource extends ResourceSupport {

	/**
	 * The maximum concurrently running task executions allowed.
	 */
	private long maximumTaskExecutions = Long.MAX_VALUE;

	/**
	 * The current number of running task executions.
	 */
	private long runningExecutionCount;

	/**
	 *
	 * @return the maximum number of concurrently running task executions allowed.
	 */
	public long getMaximumTaskExecutions() {
		return maximumTaskExecutions;
	}

	/**
	 *
	 * Set the maximum number of concurrently running task executions allowed.
	 */
	public void setMaximumTaskExecutions(long maximumTaskExecutions) {
		this.maximumTaskExecutions = maximumTaskExecutions;
	}

	/**
	 *
	 * @return the current number of running task executions.
	 */
	public long getRunningExecutionCount() {
		return runningExecutionCount;
	}

	/**
	 *
	 * Set the current number of running task executions.
	 */
	public void setRunningExecutionCount(long runningExecutionCount) {
		this.runningExecutionCount = runningExecutionCount;
	}


}
