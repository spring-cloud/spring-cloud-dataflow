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
package org.springframework.cloud.skipper.server.deployer;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifest;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationSpec;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Factory managing {@link AppDeploymentRequest}s.
 *
 * @author Mark Pollack
 * @author Janne Valkealahti
 * @author Ilayaperumal Gopinathan
 */
public class AppDeploymentRequestFactory {
	private static final String APP_NAME_PROPERY = AppDeployer.PREFIX + "appName";
	private static final Logger logger = LoggerFactory.getLogger(AppDeploymentRequestFactory.class);

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

	public static String getResourceLocation(String specResource, String specVersion) {
		Assert.hasText(specResource, "Spec resource must not be empty");
		if (specVersion != null) {
			if ((specResource.startsWith("maven") || specResource.startsWith("docker"))) {
				if (specResource.endsWith(":" + specVersion)) {
					return specResource;
				}
				else {
					return String.format("%s:%s", specResource, specVersion);
				}
			}
			// Assume the resource extension is JAR when it is neither maven nor docker.
			else {
				return String.format("%s-%s.jar", specResource, specVersion);
			}
		}
		else {
			return specResource;
		}
	}

	/**
	 * Creates an {@link AppDeploymentRequest}.
	 *
	 * @param applicationSpec the Spring Cloud Deployer application spec
	 * @param releaseName the release name
	 * @param version the release version
	 * @return a created AppDeploymentRequest
	 */
	public AppDeploymentRequest createAppDeploymentRequest(SpringCloudDeployerApplicationManifest applicationSpec, String releaseName,
			String version) {
		SpringCloudDeployerApplicationSpec spec = applicationSpec.getSpec();
		Map<String, String> applicationProperties = new TreeMap<>();
		if (spec.getApplicationProperties() != null) {
			applicationProperties.putAll(spec.getApplicationProperties());
		}
		// we need to keep group name same for consumer groups not getting broken, but
		// app name needs to differentiate as otherwise it may result same deployment id and
		// failure on a deployer.
		AppDefinition appDefinition = new AppDefinition(applicationSpec.getApplicationName() + "-v" + version,
				applicationProperties);
		Resource resource;
		try {
			resource = delegatingResourceLoader.getResource(getResourceLocation(spec.getResource(), spec.getVersion()));
		}
		catch (Exception e) {
			throw new SkipperException(
					"Could not load Resource " + spec.getResource() + ". Message = " + e.getMessage(), e);
		}

		Map<String, String> deploymentProperties = new TreeMap<>();
		if (spec.getDeploymentProperties() != null) {
			deploymentProperties.putAll(spec.getDeploymentProperties());
		}
		if (!deploymentProperties.containsKey(AppDeployer.GROUP_PROPERTY_KEY)) {
			logger.debug("Defaulting spring.cloud.deployer.group=" + releaseName);
			deploymentProperties.put(AppDeployer.GROUP_PROPERTY_KEY, releaseName);
		}

		deploymentProperties.put(APP_NAME_PROPERY, applicationSpec.getApplicationName());

		return new AppDeploymentRequest(appDefinition, resource, deploymentProperties);
	}
}
