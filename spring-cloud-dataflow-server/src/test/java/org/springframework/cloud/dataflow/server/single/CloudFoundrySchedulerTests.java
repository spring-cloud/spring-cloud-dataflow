/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.single;

import java.util.List;

import io.pivotal.scheduler.SchedulerClient;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.reactor.TokenProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.server.config.cloudfoundry.CloudFoundryPlatformClientProvider;
import org.springframework.cloud.dataflow.server.config.cloudfoundry.CloudFoundryPlatformTokenProvider;
import org.springframework.cloud.dataflow.server.config.cloudfoundry.CloudFoundrySchedulerClientProvider;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.scheduler.spi.cloudfoundry.CloudFoundrySchedulerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David Turanski
 **/
@ActiveProfiles("cloud")
@SpringBootTest(
	classes = { DataFlowServerApplication.class, CloudFoundrySchedulerTests.TestConfig.class },
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties = {
	"spring.cloud.dataflow.features.schedules-enabled=true",
	"VCAP_SERVICES=foo",
	"spring.cloud.dataflow.task.platform.cloudfoundry.accounts[cf].connection.url=https://localhost",
	"spring.cloud.dataflow.task.platform.cloudfoundry.accounts[cf].connection.org=org",
	"spring.cloud.dataflow.task.platform.cloudfoundry.accounts[cf].connection.space=space",
	"spring.cloud.scheduler.cloudfoundry.scheduler-url=https://localhost"
	})
@RunWith(SpringRunner.class)
public class CloudFoundrySchedulerTests {

	@Autowired
	List<TaskPlatform> taskPlatforms;

	@Autowired
	SchedulerService schedulerService;

	@Test
	public void schedulerServiceCreated() {
		for (TaskPlatform taskPlatform: taskPlatforms) {
			if (taskPlatform.isPrimary()) {
				assertThat(taskPlatform.getName()).isEqualTo("Cloud Foundry");
			}
		}

		assertThat(schedulerService).isNotNull();
	}

	@Configuration
	static class TestConfig {
		private CloudFoundryClient cloudFoundryClient = mock(CloudFoundryClient.class);

		private TokenProvider tokenProvider = mock(TokenProvider.class);

		@Bean
		@Primary
		public CloudFoundryPlatformClientProvider mockCloudFoundryClientProvider() {
			when(cloudFoundryClient.info())
				.thenReturn(getInfoRequest -> Mono.just(GetInfoResponse.builder().apiVersion("0.0.0").build()));
			CloudFoundryPlatformClientProvider cloudFoundryClientProvider = mock(
				CloudFoundryPlatformClientProvider.class);
			when(cloudFoundryClientProvider.cloudFoundryClient(anyString())).thenAnswer(invocation -> {
				System.out.println("Returning " + cloudFoundryClient);
				return cloudFoundryClient;
			});
			return cloudFoundryClientProvider;
		}

		@Bean
		@Primary
		public CloudFoundrySchedulerClientProvider mockCloudFoundryShedulerClientProvider() {
			CloudFoundrySchedulerClientProvider cloudFoundrySchedulerClientProvider =
				mock(CloudFoundrySchedulerClientProvider.class);
			when(cloudFoundrySchedulerClientProvider.cloudFoundrySchedulerClient(anyString()))
				.thenReturn(mock(SchedulerClient.class));
			when(cloudFoundrySchedulerClientProvider.schedulerProperties()).thenReturn(
				new CloudFoundrySchedulerProperties());
			return cloudFoundrySchedulerClientProvider;
		}

		@Bean
		@Primary
		public CloudFoundryPlatformTokenProvider mockPlatformTokenProvider() {
			CloudFoundryPlatformTokenProvider platformTokenProvider = mock(CloudFoundryPlatformTokenProvider.class);
			when(platformTokenProvider.tokenProvider(anyString())).thenReturn(tokenProvider);
			return platformTokenProvider;
		}
	}

}
