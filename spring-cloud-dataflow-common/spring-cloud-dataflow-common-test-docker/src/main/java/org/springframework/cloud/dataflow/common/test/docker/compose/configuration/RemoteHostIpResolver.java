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

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.TCP_PROTOCOL;

public class RemoteHostIpResolver implements HostIpResolver {

	@Override
	public String resolveIp(String dockerHost) {
		return Optional.ofNullable(org.springframework.util.StringUtils.hasText(dockerHost) ? dockerHost : null)
					   .map(host -> StringUtils.substringAfter(host, TCP_PROTOCOL))
					   .map(ipAndMaybePort -> StringUtils.substringBefore(ipAndMaybePort, ":"))
					   .orElseThrow(() -> new IllegalArgumentException("DOCKER_HOST cannot be blank/null"));
	}
}
