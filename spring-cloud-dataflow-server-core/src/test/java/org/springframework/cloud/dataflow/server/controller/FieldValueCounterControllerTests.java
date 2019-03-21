/*
 * Copyright 2015-2016 the original author or authors.
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.analytics.metrics.FieldValueCounterRepository;
import org.springframework.analytics.metrics.memory.InMemoryFieldValueCounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Tests for {@link FieldValueCounterController}.
 * @author Eric Bottard
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {FieldValueCounterControllerTests.Config.class})
@WebAppConfiguration
public class FieldValueCounterControllerTests {

	@Autowired
	private FieldValueCounterRepository repository;

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultRequest(
				get("/").accept(MediaType.APPLICATION_JSON)).build();
	}

	@After
	public void cleanUp() {
		for (String counter : repository.list()) {
			repository.reset(counter);
		}
	}

	@Test
	public void testList() throws Exception {
		repository.increment("foo", "spring", 20D);
		repository.increment("bar", "java", 2D);
		mockMvc.perform(
				get("/metrics/field-value-counters").accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements", is(2)))
				.andExpect(jsonPath("$.content.*.name", containsInAnyOrder("foo", "bar")))
		;
	}

	@Test
	public void testGetAndDelete() throws Exception {
		repository.increment("foo", "spring", 20D);
		repository.increment("foo", "java", 2D);
		mockMvc.perform(
				get("/metrics/field-value-counters/foo").accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$.name", is("foo")))
				.andExpect(jsonPath("$.values.spring", is(20D)))
				.andExpect(jsonPath("$.values.java", is(2D)))
		;

		mockMvc.perform(
				delete("/metrics/field-value-counters/foo").accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
		;

		mockMvc.perform(
				get("/metrics/field-value-counters/foo").accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isNotFound())
		;

	}

	@Configuration
	@EnableSpringDataWebSupport
	@EnableHypermediaSupport(type = HAL)
	@EnableWebMvc
	public static class Config {

		@Bean
		public FieldValueCounterRepository fieldValueCounterRepository() {
			return new InMemoryFieldValueCounterRepository();
		}


		@Bean
		public FieldValueCounterController fieldValueCounterController() {
			return new FieldValueCounterController(fieldValueCounterRepository());
		}

	}
}
