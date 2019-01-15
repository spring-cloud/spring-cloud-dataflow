/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.autoconfigure.local;

import io.pivotal.reactor.scheduler.ReactorSchedulerClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundry2630AndLaterTaskLauncher;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration;
import org.springframework.cloud.scheduler.spi.cloudfoundry.CloudFoundrySchedulerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Christian Tzolov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = AbstractSchedulerPerPlatformTest.AutoConfigurationApplication.class)
@DirtiesContext
public abstract class AbstractSchedulerPerPlatformTest {

	@Autowired
	protected ApplicationContext context;

	@Configuration
	@EnableAutoConfiguration(exclude = { LocalDataFlowServerAutoConfiguration.class,
			CloudFoundryDeployerAutoConfiguration.class })
	public static class AutoConfigurationApplication {


		@Configuration
		@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
		public static class CloudFoundryMockConfig {
			@MockBean
			protected CloudFoundrySchedulerProperties cloudFoundrySchedulerProperties;

			@Bean
			@Primary
			public ReactorSchedulerClient reactorSchedulerClient() {
				return Mockito.mock(ReactorSchedulerClient.class);
			}

			@Bean
			@Primary
			public CloudFoundryOperations cloudFoundryOperations() {
				return Mockito.mock(CloudFoundryOperations.class);
			}

			@Bean
			@Primary
			public CloudFoundryConnectionProperties cloudFoundryConnectionProperties() {
				return Mockito.mock(CloudFoundryConnectionProperties.class);
			}

			@Bean
			@Primary
			public CloudFoundry2630AndLaterTaskLauncher cloudFoundry2630AndLaterTaskLauncher() {
				return Mockito.mock(CloudFoundry2630AndLaterTaskLauncher.class);
			}
		}
	}
}
