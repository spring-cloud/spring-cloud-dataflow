/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.scheduler.launcher.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties used to configure the task launcher.
 *
 * @author Glenn Renfro
 **/
@ConfigurationProperties(prefix = "spring.cloud.scheduler.task.launcher")
public class SchedulerTaskLauncherProperties {
	/**
	 * The Spring Cloud Data Flow platform to use for launching tasks.
	 */
	private String platformName = "default";

	private String taskName;

	private String dataflowServerUri = "http://localhost:9393";

	private String taskLauncherPropertyPrefix = "tasklauncher";

	public String getPlatformName() {
		return platformName;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getDataflowServerUri() {
		return dataflowServerUri;
	}

	public void setDataflowServerUri(String dataflowServerUri) {
		this.dataflowServerUri = dataflowServerUri;
	}

	public String getTaskLauncherPropertyPrefix() {
		return taskLauncherPropertyPrefix;
	}

	public void setTaskLauncherPropertyPrefix(String taskLauncherPropertyPrefix) {
		this.taskLauncherPropertyPrefix = taskLauncherPropertyPrefix;
	}
}

