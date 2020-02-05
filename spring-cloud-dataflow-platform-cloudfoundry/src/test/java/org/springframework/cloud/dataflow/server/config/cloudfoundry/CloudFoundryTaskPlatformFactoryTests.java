/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config.cloudfoundry;

import java.net.URL;
import java.util.Collections;
import java.util.Optional;

import io.pivotal.scheduler.SchedulerClient;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.Metadata;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.organizations.ListOrganizationsResponse;
import org.cloudfoundry.client.v2.organizations.OrganizationResource;
import org.cloudfoundry.client.v2.organizations.Organizations;
import org.cloudfoundry.client.v2.spaces.ListSpacesResponse;
import org.cloudfoundry.client.v2.spaces.SpaceResource;
import org.cloudfoundry.client.v2.spaces.Spaces;
import org.cloudfoundry.reactor.TokenProvider;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.core.TaskPlatformFactory;
import org.springframework.cloud.dataflow.server.config.cloudfoundry.CloudFoundryPlatformProperties.CloudFoundryProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryTaskLauncher;
import org.springframework.cloud.deployer.spi.scheduler.cloudfoundry.CloudFoundrySchedulerProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David Turanski
 **/
public class CloudFoundryTaskPlatformFactoryTests {

	private CloudFoundryPlatformTokenProvider platformTokenProvider;

	private CloudFoundryPlatformConnectionContextProvider connectionContextProvider;

	private CloudFoundryPlatformClientProvider cloudFoundryClientProvider = mock(
			CloudFoundryPlatformClientProvider.class);

	private CloudFoundrySchedulerClientProvider cloudFoundrySchedulerClientProvider = mock(
			CloudFoundrySchedulerClientProvider.class);

	private CloudFoundryClient cloudFoundryClient = mock(CloudFoundryClient.class);

	private CloudFoundryPlatformProperties cloudFoundryPlatformProperties;

	private CloudFoundryConnectionProperties connectionProperties;

	private CloudFoundryDeploymentProperties deploymentProperties;

	@Before
	public void setUp() throws Exception {
		when(cloudFoundryClient.info())
				.thenReturn(getInfoRequest -> Mono.just(GetInfoResponse.builder().apiVersion("0.0.0").build()));
		when(cloudFoundryClient.organizations()).thenReturn(mock(Organizations.class));
		when(cloudFoundryClient.spaces()).thenReturn(mock(Spaces.class));
		when(cloudFoundryClient.organizations().list(any())).thenReturn(listOrganizationsResponse());
		when(cloudFoundryClient.spaces().list(any())).thenReturn(listSpacesResponse());
		when(cloudFoundryClientProvider.cloudFoundryClient(anyString())).thenReturn(cloudFoundryClient);

		cloudFoundryPlatformProperties = new CloudFoundryPlatformProperties();
		CloudFoundryProperties cloudFoundryProperties = new CloudFoundryProperties();
		connectionProperties = new CloudFoundryConnectionProperties();
		connectionProperties.setOrg("org");
		connectionProperties.setSpace("space");
		connectionProperties.setUrl(new URL("https://localhost:9999"));

		deploymentProperties = new CloudFoundryDeploymentProperties();
		deploymentProperties.setApiTimeout(1L);
		cloudFoundryProperties.setDeployment(new CloudFoundryDeploymentProperties());
		cloudFoundryProperties.setConnection(connectionProperties);
		cloudFoundryPlatformProperties.setAccounts(Collections.singletonMap("default", cloudFoundryProperties));

		connectionContextProvider = new CloudFoundryPlatformConnectionContextProvider(cloudFoundryPlatformProperties);
		platformTokenProvider = mock(CloudFoundryPlatformTokenProvider.class);
		when(platformTokenProvider.tokenProvider(anyString())).thenReturn(mock(TokenProvider.class));
	}

	@Test
	public void cloudFoundryTaskPlatformNoScheduler() {

		TaskPlatformFactory taskPlatformFactory = CloudFoundryTaskPlatformFactory
				.builder()
				.platformProperties(cloudFoundryPlatformProperties)
				.platformTokenProvider(platformTokenProvider)
				.connectionContextProvider(connectionContextProvider)
				.cloudFoundryClientProvider(cloudFoundryClientProvider)
				.build();

		TaskPlatform taskPlatform = taskPlatformFactory.createTaskPlatform();
		assertThat(taskPlatform.getName()).isEqualTo("Cloud Foundry");
		assertThat(taskPlatform.getLaunchers()).hasSize(1);
		assertThat(taskPlatform.getLaunchers().get(0).getType()).isEqualTo("Cloud Foundry");
		assertThat(taskPlatform.getLaunchers().get(0).getName()).isEqualTo("default");
		assertThat(taskPlatform.getLaunchers().get(0).getTaskLauncher()).isInstanceOf(
				CloudFoundryTaskLauncher.class);
		assertThat(taskPlatform.getLaunchers().get(0).getDescription()).isEqualTo(
				"org = [org], space = [space], url = [https://localhost:9999]");
		assertThat(taskPlatform.getLaunchers().get(0).getScheduler()).isNull();
	}

	@Test
	public void cloudFoundryTaskPlatformWithScheduler() {

		when(cloudFoundrySchedulerClientProvider.cloudFoundrySchedulerClient(anyString())).thenReturn(
				mock(SchedulerClient.class));
		when(cloudFoundrySchedulerClientProvider.schedulerProperties())
				.thenReturn(new CloudFoundrySchedulerProperties());

		CloudFoundrySchedulerProperties schedulerProperties = new CloudFoundrySchedulerProperties();
		schedulerProperties.setSchedulerUrl("https://localhost:9999");

		TaskPlatformFactory taskPlatformFactory = CloudFoundryTaskPlatformFactory
				.builder()
				.platformProperties(cloudFoundryPlatformProperties)
				.platformTokenProvider(platformTokenProvider)
				.connectionContextProvider(connectionContextProvider)
				.cloudFoundryClientProvider(cloudFoundryClientProvider)
				.cloudFoundrySchedulerClientProvider(Optional.of(cloudFoundrySchedulerClientProvider))
				.schedulesEnabled(true)
				.schedulerProperties(Optional.of(schedulerProperties))
				.build();

		TaskPlatform taskPlatform = taskPlatformFactory.createTaskPlatform();
		assertThat(taskPlatform.getName()).isEqualTo("Cloud Foundry");
		assertThat(taskPlatform.getLaunchers()).hasSize(1);
		assertThat(taskPlatform.getLaunchers().get(0).getType()).isEqualTo("Cloud Foundry");
		assertThat(taskPlatform.getLaunchers().get(0).getName()).isEqualTo("default");
		assertThat(taskPlatform.getLaunchers().get(0).getTaskLauncher()).isInstanceOf(
				CloudFoundryTaskLauncher.class);
		assertThat(taskPlatform.getLaunchers().get(0).getDescription()).isEqualTo(
				"org = [org], space = [space], url = [https://localhost:9999]");
		assertThat(taskPlatform.getLaunchers().get(0).getScheduler()).isNotNull();
	}

	private Mono<ListOrganizationsResponse> listOrganizationsResponse() {
		ListOrganizationsResponse response = ListOrganizationsResponse.builder()
				.addAllResources(Collections.<OrganizationResource>singletonList(
						OrganizationResource.builder()
								.metadata(Metadata.builder().id("123").build()).build())
				).build();
		return Mono.just(response);
	}

	private Mono<ListSpacesResponse> listSpacesResponse() {
		ListSpacesResponse response = ListSpacesResponse.builder()
				.addAllResources(Collections.<SpaceResource>singletonList(
						SpaceResource.builder()
								.metadata(Metadata.builder().id("123").build()).build())
				).build();
		return Mono.just(response);
	}
}
