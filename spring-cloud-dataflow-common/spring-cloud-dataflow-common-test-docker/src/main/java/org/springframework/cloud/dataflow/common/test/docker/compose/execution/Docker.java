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
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerMachine;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.State;
import org.springframework.util.Assert;

public class Docker {

	private static final Logger log = LoggerFactory.getLogger(Docker.class);

	// Without java escape characters: ^(\d+)\.(\d+)\.(\d+)(?:-.*)?$
	private static final Pattern VERSION_PATTERN = Pattern.compile("^Docker version (\\d+)\\.(\\d+)\\.(\\d+)(?:-.*)?$");
	private static final String HEALTH_STATUS_FORMAT = "--format=" + "{{if not .State.Running}}DOWN"
			+ "{{else if .State.Paused}}PAUSED" + "{{else if index .State \"Health\"}}"
			+ "{{if eq .State.Health.Status \"healthy\"}}HEALTHY" + "{{else}}UNHEALTHY{{end}}"
			+ "{{else}}HEALTHY{{end}}";
	private static final String HEALTH_STATUS_FORMAT_WINDOWS = HEALTH_STATUS_FORMAT.replaceAll("\"", "`\"");

	public static Version version() throws IOException, InterruptedException {
		return new Docker(DockerExecutable.builder().dockerConfiguration(DockerMachine.localMachine().build()).build())
				.configuredVersion();
	}

	public Version configuredVersion() throws IOException, InterruptedException {
		String versionString = command.execute(Command.throwingOnError(), false, "-v");
		Matcher matcher = VERSION_PATTERN.matcher(versionString);
		Assert.state(matcher.matches(), "Unexpected output of docker -v: " + versionString);
		return Version.forIntegers(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
				Integer.parseInt(matcher.group(3)));
	}

	private final Command command;

	public Docker(DockerExecutable rawExecutable) {
		this.command = new Command(rawExecutable, log::trace);
	}

	public State state(String containerId) throws IOException, InterruptedException {
		String formatString = SystemUtils.IS_OS_WINDOWS ? HEALTH_STATUS_FORMAT_WINDOWS : HEALTH_STATUS_FORMAT;
		String stateString = command.execute(Command.throwingOnError(), false,"inspect", formatString, containerId);
		return State.valueOf(stateString);
	}

	public void rm(Collection<String> containerNames) throws IOException, InterruptedException {
		rm(containerNames.toArray(new String[containerNames.size()]));
	}

	public void rm(String... containerNames) throws IOException, InterruptedException {
		List<String> commands = new ArrayList<>();
		commands.add("rm");
		commands.add("-f");
		if (containerNames != null) {
			for (String containerName : containerNames) {
				commands.add(containerName);
			}
		}
		command.execute(Command.throwingOnError(), false, commands.toArray(new String[0]));
	}

	public String listNetworks() throws IOException, InterruptedException {
		return command.execute(Command.throwingOnError(), false, "network", "ls");
	}

	public String pruneNetworks() throws IOException, InterruptedException {
		return command.execute(Command.throwingOnError(), false,"network", "prune", "--force");
	}
}
