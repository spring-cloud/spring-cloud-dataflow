/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.cloud.dataflow.autoconfigure.local;

import io.pivotal.reactor.scheduler.ReactorSchedulerClient;
import org.cloudfoundry.operations.CloudFoundryOperations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExecutionQueryDao;
import org.springframework.cloud.dataflow.server.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.server.task.TaskDeploymentReader;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryTaskLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.Mockito.mock;

/**
 * @author Christian Tzolov
 * @author Corneil du Plessis
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = AbstractSchedulerPerPlatformTest.AutoConfigurationApplication.class)
@DirtiesContext
public abstract class AbstractSchedulerPerPlatformTest {

	@Autowired
	protected ApplicationContext context;

	@Configuration
	@EnableAutoConfiguration(exclude = {LocalDataFlowServerAutoConfiguration.class,
			CloudFoundryDeployerAutoConfiguration.class, SecurityAutoConfiguration.class,
			SecurityFilterAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
	public static class AutoConfigurationApplication {
		@Bean
		public AppRegistryService appRegistryService() {
			return mock(AppRegistryService.class);
		}

		@Bean
		public TaskDefinitionReader taskDefinitionReader() {
			return mock(TaskDefinitionReader.class);
		}

		@Bean
		public TaskDeploymentReader taskDeploymentReader() {
			return mock(TaskDeploymentReader.class);
		}

		@Bean
		DataflowTaskExecutionQueryDao dataflowTaskExecutionQueryDao() {
			return mock(DataflowTaskExecutionQueryDao.class);
		}

		@Configuration
		@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
		public static class CloudFoundryMockConfig {
			@MockBean
			protected CloudFoundryDeploymentProperties cloudFoundryDeploymentProperties;

			@Bean
			@Primary
			public ReactorSchedulerClient reactorSchedulerClient() {
				return mock(ReactorSchedulerClient.class);
			}

			@Bean
			@Primary
			public CloudFoundryOperations cloudFoundryOperations() {
				return mock(CloudFoundryOperations.class);
			}

			@Bean
			@Primary
			public CloudFoundryConnectionProperties cloudFoundryConnectionProperties() {
				return mock(CloudFoundryConnectionProperties.class);
			}

			@Bean
			@Primary
			public CloudFoundryTaskLauncher CloudFoundryTaskLauncher() {
				return mock(CloudFoundryTaskLauncher.class);
			}
		}
	}
}
