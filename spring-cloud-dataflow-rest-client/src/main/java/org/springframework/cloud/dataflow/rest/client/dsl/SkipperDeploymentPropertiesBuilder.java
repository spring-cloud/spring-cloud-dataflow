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
package org.springframework.cloud.dataflow.rest.client.dsl;

import java.util.Map;

import org.springframework.cloud.dataflow.rest.SkipperStream;
import org.springframework.util.Assert;

/**
 * @author Vinicius Carvalho
 *
 * Utility class to help building a Map of skipper based deployment properties
 */
public class SkipperDeploymentPropertiesBuilder extends AbstractPropertiesBuilder {

	public SkipperDeploymentPropertiesBuilder(){}

	public SkipperDeploymentPropertiesBuilder(Map<String, String> map) {
		this.deploymentProperties.putAll(map);
	}

	public SkipperDeploymentPropertiesBuilder put(String key, String value) {
		this.deploymentProperties.put(key, value.toString());
		return this;
	}

	public SkipperDeploymentPropertiesBuilder putAll(Map<String, String> map){
		this.deploymentProperties.putAll(map);
		return this;
	}

	/**
	 * Configures the memory size allocated for the application.
	 *
	 * @param label - The application name or label to configure the number of instances
	 * @param memory - Memory size in Mb, must be greater or equal 128
	 *
	 * @return the instance of this SkipperDeploymentPropertiesBuilder
	 */
	public SkipperDeploymentPropertiesBuilder memory(String label, Integer memory){
		Assert.notNull(memory, "Memory value can't be null");
		Assert.hasLength(label, "Application name/label can not be empty");
		Assert.isTrue(memory >= 128 , "Memory value must be greater or equal than 128mb");
		this.deploymentProperties.put(String.format(DEPLOYER_PREFIX, label, "memory"), memory.toString());
		return this;
	}

	/**
	 * Sets the number of instances of the target application.
	 *
	 * @param label The application name or label to configure the number of instances
	 * @param instances Number of instances, must be greater than zero
	 *
	 * @return the instance of this SkipperDeploymentPropertiesBuilder
	 */
	public SkipperDeploymentPropertiesBuilder count(String label, Integer instances) {
		Assert.notNull(instances, "Number of instances can't be null");
		Assert.hasLength(label, "Application name/label can not be empty");
		Assert.isTrue(instances > 0, "Number of instances must be greater than zero");
		this.deploymentProperties.put(String.format(DEPLOYER_PREFIX, label, "count"), instances.toString());
		return this;
	}


	/**
	 * Sets the target platform to be used by skipper
	 * @param platformName the platform name
	 * @return the instance of this SkipperDeploymentPropertiesBuilder
	 */
	public SkipperDeploymentPropertiesBuilder platformName(String platformName){
		Assert.hasLength(platformName, "Platform can't be empty");
		this.deploymentProperties.put(SkipperStream.SKIPPER_PLATFORM_NAME, platformName);
		return this;
	}

	/**
	 * Sets the package version to be used by skipper
	 * @param packageVersion the package version
	 * @return the instance of this SkipperDeploymentPropertiesBuilder
	 */
	public SkipperDeploymentPropertiesBuilder packageVersion(String packageVersion){
		Assert.hasLength(packageVersion, "Package version can't be empty");
		this.deploymentProperties.put(SkipperStream.SKIPPER_PACKAGE_VERSION, packageVersion);
		return this;
	}

	/**
	 * Sets the repo name platform to be used by skipper
	 * @param repoName the repo name
	 * @return the instance of this SkipperDeploymentPropertiesBuilder
	 */
	public SkipperDeploymentPropertiesBuilder repoName(String repoName){
		Assert.hasLength(repoName, "Repository name can't be empty");
		this.deploymentProperties.put(SkipperStream.SKIPPER_REPO_NAME, repoName);
		return this;
	}


}
