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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.jayway.jsonpath.JsonPath;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.integration.test.util.RuntimeApplicationHelper;
import org.springframework.cloud.dataflow.integration.test.util.Wait;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.dsl.DeploymentPropertiesBuilder;
import org.springframework.cloud.dataflow.rest.client.dsl.Stream;
import org.springframework.cloud.dataflow.rest.client.dsl.StreamApplication;
import org.springframework.cloud.dataflow.rest.client.dsl.StreamDefinition;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.Assert.assertThat;

/**
 *
 * @author Christian Tzolov
 */
public class DockerComposeTestStream {

	private static final Logger logger = LoggerFactory.getLogger(DockerComposeTestStream.class);

	// Stream lifecycle states
	private static final String DEPLOYED = "deployed";
	private static final String DELETED = "deleted";
	private static final String UNDEPLOYED = "undeployed";
	private static final String DEPLOYING = "deploying";
	private static final String PARTIAL = "partial";

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

	private static DataFlowTemplate dataFlowOperations;
	private static RuntimeApplicationHelper runtimeApps;
	private static RestTemplate restTemplate;

	@BeforeClass
	public static void beforeClass() {
		dataFlowOperations = new DataFlowTemplate(URI.create("http://localhost:9393"));
		logger.info("Configured platforms: " + dataFlowOperations.streamOperations().listPlatforms().stream()
				.map(d -> String.format("[%s:%s]", d.getName(), d.getType())).collect(Collectors.joining()));
		runtimeApps = new RuntimeApplicationHelper(dataFlowOperations, TEST_PLATFORM_NAME);
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
	public void streamScaling() {
		logger.info("stream-scaling-test");
		try (Stream stream = Stream.builder(dataFlowOperations)
				.name("stream-scaling-test")
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

	private boolean isPrometheusPresent() {
		return runtimeApps.isServicePresent("http://localhost:9090/api/v1/query?query=up");
	}

	private boolean isInfluxPresent() {
		return runtimeApps.isServicePresent("http://localhost:8086/ping");
	}
}
