/*
 * Copyright 2017-2023 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.server.repository.JobRepositoryContainer;
import org.springframework.cloud.dataflow.server.repository.TaskBatchDaoContainer;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionDaoContainer;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.test.annotation.DirtiesContext;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the /jobs/instances endpoint.
 *
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@SuppressWarnings({"NewClassNamingConvention", "SameParameterValue"})
@SpringBootTest(classes = { EmbeddedDataSourceConfiguration.class })
@DirtiesContext
public class JobInstancesDocumentation extends BaseDocumentation {

	private final static String JOB_NAME = "DOCJOB";

	private JobRepositoryContainer jobRepositoryContainer;
	private TaskExecutionDaoContainer daoContainer;
	private TaskBatchDaoContainer taskBatchDaoContainer;
	private AggregateExecutionSupport aggregateExecutionSupport;
	private TaskDefinitionReader taskDefinitionReader;

	@BeforeEach
	public void setup() throws Exception {
		registerApp(ApplicationType.task, "timestamp", "1.2.0.RELEASE");
		initialize();
		createJobExecution(JOB_NAME, BatchStatus.STARTED);
	}

	@Test
	public void listJobInstances() throws Exception {
		this.mockMvc.perform(
				get("/jobs/instances")
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
								.description("The name associated with the job instance")),
				responseFields(
						subsectionWithPath("_embedded.jobInstanceResourceList")
								.description("Contains a collection of Job Instances/"),
						subsectionWithPath("_links.self").description("Link to the job instance resource"),
						subsectionWithPath("page").description("Pagination properties"))));
	}

	@Test
	public void jobDisplayDetail() throws Exception {
		this.mockMvc.perform(
				get("/jobs/instances/{id}", "1").queryParam("schemaTarget", "boot2"))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
					pathParameters(
						parameterWithName("id").description("The id of an existing job instance (required)")
					),
					requestParameters(
							parameterWithName("schemaTarget").description("Schema target").optional()
					),
					responseFields(
						fieldWithPath("jobName").description("The name of the job instance"),
						fieldWithPath("jobInstanceId").description("The ID of the job instance"),
						subsectionWithPath("jobExecutions").description("The executions of the job instance"),
						subsectionWithPath("_links.self").description("Link to the job instance resource")
					)
				));
	}


	private void initialize() {
		this.taskDefinitionReader = context.getBean(TaskDefinitionReader.class);
		this.aggregateExecutionSupport = context.getBean(AggregateExecutionSupport.class);
		this.jobRepositoryContainer = context.getBean(JobRepositoryContainer.class);
		this.daoContainer = context.getBean(TaskExecutionDaoContainer.class);
		this.taskBatchDaoContainer = context.getBean(TaskBatchDaoContainer.class);
	}

	private void createJobExecution(String name, BatchStatus status) {
		SchemaVersionTarget schemaVersionTarget = this.aggregateExecutionSupport.findSchemaVersionTarget(name, taskDefinitionReader);
		TaskExecutionDao dao = this.daoContainer.get(schemaVersionTarget.getName());
		TaskExecution taskExecution = dao.createTaskExecution(name, new Date(), new ArrayList<>(), null);
		JobRepository jobRepository = this.jobRepositoryContainer.get(schemaVersionTarget.getName());
		JobExecution jobExecution = jobRepository.createJobExecution(jobRepository.createJobInstance(name, new JobParameters()), new JobParameters(), null);
		TaskBatchDao taskBatchDao = this.taskBatchDaoContainer.get(schemaVersionTarget.getName());
		taskBatchDao.saveRelationship(taskExecution, jobExecution);
		jobExecution.setStatus(status);
		jobExecution.setStartTime(new Date());
		jobRepository.update(jobExecution);
	}
}
