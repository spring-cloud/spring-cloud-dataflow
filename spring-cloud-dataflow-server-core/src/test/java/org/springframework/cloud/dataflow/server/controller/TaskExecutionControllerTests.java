/*
 * Copyright 2016-2017 the original author or authors.
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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.server.configuration.JobDependencies;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { EmbeddedDataSourceConfiguration.class, JobDependencies.class,
		PropertyPlaceholderAutoConfiguration.class, BatchProperties.class })
@DirtiesContext
public class TaskExecutionControllerTests {

	private final static String BASE_TASK_NAME = "myTask";

	private final static String TASK_NAME_ORIG = BASE_TASK_NAME + "_ORIG";

	private final static String TASK_NAME_FOO = BASE_TASK_NAME + "_FOO";

	private final static String TASK_NAME_FOOBAR = BASE_TASK_NAME + "_FOOBAR";

	private static boolean initialized = false;

	private static List sampleArgumentList;

	private static List sampleCleansedArgumentList;

	@Autowired
	private TaskExecutionDao dao;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	private TaskBatchDao taskBatchDao;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private TaskExplorer taskExplorer;

	@Autowired
	private TaskService taskService;

	@Autowired
	private TaskLauncher taskLauncher;

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		if (!initialized) {
			this.sampleArgumentList = new LinkedList<String>();
			this.sampleArgumentList.add("--password=foo");
			this.sampleArgumentList.add("password=bar");
			this.sampleArgumentList.add("org.woot.password=baz");
			this.sampleArgumentList.add("foo.bar=foo");
			this.sampleArgumentList.add("bar.baz = boo");
			this.sampleArgumentList.add("foo.credentials.boo=bar");
			this.sampleArgumentList.add("spring.datasource.username=dbuser");
			this.sampleArgumentList.add("spring.datasource.password=dbpass");

			this.sampleCleansedArgumentList = new LinkedList<String>();
			this.sampleCleansedArgumentList.add("--password=******");
			this.sampleCleansedArgumentList.add("password=******");
			this.sampleCleansedArgumentList.add("org.woot.password=******");
			this.sampleCleansedArgumentList.add("foo.bar=foo");
			this.sampleCleansedArgumentList.add("bar.baz = boo");
			this.sampleCleansedArgumentList.add("foo.credentials.boo=******");
			this.sampleCleansedArgumentList.add("spring.datasource.username=dbuser");
			this.sampleCleansedArgumentList.add("spring.datasource.password=******");

			taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
			dao.createTaskExecution(TASK_NAME_ORIG, new Date(), this.sampleArgumentList, "foobar");
			dao.createTaskExecution(TASK_NAME_ORIG, new Date(), this.sampleArgumentList, null);
			dao.createTaskExecution(TASK_NAME_FOO, new Date(), this.sampleArgumentList, null);
			TaskExecution taskExecution = dao.createTaskExecution(TASK_NAME_FOOBAR, new Date(), this.sampleArgumentList,
					null);
			JobInstance instance = jobRepository.createJobInstance(TASK_NAME_FOOBAR, new JobParameters());
			JobExecution jobExecution = jobRepository.createJobExecution(instance, new JobParameters(), null);
			taskBatchDao.saveRelationship(taskExecution, jobExecution);
			initialized = true;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskExecutionControllerConstructorMissingExplorer() {
		new TaskExecutionController(null, taskService, taskDefinitionRepository);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskExecutionControllerConstructorMissingTaskService() {
		new TaskExecutionController(taskExplorer, null, taskDefinitionRepository);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskExecutionControllerConstructorMissingTaskDefinitionRepository() {
		new TaskExecutionController(taskExplorer, taskService, null);
	}

	@Test
	public void testGetExecutionNotFound() throws Exception {
		mockMvc.perform(get("/tasks/executions/1345345345345").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testGetExecution() throws Exception {
		verifyTaskArgs(sampleCleansedArgumentList, "",
				mockMvc.perform(get("/tasks/executions/1").accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk()).andExpect(content().json("{taskName: \"" + TASK_NAME_ORIG + "\"}"))
						.andExpect(jsonPath("jobExecutionIds", hasSize(0))));
	}

	@Test
	public void testGetExecutionForJob() throws Exception {
		mockMvc.perform(get("/tasks/executions/4").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(content().json("{taskName: \"" + TASK_NAME_FOOBAR + "\"}"))
				.andExpect(jsonPath("jobExecutionIds[0]", is(1))).andExpect(jsonPath("jobExecutionIds", hasSize(1)));
	}

	@Test
	public void testGetAllExecutions() throws Exception {
		verifyTaskArgs(this.sampleCleansedArgumentList, "$.content[0].",
				mockMvc.perform(get("/tasks/executions/").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
						.andExpect(jsonPath("$.content[*].executionId", containsInAnyOrder(4, 3, 2, 1)))
						.andExpect(jsonPath("$.content", hasSize(4))));
	}

	@Test
	public void testGetExecutionsByName() throws Exception {
		verifyTaskArgs(this.sampleCleansedArgumentList, "$.content[0].", mockMvc
				.perform(get("/tasks/executions/").param("name", TASK_NAME_ORIG).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].taskName", is(TASK_NAME_ORIG)))
				.andExpect(jsonPath("$.content[1].taskName", is(TASK_NAME_ORIG)))
				.andExpect(jsonPath("$.content[0].jobExecutionIds", hasSize(0)))
				.andExpect(jsonPath("$.content[1].jobExecutionIds", hasSize(0)))
				.andExpect(jsonPath("$.content", hasSize(2))));
	}

	@Test
	public void testGetExecutionsByNameNotFound() throws Exception {
		mockMvc.perform(get("/tasks/executions/").param("name", "BAZ").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString()
				.contains("NoSuchTaskException");
	}

	@Test
	public void testCleanup() throws Exception {
		mockMvc.perform(delete("/tasks/executions/1")).andExpect(status().is(200));

		verify(taskLauncher).cleanup("foobar");
	}

	@Test
	public void testCleanupByIdNotFound() throws Exception {
		mockMvc.perform(delete("/tasks/executions/10")).andExpect(status().is(404)).andReturn().getResponse()
				.getContentAsString().contains("NoSuchTaskExecutionException");
	}

	private ResultActions verifyTaskArgs(List<String> expectedArgs, String prefix, ResultActions ra) throws Exception {
		ra.andExpect(jsonPath(prefix + "arguments", hasSize(expectedArgs.size())));
		for (int argCount = 0; argCount < expectedArgs.size(); argCount++) {
			ra.andExpect(jsonPath(String.format(prefix + "arguments[%d]", argCount), is(expectedArgs.get(argCount))));
		}
		return ra;
	}
}
