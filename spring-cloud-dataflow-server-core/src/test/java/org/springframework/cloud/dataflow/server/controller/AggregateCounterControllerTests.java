/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.cloud.dataflow.server.controller;

import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.analytics.metrics.AggregateCounterRepository;
import org.springframework.analytics.metrics.AggregateCounterResolution;
import org.springframework.analytics.metrics.memory.InMemoryAggregateCounterRepository;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @author Ilayaperumal Gopinathan
 * @author Alex Boyko
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { AggregateCounterControllerTests.Config.class })
public class AggregateCounterControllerTests {

	@Autowired
	AggregateCounterRepository aggregateCounterRepository;

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@Before
	public synchronized void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultRequest(
				get("/").accept(MediaType.APPLICATION_JSON)).build();
	}

	@After
	public synchronized void cleanUp() {
		for (String counter : aggregateCounterRepository.list()) {
			aggregateCounterRepository.reset(counter);
		}
	}

	private void setupAggCounts(int number) {
		AggregateCounterResolution resolution = AggregateCounterResolution.minute;
		for (int i = 0; i < number; i++) {
			aggregateCounterRepository.getCounts("c" + i, 1, resolution);
		}
	}

	@Test
	public void testAggregateCountersListing() throws Exception {
		setupAggCounts(2);
		ResultActions resActions = mockMvc.perform(get("/metrics/aggregate-counters")).andExpect(status().isOk())//
				.andExpect(jsonPath("$.content", Matchers.hasSize(2)));

		for (int i = 0; i < 2; i++) {
			resActions.andExpect(jsonPath("$.content[" + i + "].name").value("c" + i));
			resActions.andExpect(jsonPath("$.content[" + i + "].counts").isEmpty());
		}
	}

	@Test
	public void testDetailedAggregateCountersListing() throws Exception {
		setupAggCounts(2);
		ResultActions resActions = mockMvc.perform(get("/metrics/aggregate-counters?detailed=true&resolution=minute")).andExpect(
				status().isOk())//
				.andExpect(jsonPath("$.content", Matchers.hasSize(2)));

		for (int i = 0; i < 2; i++) {
			resActions.andExpect(jsonPath("$.content[" + i + "].name").value("c" + i));
			resActions.andExpect(jsonPath("$.content[" + i + "].counts").isNotEmpty());
		}
	}

	@Configuration
	@EnableSpringDataWebSupport
	@EnableHypermediaSupport(type = HAL)
	@EnableWebMvc
	public static class Config {

		@Bean
		public InMemoryAggregateCounterRepository aggregateCounterRepository() {
			return new InMemoryAggregateCounterRepository();
		}

		@Bean
		public AggregateCounterController aggregateCounterController() {
			return new AggregateCounterController(aggregateCounterRepository());
		}
	}
}
