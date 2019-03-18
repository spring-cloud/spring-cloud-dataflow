/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.rest.SkipperStream;
import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SkipperStreamDeploymentController.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
@RunWith(MockitoJUnitRunner.class)
public class StreamDeploymentControllerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private StreamDeploymentController controller;

	@Mock
	private StreamDefinitionRepository streamDefinitionRepository;

	@Mock
	private StreamService streamService;

	@Mock
	private Deployer deployer;

	@Before
	public void setup() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		this.controller = new StreamDeploymentController(streamDefinitionRepository, streamService);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDeployViaStreamService() {
		this.controller.deploy("test", new HashMap<>());
		ArgumentCaptor<String> argumentCaptor1 = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Map> argumentCaptor2 = ArgumentCaptor.forClass(Map.class);
		verify(streamService).deployStream(argumentCaptor1.capture(), argumentCaptor2.capture());
		Assert.assertEquals(argumentCaptor1.getValue(), "test");
	}

	@Test
	public void testUpdateStream() {
		Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put(SkipperStream.SKIPPER_PACKAGE_NAME, "ticktock");
		deploymentProperties.put(SkipperStream.SKIPPER_PACKAGE_VERSION, "1.0.0");
		deploymentProperties.put("version.log", "1.2.0.RELEASE");

		UpdateStreamRequest updateStreamRequest = new UpdateStreamRequest("ticktock", new PackageIdentifier(), deploymentProperties);
		this.controller.update("ticktock", updateStreamRequest);
		ArgumentCaptor<UpdateStreamRequest> argumentCaptor1 = ArgumentCaptor.forClass(UpdateStreamRequest.class);
		verify(streamService).updateStream(ArgumentMatchers.eq("ticktock"), argumentCaptor1.capture());
		Assert.assertEquals(updateStreamRequest, argumentCaptor1.getValue());
	}

	@Test
	public void testStreamManifest() {
		this.controller.manifest("ticktock", 666);
		verify(streamService, times(1)).manifest(ArgumentMatchers.eq("ticktock"), ArgumentMatchers.eq(666));
	}

	@Test
	public void testStreamHistory() {
		this.controller.history("releaseName");
		verify(streamService, times(1)).history(ArgumentMatchers.eq("releaseName"));
	}

	@Test
	public void testRollbackViaStreamService() {
		this.controller.rollback("test1", 2);
		ArgumentCaptor<String> argumentCaptor1 = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Integer> argumentCaptor2 = ArgumentCaptor.forClass(Integer.class);
		verify(streamService).rollbackStream(argumentCaptor1.capture(), argumentCaptor2.capture());
		Assert.assertEquals(argumentCaptor1.getValue(), "test1");
		Assert.assertEquals("Rollback version is incorrect", 2, (int) argumentCaptor2.getValue());
	}

	@Test
	public void testPlatformsListViaSkipperClient() {
		when(streamService.platformList()).thenReturn(Arrays.asList(deployer));
		this.controller.platformList();
		verify(streamService, times(1)).platformList();
	}

	@Test
	public void testShowStreamInfo() {
		Map<String, String> deploymentProperties1 = new HashMap<>();
		deploymentProperties1.put("test1", "value1");
		Map<String, String> deploymentProperties2 = new HashMap<>();
		deploymentProperties2.put("test2", "value2");
		Map<String, Map<String, String>> streamDeploymentProperties = new HashMap<>();
		streamDeploymentProperties.put("time", deploymentProperties1);
		streamDeploymentProperties.put("log", deploymentProperties2);
		Map<String, String> appVersions = new HashMap<>();
		appVersions.put("time", "1.0.0.BUILD-SNAPSHOT");
		appVersions.put("log", "1.0.0.BUILD-SNAPSHOT");
		StreamDefinition streamDefinition = new StreamDefinition("testStream1", "time | log");
		StreamDeployment streamDeployment = new StreamDeployment(streamDefinition.getName(),
				new JSONObject(streamDeploymentProperties).toString());
		Map<StreamDefinition, DeploymentState> streamDeploymentStates = new HashMap<>();
		streamDeploymentStates.put(streamDefinition, DeploymentState.deployed);

		when(this.streamDefinitionRepository.findById(streamDefinition.getName())).thenReturn(Optional.of(streamDefinition));
		when(this.streamService.info(streamDefinition.getName())).thenReturn(streamDeployment);
		when(this.streamService.state(anyList())).thenReturn(streamDeploymentStates);

		StreamDeploymentResource streamDeploymentResource = this.controller.info(streamDefinition.getName());
		Assert.assertEquals(streamDeploymentResource.getStreamName(), streamDefinition.getName());
		Assert.assertEquals(streamDeploymentResource.getDslText(), streamDefinition.getDslText());
		Assert.assertEquals(streamDeploymentResource.getStreamName(), streamDefinition.getName());
		Assert.assertEquals("{\"log\":{\"test2\":\"value2\"},\"time\":{\"test1\":\"value1\"}}", streamDeploymentResource.getDeploymentProperties());
		Assert.assertEquals(streamDeploymentResource.getStatus(), DeploymentState.deployed.name());
	}

}
