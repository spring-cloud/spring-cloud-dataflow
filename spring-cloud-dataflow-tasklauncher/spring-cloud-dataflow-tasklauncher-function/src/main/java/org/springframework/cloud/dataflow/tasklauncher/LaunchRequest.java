/*
 * Copyright 2021-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.tasklauncher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.util.Assert;

/**
 * @author David Turanski
 * @author Corneil du Plessis
 **/

public class LaunchRequest {
	@JsonProperty("args")
	private List<String> commandlineArguments = new ArrayList<>();
	@JsonProperty("deploymentProps")
	private Map<String, String> deploymentProperties = new HashMap<>();
	@JsonProperty("name")
	private String taskName;

	public LaunchRequest() {
	}

	public LaunchRequest(String taskName, List<String> commandlineArguments, Map<String, String> deploymentProperties) {
		this.commandlineArguments = commandlineArguments;
		this.deploymentProperties = deploymentProperties;
		this.taskName = taskName;
	}

	public List<String> getCommandlineArguments() {
		return commandlineArguments;
	}

	public void setCommandlineArguments(List<String> commandlineArguments) {
		Assert.notNull(commandlineArguments, "'commandLineArguments' cannot be null.");
		this.commandlineArguments = commandlineArguments;
	}

	public Map<String, String> getDeploymentProperties() {
		return deploymentProperties;
	}

	public void setDeploymentProperties(Map<String, String> deploymentProperties) {
		Assert.notNull(commandlineArguments, "'deploymentProperties' cannot be null.");
		this.deploymentProperties = deploymentProperties;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		Assert.hasText(taskName, "'taskName' cannot be blank.");
		this.taskName = taskName;
	}

	@Override
	public String toString() {
		return "LaunchRequest{" +
			"commandlineArguments=" + commandlineArguments +
			", deploymentProperties=" + deploymentProperties +
			", taskName='" + taskName + '\'' +
			'}';
	}
}
