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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.io.IOUtils;
import org.awaitility.Awaitility;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.DockerComposeFiles;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.ProjectName;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.Container;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.ContainerName;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.ContainerNames;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerMachine;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.Ports;
import org.springframework.util.StringUtils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.Validate.validState;
import static org.joda.time.Duration.standardMinutes;

public class DefaultDockerCompose implements DockerCompose {

	public static final Version VERSION_1_7_0 = Version.valueOf("1.7.0");
	private static final Duration COMMAND_TIMEOUT = standardMinutes(2);
	private static final Duration LOG_WAIT_TIMEOUT = standardMinutes(30);
	private static final Logger log = LoggerFactory.getLogger(DefaultDockerCompose.class);

	private final Command command;
	private final DockerMachine dockerMachine;
	private final DockerComposeExecutable rawExecutable;


	public DefaultDockerCompose(DockerComposeFiles dockerComposeFiles, DockerMachine dockerMachine, ProjectName projectName) {
		this(DockerComposeExecutable.builder()
				.dockerComposeFiles(dockerComposeFiles)
				.dockerConfiguration(dockerMachine)
				.projectName(projectName)
				.build(), dockerMachine);
	}

	public DefaultDockerCompose(DockerComposeExecutable rawExecutable, DockerMachine dockerMachine) {
		this.rawExecutable = rawExecutable;
		this.command = new Command(rawExecutable, log::debug);
		this.dockerMachine = dockerMachine;
	}

	@Override
	public void pull() throws IOException, InterruptedException {
		command.execute(Command.throwingOnError(), true, "pull");
	}

	@Override
	public void build() throws IOException, InterruptedException {
		command.execute(Command.throwingOnError(), true, "build");
	}

	@Override
	public void up() throws IOException, InterruptedException {
		command.execute(Command.throwingOnError(), true, "up", "-d");
	}

	@Override
	public void down() throws IOException, InterruptedException {
		command.execute(swallowingDownCommandDoesNotExist(), true, "down", "--volumes");
	}

	@Override
	public void kill() throws IOException, InterruptedException {
		command.execute(Command.throwingOnError(), true, "kill");
	}

	@Override
	public void rm() throws IOException, InterruptedException {
		command.execute(Command.throwingOnError(), true, "rm", "--force", "-v");
	}

	@Override
	public void up(Container container) throws IOException, InterruptedException {
		command.execute(Command.throwingOnError(), true, "up", "-d", container.getContainerName());
	}

	@Override
	public void start(Container container) throws IOException, InterruptedException {
		command.execute(Command.throwingOnError(), true, "start", container.getContainerName());
	}

	@Override
	public void stop(Container container) throws IOException, InterruptedException {
		command.execute(Command.throwingOnError(), true, "stop", container.getContainerName());
	}

	@Override
	public void kill(Container container) throws IOException, InterruptedException {
		command.execute(Command.throwingOnError(), true, "kill", container.getContainerName());
	}

	@Override
	public String exec(DockerComposeExecOption dockerComposeExecOption, String containerName,
			DockerComposeExecArgument dockerComposeExecArgument) throws IOException, InterruptedException {
		verifyDockerComposeVersionAtLeast(VERSION_1_7_0, "You need at least docker-compose 1.7 to run docker-compose exec");
		String[] fullArgs = constructFullDockerComposeExecArguments(dockerComposeExecOption, containerName, dockerComposeExecArgument);
		if (log.isDebugEnabled()) {
			log.debug("exec:{}", StringUtils.collectionToDelimitedString(Arrays.asList(fullArgs), " "));
		}
		return command.execute(Command.throwingOnError(), true, fullArgs);
	}

	@Override
	public String run(DockerComposeRunOption dockerComposeRunOption, String containerName,
			DockerComposeRunArgument dockerComposeRunArgument) throws IOException, InterruptedException {
		String[] fullArgs = constructFullDockerComposeRunArguments(dockerComposeRunOption, containerName, dockerComposeRunArgument);
		return command.execute(Command.throwingOnError(), true, fullArgs);
	}

	private void verifyDockerComposeVersionAtLeast(Version targetVersion, String message) throws IOException, InterruptedException {
		validState(version().isHigherThanOrEquivalentTo(targetVersion), message);
	}

	private Version version() throws IOException, InterruptedException {
		String versionOutput = command.execute(Command.throwingOnError(), false, "-v");
		return DockerComposeVersion.parseFromDockerComposeVersion(versionOutput);
	}

	private static String[] constructFullDockerComposeExecArguments(DockerComposeExecOption dockerComposeExecOption,
			String containerName, DockerComposeExecArgument dockerComposeExecArgument) {
		// The "-T" option here disables pseudo-TTY allocation, which is not useful here since we are not using
		// terminal features here (e.g. we are not sending ^C to kill the executed process).
		// Disabling pseudo-TTY allocation means this will work on OS's that don't support TTY (i.e. Windows)
		List<String> fullArgs = new ArrayList<>();
		fullArgs.add("exec");
		fullArgs.add("-T");
		fullArgs.addAll(dockerComposeExecOption.options());
		fullArgs.add(containerName);
		fullArgs.addAll(dockerComposeExecArgument.arguments());
		return fullArgs.toArray(new String[fullArgs.size()]);
	}

	private static String[] constructFullDockerComposeRunArguments(DockerComposeRunOption dockerComposeRunOption,
			String containerName, DockerComposeRunArgument dockerComposeRunArgument) {
			List<String> fullArgs = new ArrayList<>();
			fullArgs.add("run");
			fullArgs.addAll(dockerComposeRunOption.options());
			fullArgs.add(containerName);
			fullArgs.addAll(dockerComposeRunArgument.arguments());
		return fullArgs.toArray(new String[fullArgs.size()]);
	}

	@Override
	public List<ContainerName> ps() throws IOException, InterruptedException {
		String psOutput = command.execute(Command.throwingOnError(), true, "ps");
		return ContainerNames.parseFromDockerComposePs(psOutput);
	}

	@Override
	public Optional<String> id(Container container) throws IOException, InterruptedException {
		return id(container.getContainerName());
	}

	@Override
	public String config() throws IOException, InterruptedException {
		return command.execute(Command.throwingOnError(), true, "config");
	}

	@Override
	public List<String> services() throws IOException, InterruptedException {
		String servicesOutput = command.execute(Command.throwingOnError(), true, "config", "--services");
		return Arrays.asList(servicesOutput.split("(\r|\n)+"));
	}

	/**
	 * Blocks until all logs collected from the container.
	 * @return Whether the docker container terminated prior to log collection ending
	 */
	@Override
	public boolean writeLogs(String container, OutputStream output) throws IOException {
		try {
			Awaitility.await()
					.pollInterval(50, TimeUnit.MILLISECONDS)
					.atMost(LOG_WAIT_TIMEOUT.getMillis(), TimeUnit.MILLISECONDS)
					.until(() -> exists(container));
			Process executedProcess = followLogs(container);
			IOUtils.copy(executedProcess.getInputStream(), output);
			executedProcess.waitFor(COMMAND_TIMEOUT.getMillis(), MILLISECONDS);
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	private boolean exists(final String containerName) throws IOException, InterruptedException {
		return id(containerName).orElse(null) != null;
	}

	private Optional<String> id(String containerName) throws IOException, InterruptedException {
		String id = command.execute(Command.throwingOnError(), true, "ps", "-q", containerName);
		if (id.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(id);
	}

	private Process followLogs(String container) throws IOException, InterruptedException {
		if (version().greaterThanOrEqualTo(VERSION_1_7_0)) {
			return rawExecutable.execute(true, "logs", "--no-color", "--follow", container);
		}

		return rawExecutable.execute(true, "logs", "--no-color", container);
	}

	@Override
	public Ports ports(String service) throws IOException, InterruptedException {
		return Ports.parseFromDockerComposePs(psOutput(service), dockerMachine.getIp());
	}

	private static ErrorHandler swallowingDownCommandDoesNotExist() {
		return (exitCode, output, commandName, commands) -> {
			if (downCommandWasPresent(output)) {
				Command.throwingOnError().handle(exitCode, output, commandName, commands);
			}

			log.warn("It looks like `docker-compose down` didn't work.");
			log.warn("This probably means your version of docker-compose doesn't support the `down` command");
			log.warn("Updating to version 1.6+ of docker-compose is likely to fix this issue.");
		};
	}

	private static boolean downCommandWasPresent(String output) {
		return !output.contains("No such command");
	}

	private String psOutput(String service) throws IOException, InterruptedException {
		String psOutput = command.execute(Command.throwingOnError(), true, "ps", service);
		validState(StringUtils.hasText(psOutput), "No container with name '" + service + "' found");
		return psOutput;
	}
}
