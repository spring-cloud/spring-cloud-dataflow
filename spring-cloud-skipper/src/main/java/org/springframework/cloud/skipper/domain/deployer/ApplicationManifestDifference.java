/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.skipper.domain.deployer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.MapDifference;

import org.springframework.util.Assert;

/**
 * Describes the difference between two ApplicationManifests
 * @author Mark Pollack
 */
public class ApplicationManifestDifference {

	private final String applicationName;

	private final MapDifference apiAndKindDifference;

	private final MapDifference metadataDifference;

	private final MapDifference resourceAndVersionDifference;

	private final MapDifference applicationPropertiesDifference;

	private final MapDifference deploymentPropertiesDifference;

	@JsonCreator
	public ApplicationManifestDifference(@JsonProperty("applicationName") String applicationName,
			@JsonProperty("apiAndKindDifference") MapDifference apiAndKindDifference,
			@JsonProperty("metadataDifference") MapDifference metadataDifference,
			@JsonProperty("resourceAndVersionDifference") MapDifference resourceAndVersionDifference,
			@JsonProperty("applicationPropertiesDifference") MapDifference applicationPropertiesDifference,
			@JsonProperty("deploymentPropertiesDifference") MapDifference deploymentPropertiesDifference) {
		Assert.notNull(applicationName, "applicationName can not be null");
		Assert.notNull(apiAndKindDifference, "apiAndKindDifference can not be null");
		Assert.notNull(metadataDifference, "metadataDifference can not be null");
		Assert.notNull(resourceAndVersionDifference, "resourceAndVersionDifference can not be null");
		Assert.notNull(applicationPropertiesDifference, "applicationPropertiesDifference can not be null");
		Assert.notNull(deploymentPropertiesDifference, "deploymentPropertiesDifference can not be null");
		this.applicationName = applicationName;
		this.apiAndKindDifference = apiAndKindDifference;
		this.metadataDifference = metadataDifference;
		this.resourceAndVersionDifference = resourceAndVersionDifference;
		this.applicationPropertiesDifference = applicationPropertiesDifference;
		this.deploymentPropertiesDifference = deploymentPropertiesDifference;
	}

	public String getApplicationName() {
		return this.applicationName;
	}

	public MapDifference getApiAndKindDifference() {
		return this.apiAndKindDifference;
	}

	public MapDifference getMetadataDifference() {
		return this.metadataDifference;
	}

	public MapDifference getResourceAndVersionDifference() {
		return this.resourceAndVersionDifference;
	}

	public MapDifference getApplicationPropertiesDifference() {
		return this.applicationPropertiesDifference;
	}

	public MapDifference getDeploymentPropertiesDifference() {
		return this.deploymentPropertiesDifference;
	}

	/**
	 * Returns {@code true} if there are no differences between the two application manifests;
	 * that is, if the manifests are equal.
	 */
	public boolean areEqual() {
		return this.apiAndKindDifference.areEqual() &&
				this.metadataDifference.areEqual() &&
				this.resourceAndVersionDifference.areEqual() &&
				this.applicationPropertiesDifference.areEqual() &&
				this.deploymentPropertiesDifference.areEqual();
	}

}
