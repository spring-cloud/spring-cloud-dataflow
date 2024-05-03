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

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the /jobs/executions/{id}/steps endpoint.
 *
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@SuppressWarnings({"NewClassNamingConvention", "SameParameterValue"})
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EmbeddedDataSourceConfiguration.class})
@DirtiesContext
public class JobStepExecutionsDocumentation extends BaseDocumentation {

	private final static String JOB_NAME = "DOCJOB";

	private static boolean initialized;

	private JobRepository jobRepository;

	private TaskExecutionDao taskExecutionDao;

	private TaskBatchDao taskBatchDao;


	@Before
	public void setup() throws Exception {
		if (!initialized) {
			registerApp(ApplicationType.task, "timestamp", "1.2.0.RELEASE");
			initialize();
			createJobExecution(JOB_NAME, BatchStatus.STARTED);

			documentation.dontDocument(() -> this.mockMvc.perform(
							post("/tasks/definitions")
									.param("name", "DOCJOB1")
									.param("definition", "timestamp --format='YYYY MM DD'"))
					.andExpect(status().isOk()));

			initialized = true;
		}
	}


	@Test
	public void listStepExecutionsForJob() throws Exception {
		this.mockMvc.perform(
						get("/jobs/executions/{id}/steps", "1")
								.param("page", "0")
								.param("size", "10"))
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)")),
						pathParameters(parameterWithName("id")
								.description("The id of an existing job execution (required)")),
						responseFields(
								subsectionWithPath("_embedded.stepExecutionResourceList")
										.description("Contains a collection of Step Executions/"),
								subsectionWithPath("_links.self").description("Link to the job execution resource"),
								subsectionWithPath("page").description("Pagination properties"))));
	}

	@Test
	public void stepDetail() throws Exception {
		this.mockMvc.perform(
						get("/jobs/executions/{id}/steps/{stepid}", "1", "1"))
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
						pathParameters(
								parameterWithName("id").description("The id of an existing job execution (required)"),
								parameterWithName("stepid")
										.description("The id of an existing step execution for a specific job execution (required)")
						),
						responseFields(
								fieldWithPath("jobExecutionId").description("The ID of the job step execution"),
								fieldWithPath("stepType").description("The type of the job step execution"),
								subsectionWithPath("stepExecution").description("The step details of the job step execution"),
								subsectionWithPath("_links.self").description("Link to the job step execution resource"),
								subsectionWithPath("_links.progress").description("Link to retrieve the progress")
						)
				));
	}

	@Ignore("TODO: Boot3x followup : Need to create DataflowSqlPagingQueryProvider so that dataflow can call generateJumpToItemQuery")
	@Test
	public void stepProgress() throws Exception {
		this.mockMvc.perform(
						get("/jobs/executions/{id}/steps/{stepid}/progress", "1", "1"))
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
						pathParameters(
								parameterWithName("id").description("The id of an existing job execution (required)"),
								parameterWithName("stepid")
										.description("The id of an existing step execution for a specific job execution (required)")
						),
						responseFields(
								subsectionWithPath("stepExecution").description("The detailed step details of the job step execution"),
								subsectionWithPath("stepExecutionHistory")
										.description("The history of the job step execution"),
								fieldWithPath("percentageComplete").description("The percentage complete of the job step execution"),
								fieldWithPath("finished").description("The status finished of the job step execution"),
								fieldWithPath("duration").description("The duration of the job step execution"),
								subsectionWithPath("_links.self").description("Link to the job step execution resource"),
								subsectionWithPath("_links.progress").description("Link to the job step progress")
						)
				));
	}


	private void initialize() {
		this.jobRepository = context.getBean(JobRepository.class);
		this.taskExecutionDao = context.getBean(TaskExecutionDao.class);
		this.taskBatchDao = context.getBean(TaskBatchDao.class);
	}

	private void createJobExecution(String name, BatchStatus status) throws JobInstanceAlreadyCompleteException,
		JobExecutionAlreadyRunningException, JobRestartException {
		TaskExecution taskExecution = taskExecutionDao.createTaskExecution(name, LocalDateTime.now(), new ArrayList<>(), null);
		JobExecution jobExecution = jobRepository.createJobExecution(name, new JobParameters());
		StepExecution stepExecution = new StepExecution(name + "_STEP", jobExecution, jobExecution.getId());
		stepExecution.setId(null);
		jobRepository.add(stepExecution);
		taskBatchDao.saveRelationship(taskExecution, jobExecution);
		jobExecution.setStatus(status);
		jobExecution.setStartTime(LocalDateTime.now());
		jobRepository.update(jobExecution);
	}
}
