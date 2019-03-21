/*
 * Copyright 2018 the original author or authors.
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

import java.util.concurrent.ForkJoinPool;

import org.junit.Test;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Christian Tzolov
 */
public class AppDeployerStreamDeployerTests {

	@Test
	public void testEnvironmentInfo() {
		AppDeployer appDeployer = mock(AppDeployer.class);

		AppDeployerStreamDeployer appDeployerStreamDeployer = new AppDeployerStreamDeployer(
				appDeployer, mock(DeploymentIdRepository.class),
				mock(StreamDefinitionRepository.class),
				mock(StreamDeploymentRepository.class),
				mock(ForkJoinPool.class));

		RuntimeEnvironmentInfo info = appDeployerStreamDeployer.environmentInfo();

		verify(appDeployer).environmentInfo();
	}

	@Test(expected = NoSuchStreamDefinitionException.class)
	public void testStreamInfoMissingStream() {
		AppDeployerStreamDeployer appDeployerStreamDeployer = new AppDeployerStreamDeployer(
				mock(AppDeployer.class), mock(DeploymentIdRepository.class),
				mock(StreamDefinitionRepository.class),
				mock(StreamDeploymentRepository.class),
				mock(ForkJoinPool.class));

		appDeployerStreamDeployer.getStreamInfo("myStream");
	}

	@Test
	public void testStreamInfo() {
		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);
		when(streamDefinitionRepository.findOne(eq("myStream"))).thenReturn(new StreamDefinition("myStream", "dsl"));
		StreamDeploymentRepository streamDeploymentRepository = mock(StreamDeploymentRepository.class);
		when(streamDeploymentRepository.findOne(eq("myStream")))
				.thenReturn(new StreamDeployment("myStream", "props"));

		AppDeployerStreamDeployer appDeployerStreamDeployer = new AppDeployerStreamDeployer(
				mock(AppDeployer.class), mock(DeploymentIdRepository.class),
				streamDefinitionRepository,
				streamDeploymentRepository,
				mock(ForkJoinPool.class));

		appDeployerStreamDeployer.getStreamInfo("myStream");

		verify(streamDefinitionRepository).findOne(eq("myStream"));
		verify(streamDeploymentRepository).findOne(eq("myStream"));
	}

}
