/*
 * Copyright 2018-2024 the original author or authors.
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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.server.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.rest.job.support.TimeUtils;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.JobDependencies;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Glenn Renfro
 * @author  Corneil du Plessis
 */

@SpringBootTest(classes = {JobDependencies.class,
		PropertyPlaceholderAutoConfiguration.class, BatchProperties.class})
@EnableConfigurationProperties({CommonApplicationProperties.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
class JobExecutionThinControllerTests {

	@Autowired
	private TaskExecutionDao taskExecutionDao;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private TaskBatchDao taskBatchDao;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	RequestMappingHandlerAdapter adapter;

	@Autowired
	TaskDefinitionReader taskDefinitionReader;

	@BeforeEach
	void setupMockMVC() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobRestartException {
		this.mockMvc = JobExecutionUtils.createBaseJobExecutionMockMvc(
				jobRepository,
				taskBatchDao,
				taskExecutionDao,
				taskDefinitionReader,
				wac,
				adapter
		);
	}

	@Test
	void jobExecutionThinControllerConstructorMissingRepository() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new JobExecutionThinController(null));
	}

	@Test
	void getAllExecutionsJobExecutionOnly() throws Exception {
		mockMvc.perform(get("/jobs/thinexecutions").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.jobExecutionThinResourceList[*].taskExecutionId", containsInAnyOrder(9, 8, 7, 6, 5, 4, 3, 3, 2, 1)))
				.andExpect(jsonPath("$._embedded.jobExecutionThinResourceList[0].stepExecutionCount", is(1)))
				.andExpect(jsonPath("$._embedded.jobExecutionThinResourceList", hasSize(10)));
	}

	@Test
	void getExecutionsByName() throws Exception {
		mockMvc.perform(get("/jobs/thinexecutions").param("name", JobExecutionUtils.JOB_NAME_ORIG)
				.accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.jobExecutionThinResourceList[0].name", is(JobExecutionUtils.JOB_NAME_ORIG)))
				.andExpect(jsonPath("$._embedded.jobExecutionThinResourceList", hasSize(1)));
	}

	@Test
	void getExecutionsByDateRange() throws Exception {
		Date toDate = new Date();
		final Date fromDate = DateUtils.addMinutes(toDate, -10);
		toDate = DateUtils.addMinutes(toDate, 10);
		mockMvc.perform(get("/jobs/thinexecutions")
				.param("fromDate",
						new SimpleDateFormat(TimeUtils.DEFAULT_DATAFLOW_DATE_TIME_PARAMETER_FORMAT_PATTERN)
								.format(fromDate))
				.param("toDate",
						new SimpleDateFormat(TimeUtils.DEFAULT_DATAFLOW_DATE_TIME_PARAMETER_FORMAT_PATTERN)
								.format(toDate))
				.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.jobExecutionThinResourceList", hasSize(10)))
				.andExpect(jsonPath("$._embedded.jobExecutionThinResourceList[*].taskExecutionId", containsInAnyOrder(9, 8, 7, 6, 5, 4, 3, 3, 2, 1)))
				.andExpect(jsonPath("$._embedded.jobExecutionThinResourceList[0].stepExecutionCount", is(1)));
	}

	@Test
	void getExecutionsByJobInstanceId() throws Exception {
		mockMvc.perform(get("/jobs/thinexecutions").param("jobInstanceId", "1")
				.accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.jobExecutionThinResourceList[0].name", is(JobExecutionUtils.JOB_NAME_ORIG)))
				.andExpect(jsonPath("$._embedded.jobExecutionThinResourceList[0].instanceId", is(1)))
				.andExpect(jsonPath("$._embedded.jobExecutionThinResourceList", hasSize(1)));
	}

	@Test
	void getExecutionsByTaskExecutionId() throws Exception {
		mockMvc.perform(get("/jobs/thinexecutions").param("taskExecutionId", "4")
				.accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.jobExecutionThinResourceList[0].taskExecutionId", is(4)))
				.andExpect(jsonPath("$._embedded.jobExecutionThinResourceList", hasSize(1)));
	}

}
