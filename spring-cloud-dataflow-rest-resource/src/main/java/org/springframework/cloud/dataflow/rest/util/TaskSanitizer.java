/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.core.TaskExecutionManifest;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.task.repository.TaskExecution;

/**
 * Sanitizes sensitive key values for Task related properties.
 *
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public class TaskSanitizer {

	private ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	public TaskExecution sanitizeTaskExecutionArguments(TaskExecution taskExecution) {
		List<String> args = taskExecution.getArguments().stream()
				.map(argument -> (this.argumentSanitizer.sanitize(argument))).collect(Collectors.toList());
		taskExecution.setArguments(args);
		return taskExecution;
	}

	public TaskExecutionManifest sanitizeTaskManifest(TaskExecutionManifest taskManifest) {
		if (taskManifest == null) {
			return null;
		}
		TaskExecutionManifest sanitizedTaskExecutionManifest = new TaskExecutionManifest();
		TaskExecutionManifest.Manifest sanitizedManifest = sanitizedTaskExecutionManifest.getManifest();
		TaskExecutionManifest.Manifest dirtyTaskManifest = taskManifest.getManifest();
		sanitizedManifest.setPlatformName(dirtyTaskManifest.getPlatformName());
		AppDeploymentRequest existingAppDeploymentRequest = dirtyTaskManifest.getTaskDeploymentRequest();
		// Sanitize App Properties
		Map<String, String> existingAppProperties = existingAppDeploymentRequest.getDefinition().getProperties();
		Map<String, String> sanitizedAppProperties = this.argumentSanitizer.sanitizeProperties(existingAppProperties);

		// Sanitize Deployment Properties
		Map<String, String> existingDeploymentProperties = existingAppDeploymentRequest.getDeploymentProperties();
		Map<String, String> sanitizedDeploymentProperties = this.argumentSanitizer.sanitizeProperties(existingDeploymentProperties);

		AppDefinition sanitizedAppDefinition = new AppDefinition(existingAppDeploymentRequest.getDefinition().getName(),
				sanitizedAppProperties);
		List<String> sanitizedCommandLineArgs = existingAppDeploymentRequest.getCommandlineArguments().stream()
				.map(argument -> (this.argumentSanitizer.sanitize(argument))).collect(Collectors.toList());
		AppDeploymentRequest sanitizedAppDeploymentRequest = new AppDeploymentRequest(
				sanitizedAppDefinition,
				existingAppDeploymentRequest.getResource(),
				sanitizedDeploymentProperties,
				sanitizedCommandLineArgs);
		sanitizedManifest.setTaskDeploymentRequest(sanitizedAppDeploymentRequest);
		return sanitizedTaskExecutionManifest;
	}
}
