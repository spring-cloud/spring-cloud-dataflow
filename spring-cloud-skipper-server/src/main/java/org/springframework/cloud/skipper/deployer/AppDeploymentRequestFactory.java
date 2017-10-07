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
package org.springframework.cloud.skipper.deployer;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.domain.SpringBootAppKind;
import org.springframework.cloud.skipper.domain.SpringBootAppSpec;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Utilities for managing AppDeploymentRequests
 * @author Mark Pollack
 */
public class AppDeploymentRequestFactory {

	private final DelegatingResourceLoader delegatingResourceLoader;

	public AppDeploymentRequestFactory(DelegatingResourceLoader delegatingResourceLoader) {
		this.delegatingResourceLoader = delegatingResourceLoader;
	}

	public AppDeploymentRequest createAppDeploymentRequest(SpringBootAppKind springBootAppKind, String releaseName,
			String version) {
		SpringBootAppSpec spec = springBootAppKind.getSpec();
		Map<String, String> applicationProperties = new TreeMap<>();
		if (spec.getApplicationProperties() != null) {
			applicationProperties.putAll(spec.getApplicationProperties());
		}
		AppDefinition appDefinition = new AppDefinition(springBootAppKind.getApplicationName(), applicationProperties);

		Assert.hasText(spec.getResource(), "Package template must define a resource uri");
		Resource resource = delegatingResourceLoader.getResource(spec.getResource());

		Map<String, String> deploymentProperties = new TreeMap<>();
		if (spec.getDeploymentProperties() != null) {
			deploymentProperties.putAll(spec.getDeploymentProperties());
		}
		Map<String, String> metadata = springBootAppKind.getMetadata();
		if (metadata.containsKey("count")) {
			deploymentProperties.put(AppDeployer.COUNT_PROPERTY_KEY, String.valueOf(metadata.get("count")));
		}

		// TODO fix setting of group property with new version, will mess up consumer groups.
		deploymentProperties.put(AppDeployer.GROUP_PROPERTY_KEY, releaseName + "-v" + version);

		AppDeploymentRequest appDeploymentRequest = new AppDeploymentRequest(appDefinition, resource,
				deploymentProperties);
		return appDeploymentRequest;
	}

}
