/*
 * Copyright 2019-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamRuntimePropertyKeys;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepository;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
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

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for metrics controller.
 *
 * @author Christian Tzolov
 * @author Daniel Serleg
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class RuntimeStreamsControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private AppRegistrationRepository appRegistrationRepository;

	@Autowired
	private SkipperClient skipperClient;

	@BeforeEach
	public void setupMocks() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		this.appRegistrationRepository.deleteAll();

		StreamDefinition streamDefinition1 = new StreamDefinition("ticktock1", "time1|log1");
		StreamDefinition streamDefinition2 = new StreamDefinition("ticktock2", "time2|log2");
		StreamDefinition streamDefinition3 = new StreamDefinition("ticktock3", "time3|log3");
		streamDefinitionRepository.save(streamDefinition1);
		streamDefinitionRepository.save(streamDefinition2);
		streamDefinitionRepository.save(streamDefinition3);

		List<AppStatus> appStatues1 = Arrays.asList( // CF deployer id
				AppStatus.of("boza-ticktock1-log1-v1")
						.with(instance("ticktock1-log1-v1-0", "guid1", "log1")).build(),
				AppStatus.of("boza-ticktock1-time1-v1")
						.with(instance("ticktock1-time1-v1-0", "guid2", "time1")).build());

		List<AppStatus> appStatues2 = Arrays.asList( // K8s deployer Id
				AppStatus.of("ticktock2-log2-v1")
						.with(instance("ticktock2-log2-v1-0", "guid3", "log2")).build(),
				AppStatus.of("ticktock2-time2-v1")
						.with(instance("ticktock2-time2-v1-0", "guid4", "time2")).build());

		List<AppStatus> appStatues3 = Arrays.asList( // Local deployer id
				AppStatus.of("ticktock3.log3-v1")
						.with(instance("ticktock3.log3-v1-0", null, "log3")).build(),
				AppStatus.of("ticktock3.time3-v1")
						.with(instance("ticktock3.time3-v1-0", null, "time3")).build());

		when(this.skipperClient.status("ticktock1")).thenReturn(toInfo(appStatues1));
		when(this.skipperClient.status("ticktock2")).thenReturn(toInfo(appStatues2));
		when(this.skipperClient.status("ticktock3")).thenReturn(toInfo(appStatues3));
		Map<String, Info> mockInfoTwo = new HashMap<>();
		mockInfoTwo.put("ticktock1", toInfo(appStatues1));
		mockInfoTwo.put("ticktock2", toInfo(appStatues2));
		when(skipperClient.statuses("ticktock1", "ticktock2")).thenReturn(mockInfoTwo);
		Map<String, Info> mockInfoThree = new HashMap<>();
		mockInfoThree.put("ticktock1", toInfo(appStatues1));
		mockInfoThree.put("ticktock2", toInfo(appStatues2));
		mockInfoThree.put("ticktock3", toInfo(appStatues3));
		when(skipperClient.statuses("ticktock1", "ticktock2", "ticktock3")).thenReturn(mockInfoThree);
		Map<String, Info> mockInfoOne = new HashMap<>();
		mockInfoOne.put("ticktock3", toInfo(appStatues3));
		when(skipperClient.statuses("ticktock3")).thenReturn(mockInfoOne);
		List<Release> releaseList = new ArrayList<>();
		Release release1 = new Release();
		release1.setName("ticktock1");
		releaseList.add(release1);
		Release release2 = new Release();
		release2.setName("ticktock2");
		releaseList.add(release2);
		Release release3 = new Release();
		release3.setName("ticktock3");
		releaseList.add(release3);
		when(skipperClient.list(any())).thenReturn(releaseList);
	}

	private Info toInfo(List<AppStatus> appStatues) throws JsonProcessingException {
		Info info = new Info();
		Status ticktock3Status = new Status();
		ticktock3Status.setStatusCode(StatusCode.DEPLOYED);
		ticktock3Status.setPlatformStatus(new ObjectMapper().writeValueAsString(appStatues));
		info.setStatus(ticktock3Status);
		return info;
	}

	@Test
	public void testMultiStreamNames() throws Exception {
		this.mockMvc.perform(
				get("/runtime/streams/ticktock1,ticktock2,ticktock3")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())

				.andExpect(jsonPath("$.**", hasSize(3)))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].name", anyOf(is("ticktock1"), is("ticktock2"), is("ticktock3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList.*", hasSize(2)))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList[0].name", anyOf(is("log1"), is("log2"), is("log3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList[0].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid1"), is("guid3"), is("ticktock3.log3-v1-0"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList[1].name", anyOf(is("time1"), is("time2"), is("time3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList[1].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid2"), is("guid4"), is("ticktock3.time3-v1-0"))))

				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].name", anyOf(is("ticktock1"), is("ticktock2"), is("ticktock3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList.*", hasSize(2)))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList[0].name", anyOf(is("log1"), is("log2"), is("log3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList[0].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid1"), is("guid3"), is("ticktock3.log3-v1-0"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList[1].name", anyOf(is("time1"), is("time2"), is("time3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList[1].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid2"), is("guid4"), is("ticktock3.time3-v1-0"))))

				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].name", anyOf(is("ticktock1"), is("ticktock2"), is("ticktock3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList.*", hasSize(2)))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList[0].name", anyOf(is("log1"), is("log2"), is("log3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList[0].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid1"), is("guid3"), is("ticktock3.log3-v1-0"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList[1].name", anyOf(is("time1"), is("time2"), is("time3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList[1].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid2"), is("guid4"), is("ticktock3.time3-v1-0"))));

	}


	@Test
	public void testPagedStreamNames() throws Exception {
		this.mockMvc.perform(
				get("/runtime/streams?page=0&size=2")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.streamStatusResourceList.*", hasSize(2)));
		this.mockMvc.perform(
				get("/runtime/streams?page=1&size=2")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.streamStatusResourceList.*", hasSize(1)));
		this.mockMvc.perform(
				get("/runtime/streams?page=1&size=3")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.streamStatusResourceList.*").doesNotExist());
		this.mockMvc.perform(
				get("/runtime/streams?page=1000&size=30")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.streamStatusResourceList.*").doesNotExist());
	}

	@Test
	public void testGetResponseForAllRunningStreams() throws Exception {
		this.mockMvc.perform(
				get("/runtime/streams")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())

				.andExpect(jsonPath("$.**", hasSize(3)))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].name", anyOf(is("ticktock1"), is("ticktock2"), is("ticktock3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList.*", hasSize(2)))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList[0].name", anyOf(is("log1"), is("log2"), is("log3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList[0].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid1"), is("guid3"), is("ticktock3.log3-v1-0"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList[1].name", anyOf(is("time1"), is("time2"), is("time3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList[1].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid2"), is("guid4"), is("ticktock3.time3-v1-0"))))

				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].name", anyOf(is("ticktock1"), is("ticktock2"), is("ticktock3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList.*", hasSize(2)))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList[0].name", anyOf(is("log1"), is("log2"), is("log3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList[0].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid1"), is("guid3"), is("ticktock3.log3-v1-0"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList[1].name", anyOf(is("time1"), is("time2"), is("time3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList[1].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid2"), is("guid4"), is("ticktock3.time3-v1-0"))))

				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].name", anyOf(is("ticktock1"), is("ticktock2"), is("ticktock3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList.*", hasSize(2)))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList[0].name", anyOf(is("log1"), is("log2"), is("log3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList[0].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid1"), is("guid3"), is("ticktock3.log3-v1-0"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList[1].name", anyOf(is("time1"), is("time2"), is("time3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList[1].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid2"), is("guid4"), is("ticktock3.time3-v1-0"))));
	}

	@Test
	public void testGetResponseByStreamNames() throws Exception {
		mockMvc.perform(
				get("/runtime/streams")
						.param("names", "ticktock1,ticktock2,ticktock3")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())

				.andExpect(jsonPath("$.**", hasSize(3)))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].name", anyOf(is("ticktock1"), is("ticktock2"), is("ticktock3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList.*", hasSize(2)))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList[0].name", anyOf(is("log1"), is("log2"), is("log3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList[0].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid1"), is("guid3"), is("ticktock3.log3-v1-0"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList[1].name", anyOf(is("time1"), is("time2"), is("time3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[0].applications._embedded.appStatusResourceList[1].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid2"), is("guid4"), is("ticktock3.time3-v1-0"))))

				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].name", anyOf(is("ticktock1"), is("ticktock2"), is("ticktock3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList.*", hasSize(2)))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList[0].name", anyOf(is("log1"), is("log2"), is("log3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList[0].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid1"), is("guid3"), is("ticktock3.log3-v1-0"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList[1].name", anyOf(is("time1"), is("time2"), is("time3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[1].applications._embedded.appStatusResourceList[1].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid2"), is("guid4"), is("ticktock3.time3-v1-0"))))

				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].name", anyOf(is("ticktock1"), is("ticktock2"), is("ticktock3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList.*", hasSize(2)))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList[0].name", anyOf(is("log1"), is("log2"), is("log3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList[0].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid1"), is("guid3"), is("ticktock3.log3-v1-0"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList[1].name", anyOf(is("time1"), is("time2"), is("time3"))))
				.andExpect(jsonPath("$._embedded.streamStatusResourceList[2].applications._embedded.appStatusResourceList[1].instances._embedded.appInstanceStatusResourceList[0].guid", anyOf(is("guid2"), is("guid4"), is("ticktock3.time3-v1-0"))));
	}

	private AppInstanceStatus instance(String id, String guid, String appName) {
		return new AppInstanceStatus() {
			@Override
			public String getId() {
				return id;
			}

			@Override
			public DeploymentState getState() {
				return DeploymentState.deployed;
			}

			@Override
			public Map<String, String> getAttributes() {
				Map<String, String> attributes = new HashMap<>();
				attributes.put(StreamRuntimePropertyKeys.ATTRIBUTE_SKIPPER_APPLICATION_NAME, appName);
				if (guid != null) {
					attributes.put(StreamRuntimePropertyKeys.ATTRIBUTE_GUID, guid);
				}
				return attributes;
			}
		};
	}
}
