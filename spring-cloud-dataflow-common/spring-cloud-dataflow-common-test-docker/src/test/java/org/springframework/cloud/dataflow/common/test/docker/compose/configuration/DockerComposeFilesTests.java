/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.cloud.dataflow.common.test.docker.compose.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.DockerComposeFiles;

public class DockerComposeFilesTests {

	@Rule
	public final TemporaryFolder tempFolder = new TemporaryFolder();

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void throw_exception_when_compose_file_is_not_specified() {
		exception.expect(IllegalStateException.class);
		exception.expectMessage("A docker compose file must be specified.");
		DockerComposeFiles.from();
	}

	@Test
	public void throw_exception_when_compose_file_does_not_exist() {
		exception.expect(IllegalStateException.class);
		exception.expectMessage("The following docker-compose files:");
		exception.expectMessage("does-not-exist.yaml");
		exception.expectMessage("do not exist.");
		DockerComposeFiles.from("does-not-exist.yaml");
	}

	@Test
	public void
			throw_correct_exception_when_there_is_a_single_missing_compose_file_with_an_existing_compose_file()
			throws Exception {
		exception.expect(IllegalStateException.class);
		exception.expectMessage("The following docker-compose files:");
		exception.expectMessage("does-not-exist.yaml");
		exception.expectMessage("do not exist.");
		exception.expectMessage(not(containsString("docker-compose.yaml")));

		File composeFile = tempFolder.newFile("docker-compose.yaml");
		DockerComposeFiles.from("does-not-exist.yaml", composeFile.getAbsolutePath());
	}

	@Test
	public void generate_docker_compose_file_command_correctly_for_single_compose_file() throws Exception {
		File composeFile = tempFolder.newFile("docker-compose.yaml");
		DockerComposeFiles dockerComposeFiles = DockerComposeFiles.from(composeFile.getAbsolutePath());
		assertThat(dockerComposeFiles.constructComposeFileCommand(), contains("--file", composeFile.getAbsolutePath()));
	}

	@Test
	public void generate_docker_compose_file_command_correctly_for_multiple_compose_files() throws Exception {
		File composeFile1 = tempFolder.newFile("docker-compose1.yaml");
		File composeFile2 = tempFolder.newFile("docker-compose2.yaml");
		DockerComposeFiles dockerComposeFiles = DockerComposeFiles.from(composeFile1.getAbsolutePath(), composeFile2.getAbsolutePath());
		assertThat(dockerComposeFiles.constructComposeFileCommand(), contains(
				"--file", composeFile1.getAbsolutePath(), "--file", composeFile2.getAbsolutePath()));
	}

	@Test
	public void testFromClasspathExist() {
		DockerComposeFiles dockerComposeFiles = DockerComposeFiles.from("classpath:docker-compose-cp1.yaml",
				"classpath:org/springframework/cloud/dataflow/common/test/docker/compose/docker-compose-cp2.yaml");
		assertThat(dockerComposeFiles.constructComposeFileCommand(), contains(is("--file"),
				containsString("docker-compose-cp1.yaml"), is("--file"), containsString("docker-compose-cp2.yaml")));
	}

	@Test
	public void testFromClasspathDoesNotExist() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Can't find resource classpath:does-not-exist.yaml");
		DockerComposeFiles.from("classpath:does-not-exist.yaml");
	}
}
