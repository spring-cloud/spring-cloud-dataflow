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
	 * The JSON String value of the deployment properties Map<String, String> values.
	 */
	private String deploymentProperties;

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected StreamDeploymentResource() {
	}

	public String getStreamName() {
		return streamName;
	}

	public String getDeploymentProperties() {
		return deploymentProperties;
	}

	public StreamDeploymentResource(String streamName, String dslText) {
		this(streamName, dslText, "");
	}

	public StreamDeploymentResource(String streamName, String dslText, String deploymentProperties) {
		super(streamName, dslText);
		this.streamName = streamName;
		this.deploymentProperties = deploymentProperties;
	}

	public static class Page extends PagedResources<StreamDeploymentResource> {

	}

}
