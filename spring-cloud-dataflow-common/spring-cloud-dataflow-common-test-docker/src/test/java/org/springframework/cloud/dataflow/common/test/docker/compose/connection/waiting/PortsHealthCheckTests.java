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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.SuccessOrFailureMatchers.failure;
import static org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.SuccessOrFailureMatchers.successful;

import org.junit.Test;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.Container;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.HealthCheck;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.HealthChecks;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.SuccessOrFailure;

public class PortsHealthCheckTests {
    private final HealthCheck<Container> healthCheck = HealthChecks.toHaveAllPortsOpen();
    private final Container container = mock(Container.class);

    @Test
    public void be_healthy_when_all_ports_are_listening() {
        whenTheContainerHasAllPortsOpen();

        assertThat(healthCheck.isHealthy(container), successful());
    }

    @Test
    public void be_unhealthy_when_all_ports_are_not_listening() {
        whenTheContainerDoesNotHaveAllPortsOpen();

        assertThat(healthCheck.isHealthy(container), failure());
    }

    private void whenTheContainerDoesNotHaveAllPortsOpen() {
        when(container.areAllPortsOpen()).thenReturn(SuccessOrFailure.failure("not all ports open"));
    }

    private void whenTheContainerHasAllPortsOpen() {
        when(container.areAllPortsOpen()).thenReturn(SuccessOrFailure.success());
    }
}
