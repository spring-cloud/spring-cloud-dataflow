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
import java.nio.file.Path;
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

import com.jayway.jsonpath.JsonPath;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.assertj.core.api.Condition;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.integration.test.util.DockerComposeFactory;
import org.springframework.cloud.dataflow.integration.test.util.DockerComposeFactoryProperties;
import org.springframework.cloud.dataflow.integration.test.util.ResourceExtractor;
import org.springframework.cloud.dataflow.integration.test.util.RuntimeApplicationHelper;
import org.springframework.cloud.dataflow.integration.test.util.task.dsl.Task;
import org.springframework.cloud.dataflow.integration.test.util.task.dsl.Tasks;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.dsl.DeploymentPropertiesBuilder;
import org.springframework.cloud.dataflow.rest.client.dsl.Stream;
import org.springframework.cloud.dataflow.rest.client.dsl.StreamApplication;
import org.springframework.cloud.dataflow.rest.client.dsl.StreamDefinition;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionStatus;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataFlow smoke tests that uses docker-compose to create fully fledged, local test environment.
 *
 * Test fixture applies the same docker compose files used for the Data Flow local installation:
 *  - https://dataflow.spring.io/docs/installation/local/docker/
 *  - https://dataflow.spring.io/docs/installation/local/docker-customize/
 *
 * The Palantir DockerMachine and DockerComposeExtension are used to programmatically deploy the docker-compose files.
 *
 * All important bootstrap parameters are configurable via the {@link DockerComposeFactoryProperties} properties and variables.
 *
 * The {@link DockerComposeFactoryProperties#TEST_DOCKER_COMPOSE_PATHS} property allow to configure the list of docker-compose files
 * used for the test. It accepts a comma separated list of docker-compose yaml file names. It supports local files names
 * as well  http:/https:, classpath: or specific file: locations. Consult the {@link ResourceExtractor} for further
 * information.
 *
 * The {@link DockerComposeFactoryProperties#TEST_DOCKER_COMPOSE_DATAFLOW_VERSIONN}, {@link DockerComposeFactoryProperties#TEST_DOCKER_COMPOSE_SKIPPER_VERSIONN},
 * {@link DockerComposeFactoryProperties#TEST_DOCKER_COMPOSE_STREAM_APPS_URI} and {@link DockerComposeFactoryProperties#TEST_DOCKER_COMPOSE_TASK_APPS_URI}
 * properties (configured via the DockerMachine) allow to specify the dataflow/skipper versions as well as the version
 * of the Apps and Tasks used.
 *
 * Set the {@link DockerComposeFactoryProperties#TEST_DOCKER_COMPOSE_PULLONSTARTUP} to false to use the local docker images instead
 * of pulling latest on from the Docker Hub.
 *
 * Logs for all docker containers (expect deployed apps) are saved under target/dockerLogs/DockerComposeIT.
 *
 * The Data Flow REST API (https://docs.spring.io/spring-cloud-dataflow/docs/current/reference/htmlsingle/#api-guide),
 * Java REST Clients (such as DataFlowTemplate, RuntimeOperations, TaskOperations) and the
 * Java DSL (https://dataflow.spring.io/docs/feature-guides/streams/java-dsl/) are used by the tests to interact with
 * the Data Flow environment.
 *
 * The {@link DockerComposeTestProperties} allow to disable the Docker Compose installation
 * all together and usually is used in combination with {@link DockerComposeTestProperties#getDataflowServerUrl}
 * to connect the and run the test to an external pre-configured cluster.
 * Use {@link DockerComposeTestProperties#getPlatformName()} to test no default platform on the SCDF cluster.
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
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties({ DockerComposeTestProperties.class })
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
public class DockerComposeIT {

	private static final Logger logger = LoggerFactory.getLogger(DockerComposeIT.class);

	@Autowired
	private DockerComposeTestProperties testProperties;

	/**
	 * REST and DSL clients used to interact with the SCDF server and run the tests.
	 */
	private DataFlowTemplate dataFlowOperations;
	private RuntimeApplicationHelper runtimeApps;
	private RestTemplate restTemplate;
	private Tasks tasks;

	/**
	 * Folder that collects the external docker-compose YAML files such as
	 * coming from external classpath, http/https or file locations.
	 */
	static Path tempYamlFolder = DockerComposeFactory.createTempDirectory();

	/**
	 * A JUnit 5 extension to bring up Docker containers defined in docker-compose-xxx.yml files before running tests.
	 * You can set either test.docker.compose.disable.extension property of DISABLE_DOCKER_COMPOSE_EXTENSION variable to
	 * disable the extension.
	 */
	@RegisterExtension
	public static Extension dockerCompose = DockerComposeFactory.startDockerCompose(tempYamlFolder);

	@AfterAll
	public static void afterAll() {
		if (tempYamlFolder != null && tempYamlFolder.toFile().exists()) {
			tempYamlFolder.toFile().delete();
		}
	}

	@BeforeEach
	public void before() {
		dataFlowOperations = new DataFlowTemplate(URI.create(testProperties.getDataflowServerUrl()));
		runtimeApps = new RuntimeApplicationHelper(dataFlowOperations,
				testProperties.getPlatformName(), testProperties.getKubernetesAppHostSuffix());
		tasks = new Tasks(dataFlowOperations);
		restTemplate = new RestTemplate(); // used for HTTP post in tests

		Awaitility.setDefaultPollInterval(Duration.ofSeconds(5));
		Awaitility.setDefaultTimeout(Duration.ofMinutes(10));
		Awaitility.await().until(() -> dataFlowOperations.appRegistryOperations().list().getMetadata().getTotalElements() >= 60L);
	}

	@AfterEach
	public void after() {
		dataFlowOperations.streamOperations().destroyAll();
		dataFlowOperations.taskOperations().destroyAll();
	}

	@Test
	public void aboutTestInfo() {
		logger.info("Configured server platforms: " + dataFlowOperations.streamOperations().listPlatforms().stream()
				.map(d -> String.format("[name: %s, type: %s]", d.getName(), d.getType())).collect(Collectors.joining()));
		logger.info(String.format("Selected platform: [name: %s, type: %s]", runtimeApps.getPlatformName(), runtimeApps.getPlatformType()));
	}

	// -----------------------------------------------------------------------
	//                          PLATFORM  TESTS
	// -----------------------------------------------------------------------
	@Test
	public void featureInfo() {
		logger.info("platform-feature-info-test");
		AboutResource about = dataFlowOperations.aboutOperation().get();
		assertThat(about.getFeatureInfo().isGrafanaEnabled())
				.withFailMessage(String.format("Grafana should be enabled [%s] only if either Prometheus [%s] or " +
						"Influx [%s] are enabled"), about.getFeatureInfo().isGrafanaEnabled(), prometheusPresent(), influxPresent())
				.isEqualTo(prometheusPresent() || influxPresent());
		assertThat(about.getFeatureInfo().isAnalyticsEnabled()).isTrue();
		assertThat(about.getFeatureInfo().isStreamsEnabled()).isTrue();
		assertThat(about.getFeatureInfo().isTasksEnabled()).isTrue();
		assertThat(about.getFeatureInfo().isSchedulesEnabled()).isFalse();
	}

	@Test
	public void appsCount() {
		logger.info("platform-apps-count-test");
		assertThat(dataFlowOperations.appRegistryOperations().list().getMetadata().getTotalElements())
				.isGreaterThanOrEqualTo(60L);
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
					condition(status -> status.equals(DEPLOYING) || status.equals(PARTIAL)));

			Awaitility.await().atMost(Duration.ofMinutes(10)).until(() -> stream.getStatus().equals(DEPLOYED));

			Map<String, String> httpApp = runtimeApps.getApplicationInstances(stream.getName(), "http")
					.values().iterator().next();

			String message = "Unique Test message: " + new Random().nextInt();

			httpPost(runtimeApps.getApplicationInstanceUrl(httpApp), message);

			Awaitility.await().atMost(Duration.ofMinutes(10)).until(
					() -> runtimeApps.getFirstInstanceLog(stream.getName(), "log").contains(message.toUpperCase()));
		}
	}

	@Test
	public void streamPartitioning() {
		logger.info("stream-partitioning-test (aka. WoodChuckTests)");
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
				.put("app.log.logging.pattern.level", "WOODCHUCK-${INSTANCE_INDEX:${CF_INSTANCE_INDEX:${spring.cloud.stream.instanceIndex:666}}} %5p")
				.build())) {

			assertThat(stream.getStatus()).is(
					condition(status -> status.equals(DEPLOYING) || status.equals(PARTIAL)));

			Awaitility.await().atMost(Duration.ofMinutes(10)).until(() -> stream.getStatus().equals(DEPLOYED));

			Map<String, String> httpApp = runtimeApps.getApplicationInstances(stream.getName(), "http")
					.values().iterator().next();

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpApp);

			String message = "How much wood would a woodchuck chuck if a woodchuck could chuck wood";
			httpPost(httpAppUrl, message);

			Awaitility.await().atMost(Duration.ofMinutes(10)).until(() -> {
				Collection<String> logs = runtimeApps.applicationInstanceLogs(stream.getName(), "log").values();

				return (logs.size() == 2) && logs.stream()
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

			assertThat(stream.getStatus()).is(
					condition(status -> status.equals(DEPLOYING) || status.equals(PARTIAL)));

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
			httpPost(httpAppUrl, message);

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
			httpPost(httpAppUrl, message);

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
			httpPost(httpAppUrl, messageOne);

			Awaitility.await().until(() -> runtimeApps.getFirstInstanceLog(logStream.getName(), "log")
					.contains(messageOne));

			String messageTwo = "Unique Test message: " + new Random().nextInt();

			String httpAppUrl2 = runtimeApps.getApplicationInstanceUrl(httpStreamTwo.getName(), "http");
			httpPost(httpAppUrl2, messageTwo);

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
			httpPost(httpAppUrl, "abcd");
			httpPost(httpAppUrl, "defg");

			Awaitility.await().until(() -> runtimeApps.getFirstInstanceLog(fooLogStream.getName(), "log")
					.contains("abcd-foo"));
			Awaitility.await().until(() -> runtimeApps.getFirstInstanceLog(barLogStream.getName(), "log")
					.contains("defg-bar"));
		}
	}

	// -----------------------------------------------------------------------
	//                       STREAM  METRICS TESTS
	// -----------------------------------------------------------------------
	@DisplayName("Test Analytics Counters")
	@Test
	public void analyticsCounter() {

		if (!prometheusPresent() && !influxPresent()) {
			logger.info("stream-analytics-counter-test: SKIP - no metrics configured!");
		}

		Assumptions.assumeTrue(prometheusPresent() || influxPresent());

		logger.info("stream-analytics-counter-test");

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
			httpPost(httpAppUrl, message1);
			httpPost(httpAppUrl, message2);
			httpPost(httpAppUrl, message3);

			// Prometheus tests
			Assumptions.assumingThat(this::prometheusPresent, () -> {
				logger.info("stream-analytics-counter-test: Prometheus");

				// Wait for ~1 min for Micrometer to send first metrics to Prometheus.
				Awaitility.await().until(() -> (int) JsonPath.parse(
						httpGet(testProperties.getPrometheusUrl() + "/api/v1/query?query=my_http_counter_total"))
						.read("$.data.result.length()") > 0);

				JsonAssertions.assertThatJson(httpGet(testProperties.getPrometheusUrl() + "/api/v1/query?query=my_http_counter_total"))
						.isEqualTo(resourceToString("classpath:/my_http_counter_total.json"));

				JsonAssertions.assertThatJson(httpGet(testProperties.getPrometheusUrl() + "/api/v1/query?query=message_my_http_counter_total"))
						.inPath("$.data.result[0].value[1]")
						.isEqualTo("\"3\"");
			});

			// InfluxDB tests
			Assumptions.assumingThat(this::influxPresent, () -> {
				logger.info("stream-analytics-counter-test: InfluxDB");

				// Wait for ~1 min for Micrometer to send first metrics to Influx.
				Awaitility.await().until(() -> !JsonPath.parse(httpGet(testProperties.getInfluxUrl() + "/query?db=myinfluxdb&q=SELECT * FROM \"my_http_counter\""))
						.read("$.results[0][?(@.series)].length()").toString().equals("[]"));

				//http://localhost:8086/query?db=myinfluxdb&q=SELECT%20%22count%22%20FROM%20%22spring_integration_send%22
				//http://localhost:8086/query?db=myinfluxdb&q=SHOW%20MEASUREMENTS

				// http://localhost:8086/query?db=myinfluxdb&q=SELECT%20value%20FROM%20%22message_my_http_counter%22%20GROUP%20BY%20%2A%20ORDER%20BY%20ASC%20LIMIT%201

				// http://localhost:8086/query?q=SHOW%20DATABASES
				JsonAssertions.assertThatJson(httpGet(testProperties.getInfluxUrl() + "/query?q=SHOW DATABASES"))
						.inPath("$.results[0].series[0].values[1][0]")
						.isEqualTo("myinfluxdb");

				// http://localhost:8086/query?db=myinfluxdb&q=SELECT%20%2A%20FROM%20%22my_http_counter%22
				String myHttpCounter = httpGet(testProperties.getInfluxUrl() + "/query?db=myinfluxdb&q=SELECT * FROM \"my_http_counter\"");
				JsonAssertions.assertThatJson(myHttpCounter).inPath("$.results[0].series[0].values[0][6]")
						.isEqualTo(String.format("\"%s\"", message1.length()));
				JsonAssertions.assertThatJson(myHttpCounter).inPath("$.results[0].series[0].values[1][6]")
						.isEqualTo(String.format("\"%s\"", message2.length()));
				JsonAssertions.assertThatJson(myHttpCounter).inPath("$.results[0].series[0].values[2][6]")
						.isEqualTo(String.format("\"%s\"", message3.length()));
			});
		}
	}

	/**
	 * For the purpose of testing, disable security, expose the all actuators, and configure logfiles.
	 * @return Deployment properties required for the deployment of all test pipelines.
	 */
	private Map<String, String> testDeploymentProperties() {
		DeploymentPropertiesBuilder propertiesBuilder = new DeploymentPropertiesBuilder()
				.put(SPRING_CLOUD_DATAFLOW_SKIPPER_PLATFORM_NAME, runtimeApps.getPlatformName())
				.put("app.*.logging.file", "${PID}-test.log")
				.put("app.*.endpoints.logfile.sensitive", "false")
				.put("app.*.management.endpoints.web.exposure.include", "*")
				.put("app.*.spring.cloud.streamapp.security.enabled", "false");

		// TODO this misterious 80 port set for K8s comes from the ATs and i'm not sure why. But without it partition tests fail.
		if (this.runtimeApps.getPlatformType().equalsIgnoreCase(RuntimeApplicationHelper.KUBERNETES_PLATFORM_TYPE)) {
			propertiesBuilder.put("app.*.server.port", "80");
		}

		return propertiesBuilder.build();
	}

	private void httpPost(String url, String message) {
		restTemplate.postForObject(url, message, String.class);
	}

	private String httpGet(String url) {
		return restTemplate.getForObject(url, String.class);
	}

	private static String resourceToString(String resourcePath) throws IOException {
		return StreamUtils.copyToString(new DefaultResourceLoader().getResource(resourcePath).getInputStream(), StandardCharsets.UTF_8);
	}

	private boolean prometheusPresent() {
		return runtimeApps.isServicePresent("http://localhost:9090/api/v1/query?query=up");
	}

	private boolean influxPresent() {
		return runtimeApps.isServicePresent("http://localhost:8086/ping");
	}

	private static Condition<String> condition(Predicate predicate) {
		return new Condition<>(predicate, "");
	}

	// -----------------------------------------------------------------------
	//                               TASK TESTS
	// -----------------------------------------------------------------------
	public static final int EXIT_CODE_SUCCESS = 0;
	public static final int EXIT_CODE_ERROR = 1;

	@Test
	public void timestampTask() {
		logger.info("task-timestamp-test");

		try (Task task = tasks.builder()
				.name(randomTaskName())
				.definition("timestamp")
				.description("Test timestamp task")
				.create()) {

			// task first launch
			long launchId1 = task.launch();

			Awaitility.await().until(() -> task.executionStatus(launchId1) == TaskExecutionStatus.COMPLETE);
			assertThat(task.executions().size()).isEqualTo(1);
			assertThat(task.execution(launchId1).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);

			// task second launch
			long launchId2 = task.launch();

			Awaitility.await().until(() -> task.executionStatus(launchId2) == TaskExecutionStatus.COMPLETE);
			assertThat(task.executions().size()).isEqualTo(2);
			assertThat(task.execution(launchId2).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);

			// All
			task.executions().forEach(execution -> assertThat(execution.getExitCode()).isEqualTo(EXIT_CODE_SUCCESS));
		}
	}

	@Test
	public void composedTask() {
		logger.info("task-composed-task-runner-test");

		try (Task task = tasks.builder()
				.name(randomTaskName())
				.definition("a: timestamp && b:timestamp")
				.description("Test composedTask")
				.create()) {

			assertThat(task.children().size()).isEqualTo(2);

			// first launch

			long launchId1 = task.launch();

			Awaitility.await().until(() -> task.executionStatus(launchId1) == TaskExecutionStatus.COMPLETE);

			assertThat(task.executions().size()).isEqualTo(1);
			assertThat(task.executionStatus(launchId1)).isEqualTo(TaskExecutionStatus.COMPLETE);
			assertThat(task.execution(launchId1).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);

			task.children().forEach(childTask -> {
				assertThat(childTask.executions().size()).isEqualTo(1);
				assertThat(childTask.executionByParentExecutionId(launchId1).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);
			});

			task.executions().forEach(execution -> assertThat(execution.getExitCode()).isEqualTo(EXIT_CODE_SUCCESS));

			// second launch
			long launchId2 = task.launch();

			Awaitility.await().until(() -> task.executionStatus(launchId2) == TaskExecutionStatus.ERROR);

			assertThat(task.executions().size()).isEqualTo(2);
			assertThat(task.executionStatus(launchId2)).isEqualTo(TaskExecutionStatus.ERROR);
			assertThat(task.execution(launchId2).get().getExitCode()).isEqualTo(EXIT_CODE_ERROR);

			task.children().forEach(childTask -> {
				assertThat(childTask.executions().size()).isEqualTo(1);
				assertThat(childTask.executionByParentExecutionId(launchId2).isPresent()).isFalse();
			});

			assertThat(tasks.list().size()).isEqualTo(3);
		}
		assertThat(tasks.list().size()).isEqualTo(0);
	}

	@Test
	public void multipleComposedTaskWithArguments() {
		logger.info("task-multiple-composed-task-with-arguments-test");

		try (Task task = tasks.builder()
				.name(randomTaskName())
				.definition("a: timestamp && b:timestamp")
				.description("Test multipleComposedTaskhWithArguments")
				.create()) {

			assertThat(task.children().size()).isEqualTo(2);

			// first launch
			List<String> arguments = Arrays.asList("--increment-instance-enabled=true");
			long launchId1 = task.launch(arguments);

			Awaitility.await().until(() -> task.executionStatus(launchId1) == TaskExecutionStatus.COMPLETE);

			assertThat(task.executions().size()).isEqualTo(1);
			assertThat(task.executionStatus(launchId1)).isEqualTo(TaskExecutionStatus.COMPLETE);
			assertThat(task.execution(launchId1).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);

			task.children().forEach(childTask -> {
				assertThat(childTask.executions().size()).isEqualTo(1);
				assertThat(childTask.executionByParentExecutionId(launchId1).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);
			});

			task.executions().forEach(execution -> assertThat(execution.getExitCode()).isEqualTo(EXIT_CODE_SUCCESS));

			// second launch
			long launchId2 = task.launch(arguments);

			Awaitility.await().until(() -> task.executionStatus(launchId2) == TaskExecutionStatus.COMPLETE);

			assertThat(task.executions().size()).isEqualTo(2);
			assertThat(task.executionStatus(launchId2)).isEqualTo(TaskExecutionStatus.COMPLETE);
			assertThat(task.execution(launchId2).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);

			task.children().forEach(childTask -> {
				assertThat(childTask.executions().size()).isEqualTo(2);
				assertThat(childTask.executionByParentExecutionId(launchId2).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);
			});

			assertThat(task.jobExecutionResources().size()).isEqualTo(2);

			assertThat(tasks.list().size()).isEqualTo(3);
		}
		assertThat(tasks.list().size()).isEqualTo(0);
	}

	private static String randomTaskName() {
		return "task-" + UUID.randomUUID().toString().substring(0, 10);
	}
}
