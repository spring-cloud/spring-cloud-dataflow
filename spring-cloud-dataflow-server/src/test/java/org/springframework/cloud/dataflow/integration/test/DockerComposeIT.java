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

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.JsonPath;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.integration.test.util.ClosableDockerComposeRule;
import org.springframework.cloud.dataflow.integration.test.util.RuntimeApplicationHelper;
import org.springframework.cloud.dataflow.integration.test.util.Wait;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
 * - Add  the docker-compose-k8s.yml to the DOCKER_COMPOSE_PATHS list.
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

	// Stream lifecycle states
	private static final String DEPLOYED = "deployed";
	private static final String DELETED = "deleted";
	private static final String UNDEPLOYED = "undeployed";
	private static final String DEPLOYING = "deploying";
	private static final String PARTIAL = "partial";

	/**
	 * Data Flow version to use for the tests. The findCurrentDataFlowVersion will try to retrieve the latest BS version
	 * or will fall back to the default version provided as parameter.
	 */
	private static final String DATAFLOW_VERSION = findCurrentDataFlowVersion("2.4.0.BUILD-SNAPSHOT");

	/**
	 * Skipper version used for the tests.
	 */
	private static final String SKIPPER_VERSION = "2.2.1.BUILD-SNAPSHOT";

	/**
	 * Pre-registered Task apps used for testing.
	 */
	private static final String TASK_APPS_URI = "https://dataflow.spring.io/task-maven-latest&force=true";

	/**
	 * Common Apps URIs
	 */
	private static final String KAFKA_MAVEN_STREAM_APPS_URI = "https://dataflow.spring.io/kafka-maven-latest&force=true"; // local/kafka
	private static final String RABBITMQ_MAVEN_STREAM_APPS_URI = "https://dataflow.spring.io/rabbitmq-maven-latest&force=true"; // cf or local/rabbit
	private static final String KAFKA_DOCKER_STREAM_APPS_URI = "https://dataflow.spring.io/kafka-docker-latest&force=true"; // k8s

	/**
	 * Pre-registered Stream apps used in the tests
	 */
	private static final String STREAM_APPS_URI = KAFKA_MAVEN_STREAM_APPS_URI;

	/**
	 * default - local platform (e.g. docker-compose)
	 * cf - Cloud Foundry platform, configured in docker-compose-cf.yml
	 * k8s - GKE/Kubernetes platform, configured via docker-compose-k8s.yml.
	 */
	private static final String TEST_PLATFORM_NAME = "default";

	/**
	 * Target Data FLow platform to use for the testing: https://dataflow.spring.io/docs/concepts/architecture/#platforms
	 *
	 * By default the Local (e.g. platformName=default) Data Flow environment is used for testing. If you have
	 * provisioned docker-compose file to add remote access ot CF or K8s environments you can use the target
	 * platform/account name instead.
	 */
	private static final String SPRING_CLOUD_DATAFLOW_SKIPPER_PLATFORM_NAME = "spring.cloud.dataflow.skipper.platformName";

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
					.saveLogsTo("target/dockerLogs/dockerComposeRuleTest")
					.waitingForService("dataflow-server", HealthChecks.toRespond2xxOverHttp(9393,
							(port) -> port.inFormat("http://$HOST:$EXTERNAL_PORT")))
					.waitingForService("skipper-server", HealthChecks.toRespond2xxOverHttp(7577,
							(port) -> port.inFormat("http://$HOST:$EXTERNAL_PORT")))
					.pullOnStartup(true) // set to false to test with local dataflow and skipper images.
					.build());

	private static DataFlowTemplate dataFlowOperations;
	private static RuntimeApplicationHelper runtimeApps;
	private static TaskOperations taskOperations;
	private static RestTemplate restTemplate;

	@BeforeClass
	public static void beforeClass() {
		dataFlowOperations = new DataFlowTemplate(URI.create("http://localhost:9393"));
		logger.info("Configured platforms: " + dataFlowOperations.streamOperations().listPlatforms().stream()
				.map(d -> String.format("[%s:%s]", d.getName(), d.getType())).collect(Collectors.joining()));
		runtimeApps = new RuntimeApplicationHelper(dataFlowOperations, TEST_PLATFORM_NAME);
		taskOperations = dataFlowOperations.taskOperations();
		restTemplate = new RestTemplate(); // used for HTTP post in tests
	}

	@Before
	public void before() {
		Wait.on(dataFlowOperations.appRegistryOperations()).until(appRegistry ->
				appRegistry.list().getMetadata().getTotalElements() >= 68L);
	}

	@After
	public void after() {
		dataFlowOperations.streamOperations().destroyAll();
		dataFlowOperations.taskOperations().destroyAll();
	}

	@Test
	public void featureInfo() {
		AboutResource about = dataFlowOperations.aboutOperation().get();
		if (isPrometheusPresent() || isInfluxPresent()) {
			assertTrue(about.getFeatureInfo().isGrafanaEnabled());
		}
		assertTrue(about.getFeatureInfo().isAnalyticsEnabled());
		assertTrue(about.getFeatureInfo().isStreamsEnabled());
		assertTrue(about.getFeatureInfo().isTasksEnabled());
		assertFalse(about.getFeatureInfo().isSchedulesEnabled());
	}

	@Test
	public void appsCount() {
		assertThat(dataFlowOperations.appRegistryOperations().list().getMetadata().getTotalElements(),
				greaterThanOrEqualTo(68L));
	}

	// -----------------------------------------------------------------------
	//                            STREAM  TESTS
	// -----------------------------------------------------------------------
	@Test
	public void streamTransform() {
		logger.info("stream-transform-test");
		try (Stream stream = Stream.builder(dataFlowOperations)
				.name("transform-test")
				.definition("http | transform --expression=payload.toUpperCase() | log")
				.create()
				.deploy(testDeploymentProperties())) {

			assertThat(stream.getStatus(), is(oneOf(DEPLOYING, PARTIAL)));
			Wait.on(stream).withTimeout(Duration.ofMinutes(10)).until(s -> s.getStatus().equals(DEPLOYED));

			Map<String, String> httpApp = runtimeApps.getApplicationInstances(stream.getName(), "http")
					.values().iterator().next();

			String message = "Unique Test message: " + new Random().nextInt();

			restTemplate.postForObject(runtimeApps.getApplicationInstanceUrl(httpApp), message, String.class);

			Wait.on(stream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
					.contains(message.toUpperCase()));
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

			assertThat(stream.getStatus(), is(oneOf(DEPLOYING, PARTIAL)));
			Wait.on(stream).withTimeout(Duration.ofMinutes(10)).until(s -> s.getStatus().equals(DEPLOYED));

			Map<String, String> httpApp = runtimeApps.getApplicationInstances(stream.getName(), "http")
					.values().iterator().next();

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpApp);

			String message = "How much wood would a woodchuck chuck if a woodchuck could chuck wood";
			restTemplate.postForObject(httpAppUrl, message, String.class);

			Wait.on(stream)
					.withDescription("Each partition should contain parts of the message")
					.withTimeout(Duration.ofSeconds(60))
					.until(s -> {
						Collection<String> logs = runtimeApps.applicationInstanceLogs(s.getName(), "log").values();

						if (logs.size() != 2) return false;

						return logs.stream()
								// partition order is undetermined
								.map(log -> (log.contains("WOODCHUCK-0")) ?
										allMatch(log, "WOODCHUCK-0", "How", "chuck") :
										allMatch(log, "WOODCHUCK-1", "much", "wood", "would", "if", "a", "woodchuck", "could"))
								.reduce(Boolean::logicalAnd)
								.orElse(false);
					});
		}
	}

	private boolean allMatch(String inputStr, String... items) {
		return Arrays.stream(items).allMatch(inputStr::contains);
	}

	@Test
	public void streamLifecycle() {
		logger.info("stream-lifecycle-test: DEPLOY");
		try (Stream stream = Stream.builder(dataFlowOperations)
				.name("lifecycle-test")
				.definition("time | log --log.expression='TICKTOCK - TIMESTAMP: '.concat(payload)")
				.create()
				.deploy(testDeploymentProperties())) {

			assertThat(stream.getStatus(), is(oneOf(DEPLOYING, PARTIAL)));
			Wait.on(stream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));

			Wait.on(stream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
					.contains("TICKTOCK - TIMESTAMP:"));

			assertThat(stream.history().size(), is(1));
			assertThat(stream.history().get(1), is(DEPLOYED));

			// UPDATE
			logger.info("stream-lifecycle-test: UPDATE");
			stream.update(new DeploymentPropertiesBuilder()
					.put("app.log.log.expression", "'Updated TICKTOCK - TIMESTAMP: '.concat(payload)")
					// TODO investigate why on update the app-starters-core overrides the original web.exposure.include!!!
					.put("app.*.management.endpoints.web.exposure.include", "*")
					.build());

			Wait.on(stream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));
			assertThat(stream.getStatus(), is(DEPLOYED));

			Wait.on(stream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
					.contains("Updated TICKTOCK - TIMESTAMP:"));

			assertThat(stream.history().size(), is(2));
			assertThat(stream.history().get(1), is(DELETED));
			assertThat(stream.history().get(2), is(DEPLOYED));

			// ROLLBACK
			logger.info("stream-lifecycle-test: ROLLBACK");
			stream.rollback(0);

			Wait.on(stream).until(s -> s.getStatus().equals(DEPLOYED));
			assertThat(stream.getStatus(), is(DEPLOYED));

			Wait.on(stream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
					.contains("TICKTOCK - TIMESTAMP:"));

			assertThat(stream.history().size(), is(3));
			assertThat(stream.history().get(1), is(DELETED));
			assertThat(stream.history().get(2), is(DELETED));
			assertThat(stream.history().get(3), is(DEPLOYED));

			// UNDEPLOY
			logger.info("stream-lifecycle-test: UNDEPLOY");
			stream.undeploy();

			Wait.on(stream).until(s -> s.getStatus().equals(UNDEPLOYED));
			assertThat(stream.getStatus(), is(UNDEPLOYED));

			assertThat(stream.history().size(), is(3));
			assertThat(stream.history().get(1), is(DELETED));
			assertThat(stream.history().get(2), is(DELETED));
			assertThat(stream.history().get(3), is(DELETED));

			assertThat(dataFlowOperations.streamOperations().list().getMetadata().getTotalElements(), is(1L));
			// DESTROY
		}
		logger.info("stream-lifecycle-test: DESTROY");
		assertThat(dataFlowOperations.streamOperations().list().getMetadata().getTotalElements(), is(0L));
	}

	@Test
	public void scaleApplicationInstances() {
		logger.info("stream-scale-test");
		try (Stream stream = Stream.builder(dataFlowOperations)
				.name("stream-scale-test")
				.definition("time | log --log.expression='TICKTOCK - TIMESTAMP: '.concat(payload)")
				.create()
				.deploy(testDeploymentProperties())) {

			Wait.on(stream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));

			final StreamApplication time = new StreamApplication("time");
			final StreamApplication log = new StreamApplication("log");

			Map<StreamApplication, Map<String, String>> streamApps = stream.runtimeApps();
			assertThat(streamApps.size(), is(2));
			assertThat(streamApps.get(time).size(), is(1));
			assertThat(streamApps.get(log).size(), is(1));

			// Scale up log
			stream.scaleApplicationInstances(log, 2, Collections.emptyMap());

			Wait.on(stream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));
			Wait.on(stream).withTimeout(Duration.ofMinutes(5)).until(s -> s.runtimeApps().get(log).size() == 2);

			assertThat(stream.getStatus(), is(DEPLOYED));
			streamApps = stream.runtimeApps();
			assertThat(streamApps.size(), is(2));
			assertThat(streamApps.get(time).size(), is(1));
			assertThat(streamApps.get(log).size(), is(2));
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

			Wait.on(logStream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));
			Wait.on(httpStream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));

			String message = "Unique Test message: " + new Random().nextInt();

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpStream.getName(), "http");
			restTemplate.postForObject(httpAppUrl, message, String.class);

			Wait.on(logStream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
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

			Wait.on(httpLogStream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));
			Wait.on(tapStream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));

			String message = "Unique Test message: " + new Random().nextInt();

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpLogStream.getName(), "http");
			restTemplate.postForObject(httpAppUrl, message, String.class);

			Wait.on(tapStream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
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

			Wait.on(logStream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));
			Wait.on(httpStreamOne).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));
			Wait.on(httpStreamTwo).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));

			String messageOne = "Unique Test message: " + new Random().nextInt();

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpStreamOne.getName(), "http");
			restTemplate.postForObject(httpAppUrl, messageOne, String.class);

			Wait.on(logStream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
					.contains(messageOne));

			String messageTwo = "Unique Test message: " + new Random().nextInt();

			String httpAppUrl2 = runtimeApps.getApplicationInstanceUrl(httpStreamTwo.getName(), "http");
			restTemplate.postForObject(httpAppUrl2, messageTwo, String.class);

			Wait.on(logStream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
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

			Wait.on(fooLogStream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));
			Wait.on(barLogStream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));
			Wait.on(httpStream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpStream.getName(), "http");
			restTemplate.postForObject(httpAppUrl, "abcd", String.class);
			restTemplate.postForObject(httpAppUrl, "defg", String.class);

			Wait.on(fooLogStream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
					.contains("abcd-foo"));
			Wait.on(barLogStream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
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

				Wait.on(stream).withTimeout(Duration.ofMinutes(10)).until(s -> s.getStatus().equals(DEPLOYED));

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

					assertThat("Number of passed messages must match 3", JsonPath.read(
							restTemplate.getForObject("http://localhost:9090/api/v1/query?query=message_my_http_counter_total", String.class),
							"$.data.result[0].value[1]"), is("3"));
				}
				else if (isInfluxPresent()) {
					//http://localhost:8086/query?db=myinfluxdb&q=SELECT%20%22count%22%20FROM%20%22spring_integration_send%22
					//http://localhost:8086/query?db=myinfluxdb&q=SHOW%20MEASUREMENTS
					Thread.sleep(Duration.ofSeconds(30).toMillis());

					// http://localhost:8086/query?q=SHOW%20DATABASES
					assertThat(JsonPath.read(
							restTemplate.getForObject("http://localhost:8086/query?q=SHOW DATABASES", String.class),
							"$.results[0].series[0].values[1][0]"), is("myinfluxdb"));

					// http://localhost:8086/query?db=myinfluxdb&q=SELECT%20value%20FROM%20%22message_my_http_counter%22%20GROUP%20BY%20%2A%20ORDER%20BY%20ASC%20LIMIT%201
					assertThat("Number of passed messages must match 3", JsonPath.read(
							restTemplate.getForObject("http://localhost:8086/query?db=myinfluxdb&q=SELECT value FROM \"message_my_http_counter\" GROUP BY * ORDER BY ASC LIMIT 1", String.class),
							"$.results[0].series[0].values[0][1]"), is(3));

					// http://localhost:8086/query?db=myinfluxdb&q=SELECT%20%2A%20FROM%20%22my_http_counter%22
					String myHttpCounter = restTemplate.getForObject("http://localhost:8086/query?db=myinfluxdb&q=SELECT * FROM \"my_http_counter\"", String.class);
					assertThat(JsonPath.read(myHttpCounter, "$.results[0].series[0].values[0][6]"), is("" + message1.length()));
					assertThat(JsonPath.read(myHttpCounter, "$.results[0].series[0].values[1][6]"), is("" + message2.length()));
					assertThat(JsonPath.read(myHttpCounter, "$.results[0].series[0].values[2][6]"), is("" + message3.length()));
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

		assertThat(taskOperations.list().getContent().size(), is(3));
		taskOperations.destroy(taskDefinitionName);
		assertThat(taskOperations.list().getContent().size(), is(0));
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
		assertThat(jobExecutionResources.size(), is(2));

		taskOperations.destroy(taskDefinitionName);
	}

	private void waitTaskCompletion(String taskDefinitionName, int taskExecutionCount) {
		Wait.on(taskDefinitionName).until(taskDefName -> {
			Collection<TaskExecutionResource> taskExecutions =
					taskOperations.executionListByTaskName(taskDefName).getContent();
			return (taskExecutions.size() >= taskExecutionCount) &&
					taskExecutions.stream()
							.map(execution -> execution != null && execution.getEndTime() != null)
							.reduce(Boolean::logicalAnd)
							.orElse(false);
		});
	}

	private void assertCompletionWithExitCode(String taskDefinitionName, int exitCode) {
		taskOperations.executionListByTaskName(taskDefinitionName).getContent().stream()
				.forEach(taskExecution -> assertThat(taskExecution.getExitCode(), is(exitCode)));
	}

	/**
	 * Attempt to retrieve the current DataFlow version from the generated application.yml
	 * @param defaultVersion Default version to use if it fail to retrieve the current version.
	 * @return If available returns the DataFlow version from the application.yml or default version otherwise.
	 */
	private static String findCurrentDataFlowVersion(String defaultVersion) {
		try {
			String content = StreamUtils.copyToString(new ClassPathResource("application.yml").getInputStream(),
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

	private boolean isPrometheusPresent() {
		return runtimeApps.isServicePresent("http://localhost:9090/api/v1/query?query=up");
	}

	private boolean isInfluxPresent() {
		return runtimeApps.isServicePresent("http://localhost:8086/ping");
	}
}
