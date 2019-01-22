/*
 * Copyright 2019 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepository;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for metrics controller.
 *
 * @author Christian Tzolov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class RuntimeAppsMetricsControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private AppRegistrationRepository appRegistrationRepository;

	@Autowired
	private SkipperClient skipperClient;

	@Before
	public void setupMocks() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		for (AppRegistration appRegistration : this.appRegistrationRepository.findAll()) {
			this.appRegistrationRepository.deleteAll();
		}

		StreamDefinition streamDefinition3 = new StreamDefinition("ticktock3", "time|log");
		streamDefinitionRepository.save(streamDefinition3);

		AppStatus.Builder logAppStatus = AppStatus.of("ticktock3.log-v1");
		logAppStatus.with(instance("ticktock3.log-v1-0", "46188"));

		AppStatus.Builder timeAppStatus = AppStatus.of("ticktock3.time-v1");
		timeAppStatus.with(instance("ticktock3.time-v1-0", "46188"));

		List<AppStatus> appStatues = Arrays.asList(timeAppStatus.build(), logAppStatus.build());

		Info ticktock3Info = new Info();
		Status ticktock3Status = new Status();
		ticktock3Status.setStatusCode(StatusCode.DEPLOYED);
		ticktock3Status.setPlatformStatus(new ObjectMapper().writeValueAsString(appStatues));
		ticktock3Info.setStatus(ticktock3Status);

		when(this.skipperClient.status("ticktock3")).thenReturn(ticktock3Info);
	}

	@Test
	public void testGetResponse() throws Exception {
		MockHttpServletResponse responseString = mockMvc
				.perform(get("/metrics/streams").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk()).andReturn().getResponse();
		// for now we just get dummy mocked response
		assertThat(responseString.getContentAsString(), containsString("ticktock3"));
		assertThat(responseString.getContentAsString(), containsString("time"));
		assertThat(responseString.getContentAsString(), containsString("log"));
	}

	private AppInstanceStatus instance(String id, String guid) {
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
				return Collections.singletonMap("guid", guid);
			}
		};
	}
}
