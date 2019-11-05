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
package org.springframework.cloud.dataflow.server.service.impl.diff;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;

/**
 * Analyze a task and create a {@link TaskAnalysisReport} which can be used
 * resurrect needed properties and arguments to bring forward to new task launch.
 *
 * @author Janne Valkealahti
 *
 */
public class TaskAnalyzer {

	/**
	 * Compares current and newly created manifest for their differences.
	 *
	 * @param existing the current manifest
	 * @param replacing the new replacing manifest
	 * @return the task analysis report
	 */
	public TaskAnalysisReport analyze(TaskManifest existing, TaskManifest replacing) {
		Map<String, String> existingDeploymentProperties = null;
		Map<String, String> existingCommandLineArgumentPropertiesDoubleDash = new HashMap<>();
		Map<String, String> existingCommandLineArgumentPropertiesSingleDash = new HashMap<>();
		Map<String, String> existingCommandLineArgumentPropertiesNoDash = new HashMap<>();
		if (existing != null) {
			AppDeploymentRequest taskDeploymentRequest = existing.getTaskDeploymentRequest();
			if (taskDeploymentRequest != null) {
				existingDeploymentProperties = taskDeploymentRequest.getDeploymentProperties();

				for (String arg : taskDeploymentRequest.getCommandlineArguments()) {
					if (arg.startsWith("--")) {
						String[] split = arg.substring(2).split("=", 2);
						if (split.length == 2) {
							existingCommandLineArgumentPropertiesDoubleDash.put(split[0], split[1]);
						}
					}
					else if (arg.startsWith("-")) {
						String[] split = arg.substring(1).split("=", 2);
						if (split.length == 2) {
							existingCommandLineArgumentPropertiesSingleDash.put(split[0], split[1]);
						}
					}
					else {
						String[] split = arg.split("=", 2);
						if (split.length == 2) {
							existingCommandLineArgumentPropertiesNoDash.put(split[0], split[1]);
						}
					}
				}
			}
		}

		Map<String, String> replacingDeploymentProperties = null;
		Map<String, String> replacingCommandLineArgumentPropertiesDoubleDash = new HashMap<>();
		Map<String, String> replacingCommandLineArgumentPropertiesSingleDash = new HashMap<>();
		Map<String, String> replacingCommandLineArgumentPropertiesNoDash = new HashMap<>();
		if (replacing != null) {
			AppDeploymentRequest taskDeploymentRequest = replacing.getTaskDeploymentRequest();
			if (taskDeploymentRequest != null) {
				replacingDeploymentProperties = taskDeploymentRequest.getDeploymentProperties();

				for (String arg : taskDeploymentRequest.getCommandlineArguments()) {
					if (arg.startsWith("--")) {
						String[] split = arg.substring(2).split("=", 2);
						if (split.length == 2) {
							replacingCommandLineArgumentPropertiesDoubleDash.put(split[0], split[1]);
						}
					}
					else if (arg.startsWith("-")) {
						String[] split = arg.substring(1).split("=", 2);
						if (split.length == 2) {
							replacingCommandLineArgumentPropertiesSingleDash.put(split[0], split[1]);
						}
					}
					else {
						String[] split = arg.split("=", 2);
						if (split.length == 2) {
							replacingCommandLineArgumentPropertiesNoDash.put(split[0], split[1]);
						}
					}
				}
			}
		}

		PropertiesDiff deploymentPropertiesDifference = PropertiesDiff.builder().left(existingDeploymentProperties)
				.right(replacingDeploymentProperties).build();

		PropertiesDiff existingCommandLineArgumentPropertiesDifferenceDoubleDash = PropertiesDiff.builder()
				.left(existingCommandLineArgumentPropertiesDoubleDash)
				.right(replacingCommandLineArgumentPropertiesDoubleDash).build();

		PropertiesDiff existingCommandLineArgumentPropertiesDifferenceSingleDash = PropertiesDiff.builder()
				.left(existingCommandLineArgumentPropertiesSingleDash)
				.right(replacingCommandLineArgumentPropertiesSingleDash).build();

		PropertiesDiff existingCommandLineArgumentPropertiesDifferenceNoDash = PropertiesDiff.builder()
				.left(existingCommandLineArgumentPropertiesNoDash).right(replacingCommandLineArgumentPropertiesNoDash)
				.build();

		TaskManifestDifference taskManifestDifference = new TaskManifestDifference(deploymentPropertiesDifference,
				existingCommandLineArgumentPropertiesDifferenceDoubleDash,
				existingCommandLineArgumentPropertiesDifferenceSingleDash,
				existingCommandLineArgumentPropertiesDifferenceNoDash);
		return new TaskAnalysisReport(taskManifestDifference);
	}
}
