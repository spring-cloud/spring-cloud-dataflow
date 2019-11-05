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

/**
 * Contains difference for a task manifest properties.
 *
 * @author Janne Valkealahti
 *
 */
public class TaskManifestDifference {

	private final PropertiesDiff deploymentPropertiesDifference;
	private final PropertiesDiff commandLineArgumentPropertiesDifferenceDoubleDash;
	private final PropertiesDiff commandLineArgumentPropertiesDifferenceSingleDash;
	private final PropertiesDiff commandLineArgumentPropertiesDifferenceNoDash;

	public TaskManifestDifference(PropertiesDiff deploymentPropertiesDifference,
			PropertiesDiff commandLineArgumentPropertiesDifferenceDoubleDash,
			PropertiesDiff commandLineArgumentPropertiesDifferenceSingleDash,
			PropertiesDiff commandLineArgumentPropertiesDifferenceNoDash) {
		this.deploymentPropertiesDifference = deploymentPropertiesDifference;
		this.commandLineArgumentPropertiesDifferenceDoubleDash = commandLineArgumentPropertiesDifferenceDoubleDash;
		this.commandLineArgumentPropertiesDifferenceSingleDash = commandLineArgumentPropertiesDifferenceSingleDash;
		this.commandLineArgumentPropertiesDifferenceNoDash = commandLineArgumentPropertiesDifferenceNoDash;
	}

	public PropertiesDiff getDeploymentPropertiesDifference() {
		return deploymentPropertiesDifference;
	}

	public PropertiesDiff getCommandLineArgumentPropertiesDifferenceDoubleDash() {
		return commandLineArgumentPropertiesDifferenceDoubleDash;
	}

	public PropertiesDiff getCommandLineArgumentPropertiesDifferenceSingleDash() {
		return commandLineArgumentPropertiesDifferenceSingleDash;
	}

	public PropertiesDiff getCommandLineArgumentPropertiesDifferenceNoDash() {
		return commandLineArgumentPropertiesDifferenceNoDash;
	}

	@Override
	public String toString() {
		return "TaskManifestDifference [commandLineArgumentPropertiesDifferenceDoubleDash="
				+ commandLineArgumentPropertiesDifferenceDoubleDash + ", commandLineArgumentPropertiesDifferenceNoDash="
				+ commandLineArgumentPropertiesDifferenceNoDash + ", commandLineArgumentPropertiesDifferenceSingleDash="
				+ commandLineArgumentPropertiesDifferenceSingleDash + ", deploymentPropertiesDifference="
				+ deploymentPropertiesDifference + "]";
	}
}
