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
import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.EnvironmentVariables.TCP_PROTOCOL;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.RemoteHostIpResolver;

public class RemoteHostIpResolverTests {

    private static final String IP = "192.168.99.100";
    private static final int PORT = 2376;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void result_in_error_blank_when_resolving_invalid_docker_host() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("DOCKER_HOST cannot be blank/null");
        new RemoteHostIpResolver().resolveIp("");
    }

    @Test
    public void result_in_error_null_when_resolving_invalid_docker_host() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("DOCKER_HOST cannot be blank/null");
        new RemoteHostIpResolver().resolveIp(null);
    }

    @Test
    public void resolve_docker_host_with_port() {
        String dockerHost = String.format("%s%s:%d", TCP_PROTOCOL, IP, PORT);
		assertThat(new RemoteHostIpResolver().resolveIp(dockerHost)).isEqualTo(IP);
    }

    @Test
    public void resolve_docker_host_without_port() {
        String dockerHost = String.format("%s%s", TCP_PROTOCOL, IP);
		assertThat(new RemoteHostIpResolver().resolveIp(dockerHost)).isEqualTo(IP);
    }
}
