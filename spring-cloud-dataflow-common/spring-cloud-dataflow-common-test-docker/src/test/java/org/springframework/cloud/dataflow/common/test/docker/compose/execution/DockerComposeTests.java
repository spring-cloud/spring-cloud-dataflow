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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.dataflow.common.test.docker.compose.connection.Container;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.ContainerName;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerMachine;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerPort;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.Ports;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerComposeExecArgument.arguments;
import static org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerComposeExecOption.options;

public class DockerComposeTests {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final DockerComposeExecutable executor = mock(DockerComposeExecutable.class);
    private final DockerMachine dockerMachine = mock(DockerMachine.class);
    private final DockerCompose compose = new DefaultDockerCompose(executor, dockerMachine);

    private final Process executedProcess = mock(Process.class);
    private final Container container = mock(Container.class);

    @Before
    public void before() throws IOException {
        when(dockerMachine.getIp()).thenReturn("0.0.0.0");
        when(executor.execute(any())).thenReturn(executedProcess);
        when(executedProcess.getInputStream()).thenReturn(toInputStream("0.0.0.0:7000->7000/tcp"));
        when(executedProcess.exitValue()).thenReturn(0);
        when(container.getContainerName()).thenReturn("my-container");
    }

    @Test
    public void call_docker_compose_up_with_daemon_flag_on_up() throws IOException, InterruptedException {
        compose.up();
        verify(executor).execute("up", "-d");
    }

    @Test
    public void call_docker_compose_rm_with_force_and_volume_flags_on_rm() throws IOException, InterruptedException {
        compose.rm();
        verify(executor).execute("rm", "--force", "-v");
    }

    @Test
    public void call_docker_compose_stop_on_stop() throws IOException, InterruptedException {
        compose.stop(container);
        verify(executor).execute("stop", "my-container");
    }

    @Test
    public void call_docker_compose_start_on_start() throws IOException, InterruptedException {
        compose.start(container);
        verify(executor).execute("start", "my-container");
    }

    @Test
    public void parse_and_returns_container_names_on_ps() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(toInputStream("ps\n----\ndir_db_1"));
        List<ContainerName> containerNames = compose.ps();
        verify(executor).execute("ps");
        assertThat(containerNames, contains(ContainerName.builder().semanticName("db").rawName("dir_db_1").build()));
    }

    @Test
    public void call_docker_compose_with_no_colour_flag_on_logs() throws IOException {
        when(executedProcess.getInputStream()).thenReturn(
                toInputStream("id"),
                toInputStream("docker-compose version 1.5.6, build 1ad8866"),
                toInputStream("logs"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        compose.writeLogs("db", output);
        verify(executor).execute("logs", "--no-color", "db");
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8), is("logs"));
    }

    @Test
    public void call_docker_compose_with_no_container_on_logs() throws IOException {
        reset(executor);
        final Process mockIdProcess = mock(Process.class);
        when(mockIdProcess.exitValue()).thenReturn(0);
        final InputStream emptyStream = toInputStream("");
        when(mockIdProcess.getInputStream()).thenReturn(emptyStream, emptyStream, emptyStream, toInputStream("id"));

        final Process mockVersionProcess = mock(Process.class);
        when(mockVersionProcess.exitValue()).thenReturn(0);
        when(mockVersionProcess.getInputStream()).thenReturn(toInputStream("docker-compose version 1.5.6, build 1ad8866"));
        when(executor.execute("ps", "-q", "db")).thenReturn(mockIdProcess);
        when(executor.execute("-v")).thenReturn(mockVersionProcess);
        when(executor.execute("logs", "--no-color", "db")).thenReturn(executedProcess);
        when(executedProcess.getInputStream()).thenReturn(toInputStream("logs"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        compose.writeLogs("db", output);
        verify(executor, times(4)).execute("ps", "-q", "db");
        verify(executor).execute("logs", "--no-color", "db");
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8), is("logs"));
    }

    @Test
    public void call_docker_compose_with_the_follow_flag_when_the_version_is_at_least_1_7_0_on_logs()
            throws IOException {
        when(executedProcess.getInputStream()).thenReturn(
                toInputStream("id"),
                toInputStream("docker-compose version 1.7.0, build 1ad8866"),
                toInputStream("logs"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        compose.writeLogs("db", output);
        verify(executor).execute("logs", "--no-color", "--follow", "db");
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8), is("logs"));
    }

    @Test
    public void throw_exception_when_kill_exits_with_a_non_zero_exit_code() throws IOException, InterruptedException {
        when(executedProcess.exitValue()).thenReturn(1);
        exception.expect(DockerExecutionException.class);
        exception.expectMessage("'docker-compose kill' returned exit code 1");
        compose.kill();
    }

    @Test
    public void not_throw_exception_when_down_fails_because_the_command_does_not_exist()
            throws IOException, InterruptedException {
        when(executedProcess.exitValue()).thenReturn(1);
        when(executedProcess.getInputStream()).thenReturn(toInputStream("No such command: down"));
        compose.down();
    }

    @Test
    public void throw_exception_when_down_fails_for_a_reason_other_than_the_command_not_being_present()
            throws IOException, InterruptedException {
        when(executedProcess.exitValue()).thenReturn(1);
        when(executedProcess.getInputStream()).thenReturn(toInputStream(""));

        exception.expect(DockerExecutionException.class);

        compose.down();
    }

    @Test
    public void use_the_remove_volumes_flag_when_down_exists() throws IOException, InterruptedException {
        compose.down();
        verify(executor).execute("down", "--volumes");
    }

    @Test
    public void parse_the_ps_output_on_ports() throws IOException, InterruptedException {
        Ports ports = compose.ports("db");
        verify(executor).execute("ps", "db");
        assertThat(ports, is(new Ports(new DockerPort("0.0.0.0", 7000, 7000))));
    }

    @Test
    public void throw_illegal_state_exception_when_there_is_no_container_found_for_ports()
            throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(toInputStream(""));
        exception.expect(IllegalStateException.class);
        exception.expectMessage("No container with name 'db' found");
        compose.ports("db");
    }

    @Test
    public void pass_concatenated_arguments_to_executor_on_docker_compose_exec()
            throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(toInputStream("docker-compose version 1.7.0rc1, build 1ad8866"));
        compose.exec(options("-d"), "container_1", arguments("ls"));
        verify(executor, times(1)).execute("exec", "-T", "-d", "container_1", "ls");
    }

    @Test
    public void fail_if_docker_compose_version_is_prior_1_7_on_docker_compose_exec()
            throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(toInputStream("docker-compose version 1.5.6, build 1ad8866"));
        exception.expect(IllegalStateException.class);
        exception.expectMessage("You need at least docker-compose 1.7 to run docker-compose exec");
        compose.exec(options("-d"), "container_1", arguments("ls"));
    }

    @Test
    public void pass_concatenated_arguments_to_executor_on_docker_compose_run()
            throws IOException, InterruptedException {
        compose.run(DockerComposeRunOption.options("-d"), "container_1", DockerComposeRunArgument.arguments("ls"));
        verify(executor, times(1)).execute("run", "-d", "container_1", "ls");
    }

    @Test
    public void return_the_output_from_the_executed_process_on_docker_compose_exec() throws Exception {
        String lsString = String.format("-rw-r--r--  1 user  1318458867  11326 Mar  9 17:47 LICENSE%n"
                                        + "-rw-r--r--  1 user  1318458867  12570 May 12 14:51 README.md");

        String versionString = "docker-compose version 1.7.0rc1, build 1ad8866";

        DockerComposeExecutable processExecutor = mock(DockerComposeExecutable.class);

        addProcessToExecutor(processExecutor, processWithOutput(versionString), "-v");
        addProcessToExecutor(processExecutor, processWithOutput(lsString), "exec", "-T", "container_1", "ls", "-l");

        DockerCompose processCompose = new DefaultDockerCompose(processExecutor, dockerMachine);

        assertThat(processCompose.exec(options(), "container_1", arguments("ls", "-l")), is(lsString));
    }

    @Test
    public void return_the_output_from_the_executed_process_on_docker_compose_run() throws Exception {
        String lsString = String.format("-rw-r--r--  1 user  1318458867  11326 Mar  9 17:47 LICENSE%n"
                                        + "-rw-r--r--  1 user  1318458867  12570 May 12 14:51 README.md");

        DockerComposeExecutable processExecutor = mock(DockerComposeExecutable.class);

        addProcessToExecutor(processExecutor, processWithOutput(lsString), "run", "-it", "container_1", "ls", "-l");

        DockerCompose processCompose = new DefaultDockerCompose(processExecutor, dockerMachine);

        assertThat(processCompose.run(DockerComposeRunOption.options("-it"), "container_1", DockerComposeRunArgument.arguments("ls", "-l")), is(lsString));
    }

    private static void addProcessToExecutor(DockerComposeExecutable dockerComposeExecutable, Process process, String... commands) throws Exception {
        when(dockerComposeExecutable.execute(commands)).thenReturn(process);
    }

    private static Process processWithOutput(String output) {
        Process mockedProcess = mock(Process.class);
        when(mockedProcess.getInputStream()).thenReturn(toInputStream(output));
        when(mockedProcess.exitValue()).thenReturn(0);
        return mockedProcess;
    }

}
