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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.integration.test.util.DockerComposeFactory;
import org.springframework.cloud.dataflow.integration.test.util.DockerComposeFactoryProperties;
import org.springframework.cloud.dataflow.integration.test.util.ResourceExtractor;
import org.springframework.cloud.dataflow.integration.test.util.RuntimeApplicationHelper;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.dsl.DeploymentPropertiesBuilder;
import org.springframework.cloud.dataflow.rest.client.dsl.Stream;
import org.springframework.cloud.dataflow.rest.client.dsl.StreamApplication;
import org.springframework.cloud.dataflow.rest.client.dsl.StreamDefinition;
import org.springframework.cloud.dataflow.rest.client.dsl.task.Task;
import org.springframework.cloud.dataflow.rest.client.dsl.task.TaskBuilder;
import org.springframework.cloud.dataflow.rest.resource.DetailedAppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionStatus;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataFlow smoke tests that by default uses docker-compose files to install the Data Flow local platform:
 *  - https://dataflow.spring.io/docs/installation/local/docker/
 *  - https://dataflow.spring.io/docs/installation/local/docker-customize/
 * The Palantir DockerMachine and DockerComposeExtension are used to programmatically deploy the docker-compose files.
 *
 * The {@link DockerComposeFactoryProperties} properties and variables are used to configure the {@link DockerComposeFactory}.
 *
 * The {@link DockerComposeFactoryProperties#TEST_DOCKER_COMPOSE_PATHS} property allow to configure the list of docker-compose files
 * used for the test. It accepts a comma separated list of docker-compose yaml file names. It supports local files names
 * as well  http:/https:, classpath: or specific file: locations. Consult the {@link ResourceExtractor} for further
 * information.
 *
 * The {@link DockerComposeFactoryProperties#TEST_DOCKER_COMPOSE_DATAFLOW_VERSIONN},
 * {@link DockerComposeFactoryProperties#TEST_DOCKER_COMPOSE_SKIPPER_VERSIONN},
 * {@link DockerComposeFactoryProperties#TEST_DOCKER_COMPOSE_STREAM_APPS_URI},
 * {@link DockerComposeFactoryProperties#TEST_DOCKER_COMPOSE_TASK_APPS_URI}
 * properties specify the dataflow/skipper versions as well as the version of the Apps and Tasks used.
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
 * When the {@link DockerComposeFactoryProperties#TEST_DOCKER_COMPOSE_DISABLE_EXTENSION} is set to true the
 * Docker Compose installation is skipped. In this case the {@link DataFlowITProperties#getDataflowServerUrl} should be
 * used to connect the IT tests to an external pre-configured SCDF server.
 *
 * For example to run the following test suite against SCDF Kubernetes cluster deployed on GKE:
 * <code>
 *    ./mvnw clean install -pl spring-cloud-dataflow-server -Dtest=foo -DfailIfNoTests=false \
 *        -Dtest.docker.compose.disable.extension=true \
 *        -Dtest.docker.compose.dataflowServerUrl=https://scdf-server.gke.io \
 *        -Pfailsafe
 * </code>
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
 * Follow the https://dataflow.spring.io/docs/installation/local/docker-customize/#multi-platform-support
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
@EnableConfigurationProperties({ DataFlowITProperties.class })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataFlowIT {

	private static final Logger logger = LoggerFactory.getLogger(DataFlowIT.class);

	@Autowired
	private DataFlowITProperties testProperties;

	/**
	 * REST and DSL clients used to interact with the SCDF server and run the tests.
	 */
	private DataFlowTemplate dataFlowOperations;
	private RuntimeApplicationHelper runtimeApps;
	private RestTemplate restTemplate;

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
		restTemplate = new RestTemplate(); // used for HTTP post in tests

		Awaitility.setDefaultPollInterval(Duration.ofSeconds(5));
		Awaitility.setDefaultTimeout(Duration.ofMinutes(10));
	}

	@AfterEach
	public void after() {
		dataFlowOperations.streamOperations().destroyAll();
		dataFlowOperations.taskOperations().destroyAll();
	}

	@Test
	@Order(Integer.MIN_VALUE)
	public void aboutTestInfo() {
		logger.info("Available platforms: " + dataFlowOperations.streamOperations().listPlatforms().stream()
				.map(d -> String.format("[name: %s, type: %s]", d.getName(), d.getType())).collect(Collectors.joining()));
		logger.info(String.format("Selected platform: [name: %s, type: %s]", runtimeApps.getPlatformName(), runtimeApps.getPlatformType()));
		logger.info("Wait until at least 60 apps are registered in SCDF");
		Awaitility.await().until(() -> dataFlowOperations.appRegistryOperations().list().getMetadata().getTotalElements() >= 60L);
	}

	@Test
	public void applicationMetadataTests() {
		logger.info("application-metadata-test");
		// Maven app with metadata
		DetailedAppRegistrationResource mavenAppWithJarMetadata = dataFlowOperations.appRegistryOperations()
				.info("file", ApplicationType.sink, false);
		assertThat(mavenAppWithJarMetadata.getOptions()).hasSize(8);

		// Maven app without metadata
		dataFlowOperations.appRegistryOperations().register("maven-app-without-metadata", ApplicationType.sink,
				"maven://org.springframework.cloud.stream.app:file-sink-kafka:2.1.1.RELEASE", null, true);
		DetailedAppRegistrationResource mavenAppWithoutMetadata = dataFlowOperations.appRegistryOperations()
				.info("maven-app-without-metadata", ApplicationType.sink, false);
		assertThat(mavenAppWithoutMetadata.getOptions()).hasSize(8);

		// Docker app with container image metadata
		dataFlowOperations.appRegistryOperations().register("docker-app-with-container-metadata", ApplicationType.source,
				"docker:springcloudstream/time-source-kafka:2.1.4.RELEASE", null, true);
		DetailedAppRegistrationResource dockerAppWithContainerMetadata = dataFlowOperations.appRegistryOperations()
				.info("docker-app-with-container-metadata", ApplicationType.source, false);
		assertThat(dockerAppWithContainerMetadata.getOptions()).hasSize(6);

		// Docker app with container image metadata with escape characters.
		dataFlowOperations.appRegistryOperations().register("docker-app-with-container-metadata-escape-chars", ApplicationType.source,
				"docker:springcloudstream/http-source-rabbit:2.1.3.RELEASE", null, true);
		DetailedAppRegistrationResource dockerAppWithContainerMetadataWithEscapeChars = dataFlowOperations.appRegistryOperations()
				.info("docker-app-with-container-metadata-escape-chars", ApplicationType.source, false);
		assertThat(dockerAppWithContainerMetadataWithEscapeChars.getOptions()).hasSize(6);

		// Docker app without metadata
		dataFlowOperations.appRegistryOperations().register("docker-app-without-metadata", ApplicationType.sink,
				"docker:springcloudstream/file-sink-kafka:2.1.1.RELEASE", null, true);
		DetailedAppRegistrationResource dockerAppWithoutMetadata = dataFlowOperations.appRegistryOperations()
				.info("docker-app-without-metadata", ApplicationType.sink, false);
		assertThat(dockerAppWithoutMetadata.getOptions()).hasSize(0);

		// Docker app with jar metadata
		dataFlowOperations.appRegistryOperations().register("docker-app-with-jar-metadata", ApplicationType.sink,
				"docker:springcloudstream/file-sink-kafka:2.1.1.RELEASE",
				"maven://org.springframework.cloud.stream.app:file-sink-kafka:jar:metadata:2.1.1.RELEASE", true);
		DetailedAppRegistrationResource dockerAppWithJarMetadata = dataFlowOperations.appRegistryOperations()
				.info("docker-app-with-jar-metadata", ApplicationType.sink, false);
		assertThat(dockerAppWithJarMetadata.getOptions()).hasSize(8);

		// unregister the test apps
		dataFlowOperations.appRegistryOperations().unregister("docker-app-with-container-metadata", ApplicationType.source);
		dataFlowOperations.appRegistryOperations().unregister("docker-app-with-container-metadata-escape-chars", ApplicationType.source);
		dataFlowOperations.appRegistryOperations().unregister("docker-app-without-metadata", ApplicationType.sink);
		dataFlowOperations.appRegistryOperations().unregister("maven-app-without-metadata", ApplicationType.sink);
		dataFlowOperations.appRegistryOperations().unregister("docker-app-with-jar-metadata", ApplicationType.sink);
	}

	// -----------------------------------------------------------------------
	//                          PLATFORM  TESTS
	// -----------------------------------------------------------------------
	@Test
	public void featureInfo() {
		logger.info("platform-feature-info-test");
		AboutResource about = dataFlowOperations.aboutOperation().get();
		assertThat(about.getFeatureInfo().isAnalyticsEnabled()).isTrue();
		assertThat(about.getFeatureInfo().isStreamsEnabled()).isTrue();
		assertThat(about.getFeatureInfo().isTasksEnabled()).isTrue();
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

			Awaitility.await().until(() -> stream.getStatus().equals(DEPLOYED));

			Map<String, String> httpApp = runtimeApps.getApplicationInstances(stream.getName(), "http")
					.values().iterator().next();

			String message = "Unique Test message: " + new Random().nextInt();

			httpPost(runtimeApps.getApplicationInstanceUrl(httpApp), message);

			Awaitility.await().until(() -> stream.logs(app("log")).contains(message.toUpperCase()));
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

			Awaitility.await().until(() -> stream.getStatus().equals(DEPLOYED));

			Map<String, String> httpApp = runtimeApps.getApplicationInstances(stream.getName(), "http")
					.values().iterator().next();

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpApp);

			String message = "How much wood would a woodchuck chuck if a woodchuck could chuck wood";
			httpPost(httpAppUrl, message);

			Awaitility.await().until(() -> {
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
				.definition("time | log --log.name=\"\" --log.expression='TICKTOCK - TIMESTAMP: '.concat(payload)")
				.create()
				.deploy(testDeploymentProperties())) {

			assertThat(stream.getStatus()).is(
					condition(status -> status.equals(DEPLOYING) || status.equals(PARTIAL)));

			Awaitility.await().until(() -> stream.getStatus().equals(DEPLOYED));

			Awaitility.await().until(
					() -> stream.logs(app("log")).contains("TICKTOCK - TIMESTAMP:"));

			assertThat(stream.history().size()).isEqualTo(1L);
			Awaitility.await().until(() -> stream.history().get(1).equals(DEPLOYED));

			assertThat(stream.logs()).contains("TICKTOCK - TIMESTAMP:");
			assertThat(stream.logs(app("log"))).contains("TICKTOCK - TIMESTAMP:");

			// UPDATE
			logger.info("stream-lifecycle-test: UPDATE");
			stream.update(new DeploymentPropertiesBuilder()
					.put("app.log.log.expression", "'Updated TICKTOCK - TIMESTAMP: '.concat(payload)")
					// TODO investigate why on update the app-starters-core overrides the original web.exposure.include!!!
					.put("app.*.management.endpoints.web.exposure.include", "*")
					.build());

			Awaitility.await().until(() -> stream.getStatus().equals(DEPLOYED));

			Awaitility.await().until(
					() -> stream.logs(app("log")).contains("Updated TICKTOCK - TIMESTAMP:"));

			assertThat(stream.history().size()).isEqualTo(2);
			Awaitility.await().until(() -> stream.history().get(1).equals(DELETED));
			Awaitility.await().until(() -> stream.history().get(2).equals(DEPLOYED));

			// ROLLBACK
			logger.info("stream-lifecycle-test: ROLLBACK");
			stream.rollback(0);

			Awaitility.await().until(() -> stream.getStatus().equals(DEPLOYED));
			assertThat(stream.getStatus()).isEqualTo(DEPLOYED);

			Awaitility.await().until(
					() -> stream.logs(app("log")).contains("TICKTOCK - TIMESTAMP:"));

			assertThat(stream.history().size()).isEqualTo(3);
			Awaitility.await().until(() -> stream.history().get(1).equals(DELETED));
			Awaitility.await().until(() -> stream.history().get(2).equals(DELETED));
			Awaitility.await().until(() -> stream.history().get(3).equals(DEPLOYED));

			// UNDEPLOY
			logger.info("stream-lifecycle-test: UNDEPLOY");
			stream.undeploy();

			Awaitility.await().until(() -> stream.getStatus().equals(UNDEPLOYED));
			assertThat(stream.getStatus()).isEqualTo(UNDEPLOYED);

			assertThat(stream.history().size()).isEqualTo(3);
			Awaitility.await().until(() -> stream.history().get(1).equals(DELETED));
			Awaitility.await().until(() -> stream.history().get(2).equals(DELETED));
			Awaitility.await().until(() -> stream.history().get(3).equals(DELETED));

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

			final StreamApplication time = app("time");
			final StreamApplication log = app("log");

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

			Awaitility.await().until(() -> logStream.logs(app("log")).contains(message));
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

			Awaitility.await().until(
					() -> tapStream.logs(app("log")).contains(message));
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

			Awaitility.await().until(
					() -> logStream.logs(app("log")).contains(messageOne));

			String messageTwo = "Unique Test message: " + new Random().nextInt();

			String httpAppUrl2 = runtimeApps.getApplicationInstanceUrl(httpStreamTwo.getName(), "http");
			httpPost(httpAppUrl2, messageTwo);

			Awaitility.await().until(
					() -> logStream.logs(app("log")).contains(messageTwo));

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

			Awaitility.await().until(() -> fooLogStream.logs(app("log")).contains("abcd-foo"));
			Awaitility.await().until(() -> barLogStream.logs(app("log")).contains("defg-bar"));
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
				JsonAssertions.assertThatJson(myHttpCounter).inPath("$.results[0].series[0].values[0][7]")
						.isEqualTo(String.format("\"%s\"", message1.length()));
				JsonAssertions.assertThatJson(myHttpCounter).inPath("$.results[0].series[0].values[1][7]")
						.isEqualTo(String.format("\"%s\"", message2.length()));
				JsonAssertions.assertThatJson(myHttpCounter).inPath("$.results[0].series[0].values[2][7]")
						.isEqualTo(String.format("\"%s\"", message3.length()));
			});
		}
	}

	// -----------------------------------------------------------------------
	//                     STREAM  CONFIG SERVER (PCF ONLY)
	// -----------------------------------------------------------------------
	@Test
	@EnabledIfSystemProperty(named = "PLATFORM_TYPE", matches = "cloudfoundry")
	@DisabledIfSystemProperty(named = "SKIP_CLOUD_CONFIG", matches = "true")
	public void streamWithConfigServer() {

		logger.info("stream-server-config-test");

		try (Stream stream = Stream.builder(dataFlowOperations)
				.name("TICKTOCK-config-server")
				.definition("time | log")
				.create()
				.deploy(new DeploymentPropertiesBuilder()
						.putAll(testDeploymentProperties())
						.put("app.log.spring.profiles.active", "test")
						.put("deployer.log.cloudfoundry.services", "cloud-config-server")
						.put("app.log.spring.cloud.config.name", "MY_CONFIG_TICKTOCK_LOG_NAME")
						.build())) {

			Awaitility.await(stream.getName() + " failed to deploy!")
					.until(() -> stream.getStatus().equals(DEPLOYED));

			Awaitility.await("Source not started").until(
					() -> stream.logs(app("time")).contains("Started TimeSource"));
			Awaitility.await("Sink not started").until(
					() -> stream.logs(app("log")).contains("Started LogSink"));
			Awaitility.await("No output found").until(
					() -> stream.logs(app("log")).contains("TICKTOCK CLOUD CONFIG - TIMESTAMP:"));
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

		if (this.runtimeApps.getPlatformType().equalsIgnoreCase(RuntimeApplicationHelper.KUBERNETES_PLATFORM_TYPE)) {
			propertiesBuilder.put("app.*.server.port", "80");
			propertiesBuilder.put("deployer.*.kubernetes.createLoadBalancer", "true"); // requires LoadBalancer support on the platform
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
		return runtimeApps.isServicePresent(testProperties.getPrometheusUrl() + "/api/v1/query?query=up");
	}

	private boolean influxPresent() {
		return runtimeApps.isServicePresent(testProperties.getInfluxUrl() + "/ping");
	}

	private static Condition<String> condition(Predicate predicate) {
		return new Condition<>(predicate, "");
	}

	private StreamApplication app(String appName) {
		return new StreamApplication(appName);
	}

	// -----------------------------------------------------------------------
	//                               TASK TESTS
	// -----------------------------------------------------------------------
	public static final int EXIT_CODE_SUCCESS = 0;
	public static final int EXIT_CODE_ERROR = 1;

	@Test
	public void timestampTask() {
		logger.info("task-timestamp-test");

		try (Task task = Task.builder(dataFlowOperations)
				.name(randomTaskName())
				.definition("timestamp")
				.description("Test timestamp task")
				.build()) {

			// task first launch
			long launchId1 = task.launch();

			Awaitility.await().until(() -> task.executionStatus(launchId1) == TaskExecutionStatus.COMPLETE);
			assertThat(task.executions().size()).isEqualTo(1);
			assertThat(task.execution(launchId1).isPresent()).isTrue();
			assertThat(task.execution(launchId1).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);

			// task second launch
			long launchId2 = task.launch();

			Awaitility.await().until(() -> task.executionStatus(launchId2) == TaskExecutionStatus.COMPLETE);
			assertThat(task.executions().size()).isEqualTo(2);
			assertThat(task.execution(launchId2).isPresent()).isTrue();
			assertThat(task.execution(launchId2).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);

			// All
			task.executions().forEach(execution -> assertThat(execution.getExitCode()).isEqualTo(EXIT_CODE_SUCCESS));
		}
	}

	@Test
	public void composedTask() {
		logger.info("task-composed-task-runner-test");

		TaskBuilder taskBuilder = Task.builder(dataFlowOperations);

		try (Task task = taskBuilder
				.name(randomTaskName())
				.definition("a: timestamp && b:timestamp")
				.description("Test composedTask")
				.build()) {

			assertThat(task.composedTaskChildTasks().size()).isEqualTo(2);

			// first launch

			long launchId1 = task.launch();

			Awaitility.await().until(() -> task.executionStatus(launchId1) == TaskExecutionStatus.COMPLETE);

			assertThat(task.executions().size()).isEqualTo(1);
			assertThat(task.executionStatus(launchId1)).isEqualTo(TaskExecutionStatus.COMPLETE);
			assertThat(task.execution(launchId1).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);

			task.composedTaskChildTasks().forEach(childTask -> {
				assertThat(childTask.executions().size()).isEqualTo(1);
				assertThat(childTask.executionByParentExecutionId(launchId1).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);
			});

			task.executions().forEach(execution -> assertThat(execution.getExitCode()).isEqualTo(EXIT_CODE_SUCCESS));

			// second launch
			long launchId2 = task.launch();

			Awaitility.await().until(() -> task.executionStatus(launchId2) == TaskExecutionStatus.COMPLETE);

			assertThat(task.executions().size()).isEqualTo(2);
			assertThat(task.executionStatus(launchId2)).isEqualTo(TaskExecutionStatus.COMPLETE);
			assertThat(task.execution(launchId2).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);

			task.composedTaskChildTasks().forEach(childTask -> {
				assertThat(childTask.executions().size()).isEqualTo(2);
				assertThat(childTask.executionByParentExecutionId(launchId2).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);
			});

			assertThat(taskBuilder.allTasks().size()).isEqualTo(3);
		}
		assertThat(taskBuilder.allTasks().size()).isEqualTo(0);
	}

	@Test
	public void multipleComposedTaskWithArguments() {
		logger.info("task-multiple-composed-task-with-arguments-test");

		TaskBuilder taskBuilder = Task.builder(dataFlowOperations);
		try (Task task = taskBuilder
				.name(randomTaskName())
				.definition("a: timestamp && b:timestamp")
				.description("Test multipleComposedTaskhWithArguments")
				.build()) {

			assertThat(task.composedTaskChildTasks().size()).isEqualTo(2);

			// first launch
			List<String> arguments = Arrays.asList("--increment-instance-enabled=true");
			long launchId1 = task.launch(arguments);

			Awaitility.await().until(() -> task.executionStatus(launchId1) == TaskExecutionStatus.COMPLETE);

			assertThat(task.executions().size()).isEqualTo(1);
			assertThat(task.executionStatus(launchId1)).isEqualTo(TaskExecutionStatus.COMPLETE);
			assertThat(task.execution(launchId1).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);

			task.composedTaskChildTasks().forEach(childTask -> {
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

			task.composedTaskChildTasks().forEach(childTask -> {
				assertThat(childTask.executions().size()).isEqualTo(2);
				assertThat(childTask.executionByParentExecutionId(launchId2).get().getExitCode()).isEqualTo(EXIT_CODE_SUCCESS);
			});

			assertThat(task.jobExecutionResources().size()).isEqualTo(2);

			assertThat(taskBuilder.allTasks().size()).isEqualTo(3);
		}
		assertThat(taskBuilder.allTasks().size()).isEqualTo(0);
	}

	private static String randomTaskName() {
		return "task-" + UUID.randomUUID().toString().substring(0, 10);
	}
}
