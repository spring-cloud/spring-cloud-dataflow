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

package org.springframework.cloud.dataflow.server.service.impl;

import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.validation.annotation.Validated;


/**
 * Properties used to define the behavior of tasks created by Spring Cloud Data Flow.
 *
 * @author Glenn Renfro
 * @author David Turanski
 */
@Validated
@ConfigurationProperties(prefix = TaskConfigurationProperties.COMPOSED_TASK_PREFIX)
public class TaskConfigurationProperties {

	public static final String COMPOSED_TASK_PREFIX = DataFlowPropertyKeys.PREFIX + "task";

	/**
	 * The task application name to be used for the composed task runner.
	 */
	@NotBlank
	private String composedTaskRunnerName = "composed-task-runner";

	@NotBlank
	private String schedulerTaskLauncherName = "scheduler-task-launcher";

	public String getComposedTaskRunnerName() {
		return composedTaskRunnerName;
	}

	public void setComposedTaskRunnerName(String taskName) {
		this.composedTaskRunnerName = taskName;
	}

	public String getSchedulerTaskLauncherName() {
		return schedulerTaskLauncherName;
	}

	public void setSchedulerTaskLauncherName(String schedulerTaskLauncherName) {
		this.schedulerTaskLauncherName = schedulerTaskLauncherName;
	}
}
