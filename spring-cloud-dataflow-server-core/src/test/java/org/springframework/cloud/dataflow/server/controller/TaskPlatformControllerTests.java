/*
 * Copyright 2019 the original author or authors.
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
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.JobDependencies;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = {  JobDependencies.class,
		PropertyPlaceholderAutoConfiguration.class, BatchProperties.class })
@EnableConfigurationProperties({ CommonApplicationProperties.class })
@AutoConfigureTestDatabase(replace = Replace.ANY)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class TaskPlatformControllerTests {

	@Autowired
	private TaskLauncher taskLauncher;

	@Autowired
	private LauncherRepository launcherRepository;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@BeforeEach
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		Launcher launcher = new Launcher("default", "local", taskLauncher);
		Launcher cfLauncher = new Launcher("cf", "Cloud Foundry", mock(TaskLauncher.class));
		Launcher cfLauncherWithScheduler = new Launcher("cfsched", "Cloud Foundry", mock(TaskLauncher.class), mock(Scheduler.class));
		assertThat(this.launcherRepository.findByName("default")).isNull();
		this.launcherRepository.save(launcher);
		this.launcherRepository.save(cfLauncher);
		this.launcherRepository.save(cfLauncherWithScheduler);
	}

	@Test
	public void testGetPlatformList() throws Exception {
		String responseString = mockMvc
				.perform(get("/tasks/platforms").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertTrue(responseString.contains("{\"name\":\"default\",\"type\":\"local\",\"description\":null"));
		assertTrue(responseString.contains("{\"name\":\"cf\",\"type\":\"Cloud Foundry\",\"description\":null"));
		assertTrue(responseString.contains("{\"name\":\"cfsched\",\"type\":\"Cloud Foundry\",\"description\":null"));
	}

	@Test
	public void testGetPlatformSchedulerList() throws Exception {
		String responseString = mockMvc
				.perform(get("/tasks/platforms?schedulesEnabled=true").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertTrue(responseString.contains("{\"name\":\"cfsched\",\"type\":\"Cloud Foundry\",\"description\":null"));
	}

	@Test
	public void testGetPlatformSchedulerListFalse() throws Exception {
		String responseString = mockMvc
				.perform(get("/tasks/platforms?schedulesEnabled=false").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertTrue(responseString.contains("{\"name\":\"default\",\"type\":\"local\",\"description\":null"));
		assertTrue(responseString.contains("{\"name\":\"cf\",\"type\":\"Cloud Foundry\",\"description\":null"));
		assertTrue(responseString.contains("{\"name\":\"cfsched\",\"type\":\"Cloud Foundry\",\"description\":null"));
	}

}
