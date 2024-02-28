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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionService;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
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
				"--management.wavefront.metrics.export.enabled=true",
				"--management.wavefront.api-token=654-token",
				"--management.wavefront.uri=https://vmware.wavefront.com",
				"--management.wavefront.source=my-source")) {
			assertEnvHasProperty(ctx, monitoringDashboardProperty("type"), "WAVEFRONT");
			assertEnvHasProperty(ctx, monitoringDashboardProperty("url"), "https://vmware.wavefront.com");
			assertEnvHasProperty(ctx, monitoringDashboardProperty("wavefront.source"), "my-source");
		}
	}

	@Test
	void monitoringDashboardInfluxGrafana() {
		try (ConfigurableApplicationContext ctx = applicationContext("--management.influx.metrics.export.enabled=true")) {
			assertEnvHasProperty(ctx, monitoringDashboardProperty("type"), "GRAFANA");
		}
	}

	@Test
	void monitoringDashboardPrometheusGrafana() {
		try (ConfigurableApplicationContext ctx = applicationContext("--management.prometheus.metrics.export.enabled=true")) {
			assertEnvHasProperty(ctx, monitoringDashboardProperty("type"), "GRAFANA");
		}
	}

	@Test
	void monitoringDashboardExplicitProperties() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.wavefront.metrics.export.enabled=true",
				"--management.wavefront.api-token=654-token",
				"--management.wavefront.uri=https://vmware.wavefront.com",
				"--management.wavefront.source=my-source",
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
				"--management.wavefront.metrics.export.enabled=true",
				"--management.wavefront.api-token=654-token",
				"--management.wavefront.api-token-type=WAVEFRONT_API_TOKEN",
				"--management.wavefront.uri=https://vmware.wavefront.com",
				"--management.wavefront.source=my-source",
				"--management.wavefront.application.cluster-name=foo",
				"--management.wavefront.sender.batch-size=20000")) {
			for (String commonPropPrefix : COMMON_APPLICATION_PREFIXES) {
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.metrics.export.enabled", "true");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.api-token", "654-token");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.api-token-type", "WAVEFRONT_API_TOKEN");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.uri", "https://vmware.wavefront.com");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.source", "my-source");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.application.cluster-name", "foo");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.sender.batch-size", "20000");
			}
		}
	}

	@Test
	void wavefrontPropertiesReplicationWithPlaceholders() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.wavefront.metrics.export.enabled=true",
				"--management.wavefront.api-token=${wavefront-api-secret}",
				"--management.wavefront.uri=https://vmware.wavefront.com",
				"--management.wavefront.source=my-source",
				"--management.wavefront.sender.batch-size=20000")) {
			for (String commonPropPrefix : COMMON_APPLICATION_PREFIXES) {
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.metrics.export.enabled", "true");
				ctx.getEnvironment().setIgnoreUnresolvableNestedPlaceholders(true);
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.api-token", "${wavefront-api-secret}");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.uri", "https://vmware.wavefront.com");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.source", "my-source");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.wavefront.sender.batch-size", "20000");
			}
		}
	}

	@Test
	void disabledPropertiesReplication() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--spring.cloud.dataflow.metrics.property-replication=false",
				"--management.wavefront.metrics.export.enabled=true",
				"--management.wavefront.api-token=654-token",
				"--management.wavefront.uri=https://vmware.wavefront.com",
				"--management.wavefront.source=my-source",
				"--management.wavefront.sender.batch-size=20000")) {
			for (String commonPropPrefix : COMMON_APPLICATION_PREFIXES) {
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.wavefront.metrics.export.enabled");
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.wavefront.api-token");
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.wavefront.uri");
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.wavefront.source");
				assertEnvDoesNotContainProperty(ctx, commonPropPrefix + "management.wavefront.sender.batch-size");
			}
		}
	}
	
	@Test
	void doNotReplicateExplicitlySetStreamOrTaskProperties() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.wavefront.metrics.export.enabled=true",
				"--management.wavefront.api-token=654-token",
				"--%smanagement.wavefront.uri=https://StreamUri".formatted(COMMON_STREAM_PROP_PREFIX),
				"--%smanagement.wavefront.uri=https://TaskUri".formatted(COMMON_TASK_PROP_PREFIX),
				"--management.wavefront.uri=https://vmware.wavefront.com")) {
			assertEnvHasProperty(ctx, COMMON_STREAM_PROP_PREFIX + "management.wavefront.uri", "https://StreamUri");
			assertEnvHasProperty(ctx, COMMON_TASK_PROP_PREFIX + "management.wavefront.uri", "https://TaskUri");
		}
	}

	@Test
	void influxPropertiesReplication() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.influx.metrics.export.enabled=true",
				"--management.influx.metrics.export.db=myinfluxdb",
				"--management.influx.metrics.export.uri=http://influxdb:8086",
				"--management.influx.metrics.export.batch-size=20000")) {
			for (String commonPropPrefix : COMMON_APPLICATION_PREFIXES) {
				assertEnvHasProperty(ctx, commonPropPrefix + "management.influx.metrics.export.enabled", "true");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.influx.metrics.export.db", "myinfluxdb");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.influx.metrics.export.uri", "http://influxdb:8086");
				assertEnvHasProperty(ctx, commonPropPrefix + "management.influx.metrics.export.batch-size", "20000");
			}
		}
	}

	@Disabled("Waiting on https://github.com/spring-cloud/spring-cloud-dataflow/issues/5675#issuecomment-1953867317")
	@Test
	void prometheusPropertiesReplication() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.prometheus.metrics.export.enabled=true",
				"--management.prometheus.metrics.export.rsocket.enabled=true",
				"--management.prometheus.metrics.export.rsocket.host=prometheus-rsocket-proxy",
				"--management.prometheus.metrics.export.rsocket.port=7001",
				// Inherited property
				"--management.prometheus.metrics.export.pushgateway.enabled=false")) {
			for (String commonPropPrefix : COMMON_APPLICATION_PREFIXES) {
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
		public TaskRepository taskRepository() {
			return mock(SimpleTaskRepository.class);
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
