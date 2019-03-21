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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Andy Clement
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ToolsControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private AppDeployer appDeployer;

	@Before
	public void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		when(appDeployer.deploy(any(AppDeploymentRequest.class))).thenReturn("testID");
	}

	@Test
	public void testMissingArgumentFailure() throws Exception {
		mockMvc.perform(post("/tools/parseTaskTextToGraph").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isInternalServerError());
	}

	@Test
	public void testValidInput() throws Exception {
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
	public void badName() throws Exception {
		mockMvc.perform(post("/tools/parseTaskTextToGraph").content("{\"name\":\"a.b\",\"dsl\":\"appA\"}")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].position", is(0)))
				.andExpect(jsonPath("$.errors[0].message", is("123E:(pos 0): illegal name for a task 'a.b'")));
	}

	@Test
	public void syntaxErrorInDsl() throws Exception {
		mockMvc.perform(post("/tools/parseTaskTextToGraph").content("{\"name\":\"a\",\"dsl\":\"appA & appB\"}")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].position", is(5))).andExpect(jsonPath("$.errors[0].message",
						is("150E:(pos 5): task dsl flow requires a pair of &, not " + "just one")));
	}

	@Test
	public void validationProblem() throws Exception {
		mockMvc.perform(
				post("/tools/parseTaskTextToGraph").content("{\"name\":\"a\",\"dsl\":\"label: appA && label: appA\"}")
						.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.errors[0].position", is(15)))
				.andExpect(jsonPath("$.errors[0].message", is("159E:(pos 15): this label has already been defined")));
	}

	@Test
	public void twoValidationProblems() throws Exception {
		mockMvc.perform(post("/tools/parseTaskTextToGraph")
				.content("{\"name\":\"a\",\"dsl\":\"label: appA && label: appA && label: appA\"}")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].position", is(15)))
				.andExpect(jsonPath("$.errors[0].message", is("159E:(pos 15): this label has already been defined")))
				.andExpect(jsonPath("$.errors[1].position", is(30)))
				.andExpect(jsonPath("$.errors[1].message", is("159E:(pos 30): this label has already been defined")));
	}

}
