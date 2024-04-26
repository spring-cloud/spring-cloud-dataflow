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

import java.util.Date;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
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
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
@SpringBootTest(classes = {JobDependencies.class,
		PropertyPlaceholderAutoConfiguration.class, BatchProperties.class})
@EnableConfigurationProperties({CommonApplicationProperties.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class JobExecutionControllerTests {

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
	RequestMappingHandlerAdapter adapter;

	@Autowired
	AggregateExecutionSupport aggregateExecutionSupport;

	@Autowired
	TaskDefinitionReader taskDefinitionReader;

	@BeforeEach
	public void setupMockMVC() {
		this.mockMvc = JobExecutionUtils.createBaseJobExecutionMockMvc(
				jobRepositoryContainer,
				taskBatchDaoContainer,
				daoContainer,
				aggregateExecutionSupport,
				taskDefinitionReader,
				wac,
				adapter
		);
	}

	@Test
	public void testJobExecutionControllerConstructorMissingRepository() {
		assertThatIllegalArgumentException().isThrownBy(() ->new JobExecutionController(null));
	}

	@Test
	public void testGetExecutionNotFound() throws Exception {
		mockMvc.perform(get("/jobs/executions/1345345345345").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testStopNonExistingJobExecution() throws Exception {
		mockMvc.perform(put("/jobs/executions/1345345345345").accept(MediaType.APPLICATION_JSON).param("stop", "true"))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testRestartNonExistingJobExecution() throws Exception {
		mockMvc.perform(
						put("/jobs/executions/1345345345345").accept(MediaType.APPLICATION_JSON).param("restart", "true"))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testRestartCompletedJobExecution() throws Exception {
		mockMvc.perform(put("/jobs/executions/5").accept(MediaType.APPLICATION_JSON).param("restart", "true"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	public void testStopStartedJobExecution() throws Exception {
		mockMvc.perform(put("/jobs/executions/6").accept(MediaType.APPLICATION_JSON).param("stop", "true"))
				.andExpect(status().isOk());
	}

	@Test
	public void testStopStartedJobExecutionWithInvalidSchema() throws Exception {
		mockMvc.perform(put("/jobs/executions/6").accept(MediaType.APPLICATION_JSON)
						.param("stop", "true")
						.queryParam("schemaTarget", "foo"))
				.andExpect(status().is4xxClientError());
	}


	@Test
	public void testStopStartedJobExecutionTwice() throws Exception {
		mockMvc.perform(put("/jobs/executions/6").accept(MediaType.APPLICATION_JSON).param("stop", "true"))
				.andExpect(status().isOk());
		SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget(JobExecutionUtils.JOB_NAME_STARTED, taskDefinitionReader);
		JobRepository jobRepository = jobRepositoryContainer.get(schemaVersionTarget.getName());
		final JobExecution jobExecution = jobRepository.getLastJobExecution(JobExecutionUtils.JOB_NAME_STARTED,
				new JobParameters());
		assertThat(jobExecution).isNotNull();
		assertThat(jobExecution.getId()).isEqualTo(Long.valueOf(6));
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.STOPPING);

		mockMvc.perform(put("/jobs/executions/6").accept(MediaType.APPLICATION_JSON).param("stop", "true"))
				.andExpect(status().isOk());
	}

	@Test
	public void testStopStoppedJobExecution() throws Exception {
		mockMvc.perform(put("/jobs/executions/7").accept(MediaType.APPLICATION_JSON).param("stop", "true"))
				.andExpect(status().isUnprocessableEntity());
		SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget(JobExecutionUtils.JOB_NAME_STOPPED, taskDefinitionReader);
		JobRepository jobRepository = jobRepositoryContainer.get(schemaVersionTarget.getName());
		final JobExecution jobExecution = jobRepository.getLastJobExecution(JobExecutionUtils.JOB_NAME_STOPPED,
				new JobParameters());
		assertThat(jobExecution).isNotNull();
		assertThat(jobExecution.getId()).isEqualTo(Long.valueOf(7));
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.STOPPED);

	}

	@Test
	public void testGetExecution() throws Exception {
		mockMvc.perform(get("/jobs/executions/1").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.executionId", is(1)))
				.andExpect(jsonPath("$.jobExecution.stepExecutions", hasSize(1)));
	}

	@Test
	public void testGetExecutionWithJobProperties() throws Exception {
		MvcResult result = mockMvc.perform(get("/jobs/executions/10").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.executionId", is(10)))
			.andExpect(jsonPath("$.jobExecution.jobParameters.parameters", Matchers.hasKey(("javaUtilDate"))))
			.andExpect(jsonPath("$.jobExecution.stepExecutions", hasSize(1))).andReturn();
		assertThat(result.getResponse().getContentAsString()).contains("\"identifying\":true");
		assertThat(result.getResponse().getContentAsString()).contains("\"type\":\"DATE\"");
	}

	@Test
	public void testGetAllExecutionsFailed() throws Exception {
		createDirtyJob();
		// expecting to ignore dirty job
		mockMvc.perform(get("/jobs/executions/").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList", hasSize(10)));
	}

	@Test
	public void testGetAllExecutions() throws Exception {
		mockMvc.perform(get("/jobs/executions/").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList", hasSize(10)))
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList[*].executionId", containsInRelativeOrder(10, 9, 8, 7, 6, 5, 4, 3, 2, 1)));
	}

	@Test
	public void testGetAllExecutionsPageOffsetLargerThanIntMaxValue() throws Exception {
		verify5XXErrorIsThrownForPageOffsetError(get("/jobs/executions/"));
		verifyBorderCaseForMaxInt(get("/jobs/executions/"));
	}

	@Test
	public void testGetExecutionsByName() throws Exception {
		mockMvc.perform(get("/jobs/executions/").param("name", JobExecutionUtils.JOB_NAME_ORIG)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(
						jsonPath("$._embedded.jobExecutionResourceList[0].jobExecution.jobInstance.jobName", is(JobExecutionUtils.JOB_NAME_ORIG)))
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList", hasSize(1)));
	}

	@Test
	public void testGetExecutionsByNamePageOffsetLargerThanIntMaxValue() throws Exception {
		verify5XXErrorIsThrownForPageOffsetError(
				get("/jobs/executions/").param("name", JobExecutionUtils.JOB_NAME_ORIG));
		verifyBorderCaseForMaxInt(get("/jobs/executions/").param("name", JobExecutionUtils.JOB_NAME_ORIG));
	}

	@Test
	public void testGetExecutionsByNameMultipleResult() throws Exception {
		mockMvc.perform(get("/jobs/executions/").param("name", JobExecutionUtils.JOB_NAME_FOOBAR)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList[0].jobExecution.jobInstance.jobName",
						is(JobExecutionUtils.JOB_NAME_FOOBAR)))
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList[1].jobExecution.jobInstance.jobName",
						is(JobExecutionUtils.JOB_NAME_FOOBAR)))
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList", hasSize(2)));
	}

	@Test
	public void testFilteringByStatusAndName_EmptyNameAndStatusGiven() throws Exception {
		mockMvc.perform(get("/jobs/executions/")
						.param("name", "")
						.param("status", "FAILED")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList[0].jobExecution.jobInstance.jobName",
						is(JobExecutionUtils.JOB_NAME_FAILED2)))
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList[1].jobExecution.jobInstance.jobName",
						is(JobExecutionUtils.JOB_NAME_FAILED1)))
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList", hasSize(2)));
	}

	@Test
	public void testFilteringByUnknownStatus() throws Exception {
		mockMvc.perform(get("/jobs/executions/")
						.param("status", "UNKNOWN")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList", hasSize(5)));
	}

	@Test
	public void testFilteringByStatusAndName_NameAndStatusGiven() throws Exception {
		mockMvc.perform(get("/jobs/executions/")
						.param("name", JobExecutionUtils.BASE_JOB_NAME + "%")
						.param("status", "COMPLETED")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList[0].jobExecution.jobInstance.jobName",
						is(JobExecutionUtils.JOB_NAME_COMPLETED)))
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList", hasSize(1)));
	}

	@Test
	public void testGetExecutionsByNameNotFound() throws Exception {
		mockMvc.perform(get("/jobs/executions/").param("name", "BAZ").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testWildcardMatchMultipleResult() throws Exception {
		mockMvc.perform(get("/jobs/executions/")
						.param("name", JobExecutionUtils.BASE_JOB_NAME + "_FOO_ST%").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList[0].jobExecution.jobInstance.jobName",
						is(JobExecutionUtils.JOB_NAME_STOPPED)))
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList[1].jobExecution.jobInstance.jobName",
						is(JobExecutionUtils.JOB_NAME_STARTED)))
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList", hasSize(2)));
	}

	@Test
	public void testWildcardMatchSingleResult() throws Exception {
		mockMvc.perform(get("/jobs/executions/")
						.param("name", "m_Job_ORIG").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.jobExecutionResourceList", hasSize(1)))
				.andExpect(
						jsonPath("$._embedded.jobExecutionResourceList[0].jobExecution.jobInstance.jobName", is(JobExecutionUtils.JOB_NAME_ORIG))
				);
	}

	private void createDirtyJob() {
		JobRepository jobRepository = jobRepositoryContainer.get(SchemaVersionTarget.defaultTarget().getName());
		JobInstance instance = jobRepository.createJobInstance(JobExecutionUtils.BASE_JOB_NAME + "_NO_TASK",
				new JobParameters());
		JobExecution jobExecution = jobRepository.createJobExecution(
				instance, new JobParameters(), null);
		jobExecution.setStatus(BatchStatus.STOPPED);
		jobExecution.setEndTime(new Date());
		jobRepository.update(jobExecution);
	}

	private void verify5XXErrorIsThrownForPageOffsetError(MockHttpServletRequestBuilder builder) throws Exception {
		mockMvc.perform(builder.param("page", String.valueOf(Integer.MAX_VALUE))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError())
				.andReturn().getResponse().getContentAsString()
				.contains("OffsetOutOfBoundsException");
	}

	private void verifyBorderCaseForMaxInt(MockHttpServletRequestBuilder builder) throws Exception {
		mockMvc.perform(builder.param("page", String.valueOf(Integer.MAX_VALUE - 1))
				.param("size", "1")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}
}
