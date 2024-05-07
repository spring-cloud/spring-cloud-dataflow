/*
 * Copyright 2023 the original author or authors.
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


import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestDependencies.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class SchemaControllerTests {
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@BeforeEach
	public void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(
						get("/").accept(MediaType.APPLICATION_JSON)
				).build();
	}

	@Test
	public void testVersions() throws Exception {
		// when
		ResultActions result = mockMvc.perform(get("/schema/versions").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
		// then
		result.andExpect(jsonPath("$.defaultSchemaVersion", is("2")));
		result.andExpect(jsonPath("$.versions", is(Arrays.asList("2", "3"))));
	}

	@Test
	public void testTargets() throws Exception {
		// when
		ResultActions result = mockMvc.perform(get("/schema/targets").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
		// then
		result.andExpect(jsonPath("$.defaultSchemaTarget", is("boot2")));
		result.andExpect(jsonPath("$._links.self.href", is("http://localhost/schema/targets")));
		result.andExpect(jsonPath("$.schemas", hasSize(2)));
		result.andExpect(jsonPath("$.schemas[?(@.name=='boot3')]._links.self.href", hasItem("http://localhost/schema/targets/boot3")));
		result.andExpect(jsonPath("$.schemas[?(@.name=='boot3')].batchPrefix", hasItem("BOOT3_BATCH_")));
		result.andExpect(jsonPath("$.schemas[?(@.name=='boot3')].schemaVersion", hasItem("3")));
	}
}
