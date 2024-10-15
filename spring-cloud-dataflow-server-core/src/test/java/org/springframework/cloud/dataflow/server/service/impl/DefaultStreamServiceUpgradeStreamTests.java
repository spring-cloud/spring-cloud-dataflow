/*
 * Copyright 2017-2019 the original author or authors.
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

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.cloud.dataflow.server.support.PlatformUtils;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Mark Pollack
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
class DefaultStreamServiceUpgradeStreamTests {

	@MockBean
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private StreamService streamService;

	@MockBean
	private SkipperStreamDeployer skipperStreamDeployer;

	private final StreamDefinition streamDefinition2 = new StreamDefinition("test2", "time | log");

	private final StreamDeployment streamDeployment2 = new StreamDeployment(streamDefinition2.getName(), "");

	@Test
	void verifyUpgradeStream() {
		if (!PlatformUtils.isWindows()) {
			when(streamDefinitionRepository.findById("test2")).thenReturn(Optional.of(streamDefinition2));

			final UpdateStreamRequest updateStreamRequest = new UpdateStreamRequest(streamDeployment2.getStreamName(), null, null);
			streamService.updateStream(streamDeployment2.getStreamName(), updateStreamRequest);
			verify(this.skipperStreamDeployer, times(1))
					.upgradeStream(this.streamDeployment2.getStreamName(),
							null, """
									log:
									  spec:
									    applicationProperties:
									      spring.cloud.dataflow.stream.app.type: sink
									time:
									  spec:
									    applicationProperties:
									      spring.cloud.dataflow.stream.app.type: source
									""", false, null);
			verifyNoMoreInteractions(this.skipperStreamDeployer);
		}
	}

}
