/*
 * Copyright 2018-2019 the original author or authors.
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

import org.springframework.cloud.dataflow.core.PlatformTaskExecutionInformation;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;

/**
 *  A HATEOAS representation of a the currently running task executions and server limits.
 *
 * @author David Turanski
 **/
public class CurrentTaskExecutionsResource extends ResourceSupport {

	/**
	 * The platform instance (account) name.
	 */
	private String name;

	/**
	 * The platform type name.
	 */
	private String type;

	/**
	 * The maximum concurrently running task executions allowed.
	 */
	private int maximumTaskExecutions;

	/**
	 * The current number of running task executions.
	 */
	private int runningExecutionCount;


	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public int getMaximumTaskExecutions() {
		return maximumTaskExecutions;
	}
	public void setMaximumTaskExecutions(int maximumTaskExecutions) {
		this.maximumTaskExecutions = maximumTaskExecutions;
	}
	public int getRunningExecutionCount() {
		return runningExecutionCount;
	}
	public void setRunningExecutionCount(int runningExecutionCount) {
		this.runningExecutionCount = runningExecutionCount;
	}
	/**
	 *
	 * @param taskExecutionInformation
	 */
	public static CurrentTaskExecutionsResource fromTaskExecutionInformation(
		PlatformTaskExecutionInformation taskExecutionInformation) {
		CurrentTaskExecutionsResource resource = new CurrentTaskExecutionsResource();
		resource.setName(taskExecutionInformation.getName());
		resource.setType(taskExecutionInformation.getType());
		resource.setMaximumTaskExecutions(taskExecutionInformation.getMaximumTaskExecutions());
		resource.setRunningExecutionCount(taskExecutionInformation.getRunningExecutionCount());
		return resource;
	}

	public static class Page extends PagedResources<CurrentTaskExecutionsResource> {
	}

}
