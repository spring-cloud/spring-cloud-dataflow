/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.cloud.dataflow.server.service.impl;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.server.ConditionalOnSkipperEnabled;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Mark Pollack
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
@TestPropertySource(properties = { FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.SKIPPER_ENABLED + "=true" })
public class SkipperStreamServiceUpgradeStreamTests {

	@MockBean
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private SkipperStreamService streamService;

	@MockBean
	@ConditionalOnSkipperEnabled
	private SkipperStreamDeployer skipperStreamDeployer;

	@MockBean
	private StreamDeploymentRepository streamDeploymentRepository;

	private StreamDefinition streamDefinition2 = new StreamDefinition("test2", "time | log");

	private StreamDeployment streamDeployment2 = new StreamDeployment(streamDefinition2.getName(), "");

	@Test
	public void verifyUpgradeStream() {
		when(streamDefinitionRepository.findOne("test2")).thenReturn(streamDefinition2);
		when(streamDeploymentRepository.findOne(streamDeployment2.getStreamName())).thenReturn(streamDeployment2);
		streamService.updateStream(streamDeployment2.getStreamName(), streamDeployment2.getStreamName(),
				null, null);
		verify(this.skipperStreamDeployer, times(1))
				.upgradeStream(this.streamDeployment2.getStreamName(),
						null, "log:\n" +
								"  spec:\n" +
								"    applicationProperties:\n" +
								"      spring.cloud.dataflow.stream.app.type: sink\n" +
								"time:\n" +
								"  spec:\n" +
								"    applicationProperties:\n" +
								"      spring.cloud.dataflow.stream.app.type: source\n");
		verifyNoMoreInteractions(this.skipperStreamDeployer);
	}

}
