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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.stream.AppDeployerStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.StreamDeployers;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Mark Pollack
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
public class DefaultStreamServiceUpgradeStreamTests {

	@MockBean
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private StreamService streamService;

	@MockBean
	private SkipperStreamDeployer skipperStreamDeployer;

	@MockBean
	private StreamDeploymentRepository streamDeploymentRepository;

	@MockBean
	private AppDeployerStreamDeployer appDeployerStreamDeployer;

	private StreamDefinition streamDefinition2 = new StreamDefinition("test2", "time | log");

	private StreamDeployment streamDeployment2 = new StreamDeployment(streamDefinition2.getName(),
			StreamDeployers.skipper.name(), "pkg1", "release1", "local");

	@Test
	public void verifyUpgradeStream() {
		when(streamDefinitionRepository.findOne("test2")).thenReturn(streamDefinition2);
		when(streamDeploymentRepository.findOne(streamDeployment2.getStreamName())).thenReturn(streamDeployment2);
		((DefaultStreamService) streamService).updateStream(streamDeployment2.getStreamName(),
				streamDeployment2.getReleaseName(),
				null, null);
		verify(this.skipperStreamDeployer, times(1))
				.upgradeStream(this.streamDeployment2.getReleaseName(),
						null, "");
		verifyNoMoreInteractions(this.skipperStreamDeployer);
		verify(this.appDeployerStreamDeployer, never()).deployStream(any());
	}

}
