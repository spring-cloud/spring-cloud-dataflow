/*
 * Copyright 2018-2023 the original author or authors.
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepository;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
class RuntimeAppsControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private AppRegistrationRepository appRegistrationRepository;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private SkipperClient skipperClient;

	@BeforeEach
	void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		for (AppRegistration appRegistration : this.appRegistrationRepository.findAll()) {
			this.appRegistrationRepository.deleteAll();
		}

		StreamDefinition streamDefinition3 = new StreamDefinition("ticktock3", "time|log");
		StreamDefinition streamDefinition4 = new StreamDefinition("ticktock4", "time|log");
		streamDefinitionRepository.save(streamDefinition3);
		streamDefinitionRepository.save(streamDefinition4);

		Info ticktock3Info = new Info();
		Status ticktock3Status = new Status();
		ticktock3Status.setStatusCode(StatusCode.DEPLOYED);
		ticktock3Status.setPlatformStatus("[{\"deploymentId\":\"ticktock3.log-v1\","
				+ "\"instances\":{\"ticktock3.log-v1-0\":{\"instanceNumber\":0,\"id\":\"ticktock3.log-v1-0\",\"state\":\"deployed\"}},\"state\":\"deployed\"},"
				+ "{\"deploymentId\":\"ticktock3.time-v1\",\"instances\":{\"ticktock3.time-v1-0\":{\"instanceNumber\":0,\"baseUrl\":\"https://192.168.1.100:32451\","
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
				+ "\"baseUrl\":\"https://192.168.1.100:32451\","
				+ "\"process\":{\"alive\":true,\"inputStream\":{},\"outputStream\":{},\"errorStream\":{}},"
				+ "\"attributes\":{\"guid\":\"32451\",\"pid\":\"53492\",\"port\":\"32451\"},"
				+ "\"id\":\"ticktock4.time-v1-0\",\"state\":\"deployed\"}},\"state\":\"deployed\"}]");
		ticktock4Info.setStatus(ticktock4Status);
		Map<String, Map<String, DeploymentState>> streamDeploymentStates = new HashMap<>();
		Map<String, DeploymentState> t3Deployments = new HashMap<>();
		t3Deployments.put("ticktock3.log-v1-0", DeploymentState.deployed);
		t3Deployments.put("ticktock3.time-v1", DeploymentState.deployed);
		Map<String, DeploymentState> t4Deployments = new HashMap<>();
		t4Deployments.put("ticktock4.log-v1-0", DeploymentState.deployed);
		t4Deployments.put("ticktock4.time-v1", DeploymentState.deployed);
		streamDeploymentStates.put("ticktock3", t3Deployments);
		streamDeploymentStates.put("ticktock4", t4Deployments);
		Map<String, Info> streamsInfo = new HashMap<>();
		streamsInfo.put("ticktock3", ticktock3Info);
		streamsInfo.put("ticktock4", ticktock4Info);
		Map<String, Info> t3streamsInfo = new HashMap<>();
		t3streamsInfo.put("ticktock3", ticktock3Info);
		Map<String, Info> t4streamsInfo = new HashMap<>();
		t4streamsInfo.put("ticktock4", ticktock4Info);

		List<Release> releases = new ArrayList<>();
		Release release3 = new Release();
		release3.setInfo(ticktock3Info);
		releases.add(release3);
		Release release4 = new Release();
		release4.setInfo(ticktock4Info);
		releases.add(release4);
		when(this.skipperClient.list(any())).thenReturn(releases);

		when(this.skipperClient.statuses("ticktock3", "ticktock4")).thenReturn(streamsInfo);
		when(this.skipperClient.states("ticktock3", "ticktock4")).thenReturn(streamDeploymentStates);
		when(this.skipperClient.states("ticktock3")).thenReturn(streamDeploymentStates);
		when(this.skipperClient.states("ticktock4")).thenReturn(streamDeploymentStates);
		when(this.skipperClient.status("ticktock3")).thenReturn(ticktock3Info);
		when(this.skipperClient.status("ticktock4")).thenReturn(ticktock4Info);
		when(this.skipperClient.statuses("ticktock3")).thenReturn(t3streamsInfo);
		when(this.skipperClient.statuses("ticktock4")).thenReturn(t4streamsInfo);
	}

	@Test
	void findNonExistentApp() throws Exception {
		mockMvc.perform(get("/runtime/apps/foo").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError())
				.andExpect(jsonPath("_embedded.errors[0].logref", is("NoSuchAppException")));
	}

	@Test
	void findNonExistentAppUnknownState() throws Exception {
		Info info = new Info();
		info.setStatus(new Status());
		info.getStatus().setStatusCode(StatusCode.UNKNOWN);
		info.getStatus().setPlatformStatusAsAppStatusList(
			Collections.singletonList(AppStatus.of("ticktock5.log2-v1").generalState(DeploymentState.unknown).build()));

		when(this.skipperClient.status("ticktock5")).thenReturn(info);
		streamDefinitionRepository.save(new StreamDefinition("ticktock5", "time2|log2"));

		mockMvc.perform(get("/runtime/apps/ticktock5.log2-v1.").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("_embedded.errors[0].logref", is("NoSuchAppException")));
	}

	@Test
	void findNonExistentAppInstance() throws Exception {
		Info info = new Info();
		info.setStatus(new Status());
		info.getStatus().setStatusCode(StatusCode.UNKNOWN);
		info.getStatus().setPlatformStatusAsAppStatusList(
			Collections.singletonList(AppStatus.of("ticktock5.log2-v1").generalState(DeploymentState.unknown).build()));

		List<Release> releases = new ArrayList<>();
		Release release = new Release();
		release.setInfo(info);
		releases.add(release);
		when(this.skipperClient.list(any())).thenReturn(releases);
		streamDefinitionRepository.save(new StreamDefinition("ticktock5", "time2|log2"));

		mockMvc.perform(get("/runtime/apps/ticktock5.log2-v1/instances/log2-0").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError())
				.andExpect(jsonPath("_embedded.errors[0].logref", is("NoSuchAppException")));

		info.getStatus().setPlatformStatusAsAppStatusList(
			Collections.singletonList(AppStatus.of("ticktock5.log2-v1").generalState(DeploymentState.deployed).build()));

		mockMvc.perform(get("/runtime/apps/ticktock5.log2-v1/instances/log2-0").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError())
				.andExpect(jsonPath("_embedded.errors[0].logref", is("NoSuchAppInstanceException")));
	}

	@Test
	void findNonExistentAppInstance2() throws Exception {
		mockMvc.perform(
				get("/runtime/apps/ticktock4.log-v1/instances/ticktock4.log-v1-0").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.instanceId", is("ticktock4.log-v1-0")))
				.andExpect(jsonPath("$.state", is("deployed")))
				.andExpect(jsonPath("$.attributes").value(nullValue()))
				.andExpect(jsonPath("$._links.self.href",
						is("http://localhost/runtime/apps/ticktock4.log-v1/instances/ticktock4.log-v1-0")));
	}

	@Test
	void listRuntimeApps() throws Exception {
		mockMvc.perform(get("/runtime/apps").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())

				.andExpect(jsonPath("$._embedded.appStatusResourceList[0].deploymentId", is("ticktock3.log-v1")))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[1].deploymentId", is("ticktock3.time-v1")))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[2].deploymentId", is("ticktock4.log-v1")))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[3].deploymentId", is("ticktock4.time-v1")))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[0].instances._embedded.appInstanceStatusResourceList[0].instanceId", is("ticktock3.log-v1-0")))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[1].instances._embedded.appInstanceStatusResourceList[0].instanceId", is("ticktock3.time-v1-0")))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[2].instances._embedded.appInstanceStatusResourceList[0].instanceId", is("ticktock4.log-v1-0")))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[3].instances._embedded.appInstanceStatusResourceList[0].instanceId", is("ticktock4.time-v1-0")));
	}

	@Test
	void listRuntimeAppsPageSizes() throws Exception {

		mockMvc.perform(get("/runtime/apps?page=0&size=1").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())

				.andExpect(jsonPath("$._embedded.appStatusResourceList.*", hasSize(2)))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[0].deploymentId", is("ticktock3.log-v1")))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[1].deploymentId", is("ticktock3.time-v1")));

		mockMvc.perform(get("/runtime/apps?page=0&size=2").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())

				.andExpect(jsonPath("$._embedded.appStatusResourceList.*", hasSize(4)))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[0].deploymentId", is("ticktock3.log-v1")))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[1].deploymentId", is("ticktock3.time-v1")))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[2].deploymentId", is("ticktock4.log-v1")))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[3].deploymentId", is("ticktock4.time-v1")));

		mockMvc.perform(get("/runtime/apps?page=1&size=1").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())

				.andExpect(jsonPath("$._embedded.appStatusResourceList.*", hasSize(2)))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[0].deploymentId", is("ticktock4.log-v1")))
				.andExpect(jsonPath("$._embedded.appStatusResourceList[1].deploymentId", is("ticktock4.time-v1")));

		mockMvc.perform(get("/runtime/apps?page=1&size=3").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())

				.andExpect(jsonPath("$._embedded.appStatusResourceList.*").doesNotExist());
	}
}
