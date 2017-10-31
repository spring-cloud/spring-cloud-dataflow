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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.dataflow.server.stream.StreamDeployers;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
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

	@Autowired
	private SkipperClient skipperClient;

	@Before
	public void setupMocks() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		for (AppRegistration appRegistration : this.appRegistry.findAll()) {
			this.appRegistry.delete(appRegistration.getName(), appRegistration.getType());
		}

		StreamDefinition streamDefinition1 = new StreamDefinition("ticktock1", "time|log");
		StreamDefinition streamDefinition2 = new StreamDefinition("ticktock2", "time|log");
		StreamDefinition streamDefinition3 = new StreamDefinition("ticktock3", "time|log");
		StreamDefinition streamDefinition4 = new StreamDefinition("ticktock4", "time|log");
		streamDefinitionRepository.save(streamDefinition1);
		streamDefinitionRepository.save(streamDefinition2);
		streamDefinitionRepository.save(streamDefinition3);
		streamDefinitionRepository.save(streamDefinition4);

		StreamDeployment streamDeployment1 = new StreamDeployment(streamDefinition1.getName(), StreamDeployers
				.appdeployer.name());
		StreamDeployment streamDeployment2 = new StreamDeployment(streamDefinition2.getName(), StreamDeployers
				.appdeployer.name());
		StreamDeployment streamDeployment3 = new StreamDeployment(streamDefinition3.getName(), StreamDeployers
				.skipper.name());
		StreamDeployment streamDeployment4 = new StreamDeployment(streamDefinition4.getName(), StreamDeployers
				.skipper.name());
		this.streamDeploymentRepository.save(streamDeployment1);
		this.streamDeploymentRepository.save(streamDeployment2);
		this.streamDeploymentRepository.save(streamDeployment3);
		this.streamDeploymentRepository.save(streamDeployment4);

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
		Info ticktock3Info = new Info();
		Status ticktock3Status = new Status();
		ticktock3Status.setStatusCode(StatusCode.DEPLOYED);
		ticktock3Status.setPlatformStatus("[{\"deploymentId\":\"ticktock3.log-v1\","
				+ "\"instances\":{\"ticktock3.log-v1-0\":{\"instanceNumber\":0,\"id\":\"ticktock3.log-v1-0\",\"state\":\"deployed\"}},\"state\":\"deployed\"},"
				+ "{\"deploymentId\":\"ticktock3.time-v1\",\"instances\":{\"ticktock3.time-v1-0\":{\"instanceNumber\":0,\"baseUrl\":\"http://192.168.1.100:32451\","
				+ "\"process\":{\"alive\":true,\"inputStream\":{},\"outputStream\":{},\"errorStream\":{}},"
				+ "\"attributes\":{\"guid\":\"32451\",\"pid\":\"53492\",\"port\":\"32451\"},"
				+ "\"id\":\"ticktock3.time-v1-0\",\"state\":\"deployed\"}},\"state\":\"deployed\"}]");
		ticktock3Info.setStatus(ticktock3Status);
		Info ticktock4Info = new Info();
		Status ticktock4Status = new Status();
		ticktock4Status.setStatusCode(StatusCode.DEPLOYED);
		ticktock4Status.setPlatformStatus("[{\"deploymentId\":\"ticktock4.log-v1\","
				+ "\"instances\":{\"ticktock4.log-v1-0\":{\"instanceNumber\":0,\"id\":\"ticktock4.log-v1-0\","
				+ "\"state\":\"deployed\"}},\"state\":\"deployed\"},"
				+ "{\"deploymentId\":\"ticktock4.time-v1\",\"instances\":{\"ticktock4.time-v1-0\":{\"instanceNumber\":0,"
				+ "\"baseUrl\":\"http://192.168.1.100:32451\","
				+ "\"process\":{\"alive\":true,\"inputStream\":{},\"outputStream\":{},\"errorStream\":{}},"
				+ "\"attributes\":{\"guid\":\"32451\",\"pid\":\"53492\",\"port\":\"32451\"},"
				+ "\"id\":\"ticktock4.time-v1-0\",\"state\":\"deployed\"}},\"state\":\"deployed\"}]");
		ticktock4Info.setStatus(ticktock4Status);
		when(this.skipperClient.status("scdf_ticktock3")).thenReturn(ticktock3Info);
		when(this.skipperClient.status("scdf_ticktock4")).thenReturn(ticktock4Info);

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
				.perform(get("/runtime/apps/valid/instances/valid-0").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().is4xxClientError()).andReturn().getResponse();
		Assert.assertTrue(responseString.getContentAsString().contains("NoSuchAppInstanceException"));
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
		assertThat(responseString.getContentAsString().contains("ticktock3.time"), is(true));
		assertThat(responseString.getContentAsString().contains("ticktock3.log"), is(true));
		assertThat(responseString.getContentAsString().contains("ticktock4.time"), is(true));
		assertThat(responseString.getContentAsString().contains("ticktock4.log"), is(true));
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
