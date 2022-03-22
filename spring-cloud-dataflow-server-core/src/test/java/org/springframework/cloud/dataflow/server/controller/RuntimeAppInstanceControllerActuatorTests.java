/*
 * Copyright 2022-2022 the original author or authors.
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

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.server.stream.StreamDeployer;
import org.springframework.cloud.skipper.domain.ActuatorPostRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Actuator endpoints exposed on {@link RuntimeAppInstanceController}.
 *
 * @author Chris Bono
 */
@WebMvcTest(value = RuntimeAppInstanceController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class RuntimeAppInstanceControllerActuatorTests {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private StreamDeployer streamDeployer;

	@Test
	void actuatorGetOnAppInstance() throws Exception {
		String json = "{ \"name\": \"foo\" }";
		when(streamDeployer.getFromActuator("ticktock3.log-v1", "ticktock3.log-v1-0", "info")).thenReturn(json);

		this.mvc.perform(get("/runtime/apps/ticktock3.log-v1/instances/ticktock3.log-v1-0/actuator?endpoint=info").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().json(json));
	}

	@Test
	void actuatorPostOnAppInstance(@Autowired ObjectMapper objectMapper) throws Exception {
		ActuatorPostRequest postRequest = new ActuatorPostRequest();
		postRequest.setEndpoint("info");
		postRequest.setBody(Collections.singletonMap("name", "bar"));
		String requestDataJson = objectMapper.writeValueAsString(postRequest);

		this.mvc.perform(post("/runtime/apps/ticktock3.log-v1/instances/ticktock3.log-v1-0/actuator")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestDataJson))
				.andExpect(status().isCreated());
		verify(streamDeployer).postToActuator(eq("ticktock3.log-v1"), eq("ticktock3.log-v1-0"), eq(postRequest));
	}

	@SpringBootApplication
	static class TestConfiguration {

	}
}
