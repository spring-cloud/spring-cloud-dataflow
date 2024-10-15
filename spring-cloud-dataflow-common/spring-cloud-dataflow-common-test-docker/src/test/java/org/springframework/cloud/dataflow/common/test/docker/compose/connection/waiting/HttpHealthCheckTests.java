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
package org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting;

import java.util.function.Function;

import org.junit.Test;

import org.springframework.cloud.dataflow.common.test.docker.compose.connection.Container;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerPort;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.SuccessOrFailureMatchers.failure;
import static org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.SuccessOrFailureMatchers.successful;

public class HttpHealthCheckTests {
    private static final Function<DockerPort, String> URL_FUNCTION = port -> null;
    public static final int PORT = 1234;
    private final Container container = mock(Container.class);

    @Test
    public void be_healthy_when_the_port_is_listening_over_http() {
        whenTheContainerIsListeningOnHttpTo(PORT, URL_FUNCTION);

        assertThat(
                HealthChecks.toRespondOverHttp(PORT, URL_FUNCTION).isHealthy(container),
                successful());
    }

    @Test
    public void be_unhealthy_when_all_ports_are_not_listening() {
        whenTheContainerIsNotListeningOnHttpTo(PORT, URL_FUNCTION);

        assertThat(
                HealthChecks.toRespondOverHttp(PORT, URL_FUNCTION).isHealthy(container),
                failure());
    }

    private void whenTheContainerIsListeningOnHttpTo(int port, Function<DockerPort, String> urlFunction) {
        when(container.portIsListeningOnHttp(port, urlFunction)).thenReturn(SuccessOrFailure.success());
    }

    private void whenTheContainerIsNotListeningOnHttpTo(int port, Function<DockerPort, String> urlFunction) {
        when(container.portIsListeningOnHttp(port, urlFunction)).thenReturn(SuccessOrFailure.failure("not listening"));
    }

}
