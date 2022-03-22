/*
 * Copyright 2022-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.server.stream;

import java.util.Collections;
import java.util.concurrent.ForkJoinPool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.DefaultStreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamRuntimePropertyKeys;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.controller.NoSuchAppException;
import org.springframework.cloud.dataflow.server.controller.NoSuchAppInstanceException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.ActuatorPostRequest;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the actuator operations on {@link SkipperStreamDeployer}.
 *
 * @author Chris Bono
 */
class SkipperStreamDeployerActuatorTests {

	private SkipperClient skipperClient;

	private SkipperStreamDeployer skipperStreamDeployer;

	private final String releaseName = "flipflop3";
	
	private final String appId = "flipflop3.log-v1";

	private final String instanceId = "flipflop3.log-v1-0";

	private final String endpoint = "info";

	@BeforeEach
	void prepareForTest() {
		skipperClient = mock(SkipperClient.class);
		skipperStreamDeployer = new SkipperStreamDeployer(skipperClient, mock(StreamDefinitionRepository.class),
				mock(AppRegistryService.class), mock(ForkJoinPool.class), new DefaultStreamDefinitionService());
	}
	
	@Nested
	class GetFromActuator {
		@Test
		void happyPath() {
			Release release = newRelease(releaseName, appId, instanceId);
			when(skipperClient.list(null)).thenReturn(Collections.singletonList(release));
			skipperStreamDeployer.getFromActuator(appId, instanceId, endpoint);
			verify(skipperClient).getFromActuator(releaseName, appId, instanceId, endpoint);
		}

		@Test
		void withNoRegisteredApps() {
			when(skipperClient.list(null)).thenReturn(Collections.emptyList());
			assertThatThrownBy(() -> skipperStreamDeployer.getFromActuator(appId, instanceId, endpoint))
					.isInstanceOf(NoSuchAppException.class)
					.hasMessageContaining(appId);
			verifyZeroGetInteractions();
		}

		@Test
		void withNonExistantApp() {
			Release release = newRelease(releaseName, appId, instanceId);
			when(skipperClient.list(null)).thenReturn(Collections.singletonList(release));
			assertThatThrownBy(() -> skipperStreamDeployer.getFromActuator("app5150-nope", instanceId, endpoint))
					.isInstanceOf(NoSuchAppException.class)
					.hasMessageContaining("app5150-nope");
			verifyZeroGetInteractions();
		}

		@Test
		void withUnknownAppStatus() {
			Release release = newRelease(releaseName, appId, instanceId);
			AppStatus appStatus = release.getInfo().getStatus().getAppStatusList().get(0);
			when(appStatus.getState()).thenReturn(DeploymentState.unknown);
			when(skipperClient.list(null)).thenReturn(Collections.singletonList(release));
			assertThatThrownBy(() -> skipperStreamDeployer.getFromActuator(appId, instanceId, endpoint))
					.isInstanceOf(NoSuchAppException.class)
					.hasMessageContaining(appId);
			verifyZeroGetInteractions();
		}

		@Test
		void withNoSuchInstance() {
			Release release = newRelease(releaseName, appId, instanceId);
			when(skipperClient.list(null)).thenReturn(Collections.singletonList(release));
			assertThatThrownBy(() -> skipperStreamDeployer.getFromActuator(appId, "instance5150-nope", endpoint))
					.isInstanceOf(NoSuchAppInstanceException.class)
					.hasMessageContaining("instance5150-nope");
			verifyZeroGetInteractions();
		}

		@Test
		void withNoReleaseNameAttr() {
			Release release = newRelease(releaseName, appId, instanceId);
			AppInstanceStatus appInstanceStatus = release.getInfo().getStatus().getAppStatusList().get(0).getInstances()
					.get(instanceId);
			when(appInstanceStatus.getAttributes()).thenReturn(Collections.emptyMap());
			when(skipperClient.list(null)).thenReturn(Collections.singletonList(release));
			assertThatThrownBy(() -> skipperStreamDeployer.getFromActuator(appId, instanceId, endpoint))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Could not determine release name");
			verifyZeroGetInteractions();
		}
		
		private void verifyZeroGetInteractions() {
			verify(skipperClient, never()).getFromActuator(anyString(), anyString(), anyString(), anyString());
		}
	}

	@Nested
	class PostToActuator {

		private ActuatorPostRequest postRequest = ActuatorPostRequest.of(endpoint, Collections.singletonMap("foo", "bar"));
		
		@Test
		void happyPath() {
			Release release = newRelease(releaseName, appId, instanceId);
			when(skipperClient.list(null)).thenReturn(Collections.singletonList(release));
			skipperStreamDeployer.postToActuator(appId, instanceId, postRequest);
			verify(skipperClient).postToActuator(releaseName, appId, instanceId, postRequest);
		}

		@Test
		void withNoRegisteredApps() {
			when(skipperClient.list(null)).thenReturn(Collections.emptyList());
			assertThatThrownBy(() -> skipperStreamDeployer.postToActuator(appId, instanceId, postRequest))
					.isInstanceOf(NoSuchAppException.class)
					.hasMessageContaining(appId);
			verifyZeroPostInteractions();
		}

		@Test
		void withNonExistantApp() {
			Release release = newRelease(releaseName, appId, instanceId);
			when(skipperClient.list(null)).thenReturn(Collections.singletonList(release));
			assertThatThrownBy(() -> skipperStreamDeployer.postToActuator("app5150-nope", instanceId, postRequest))
					.isInstanceOf(NoSuchAppException.class)
					.hasMessageContaining("app5150-nope");
			verifyZeroPostInteractions();
		}

		@Test
		void withUnknownAppStatus() {
			Release release = newRelease(releaseName, appId, instanceId);
			AppStatus appStatus = release.getInfo().getStatus().getAppStatusList().get(0);
			when(appStatus.getState()).thenReturn(DeploymentState.unknown);
			when(skipperClient.list(null)).thenReturn(Collections.singletonList(release));
			assertThatThrownBy(() -> skipperStreamDeployer.postToActuator(appId, instanceId, postRequest))
					.isInstanceOf(NoSuchAppException.class)
					.hasMessageContaining(appId);
			verifyZeroPostInteractions();
		}

		@Test
		void withNoSuchInstance() {
			Release release = newRelease(releaseName, appId, instanceId);
			when(skipperClient.list(null)).thenReturn(Collections.singletonList(release));
			assertThatThrownBy(() -> skipperStreamDeployer.postToActuator(appId, "instance5150-nope", postRequest))
					.isInstanceOf(NoSuchAppInstanceException.class)
					.hasMessageContaining("instance5150-nope");
			verifyZeroPostInteractions();
		}

		@Test
		void withNoReleaseNameAttr() {
			Release release = newRelease(releaseName, appId, instanceId);
			AppInstanceStatus appInstanceStatus = release.getInfo().getStatus().getAppStatusList().get(0).getInstances()
					.get(instanceId);
			when(appInstanceStatus.getAttributes()).thenReturn(Collections.emptyMap());
			when(skipperClient.list(null)).thenReturn(Collections.singletonList(release));
			assertThatThrownBy(() -> skipperStreamDeployer.postToActuator(appId, instanceId, postRequest))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Could not determine release name");
			verifyZeroPostInteractions();
		}
		
		private void verifyZeroPostInteractions() {
			verify(skipperClient, never()).postToActuator(anyString(), anyString(), anyString(), any(ActuatorPostRequest.class));
		}
	}
	
	private Release newRelease(String releaseName, String appId, String instanceId) {
		AppInstanceStatus appInstanceStatus = mock(AppInstanceStatus.class);
		when(appInstanceStatus.getAttributes()).thenReturn(Collections.singletonMap(StreamRuntimePropertyKeys.ATTRIBUTE_SKIPPER_RELEASE_NAME, releaseName));

		AppStatus appStatus = mock(AppStatus.class);
		when(appStatus.getDeploymentId()).thenReturn(appId);
		when(appStatus.getState()).thenReturn(DeploymentState.deployed);
		when(appStatus.getInstances()).thenReturn(Collections.singletonMap(instanceId, appInstanceStatus));

		Status status = mock(Status.class);
		when(status.getAppStatusList()).thenReturn(Collections.singletonList(appStatus));

		Info info = mock(Info.class);
		when(info.getStatus()).thenReturn(status);

		Release release = new Release();
		release.setName(releaseName);
		release.setInfo(info);

		return release;
	}
}
