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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class ConflictingContainerRemovingDockerCompose extends DelegatingDockerCompose {
	private static final Logger log = LoggerFactory.getLogger(ConflictingContainerRemovingDockerCompose.class);
	private static final Pattern NAME_CONFLICT_PATTERN = Pattern.compile("name \"([^\"]*)\" is already in use");

	private final Docker docker;
	private final int retryAttempts;

	public ConflictingContainerRemovingDockerCompose(DockerCompose dockerCompose, Docker docker) {
		this(dockerCompose, docker, 1);
	}

	public ConflictingContainerRemovingDockerCompose(DockerCompose dockerCompose, Docker docker, int retryAttempts) {
		super(dockerCompose);

		Assert.state(retryAttempts >= 1, "retryAttempts must be at least 1, was " + retryAttempts);
		this.docker = docker;
		this.retryAttempts = retryAttempts;
	}

	@Override
	public void up() throws IOException, InterruptedException {
		for (int currRetryAttempt = 0; currRetryAttempt <= retryAttempts; currRetryAttempt++) {
			try {
				getDockerCompose().up();
				return;
			} catch (DockerExecutionException e) {
				Set<String> conflictingContainerNames = getConflictingContainerNames(e.getMessage());
				if (conflictingContainerNames.isEmpty()) {
					// failed due to reason other than conflicting containers, so re-throw
					throw e;
				}

				log.debug("docker-compose up failed due to container name conflicts (container names: {}). "
								+ "Removing containers and attempting docker-compose up again (attempt {}).",
						conflictingContainerNames, currRetryAttempt + 1);
				removeContainers(conflictingContainerNames);
			}
		}

		throw new DockerExecutionException("docker-compose up failed");
	}

	private void removeContainers(Collection<String> containerNames) throws IOException, InterruptedException {
		try {
			docker.rm(containerNames);
		} catch (DockerExecutionException e) {
			// there are cases such as in CircleCI where 'docker rm' returns a non-0 exit code and "fails",
			// but container is still effectively removed as far as conflict resolution is concerned. Because
			// of this, be permissive and do not fail task even if 'rm' fails.
			log.debug("docker rm failed, but continuing execution", e);
		}
	}

	Set<String> getConflictingContainerNames(String output) {
		HashSet<String> set = new HashSet<>();
		Matcher matcher = NAME_CONFLICT_PATTERN.matcher(output);
		while (matcher.find()) {
			set.add(matcher.group(1));
		}
		return set;
	}

}
