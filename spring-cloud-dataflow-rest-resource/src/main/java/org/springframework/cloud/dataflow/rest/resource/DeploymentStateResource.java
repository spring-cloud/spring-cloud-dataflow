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
package org.springframework.cloud.dataflow.rest.resource;

import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.util.Assert;

/**
 * Provides a typed enumeration of deployment statuses for Streams. Currently, this class
 * is used more as a helper class, and is not serialized via the REST API as of now, this
 * may change however with the next major release.
 *
 * @author Gunnar Hillert
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DeploymentStateResource {

	DEPLOYING("deploying", "Deploying", "The stream is being deployed."),
	DEPLOYED("deployed", "Deployed", "The stream has been successfully deployed"),
	UNDEPLOYED("undeployed", "Undeployed", "The app or group is known to the system, but is not currently deployed"),
	PARTIAL("partial", "Partial",
			"In the case of multiple apps, some have successfully deployed, while others have " + "not"),
	FAILED("failed", "Failed", "All apps have failed deployment"),
	ERROR("error", "Error", "A system error occurred trying to determine deployment status"),
	UNKNOWN("unknown", "Unknown", "The app or group deployment is not known to the system");

	private final String key;

	private final String displayName;

	private final String description;

	/**
	 * Constructor.
	 */
	DeploymentStateResource(final String key, final String displayName, final String description) {
		this.key = key;
		this.displayName = displayName;
		this.description = description;
	}

	public static DeploymentStateResource fromKey(String deploymentStateResourceKey) {

		Assert.hasText(deploymentStateResourceKey, "Parameter deploymentStateResourceKey must not be null or empty.");

		for (DeploymentStateResource deploymentStateResource : DeploymentStateResource.values()) {
			if (deploymentStateResource.getKey().equals(deploymentStateResourceKey)) {
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
