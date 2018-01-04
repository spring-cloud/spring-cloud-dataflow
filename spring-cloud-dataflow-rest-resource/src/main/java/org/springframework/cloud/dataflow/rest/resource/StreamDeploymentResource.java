/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource;

import org.springframework.hateoas.PagedResources;

/**
 * A HATEOAS representation of a stream deployment.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
public class StreamDeploymentResource extends StreamDefinitionResource {

	/**
	 * The name of the stream under deployment.
	 */
	private String streamName;

	/**
	 * The deployer used for the stream deployment. The deployer could be app deployer or Skipper.
	 */
	private String deployerName;

	/**
	 * The package used during the deployment (mainly used by Skipper).
	 */
	private String packageName;

	/**
	 * The release name for the deployment in Skipper.
	 */
	private String releaseName;

	/**
	 * The package repository name used by Skipper for the package used in the deployment.
	 */
	private String repoName;

	/**
	 * The JSON String value of the deployment properties Map<String, String> values.
	 */
	private String deploymentProperties;

	/**
	 * The JSON String value of the apps and their versions used in the stream..
	 */
	private String appVersions;

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected StreamDeploymentResource() {
	}

	public String getStreamName() {
		return streamName;
	}

	public String getDeployerName() {
		return deployerName;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getReleaseName() {
		return releaseName;
	}

	public String getRepoName() {
		return repoName;
	}

	public String getDeploymentProperties() {
		return deploymentProperties;
	}

	public String getAppVersions() {
		return appVersions;
	}

	public StreamDeploymentResource(String streamName, String dslText, String deploymentProperties, String appVersions,
			String deployerName, String packageName, String releaseName, String repoName) {
		super(streamName, dslText);
		this.streamName = streamName;
		this.deploymentProperties = deploymentProperties;
		this.appVersions = appVersions;
		this.deployerName = deployerName;
		this.packageName = packageName;
		this.releaseName = releaseName;
		this.repoName = repoName;
	}

	public static class Page extends PagedResources<StreamDeploymentResource> {

	}

}
