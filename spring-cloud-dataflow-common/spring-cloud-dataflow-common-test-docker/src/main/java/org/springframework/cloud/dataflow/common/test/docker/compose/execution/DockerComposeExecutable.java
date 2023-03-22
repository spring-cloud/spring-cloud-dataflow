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

import com.github.zafarkhaja.semver.Version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.DockerComposeFiles;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.ProjectName;

public class DockerComposeExecutable implements Executable {

	private static final Logger log = LoggerFactory.getLogger(DockerComposeExecutable.class);

	private static final DockerCommandLocations DOCKER_COMPOSE_LOCATIONS = new DockerCommandLocations(
			System.getenv("DOCKER_COMPOSE_LOCATION"),
			"/usr/local/bin/docker-compose",
			"/usr/bin/docker-compose"
	);

	private static String defaultDockerComposePath() {
		String pathToUse = DOCKER_COMPOSE_LOCATIONS.preferredLocation()
				.orElseThrow(() -> new IllegalStateException(
						"Could not find docker-compose, looked in: " + DOCKER_COMPOSE_LOCATIONS));

		log.debug("Using docker-compose found at " + pathToUse);

		return pathToUse;
	}

	static Version version() throws IOException, InterruptedException {
		Command dockerCompose = new Command(new Executable() {

			@Override
			public String commandName() {
				return "docker-compose";
			}

			@Override
			public Process execute(String... commands) throws IOException {
				List<String> args = new ArrayList<>();
				args.add(defaultDockerComposePath());
				args.addAll(Arrays.asList(commands));
				return new ProcessBuilder(args).redirectErrorStream(true).start();
			}
		}, log::trace);

		String versionOutput = dockerCompose.execute(Command.throwingOnError(), "-v");
		return DockerComposeVersion.parseFromDockerComposeVersion(versionOutput);
	}

	private DockerComposeFiles dockerComposeFiles;
	private DockerConfiguration dockerConfiguration;
	private ProjectName projectName = ProjectName.random();

	public DockerComposeExecutable(DockerComposeFiles dockerComposeFiles, DockerConfiguration dockerConfiguration, ProjectName projectName) {
		this.dockerComposeFiles = dockerComposeFiles;
		this.dockerConfiguration = dockerConfiguration;
		if (projectName != null) {
			this.projectName = projectName;
		}
	}

	public DockerComposeFiles dockerComposeFiles() {
		return dockerComposeFiles;
	}
	public DockerConfiguration dockerConfiguration() {
		return dockerConfiguration;
	}

	public ProjectName projectName() {
		return projectName;
//        return projectName != null ? projectName : ProjectName.random();
	}

	@Override
	public final String commandName() {
		return "docker-compose";
	}

	protected String dockerComposePath() {
		return defaultDockerComposePath();
	}

	@Override
	public Process execute(String... commands) throws IOException {
		DockerForMacHostsIssue.issueWarning();

		List<String> args = new ArrayList<>();
		args.add(dockerComposePath());
		args.addAll(projectName().constructComposeFileCommand());
		args.addAll(dockerComposeFiles().constructComposeFileCommand());
		args.addAll(Arrays.asList(commands));

		return dockerConfiguration().configuredDockerComposeProcess()
				.command(args)
				.redirectErrorStream(true)
				.start();
	}

	public static Builder builder() {
		return new Builder();
	}


	public static class Builder {
		private DockerComposeFiles dockerComposeFiles;
		private DockerConfiguration dockerConfiguration;
		private ProjectName projectName;

		public Builder dockerComposeFiles(DockerComposeFiles dockerComposeFiles) {
			this.dockerComposeFiles = dockerComposeFiles;
			return this;
		}

		public Builder dockerConfiguration(DockerConfiguration dockerConfiguration) {
			this.dockerConfiguration = dockerConfiguration;
			return this;
		}

		public Builder projectName(ProjectName projectName) {
			this.projectName = projectName;
			return this;
		}

		public DockerComposeExecutable build() {
			return new DockerComposeExecutable(dockerComposeFiles, dockerConfiguration, projectName);
		}
	}
}
