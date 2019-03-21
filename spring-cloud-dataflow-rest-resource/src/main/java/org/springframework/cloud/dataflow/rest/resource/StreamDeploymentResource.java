/*
 * Copyright 2015-2018 the original author or authors.
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

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;

/**
 * A HATEOAS representation of a stream deployment.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
public class StreamDeploymentResource extends ResourceSupport {

	/**
	 * The name of the stream under deployment.
	 */
	private String streamName;

	/**
	 * Stream definition DSL text.
	 */
	private String dslText;

	/**
	 * Stream status (i.e. deployed, undeployed, etc).
	 */
	private String status;

	/**
	 * The JSON String value of the deployment properties Map<String, String> values.
	 */
	private String deploymentProperties;

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected StreamDeploymentResource() {
	}

	public StreamDeploymentResource(String streamName, String dslText) {
		this(streamName, dslText, "");
	}

	public StreamDeploymentResource(String streamName, String dslText, String deploymentProperties) {
		this(streamName, dslText, deploymentProperties, "");
	}

	public StreamDeploymentResource(String streamName, String dslText, String deploymentProperties, String status) {
		this.streamName = streamName;
		this.dslText = dslText;
		this.deploymentProperties = deploymentProperties;
		this.status = status;
	}

	public String getStreamName() {
		return streamName;
	}

	public String getDeploymentProperties() {
		return deploymentProperties;
	}

	public String getDslText() {
		return dslText;
	}

	public String getStatus() {
		return status;
	}

	public static class Page extends PagedResources<StreamDeploymentResource> {

	}

}
