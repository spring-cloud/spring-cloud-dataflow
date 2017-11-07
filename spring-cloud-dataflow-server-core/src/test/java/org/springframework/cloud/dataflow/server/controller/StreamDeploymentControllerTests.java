/*
 * Copyright 2016 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.impl.DefaultStreamService;

import static org.mockito.Mockito.verify;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_ENABLED_PROPERTY_KEY;

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
	private DefaultStreamService defaultStreamService;

	@Before
	public void setup() {
		this.controller = new StreamDeploymentController(streamDefinitionRepository, defaultStreamService);
	}

	@Test
	public void testDeployViaStreamService() {
		Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put(SKIPPER_ENABLED_PROPERTY_KEY, "true");
		this.controller.deploy("test", deploymentProperties);
		ArgumentCaptor<String> argumentCaptor1 = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Map> argumentCaptor2 = ArgumentCaptor.forClass(Map.class);
		verify(defaultStreamService).deployStream(argumentCaptor1.capture(), argumentCaptor2.capture());
		Assert.assertEquals(argumentCaptor1.getValue(), "test");
		Assert.assertTrue("Skipper enabled property is missing", argumentCaptor2.getValue().containsKey(SKIPPER_ENABLED_PROPERTY_KEY));
		Assert.assertFalse("useSkipper key shouldn't exist", argumentCaptor2.getValue().containsKey("useSkipper"));
		Assert.assertEquals(argumentCaptor2.getValue().get(SKIPPER_ENABLED_PROPERTY_KEY), "true");
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

}
