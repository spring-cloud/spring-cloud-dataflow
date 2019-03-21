/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StreamDeploymentController.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@RunWith(MockitoJUnitRunner.class)
public class StreamDeploymentControllerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private StreamDeploymentController controller;

	@Mock
	private StreamDefinitionRepository streamDefinitionRepository;

	@Mock
	private StreamService defaultStreamService;

	@Mock
	private Deployer deployer;

	@Before
	public void setup() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		this.controller = new StreamDeploymentController(streamDefinitionRepository, defaultStreamService);
	}

	@Test
	public void testDeployViaStreamService() {
		this.controller.deploy("test", new HashMap<>());
		ArgumentCaptor<String> argumentCaptor1 = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Map> argumentCaptor2 = ArgumentCaptor.forClass(Map.class);
		verify(defaultStreamService).deployStream(argumentCaptor1.capture(), argumentCaptor2.capture());
		Assert.assertEquals(argumentCaptor1.getValue(), "test");
	}

	@Test
	public void testRollbackViaStreamService() {
		this.controller.rollback("test1", 2);
		ArgumentCaptor<String> argumentCaptor1 = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Integer> argumentCaptor2 = ArgumentCaptor.forClass(Integer.class);
		verify(defaultStreamService).rollbackStream(argumentCaptor1.capture(), argumentCaptor2.capture());
		Assert.assertEquals(argumentCaptor1.getValue(), "test1");
		Assert.assertTrue("Rollback version is incorrect", argumentCaptor2.getValue() == 2);
	}

	@Test
	public void tesPlatformsListViaSkipperClient() {
		when(defaultStreamService.platformList()).thenReturn(Arrays.asList(deployer));
		this.controller.platformList();
		verify(defaultStreamService, times(1)).platformList();
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
		when(this.streamDefinitionRepository.findOne(streamDefinition.getName())).thenReturn(streamDefinition);
		when(this.defaultStreamService.info(streamDefinition.getName())).thenReturn(streamDeployment);
		when(this.defaultStreamService.state(anyListOf(StreamDefinition.class))).thenReturn(streamDeploymentStates);
		StreamDeploymentResource streamDeploymentResource = this.controller.info(streamDefinition.getName());
		Assert.assertTrue(streamDeploymentResource.getStreamName().equals(streamDefinition.getName()));
		Assert.assertTrue(streamDeploymentResource.getDslText().equals(streamDefinition.getDslText()));
		Assert.assertTrue(streamDeploymentResource.getStreamName().equals(streamDefinition.getName()));
		Assert.assertTrue(streamDeploymentResource.getDeploymentProperties().equals("{\"log\":{\"test2\":\"value2\"},\"time\":{\"test1\":\"value1\"}}"));
		Assert.assertTrue(streamDeploymentResource.getStatus().equals(DeploymentState.deployed.name()));
	}

}
