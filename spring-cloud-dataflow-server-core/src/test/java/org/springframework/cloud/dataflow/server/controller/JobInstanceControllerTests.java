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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.server.configuration.JobDependencies;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EmbeddedDataSourceConfiguration.class,
		JobDependencies.class,
		PropertyPlaceholderAutoConfiguration.class, BatchProperties.class})
@DirtiesContext
public class JobInstanceControllerTests {

	private final static String BASE_JOB_NAME = "myJob";

	private final static String JOB_NAME_ORIG = BASE_JOB_NAME + "_ORIG";

	private final static String JOB_NAME_FOO = BASE_JOB_NAME + "_FOO";

	private final static String JOB_NAME_FOOBAR = BASE_JOB_NAME + "_FOOBAR";

	private static boolean initialized = false;

	@Autowired
	private TaskExecutionDao dao;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private TaskBatchDao taskBatchDao;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultRequest(
				get("/").accept(MediaType.APPLICATION_JSON)).build();
		if (!initialized) {
			createSampleJob(JOB_NAME_ORIG, 1);
			createSampleJob(JOB_NAME_FOO, 1);
			createSampleJob(JOB_NAME_FOOBAR, 2);
			initialized = true;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJobInstanceControllerConstructorMissingRepository() {
		new JobInstanceController(null);
	}

	@Test
	public void testGetInstanceNotFound() throws Exception{
		mockMvc.perform(
				get("/jobs/instances/1345345345345").accept(MediaType.APPLICATION_JSON)
		).andExpect(status().isNotFound());
	}

	@Test
	public void testGetInstance() throws Exception{
		mockMvc.perform(
				get("/jobs/instances/1").accept(MediaType.APPLICATION_JSON)
		).andExpect(status().isOk()).andExpect(content().json("{jobInstanceId: " +
				1 + "}"));
	}

	@Test
	public void testGetInstancesByName() throws Exception{
		mockMvc.perform(
				get("/jobs/instances/").param("name", JOB_NAME_ORIG).accept(MediaType.APPLICATION_JSON)
		).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].jobName", is(JOB_NAME_ORIG)))
				.andExpect(jsonPath("$.content", hasSize(1)));
	}

	@Test
	public void testGetExecutionsByNameMultipleResult() throws Exception{
		mockMvc.perform(
				get("/jobs/instances/").param("name", JOB_NAME_FOOBAR).accept(MediaType.APPLICATION_JSON)
		).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].jobName", is(JOB_NAME_FOOBAR)))
				.andExpect(jsonPath("$.content[0].jobExecutions[0].executionId", is(4)))
				.andExpect(jsonPath("$.content[0].jobExecutions[1].executionId", is(3)))
				.andExpect(jsonPath("$.content", hasSize(1)));
	}

	@Test
	public void testGetInstanceByNameNotFound() throws Exception{
		mockMvc.perform(
				get("/jobs/instances/").param("name", "BAZ").accept(MediaType.APPLICATION_JSON)
		).andExpect(status().is4xxClientError())
				.andReturn().getResponse().getContentAsString().contains("NoSuchJobException");
	}

	private void createSampleJob(String jobName, int jobExecutionCount){
		JobInstance instance = jobRepository.createJobInstance(jobName, new JobParameters());
		TaskExecution taskExecution = dao.createTaskExecution(
				jobName, new Date(), new ArrayList<String>(), null);
		JobExecution jobExecution = null;

		for(int i = 0 ; i < jobExecutionCount; i++){
			jobExecution = jobRepository.createJobExecution(
					instance, new JobParameters(), null);
			taskBatchDao.saveRelationship(taskExecution,jobExecution);
		}
	}
}
