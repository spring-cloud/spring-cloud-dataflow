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

package org.springframework.cloud.dataflow.core;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Represents Stream deployment model.
 *
 * @author Ilayaperumal Gopinathan
 */
public class StreamDeployment {

	/**
	 * The name of the stream under deployment.
	 */
	private final String streamName;

	/**
	 * The JSON String value of the deployment properties Map<String, String> values.
	 */
	private String deploymentProperties;

	public StreamDeployment(String streamName) {
		this(streamName, "");
	}

	public StreamDeployment(String streamName, String deploymentProperties) {
		Assert.hasText(streamName, "Stream name must not be null");
		this.streamName = streamName;
		this.deploymentProperties = StringUtils.hasText(deploymentProperties) ? deploymentProperties: "";
	}

	public String getStreamName() {
		return streamName;
	}

	public String getDeploymentProperties() {
		return this.deploymentProperties;
	}

}
