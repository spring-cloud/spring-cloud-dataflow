/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.common.test.docker.compose.connection;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.AdditionalEnvironmentValidator;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.DockerType;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.RemoteHostIpResolver;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerConfiguration;

import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;

public class DockerMachine implements DockerConfiguration {

	private static final Logger log = LoggerFactory.getLogger(DockerMachine.class);
	private static final DockerType FALLBACK_DOCKER_TYPE = DockerType.DAEMON;

	private final String hostIp;
	private final Map<String, String> environment;

	public DockerMachine(String hostIp, Map<String, String> environment) {
		this.hostIp = hostIp;
		this.environment = environment;
	}

	public String getIp() {
		return hostIp;
	}

	@Override
	public ProcessBuilder configuredDockerComposeProcess() {
		ProcessBuilder process = new ProcessBuilder();
		augmentGivenEnvironment(process.environment());
		return process;
	}

	private void augmentGivenEnvironment(Map<String, String> environmentToAugment) {
		environmentToAugment.putAll(environment);
	}

	public static LocalBuilder localMachine() {
		Map<String, String> systemEnv = System.getenv();
		Optional<DockerType> dockerType = DockerType.getFirstValidDockerTypeForEnvironment(systemEnv);
		if (!dockerType.isPresent()) {
			log.debug(
					"Failed to determine Docker type (daemon or remote) based on current environment. "
							+ "Proceeding with {} as the type.", FALLBACK_DOCKER_TYPE);
		}

		return new LocalBuilder(dockerType.orElse(FALLBACK_DOCKER_TYPE), systemEnv);
	}

	public static LocalBuilder localMachine(DockerType dockerType) {
		return new LocalBuilder(dockerType, System.getenv());
	}

	public static class LocalBuilder {

		private final DockerType dockerType;
		private final Map<String, String> systemEnvironment;
		private Map<String, String> additionalEnvironment = new HashMap<>();

		LocalBuilder(DockerType dockerType, Map<String, String> systemEnvironment) {
			this.dockerType = dockerType;
			this.systemEnvironment = new HashMap<>(systemEnvironment);
		}

		public LocalBuilder withAdditionalEnvironmentVariable(String key, String value) {
			additionalEnvironment.put(key, value);
			return this;
		}

		public LocalBuilder withEnvironment(Map<String, String> newEnvironment) {
			this.additionalEnvironment = new HashMap<>(newEnvironment != null ? newEnvironment : new HashMap<>());
			return this;
		}

		public DockerMachine build() {
			dockerType.validateEnvironmentVariables(systemEnvironment);
			AdditionalEnvironmentValidator.validate(additionalEnvironment);
			Map<String, String> combinedEnvironment = new HashMap<>();
			combinedEnvironment.putAll(systemEnvironment);
			combinedEnvironment.putAll(additionalEnvironment);

			String dockerHost = systemEnvironment.getOrDefault(DOCKER_HOST, "");
			return new DockerMachine(dockerType.resolveIp(dockerHost), new HashMap<>(combinedEnvironment));
		}
	}

	public static RemoteBuilder remoteMachine() {
		return new RemoteBuilder();
	}

	public static class RemoteBuilder {

		private final Map<String, String> dockerEnvironment = new HashMap<>();
		private Map<String, String> additionalEnvironment = new HashMap<>();

		private RemoteBuilder() {}

		public RemoteBuilder host(String hostname) {
			dockerEnvironment.put(DOCKER_HOST, hostname);
			return this;
		}

		public RemoteBuilder withTLS(String certPath) {
			dockerEnvironment.put(DOCKER_TLS_VERIFY, "1");
			dockerEnvironment.put(DOCKER_CERT_PATH, certPath);
			return this;
		}

		public RemoteBuilder withoutTLS() {
			dockerEnvironment.remove(DOCKER_TLS_VERIFY);
			dockerEnvironment.remove(DOCKER_CERT_PATH);
			return this;
		}

		public RemoteBuilder withAdditionalEnvironmentVariable(String key, String value) {
			additionalEnvironment.put(key, value);
			return this;
		}

		public RemoteBuilder withEnvironment(Map<String, String> newEnvironment) {
			this.additionalEnvironment = new HashMap<>(newEnvironment != null ? newEnvironment : new HashMap<>());
			return this;
		}

		public DockerMachine build() {
			DockerType.REMOTE.validateEnvironmentVariables(dockerEnvironment);
			AdditionalEnvironmentValidator.validate(additionalEnvironment);

			String dockerHost = dockerEnvironment.getOrDefault(DOCKER_HOST, "");
			String hostIp = new RemoteHostIpResolver().resolveIp(dockerHost);

			Map<String, String> environment = new HashMap<>();
			environment.putAll(dockerEnvironment);
			environment.putAll(additionalEnvironment);
			return new DockerMachine(hostIp, environment);
		}

	}

}
