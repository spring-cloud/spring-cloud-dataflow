/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.cloud.dataflow.configuration.metadata.BootApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.StreamDeploymentRequest;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_PACKAGE_VERSION;
import static org.springframework.cloud.dataflow.server.service.impl.SkipperStreamService.DEFAULT_SKIPPER_PACKAGE_VERSION;

/**
 * @author Ilayaperumal Gopinathan
 * @author Eric Bottard
 * @author Christian Tzolov
 */
@RunWith(SpringRunner.class)
public class SkipperStreamServiceTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private StreamDefinition streamDefinition1 = new StreamDefinition("test1", "time | log");
	private StreamDefinition streamDefinition2 = new StreamDefinition("test2", "time | log");
	private StreamDefinition streamDefinition3 = new StreamDefinition("test3", "time | log");
	private StreamDefinition streamDefinition4 = new StreamDefinition("test4", "time | log");

	private StreamDeployment streamDeployment1 = new StreamDeployment(streamDefinition1.getName());
	private StreamDeployment streamDeployment2 = new StreamDeployment(streamDefinition2.getName());
	private StreamDeployment streamDeployment3 = new StreamDeployment(streamDefinition3.getName());

	private List<StreamDefinition> streamDefinitionList = new ArrayList<>();
	private List<StreamDefinition> skipperStreamDefinitions = new ArrayList<>();
	private StreamDeploymentRepository streamDeploymentRepository;

	private StreamDefinitionRepository streamDefinitionRepository;
	private SkipperStreamDeployer skipperStreamDeployer;
	private AppDeploymentRequestCreator appDeploymentRequestCreator;

	private SkipperStreamService skipperStreamService;
	private AppRegistryCommon appRegistryCommon;

	@Before
	public void setupMock() {
		this.streamDeploymentRepository = mock(StreamDeploymentRepository.class);
		this.streamDefinitionRepository = mock(StreamDefinitionRepository.class);
		this.skipperStreamDeployer = mock(SkipperStreamDeployer.class);
		this.appRegistryCommon = mock(AppRegistryCommon.class);
		this.appDeploymentRequestCreator = new AppDeploymentRequestCreator(this.appRegistryCommon,
				mock(CommonApplicationProperties.class),
				new BootApplicationConfigurationMetadataResolver());
		this.skipperStreamService = new SkipperStreamService(streamDefinitionRepository,
				this.skipperStreamDeployer, this.appDeploymentRequestCreator);
		this.streamDefinitionList.add(streamDefinition1);
		this.streamDefinitionList.add(streamDefinition2);
		this.streamDefinitionList.add(streamDefinition3);
		this.skipperStreamDefinitions.add(streamDefinition2);
		this.skipperStreamDefinitions.add(streamDefinition3);
		this.skipperStreamDefinitions.add(streamDefinition4);
		when(streamDefinitionRepository.findOne("test2")).thenReturn(streamDefinition2);
		when(streamDeploymentRepository.findOne(streamDeployment1.getStreamName())).thenReturn(streamDeployment1);
		when(streamDeploymentRepository.findOne(streamDeployment2.getStreamName())).thenReturn(streamDeployment2);
		when(streamDeploymentRepository.findOne(streamDeployment3.getStreamName())).thenReturn(streamDeployment3);
	}

	@Test
	public void verifyUndeployStream() {
		StreamDefinition streamDefinition2 = new StreamDefinition("test2", "time | log");
		StreamDeployment streamDeployment2 = new StreamDeployment(streamDefinition2.getName(), "");

		when(this.streamDeploymentRepository.findOne(streamDeployment2.getStreamName())).thenReturn(streamDeployment2);

		this.skipperStreamService.undeployStream(streamDefinition2.getName());
		verify(this.skipperStreamDeployer, times(1)).undeployStream(streamDefinition2.getName());
		verifyNoMoreInteractions(this.skipperStreamDeployer);
	}

	@Test
	public void verifyRollbackStream() {
		StreamDefinition streamDefinition2 = new StreamDefinition("test2", "time | log");
		StreamDeployment streamDeployment2 = new StreamDeployment(streamDefinition2.getName(), "");
		when(this.streamDeploymentRepository.findOne(streamDeployment2.getStreamName())).thenReturn(streamDeployment2);

		verifyNoMoreInteractions(this.skipperStreamDeployer);
		this.skipperStreamService.rollbackStream(streamDefinition2.getName(), 0);
		verify(this.skipperStreamDeployer, times(1)).rollbackStream(streamDefinition2.getName(), 0);
	}

	@Test
	public void verifyStreamInfo() {
		StreamDefinition streamDefinition1 = new StreamDefinition("test1", "time | log");
		Map<String, String> deploymentProperties1 = new HashMap<>();
		deploymentProperties1.put("test1", "value1");
		Map<String, String> deploymentProperties2 = new HashMap<>();
		deploymentProperties2.put("test2", "value2");
		Map<String, Map<String, String>> streamDeploymentProperties = new HashMap<>();
		streamDeploymentProperties.put("time", deploymentProperties1);
		streamDeploymentProperties.put("log", deploymentProperties2);
		StreamDeployment streamDeployment1 = new StreamDeployment(streamDefinition1.getName(),
				new JSONObject(streamDeploymentProperties).toString());
		when(this.skipperStreamDeployer.getStreamInfo(streamDeployment1.getStreamName())).thenReturn(streamDeployment1);
		StreamDeployment streamDeployment = this.skipperStreamService.info("test1");
		Assert.assertTrue(streamDeployment.getStreamName().equals(streamDefinition1.getName()));
		Assert.assertTrue(streamDeployment.getDeploymentProperties().equals("{\"log\":{\"test2\":\"value2\"},\"time\":{\"test1\":\"value1\"}}"));
	}

	@Test
	public void verifyStreamState() {
		StreamDefinition streamDefinition = new StreamDefinition("myStream", "time|log");
		Map<StreamDefinition, DeploymentState> streamSates = new HashMap<>();
		streamSates.put(streamDefinition, DeploymentState.deployed);
		when(this.skipperStreamDeployer.streamsStates(eq(Arrays.asList(streamDefinition)))).thenReturn(streamSates);

		Map<StreamDefinition, DeploymentState> resultStates = this.skipperStreamService.state(Arrays.asList(streamDefinition));

		verify(this.skipperStreamDeployer, times(1)).streamsStates(any());

		Assert.assertNotNull(resultStates);
		Assert.assertEquals(1, resultStates.size());
		Assert.assertEquals(DeploymentState.deployed, resultStates.get(streamDefinition));
	}


	@Test
	public void verifyStreamHistory() {
		Release release = new Release();
		release.setName("RELEASE666");
		when(this.skipperStreamDeployer.history(eq("myStream"))).thenReturn(Arrays.asList(release));

		Collection<Release> releases = this.skipperStreamService.history("myStream");

		verify(this.skipperStreamDeployer, times(1)).history(eq("myStream"));

		Assert.assertNotNull(releases);
		Assert.assertEquals(1, releases.size());
		Assert.assertEquals("RELEASE666", releases.iterator().next().getName());
	}

	@Test
	public void verifyStreamPlatformList() {
		Deployer deployer = new Deployer("testDeployer", "testType", null);
		when(this.skipperStreamDeployer.platformList()).thenReturn(Arrays.asList(deployer));
		Collection<Deployer> deployers = this.skipperStreamService.platformList();

		verify(this.skipperStreamDeployer, times(1)).platformList();

		Assert.assertNotNull(deployers);
		Assert.assertEquals(1, deployers.size());
		Assert.assertEquals("testDeployer", deployers.iterator().next().getName());
	}

	@Test
	public void verifyStreamManifest() {
		when(this.skipperStreamDeployer.manifest(eq("myManifest"), eq(666))).thenReturn("MANIFEST666");

		String manifest = this.skipperStreamService.manifest("myManifest", 666);

		verify(this.skipperStreamDeployer, times(1)).manifest(anyString(), anyInt());
		Assert.assertEquals("MANIFEST666", manifest);
	}

	@Test
	public void testStreamDeployWithDefaultPackageVersion() {
		Map<String, String> deploymentProperties = new HashMap<>();

		ArgumentCaptor<StreamDeploymentRequest> argumentCaptor = this.testStreamDeploy(deploymentProperties);

		Assert.assertEquals(DEFAULT_SKIPPER_PACKAGE_VERSION,
				argumentCaptor.getValue().getStreamDeployerProperties().get(SKIPPER_PACKAGE_VERSION));
	}

	@Test
	public void testStreamDeployWithPreDefinedPackageVersion() {
		Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put(SKIPPER_PACKAGE_VERSION, "2.0.0");

		ArgumentCaptor<StreamDeploymentRequest> argumentCaptor = this.testStreamDeploy(deploymentProperties);

		Assert.assertEquals("2.0.0",
				argumentCaptor.getValue().getStreamDeployerProperties().get(SKIPPER_PACKAGE_VERSION));
	}

	public ArgumentCaptor<StreamDeploymentRequest> testStreamDeploy(Map<String, String> deploymentProperties) {
		appDeploymentRequestCreator = mock(AppDeploymentRequestCreator.class);
		skipperStreamDeployer = mock(SkipperStreamDeployer.class);
		streamDefinitionRepository = mock(StreamDefinitionRepository.class);

		this.skipperStreamService = new SkipperStreamService(streamDefinitionRepository,
				this.skipperStreamDeployer, this.appDeploymentRequestCreator);

		StreamDefinition streamDefinition = new StreamDefinition("test1", "time | log");

		when(streamDefinitionRepository.findOne(streamDefinition.getName())).thenReturn(streamDefinition);

		List<AppDeploymentRequest> appDeploymentRequests = Arrays.asList(mock(AppDeploymentRequest.class));
		when(appDeploymentRequestCreator.createRequests(streamDefinition, new HashMap<>()))
				.thenReturn(appDeploymentRequests);

		this.skipperStreamService.deployStream(streamDefinition1.getName(), deploymentProperties);

		ArgumentCaptor<StreamDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(StreamDeploymentRequest.class);
		verify(skipperStreamDeployer, times(1)).deployStream(argumentCaptor.capture());

		return argumentCaptor;
	}
}
