/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.single;

import java.util.List;

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
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundry2630AndLaterTaskLauncher;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesTaskLauncher;
import org.springframework.cloud.deployer.spi.local.LocalTaskLauncher;
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

@ActiveProfiles("multiplatform")
@SpringBootTest(classes = { DataFlowServerApplication.class,
		MultiplePlatformTypeTests.TestConfig.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class MultiplePlatformTypeTests {

	@Autowired
	List<TaskPlatform> taskPlatforms;

	@Test
	public void localTaskPlatform() {
		assertThat(taskPlatforms).hasSize(3);

		TaskPlatform localDefault =
			taskPlatforms.stream().filter(taskPlatform -> taskPlatform.getName().equals("Local")).findFirst().get();

		assertThat(localDefault).isNotNull();
		assertThat(localDefault.getLaunchers()).hasSize(1);
		assertThat(localDefault.getLaunchers().get(0).getType()).isEqualTo(localDefault.getName());
		assertThat(localDefault.getLaunchers().get(0).getName()).isEqualTo("default");
		assertThat(localDefault.getLaunchers().get(0).getTaskLauncher()).isInstanceOf(LocalTaskLauncher.class);
	}

	@Test
	public void cloudFoundryTaskPlatform() {
		TaskPlatform cloudFoundry =
			taskPlatforms.stream().filter(taskPlatform -> taskPlatform.getName().equals("Cloud Foundry")).findFirst().get();

		assertThat(cloudFoundry).isNotNull();
		assertThat(cloudFoundry.getLaunchers()).hasSize(1);
		assertThat(cloudFoundry.getLaunchers().get(0).getType()).isEqualTo(cloudFoundry.getName());
		assertThat(cloudFoundry.getLaunchers().get(0).getName()).isEqualTo("cf");
		assertThat(cloudFoundry.getLaunchers().get(0).getTaskLauncher()).isInstanceOf(
			CloudFoundry2630AndLaterTaskLauncher.class);
	}

	@Test
	public void kubernetesTaskPlatform() {
		TaskPlatform kubernetes =
			taskPlatforms.stream().filter(taskPlatform -> taskPlatform.getName().equals("Kubernetes")).findFirst().get();

		assertThat(kubernetes).isNotNull();
		assertThat(kubernetes.getLaunchers()).hasSize(1);
		assertThat(kubernetes.getLaunchers().get(0).getType()).isEqualTo(kubernetes.getName());
		assertThat(kubernetes.getLaunchers().get(0).getName()).isEqualTo("k8s");
		assertThat(kubernetes.getLaunchers().get(0).getTaskLauncher()).isInstanceOf(KubernetesTaskLauncher.class);
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
		public CloudFoundryPlatformTokenProvider mockPlatformTokenProvider() {
			CloudFoundryPlatformTokenProvider platformTokenProvider = mock(CloudFoundryPlatformTokenProvider.class);
			when(platformTokenProvider.tokenProvider(anyString())).thenReturn(tokenProvider);
			return platformTokenProvider;
		}
	}

}
