/*
 * Copyright 2016-2024 the original author or authors.
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

import javax.sql.DataSource;

import org.h2.tools.Server;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryService;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
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
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
/**
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * @author Michael Wirth
 * @author Corneil du Plessis
 */
class DataFlowServerConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withAllowBeanDefinitionOverriding(true)
			.withUserConfiguration(
					DataFlowServerConfigurationTests.TestConfiguration.class,
					TransactionManagerCustomizationAutoConfiguration.class,
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
	void startEmbeddedH2Server() {
		contextRunner.withPropertyValues(
						"spring.datasource.url=jdbc:h2:tcp://localhost:19092/mem:dataflow;DATABASE_TO_UPPER=FALSE",
						"spring.dataflow.embedded.database.enabled=true")
				.run(context -> {
					assertThat(context.containsBean("h2TcpServer")).isTrue();
					Server server = context.getBean("h2TcpServer", Server.class);
					assertThat(server.isRunning(false)).isTrue();

					// Verify H2 Service is stopped
					context.close();
					assertThat(server.isRunning(false)).isFalse();
				});
	}

	/**
	 * Verify that embedded h2 does not start if h2 url is specified with the
	 * spring.dataflow.embedded.database.enabled is set to false.
	 */
	@Test
	void doNotStartEmbeddedH2Server() {
		contextRunner.withPropertyValues(
						"spring.datasource.url=jdbc:h2:tcp://localhost:19092/mem:dataflow;DATABASE_TO_UPPER=FALSE",
						"spring.dataflow.embedded.database.enabled=false",
						"spring.jpa.database=H2"
				)
				.run(context -> {
					assertThat(context.getStartupFailure()).isNotNull();
					assertThat(context.getStartupFailure()).isInstanceOf(BeanCreationException.class);
					assertThat(NestedExceptionUtils.getRootCause(context.getStartupFailure())).isInstanceOf(ConnectException.class);
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
		public TaskRepository taskRepository() {
			return mock(SimpleTaskRepository.class);
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

		@Bean
		public JobExplorer jobExplorer(DataSource dataSource, PlatformTransactionManager platformTransactionManager)
			throws Exception {
			JobExplorerFactoryBean factoryBean = new JobExplorerFactoryBean();
			factoryBean.setDataSource(dataSource);
			factoryBean.setTransactionManager(platformTransactionManager);
			try {
				factoryBean.afterPropertiesSet();
			} catch (Throwable x) {
				throw new RuntimeException("Exception creating JobExplorer", x);
			}
			return factoryBean.getObject();
		}

		@Bean
		public JobRepository jobRepository(DataSource dataSource,
										   PlatformTransactionManager platformTransactionManager) throws Exception {
			JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
			factoryBean.setDataSource(dataSource);
			factoryBean.setTransactionManager(platformTransactionManager);

			try {
				factoryBean.afterPropertiesSet();
			} catch (Throwable x) {
				throw new RuntimeException("Exception creating JobRepository", x);
			}
			return factoryBean.getObject();
		}
	}
}
