/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskService;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;

/**
 * @author Josh Long
 */
public class DefaultEnvironmentPostProcessorTests {

	private static final String MANAGEMENT_CONTEXT_PATH = "management.contextPath";

	private static final String CONTRIBUTED_PATH = "/bar";

	@Configuration
	@Import(TestConfiguration.class)
	@EnableAutoConfiguration(exclude=SessionAutoConfiguration.class)
	@EnableDataFlowServer
	public static class EmptyDefaultApp {
	}

	@Test
	public void testDefaultsBeingContributedByServerModule() throws Exception {
		try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultApp.class, "--server.port=0")) {
			String cp = ctx.getEnvironment().getProperty(MANAGEMENT_CONTEXT_PATH);
			assertEquals(CONTRIBUTED_PATH, cp);
		}
	}

	@Test
	public void testOverridingDefaultsWithAConfigFile() {
		try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultApp.class,
				"--spring.config.name=test", "--server.port=0")) {
			String cp = ctx.getEnvironment().getProperty(MANAGEMENT_CONTEXT_PATH);
			assertEquals(cp, "/foo");
		}
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
		public TaskService taskService() {
			return mock(DefaultTaskService.class);
		}
	}
}
