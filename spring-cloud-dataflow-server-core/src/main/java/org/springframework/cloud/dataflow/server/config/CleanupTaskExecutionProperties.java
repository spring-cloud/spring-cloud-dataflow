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

package org.springframework.cloud.dataflow.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.cloud.dataflow.server.controller.support.TaskExecutionControllerDeleteAction;

/**
 * @author Ganghun Cho
 */
@ConfigurationProperties(prefix = CleanupTaskExecutionProperties.CLEANUP_TASK_EXECUTION_PROPS_PREFIX)
public class CleanupTaskExecutionProperties {

	public static final String CLEANUP_TASK_EXECUTION_PROPS_PREFIX = DataFlowPropertyKeys.PREFIX + "task.execution.cleanup";

	private boolean enabled = false;

	private TaskExecutionControllerDeleteAction[] action = new TaskExecutionControllerDeleteAction[] {
		TaskExecutionControllerDeleteAction.CLEANUP };

	private String taskName = "";

	private boolean completed = false;

	private String schedule = "0 0 * * * *";

	private Integer days;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public TaskExecutionControllerDeleteAction[] getAction() {
		return action;
	}

	public void setAction(TaskExecutionControllerDeleteAction[] action) {
		this.action = action;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	public Integer getDays() {
		return days;
	}

	public void setDays(Integer days) {
		this.days = days;
	}
}
