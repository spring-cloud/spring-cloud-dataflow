/*
 * Copyright 2016-2017 the original author or authors.
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.registry.DataFlowUriRegistryPopulator;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class AppRegistryControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private AppRegistry appRegistry;

	@Autowired
	private DataFlowUriRegistryPopulator uriRegistryPopulator;

	@Before
	public void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultRequest(
				get("/").accept(MediaType.APPLICATION_JSON)).build();
		for (AppRegistration appRegistration: this.appRegistry.findAll()) {
			this.appRegistry.delete(appRegistration.getName(), appRegistration.getType());
		}
		this.uriRegistryPopulator.afterPropertiesSet();
	}

	@Test
	public void testRegisterApplication() throws Exception {
		mockMvc.perform(
				post("/apps/processor/blubba").param("uri", "file:///foo").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
	}

	@Test
	public void testRegisterApplicationTwice() throws Exception {
		mockMvc.perform(
				post("/apps/processor/blubba").param("uri", "file:///foo").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/apps/processor/blubba").param("uri", "file:///foo").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isConflict());
	}

	@Test
	public void testRegisterFromPropertyFile() throws Exception {
		mockMvc.perform(
				post("/apps").param("uri", "classpath:app-registry.properties").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
	}

	@Test
	public void testRegisterFromBadPropertyFile() throws Exception {
		mockMvc.perform(
				post("/apps").param("uri", "classpath:app-registry-bad.properties").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().is5xxServerError());
	}

	@Test
	public void testRegisterAll() throws Exception {
		mockMvc.perform(
				post("/apps").param("apps", "sink.foo=file:///bar").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
	}

	@Test
	public void testRegisterAllWithBadApplication() throws Exception {
		mockMvc.perform(
				post("/apps").param("apps", "sink-foo=file:///bar").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().is5xxServerError());
	}

	@Test
	public void testListApplications() throws Exception {
		mockMvc.perform(
				get("/apps").accept(MediaType.APPLICATION_JSON)).andDo(print())
		.andExpect(status().isOk()).andExpect(jsonPath("content", hasSize(4)));
	}

	@Test
	public void testFindNonExistentApp() throws Exception {
		mockMvc.perform(
				get("/apps/source/foo").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString().contains("NoSuchAppRegistrationException");
	}

	@Test
	public void testRegisterAndListApplications() throws Exception {
		mockMvc.perform(
				get("/apps").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk()).andExpect(jsonPath("content", hasSize(4)));
		mockMvc.perform(
				post("/apps/processor/blubba").param("uri", "file:///foo").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				get("/apps").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk()).andExpect(jsonPath("content", hasSize(5)));
	}

	@Test
	public void testListSingleApplication() throws Exception {
		mockMvc.perform(
				get("/apps/source/time").accept(MediaType.APPLICATION_JSON)).andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("name", is("time")))
		.andExpect(jsonPath("type", is("source")));
	}

	@Test
	public void testUnregisterApplication() throws Exception {
		mockMvc.perform(
				post("/apps/processor/blubba").param("uri", "file:///foo").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				delete("/apps/processor/blubba").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testUnregisterApplicationNotFound() throws Exception {
		mockMvc.perform(
				delete("/apps/processor/blubba").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isNotFound());
	}
}
