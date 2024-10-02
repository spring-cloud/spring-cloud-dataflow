/*
 * Copyright 2019-2022 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.Metadata;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.organizations.ListOrganizationsResponse;
import org.cloudfoundry.client.v2.organizations.OrganizationResource;
import org.cloudfoundry.client.v2.organizations.Organizations;
import org.cloudfoundry.client.v2.spaces.ListSpacesResponse;
import org.cloudfoundry.client.v2.spaces.SpaceResource;
import org.cloudfoundry.client.v2.spaces.Spaces;
import org.cloudfoundry.logcache.v1.LogCacheClient;
import org.cloudfoundry.reactor.TokenProvider;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.server.config.cloudfoundry.CloudFoundryPlatformClientProvider;
import org.springframework.cloud.dataflow.server.config.cloudfoundry.CloudFoundryPlatformTokenProvider;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryTaskLauncher;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesTaskLauncher;
import org.springframework.cloud.deployer.spi.local.LocalTaskLauncher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David Turanski
 * @author Corneil du Plessis
 **/
@SpringBootTest(
		classes = {DataFlowServerApplication.class, MultiplePlatformTypeTests.TestConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"spring.cloud.dataflow.features.schedules-enabled=true",
				"spring.cloud.dataflow.task.platform.kubernetes.accounts[k8s].namespace=default",
				"spring.cloud.dataflow.task.platform.cloudfoundry.accounts[cf].connection.url=https://localhost",
				"spring.cloud.dataflow.task.platform.cloudfoundry.accounts[cf].connection.org=org",
				"spring.cloud.dataflow.task.platform.cloudfoundry.accounts[cf].connection.space=space",
				"spring.cloud.dataflow.task.platform.cloudfoundry.accounts[cf].deployment.scheduler-url=https://localhost"
		})
class MultiplePlatformTypeTests {

	@Autowired
	List<TaskPlatform> taskPlatforms;

	@Test
	void localTaskPlatform() {
		assertThat(taskPlatforms).hasSize(3);

		TaskPlatform localDefault = taskPlatforms.stream()
				.filter(taskPlatform -> taskPlatform.getName().equals("Local")).findFirst().get();

		assertThat(localDefault.isPrimary()).isTrue();
		assertThat(localDefault).isNotNull();
		assertThat(localDefault.getLaunchers()).hasSize(1);
		assertThat(localDefault.getLaunchers().get(0).getType()).isEqualTo(localDefault.getName());
		assertThat(localDefault.getLaunchers().get(0).getName()).isEqualTo("default");
		assertThat(localDefault.getLaunchers().get(0).getTaskLauncher()).isInstanceOf(LocalTaskLauncher.class);
	}

	@Test
	void cloudFoundryTaskPlatform() {
		TaskPlatform cloudFoundry = taskPlatforms.stream()
				.filter(taskPlatform -> taskPlatform.getName().equals("Cloud Foundry")).findFirst().get();

		assertThat(cloudFoundry).isNotNull();
		assertThat(cloudFoundry.getLaunchers()).hasSize(1);
		assertThat(cloudFoundry.getLaunchers().get(0).getType()).isEqualTo(cloudFoundry.getName());
		assertThat(cloudFoundry.getLaunchers().get(0).getName()).isEqualTo("cf");
		assertThat(cloudFoundry.getLaunchers().get(0).getTaskLauncher()).isInstanceOf(
				CloudFoundryTaskLauncher.class);
	}

	@Test
	void kubernetesTaskPlatform() {
		TaskPlatform kubernetes = taskPlatforms.stream()
				.filter(taskPlatform -> taskPlatform.getName().equals("Kubernetes")).findFirst().get();

		assertThat(kubernetes).isNotNull();
		assertThat(kubernetes.getLaunchers()).hasSize(1);
		assertThat(kubernetes.getLaunchers().get(0).getType()).isEqualTo(kubernetes.getName());
		assertThat(kubernetes.getLaunchers().get(0).getName()).isEqualTo("k8s");
		assertThat(kubernetes.getLaunchers().get(0).getTaskLauncher()).isInstanceOf(KubernetesTaskLauncher.class);
	}

	@Configuration
	static class TestConfig {
		private final CloudFoundryClient cloudFoundryClient = mock(CloudFoundryClient.class);

		private final LogCacheClient logCacheClient = mock(LogCacheClient.class);

		private final TokenProvider tokenProvider = mock(TokenProvider.class);

		@Bean
		@Primary
		public CloudFoundryPlatformClientProvider mockCloudFoundryClientProvider() {
			when(cloudFoundryClient.info())
					.thenReturn(getInfoRequest -> Mono.just(GetInfoResponse.builder().apiVersion("0.0.0").build()));
			when(cloudFoundryClient.organizations()).thenReturn(mock(Organizations.class));
			when(cloudFoundryClient.spaces()).thenReturn(mock(Spaces.class));
			when(cloudFoundryClient.organizations().list(any())).thenReturn(listOrganizationsResponse());
			when(cloudFoundryClient.spaces().list(any())).thenReturn(listSpacesResponse());
			CloudFoundryPlatformClientProvider cloudFoundryClientProvider = mock(
					CloudFoundryPlatformClientProvider.class);
			when(cloudFoundryClientProvider.cloudFoundryClient(anyString())).thenAnswer((__) -> this.cloudFoundryClient);
			when(cloudFoundryClientProvider.logCacheClient(anyString())).thenAnswer((__) -> this.logCacheClient);
			return cloudFoundryClientProvider;
		}

		private Mono<ListOrganizationsResponse> listOrganizationsResponse() {
			ListOrganizationsResponse response = ListOrganizationsResponse.builder()
					.addAllResources(Collections.singletonList(
							OrganizationResource.builder()
									.metadata(Metadata.builder().id("123").build()).build())
					).build();
			return Mono.just(response);
		}

		private Mono<ListSpacesResponse> listSpacesResponse() {
			ListSpacesResponse response = ListSpacesResponse.builder()
					.addAllResources(Collections.singletonList(
							SpaceResource.builder()
									.metadata(Metadata.builder().id("123").build()).build())
					).build();
			return Mono.just(response);
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
