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

import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;

import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.DaemonEnvironmentValidator;

public class DaemonEnvironmentValidatorTests {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void validate_successfully_when_docker_environment_does_not_contain_docker_variables() {
		Map<String, String> variables = new HashMap<>();
		variables.put("SOME_VARIABLE", "SOME_VALUE");
		variables.put("ANOTHER_VARIABLE", "ANOTHER_VALUE");

		DaemonEnvironmentValidator.instance().validateEnvironmentVariables(variables);
	}

	@Test
	public void throw_exception_when_docker_environment_contains_illegal_docker_variables() {
		Map<String, String> variables = new HashMap<>();
		variables.put(DOCKER_HOST, "tcp://192.168.99.100:2376");
		variables.put(DOCKER_TLS_VERIFY, "1");
		variables.put(DOCKER_CERT_PATH, "/path/to/certs");

		exception.expect(IllegalStateException.class);
		exception.expectMessage("These variables were set:");
		exception.expectMessage(DOCKER_HOST);
		exception.expectMessage(DOCKER_CERT_PATH);
		exception.expectMessage(DOCKER_TLS_VERIFY);
		exception.expectMessage("They cannot be set when connecting to a local docker daemon");
		DaemonEnvironmentValidator.instance().validateEnvironmentVariables(variables);
	}

}
