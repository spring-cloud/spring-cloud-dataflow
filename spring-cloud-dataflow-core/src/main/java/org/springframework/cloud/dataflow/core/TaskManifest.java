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
package org.springframework.cloud.dataflow.core;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.style.ToStringCreator;

/**
 * Description of an execution of a task including resource to be executed and how it was configured via Spring Cloud
 * Data Flow
 *
 * @author Mark Pollack
 * @author Michael Minella
 * @since 2.3
 */
public class TaskManifest {

	private AppDeploymentRequest taskDeploymentRequest;

	private String platformName;

	/**
	 * Name of the platform the related task execution was executed on.
	 *
	 * @return name of the platform
	 */
	public String getPlatformName() {
		return platformName;
	}

	/**
	 * Name of the platform the related task execution was executed on.
	 *
	 * @param platformName platform name
	 */
	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	/**
	 * {@code AppDeploymentRequest} representing the task being executed
	 *
	 * @return {@code AppDeploymentRequest}
	 */
	public AppDeploymentRequest getTaskDeploymentRequest() {
		return taskDeploymentRequest;
	}

	/**
	 * Task deployment
	 *
	 * @param taskDeploymentRequest {@code AppDeploymentRequest}
	 */
	public void setTaskDeploymentRequest(AppDeploymentRequest taskDeploymentRequest) {
		this.taskDeploymentRequest = taskDeploymentRequest;
	}

	public String toString() {
		return (new ToStringCreator(this)).append("taskDeploymentRequest", this.taskDeploymentRequest).append("platformName", this.platformName).toString();
	}

}
