/*
 * Copyright 2020-2021 the original author or authors.
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

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.DockerComposeFiles;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.ProjectName;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerMachine;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.HealthChecks;
import org.springframework.cloud.dataflow.common.test.docker.junit5.LegacyDockerComposeExtension;

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
	 * Pre-registered Task apps used for testing.
	 */
	public static final String DEFAULT_TASK_APPS_URI = "https://dataflow.spring.io/task-maven-3-0-x&force=true";

	/**
	 * Common Apps URIs
	 */
	public static final String KAFKA_MAVEN_STREAM_APPS_URI = "https://dataflow.spring.io/kafka-maven-5-0-x&force=true"; // local/kafka
	public static final String RABBITMQ_MAVEN_STREAM_APPS_URI = "https://dataflow.spring.io/rabbitmq-maven-5-0-x&force=true"; // cf or local/rabbit
	public static final String KAFKA_DOCKER_STREAM_APPS_URI = "https://dataflow.spring.io/kafka-docker-5-0-x&force=true"; // k8s

	/**
	 * Pre-registered Stream apps used in the tests
	 */
	private static final String DEFAULT_STREAM_APPS_URI = KAFKA_MAVEN_STREAM_APPS_URI;

	/**
	 * List of docker compose files to mix to bootstrap as a test environment. Most files are found under the
	 * 'spring-cloud-dataflow/src/docker-compose' folder.
	 */
	private static final String[] DEFAULT_DOCKER_COMPOSE_PATHS = {
			"../src/docker-compose/docker-compose.yml", // Configures DataFlow, Skipper, Kafka/Zookeeper and MariaDB
			"../src/docker-compose/docker-compose-prometheus.yml" //,   // metrics collection/visualization with Prometheus and Grafana.
			//"../src/docker-compose/docker-compose-influxdb.yml",     // metrics collection/visualization with InfluxDB and Grafana.
			//"../src/docker-compose/docker-compose-wavefront.yml",     // metrics collection/visualization with Wavefront.
			//"../src/docker-compose/docker-compose-postgres.yml",     // Replaces local MariaDB database by Postgres.
			//"../src/docker-compose/docker-compose-rabbitmq.yml",     // Replaces local Kafka message broker by RabbitMQ.
			//"../src/docker-compose/docker-compose-k8s.yml",          // Adds K8s target platform (called k8s).
			//"../src/docker-compose/docker-compose-cf.yml"            // Adds CloudFoundry target platform (called cf).
	};

	private static boolean isDood = DockerComposeFactoryProperties.getBoolean(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_DOOD, false);

	/**
	 * Initialize the docker machine with the required environment variables.
	 */
	private static DockerMachine dockerMachine = DockerMachine.localMachine()
			.withAdditionalEnvironmentVariable("COMPOSE_HTTP_TIMEOUT", "300")
			.withAdditionalEnvironmentVariable("PLATFORM_TYPE", "local")
			.withAdditionalEnvironmentVariable("DATAFLOW_URI",
					DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_DATAFLOW_URI, "http://dataflow-server:9393"))
			.withAdditionalEnvironmentVariable("BP_JVM_VERSION",
					DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_BP_JVM_VERSION, ""))
			.withAdditionalEnvironmentVariable("DATAFLOW_VERSION",
					DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_DATAFLOW_VERSIONN, ""))
			.withAdditionalEnvironmentVariable("SKIPPER_URI",
					DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_SKIPPER_URI, "http://skipper-server:7577"))
			.withAdditionalEnvironmentVariable("SKIPPER_VERSION",
					DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_SKIPPER_VERSIONN, ""))
			.withAdditionalEnvironmentVariable("STREAM_APPS_URI",
					DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_STREAM_APPS_URI, (isDood ? KAFKA_DOCKER_STREAM_APPS_URI : DEFAULT_STREAM_APPS_URI)))
			.withAdditionalEnvironmentVariable("TASK_APPS_URI",
					DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_TASK_APPS_URI, (isDood ? "https://dataflow.spring.io/task-docker-3-0-x" : DEFAULT_TASK_APPS_URI)))
			.withAdditionalEnvironmentVariable("APPS_PORT_RANGE",
					DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_APPS_PORT_RANGE, "20000-20195:20000-20195"))
			.withAdditionalEnvironmentVariable("DOCKER_DELETE_CONTAINER_ON_EXIT",
					"" + DockerComposeFactoryProperties.getBoolean(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_DOCKER_DELETE_CONTAINER_ON_EXIT, true))
			.withAdditionalEnvironmentVariable("METADATA_DEFAULT_DOCKERHUB_USER", DockerComposeFactoryProperties.get("METADATA_DEFAULT_DOCKERHUB_USER", ""))
			.withAdditionalEnvironmentVariable("METADATA_DEFAULT_DOCKERHUB_PASSWORD", DockerComposeFactoryProperties.get("METADATA_DEFAULT_DOCKERHUB_PASSWORD", ""))
			.withAdditionalEnvironmentVariable("COMPOSE_PROJECT_NAME", "scdf")
			.withAdditionalEnvironmentVariable("CR_AZURE_USER", DockerComposeFactoryProperties.get("CR_AZURE_USER", ""))
			.withAdditionalEnvironmentVariable("CR_AZURE_PASS", DockerComposeFactoryProperties.get("CR_AZURE_PASS", ""))
			.withAdditionalEnvironmentVariable("CR_GITHUB_USER", DockerComposeFactoryProperties.get("CR_GITHUB_USER", ""))
			.withAdditionalEnvironmentVariable("CR_GITHUB_PASS", DockerComposeFactoryProperties.get("CR_GITHUB_PASS", ""))
			.withAdditionalEnvironmentVariable("CR_HARBOR_USER", DockerComposeFactoryProperties.get("CR_HARBOR_USER", ""))
			.withAdditionalEnvironmentVariable("CR_HARBOR_PASS", DockerComposeFactoryProperties.get("CR_HARBOR_PASS", ""))
			.build();

	private static String[] addDockerComposeToPath(String[] dockerComposePaths, String additionalDockerCompose) {
		if (java.util.stream.Stream.of(dockerComposePaths).anyMatch(p -> p.contains(additionalDockerCompose))) {
			return dockerComposePaths;
		}

		String[] dockerComposePathsEx = new String[dockerComposePaths.length + 1];
		System.arraycopy(dockerComposePaths, 0, dockerComposePathsEx, 0, dockerComposePaths.length);
		dockerComposePathsEx[dockerComposePaths.length] = additionalDockerCompose;
		return dockerComposePathsEx;
	}

	public static Extension startDockerCompose(Path tempFolder) {

		if (DockerComposeFactoryProperties.isDockerComposeDisabled()) {
			return (BeforeAllCallback) context -> logger.debug("Docker Compose installation is disabled!");
		}

		logger.info("""
				Docker Compose based Integration Tests.\s
				Following environment variables or properties can be used to configure the testing fixture:\s
				 - TEST_DOCKER_COMPOSE_PATHS (test.docker.compose.paths) configures the list of docker-compose files.\s
				 - TEST_DOCKER_COMPOSE_DATAFLOW_VERSION (test.docker.compose.dataflow.version) sets the Data Flow version used for testing.\s
				 - TEST_DOCKER_COMPOSE_SKIPPER_VERSION (test.docker.compose.skipper.version) sets the Skipper version used for testing.\s
				 - TEST_DOCKER_COMPOSE_STREAM_APPS_URI (test.docker.compose.stream.apps.uri) version of the Streaming apps used for testing.\s
				 - TEST_DOCKER_COMPOSE_TASK_APPS_URI (test.docker.compose.taks.apps.uri)  sets the Tasks apps version used for testing.\s
				 - TEST_DOCKER_COMPOSE_PULLONSTARTUP (test.docker.compose.pullOnStartup) enable/disable pulling latest docker images from the Docker Hub.\s
				""");

		logger.info("{} = {}", DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_DATAFLOW_VERSIONN,
				DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_DATAFLOW_VERSIONN, "default version set in ../src/docker-compose/docker-compose.yml"));
		logger.info("{} = {}", DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_SKIPPER_VERSIONN,
				DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_SKIPPER_VERSIONN, "default version set in ../src/docker-compose/docker-compose.yml"));
		logger.info("{} = {}", DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_PULLONSTARTUP,
				DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_PULLONSTARTUP, "true"));
		logger.info("{} = {}", DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_STREAM_APPS_URI,
				DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_STREAM_APPS_URI, (isDood ? KAFKA_DOCKER_STREAM_APPS_URI : DEFAULT_STREAM_APPS_URI)));
		logger.info("{} = {}", DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_TASK_APPS_URI,
				DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_TASK_APPS_URI, (isDood ? "https://dataflow.spring.io/task-docker-3-0-x" : DEFAULT_TASK_APPS_URI)));
		logger.info("{} = {}", DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_PATHS,
				DockerComposeFactoryProperties.getDockerComposePaths(DEFAULT_DOCKER_COMPOSE_PATHS));

		String[] dockerComposePaths = new ResourceExtractor(tempFolder).extract(
				DockerComposeFactoryProperties.getDockerComposePaths(DEFAULT_DOCKER_COMPOSE_PATHS));

		// If DooD is enabled but the docker-compose-dood.yml is not listed in the dockerComposePaths then
		// add it explicitly at the end of the list.
		if (isDood) {
			dockerComposePaths = addDockerComposeToPath(dockerComposePaths, "../src/docker-compose/docker-compose-dood.yml");
			dockerComposePaths = addDockerComposeToPath(dockerComposePaths, "./src/test/resources/docker-compose-docker-it-task-import.yml");
		}
		else {
			dockerComposePaths = addDockerComposeToPath(dockerComposePaths, "./src/test/resources/docker-compose-maven-it-task-import.yml");
		}

		logger.info("Extracted docker compose files = {}", Arrays.toString(dockerComposePaths));

		// For the purpose of waitingForService when using self-signed certificate (inside DockerPort#isHttpRespondingSuccessfully)
		// we have to disable the ssl-validation!
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null,
					new TrustManager[] {
							new X509TrustManager() {
								public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
								public void checkClientTrusted(X509Certificate[] certs, String authType) { }
								public void checkServerTrusted(X509Certificate[] certs, String authType) { }
							}
					},
					new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		}
		catch (Exception e) {
			logger.warn("Failed to configure Skip SSL Verification!");
		}

		String waitingForServiceFormat =
				DockerComposeFactoryProperties.get(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_WAITING_FOR_SERVICE_FORMAT,
				"http://$HOST:$EXTERNAL_PORT");

		return LegacyDockerComposeExtension.builder()
				.projectName(ProjectName.fromString("scdf"))
				.files(DockerComposeFiles.from(dockerComposePaths))
				.machine(dockerMachine)
				.saveLogsTo("target/dockerLogs/DockerComposeIT")
				.waitingForService("dataflow-server", HealthChecks.toRespond2xxOverHttp(9393,
						(port) -> port.inFormat(waitingForServiceFormat)), org.joda.time.Duration.standardMinutes(10))
				.waitingForService("skipper-server", HealthChecks.toRespond2xxOverHttp(7577,
						(port) -> port.inFormat(waitingForServiceFormat)), org.joda.time.Duration.standardMinutes(10))
				// set to false to test with local dataflow and skipper images.
				.pullOnStartup(DockerComposeFactoryProperties.getBoolean(DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_PULLONSTARTUP, true))
				.build();

	}
}
