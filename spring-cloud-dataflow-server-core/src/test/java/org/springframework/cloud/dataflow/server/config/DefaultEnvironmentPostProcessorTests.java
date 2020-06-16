/*
 * Copyright 2015-2019 the original author or authors.
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

import org.junit.Test;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author Josh Long
 */
public class DefaultEnvironmentPostProcessorTests {

	private static final String MANAGEMENT_CONTEXT_PATH = "management.contextPath";

	private static final String CONTRIBUTED_PATH = "/bar";

	@Test
	public void testDefaultsBeingContributedByServerModule() throws Exception {
		try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultApp.class, "--server.port=0",
				"--spring.main.allow-bean-definition-overriding=true",
				"--spring.autoconfigure.exclude=org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration,org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration")) {
			String cp = ctx.getEnvironment().getProperty(MANAGEMENT_CONTEXT_PATH);
			assertEquals(CONTRIBUTED_PATH, cp);
		}
	}

	@Test
	public void testOverridingDefaultsWithAConfigFile() {
		try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultApp.class,
				"--spring.config.name=test", "--server.port=0",
				"--spring.main.allow-bean-definition-overriding=true",
				"--spring.cloud.dataflow.server.profileapplicationlistener.ignore=true",
				"--spring.autoconfigure.exclude=org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration,org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration")) {
			String cp = ctx.getEnvironment().getProperty(MANAGEMENT_CONTEXT_PATH);
			assertEquals(cp, "/foo");
			assertNotNull(ctx.getEnvironment().getProperty("spring.flyway.locations[0]"));
		}
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
