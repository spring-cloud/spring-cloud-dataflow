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

package org.springframework.cloud.dataflow.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;

/**
 * Configuration properties for setting up app validation on a docker repository.
 *
 * @author Glenn Renfro
 */
@ConfigurationProperties(prefix = DockerValidatorProperties.DOCKER_VALIDATION_PREFIX)
public class DockerValidatorProperties {
	public static final String DOCKER_VALIDATION_PREFIX = DataFlowPropertyKeys.PREFIX + "docker.validation";

	public static final String DOCKER_REGISTRY_URL = "https://hub.docker.com/v2/repositories";

	private String userName;
	private String password;

	private String dockerAuthUrl = "https://hub.docker.com/v2/users/login/";
	private String dockerRegistryUrl = DOCKER_REGISTRY_URL;

	private int connectTimeoutInMillis = 10000;
	private int readTimeoutInMillis = 10000;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDockerAuthUrl() {
		return dockerAuthUrl;
	}

	public void setDockerAuthUrl(String dockerAuthUrl) {
		this.dockerAuthUrl = dockerAuthUrl;
	}

	public String getDockerRegistryUrl() {
		return dockerRegistryUrl;
	}

	public void setDockerRegistryUrl(String dockerRegistryUrl) {
		this.dockerRegistryUrl = dockerRegistryUrl;
	}

	public int getConnectTimeoutInMillis() {
		return connectTimeoutInMillis;
	}

	public void setConnectTimeoutInMillis(int connectTimeoutInMillis) {
		this.connectTimeoutInMillis = connectTimeoutInMillis;
	}

	public int getReadTimeoutInMillis() {
		return readTimeoutInMillis;
	}

	public void setReadTimeoutInMillis(int readTimeoutInMillis) {
		this.readTimeoutInMillis = readTimeoutInMillis;
	}
}

