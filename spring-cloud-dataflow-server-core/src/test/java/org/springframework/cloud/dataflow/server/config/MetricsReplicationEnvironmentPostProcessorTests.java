/*
 * Copyright 2020-2023 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.aggregate.task.impl.DefaultTaskRepositoryContainer;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.aggregate.task.TaskRepositoryContainer;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionService;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetricsReplicationEnvironmentPostProcessor}.
 * 
 * @author Christian Tzolov
 * @author Chris Bono
 */
class MetricsReplicationEnvironmentPostProcessorTests {

	private static final String COMMON_STREAM_PROP_PREFIX = "spring.cloud.dataflow.application-properties.stream.";
	private static final String COMMON_TASK_PROP_PREFIX = "spring.cloud.dataflow.application-properties.task.";
	private static final String[] COMMON_APPLICATION_PREFIXES = new String[] { COMMON_STREAM_PROP_PREFIX, COMMON_TASK_PROP_PREFIX };
	
	@Test
	void monitoringDashboardWavefront() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.wavefront.enabled=true",
				"--management.metrics.export.wavefront.api-token=654-token",
				"--management.metrics.export.wavefront.uri=https://vmware.wavefront.com",
				"--management.metrics.export.wavefront.source=my-source")) {
			assertEnvHasProperty(ctx, monitoringDashboardProperty("type"), "WAVEFRONT");
			assertEnvHasProperty(ctx, monitoringDashboardProperty("url"), "https://vmware.wavefront.com");
			assertEnvHasProperty(ctx, monitoringDashboardProperty("wavefront.source"), "my-source");
		}
	}

	@Test
	void monitoringDashboardInfluxGrafana() {
		try (ConfigurableApplicationContext ctx = applicationContext("--management.metrics.export.influx.enabled=true")) {
			assertEnvHasProperty(ctx, monitoringDashboardProperty("type"), "GRAFANA");
		}
	}

	@Test
	void monitoringDashboardPrometheusGrafana() {
		try (ConfigurableApplicationContext ctx = applicationContext("--management.metrics.export.prometheus.enabled=true")) {
			assertEnvHasProperty(ctx, monitoringDashboardProperty("type"), "GRAFANA");
		}
	}

	@Test
	void monitoringDashboardExplicitProperties() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.wavefront.enabled=true",
				"--management.metrics.export.wavefront.api-token=654-token",
				"--management.metrics.export.wavefront.uri=https://vmware.wavefront.com",
				"--management.metrics.export.wavefront.source=my-source",
				// The explicit monitoring dashboard properties have precedence over the inferred from the metrics.
				"--" + monitoringDashboardProperty("url") + "=http://dashboard",
				"--" + monitoringDashboardProperty("wavefront.source") + "=different-source")) {
			assertEnvHasProperty(ctx, monitoringDashboardProperty("type"), "WAVEFRONT");
			assertEnvHasProperty(ctx, monitoringDashboardProperty("url"), "http://dashboard");
			assertEnvHasProperty(ctx, monitoringDashboardProperty("wavefront.source"), "different-source");
		}
	}
	
	private String monitoringDashboardProperty(String propName) {
		return "spring.cloud.dataflow.metrics.dashboard." + propName;
	}
	
	@Test
	void wavefrontPropertiesReplication() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.wavefront.enabled=true",
				"--management.metrics.export.wavefront.api-token=654-token",
				"--management.metrics.export.wavefront.uri=https://vmware.wavefront.com",
				"--management.metrics.export.wavefront.source=my-source",
				// Inherited property from parent PushRegistryProperties
				"--management.metrics.export.wavefront.batch-size=20000")) {
			for (String commonPropPrefix : COMMON_APPLICATION_PREFIXES) {
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.enabled", "true");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.api-token", "654-token");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.uri", "https://vmware.wavefront.com");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.source", "my-source");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.batch-size", "20000");

				// Boot3 properties are replicated as well
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.metrics.export.enabled", "true");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.metrics.export.api-token", "654-token");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.metrics.export.uri", "https://vmware.wavefront.com");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.metrics.export.source", "my-source");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.metrics.export.batch-size", "20000");
			}
		}
	}

	@Test
	void wavefrontPropertiesReplicationWithPlaceholders() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.wavefront.enabled=true",
				"--management.metrics.export.wavefront.api-token=${wavefront-api-secret}",
				"--management.metrics.export.wavefront.uri=https://vmware.wavefront.com",
				"--management.metrics.export.wavefront.source=my-source",
				// Inherited property from parent PushRegistryProperties
				"--management.metrics.export.wavefront.batch-size=20000")) {
			for (String commonPropPrefix : COMMON_APPLICATION_PREFIXES) {
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.enabled", "true");
				ctx.getEnvironment().setIgnoreUnresolvableNestedPlaceholders(true);
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.api-token", "${wavefront-api-secret}");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.uri", "https://vmware.wavefront.com");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.source", "my-source");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.batch-size", "20000");
			}
		}
	}

	@Test
	void disabledPropertiesReplication() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--spring.cloud.dataflow.metrics.property-replication=false",
				"--management.metrics.export.wavefront.enabled=true",
				"--management.metrics.export.wavefront.api-token=654-token",
				"--management.metrics.export.wavefront.uri=https://vmware.wavefront.com",
				"--management.metrics.export.wavefront.source=my-source",
				// Inherited property from parent PushRegistryProperties
				"--management.metrics.export.wavefront.batch-size=20000")) {
			for (String commonPropPrefix : COMMON_APPLICATION_PREFIXES) {
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.enabled");
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.api-token");
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.uri");
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.source");
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.batch-size");

				// Boot3 variants are also not available
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.wavefront.metrics.export.enabled");
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.wavefront.metrics.export.api-token");
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.wavefront.metrics.export.uri");
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.wavefront.metrics.export.source");
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.wavefront.metrics.export.batch-size");
			}
		}
	}
	
	@Test
	void doNotReplicateExplicitlySetStreamOrTaskProperties() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.wavefront.enabled=true",
				"--" + COMMON_STREAM_PROP_PREFIX + "management.metrics.export.wavefront.uri=https://StreamUri",
				"--" + COMMON_TASK_PROP_PREFIX + "management.metrics.export.wavefront.uri=https://TaskUri",
				"--" + COMMON_STREAM_PROP_PREFIX + "management.wavefront.metrics.export.uri=https://StreamUri",
				"--" + COMMON_TASK_PROP_PREFIX + "management.wavefront.metrics.export.uri=https://TaskUri",
				"--management.metrics.export.wavefront.api-token=654-token",
				"--management.metrics.export.wavefront.uri=https://vmware.wavefront.com",
				"--management.metrics.export.wavefront.source=my-source",
				// Inherited property from parent PushRegistryProperties
				"--management.metrics.export.wavefront.batch-size=20000")) {
			for (String commonPropPrefix : COMMON_APPLICATION_PREFIXES) {
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.enabled", "true");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.enabled", "true");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.api-token", "654-token");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.source", "my-source");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.wavefront.batch-size", "20000");
			}
			assertEnvHasProperty(ctx, COMMON_STREAM_PROP_PREFIX + "management.metrics.export.wavefront.uri", "https://StreamUri");
			assertEnvHasProperty(ctx, COMMON_TASK_PROP_PREFIX + "management.metrics.export.wavefront.uri", "https://TaskUri");
			// Boot3 variants are also not overridden
			assertEnvHasProperty(ctx, COMMON_STREAM_PROP_PREFIX + "management.wavefront.metrics.export.uri", "https://StreamUri");
			assertEnvHasProperty(ctx, COMMON_TASK_PROP_PREFIX + "management.wavefront.metrics.export.uri", "https://TaskUri");
		}
	}

	@Test
	void influxPropertiesReplication() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.influx.enabled=true",
				"--management.metrics.export.influx.db=myinfluxdb",
				"--management.metrics.export.influx.uri=http://influxdb:8086",
				// Inherited property
				"--management.metrics.export.influx.batch-size=20000")) {
			for (String commonPropPrefix : COMMON_APPLICATION_PREFIXES) {
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.influx.enabled", "true");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.influx.db", "myinfluxdb");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.influx.uri", "http://influxdb:8086");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.influx.batch-size", "20000");
				// Boot3 variants are replicated
				assertEnvHasProperty(ctx, commonPropPrefix + "management.influx.metrics.export.enabled", "true");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.influx.metrics.export.db", "myinfluxdb");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.influx.metrics.export.uri", "http://influxdb:8086");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.influx.metrics.export.batch-size", "20000");
			}
		}
	}

	@Test
	void prometheusPropertiesReplication() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.prometheus.enabled=true",
				"--management.metrics.export.prometheus.rsocket.enabled=true",
				"--management.metrics.export.prometheus.rsocket.host=prometheus-rsocket-proxy",
				"--management.metrics.export.prometheus.rsocket.port=7001",
				// Inherited property
				"--management.metrics.export.prometheus.pushgateway.enabled=false")) {
			for (String commonPropPrefix : COMMON_APPLICATION_PREFIXES) {
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.prometheus.enabled", "true");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.prometheus.rsocket.enabled", "true");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.prometheus.rsocket.host", "prometheus-rsocket-proxy");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.prometheus.rsocket.port", "7001");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.metrics.export.prometheus.pushgateway.enabled", "false");
				// Boot3 variants are replicated
				assertEnvHasProperty(ctx, commonPropPrefix + "management.prometheus.metrics.export.enabled", "true");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.prometheus.metrics.export.rsocket.enabled", "true");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.prometheus.metrics.export.rsocket.host", "prometheus-rsocket-proxy");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.prometheus.metrics.export.rsocket.port", "7001");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.prometheus.metrics.export.pushgateway.enabled", "false");
			}
		}
	}


	private void assertEnvDoesNotContainProperty(ConfigurableApplicationContext ctx, String property) {
		assertThat(ctx.getEnvironment().containsProperty(property)).isFalse();
	}

	private void assertEnvHasProperty(ConfigurableApplicationContext ctx, String property, Object expectedValue) {
		assertThat(ctx.getEnvironment().getProperty(property)).isEqualTo(expectedValue);
	}
	
	private ConfigurableApplicationContext applicationContext(String... args) {
		String[] commonArgs = {
				"--server.port=0",
				"--spring.main.allow-bean-definition-overriding=true",
				"--spring.autoconfigure.exclude=" +
						"org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration," +
						"org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration"
		};
		String[] allArgs = Stream.of(commonArgs, args)
				.flatMap(Stream::of)
				.toArray(String[]::new);
		return SpringApplication.run(EmptyDefaultApp.class, allArgs);
	}

	@Configuration
	@EnableAutoConfiguration(exclude = { SessionAutoConfiguration.class, FlywayAutoConfiguration.class,
			SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
			ManagementWebSecurityAutoConfiguration.class })
	@EnableDataFlowServer
	public static class EmptyDefaultApp {
		@Bean
		public AppDeployer appDeployer() {
			return mock(AppDeployer.class);
		}

		@Bean
		public TaskLauncher taskLauncher() {
			return mock(TaskLauncher.class);
		}

		@Bean
		public AuthenticationManager authenticationManager() {
			return mock(AuthenticationManager.class);
		}

		@Bean
		public TaskExecutionService taskService() {
			return mock(DefaultTaskExecutionService.class);
		}

		@Bean
		public TaskRepositoryContainer taskRepositoryContainer() {
			return mock(DefaultTaskRepositoryContainer.class);
		}

		@Bean
		public SchedulerService schedulerService() {
			return mock(SchedulerService.class);
		}

		@Bean
		public Scheduler scheduler() {
			return mock(Scheduler.class);
		}

		@Bean
		public OAuth2TokenUtilsService oauth2TokenUtilsService() {
			return mock(OAuth2TokenUtilsService.class);
		}

		@Bean
		public StreamDefinitionService streamDefinitionService() {
			return mock(StreamDefinitionService.class);
		}
	}
}
