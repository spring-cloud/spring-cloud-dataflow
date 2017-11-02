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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.support.TestResourceUtils;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
public class DefaultStreamServiceUpdateTests {

	@Autowired
	private AppRegistry appRegistry;

	@Autowired
	private DefaultStreamService streamService;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Test
	public void testCreateUpdateRequests() throws IOException {
		StreamDefinition streamDefinition = new StreamDefinition("test", "time | log");
		this.streamDefinitionRepository.save(streamDefinition);
		Map<String, String> updateProperties = new HashMap<>();
		updateProperties.put("app.log.server.port", "9999");
		updateProperties.put("app.log.endpoints.sensitive", "false");
		updateProperties.put("app.log.level", "ERROR"); //this should be expanded
		updateProperties.put("deployer.log.memory", "4096m");
		updateProperties.put("version.log", "1.1.1.RELEASE");
		String yml = streamService.convertPropertiesToSkipperYaml("test", updateProperties);
		String expectedYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "update.yml").getInputStream(),
				Charset.defaultCharset());
		assertThat(yml).isEqualTo(expectedYaml);
	}

}
