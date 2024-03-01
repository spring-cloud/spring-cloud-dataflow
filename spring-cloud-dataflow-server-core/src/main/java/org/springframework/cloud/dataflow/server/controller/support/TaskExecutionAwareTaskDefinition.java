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
package org.springframework.cloud.dataflow.server.controller.support;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.util.Assert;

/**
 * {@link TaskDefinition} with the associated latest {@link TaskExecution}.
 *
 * @author Gunnar Hillert
 *
 */
public class TaskExecutionAwareTaskDefinition {

	final TaskDefinition taskDefinition;

	final TaskExecution latestTaskExecution;

	/**
	 * Initialized the {@link TaskExecutionAwareTaskDefinition} with the provided
	 * {@link TaskDefinition} and {@link TaskExecution}.
	 *
	 * @param taskDefinition Must not be null
	 * @param latestTaskExecution Must not be null
	 */
	public TaskExecutionAwareTaskDefinition(TaskDefinition taskDefinition, TaskExecution latestTaskExecution) {
		super();

		Assert.notNull(taskDefinition, "The provided taskDefinition must not be null.");
		Assert.notNull(latestTaskExecution, "The provided latestTaskExecution must not be null.");

		this.taskDefinition = taskDefinition;
		this.latestTaskExecution = latestTaskExecution;
	}

	/**
	 * Initialized the {@link TaskExecutionAwareTaskDefinition} with the provided
	 * {@link TaskDefinition}. The underlying {@link TaskExecution} will be set to null.
	 *
	 * @param taskDefinition Must not be null
	 */
	public TaskExecutionAwareTaskDefinition(TaskDefinition taskDefinition) {
		super();

		Assert.notNull(taskDefinition, "The provided taskDefinition must not be null.");

		this.taskDefinition = taskDefinition;
		this.latestTaskExecution = null;

	}

	/**
	 * Returns the {@link TaskDefinition}.
	 *
	 * @return Never null.
	 */
	public TaskDefinition getTaskDefinition() {
		return taskDefinition;
	}

	/**
	 * Returns the associated {@link TaskExecution} if available. May return null.
	 *
	 * @return May return null
	 */
	public TaskExecution getLatestTaskExecution() {
		return latestTaskExecution;
	}
}
