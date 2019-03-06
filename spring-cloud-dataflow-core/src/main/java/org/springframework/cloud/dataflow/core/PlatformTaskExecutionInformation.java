/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

/**
 * @author David Turanski
 **/
public class PlatformTaskExecutionInformation {

	/**
	 * The platform instance (account) name.
	 */
	private final String name;

	/**
	 * The platform type name.
	 */
	private final String type;

	/**
	 * The maximum concurrently running task executions allowed.
	 */
	private final int maximumTaskExecutions;

	/**
	 * The current number of running task executions.
	 */
	private final int runningExecutionCount;

	public PlatformTaskExecutionInformation(String name, String type, int maximumTaskExecutions,
			int runningExecutionCount) {
		this.name = name;
		this.type = type;
		this.maximumTaskExecutions = maximumTaskExecutions;
		this.runningExecutionCount = runningExecutionCount;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public int getMaximumTaskExecutions() {
		return maximumTaskExecutions;
	}

	public int getRunningExecutionCount() {
		return runningExecutionCount;
	}
}
