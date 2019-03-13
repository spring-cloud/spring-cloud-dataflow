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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { JobDependencies.class,
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

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		Launcher launcher = new Launcher("default", "local", taskLauncher);
		Launcher cfLauncher = new Launcher("cf", "Cloud Foundry", mock(TaskLauncher.class));
		this.launcherRepository.save(launcher);
		this.launcherRepository.save(cfLauncher);
	}

	@Test
	public void testGetPlatformList() throws Exception {
		mockMvc.perform(get("/tasks/platforms").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())

				.andExpect(jsonPath("$.content[1].name", is("cf")))
				.andExpect(jsonPath("$.content[1].type", is("Cloud Foundry")))
				.andExpect(jsonPath("$.content[1].description").value(nullValue()))

				.andExpect(jsonPath("$.content[0].name", is("default")))
				.andExpect(jsonPath("$.content[0].type", is("local")))
				.andExpect(jsonPath("$.content[0].description").value(nullValue()));
	}

}
