/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.shell.command.support;

import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.util.Assert;

/**
 * Provides a typed enumeration of deployment statuses for a collection of applications.
 * @author Gunnar Hillert
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DeploymentStateDisplay {

	DEPLOYING("deploying", "Deploying", "The applications are being deployed."),
	DEPLOYED("deployed", "Deployed", "All applications have been successfully deployed."),
	UNDEPLOYED("undeployed", "Undeployed", "The applications are known to the system, but is not currently deployed."),
	PARTIAL("partial", "Partial",
			"The applications are being deployed."),
	FAILED("failed", "Failed", "All apps have failed deployment."),
	ERROR("error", "Error", "A system error occurred trying to determine platform status."),
	UNKNOWN("unknown", "Unknown", "The applications are not known to the system.");

	private final String key;

	private final String displayName;

	private final String description;

	/**
	 * Constructor.
	 */
	DeploymentStateDisplay(final String key, final String displayName, final String description) {
		this.key = key;
		this.displayName = displayName;
		this.description = description;
	}

	public static DeploymentStateDisplay fromKey(String deploymentStateKey) {

		Assert.hasText(deploymentStateKey, "Parameter deploymentStateKey must not be null or empty.");

		for (DeploymentStateDisplay deploymentStateResource : DeploymentStateDisplay.values()) {
			if (deploymentStateResource.getKey().equals(deploymentStateKey)) {
				return deploymentStateResource;
			}
		}

		return null;
	}

	public String getKey() {
		return key;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDescription() {
		return description;
	}
}
