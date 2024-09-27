/*
 * Copyright 2019-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.core.TaskPlatformFactory;
import org.springframework.cloud.dataflow.server.config.cloudfoundry.CloudFoundryPlatformProperties.CloudFoundryProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryTaskLauncher;

import io.pivotal.scheduler.SchedulerClient;
import reactor.core.publisher.Mono;

/**
 * @author David Turanski
 * @author Glenn Renfro
 * @author Corneil du Plessis
 **/
public class CloudFoundryTaskPlatformFactoryTests {

	private CloudFoundryPlatformTokenProvider platformTokenProvider;

	private CloudFoundryPlatformConnectionContextProvider connectionContextProvider;

	private CloudFoundryPlatformClientProvider cloudFoundryClientProvider;

	private CloudFoundrySchedulerClientProvider cloudFoundrySchedulerClientProvider;

	private CloudFoundryClient cloudFoundryClient;

	private LogCacheClient logCacheClient;

	private CloudFoundryPlatformProperties cloudFoundryPlatformProperties;

	private CloudFoundryConnectionProperties defaultConnectionProperties;

	private CloudFoundryConnectionProperties anotherOrgSpaceConnectionProperties;

	private CloudFoundryDeploymentProperties deploymentProperties;

	@BeforeEach
	public void setUp() throws Exception {
		cloudFoundryClientProvider = mock(CloudFoundryPlatformClientProvider.class);
		cloudFoundrySchedulerClientProvider = mock(CloudFoundrySchedulerClientProvider.class);
		cloudFoundryClient = mock(CloudFoundryClient.class);
		logCacheClient = mock(LogCacheClient.class);

		when(this.cloudFoundryClient.info())
				.thenReturn(getInfoRequest -> Mono.just(GetInfoResponse.builder().apiVersion("0.0.0").build()));
		when(this.cloudFoundryClient.organizations()).thenReturn(mock(Organizations.class));
		when(this.cloudFoundryClient.spaces()).thenReturn(mock(Spaces.class));
		when(this.cloudFoundryClient.organizations().list(any())).thenReturn(listOrganizationsResponse());
		when(this.cloudFoundryClient.spaces().list(any())).thenReturn(listSpacesResponse());
		when(this.cloudFoundryClientProvider.cloudFoundryClient(anyString())).thenReturn(this.cloudFoundryClient);
		when(this.cloudFoundryClientProvider.logCacheClient(anyString())).thenReturn(this.logCacheClient);

		this.cloudFoundryPlatformProperties = new CloudFoundryPlatformProperties();

		this.defaultConnectionProperties = new CloudFoundryConnectionProperties();
		this.defaultConnectionProperties.setOrg("org");
		this.defaultConnectionProperties.setSpace("space");
		this.defaultConnectionProperties.setUrl(new URL("https://localhost:9999"));

		this.deploymentProperties = new CloudFoundryDeploymentProperties();
		this.deploymentProperties.setApiTimeout(1L);
	}

	@Test
	public void cloudFoundryTaskPlatformNoScheduler() {
		setupSinglePlatform();
		TaskPlatformFactory taskPlatformFactory = CloudFoundryTaskPlatformFactory
				.builder()
				.platformProperties(this.cloudFoundryPlatformProperties)
				.platformTokenProvider(this.platformTokenProvider)
				.connectionContextProvider(this.connectionContextProvider)
				.cloudFoundryClientProvider(this.cloudFoundryClientProvider)
				.build();

		TaskPlatform taskPlatform = taskPlatformFactory.createTaskPlatform();
		assertThat(taskPlatform.getName()).isEqualTo("Cloud Foundry");
		assertThat(taskPlatform.getLaunchers()).hasSize(1);
		Launcher launcher = taskPlatform.getLaunchers().get(0);
		validateBasicLauncherInfo(launcher, "default");
		assertThat(launcher.getDescription()).isEqualTo(
				"org = [org], space = [space], url = [https://localhost:9999]");
		assertThat(launcher.getScheduler()).isNull();
	}

	@Test
	public void cloudFoundryTaskPlatformWithScheduler() {
		setupSinglePlatform();
		when(this.cloudFoundrySchedulerClientProvider.cloudFoundrySchedulerClient(anyString())).thenReturn(
				mock(SchedulerClient.class));

		CloudFoundryProperties cloudFoundryProperties = this.cloudFoundryPlatformProperties.getAccounts().get("default");
		CloudFoundryDeploymentProperties cloudFoundryDeploymentProperties = new CloudFoundryDeploymentProperties();
		cloudFoundryDeploymentProperties.setSchedulerUrl("https://localhost:9999");
		cloudFoundryProperties.setDeployment(cloudFoundryDeploymentProperties);

		TaskPlatform taskPlatform = getSchedulePlatform("default");
		assertThat(taskPlatform.getLaunchers()).hasSize(1);
		Launcher launcher = taskPlatform.getLaunchers().get(0);
		validateBasicLauncherInfo(launcher, "default");
		assertThat(launcher.getDescription()).isEqualTo(
				"org = [org], space = [space], url = [https://localhost:9999]");
		assertThat(launcher.getScheduler()).isNotNull();
	}

	@Test
	public void cloudFoundryTaskMultiPlatformWithScheduler() throws Exception{
		setupMultiPlatform();
		when(this.cloudFoundrySchedulerClientProvider.cloudFoundrySchedulerClient(anyString())).thenReturn(
				mock(SchedulerClient.class));

		TaskPlatform taskPlatform = getSchedulePlatform("default");
		assertThat(taskPlatform.getLaunchers()).hasSize(2);
		Launcher launcher = taskPlatform.getLaunchers().get(0);
		validateBasicLauncherInfo(launcher, "default");
		assertThat(launcher.getDescription()).isEqualTo(
				"org = [org], space = [space], url = [https://localhost:9999]");
		assertThat(launcher.getScheduler()).isNotNull();

		launcher = taskPlatform.getLaunchers().get(1);
		validateBasicLauncherInfo(launcher, "anotherOrgSpace");
		assertThat(launcher.getScheduler()).isNull();

		assertThat(launcher.getDescription()).isEqualTo(
				"org = [another-org], space = [another-space], url = [https://localhost:9999]");
	}

	private void validateBasicLauncherInfo(Launcher launcher, String platformName) {
		assertThat(launcher.getType()).isEqualTo("Cloud Foundry");
		assertThat(launcher.getName()).isEqualTo(platformName);
		assertThat(launcher.getTaskLauncher()).isInstanceOf(CloudFoundryTaskLauncher.class);
	}

	private void setupSinglePlatform() {
		CloudFoundryProperties cloudFoundryProperties = new CloudFoundryProperties();
		cloudFoundryProperties.setDeployment(new CloudFoundryDeploymentProperties());
		cloudFoundryProperties.setConnection(this.defaultConnectionProperties);
		this.cloudFoundryPlatformProperties.setAccounts(Collections.singletonMap("default", cloudFoundryProperties));

		this.connectionContextProvider = new CloudFoundryPlatformConnectionContextProvider(this.cloudFoundryPlatformProperties);
		this.platformTokenProvider = mock(CloudFoundryPlatformTokenProvider.class);
		when(this.platformTokenProvider.tokenProvider(anyString())).thenReturn(mock(TokenProvider.class));
	}


	private TaskPlatform getSchedulePlatform(String platformName) {
		CloudFoundryProperties cloudFoundryProperties = this.cloudFoundryPlatformProperties.getAccounts().get(platformName);
		CloudFoundryDeploymentProperties cloudFoundryDeploymentProperties = new CloudFoundryDeploymentProperties();
		cloudFoundryDeploymentProperties.setSchedulerUrl("https://localhost:9999");
		cloudFoundryProperties.setDeployment(cloudFoundryDeploymentProperties);
		TaskPlatformFactory taskPlatformFactory = CloudFoundryTaskPlatformFactory
				.builder()
				.platformProperties(this.cloudFoundryPlatformProperties)
				.platformTokenProvider(this.platformTokenProvider)
				.connectionContextProvider(this.connectionContextProvider)
				.cloudFoundryClientProvider(this.cloudFoundryClientProvider)
				.cloudFoundrySchedulerClientProvider(Optional.of(this.cloudFoundrySchedulerClientProvider))
				.schedulesEnabled(true)
				.schedulerProperties(this.cloudFoundryPlatformProperties.getAccounts().get(platformName).getDeployment())
				.build();

		TaskPlatform taskPlatform =  taskPlatformFactory.createTaskPlatform();
		assertThat(taskPlatform.getName()).isEqualTo("Cloud Foundry");
		return taskPlatform;
	}

	private void setupMultiPlatform() throws Exception{
		this.anotherOrgSpaceConnectionProperties = new CloudFoundryConnectionProperties();
		this.anotherOrgSpaceConnectionProperties.setOrg("another-org");
		this.anotherOrgSpaceConnectionProperties.setSpace("another-space");
		this.anotherOrgSpaceConnectionProperties.setUrl(new URL("https://localhost:9999"));


		CloudFoundryProperties cloudFoundryProperties = new CloudFoundryProperties();
		CloudFoundryDeploymentProperties cloudFoundryDeploymentProperties = new CloudFoundryDeploymentProperties();
		cloudFoundryDeploymentProperties.setSchedulerUrl("https://localhost:9999");
		cloudFoundryProperties.setDeployment(new CloudFoundryDeploymentProperties());
		cloudFoundryProperties.setConnection(this.defaultConnectionProperties);
		Map<String, CloudFoundryProperties> platformMap = new HashMap<>();
		platformMap.put("default", cloudFoundryProperties);
		cloudFoundryProperties = new CloudFoundryProperties();
		cloudFoundryProperties.setDeployment(new CloudFoundryDeploymentProperties());
		cloudFoundryProperties.setConnection(this.anotherOrgSpaceConnectionProperties);


		platformMap.put("anotherOrgSpace", cloudFoundryProperties);

		this.cloudFoundryPlatformProperties.setAccounts(platformMap);

		this.connectionContextProvider = new CloudFoundryPlatformConnectionContextProvider(this.cloudFoundryPlatformProperties);
		this.platformTokenProvider = mock(CloudFoundryPlatformTokenProvider.class);
		when(this.platformTokenProvider.tokenProvider(anyString())).thenReturn(mock(TokenProvider.class));
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
}
