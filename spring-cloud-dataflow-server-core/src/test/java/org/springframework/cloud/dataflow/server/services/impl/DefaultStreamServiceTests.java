/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.impl.DefaultStreamService;
import org.springframework.cloud.dataflow.server.stream.AppDeployerStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.StreamDeployers;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringRunner.class)
public class DefaultStreamServiceTests {

	private StreamDefinition streamDefinition1 = new StreamDefinition("test1", "time | log");

	private StreamDefinition streamDefinition2 = new StreamDefinition("test2", "time | log");

	private StreamDefinition streamDefinition3 = new StreamDefinition("test3", "time | log");

	private StreamDeployment streamDeployment1 = new StreamDeployment(streamDefinition1.getName(),
			StreamDeployers.appdeployer.name(), null, null, null);

	private StreamDeployment streamDeployment2 = new StreamDeployment(streamDefinition2.getName(),
			StreamDeployers.skipper.name(), "pkg1", "release1", "local");

	private StreamDeployment streamDeployment3 = new StreamDeployment(streamDefinition3.getName(),
			StreamDeployers.skipper.name(), "pkg1", "release2", "local");

	private List<StreamDefinition> streamDefinitionList = new ArrayList<>();

	private List<StreamDefinition> appDeployerStreamDefinitions = new ArrayList<>();

	private List<StreamDefinition> skipperStreamDefinitions = new ArrayList<>();

	private StreamDeploymentRepository streamDeploymentRepository ;

	private AppDeployerStreamDeployer appDeployerStreamDeployer;

	private SkipperStreamDeployer skipperStreamDeployer;

	private DefaultStreamService defaultStreamService;

	@Before
	public void setupMock() {
		this.streamDeploymentRepository = mock(StreamDeploymentRepository.class);
		this.appDeployerStreamDeployer = mock(AppDeployerStreamDeployer.class);
		this.skipperStreamDeployer = mock(SkipperStreamDeployer.class);
		this.defaultStreamService = new DefaultStreamService(mock(AppRegistry.class),
				mock(CommonApplicationProperties.class),
				mock(ApplicationConfigurationMetadataResolver.class),
				mock(StreamDefinitionRepository.class),
				this.streamDeploymentRepository, this.appDeployerStreamDeployer, this.skipperStreamDeployer);
		this.streamDefinitionList.add(streamDefinition1);
		this.appDeployerStreamDefinitions.add(streamDefinition1);
		this.streamDefinitionList.add(streamDefinition2);
		this.streamDefinitionList.add(streamDefinition3);
		this.skipperStreamDefinitions.add(streamDefinition2);
		this.skipperStreamDefinitions.add(streamDefinition3);
		when(streamDeploymentRepository.findOne(streamDeployment1.getStreamName())).thenReturn(streamDeployment1);
		when(streamDeploymentRepository.findOne(streamDeployment2.getStreamName())).thenReturn(streamDeployment2);
		when(streamDeploymentRepository.findOne(streamDeployment3.getStreamName())).thenReturn(streamDeployment3);
	}

	@Test
	public void verifyUpgradeStream() {
		this.defaultStreamService.upgradeStream(streamDeployment2.getStreamName(), streamDeployment2.getReleaseName(),
				null, null);
		verify(this.skipperStreamDeployer, times(1)).upgradeStream(this.streamDeployment2.getStreamName(),
				this.streamDeployment2.getReleaseName(), null, null);
		verifyNoMoreInteractions(this.skipperStreamDeployer);
		verify(this.appDeployerStreamDeployer, never()).deployStream(any());
	}

	@Test
	public void verifyUndeployStream() {
		StreamDefinition streamDefinition1 = new StreamDefinition("test1", "time | log");
		StreamDeployment streamDeployment1 = new StreamDeployment(streamDefinition1.getName(),
				StreamDeployers.appdeployer.name(), null, null, null);
		StreamDefinition streamDefinition2 = new StreamDefinition("test2", "time | log");
		StreamDeployment streamDeployment2 = new StreamDeployment(streamDefinition2.getName(),
				StreamDeployers.skipper.name(), "pkg1", "release1", "local");
		when(this.streamDeploymentRepository.findOne(streamDeployment1.getStreamName())).thenReturn(streamDeployment1);
		when(this.streamDeploymentRepository.findOne(streamDeployment2.getStreamName())).thenReturn(streamDeployment2);
		this.defaultStreamService.undeployStream(streamDefinition1.getName());
		verify(this.appDeployerStreamDeployer, times(1)).undeployStream(streamDefinition1.getName());
		verifyNoMoreInteractions(this.appDeployerStreamDeployer);
		verify(this.skipperStreamDeployer, never()).undeployStream(streamDefinition1.getName());
		this.defaultStreamService.undeployStream(streamDefinition2.getName());
		verify(this.skipperStreamDeployer, times(1)).undeployStream(streamDefinition2.getName());
		verifyNoMoreInteractions(this.skipperStreamDeployer);
		verify(this.appDeployerStreamDeployer, never()).undeployStream(streamDefinition2.getName());
	}

	@Test
	public void verifyAppDeployerUpgrade() {
		try {
			this.defaultStreamService.upgradeStream(this.streamDeployment1.getStreamName(), this.streamDeployment1.getReleaseName(),
					null, null);
			fail("IllegalStateException is expected to be thrown.");
		}
		catch (IllegalStateException e) {
			Assert.isTrue(e.getMessage().equals("Can only update stream when using the Skipper deployer."),
					"Incorrect Exception message");
		}
	}

	@Test
	public void verifyDeploymentState() {
		Map<StreamDefinition, DeploymentState> appDeployerStates = new HashMap<>();
		appDeployerStates.put(this.streamDefinition1, DeploymentState.deployed);
		when(this.appDeployerStreamDeployer.state(this.appDeployerStreamDefinitions)).thenReturn(appDeployerStates);
		Map<StreamDefinition, DeploymentState> skipperDeployerStates = new HashMap<>();
		skipperDeployerStates.put(this.streamDefinition2, DeploymentState.undeployed);
		skipperDeployerStates.put(this.streamDefinition3, DeploymentState.failed);
		when(this.skipperStreamDeployer.state(this.skipperStreamDefinitions)).thenReturn(skipperDeployerStates);
		Map<StreamDefinition, DeploymentState> states = this.defaultStreamService.state(this.streamDefinitionList);
		Assert.isTrue(states.size() == 3, "Deployment states size mismatch");
		Assert.isTrue(states.get(this.streamDefinition1).equals(DeploymentState.deployed), "Deployment state is incorrect");
		Assert.isTrue(states.get(this.streamDefinition2).equals(DeploymentState.undeployed),
				"Deployment state is incorrect");
		Assert.isTrue(states.get(this.streamDefinition3).equals(DeploymentState.failed), "Deployment state is incorrect");
	}

}
