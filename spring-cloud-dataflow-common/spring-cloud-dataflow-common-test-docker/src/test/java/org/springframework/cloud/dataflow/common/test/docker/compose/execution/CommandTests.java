/*
 * Copyright 2018-2024 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.apache.commons.io.IOUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CommandTests {

    private Process executedProcess = mock(Process.class);
    private DockerComposeExecutable dockerComposeExecutable = mock(DockerComposeExecutable.class);
    private ErrorHandler errorHandler = mock(ErrorHandler.class);
    private Command dockerComposeCommand;
    private final List<String> consumedLogLines = new ArrayList<>();
    private final Consumer<String> logConsumer = s -> consumedLogLines.add(s);

    @BeforeEach
    void prepareForTest() throws IOException {
        when(dockerComposeExecutable.commandName()).thenReturn("docker-compose");
        when(dockerComposeExecutable.execute(anyBoolean(), any(String[].class))).thenReturn(executedProcess);
        dockerComposeCommand = new Command(dockerComposeExecutable, logConsumer);
        givenTheUnderlyingProcessHasOutput("");
        givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(0);
    }

    @Test
    void invokeErrorHandlerWhenExitCodeOfTheExecutedProcessIsNonZero() throws Exception {
        int expectedExitCode = 1;
        givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(expectedExitCode);
        dockerComposeCommand.execute(errorHandler, true, "rm", "-f");
        verify(errorHandler).handle(expectedExitCode, "", "docker-compose", "rm", "-f");
    }

    @Test
    void notInvokeErrorHandlerWhenExitCodeOfTheExecutedProcessIsZero() throws Exception {
        dockerComposeCommand.execute(errorHandler, true, "rm", "-f");
        verifyNoMoreInteractions(errorHandler);
    }

    @Test
    void returnOutputWhenExitCodeOfTheExecutedProcessIsNonZero() throws Exception {
        String expectedOutput = "test output";
        givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(1);
        givenTheUnderlyingProcessHasOutput(expectedOutput);
        String commandOutput = dockerComposeCommand.execute(errorHandler, true, "rm", "-f");
        assertThat(commandOutput, is(expectedOutput));
    }

    @Test
    void returnOutputWhenExitCodeOfTheExecutedProcessIsZero() throws Exception {
        String expectedOutput = "test output";
        givenTheUnderlyingProcessHasOutput(expectedOutput);
        String commandOutput = dockerComposeCommand.execute(errorHandler, true,"rm", "-f");
        assertThat(commandOutput, is(expectedOutput));
    }

    @Test
    void giveTheOutputToTheSpecifiedConsumerAsItIsAvailable() throws Exception {
        givenTheUnderlyingProcessHasOutput("line 1\nline 2");
        dockerComposeCommand.execute(errorHandler, true, "rm", "-f");
        assertThat(consumedLogLines, contains("line 1", "line 2"));
    }

    @Disabled("flaky test: https://circleci.com/gh/palantir/docker-compose-rule/378, 370, 367, 366")
    @Test
    void notCreateLongLivedThreadsAfterExecution() throws Exception {
        int preThreadCount = Thread.getAllStackTraces().entrySet().size();
        dockerComposeCommand.execute(errorHandler, true, "rm", "-f");
        int postThreadCount = Thread.getAllStackTraces().entrySet().size();
        assertThat("command thread pool has exited", preThreadCount == postThreadCount);
    }

    private void givenTheUnderlyingProcessHasOutput(String output) {
        when(executedProcess.getInputStream()).thenReturn(IOUtils.toInputStream(output));
    }

    private void givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(int exitCode) {
        when(executedProcess.exitValue()).thenReturn(exitCode);
    }

}
