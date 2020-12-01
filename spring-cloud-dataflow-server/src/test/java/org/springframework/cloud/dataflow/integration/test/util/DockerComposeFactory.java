/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.dataflow.integration.test.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.palantir.docker.compose.DockerComposeExtension;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory utility that helps to install SCDF & Skipper from Docker Compose files.
 *
 * Test fixture applies the same docker compose files used for the Data Flow local installation:
 *  - https://dataflow.spring.io/docs/installation/local/docker/
 *  - https://dataflow.spring.io/docs/installation/local/docker-customize/
 *
 * The Palantir DockerMachine and DockerComposeExtension are used to programmatically deploy the docker-compose files.
 *
 * All important bootstrap parameters are configurable via the {@link DockerComposeFactoryProperties} properties and variables.
 *
 * @author Christian Tzolov
 */
public class DockerComposeFactory {

	private static final Logger logger = LoggerFactory.getLogger(DockerComposeFactory.class);

	/**
	 * Data Flow version to use for the tests.
	 */
	public static final String DEFAULT_DATAFLOW_VERSION = "2.7.1-SNAPSHOT";

	/**
	 * Skipper version used for the tests.
	 */
	public static final String DEFAULT_SKIPPER_VERSION = "2.6.1-SNAPSHOT";

	/**
	 * Pre-registered Task apps used for testing.
	 */
	public static final String DEFAULT_TASK_APPS_URI = "https://dataflow.spring.io/task-maven-latest&force=true";

	/**
	 * Common Apps URIs
	 */
	public static final String KAFKA_MAVEN_STREAM_APPS_URI = "https://dataflow.spring.io/kafka-maven-latest&force=true"; // local/kafka
	public static final String RABBITMQ_MAVEN_STREAM_APPS_URI = "https://dataflow.spring.io/rabbitmq-maven-latest&force=true"; // cf or local/rabbit
	public static final String KAFKA_DOCKER_STREAM_APPS_URI = "https://dataflow.spring.io/kafka-docker-latest&force=true"; // k8s

	/**
	 * Pre-registered Stream apps used in the tests
	 */
	private static final String DEFAULT_STREAM_APPS_URI = KAFKA_MAVEN_STREAM_APPS_URI;

	/**
	 * List of docker compose files to mix to bootstrap as a test environment. Most files are found under the
	 * 'spring-cloud-dataflow/spring-cloud-dataflow-server' folder.
	 */
	private static final String[] DEFAULT_DOCKER_COMPOSE_PATHS = {
			"docker-compose.yml", // Configures DataFlow, Skipper, Kafka/Zookeeper and MySQL
			"docker-compose-prometheus.yml" //,   // metrics collection/visualization with Prometheus and Grafana.
			//"docker-compose-influxdb.yml",     // metrics collection/visualization with InfluxDB and Grafana.
			//"docker-compose-wavefront.yml",     // metrics collection/visualization with Wavefront.
			//"docker-compose-postgres.yml",     // Replaces local MySQL database by Postgres.
			//"docker-compose-rabbitmq.yml",     // Replaces local Kafka message broker by RabbitMQ.
			//"docker-compose-k8s.yml",          // Adds K8s target platform (called k8s).
			//"docker-compose-cf.yml"            // Adds CloudFoundry target platform (called cf).
	};

	private static boolean isDood = DockerComposeFactoryProperties.getBoolean(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_DOOD, false);

	/**
	 * Initialize the docker machine with the required environment variables.
	 */
	private static DockerMachine dockerMachine = DockerMachine.localMachine()
			.withAdditionalEnvironmentVariable("DATAFLOW_VERSION",
					DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_DATAFLOW_VERSIONN, DEFAULT_DATAFLOW_VERSION))
			.withAdditionalEnvironmentVariable("SKIPPER_VERSION",
					DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_SKIPPER_VERSIONN, DEFAULT_SKIPPER_VERSION))
			.withAdditionalEnvironmentVariable("STREAM_APPS_URI",
					DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_STREAM_APPS_URI, (isDood ? KAFKA_DOCKER_STREAM_APPS_URI : DEFAULT_STREAM_APPS_URI)))
			.withAdditionalEnvironmentVariable("TASK_APPS_URI",
					DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_TASK_APPS_URI, (isDood ? "https://dataflow.spring.io/task-docker-latest" : DEFAULT_TASK_APPS_URI)))
			.withAdditionalEnvironmentVariable("DOCKER_DELETE_CONTAINER_ON_EXIT",
					"" + DockerComposeFactoryProperties.getBoolean(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_DOCKER_DELETE_CONTAINER_ON_EXIT, true))
			.withAdditionalEnvironmentVariable("METADATA_DEFAULT_DOCKERHUB_USER", DockerComposeFactoryProperties.get("METADATA_DEFAULT_DOCKERHUB_USER", ""))
			.withAdditionalEnvironmentVariable("METADATA_DEFAULT_DOCKERHUB_PASSWORD", DockerComposeFactoryProperties.get("METADATA_DEFAULT_DOCKERHUB_PASSWORD", ""))
			.withAdditionalEnvironmentVariable("COMPOSE_PROJECT_NAME", "scdf")
			.build();

	public static Extension startDockerCompose(Path tempFolder) {

		if (DockerComposeFactoryProperties.isDockerComposeDisabled()) {
			return (BeforeAllCallback) context -> logger.info("Docker Compose installation is disabled!");
		}

		logger.info("Docker Compose based Integration Tests. \nFollowing environment variables or properties can be used to configure the testing fixture: \n" +
				" - TEST_DOCKER_COMPOSE_PATHS (test.docker.compose.paths) configures the list of docker-compose files. \n" +
				" - TEST_DOCKER_COMPOSE_DATAFLOW_VERSION (test.docker.compose.dataflow.version) sets the Data Flow version used for testing. \n" +
				" - TEST_DOCKER_COMPOSE_SKIPPER_VERSION (test.docker.compose.skipper.version) sets the Skipper version used for testing. \n" +
				" - TEST_DOCKER_COMPOSE_STREAM_APPS_URI (test.docker.compose.stream.apps.uri) version of the Streaming apps used for testing. \n" +
				" - TEST_DOCKER_COMPOSE_TASK_APPS_URI (test.docker.compose.taks.apps.uri)  sets the Tasks apps version used for testing. \n" +
				" - TEST_DOCKER_COMPOSE_PULLONSTARTUP (test.docker.compose.pullOnStartup) enable/disable pulling latest docker images from the Docker Hub. \n");

		logger.info("{} = {}", DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_DATAFLOW_VERSIONN,
				DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_DATAFLOW_VERSIONN, DEFAULT_DATAFLOW_VERSION));
		logger.info("{} = {}", DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_SKIPPER_VERSIONN,
				DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_SKIPPER_VERSIONN, DEFAULT_SKIPPER_VERSION));
		logger.info("{} = {}", DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_PULLONSTARTUP,
				DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_PULLONSTARTUP, "true"));
		logger.info("{} = {}", DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_STREAM_APPS_URI,
				DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_STREAM_APPS_URI, (isDood ? KAFKA_DOCKER_STREAM_APPS_URI : DEFAULT_STREAM_APPS_URI)));
		logger.info("{} = {}", DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_TASK_APPS_URI,
				DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_TASK_APPS_URI, (isDood ? "https://dataflow.spring.io/task-docker-latest" : DEFAULT_TASK_APPS_URI)));
		logger.info("{} = {}", DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_PATHS,
				DockerComposeFactoryProperties.getDockerComposePaths(DEFAULT_DOCKER_COMPOSE_PATHS));

		String[] dockerComposePaths = new ResourceExtractor(tempFolder).extract(
				DockerComposeFactoryProperties.getDockerComposePaths(DEFAULT_DOCKER_COMPOSE_PATHS));

		// If DooD is enabled but the docker-compose-dood.yml is not listed in the dockerComposePaths then
		// add it explicitly at the end of the list.
		if (isDood && (!Arrays.asList(dockerComposePaths).contains("docker-compose-dood.yml"))) {
			String[] dockerComposePathsEx = new String[dockerComposePaths.length + 1];
			System.arraycopy(dockerComposePaths, 0, dockerComposePathsEx, 0, dockerComposePaths.length);
			dockerComposePathsEx[dockerComposePaths.length] = "docker-compose-dood.yml";
			dockerComposePaths = dockerComposePathsEx;
		}
		logger.info("Extracted docker compose files = {}", Arrays.toString(dockerComposePaths));

		return DockerComposeExtension.builder()
				.projectName(ProjectName.fromString("scdf"))
				.files(DockerComposeFiles.from(dockerComposePaths))
				.machine(dockerMachine)
				.saveLogsTo("target/dockerLogs/DockerComposeIT")
				.waitingForService("dataflow-server", HealthChecks.toRespond2xxOverHttp(9393,
						(port) -> port.inFormat("http://$HOST:$EXTERNAL_PORT")), org.joda.time.Duration.standardMinutes(10))
				.waitingForService("skipper-server", HealthChecks.toRespond2xxOverHttp(7577,
						(port) -> port.inFormat("http://$HOST:$EXTERNAL_PORT")), org.joda.time.Duration.standardMinutes(10))
				// set to false to test with local dataflow and skipper images.
				.pullOnStartup(DockerComposeFactoryProperties.getBoolean(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_PULLONSTARTUP, true))
				.build();
	}

	public static Path createTempDirectory() {
		try {
			Path tempDirPath = Files.createTempDirectory(null);
			logger.info("Temp directory: " + tempDirPath);
			return tempDirPath;
		}
		catch (IOException e) {
			throw new IllegalStateException("Could not create the temp directory");
		}
	}
}
