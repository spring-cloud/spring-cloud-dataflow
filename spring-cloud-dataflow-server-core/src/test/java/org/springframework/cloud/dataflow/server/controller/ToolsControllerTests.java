/*
 * Copyright 2017-2019 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Andy Clement
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
class ToolsControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@BeforeEach
	void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
	}

	@Test
	void missingArgumentFailure() throws Exception {
		mockMvc.perform(post("/tools/parseTaskTextToGraph").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	void validInput() throws Exception {
		mockMvc.perform(post("/tools/parseTaskTextToGraph").content("{\"name\":\"foo\",\"dsl\":\"appA\"}")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.graph.nodes[0].id", is("0")))
				.andExpect(jsonPath("$.graph.nodes[0].name", is("START")))
				.andExpect(jsonPath("$.graph.nodes[1].id", is("1")))
				.andExpect(jsonPath("$.graph.nodes[1].name", is("appA")))
				.andExpect(jsonPath("$.graph.nodes[2].id", is("2")))
				.andExpect(jsonPath("$.graph.nodes[2].name", is("END")))
				.andExpect(jsonPath("$.graph.links[0].from", is("0")))
				.andExpect(jsonPath("$.graph.links[0].to", is("1")))
				.andExpect(jsonPath("$.graph.links[1].from", is("1")))
				.andExpect(jsonPath("$.graph.links[1].to", is("2")));
	}

	@Test
	void badName() throws Exception {
		mockMvc.perform(post("/tools/parseTaskTextToGraph").content("{\"name\":\"a.b\",\"dsl\":\"appA\"}")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].position", is(0)))
				.andExpect(jsonPath("$.errors[0].message", is("123E:(pos 0): illegal name for a task 'a.b'")));
	}

	@Test
	void syntaxErrorInDsl() throws Exception {
		mockMvc.perform(post("/tools/parseTaskTextToGraph").content("{\"name\":\"a\",\"dsl\":\"appA & appB\"}")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].position", is(5))).andExpect(jsonPath("$.errors[0].message",
						is("150E:(pos 5): task dsl flow requires a pair of &, not " + "just one")));
	}

	@Test
	void validationProblem() throws Exception {
		mockMvc.perform(
				post("/tools/parseTaskTextToGraph").content("{\"name\":\"a\",\"dsl\":\"label: appA && label: appA\"}")
						.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.errors[0].position", is(15)))
				.andExpect(jsonPath("$.errors[0].message", is("159E:(pos 15): this label has already been defined")));
	}

	@Test
	void twoValidationProblems() throws Exception {
		mockMvc.perform(post("/tools/parseTaskTextToGraph")
				.content("{\"name\":\"a\",\"dsl\":\"label: appA && label: appA && label: appA\"}")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].position", is(15)))
				.andExpect(jsonPath("$.errors[0].message", is("159E:(pos 15): this label has already been defined")))
				.andExpect(jsonPath("$.errors[1].position", is(30)))
				.andExpect(jsonPath("$.errors[1].message", is("159E:(pos 30): this label has already been defined")));
	}

}
