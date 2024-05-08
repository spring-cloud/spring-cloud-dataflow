/*
 * Copyright 2016-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.JobDependencies;
import org.springframework.cloud.dataflow.server.repository.JobRepositoryContainer;
import org.springframework.cloud.dataflow.server.repository.TaskBatchDaoContainer;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionDaoContainer;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = {JobDependencies.class,
	PropertyPlaceholderAutoConfiguration.class, BatchProperties.class})
@EnableConfigurationProperties({CommonApplicationProperties.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class JobInstanceControllerTests {

	private final static String BASE_JOB_NAME = "myJob";

	private final static String JOB_NAME_ORIG = BASE_JOB_NAME + "_ORIG";

	private final static String JOB_NAME_FOO = BASE_JOB_NAME + "_FOO";

	private final static String JOB_NAME_FOOBAR = BASE_JOB_NAME + "_FOOBAR";

	private boolean initialized = false;

	@Autowired
	TaskExecutionDaoContainer daoContainer;

	@Autowired
	JobRepositoryContainer jobRepositoryContainer;

	@Autowired
	TaskBatchDaoContainer taskBatchDaoContainer;

	private MockMvc mockMvc;

	@Autowired
	WebApplicationContext wac;

	@Autowired
	AggregateExecutionSupport aggregateExecutionSupport;

	@Autowired
	TaskDefinitionReader taskDefinitionReader;

	@BeforeEach
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
			.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		if (!initialized) {
			createSampleJob(JOB_NAME_ORIG, 1);
			createSampleJob(JOB_NAME_FOO, 1);
			createSampleJob(JOB_NAME_FOOBAR, 2);
			initialized = true;
		}
	}

	@Test
	public void testJobInstanceControllerConstructorMissingRepository() {
		assertThrows(IllegalArgumentException.class, () -> {
			new JobInstanceController(null);
		});
	}

	@Test
	public void testGetInstanceNotFound() throws Exception {
		mockMvc.perform(get("/jobs/instances/1345345345345").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound());
	}

	@Test
	public void testGetInstance() throws Exception {
		mockMvc.perform(get("/jobs/instances/1").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.jobInstanceId", equalTo(1)))
			.andExpect(jsonPath("$.jobExecutions[0].stepExecutionCount", equalTo(1)))
			.andExpect(jsonPath("$.jobExecutions[0].jobExecution.stepExecutions", hasSize(1)));
	}

	@Test
	public void testGetInstancesByName() throws Exception {
		mockMvc.perform(get("/jobs/instances/").param("name", JOB_NAME_ORIG).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$._embedded.jobInstanceResourceList[0].jobName", is(JOB_NAME_ORIG)))
			.andExpect(jsonPath("$._embedded.jobInstanceResourceList", hasSize(1)));
	}

	@Test
	public void testGetExecutionsByNameMultipleResult() throws Exception {
		mockMvc.perform(get("/jobs/instances/").param("name", JOB_NAME_FOOBAR).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$._embedded.jobInstanceResourceList[0].jobName", is(JOB_NAME_FOOBAR)))
			.andExpect(jsonPath("$._embedded.jobInstanceResourceList[0].jobExecutions[0].executionId", is(4)))
			.andExpect(jsonPath("$._embedded.jobInstanceResourceList[0].jobExecutions[1].executionId", is(3)))
			.andExpect(jsonPath("$._embedded.jobInstanceResourceList", hasSize(1)));
	}

	@Test
	public void testGetInstanceByNameNotFound() throws Exception {
		mockMvc.perform(get("/jobs/instances/").param("name", "BAZ").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().is4xxClientError())
			.andExpect(content().string(containsString("NoSuchJobException")));
	}

	private void createSampleJob(String jobName, int jobExecutionCount) {
		String defaultSchemaTarget = SchemaVersionTarget.defaultTarget().getName();
		JobRepository jobRepository = jobRepositoryContainer.get(defaultSchemaTarget);
		JobInstance instance = jobRepository.createJobInstance(jobName, new JobParameters());

		TaskExecutionDao dao = daoContainer.get(defaultSchemaTarget);
		TaskExecution taskExecution = dao.createTaskExecution(jobName, new Date(), new ArrayList<String>(), null);

		TaskBatchDao taskBatchDao = taskBatchDaoContainer.get(defaultSchemaTarget);
		for (int i = 0; i < jobExecutionCount; i++) {
			JobExecution jobExecution = jobRepository.createJobExecution(instance, new JobParameters(), null);
			StepExecution stepExecution = new StepExecution("foo", jobExecution, 1L);
			stepExecution.setId(null);
			jobRepository.add(stepExecution);
			taskBatchDao.saveRelationship(taskExecution, jobExecution);
		}
	}
}
