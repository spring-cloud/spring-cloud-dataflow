/*
 * Copyright 2020 the original author or authors.
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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for TaskCtrController
 *
 * @author Janne Valkealahti
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = TestDependencies.class)
@AutoConfigureTestDatabase(replace = Replace.ANY)
class TaskCtrControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@MockBean
	private ApplicationConfigurationMetadataResolver metadataResolver;

	@BeforeEach
	void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		ConfigurationMetadataProperty p = new ConfigurationMetadataProperty();
		p.setId("oauth2-client-credentials-scopes");
		p.setName("oauth2-client-credentials-scopes");
		List<ConfigurationMetadataProperty> props = new ArrayList<>();
		props.add(p);
		p = new ConfigurationMetadataProperty();
		p.setId("spring.cloud.task.closecontext-enabled");
		p.setName("false");
		props.add(p);
		Mockito.when(metadataResolver.listProperties(any())).thenReturn(props);
	}

	@Test
	void options() throws Exception {
		mockMvc.perform(get("/tasks/ctr/options").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.[?(@.id == 'oauth2-client-credentials-scopes')].name", hasItems("oauth2-client-credentials-scopes")))
			.andExpect(jsonPath("$.[?(@.id == 'spring.cloud.task.closecontext-enabled')].name", hasItems("false")));
	}
}
