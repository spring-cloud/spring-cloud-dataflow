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

import java.net.URI;
import java.nio.charset.Charset;
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
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.integration.test.util.ClosableDockerComposeRule;
import org.springframework.cloud.dataflow.integration.test.util.RuntimeApplicationHelper;
import org.springframework.cloud.dataflow.integration.test.util.Wait;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.client.dsl.DeploymentPropertiesBuilder;
import org.springframework.cloud.dataflow.rest.client.dsl.Stream;
import org.springframework.cloud.dataflow.rest.client.dsl.StreamDefinition;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
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
@RunWith(SpringRunner.class)
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
			//"docker-compose-prometheus.yml",   // metrics collection/visualization with Prometheus and Grafana.
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

	private DataFlowTemplate dataFlowOperations;
	private RuntimeApplicationHelper runtimeApps;
	private TaskOperations taskOperations;
	private RestTemplate restTemplate;

	@Before
	public void before() {
		dataFlowOperations = new DataFlowTemplate(URI.create("http://localhost:9393"));
		logger.info("Configured platforms: " + dataFlowOperations.streamOperations().listPlatforms().stream()
				.map(d -> String.format("[%s:%s]", d.getName(), d.getType())).collect(Collectors.joining()));
		runtimeApps = new RuntimeApplicationHelper(dataFlowOperations, TEST_PLATFORM_NAME);
		taskOperations = dataFlowOperations.taskOperations();
		Wait.on(dataFlowOperations.appRegistryOperations()).until(appRegistry ->
				appRegistry.list().getMetadata().getTotalElements() >= 68L);
		restTemplate = new RestTemplate(); // used for HTTP post in tests
	}

	@After
	public void after() {
		dataFlowOperations.streamOperations().destroyAll();
		dataFlowOperations.taskOperations().destroyAll();
	}

	@Test
	public void featureInfo() {
		AboutResource about = dataFlowOperations.aboutOperation().get();
		//assertTrue(about.getFeatureInfo().isGrafanaEnabled()); // true only if the influxdb or prometheus is enabled.
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

		StreamDefinition streamDefinition = Stream.builder(dataFlowOperations)
				.name("transform-test")
				.definition("http | transform --expression=payload.toUpperCase() | log")
				.create();

		// DEPLOY
		logger.info("transform-test: DEPLOY");
		try (Stream stream = streamDefinition.deploy(new DeploymentPropertiesBuilder()
				.put(SPRING_CLOUD_DATAFLOW_SKIPPER_PLATFORM_NAME, runtimeApps.getPlatformName())
				.put("app.*.logging.file", "${PID}-test.log")
				.put("app.*.endpoints.logfile.sensitive", "false")
				.put("app.*.management.endpoints.web.exposure.include", "*")
				.put("app.*.spring.cloud.streamapp.security.enabled", "false")
				.build())) {

			assertThat(stream.getStatus(), is(oneOf(DEPLOYING, PARTIAL)));
			Wait.on(stream).withTimeout(Duration.ofMinutes(10)).until(s -> s.getStatus().equals(DEPLOYED));


			Map<String, String> httpApp = runtimeApps.getApplicationInstances(stream.getName(), "http")
					.values().iterator().next();

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpApp);

			String message = "Unique Test message: " + new Random().nextInt();

			logger.info("transform-test: send massage - " + message);
			restTemplate.postForObject(httpAppUrl, message, String.class);

			Wait.on(stream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
					.contains(message.toUpperCase()));

			logger.info("transform-test: message received");
		}
	}

	@Test
	public void streamPartitioning() {

		StreamDefinition streamDefinition = Stream.builder(dataFlowOperations)
				.name("partitioning-test")
				.definition("http | splitter --expression=payload.split(' ') | log")
				.create();

		logger.info("partitioning-test: DEPLOY");
		try (Stream stream = streamDefinition.deploy(new DeploymentPropertiesBuilder()
				.put(SPRING_CLOUD_DATAFLOW_SKIPPER_PLATFORM_NAME, runtimeApps.getPlatformName())
				.put("app.*.logging.file", "${PID}-test.log")
				.put("app.*.endpoints.logfile.sensitive", "false")
				.put("app.*.management.endpoints.web.exposure.include", "*")
				.put("app.*.spring.cloud.streamapp.security.enabled", "false")
				// Create 2 log instances with partition key computed from the payload.
				.put("deployer.log.count", "2")
				.put("app.splitter.producer.partitionKeyExpression", "payload")
				.put("app.log.spring.cloud.stream.kafka.bindings.input.consumer.autoRebalanceEnabled", "false")
				.put("app.log.logging.pattern.level", "WOODCHUCK-${INSTANCE_INDEX:${CF_INSTANCE_INDEX:${spring.cloud.stream.instanceIndex:0}}} %5p")
				.build())) {

			assertThat(stream.getStatus(), is(oneOf(DEPLOYING, PARTIAL)));
			Wait.on(stream).withTimeout(Duration.ofMinutes(10)).until(s -> s.getStatus().equals(DEPLOYED));

			logger.info("partitioning-test:" + stream.getStatus());

			Map<String, String> httpApp = runtimeApps.getApplicationInstances(stream.getName(), "http")
					.values().iterator().next();

			String httpAppUrl = runtimeApps.getApplicationInstanceUrl(httpApp);

			String message = "How much wood would a woodchuck chuck if a woodchuck could chuck wood";
			restTemplate.postForObject(httpAppUrl, message, String.class);

			logger.info("partitioning-test: send - " + message);

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

			logger.info("partitioning-test: message partitions received by both log apps");
		}
	}

	private boolean allMatch(String inputStr, String... items) {
		return Arrays.stream(items).allMatch(inputStr::contains);
	}

	@Test
	public void streamLifecycle() {
		// CREATE
		StreamDefinition streamDefinition = Stream.builder(dataFlowOperations)
				.name("lifecycle-test")
				.definition("time | log --log.expression='TICKTOCK - TIMESTAMP: '.concat(payload)")
				.create();

		// DEPLOY
		logger.info("lifecycle-test: DEPLOY");
		try (Stream stream = streamDefinition.deploy(new DeploymentPropertiesBuilder()
				.put(SPRING_CLOUD_DATAFLOW_SKIPPER_PLATFORM_NAME, runtimeApps.getPlatformName())
				.put("app.*.logging.file", "${PID}-test.log")
				.put("app.*.endpoints.logfile.sensitive", "false")
				.put("app.*.management.endpoints.web.exposure.include", "*")
				.put("app.*.spring.cloud.streamapp.security.enabled", "false")
				.build())) {

			assertThat(stream.getStatus(), is(oneOf(DEPLOYING, PARTIAL)));
			Wait.on(stream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));

			logger.info("lifecycle-test: " + stream.getStatus());

			Wait.on(stream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
					.contains("TICKTOCK - TIMESTAMP:"));

			assertThat(stream.history().size(), is(1));
			assertThat(stream.history().get(1), is(DEPLOYED));

			// UPDATE
			logger.info("lifecycle-test: UPDATE");
			stream.update(new DeploymentPropertiesBuilder()
					.put("app.log.log.expression", "'Updated TICKTOCK - TIMESTAMP: '.concat(payload)")
					// TODO investigate why on update the app-starters-core overrides the original web.exposure.include!!!
					.put("app.*.management.endpoints.web.exposure.include", "*")
					.build());

			Wait.on(stream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals(DEPLOYED));
			assertThat(stream.getStatus(), is(DEPLOYED));

			Wait.on(stream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
					.contains("Updated TICKTOCK - TIMESTAMP:"));

			logger.info("lifecycle-test: updated");

			assertThat(stream.history().size(), is(2));
			assertThat(stream.history().get(1), is(DELETED));
			assertThat(stream.history().get(2), is(DEPLOYED));

			// ROLLBACK
			logger.info("lifecycle-test: ROLLBACK");
			stream.rollback(0);

			Wait.on(stream).until(s -> s.getStatus().equals(DEPLOYED));
			assertThat(stream.getStatus(), is(DEPLOYED));

			Wait.on(stream).until(s -> runtimeApps.getFirstInstanceLog(s.getName(), "log")
					.contains("TICKTOCK - TIMESTAMP:"));

			logger.info("lifecycle-test: rolled back");
			assertThat(stream.history().size(), is(3));
			assertThat(stream.history().get(1), is(DELETED));
			assertThat(stream.history().get(2), is(DELETED));
			assertThat(stream.history().get(3), is(DEPLOYED));

			// UNDEPLOY
			logger.info("lifecycle-test: UNDEPLOY");
			stream.undeploy();

			Wait.on(stream).until(s -> s.getStatus().equals(UNDEPLOYED));
			assertThat(stream.getStatus(), is(UNDEPLOYED));

			logger.info("lifecycle-test: undeployed");

			assertThat(stream.history().size(), is(3));
			assertThat(stream.history().get(1), is(DELETED));
			assertThat(stream.history().get(2), is(DELETED));
			assertThat(stream.history().get(3), is(DELETED));

			assertThat(dataFlowOperations.streamOperations().list().getMetadata().getTotalElements(), is(1L));
			// DESTROY
		}
		assertThat(dataFlowOperations.streamOperations().list().getMetadata().getTotalElements(), is(0L));
	}

	// -----------------------------------------------------------------------
	//                               TASK TESTS
	// -----------------------------------------------------------------------
	@Test
	public void timestampTask() {
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
}
