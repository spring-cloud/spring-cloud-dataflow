/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.util.Assert;

/**
 * Properties used to define the behavior of the composed task runner.
 *
 * @author Chris Schaefer
 * @since 2.7
 */
@ConfigurationProperties(prefix = ComposedTaskRunnerConfigurationProperties.COMPOSED_TASK_PREFIX)
public class ComposedTaskRunnerConfigurationProperties {
	static final String COMPOSED_TASK_PREFIX = DataFlowPropertyKeys.PREFIX + "task.composedtaskrunner";
	static final String COMPOSED_TASK_RUNNER_NAME = "composed-task-runner";

	/**
	 * The task application uri to be used for the composed task runner.
	 */
	private String uri;

	/**
	 * The image pull secret to pass as a deployer property to deployed tasks
	 */
	private String imagePullSecret;

	/**
	 * If true SCDF will set the dataflow-server-access-token for the composed
	 * task runner to the user's token when launching composed tasks.
	 */
	private Boolean useUserAccessToken = false;

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getImagePullSecret() {
		return imagePullSecret;
	}

	public void setImagePullSecret(String imagePullSecret) {
		this.imagePullSecret = imagePullSecret;
	}

	public Boolean isUseUserAccessToken() {
		return useUserAccessToken;
	}

	public void setUseUserAccessToken(Boolean useUserAccessToken) {
		Assert.notNull(useUserAccessToken, "'useUserAccessToken' cannot be null");
		this.useUserAccessToken = useUserAccessToken;
	}
}
