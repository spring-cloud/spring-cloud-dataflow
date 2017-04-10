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

package org.springframework.cloud.dataflow.server.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Tests for metrics controller.
 *
 * @author Janne Valkealahti
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class MetricsControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private DeploymentIdRepository deploymentIdRepository;

	@Autowired
	private AppRegistry appRegistry;

	@Autowired
	private AppDeployer appDeployer;

	@Before
	public void setupMocks() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultRequest(
				get("/").accept(MediaType.APPLICATION_JSON)).build();
		for (AppRegistration appRegistration : this.appRegistry.findAll()) {
			this.appRegistry.delete(appRegistration.getName(), appRegistration.getType());
		}


		StreamDefinition streamDefinition1 = new StreamDefinition("ticktock1", "time|log");
		streamDefinitionRepository.save(streamDefinition1);

		deploymentIdRepository.save("ticktock1.time", "ticktock1.time");
		deploymentIdRepository.save("ticktock1.log", "ticktock1.log");

		when(appDeployer.status("ticktock1.time")).thenReturn(AppStatus.of("ticktock1.time").generalState(DeploymentState.deployed).build());
		when(appDeployer.status("ticktock1.log")).thenReturn(AppStatus.of("ticktock1.log").generalState(DeploymentState.deployed).build());

		when(appDeployer.status("foo")).thenReturn(AppStatus.of("foo").generalState(DeploymentState.unknown).build());
		AppStatus validAppStatus = AppStatus.of("a1.valid").generalState(DeploymentState.failed).build();
		when(appDeployer.status("valid")).thenReturn(validAppStatus);
	}

	@Test
	public void testSimpleMetricsResponseNoMetrics() throws Exception {
		String content = "{\"ticktock1\":{\"ticktock1.time-0\":\"34215\"},\"ticktock1\":{\"ticktock1.log-0\":\"39729\"}}";
		MockHttpServletResponse responseString = mockMvc.perform(
				post("/metrics/runtime").accept(MediaType.APPLICATION_JSON).content(content).contentType("application/json")).andDo(print())
				.andExpect(status().isOk()).andReturn().getResponse();
		Assert.assertTrue(responseString.getContentAsString().contains("ticktock1"));
		Assert.assertTrue(responseString.getContentAsString().contains("metrics\":{}"));
	}

	@Test
	public void testPostNoContentBody() throws Exception {
		mockMvc.perform(
				post("/metrics/runtime").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().is5xxServerError()).andReturn().getResponse();
	}

	@Test
	public void testSimpleMetricsResponseMetrics() throws Exception {
		String content = "{\"ticktock1.time\":{\"ticktock1.time-0\":\"34215\"},\"ticktock1.log\":{\"ticktock1.log-0\":\"39729\"}}";
		MockHttpServletResponse responseString = mockMvc.perform(
				post("/metrics/runtime").accept(MediaType.APPLICATION_JSON).content(content).contentType("application/json")).andDo(print())
				.andExpect(status().isOk()).andReturn().getResponse();
		Assert.assertTrue(responseString.getContentAsString().contains("ticktock1"));
		Assert.assertTrue(responseString.getContentAsString().contains("333"));
		Assert.assertTrue(responseString.getContentAsString().contains("444"));
	}

}
