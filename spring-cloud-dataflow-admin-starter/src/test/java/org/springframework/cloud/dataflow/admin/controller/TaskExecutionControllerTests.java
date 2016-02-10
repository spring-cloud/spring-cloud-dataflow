/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.admin.configuration.TestDependencies;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestDependencies.class})
@WebAppConfiguration
public class TaskExecutionControllerTests {

	private final static String TASK_NAME = "myTask";

	@Autowired
	private TaskExecutionDao dao;

	@Autowired
	private TaskExecutionController controller;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired private ApplicationContext applicationContext;

	private boolean initialized = false;

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultRequest(
				get("/").accept(MediaType.APPLICATION_JSON)).build();

		if(!initialized) {
			initialized = true;
			dao.saveTaskExecution(new TaskExecution(0, 0, TASK_NAME, new Date(),
					new Date(), null, new ArrayList<String>()));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskExecutionControllerConstructorMissingExplorer() {
		new TaskExecutionController(null);
	}

	@Test
	public void testGetExecutionNotFound() throws Exception{
		mockMvc.perform(
				get("/tasks/executions/1345345345345").accept(MediaType.APPLICATION_JSON)
		).andExpect(status().is5xxServerError());
	}

	@Test
	public void testGetExecution() throws Exception{
		mockMvc.perform(
				get("/tasks/executions/0").accept(MediaType.APPLICATION_JSON)
		).andExpect(status().isOk()).andDo(print()).andExpect(content().json("{taskName: \"" +
				TASK_NAME + "\"}"));
	}

}
