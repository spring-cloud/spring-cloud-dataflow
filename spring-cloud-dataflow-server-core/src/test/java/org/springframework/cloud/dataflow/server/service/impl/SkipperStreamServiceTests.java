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

package org.springframework.cloud.dataflow.server.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.cloud.dataflow.configuration.metadata.BootApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.repository.IncompatibleStreamDeployerException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.StreamDeployers;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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

	private StreamDeployment streamDeployment1 = new StreamDeployment(streamDefinition1.getName(),
			StreamDeployers.appdeployer.name(), null, null, null);
	private StreamDeployment streamDeployment2 = new StreamDeployment(streamDefinition2.getName(),
			StreamDeployers.skipper.name(), "pkg1", "release1", "local");
	private StreamDeployment streamDeployment3 = new StreamDeployment(streamDefinition3.getName(),
			StreamDeployers.skipper.name(), "pkg1", "release2", "local");

	private List<StreamDefinition> streamDefinitionList = new ArrayList<>();
	private List<StreamDefinition> skipperStreamDefinitions = new ArrayList<>();
	private StreamDeploymentRepository streamDeploymentRepository;

	private StreamDefinitionRepository streamDefinitionRepository;
	private SkipperStreamDeployer skipperStreamDeployer;
	private AppDeploymentRequestCreator appDeploymentRequestCreator;

	private AppRegistryService appRegistryService;
	private SkipperStreamService skipperStreamService;

	@Before
	public void setupMock() {
		this.streamDeploymentRepository = mock(StreamDeploymentRepository.class);
		this.streamDefinitionRepository = mock(StreamDefinitionRepository.class);
		this.appRegistryService = mock(AppRegistryService.class);
		this.skipperStreamDeployer = mock(SkipperStreamDeployer.class);
		this.appDeploymentRequestCreator = new AppDeploymentRequestCreator(mock(AppRegistry.class),
				mock(CommonApplicationProperties.class),
				new BootApplicationConfigurationMetadataResolver());
		this.skipperStreamService = new SkipperStreamService(mock(StreamDefinitionRepository.class),
				this.streamDeploymentRepository, this.appRegistryService, this.skipperStreamDeployer,
				this.appDeploymentRequestCreator);
		this.streamDefinitionList.add(streamDefinition1);
		this.streamDefinitionList.add(streamDefinition2);
		this.streamDefinitionList.add(streamDefinition3);
		this.skipperStreamDefinitions.add(streamDefinition2);
		this.skipperStreamDefinitions.add(streamDefinition3);
		when(streamDefinitionRepository.findOne("test2")).thenReturn(streamDefinition2);
		when(streamDeploymentRepository.findOne(streamDeployment1.getStreamName())).thenReturn(streamDeployment1);
		when(streamDeploymentRepository.findOne(streamDeployment2.getStreamName())).thenReturn(streamDeployment2);
		when(streamDeploymentRepository.findOne(streamDeployment3.getStreamName())).thenReturn(streamDeployment3);
	}


	@Test(expected = IncompatibleStreamDeployerException.class)
	public void verifyUndeployStreamInIncompatibleDeployer() {
		StreamDefinition streamDefinition1 = new StreamDefinition("test1", "time | log");
		StreamDeployment streamDeployment1 = new StreamDeployment(streamDefinition1.getName(),
				StreamDeployers.appdeployer.name(), null, null, null);

		when(this.streamDeploymentRepository.findOne(streamDeployment1.getStreamName())).thenReturn(streamDeployment1);

		this.skipperStreamService.undeployStream(streamDefinition1.getName());
	}

	@Test
	public void verifyUndeployStream() {
		StreamDefinition streamDefinition2 = new StreamDefinition("test2", "time | log");
		StreamDeployment streamDeployment2 = new StreamDeployment(streamDefinition2.getName(),
				StreamDeployers.skipper.name(), "pkg1", "release1", "local");

		when(this.streamDeploymentRepository.findOne(streamDeployment2.getStreamName())).thenReturn(streamDeployment2);

		this.skipperStreamService.undeployStream(streamDefinition2.getName());
		verify(this.skipperStreamDeployer, times(1)).undeployStream(streamDefinition2.getName());
		verifyNoMoreInteractions(this.skipperStreamDeployer);
	}

	@Test
	public void verifyRollbackStreamOnIncompatibleDeployer() {
		StreamDefinition streamDefinition1 = new StreamDefinition("test1", "time | log");
		StreamDeployment streamDeployment1 = new StreamDeployment(streamDefinition1.getName(),
				StreamDeployers.appdeployer.name(), null, null, null);

		when(this.streamDeploymentRepository.findOne(streamDeployment1.getStreamName())).thenReturn(streamDeployment1);

		verifyNoMoreInteractions(this.skipperStreamDeployer);
		try {
			this.skipperStreamService.rollbackStream(streamDefinition1.getName(), 0);
			fail("IncompatibleStreamDeployerException is expected when trying to rollback a stream that was deployed using "
					+ "app deployer");
		}
		catch (IncompatibleStreamDeployerException e) {
			assertThat(e.getMessage()).isEqualTo("Can perform this stream operation only on deployer: skipper");
		}
	}

	@Test
	public void verifyRollbackStream() {
		StreamDefinition streamDefinition2 = new StreamDefinition("test2", "time | log");
		StreamDeployment streamDeployment2 = new StreamDeployment(streamDefinition2.getName(),
				StreamDeployers.skipper.name(), "pkg1", "release1", "local");
		when(this.streamDeploymentRepository.findOne(streamDeployment2.getStreamName())).thenReturn(streamDeployment2);

		verifyNoMoreInteractions(this.skipperStreamDeployer);
		this.skipperStreamService.rollbackStream(streamDefinition2.getName(), 0);
		verify(this.skipperStreamDeployer, times(1)).rollbackStream(streamDefinition2.getName(), 0);
	}

	@Test
	public void verifyAppDeployerUpgrade() {
		try {
			this.skipperStreamService.updateStream(this.streamDeployment1.getStreamName(),
					this.streamDeployment1.getReleaseName(),
					null, null);
			fail("IllegalStateException is expected to be thrown.");
		}
		catch (IncompatibleStreamDeployerException e) {
		}
	}

	@Test
	public void verifyDeploymentState() {

		Map<StreamDefinition, DeploymentState> skipperDeployerStates = new HashMap<>();
		skipperDeployerStates.put(this.streamDefinition2, DeploymentState.undeployed);
		skipperDeployerStates.put(this.streamDefinition3, DeploymentState.failed);
		skipperDeployerStates.put(this.streamDefinition4, DeploymentState.deployed);

		when(this.skipperStreamDeployer.state(this.skipperStreamDefinitions)).thenReturn(skipperDeployerStates);

		Map<StreamDefinition, DeploymentState> states = this.skipperStreamService.state(this.streamDefinitionList);

		System.out.println(states.size());

		Assert.isTrue(states.size() == 3, "Deployment states size mismatch");

		Assert.isTrue(states.get(this.streamDefinition4).equals(DeploymentState.deployed),
				"Deployment state is incorrect");
		Assert.isTrue(states.get(this.streamDefinition2).equals(DeploymentState.undeployed),
				"Deployment state is incorrect");
		Assert.isTrue(states.get(this.streamDefinition3).equals(DeploymentState.failed),
				"Deployment state is incorrect");
	}
}
