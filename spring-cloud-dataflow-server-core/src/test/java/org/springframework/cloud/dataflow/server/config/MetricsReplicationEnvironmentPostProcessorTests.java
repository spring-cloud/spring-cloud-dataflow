/*
 * Copyright 2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Christian Tzolov
 */
public class MetricsReplicationEnvironmentPostProcessorTests {

	private static final List<String> testArgs = Arrays.asList("--server.port=0",
			"--spring.main.allow-bean-definition-overriding=true",
			"--spring.autoconfigure.exclude=" +
					"org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration," +
					"org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration");

	private static final String[] COMMON_APPLICATION_PREFIXES = new String[] {
			MetricsReplicationEnvironmentPostProcessor.COMMON_APPLICATION_PREFIX + ".stream.",
			MetricsReplicationEnvironmentPostProcessor.COMMON_APPLICATION_PREFIX + ".task." };

	@Test
	public void monitoringDashboardWavefront() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.wavefront.enabled=true",
				"--management.metrics.export.wavefront.api-token=654-token",
				"--management.metrics.export.wavefront.uri=https://vmware.wavefront.com",
				"--management.metrics.export.wavefront.source=my-source")) {

			assertThat(ctx.getEnvironment().getProperty(MetricsReplicationEnvironmentPostProcessor.MONITORING_DASHBOARD_PREFIX + ".type")).isEqualTo("WAVEFRONT");
			assertThat(ctx.getEnvironment().getProperty(MetricsReplicationEnvironmentPostProcessor.MONITORING_DASHBOARD_PREFIX + ".url")).isEqualTo("https://vmware.wavefront.com");
			assertThat(ctx.getEnvironment().getProperty(MetricsReplicationEnvironmentPostProcessor.MONITORING_DASHBOARD_PREFIX + ".wavefront.source")).isEqualTo("my-source");
		}
	}

	@Test
	public void monitoringDashboardInfluxGrafana() {
		try (ConfigurableApplicationContext ctx = applicationContext("--management.metrics.export.influx.enabled=true")) {
			assertThat(ctx.getEnvironment().getProperty(MetricsReplicationEnvironmentPostProcessor.MONITORING_DASHBOARD_PREFIX + ".type")).isEqualTo("GRAFANA");
		}
	}

	@Test
	public void monitoringDashboardPrometheusGrafana() {
		try (ConfigurableApplicationContext ctx = applicationContext("--management.metrics.export.prometheus.enabled=true")) {
			assertThat(ctx.getEnvironment().getProperty(MetricsReplicationEnvironmentPostProcessor.MONITORING_DASHBOARD_PREFIX + ".type")).isEqualTo("GRAFANA");
		}
	}

	@Test
	public void monitoringDashboardExplicitProperties() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.wavefront.enabled=true",
				"--management.metrics.export.wavefront.api-token=654-token",
				"--management.metrics.export.wavefront.uri=https://vmware.wavefront.com",
				"--management.metrics.export.wavefront.source=my-source",

				// The explicit monitoring dashboard properties have precedence over the infered from the metrics.
				"--" + MetricsReplicationEnvironmentPostProcessor.MONITORING_DASHBOARD_PREFIX + ".url=http://dashboard",
				"--" + MetricsReplicationEnvironmentPostProcessor.MONITORING_DASHBOARD_PREFIX + ".wavefront.source=different-source")) {

			assertThat(ctx.getEnvironment().getProperty(MetricsReplicationEnvironmentPostProcessor.MONITORING_DASHBOARD_PREFIX + ".type")).isEqualTo("WAVEFRONT");
			assertThat(ctx.getEnvironment().getProperty(MetricsReplicationEnvironmentPostProcessor.MONITORING_DASHBOARD_PREFIX + ".url")).isEqualTo("http://dashboard");
			assertThat(ctx.getEnvironment().getProperty(MetricsReplicationEnvironmentPostProcessor.MONITORING_DASHBOARD_PREFIX + ".wavefront.source")).isEqualTo("different-source");
		}
	}

	@Test
	public void wavefrontPropertiesReplication() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.wavefront.enabled=true",
				"--management.metrics.export.wavefront.api-token=654-token",
				"--management.metrics.export.wavefront.uri=https://vmware.wavefront.com",
				"--management.metrics.export.wavefront.source=my-source",
				// Inherited property from parent PushRegistryProperties
				"--management.metrics.export.wavefront.batch-size=20000")) {

			for (String applicationPrefix : COMMON_APPLICATION_PREFIXES) {

				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.enabled", Boolean.class)).isTrue();
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.api-token")).isEqualTo("654-token");
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.uri")).isEqualTo("https://vmware.wavefront.com");
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.source")).isEqualTo("my-source");
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.batch-size")).isEqualTo("20000");
			}
		}
	}

	@Test
	public void wavefrontPropertiesReplicationWithPlaceholders() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.wavefront.enabled=true",
				"--management.metrics.export.wavefront.api-token=${wavefront-api-secret}",
				"--management.metrics.export.wavefront.uri=https://vmware.wavefront.com",
				"--management.metrics.export.wavefront.source=my-source",
				// Inherited property from parent PushRegistryProperties
				"--management.metrics.export.wavefront.batch-size=20000")) {

			for (String applicationPrefix : COMMON_APPLICATION_PREFIXES) {

				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.enabled", Boolean.class)).isTrue();
				ctx.getEnvironment().setIgnoreUnresolvableNestedPlaceholders(true);
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.api-token")).isEqualTo("${wavefront-api-secret}");
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.uri")).isEqualTo("https://vmware.wavefront.com");
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.source")).isEqualTo("my-source");
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.batch-size")).isEqualTo("20000");
			}
		}
	}

	@Test
	public void disabledPropertiesReplication() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--spring.cloud.dataflow.metrics.property-replication=false",
				"--management.metrics.export.wavefront.enabled=true",
				"--management.metrics.export.wavefront.api-token=654-token",
				"--management.metrics.export.wavefront.uri=https://vmware.wavefront.com",
				"--management.metrics.export.wavefront.source=my-source",
				// Inherited property from parent PushRegistryProperties
				"--management.metrics.export.wavefront.batch-size=20000")) {

			for (String applicationPrefix : COMMON_APPLICATION_PREFIXES) {

				assertThat(ctx.getEnvironment().containsProperty(
						applicationPrefix + "management.metrics.export.wavefront.enabled")).isFalse();
				assertThat(ctx.getEnvironment().containsProperty(
						applicationPrefix + "management.metrics.export.wavefront.api-token")).isFalse();
				assertThat(ctx.getEnvironment().containsProperty(
						applicationPrefix + "management.metrics.export.wavefront.uri")).isFalse();
				assertThat(ctx.getEnvironment().containsProperty(
						applicationPrefix + "management.metrics.export.wavefront.source")).isFalse();
				assertThat(ctx.getEnvironment().containsProperty(
						applicationPrefix + "management.metrics.export.wavefront.batch-size")).isFalse();
			}
		}
	}

	@Test
	public void dontReplicateExplicitlySetStreamOrTaskProperties() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.wavefront.enabled=true",
				"--" + MetricsReplicationEnvironmentPostProcessor.COMMON_APPLICATION_PREFIX + ".stream." + "management.metrics.export.wavefront.uri=https://StreamUri",
				"--" + MetricsReplicationEnvironmentPostProcessor.COMMON_APPLICATION_PREFIX + ".task." + "management.metrics.export.wavefront.uri=https://TaskUri",
				"--management.metrics.export.wavefront.api-token=654-token",
				"--management.metrics.export.wavefront.uri=https://vmware.wavefront.com",
				"--management.metrics.export.wavefront.source=my-source",
				// Inherited property from parent PushRegistryProperties
				"--management.metrics.export.wavefront.batch-size=20000")) {

			for (String applicationPrefix : COMMON_APPLICATION_PREFIXES) {

				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.enabled", Boolean.class)).isTrue();
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.api-token")).isEqualTo("654-token");
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.source")).isEqualTo("my-source");
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.wavefront.batch-size")).isEqualTo("20000");
			}

			assertThat(ctx.getEnvironment().getProperty(
					MetricsReplicationEnvironmentPostProcessor.COMMON_APPLICATION_PREFIX + ".stream." + "management.metrics.export.wavefront.uri"))
					.isEqualTo("https://StreamUri");
			assertThat(ctx.getEnvironment().getProperty(
					MetricsReplicationEnvironmentPostProcessor.COMMON_APPLICATION_PREFIX + ".task." + "management.metrics.export.wavefront.uri"))
					.isEqualTo("https://TaskUri");
		}
	}

	@Test
	public void influxPropertiesReplication() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.influx.enabled=true",
				"--management.metrics.export.influx.db=myinfluxdb",
				"--management.metrics.export.influx.uri=http://influxdb:8086",
				// Inherited property from parent PushRegistryProperties
				"--management.metrics.export.influx.batch-size=20000")) {

			for (String applicationPrefix : COMMON_APPLICATION_PREFIXES) {

				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.influx.enabled", Boolean.class)).isTrue();
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.influx.db")).isEqualTo("myinfluxdb");
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.influx.uri")).isEqualTo("http://influxdb:8086");
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.influx.batch-size")).isEqualTo("20000");
			}
		}
	}

	@Test
	public void prometheusPropertiesReplication() {
		try (ConfigurableApplicationContext ctx = applicationContext(
				"--management.metrics.export.prometheus.enabled=true",
				"--management.metrics.export.prometheus.rsocket.enabled=true",
				"--management.metrics.export.prometheus.rsocket.host=prometheus-rsocket-proxy",
				"--management.metrics.export.prometheus.rsocket.port=7001",
				//Inner property class
				"--management.metrics.export.prometheus.pushgateway.enabled=false")) {

			for (String applicationPrefix : COMMON_APPLICATION_PREFIXES) {

				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.prometheus.enabled", Boolean.class)).isTrue();
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.prometheus.rsocket.enabled", Boolean.class)).isTrue();
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.prometheus.rsocket.host")).isEqualTo("prometheus-rsocket-proxy");
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.prometheus.rsocket.port")).isEqualTo("7001");
				assertThat(ctx.getEnvironment().getProperty(
						applicationPrefix + "management.metrics.export.prometheus.pushgateway.enabled", Boolean.class)).isFalse();
			}
		}
	}

	private ConfigurableApplicationContext applicationContext(String... args) {
		List<String> l = new ArrayList<>(Arrays.asList(args));
		l.addAll(testArgs);
		String[] finalArgs = l.toArray(new String[0]);
		return SpringApplication.run(EmptyDefaultApp.class, finalArgs);
	}


	@Configuration
	@Import(TestConfiguration.class)
	@EnableAutoConfiguration(exclude = { SessionAutoConfiguration.class, FlywayAutoConfiguration.class })
	@EnableDataFlowServer
	public static class EmptyDefaultApp {
	}

	private static class TestConfiguration {

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
			return mock(TaskRepository.class);
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
