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
package org.springframework.cloud.dataflow.common.test.docker.compose.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.DockerType;

public class DockerTypeTests {

	@Test
	public void return_remote_as_first_valid_type_if_environment_is_illegal_for_daemon() {

		Map<String, String> variables = new HashMap<>();
		variables.put(DOCKER_HOST, "tcp://192.168.99.100:2376");
		variables.put(DOCKER_TLS_VERIFY, "1");
		variables.put(DOCKER_CERT_PATH, "/path/to/certs");
		assertThat(DockerType.getFirstValidDockerTypeForEnvironment(variables)).isEqualTo(Optional.of(DockerType.REMOTE));
	}

	@Test
	public void return_daemon_as_first_valid_type_if_environment_is_illegal_for_remote() {
		Map<String, String> variables = new HashMap<>();
		assertThat(DockerType.getFirstValidDockerTypeForEnvironment(variables)).isEqualTo(Optional.of(DockerType.DAEMON));
	}

	@Test
	public void return_absent_as_first_valid_type_if_environment_is_illegal_for_all() {
		Map<String, String> variables = new HashMap<>();
		variables.put(DOCKER_TLS_VERIFY, "1");
		assertThat(DockerType.getFirstValidDockerTypeForEnvironment(variables)).isEqualTo(Optional.empty());
	}

}
