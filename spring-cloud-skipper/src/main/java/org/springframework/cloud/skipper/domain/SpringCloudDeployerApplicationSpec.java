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
package org.springframework.cloud.skipper.domain;

import java.util.Map;

/**
 * Specification to handle applications that can be deployed into target platforms based
 * on their Spring Cloud Deployer implementations. Contained inside a
 * {@link SpringCloudDeployerApplicationManifest} instance.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class SpringCloudDeployerApplicationSpec {

	public static final String APPLICATION_PROPERTIES_STRING = "applicationProperties";

	public static final String DEPLOYMENT_PROPERTIES_STRING = "deploymentProperties";

	private String resource;

	private String resourceMetadata;

	private String version;

	private Map<String, String> applicationProperties;

	private Map<String, String> deploymentProperties;

	public SpringCloudDeployerApplicationSpec() {
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getResourceMetadata() {
		return resourceMetadata;
	}

	public void setResourceMetadata(String resourceMetadata) {
		this.resourceMetadata = resourceMetadata;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Map<String, String> getApplicationProperties() {
		return applicationProperties;
	}

	public void setApplicationProperties(Map<String, String> applicationProperties) {
		this.applicationProperties = applicationProperties;
	}

	public Map<String, String> getDeploymentProperties() {
		return deploymentProperties;
	}

	public void setDeploymentProperties(Map<String, String> deploymentProperties) {
		this.deploymentProperties = deploymentProperties;
	}
}
