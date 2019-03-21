/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.cloud.skipper.domain.deployer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.cloud.skipper.support.PropertiesDiff;
import org.springframework.util.Assert;

/**
 * Describes the difference between two ApplicationManifests
 *
 * @author Mark Pollack
 */
public class ApplicationManifestDifference {

	private final String applicationName;

	private final PropertiesDiff apiAndKindDifference;

	private final PropertiesDiff metadataDifference;

	private final PropertiesDiff resourceAndVersionDifference;

	private final PropertiesDiff applicationPropertiesDifference;

	private final PropertiesDiff deploymentPropertiesDifference;

	@JsonCreator
	public ApplicationManifestDifference(@JsonProperty("applicationName") String applicationName,
			@JsonProperty("apiAndKindDifference") PropertiesDiff apiAndKindDifference,
			@JsonProperty("metadataDifference") PropertiesDiff metadataDifference,
			@JsonProperty("resourceAndVersionDifference") PropertiesDiff resourceAndVersionDifference,
			@JsonProperty("applicationPropertiesDifference") PropertiesDiff applicationPropertiesDifference,
			@JsonProperty("deploymentPropertiesDifference") PropertiesDiff deploymentPropertiesDifference) {
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

	public PropertiesDiff getApiAndKindDifference() {
		return this.apiAndKindDifference;
	}

	public PropertiesDiff getMetadataDifference() {
		return this.metadataDifference;
	}

	public PropertiesDiff getResourceAndVersionDifference() {
		return this.resourceAndVersionDifference;
	}

	public PropertiesDiff getApplicationPropertiesDifference() {
		return this.applicationPropertiesDifference;
	}

	public PropertiesDiff getDeploymentPropertiesDifference() {
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
