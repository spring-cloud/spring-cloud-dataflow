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

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.dataflow.common.test.docker.compose.TestContainerNames;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.ContainerName;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.Retryer.RetryableDockerOperation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerComposeExecArgument.arguments;
import static org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerComposeExecOption.options;

public class RetryingDockerComposeTests {
    private final DockerCompose dockerCompose = mock(DockerCompose.class);
    private final Retryer retryer = mock(Retryer.class);
    private final RetryingDockerCompose retryingDockerCompose = new RetryingDockerCompose(retryer, dockerCompose);
    private final List<ContainerName> someContainerNames = TestContainerNames.of("hey");
    private static final String CONTAINER_NAME = "container";

    @Before
    public void before() throws IOException, InterruptedException {
        retryerJustCallsOperation();
    }

    private void retryerJustCallsOperation() throws IOException, InterruptedException {
        when(retryer.runWithRetries(anyOperation())).thenAnswer(invocation -> {
            Retryer.RetryableDockerOperation<?> operation = (Retryer.RetryableDockerOperation<?>) invocation.getArguments()[0];
            return operation.call();
        });
    }

    private static RetryableDockerOperation<?> anyOperation() {
        return any(Retryer.RetryableDockerOperation.class);
    }

    @Test
    public void calls_up_on_the_underlying_docker_compose() throws IOException, InterruptedException {
        retryingDockerCompose.up();

        verifyRetryerWasUsed();
        verify(dockerCompose).up();
        verifyNoMoreInteractions(dockerCompose);
    }

    @Test
    public void call_ps_on_the_underlying_docker_compose_and_returns_the_same_value() throws IOException, InterruptedException {
        when(dockerCompose.ps()).thenReturn(someContainerNames);

		assertThat(retryingDockerCompose.ps()).isEqualTo(someContainerNames);

        verifyRetryerWasUsed();
        verify(dockerCompose).ps();
        verifyNoMoreInteractions(dockerCompose);
    }

    private void verifyRetryerWasUsed() throws IOException, InterruptedException {
        verify(retryer).runWithRetries(anyOperation());
    }

    private void verifyRetryerWasNotUsed() throws IOException, InterruptedException {
        verify(retryer, times(0)).runWithRetries(anyOperation());
    }

    @Test
    public void calls_exec_on_the_underlying_docker_compose_and_not_invoke_retryer() throws IOException, InterruptedException {
        retryingDockerCompose.exec(options("-d"), CONTAINER_NAME, arguments("ls"));
        verifyRetryerWasNotUsed();
        verify(dockerCompose).exec(options("-d"), CONTAINER_NAME, arguments("ls"));
    }

    @Test
    public void calls_run_on_the_underlying_docker_compose_and_not_invoke_retryer() throws IOException, InterruptedException {
        retryingDockerCompose.run(DockerComposeRunOption.options("-d"), CONTAINER_NAME, DockerComposeRunArgument.arguments("ls"));
        verifyRetryerWasNotUsed();
        verify(dockerCompose).run(DockerComposeRunOption.options("-d"), CONTAINER_NAME, DockerComposeRunArgument.arguments("ls"));
    }
}
