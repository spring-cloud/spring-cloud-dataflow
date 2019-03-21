/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.rest.documentation;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.batch.listener.support.JdbcTaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import static org.springframework.cloud.dataflow.core.ApplicationType.task;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the /jobs/executions endpoint.
 *
 * @author Glenn Renfro
 */

public class JobExecutionsDocumentation extends BaseDocumentation {

	private final static String JOB_NAME = "DOCJOB";

	private static boolean initialized;
	private JobRepository jobRepository;
	private TaskExecutionDao dao;
	private TaskBatchDao taskBatchDao;

	@Before
	public void setup() throws Exception {
		if (!initialized) {
			registerApp(task, "timestamp");
			initialize();
			createJobExecution(JOB_NAME, BatchStatus.STARTED);
			createJobExecution(JOB_NAME + "_1", BatchStatus.STOPPED);

			documentation.dontDocument(() -> this.mockMvc.perform(
					post("/tasks/definitions")
							.param("name", "DOCJOB_1")
							.param("definition", "timestamp --format='YYYY MM DD'"))
					.andExpect(status().isOk()));

			initialized = true;
		}
	}

	@Test
	public void listJobExecutions() throws Exception {
		this.mockMvc.perform(
				get("/jobs/executions")
						.param("page", "0")
						.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
				requestParameters(
						parameterWithName("page")
								.description("The zero-based page number (optional)"),
						parameterWithName("size")
								.description("The requested page size (optional)")),
				responseFields(
						fieldWithPath("_embedded.jobExecutionResourceList")
								.description("Contains a collection of Job Executions/"),
						fieldWithPath("_links.self").description("Link to the job execution resource"),
						fieldWithPath("page").description("Pagination properties"))));
	}

	@Test
	public void listJobExecutionsByName() throws Exception {
		this.mockMvc.perform(
				get("/jobs/executions")
						.param("name", JOB_NAME)
						.param("page", "0")
						.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
				requestParameters(
						parameterWithName("page")
								.description("The zero-based page number (optional)"),
						parameterWithName("size")
								.description("The requested page size (optional)"),
						parameterWithName("name")
								.description("The name associated with the job execution")),
				responseFields(
						fieldWithPath("_embedded.jobExecutionResourceList")
								.description("Contains a collection of Job Executions/"),
						fieldWithPath("_links.self").description("Link to the job execution resource"),
						fieldWithPath("page").description("Pagination properties"))));
	}

	@Test
	public void jobDisplayDetail() throws Exception {
		this.mockMvc.perform(
				get("/jobs/executions/{id}", "2"))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("id")
								.description("The id of an existing job execution (required)"))));
	}

	@Test
	public void jobStop() throws Exception {
		this.mockMvc.perform(put("/jobs/executions/{id}", "1").accept(MediaType.APPLICATION_JSON).param("stop", "true"))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("id")
								.description("The id of an existing job execution (required)"))
						, requestParameters(
								parameterWithName("stop")
										.description("Sends signal to stop the job if set to true"))));
	}

	@Test
	public void jobRestart() throws Exception {
		this.mockMvc.perform(put("/jobs/executions/{id}", "2").accept(MediaType.APPLICATION_JSON).param("restart", "true"))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("id")
								.description("The id of an existing job execution (required)"))
						, requestParameters(
								parameterWithName("restart")
										.description("Sends signal to restart the job if set to true"))));
	}

	private void initialize() throws Exception {
		JobRepositoryFactoryBean repositoryFactoryBean = new JobRepositoryFactoryBean();
		repositoryFactoryBean.setDataSource(this.dataSource);
		repositoryFactoryBean.setTransactionManager(new DataSourceTransactionManager(this.dataSource));
		this.jobRepository = repositoryFactoryBean.getObject();
		this.dao = (new TaskExecutionDaoFactoryBean(this.dataSource)).getObject();
		this.taskBatchDao = new JdbcTaskBatchDao(this.dataSource);
	}

	private void createJobExecution(String name, BatchStatus status) {
		TaskExecution taskExecution = this.dao.createTaskExecution(name, new Date(), new ArrayList<>(), null);
		JobExecution jobExecution = this.jobRepository.createJobExecution(this.jobRepository.createJobInstance(name, new JobParameters()), new JobParameters(), null);
		this.taskBatchDao.saveRelationship(taskExecution, jobExecution);
		jobExecution.setStatus(status);
		jobExecution.setStartTime(new Date());
		this.jobRepository.update(jobExecution);
	}

}
