/*
 * Copyright 2016-2023 the original author or authors.
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

import java.net.ConnectException;

import org.h2.tools.Server;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.aggregate.task.impl.DefaultTaskRepositoryContainer;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryService;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.aggregate.task.TaskRepositoryContainer;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.dataflow.server.config.features.SchedulerConfiguration;
import org.springframework.cloud.dataflow.server.service.StreamValidationService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.impl.ComposedTaskRunnerConfigurationProperties;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionService;
import org.springframework.cloud.deployer.autoconfigure.ResourceLoadingAutoConfiguration;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.security.authentication.AuthenticationManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * @author Michael Wirth
 */
public class DataFlowServerConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withAllowBeanDefinitionOverriding(true)
			.withUserConfiguration(
					DataFlowServerConfigurationTests.TestConfiguration.class,
					SecurityAutoConfiguration.class,
					DataFlowServerAutoConfiguration.class,
					DataFlowControllerAutoConfiguration.class,
					DataSourceAutoConfiguration.class,
					DataFlowServerConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class,
					RestTemplateAutoConfiguration.class,
					HibernateJpaAutoConfiguration.class,
					SchedulerConfiguration.class,
					JacksonAutoConfiguration.class,
					ResourceLoadingAutoConfiguration.class,
					ComposedTaskRunnerConfigurationProperties.class
			);

	/**
	 * Verify that embedded server starts if h2 url is specified with default properties.
	 */
	@Test
	public void testStartEmbeddedH2Server() {
		contextRunner.withPropertyValues(
						"spring.datasource.url=jdbc:h2:tcp://localhost:19092/mem:dataflow;DATABASE_TO_UPPER=FALSE",
						"spring.dataflow.embedded.database.enabled=true")
				.run(context -> {
					assertTrue(context.containsBean("h2TcpServer"));
					Server server = context.getBean("h2TcpServer", Server.class);
					assertTrue(server.isRunning(false));

					// Verify H2 Service is stopped
					context.close();
					assertFalse(server.isRunning(false));
				});
	}

	/**
	 * Verify that embedded h2 does not start if h2 url is specified with the
	 * spring.dataflow.embedded.database.enabled is set to false.
	 */
	@Test
	public void testDoNotStartEmbeddedH2Server() {
		contextRunner.withPropertyValues(
						"spring.datasource.url=jdbc:h2:tcp://localhost:19092/mem:dataflow;DATABASE_TO_UPPER=FALSE",
						"spring.dataflow.embedded.database.enabled=false",
						"spring.jpa.database=H2"
				)
				.run(context -> {
					assertNotNull(context.getStartupFailure());
					assertInstanceOf(BeanCreationException.class, context.getStartupFailure());
					assertInstanceOf(ConnectException.class, NestedExceptionUtils.getRootCause(context.getStartupFailure()));
				});
	}

	@EnableDataFlowServer
	@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
	static class TestConfiguration {

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
		public Scheduler scheduler() {
			return mock(Scheduler.class);
		}

		@Bean
		public StreamValidationService streamValidationService() {
			return mock(StreamValidationService.class);
		}

		@Bean
		public OAuth2TokenUtilsService oauth2TokenUtilsService() {
			return mock(OAuth2TokenUtilsService.class);
		}

		@Bean
		public StreamDefinitionService streamDefinitionService() {
			return mock(StreamDefinitionService.class);
		}

		@Bean
		public ContainerRegistryService containerRegistryService() {
			return mock(ContainerRegistryService.class);
		}
	}
}
