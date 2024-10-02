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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static java.util.stream.Collectors.joining;
import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;

public class RemoteEnvironmentValidator implements EnvironmentValidator {

	private static final Set<String> SECURE_VARIABLES = new HashSet<>(Arrays.asList(DOCKER_TLS_VERIFY, DOCKER_CERT_PATH));
	private static final RemoteEnvironmentValidator VALIDATOR = new RemoteEnvironmentValidator();

	public static RemoteEnvironmentValidator instance() {
		return VALIDATOR;
	}

	private RemoteEnvironmentValidator() {}

	@Override
	public void validateEnvironmentVariables(Map<String, String> dockerEnvironment) {
		Collection<String> missingVariables = getMissingEnvVariables(dockerEnvironment);
		String errorMessage = missingVariables.stream()
											  .collect(joining(", ",
															   "Missing required environment variables: ",
															   ". Please run `docker-machine env <machine-name>` and "
																	   + "ensure they are set on the DockerComposition."));

		Assert.state(missingVariables.isEmpty(), errorMessage);
	}

	private static Collection<String> getMissingEnvVariables(Map<String, String> dockerEnvironment) {
		Collection<String> requiredVariables = new HashSet<>(Arrays.asList(DOCKER_HOST));
		requiredVariables.addAll(secureVariablesRequired(dockerEnvironment));
		return requiredVariables.stream()
								.filter(envVariable -> !StringUtils.hasText(dockerEnvironment.get(envVariable)))
								.collect(Collectors.toSet());
	}

	private static Set<String> secureVariablesRequired(Map<String, String> dockerEnvironment) {
		return certVerificationEnabled(dockerEnvironment) ? SECURE_VARIABLES : new HashSet<>();
	}

	private static boolean certVerificationEnabled(Map<String, String> dockerEnvironment) {
		return dockerEnvironment.containsKey(DOCKER_TLS_VERIFY);
	}

}
