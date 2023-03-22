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
package org.springframework.cloud.dataflow.common.test.docker.compose.execution;

import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.ShutdownStrategy;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

public class KillDownShutdownStrategyTests {

    @Test
    public void call_kill_then_down() throws Exception {
        DockerCompose dockerCompose = mock(DockerCompose.class);
        Docker docker = mock(Docker.class);

        ShutdownStrategy.KILL_DOWN.shutdown(dockerCompose, docker);

        InOrder inOrder = inOrder(dockerCompose, docker);
        inOrder.verify(dockerCompose).kill();
        inOrder.verify(dockerCompose).down();
        inOrder.verify(docker).pruneNetworks();
        inOrder.verifyNoMoreInteractions();
    }
}
