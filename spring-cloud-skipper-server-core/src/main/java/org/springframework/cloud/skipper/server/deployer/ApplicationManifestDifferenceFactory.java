/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.server.deployer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifest;
import org.springframework.cloud.skipper.domain.deployer.ApplicationManifestDifference;
import org.springframework.cloud.skipper.support.PropertiesDiff;
import org.springframework.util.Assert;

/**
 * A Factory to create an {@link ApplicationManifestDifference}.
 *
 * @author Mark Pollack
 */
public class ApplicationManifestDifferenceFactory {

	/**
	 * Create an ApplicationManifestDifference for an application given two application
	 * manifests
	 * @param applicationName the name of the application represented in each manifest
	 * @param existingApplicationManifest the manifest of the currently deployed application
	 * @param replacingApplicationManifest the manifest of the application that is a candidate
	 * to replace the currently deployed application
	 * @return The ApplicationManifestDifference for the application
	 */
	public ApplicationManifestDifference createApplicationManifestDifference(
			String applicationName,
			SpringCloudDeployerApplicationManifest existingApplicationManifest,
			SpringCloudDeployerApplicationManifest replacingApplicationManifest) {

		Assert.notNull(applicationName, "applicationName can not be null");
		Assert.notNull(existingApplicationManifest, "existingApplicationManifest can not be null");
		Assert.notNull(replacingApplicationManifest, "replacingApplicationManifest can not be null");

		return new ApplicationManifestDifference(applicationName,
				getApiAndKindDifference(existingApplicationManifest, replacingApplicationManifest),
				getMetadataDifference(existingApplicationManifest, replacingApplicationManifest),
				getResourceAndVersionDifference(existingApplicationManifest, replacingApplicationManifest),
				getApplicationPropertiesDifference(existingApplicationManifest, replacingApplicationManifest),
				getDeploymentPropertiesDifference(existingApplicationManifest, replacingApplicationManifest));
	}

	protected PropertiesDiff getApiAndKindDifference(
			SpringCloudDeployerApplicationManifest existingApplicationManifest,
			SpringCloudDeployerApplicationManifest replacingApplicationManifest) {
		Map<String, String> existingApiAndKindMap = new HashMap<>();
		existingApiAndKindMap.put("apiVersion", existingApplicationManifest.getApiVersion().trim());
		existingApiAndKindMap.put("kind", existingApplicationManifest.getKind().trim());

		Map<String, String> replacingApiAndKindMap = new HashMap<>();
		replacingApiAndKindMap.put("apiVersion", replacingApplicationManifest.getApiVersion().trim());
		replacingApiAndKindMap.put("kind", replacingApplicationManifest.getKind().trim());

		return PropertiesDiff.builder().left(existingApiAndKindMap).right(replacingApiAndKindMap).build();
	}

	protected PropertiesDiff getMetadataDifference(SpringCloudDeployerApplicationManifest existingApplicationManifest,
			SpringCloudDeployerApplicationManifest replacingApplicationManifest) {

		Map<String, String> existingMetadataProperties = Optional.ofNullable(existingApplicationManifest.getMetadata())
				.orElse(new HashMap<>());

		Map<String, String> replacingMetadataProperties = Optional
				.ofNullable(replacingApplicationManifest.getMetadata()).orElse(new HashMap<>());

		return PropertiesDiff.builder().left(existingMetadataProperties).right(replacingMetadataProperties).build();
	}

	protected PropertiesDiff getResourceAndVersionDifference(
			SpringCloudDeployerApplicationManifest existingApplicationManifest,
			SpringCloudDeployerApplicationManifest replacingApplicationManifest) {
		Map<String, String> existingResourceAndVersionMap = new HashMap<>();
		existingResourceAndVersionMap.put("resource", existingApplicationManifest.getSpec().getResource().trim());
		existingResourceAndVersionMap.put("version", existingApplicationManifest.getSpec().getVersion().trim());

		Map<String, String> replacingResourceAndVersionMap = new HashMap<>();
		replacingResourceAndVersionMap.put("resource", replacingApplicationManifest.getSpec().getResource().trim());
		replacingResourceAndVersionMap.put("version", replacingApplicationManifest.getSpec().getVersion().trim());

		return PropertiesDiff.builder().left(existingResourceAndVersionMap).right(replacingResourceAndVersionMap).build();
	}

	protected PropertiesDiff getApplicationPropertiesDifference(
			SpringCloudDeployerApplicationManifest existingApplicationManifest,
			SpringCloudDeployerApplicationManifest replacingApplicationManifest) {

		Map<String, String> existingApplicationProperties = Optional
				.ofNullable(existingApplicationManifest.getSpec().getApplicationProperties()).orElse(new HashMap<>());

		Map<String, String> replacingApplicationProperties = Optional
				.ofNullable(replacingApplicationManifest.getSpec().getApplicationProperties()).orElse(new HashMap<>());

		return PropertiesDiff.builder().left(existingApplicationProperties).right(replacingApplicationProperties).build();
	}

	protected PropertiesDiff getDeploymentPropertiesDifference(
			SpringCloudDeployerApplicationManifest existingApplicationManifest,
			SpringCloudDeployerApplicationManifest replacingApplicationManifest) {

		Map<String, String> existingDeploymentProperties = Optional
				.ofNullable(existingApplicationManifest.getSpec().getDeploymentProperties()).orElse(new HashMap<>());

		Map<String, String> replacingDeploymentProperties = Optional
				.ofNullable(replacingApplicationManifest.getSpec().getDeploymentProperties()).orElse(new HashMap<>());

		// exclude deployer count from computing the difference
		existingDeploymentProperties.remove(DefaultReleaseManager.SPRING_CLOUD_DEPLOYER_COUNT);
		replacingDeploymentProperties.remove(DefaultReleaseManager.SPRING_CLOUD_DEPLOYER_COUNT);

		return PropertiesDiff.builder().left(existingDeploymentProperties).right(replacingDeploymentProperties).build();
	}
}
