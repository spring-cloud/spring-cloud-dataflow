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
package org.springframework.cloud.skipper.server.deployer;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.server.domain.SpringBootAppKind;
import org.springframework.cloud.skipper.server.domain.SpringBootAppSpec;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Factory managing {@link AppDeploymentRequest}s.
 *
 * @author Mark Pollack
 * @author Janne Valkealahti
 */
public class AppDeploymentRequestFactory {

	private final DelegatingResourceLoader delegatingResourceLoader;

	/**
	 * Instantiates a new {@code AppDeploymentRequestFactory}.
	 *
	 * @param delegatingResourceLoader the delegating resource loader
	 */
	public AppDeploymentRequestFactory(DelegatingResourceLoader delegatingResourceLoader) {
		Assert.notNull(delegatingResourceLoader, "'delegatingResourceLoader' must be set");
		this.delegatingResourceLoader = delegatingResourceLoader;
	}

	/**
	 * Creates an {@link AppDeploymentRequest}.
	 *
	 * @param springBootAppKind the boot app kind
	 * @param releaseName the release name
	 * @param version the release version
	 * @return a created AppDeploymentRequest
	 */
	public AppDeploymentRequest createAppDeploymentRequest(SpringBootAppKind springBootAppKind, String releaseName,
			String version) {
		SpringBootAppSpec spec = springBootAppKind.getSpec();
		Map<String, String> applicationProperties = new TreeMap<>();
		if (spec.getApplicationProperties() != null) {
			applicationProperties.putAll(spec.getApplicationProperties());
		}
		// we need to keep group name same for consumer groups not getting broken, but
		// app name needs to differentiate as otherwise it may result same deployment id and
		// failure on a deployer.
		AppDefinition appDefinition = new AppDefinition(springBootAppKind.getApplicationName() + "-v" + version, applicationProperties);

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

		deploymentProperties.put(AppDeployer.GROUP_PROPERTY_KEY, releaseName);

		AppDeploymentRequest appDeploymentRequest = new AppDeploymentRequest(appDefinition, resource,
				deploymentProperties);
		return appDeploymentRequest;
	}

}
