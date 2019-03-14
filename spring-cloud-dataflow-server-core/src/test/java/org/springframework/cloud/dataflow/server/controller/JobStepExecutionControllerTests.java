/*
 * Copyright 2016-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.Date;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.rest.support.jackson.ExecutionContextJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.ISO8601DateFormatWithMilliSeconds;
import org.springframework.cloud.dataflow.rest.support.jackson.StepExecutionJacksonMixIn;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.JobDependencies;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { JobDependencies.class,
		PropertyPlaceholderAutoConfiguration.class, BatchProperties.class })
@EnableConfigurationProperties({ CommonApplicationProperties.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class JobStepExecutionControllerTests {

	private final static String BASE_JOB_NAME = "myJob";

	private final static String JOB_NAME_ORIG = BASE_JOB_NAME + "_ORIG";

	private final static String JOB_NAME_FOO = BASE_JOB_NAME + "_FOO";

	private final static String JOB_NAME_FOOBAR = BASE_JOB_NAME + "_FOOBAR";

	private final static String BASE_STEP_NAME = "myStep";

	private final static String STEP_NAME_ORIG = BASE_STEP_NAME + "_ORIG";

	private final static String STEP_NAME_FOO = BASE_STEP_NAME + "_FOO";

	private final static String STEP_NAME_FOOBAR = BASE_STEP_NAME + "_FOOBAR";

	private boolean initialized = false;

	@Autowired
	private TaskExecutionDao dao;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private TaskBatchDao taskBatchDao;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private RequestMappingHandlerAdapter adapter;

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		if (!initialized) {
			createStepExecution(JOB_NAME_ORIG, STEP_NAME_ORIG);
			createStepExecution(JOB_NAME_FOO, STEP_NAME_ORIG, STEP_NAME_FOO);
			createStepExecution(JOB_NAME_FOOBAR, STEP_NAME_ORIG, STEP_NAME_FOO, STEP_NAME_FOOBAR);
			initialized = true;
		}
		for (HttpMessageConverter<?> converter : adapter.getMessageConverters()) {
			if (converter instanceof MappingJackson2HttpMessageConverter) {
				final MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
				jacksonConverter.getObjectMapper().addMixIn(StepExecution.class, StepExecutionJacksonMixIn.class);
				jacksonConverter.getObjectMapper().addMixIn(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
				jacksonConverter.getObjectMapper().setDateFormat(new ISO8601DateFormatWithMilliSeconds());
			}
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJobStepExecutionControllerConstructorMissingRepository() {
		new JobStepExecutionController(null);
	}

	@Test
	public void testGetExecutionNotFound() throws Exception {
		mockMvc.perform(get("/jobs/executions/1342434234/steps").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testSingleGetStepExecution() throws Exception {
		validateStepDetail(1, 1, STEP_NAME_ORIG);
		validateStepDetail(2, 2 ,STEP_NAME_ORIG);
		validateStepDetail(2, 3 ,STEP_NAME_FOO);
		validateStepDetail(3, 4 ,STEP_NAME_ORIG);
		validateStepDetail(3, 5 ,STEP_NAME_FOO);
		validateStepDetail(3, 6 ,STEP_NAME_FOOBAR);
	}

	private void validateStepDetail(int jobId, int stepId, String contextValue) throws Exception{
		mockMvc.perform(get(String.format("/jobs/executions/%d/steps/%d", jobId, stepId)).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(content().json(String.format("{jobExecutionId: %d}", jobId)))
				.andExpect(content().string(Matchers.containsString(String.format("{\"stepval\":\"%s\"}", contextValue))));
	}

	@Test
	public void testGetMultipleStepExecutions() throws Exception {
		mockMvc.perform(get("/jobs/executions/3/steps").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*]", hasSize(3)))
				.andExpect(jsonPath("$.content[0].stepExecution.id", is(4)))
				.andExpect(jsonPath("$.content[1].stepExecution.id", is(5)))
				.andExpect(jsonPath("$.content[2].stepExecution.id", is(6)));
	}

	@Test
	public void testSingleGetStepExecutionProgress() throws Exception {
		mockMvc.perform(get("/jobs/executions/1/steps/1/progress").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(content().json("{finished: " + false + "}"))
				.andExpect(content().json("{percentageComplete: " + 0.5 + "}"))
				.andExpect(jsonPath("$.stepExecutionHistory.count", is(0)))
				.andExpect(jsonPath("$.stepExecutionHistory.commitCount.count", is(0)));
	}

	private void createStepExecution(String jobName, String... stepNames) {
		JobInstance instance = jobRepository.createJobInstance(jobName, new JobParameters());
		JobExecution jobExecution = jobRepository.createJobExecution(instance, new JobParameters(), null);
		for (String stepName : stepNames) {
			StepExecution stepExecution = new StepExecution(stepName, jobExecution, 1L);
			stepExecution.setId(null);
			ExecutionContext context = new ExecutionContext();
			context.put("stepval", stepName);
			stepExecution.setExecutionContext(context);
			jobRepository.add(stepExecution);
		}
		TaskExecution taskExecution = dao.createTaskExecution(jobName, new Date(), new ArrayList<String>(), null);
		taskBatchDao.saveRelationship(taskExecution, jobExecution);
	}
}
