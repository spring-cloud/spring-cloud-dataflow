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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommandTests {
    @Mock private Process executedProcess;
    @Mock private DockerComposeExecutable dockerComposeExecutable;
    @Mock private ErrorHandler errorHandler;
    private Command dockerComposeCommand;
    private final List<String> consumedLogLines = new ArrayList<>();
    private final Consumer<String> logConsumer = s -> consumedLogLines.add(s);

    @Before
    public void before() throws IOException {
        when(dockerComposeExecutable.execute(any())).thenReturn(executedProcess);
        dockerComposeCommand = new Command(dockerComposeExecutable, logConsumer);

        givenTheUnderlyingProcessHasOutput("");
        givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(0);
    }

    @Test public void
    invoke_error_handler_when_exit_code_of_the_executed_process_is_non_0() throws IOException, InterruptedException {
        int expectedExitCode = 1;
        givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(expectedExitCode);
        dockerComposeCommand.execute(errorHandler, "rm", "-f");

        verify(errorHandler).handle(expectedExitCode, "", "docker-compose", "rm", "-f");
    }

    @Test public void
    not_invoke_error_handler_when_exit_code_of_the_executed_process_is_0() throws IOException, InterruptedException {
        dockerComposeCommand.execute(errorHandler, "rm", "-f");

        verifyNoMoreInteractions(errorHandler);
    }

    @Test public void
    return_output_when_exit_code_of_the_executed_process_is_non_0() throws IOException, InterruptedException {
        String expectedOutput = "test output";
        givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(1);
        givenTheUnderlyingProcessHasOutput(expectedOutput);
        String commandOutput = dockerComposeCommand.execute(errorHandler, "rm", "-f");

        assertThat(commandOutput, is(expectedOutput));
    }

    @Test public void
    return_output_when_exit_code_of_the_executed_process_is_0() throws IOException, InterruptedException {
        String expectedOutput = "test output";
        givenTheUnderlyingProcessHasOutput(expectedOutput);
        String commandOutput = dockerComposeCommand.execute(errorHandler, "rm", "-f");

        assertThat(commandOutput, is(expectedOutput));
    }

    @Test public void
    give_the_output_to_the_specified_consumer_as_it_is_available() throws IOException, InterruptedException {
        givenTheUnderlyingProcessHasOutput("line 1\nline 2");

        dockerComposeCommand.execute(errorHandler, "rm", "-f");

        assertThat(consumedLogLines, contains("line 1", "line 2"));
    }

    // flaky test: https://circleci.com/gh/palantir/docker-compose-rule/378, 370, 367, 366
    @Ignore
    @Test public void
    not_create_long_lived_threads_after_execution() throws IOException, InterruptedException {
        int preThreadCount = Thread.getAllStackTraces().entrySet().size();
        dockerComposeCommand.execute(errorHandler, "rm", "-f");
        int postThreadCount = Thread.getAllStackTraces().entrySet().size();
        assertThat("command thread pool has exited", preThreadCount == postThreadCount);
    }

    private void givenTheUnderlyingProcessHasOutput(String output) {
        when(executedProcess.getInputStream()).thenReturn(toInputStream(output));
    }

    private void givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(int exitCode) {
        when(executedProcess.exitValue()).thenReturn(exitCode);
    }

}
