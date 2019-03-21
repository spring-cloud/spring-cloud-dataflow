/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class RuntimeAppsControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private AppRegistry appRegistry;

	@Autowired
	private AppDeployer appDeployer;

	@Autowired
	private StreamDeploymentRepository streamDeploymentRepository;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private DeploymentIdRepository deploymentIdRepository;

	@Before
	public void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		for (AppRegistration appRegistration : this.appRegistry.findAll()) {
			this.appRegistry.delete(appRegistration.getName(), appRegistration.getType());
		}

		StreamDefinition streamDefinition1 = new StreamDefinition("ticktock1", "time|log");
		StreamDefinition streamDefinition2 = new StreamDefinition("ticktock2", "time|log");
		streamDefinitionRepository.save(streamDefinition1);
		streamDefinitionRepository.save(streamDefinition2);

		StreamDeployment streamDeployment1 = new StreamDeployment(streamDefinition1.getName());
		StreamDeployment streamDeployment2 = new StreamDeployment(streamDefinition2.getName());
		this.streamDeploymentRepository.save(streamDeployment1);
		this.streamDeploymentRepository.save(streamDeployment2);

		deploymentIdRepository.save("ticktock1.time", "ticktock1.time");
		deploymentIdRepository.save("ticktock1.log", "ticktock1.log");
		deploymentIdRepository.save("ticktock2.time", "ticktock2.time");
		deploymentIdRepository.save("ticktock2.log", "ticktock2.log");

		when(appDeployer.status("ticktock1.time"))
				.thenReturn(AppStatus.of("ticktock1.time").generalState(DeploymentState.deployed).build());
		when(appDeployer.status("ticktock1.log"))
				.thenReturn(AppStatus.of("ticktock1.log").generalState(DeploymentState.deployed).build());
		when(appDeployer.status("ticktock2.time"))
				.thenReturn(AppStatus.of("ticktock2.time").generalState(DeploymentState.deployed).build());
		when(appDeployer.status("ticktock2.log"))
				.thenReturn(AppStatus.of("ticktock2.log").generalState(DeploymentState.deployed).build());

		when(appDeployer.status("foo")).thenReturn(AppStatus.of("foo").generalState(DeploymentState.unknown).build());
		AppStatus validAppStatus = AppStatus.of("a1.valid").generalState(DeploymentState.failed).build();
		when(appDeployer.status("valid")).thenReturn(validAppStatus);
	}

	@Test
	public void testFindNonExistentApp() throws Exception {
		MockHttpServletResponse responseString = mockMvc
				.perform(get("/runtime/apps/foo").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().is4xxClientError()).andReturn().getResponse();
		Assert.assertTrue(responseString.getContentAsString().contains("NoSuchAppException"));
	}

	@Test
	public void testFindNonExistentAppInstance() throws Exception {
		MockHttpServletResponse responseString = mockMvc
				.perform(get("/runtime/apps/valid/instances/invalid-0").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().is4xxClientError()).andReturn().getResponse();
		Assert.assertTrue("Was expecting a NoSuchAppInstanceException but got: " + responseString.getContentAsString(),
			responseString.getContentAsString().contains("NoSuchAppInstanceException"));
	}

	@Test
	public void testListRuntimeApps() throws Exception {
		MockHttpServletResponse responseString = mockMvc
				.perform(get("/runtime/apps").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk()).andReturn().getResponse();
		assertThat(responseString.getContentAsString().contains("ticktock1.time"), is(true));
		assertThat(responseString.getContentAsString().contains("ticktock1.log"), is(true));
		assertThat(responseString.getContentAsString().contains("ticktock2.time"), is(true));
		assertThat(responseString.getContentAsString().contains("ticktock2.log"), is(true));
	}

	@Test
	public void testListRuntimeAppsPageSizes() throws Exception {
		MockHttpServletResponse responseString = mockMvc
				.perform(get("/runtime/apps?page=0&size=1").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk()).andReturn().getResponse();
		assertThat(responseString.getContentAsString().contains("ticktock1.log"), is(true));
		assertThat(responseString.getContentAsString().contains("ticktock1.time"), is(false));
		assertThat(responseString.getContentAsString().contains("ticktock2.log"), is(false));
		assertThat(responseString.getContentAsString().contains("ticktock2.time"), is(false));

		responseString = mockMvc.perform(get("/runtime/apps?page=0&size=2").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isOk()).andReturn().getResponse();
		assertThat(responseString.getContentAsString().contains("ticktock1.log"), is(true));
		assertThat(responseString.getContentAsString().contains("ticktock1.time"), is(true));
		assertThat(responseString.getContentAsString().contains("ticktock2.log"), is(false));
		assertThat(responseString.getContentAsString().contains("ticktock2.time"), is(false));

		responseString = mockMvc.perform(get("/runtime/apps?page=1&size=2").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isOk()).andReturn().getResponse();
		assertThat(responseString.getContentAsString().contains("ticktock1.log"), is(false));
		assertThat(responseString.getContentAsString().contains("ticktock1.time"), is(false));
		assertThat(responseString.getContentAsString().contains("ticktock2.log"), is(true));
		assertThat(responseString.getContentAsString().contains("ticktock2.time"), is(true));

		responseString = mockMvc.perform(get("/runtime/apps?page=3&size=1").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isOk()).andReturn().getResponse();
		assertThat(responseString.getContentAsString().contains("ticktock1.log"), is(false));
		assertThat(responseString.getContentAsString().contains("ticktock1.time"), is(false));
		assertThat(responseString.getContentAsString().contains("ticktock2.log"), is(false));
		assertThat(responseString.getContentAsString().contains("ticktock2.time"), is(true));
	}
}
