/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.integration.test;

import java.nio.charset.Charset;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.integration.test.util.ClosableDockerComposeRule;
import org.springframework.cloud.dataflow.integration.test.util.RuntimeApplicationHelper;
import org.springframework.cloud.dataflow.integration.test.util.Wait;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * DataFlow smoke tests that uses docker-compose to create fully fledged, local test environment.
 *
 * Test fixture applies the same docker compose files used for the Data Flow local installation:
 *  - https://dataflow.spring.io/docs/installation/local/docker/
 *  - https://dataflow.spring.io/docs/installation/local/docker-customize/
 *
 * The Palantir DockerMachine and DockerComposeRule are used to programmatically deploy the docker-compose files.
 *
 * The DOCKER_COMPOSE_PATHS allow to configure the list of docker-compose files used for the test.
 * The DATAFLOW_VERSION, SKIPPER_VERSION, STREAM_APPS_URI and TASK_APPS_URI variables (configured via the DockerMachine)
 * allow to specify the dataflow/skipper versions to be used in the tests as well as the version of the Apps and Tasks
 * used.
 *
 * The ClosableDockerComposeRule will ensure that all docker containers are removed on tests completion of failure.
 *
 * Logs for all docker containers (expect deployed apps) are saved under target/dockerLogs/dockerComposeRuleTest
 *
 * The Data Flow REST API (https://docs.spring.io/spring-cloud-dataflow/docs/current/reference/htmlsingle/#api-guide),
 * Java REST Clients (such as DataFlowTemplate, RuntimeOperations, TaskOperations) and the
 * Java DSL (https://dataflow.spring.io/docs/feature-guides/streams/java-dsl/) are used by the tests to interact with
 * the Data Flow environment.
 *
 * The {@link Wait} is DSL utility that allows to timeout block the test execution until certain stream or application
 * state is reached or certain log content appears.
 *
 * The {@link RuntimeApplicationHelper} help to retrieve the application attributes and log files across the Local,
 * CF and K8s platforms.
 *
 * NOTE: if you manually interrupt the test execution before it has completed of failed, it is likely that some docker
 * containers will be left hanging. Use 'docker rm $(docker ps -a -q) -f' to remove all docker containers. To clean all
 * Spring app on K8s platform use 'kubectl delete all,cm -l role=spring-app'
 *
 * --------------------------------------------------------------------------------------------------------------------
 * For testing streams on remote platforms (k8s and CF). If you configure K8s or CF runtime platforms as explained below,
 * you can have the test feature that uses the local run SCDF/Skipper/MySQL to deploy and run Stream only test to the
 * remote K8s or CF environments. Note that Tasks can only be run locally!
 *
 * Follow the https://dataflow.spring.io/docs/2.3.0.SNAPSHOT/installation/local/docker-customize/#docker-compose-extensions
 * multi-platform instructions to prepare docker-compose-k8s.yml and docker-compose-cf.yml files.
 *
 * Stream tests on Kubernetes (k8s) platform:
 * - Add the docker-compose-k8s.yml to the DOCKER_COMPOSE_PATHS list.
 * - Start Kafka message broker on the k8s cluster. Follow the kubectl DataFlow instructions:
 *   https://dataflow.spring.io/docs/installation/kubernetes/kubectl/#choose-a-message-broker
 * - Set the TEST_PLATFORM_NAME to 'k8s'.
 * - In the DockerMachine configuration set the STREAM_APPS_URI variable to link loading Kafka/Docker apps (e.g
 *   https://dataflow.spring.io/rabbitmq-maven-latest).
 *
 * Stream tests on CloudFoundry (CF) platform:
 * - Add the docker-compose-cf.yml to the DOCKER_COMPOSE_PATHS list.
 * - On the CF platform start a RabbitMQ service called 'rabbit'.
 * - Set the TEST_PLATFORM_NAME to 'cf'.
 * - In the DockerMachine configuration set the STREAM_APPS_URI variable to link loading Rabbit/Maven apps. (e.g.
 *   https://dataflow.spring.io/rabbitmq-maven-latest)
 *
 * @author Christian Tzolov
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		DockerComposeTestPlatform.class,
		DockerComposeTestStream.class,
		DockerComposeTestTask.class,
		DockerComposeTestTask2.class
})
public class DockerComposeIT {

	private static final Logger logger = LoggerFactory.getLogger(DockerComposeIT.class);

	/**
	 * Data Flow version to use for the tests. The findCurrentDataFlowVersion will try to retrieve the latest BS version
	 * or will fall back to the default version provided as parameter.
	 */
	public static final String DATAFLOW_VERSION = findCurrentDataFlowVersion("2.4.0.BUILD-SNAPSHOT");

	/**
	 * Skipper version used for the tests.
	 */
	public static final String SKIPPER_VERSION = "2.3.0.BUILD-SNAPSHOT";

	/**
	 * Forcefully pull docker images instead of using the locally cached versions.
	 * Set to false to test with local DataFlow and Skipper images.
	 */
	public static final boolean PULL_DOCKER_IMAGES_ON_STARTUP = pullDockerImagesOnStartup(true);

	/**
	 * Pre-registered Task apps used for testing.
	 */
	public static final String TASK_APPS_URI = "https://dataflow.spring.io/task-maven-latest&force=true";

	/**
	 * Common Apps URIs
	 */
	public static final String KAFKA_MAVEN_STREAM_APPS_URI = "https://dataflow.spring.io/kafka-maven-latest&force=true"; // local/kafka
	public static final String RABBITMQ_MAVEN_STREAM_APPS_URI = "https://dataflow.spring.io/rabbitmq-maven-latest&force=true"; // cf or local/rabbit
	public static final String KAFKA_DOCKER_STREAM_APPS_URI = "https://dataflow.spring.io/kafka-docker-latest&force=true"; // k8s

	/**
	 * Pre-registered Stream apps used in the tests
	 */
	private static final String STREAM_APPS_URI = KAFKA_MAVEN_STREAM_APPS_URI;

	/**
	 * List of docker compose files to mix to bootstrap as a test environment. Most files are found under the
	 * 'spring-cloud-dataflow/spring-cloud-dataflow-server' folder.
	 */
	private static final String[] DOCKER_COMPOSE_PATHS = {
			"docker-compose.yml",              // Configures DataFlow, Skipper, Kafka/Zookeeper and MySQL
			"docker-compose-prometheus.yml",   // metrics collection/visualization with Prometheus and Grafana.
			//"docker-compose-influxdb.yml",     // metrics collection/visualization with InfluxDB and Grafana.
			//"docker-compose-postgres.yml",     // Replaces local MySQL database by Postgres.
			//"docker-compose-rabbitmq.yml",     // Replaces local Kafka message broker by RabbitMQ.
			//"docker-compose-k8s.yml",          // Adds K8s target platform (called k8s).
			//"docker-compose-cf.yml"            // Adds CloudFoundry target platform (called cf).
	};

	/**
	 * Initialize the docker machine with the required environment variables.
	 */
	private static DockerMachine dockerMachine = DockerMachine.localMachine()
			.withAdditionalEnvironmentVariable("DATAFLOW_VERSION", DATAFLOW_VERSION)
			.withAdditionalEnvironmentVariable("SKIPPER_VERSION", SKIPPER_VERSION)
			.withAdditionalEnvironmentVariable("STREAM_APPS_URI", STREAM_APPS_URI)
			.withAdditionalEnvironmentVariable("TASK_APPS_URI", TASK_APPS_URI)
			.build();
	/**
	 * DockerComposeRule doesnt't release the created containers if the before() fails.
	 * The dockerRuleWrapper ensures that all containers are shutdown in case of failure.
	 */
	@ClassRule
	public static ExternalResource dockerRuleWrapper = ClosableDockerComposeRule.of(
			DockerComposeRule.builder()
					.files(DockerComposeFiles.from(DOCKER_COMPOSE_PATHS))
					.machine(dockerMachine)
					.saveLogsTo("target/dockerLogs/DockerComposeIT")
					.waitingForService("dataflow-server", HealthChecks.toRespond2xxOverHttp(9393,
							(port) -> port.inFormat("http://$HOST:$EXTERNAL_PORT")))
					.waitingForService("skipper-server", HealthChecks.toRespond2xxOverHttp(7577,
							(port) -> port.inFormat("http://$HOST:$EXTERNAL_PORT")))
					.pullOnStartup(PULL_DOCKER_IMAGES_ON_STARTUP) // set to false to test with local dataflow and skipper images.
					.build());

	@BeforeClass
	public static void beforeClass() {
		logger.info("DB: MySQL, Binder: Kafka, TSDB: Prometheus/RSocketProxy");
	}

	/**
	 * Attempt to retrieve the current DataFlow version from the generated application.yml
	 * @param defaultVersion Default version to use if it fail to retrieve the current version.
	 * @return If available returns the DataFlow version from the application.yml or default version otherwise.
	 */
	private static String findCurrentDataFlowVersion(String defaultVersion) {
		try {
			String content = StreamUtils.copyToString(new ClassPathResource("/application.yml").getInputStream(),
					Charset.forName("UTF-8"));
			Map<String, Map<String, Map<String, String>>> map = new ObjectMapper(new YAMLFactory())
					.readValue(content, Map.class);
			String version = map.get("info").get("app").get("version");
			if (!StringUtils.isEmpty(version) && !version.contains("@")) {
				logger.info("Retrieved current DATAFLOW_VERSION as: " + version);
				return version;
			}
			else {
				logger.warn("Failed to retrieve the DATAFLOW_VERSION and defaults to " + defaultVersion);
			}
		}
		catch (Exception e) {
			logger.warn("Failed to retrieve the DATAFLOW_VERSION and defaults to " + defaultVersion, e);
		}

		return defaultVersion;
	}

	private static boolean pullDockerImagesOnStartup(boolean defaultValue) {
		if (System.getenv("DATAFLOW_TEST_PULL_ON_STARTUP") != null) {
			return Boolean.parseBoolean(System.getenv("DATAFLOW_TEST_PULL_ON_STARTUP"));
		}
		else if (System.getProperty("dataflow.test.pull.on.startup") != null) {
			return Boolean.parseBoolean(System.getProperty("dataflow.test.pull.on.startup"));
		}
		return defaultValue;
	}
}
