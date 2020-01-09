/*
 * Copyright 2019-2020 the original author or authors.
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

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.text.ParagraphView;

import com.jayway.jsonpath.JsonPath;
import com.palantir.docker.compose.DockerComposeExtension;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import net.javacrumbs.jsonunit.JsonAssert;
import org.assertj.core.api.Condition;
import org.assertj.core.internal.Conditions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.integration.test.util.RuntimeApplicationHelper;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.client.dsl.DeploymentPropertiesBuilder;
import org.springframework.cloud.dataflow.rest.client.dsl.Stream;
import org.springframework.cloud.dataflow.rest.client.dsl.StreamApplication;
import org.springframework.cloud.dataflow.rest.client.dsl.StreamDefinition;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;

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
 * The {@link Awaitility} is DSL utility that allows to timeout block the test execution until certain stream or application
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
public class DockerComposeIT {

	private static final Logger logger = LoggerFactory.getLogger(DockerComposeIT.class);

	/**
	 * Data Flow version to use for the tests.
	 */
	public static final String DATAFLOW_VERSION = "2.4.0.BUILD-SNAPSHOT";

	/**
	 * Skipper version used for the tests.
	 */
	public static final String SKIPPER_VERSION = "2.3.0.BUILD-SNAPSHOT";

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
			"docker-compose.yml", // Configures DataFlow, Skipper, Kafka/Zookeeper and MySQL
			"docker-compose-prometheus.yml" //,   // metrics collection/visualization with Prometheus and Grafana.
			//"docker-compose-influxdb.yml",     // metrics collection/visualization with InfluxDB and Grafana.
			//"docker-compose-postgres.yml",     // Replaces local MySQL database by Postgres.
			//"docker-compose-rabbitmq.yml",     // Replaces local Kafka message broker by RabbitMQ.
			//"docker-compose-k8s.yml",          // Adds K8s target platform (called k8s).
			//"docker-compose-cf.yml"            // Adds CloudFoundry target platform (called cf).
	};

	private static DataFlowTemplate dataFlowOperations;
	private static RuntimeApplicationHelper runtimeApps;
	private static RestTemplate restTemplate;
	private static TaskOperations taskOperations;

	/**
	 * default - local platform (e.g. docker-compose)
	 * cf - Cloud Foundry platform, configured in docker-compose-cf.yml
	 * k8s - GKE/Kubernetes platform, configured via docker-compose-k8s.yml.
	 */
	private static final String TEST_PLATFORM_NAME = "default";

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
	@RegisterExtension
	public static DockerComposeExtension dockerCompose = DockerComposeExtension.builder()
			.files(DockerComposeFiles.from(DOCKER_COMPOSE_PATHS))
			.machine(dockerMachine)
			.saveLogsTo("target/dockerLogs/DockerComposeIT")
			.waitingForService("dataflow-server", HealthChecks.toRespond2xxOverHttp(9393,
					(port) -> port.inFormat("http://$HOST:$EXTERNAL_PORT")), org.joda.time.Duration.standardMinutes(10))
			.waitingForService("skipper-server", HealthChecks.toRespond2xxOverHttp(7577,
					(port) -> port.inFormat("http://$HOST:$EXTERNAL_PORT")), org.joda.time.Duration.standardMinutes(10))
			.pullOnStartup(true) // set to false to test with local dataflow and skipper images.
			.build();

	@BeforeAll
	public static void beforeClass() {
		dataFlowOperations = new DataFlowTemplate(URI.create("http://localhost:9393"));
		logger.info("Configured platforms: " + dataFlowOperations.streamOperations().listPlatforms().stream()
				.map(d -> String.format("[%s:%s]", d.getName(), d.getType())).collect(Collectors.joining()));
		runtimeApps = new RuntimeApplicationHelper(dataFlowOperations, TEST_PLATFORM_NAME);
		restTemplate = new RestTemplate(); // used for HTTP post in tests
		taskOperations = dataFlowOperations.taskOperations();
		Awaitility.setDefaultPollInterval(Duration.ofSeconds(5));
		Awaitility.setDefaultTimeout(Duration.ofMinutes(10));
	}

	@BeforeEach
	public void before() {
		Awaitility.await().until(() -> dataFlowOperations.appRegistryOperations().list().getMetadata().getTotalElements() >= 68L);
	}

	@AfterEach
	public void after() {
		dataFlowOperations.streamOperations().destroyAll();
		dataFlowOperations.taskOperations().destroyAll();
	}

	// -----------------------------------------------------------------------
	//                          PLATFORM  TESTS
	// -----------------------------------------------------------------------
	@Test
	public void featureInfo() {
		logger.info("feature-info-test");
		AboutResource about = dataFlowOperations.aboutOperation().get();
		if (isPrometheusPresent() || isInfluxPresent()) {
			Assertions.assertTrue(about.getFeatureInfo().isGrafanaEnabled());
		}
		Assertions.assertTrue(about.getFeatureInfo().isAnalyticsEnabled());
		Assertions.assertTrue(about.getFeatureInfo().isStreamsEnabled());
		Assertions.assertTrue(about.getFeatureInfo().isTasksEnabled());
		Assertions.assertFalse(about.getFeatureInfo().isSchedulesEnabled());
	}

	@Test
	public void appsCount() {
		logger.info("apps-count-test");
		assertThat(dataFlowOperations.appRegistryOperations().list().getMetadata().getTotalElements())
				.isGreaterThanOrEqualTo(68L);
	}

	// -----------------------------------------------------------------------
	//                            STREAM  TESTS
	// -----------------------------------------------------------------------
	/**
	 * Target Data FLow platform to use for the testing: https://dataflow.spring.io/docs/concepts/architecture/#platforms
	 *
	 * By default the Local (e.g. platformName=default) Data Flow environment is used for testing. If you have
	 * provisioned docker-compose file to add remote access ot CF or K8s environments you can use the target
	 * platform/account name instead.
	 */
	private static final String SPRING_CLOUD_DATAFLOW_SKIPPER_PLATFORM_NAME = "spring.cloud.dataflow.skipper.platformName";

	// Stream lifecycle states
	private static final String DEPLOYED = "deployed";
	private static final String DELETED = "deleted";
	private static final String UNDEPLOYED = "undeployed";
	private static final String DEPLOYING = "deploying";
	private static final String PARTIAL = "partial";

	@Test
	public void streamTransform() {
		logger.info("stream-transform-test");
		try (Stream stream = Stream.builder(dataFlowOperations)
				.name("transform-test")
				.definition("http | transform --expression=payload.toUpperCase() | log")
				.create()
				.deploy(testDeploymentProperties())) {

			assertThat(stream.getStatus()).is(
					new Condition<>(status -> status.equals(DEPLOYING) || status.equals(PARTIAL), ""));

			Awaitility.await().atMost(Duration.ofMinutes(10)).until(() -> stream.getStatus().equals(DEPLOYED));

			Map<String, String> httpApp = runtimeApps.getApplicationInstances(stream.getName(), "http")
					.values().iterator().next();

			String message = "Unique Test message: " + new Random().nextInt();

			restTemplate.postForObject(runtimeApps.getApplicationInstanceUrl(httpApp), message, String.class);

			Awaitility.await().atMost(Duration.ofMinutes(10)).until(
					() -> runtimeApps.getFirstInstanceLog(stream.getName(), "log").contains(message.toUpperCase()));
		}
	}

	@Test
	public void streamPartitioning() {
		logger.info("stream-partitioning-test");
		StreamDefinition streamDefinition = Stream.builder(dataFlowOperations)
				.name("partitioning-test")
				.definition("http | splitter --expression=payload.split(' ') | log")
				.create();

		try (Stream stream = streamDefinition.deploy(new DeploymentPropertiesBuilder()
				.putAll(testDeploymentProperties())
				.put(SPRING_CLOUD_DATAFLOW_SKIPPER_PLATFORM_NAME, runtimeApps.getPlatformName())
				// Create 2 log instances with partition key computed from the payload.
				.put("deployer.log.count", "2")
				.put("app.splitter.producer.partitionKeyExpression", "payload")
				.put("app.log.spring.cloud.stream.kafka.bindings.input.consumer.autoRebalanceEnabled", "false")
				.put("app.log.logging.pattern.level", "WOODCHUCK-${INSTANCE_INDEX:${CF_INSTANCE_INDEX:${spring.cloud.stream.instanceIndex:0}}} %5p")
				.build())) {

			//assertThat(stream.getStatus()).is(oneOf(DEPLOYING, PARTIAL)));

			Awaitility.await().atMost(Duration.ofMinutes(10)).until(() -> stream.getStatus().equals(DEPLOYED));

			Map<String, String> httpApp = runtimeApps.getApplicationInstances(stream.getName(), "http")
					.values().iterator().next();

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpApp);

			String message = "How much wood would a woodchuck chuck if a woodchuck could chuck wood";
			restTemplate.postForObject(httpAppUrl, message, String.class);

			Awaitility.await().atMost(Duration.ofMinutes(10)).until(() -> {
				Collection<String> logs = runtimeApps.applicationInstanceLogs(stream.getName(), "log").values();

				if (logs.size() != 2) return false;

				return logs.stream()
						// partition order is undetermined
						.map(log -> (log.contains("WOODCHUCK-0")) ?
								Arrays.asList("WOODCHUCK-0", "How", "chuck").stream().allMatch(log::contains) :
								Arrays.asList("WOODCHUCK-1", "much", "wood", "would", "if", "a", "woodchuck", "could").stream().allMatch(log::contains))
						.reduce(Boolean::logicalAnd)
						.orElse(false);
			});
		}
	}

	@Test
	public void streamLifecycle() {
		logger.info("stream-lifecycle-test: DEPLOY");
		try (Stream stream = Stream.builder(dataFlowOperations)
				.name("lifecycle-test")
				.definition("time | log --log.expression='TICKTOCK - TIMESTAMP: '.concat(payload)")
				.create()
				.deploy(testDeploymentProperties())) {

			//assertThat(stream.getStatus()).is(oneOf(DEPLOYING, PARTIAL));
			Awaitility.await().atMost(Duration.ofMinutes(5)).until(() -> stream.getStatus().equals(DEPLOYED));

			Awaitility.await().until(() -> runtimeApps.getFirstInstanceLog(stream.getName(), "log")
					.contains("TICKTOCK - TIMESTAMP:"));

			assertThat(stream.history().size()).isEqualTo(1L);
			assertThat(stream.history().get(1)).isEqualTo(DEPLOYED);

			// UPDATE
			logger.info("stream-lifecycle-test: UPDATE");
			stream.update(new DeploymentPropertiesBuilder()
					.put("app.log.log.expression", "'Updated TICKTOCK - TIMESTAMP: '.concat(payload)")
					// TODO investigate why on update the app-starters-core overrides the original web.exposure.include!!!
					.put("app.*.management.endpoints.web.exposure.include", "*")
					.build());

			Awaitility.await().until(() -> stream.getStatus().equals(DEPLOYED));

			Awaitility.await().until(() -> runtimeApps.getFirstInstanceLog(stream.getName(), "log")
					.contains("Updated TICKTOCK - TIMESTAMP:"));

			assertThat(stream.history().size()).isEqualTo(2);
			assertThat(stream.history().get(1)).isEqualTo(DELETED);
			assertThat(stream.history().get(2)).isEqualTo(DEPLOYED);

			// ROLLBACK
			logger.info("stream-lifecycle-test: ROLLBACK");
			stream.rollback(0);

			Awaitility.await().until(() -> stream.getStatus().equals(DEPLOYED));
			assertThat(stream.getStatus()).isEqualTo(DEPLOYED);

			Awaitility.await().until(() -> runtimeApps.getFirstInstanceLog(stream.getName(), "log")
					.contains("TICKTOCK - TIMESTAMP:"));

			assertThat(stream.history().size()).isEqualTo(3);
			assertThat(stream.history().get(1)).isEqualTo(DELETED);
			assertThat(stream.history().get(2)).isEqualTo(DELETED);
			assertThat(stream.history().get(3)).isEqualTo(DEPLOYED);

			// UNDEPLOY
			logger.info("stream-lifecycle-test: UNDEPLOY");
			stream.undeploy();

			Awaitility.await().until(() -> stream.getStatus().equals(UNDEPLOYED));
			assertThat(stream.getStatus()).isEqualTo(UNDEPLOYED);

			assertThat(stream.history().size()).isEqualTo(3);
			assertThat(stream.history().get(1)).isEqualTo(DELETED);
			assertThat(stream.history().get(2)).isEqualTo(DELETED);
			assertThat(stream.history().get(3)).isEqualTo(DELETED);

			assertThat(dataFlowOperations.streamOperations().list().getMetadata().getTotalElements()).isEqualTo(1L);
			// DESTROY
		}
		logger.info("stream-lifecycle-test: DESTROY");
		assertThat(dataFlowOperations.streamOperations().list().getMetadata().getTotalElements()).isEqualTo(0L);
	}

	@Test
	public void streamScaling() {
		logger.info("stream-scaling-test");
		try (Stream stream = Stream.builder(dataFlowOperations)
				.name("stream-scaling-test")
				.definition("time | log --log.expression='TICKTOCK - TIMESTAMP: '.concat(payload)")
				.create()
				.deploy(testDeploymentProperties())) {

			Awaitility.await().until(() -> stream.getStatus().equals(DEPLOYED));

			final StreamApplication time = new StreamApplication("time");
			final StreamApplication log = new StreamApplication("log");

			Map<StreamApplication, Map<String, String>> streamApps = stream.runtimeApps();
			assertThat(streamApps.size()).isEqualTo(2);
			assertThat(streamApps.get(time).size()).isEqualTo(1);
			assertThat(streamApps.get(log).size()).isEqualTo(1);

			// Scale up log
			stream.scaleApplicationInstances(log, 2, Collections.emptyMap());

			Awaitility.await().until(() -> stream.getStatus().equals(DEPLOYED));
			Awaitility.await().until(() -> stream.runtimeApps().get(log).size() == 2);

			assertThat(stream.getStatus()).isEqualTo(DEPLOYED);
			streamApps = stream.runtimeApps();
			assertThat(streamApps.size()).isEqualTo(2);
			assertThat(streamApps.get(time).size()).isEqualTo(1);
			assertThat(streamApps.get(log).size()).isEqualTo(2);
		}
	}

	@Test
	public void namedChannelDestination() {
		logger.info("stream-named-channel-destination-test");
		try (
				Stream logStream = Stream.builder(dataFlowOperations)
						.name("log-destination-sink")
						.definition(":LOG-DESTINATION > log")
						.create()
						.deploy(testDeploymentProperties());
				Stream httpStream = Stream.builder(dataFlowOperations)
						.name("http-destination-source")
						.definition("http > :LOG-DESTINATION")
						.create()
						.deploy(testDeploymentProperties())) {

			Awaitility.await().until(() -> logStream.getStatus().equals(DEPLOYED));
			Awaitility.await().until(() -> httpStream.getStatus().equals(DEPLOYED));

			String message = "Unique Test message: " + new Random().nextInt();

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpStream.getName(), "http");
			restTemplate.postForObject(httpAppUrl, message, String.class);

			Awaitility.await().until(() -> runtimeApps.getFirstInstanceLog(logStream.getName(), "log")
					.contains(message));
		}
	}

	@Test
	public void namedChannelTap() {
		logger.info("stream-named-channel-tap-test");
		try (
				Stream httpLogStream = Stream.builder(dataFlowOperations)
						.name("taphttp")
						.definition("http | log")
						.create()
						.deploy(testDeploymentProperties());
				Stream tapStream = Stream.builder(dataFlowOperations)
						.name("tapstream")
						.definition(":taphttp.http > log")
						.create()
						.deploy(testDeploymentProperties())) {

			Awaitility.await().until(() -> httpLogStream.getStatus().equals(DEPLOYED));
			Awaitility.await().until(() -> tapStream.getStatus().equals(DEPLOYED));

			String message = "Unique Test message: " + new Random().nextInt();

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpLogStream.getName(), "http");
			restTemplate.postForObject(httpAppUrl, message, String.class);

			Awaitility.await().until(() -> runtimeApps.getFirstInstanceLog(tapStream.getName(), "log")
					.contains(message));
		}
	}

	@Test
	public void namedChannelManyToOne() {
		logger.info("stream-named-channel-many-to-one-test");
		try (
				Stream logStream = Stream.builder(dataFlowOperations)
						.name("many-to-one")
						.definition(":MANY-TO-ONE-DESTINATION > log")
						.create()
						.deploy(testDeploymentProperties());
				Stream httpStreamOne = Stream.builder(dataFlowOperations)
						.name("http-source-1")
						.definition("http > :MANY-TO-ONE-DESTINATION")
						.create()
						.deploy(testDeploymentProperties());
				Stream httpStreamTwo = Stream.builder(dataFlowOperations)
						.name("http-source-2")
						.definition("http > :MANY-TO-ONE-DESTINATION")
						.create()
						.deploy(testDeploymentProperties())) {

			Awaitility.await().until(() -> logStream.getStatus().equals(DEPLOYED));
			Awaitility.await().until(() -> httpStreamOne.getStatus().equals(DEPLOYED));
			Awaitility.await().until(() -> httpStreamTwo.getStatus().equals(DEPLOYED));

			String messageOne = "Unique Test message: " + new Random().nextInt();

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpStreamOne.getName(), "http");
			restTemplate.postForObject(httpAppUrl, messageOne, String.class);

			Awaitility.await().until(() -> runtimeApps.getFirstInstanceLog(logStream.getName(), "log")
					.contains(messageOne));

			String messageTwo = "Unique Test message: " + new Random().nextInt();

			String httpAppUrl2 = runtimeApps.getApplicationInstanceUrl(httpStreamTwo.getName(), "http");
			restTemplate.postForObject(httpAppUrl2, messageTwo, String.class);

			Awaitility.await().until(() -> runtimeApps.getFirstInstanceLog(logStream.getName(), "log")
					.contains(messageTwo));
		}
	}

	@Test
	public void namedChannelDirectedGraph() {
		logger.info("stream-named-channel-directed-graph-test");
		try (
				Stream fooLogStream = Stream.builder(dataFlowOperations)
						.name("directed-graph-destination1")
						.definition(":foo > transform --expression=payload+'-foo' | log")
						.create()
						.deploy(testDeploymentProperties());
				Stream barLogStream = Stream.builder(dataFlowOperations)
						.name("directed-graph-destination2")
						.definition(":bar > transform --expression=payload+'-bar' | log")
						.create()
						.deploy(testDeploymentProperties());
				Stream httpStream = Stream.builder(dataFlowOperations)
						.name("directed-graph-http-source")
						.definition("http | router --expression=payload.contains('a')?'foo':'bar'")
						.create()
						.deploy(testDeploymentProperties())) {

			Awaitility.await().until(() -> fooLogStream.getStatus().equals(DEPLOYED));
			Awaitility.await().until(() -> barLogStream.getStatus().equals(DEPLOYED));
			Awaitility.await().until(() -> httpStream.getStatus().equals(DEPLOYED));

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpStream.getName(), "http");
			restTemplate.postForObject(httpAppUrl, "abcd", String.class);
			restTemplate.postForObject(httpAppUrl, "defg", String.class);

			Awaitility.await().until(() -> runtimeApps.getFirstInstanceLog(fooLogStream.getName(), "log")
					.contains("abcd-foo"));
			Awaitility.await().until(() -> runtimeApps.getFirstInstanceLog(barLogStream.getName(), "log")
					.contains("defg-bar"));
		}
	}

	// -----------------------------------------------------------------------
	//                       STREAM  METRICS TESTS
	// -----------------------------------------------------------------------
	@Test
	public void analyticsCounter() throws InterruptedException, IOException {
		logger.info("stream-analytics-counter-test");
		if (dataFlowOperations.aboutOperation().get().getFeatureInfo().isGrafanaEnabled()) {
			if (!isPrometheusPresent() && !isInfluxPresent()) {
				throw new IllegalStateException("Unknown TSDB!");
			}
			try (Stream stream = Stream.builder(dataFlowOperations)
					.name("httpCounter")
					.definition("http | counter --counter.name=my_http_counter --counter.tag.expression.msgSize=payload.length()")
					.create()
					.deploy(testDeploymentProperties())) {

				Awaitility.await().until(() -> stream.getStatus().equals(DEPLOYED));

				String message1 = "Test message 1";
				String message2 = "Test message 2 with extension";
				String message3 = "Test message 2 with double extension";

				String httpAppUrl = runtimeApps.getApplicationInstanceUrl(stream.getName(), "http");
				restTemplate.postForObject(httpAppUrl, message1, String.class);
				restTemplate.postForObject(httpAppUrl, message2, String.class);
				restTemplate.postForObject(httpAppUrl, message3, String.class);

				// Wait for ~1 min for Micrometer to send first metrics to Prometheus.
				Thread.sleep(Duration.ofMinutes(1).toMillis());

				// Prometheus tests
				if (isPrometheusPresent()) {
					JsonAssert.assertJsonEquals(
							StreamUtils.copyToString(new DefaultResourceLoader().getResource("classpath:/my_http_counter_total.json").getInputStream(), StandardCharsets.UTF_8),
							restTemplate.getForObject("http://localhost:9090/api/v1/query?query=my_http_counter_total", String.class));

					String counterTotalValue = JsonPath.read(restTemplate.getForObject("http://localhost:9090/api/v1/query?query=message_my_http_counter_total", String.class), "$.data.result[0].value[1]");
					assertThat(counterTotalValue).isEqualTo("3");
				}
				else if (isInfluxPresent()) {
					//http://localhost:8086/query?db=myinfluxdb&q=SELECT%20%22count%22%20FROM%20%22spring_integration_send%22
					//http://localhost:8086/query?db=myinfluxdb&q=SHOW%20MEASUREMENTS

					// TODO: The message_my_http_counter measurement in some cases is not incremented!
					//
					// http://localhost:8086/query?db=myinfluxdb&q=SELECT%20value%20FROM%20%22message_my_http_counter%22%20GROUP%20BY%20%2A%20ORDER%20BY%20ASC%20LIMIT%201
					//Wait.on(stream).withTimeout(Duration.ofMinutes(10))
					//		.until(s -> {
					//			Object messageCount = JsonPath.read(
					//					restTemplate.getForObject("http://localhost:8086/query?db=myinfluxdb&q=SELECT value FROM \"message_my_http_counter\" GROUP BY * ORDER BY ASC LIMIT 1", String.class),
					//					"$.results[0].series[0].values[0][1]");
					//			return messageCount != null && ((Integer) messageCount) == 3;
					//		});

					// http://localhost:8086/query?q=SHOW%20DATABASES
					assertThat((String) JsonPath.read(
							restTemplate.getForObject("http://localhost:8086/query?q=SHOW DATABASES", String.class),
							"$.results[0].series[0].values[1][0]")).isEqualTo("myinfluxdb");

					// http://localhost:8086/query?db=myinfluxdb&q=SELECT%20%2A%20FROM%20%22my_http_counter%22
					String myHttpCounter = restTemplate.getForObject("http://localhost:8086/query?db=myinfluxdb&q=SELECT * FROM \"my_http_counter\"", String.class);
					assertThat((String) JsonPath.read(myHttpCounter, "$.results[0].series[0].values[0][6]")).isEqualTo("" + message1.length());
					assertThat((String) JsonPath.read(myHttpCounter, "$.results[0].series[0].values[1][6]")).isEqualTo("" + message2.length());
					assertThat((String) JsonPath.read(myHttpCounter, "$.results[0].series[0].values[2][6]")).isEqualTo("" + message3.length());
				}
			}
		}
		else {
			logger.warn("Data Flow Monitoring is not enabled!");
		}
	}

	/**
	 * For the purpose of testing, disable security, expose the all actuators, and configure logfiles.
	 * @return Deployment properties required for the deployment of all test pipelines.
	 */
	private Map<String, String> testDeploymentProperties() {
		return new DeploymentPropertiesBuilder()
				.put(SPRING_CLOUD_DATAFLOW_SKIPPER_PLATFORM_NAME, runtimeApps.getPlatformName())
				.put("app.*.logging.file", "${PID}-test.log")
				.put("app.*.endpoints.logfile.sensitive", "false")
				.put("app.*.management.endpoints.web.exposure.include", "*")
				.put("app.*.spring.cloud.streamapp.security.enabled", "false")
				.build();
	}

	private boolean isPrometheusPresent() {
		return runtimeApps.isServicePresent("http://localhost:9090/api/v1/query?query=up");
	}

	private boolean isInfluxPresent() {
		return runtimeApps.isServicePresent("http://localhost:8086/ping");
	}

	// -----------------------------------------------------------------------
	//                               TASK TESTS
	// -----------------------------------------------------------------------
	@Test
	public void timestampTask() {
		logger.info("task-timestamp-test");
		String taskDefinitionName = "task-" + UUID.randomUUID().toString().substring(0, 10);
		TaskDefinitionResource tdr = taskOperations.create(taskDefinitionName,
				"timestamp", "Test timestamp task");

		long id1 = taskOperations.launch(taskDefinitionName, Collections.EMPTY_MAP, Collections.EMPTY_LIST, null);
		waitTaskCompletion(taskDefinitionName, 1);
		assertCompletionWithExitCode(taskDefinitionName, 0);

		// Launch existing task
		taskOperations.launch(taskDefinitionName, Collections.EMPTY_MAP, Collections.EMPTY_LIST, null);
		waitTaskCompletion(taskDefinitionName, 2);
		assertCompletionWithExitCode(taskDefinitionName, 0);

		taskOperations.destroy(taskDefinitionName);
	}

	@Test
	public void composedTask() {
		logger.info("task-composed-task-test");
		String taskDefinitionName = "task-" + UUID.randomUUID().toString().substring(0, 10);
		TaskDefinitionResource tdr = taskOperations.create(taskDefinitionName,
				"a: timestamp && b:timestamp", "Test composedTask");

		taskOperations.launch(taskDefinitionName, Collections.EMPTY_MAP, Collections.EMPTY_LIST, null);
		waitTaskCompletion(taskDefinitionName, 1);
		waitTaskCompletion(taskDefinitionName + "-a", 1);
		waitTaskCompletion(taskDefinitionName + "-b", 1);

		assertCompletionWithExitCode(taskDefinitionName, 0);

		assertThat(taskOperations.list().getContent().size()).isEqualTo(3);
		taskOperations.destroy(taskDefinitionName);
		assertThat(taskOperations.list().getContent().size()).isEqualTo(0);
	}

	@Test
	public void multipleComposedTaskWithArguments() {
		logger.info("task-multiple-composed-task-with-arguments-test");
		String taskDefinitionName = "task-" + UUID.randomUUID().toString().substring(0, 10);
		TaskDefinitionResource tdr =
				taskOperations.create(taskDefinitionName, "a: timestamp && b:timestamp", "Test multipleComposedTaskhWithArguments");

		List<String> arguments = Arrays.asList("--increment-instance-enabled=true");
		taskOperations.launch(taskDefinitionName, Collections.EMPTY_MAP, arguments, null);
		waitTaskCompletion(taskDefinitionName, 1);
		waitTaskCompletion(taskDefinitionName + "-a", 1);
		waitTaskCompletion(taskDefinitionName + "-b", 1);

		assertCompletionWithExitCode(taskDefinitionName, 0);

		taskOperations.launch(taskDefinitionName, Collections.EMPTY_MAP, arguments, null);
		waitTaskCompletion(taskDefinitionName, 2);
		waitTaskCompletion(taskDefinitionName + "-a", 2);
		waitTaskCompletion(taskDefinitionName + "-b", 2);

		assertCompletionWithExitCode(taskDefinitionName, 0);

		Collection<JobExecutionResource> jobExecutionResources =
				dataFlowOperations.jobOperations().executionListByJobName(taskDefinitionName).getContent();
		assertThat(jobExecutionResources.size()).isEqualTo(2);

		taskOperations.destroy(taskDefinitionName);
	}

	private void waitTaskCompletion(String taskDefinitionName, int taskExecutionCount) {
		Awaitility.await().until(() -> {
			Collection<TaskExecutionResource> taskExecutions =
					taskOperations.executionListByTaskName(taskDefinitionName).getContent();
			return (taskExecutions.size() >= taskExecutionCount) &&
					taskExecutions.stream()
							.map(execution -> execution != null && execution.getEndTime() != null)
							.reduce(Boolean::logicalAnd)
							.orElse(false);
		});
	}

	private void assertCompletionWithExitCode(String taskDefinitionName, int exitCode) {
		taskOperations.executionListByTaskName(taskDefinitionName).getContent().stream()
				.forEach(taskExecution -> assertThat(taskExecution.getExitCode()).isEqualTo(exitCode));
	}
}
