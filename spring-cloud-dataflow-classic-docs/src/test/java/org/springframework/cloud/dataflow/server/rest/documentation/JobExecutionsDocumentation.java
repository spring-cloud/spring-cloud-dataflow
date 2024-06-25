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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDao;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Documentation for the /jobs/executions endpoint.
 *
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EmbeddedDataSourceConfiguration.class})
@DirtiesContext
public class JobExecutionsDocumentation extends BaseDocumentation {

	private final static String JOB_NAME = "DOCJOB";

	private static boolean initialized;

	private JobRepository jobRepository;

	private TaskExecutionDao taskExecutionDao;

	private TaskBatchDao taskBatchDao;

	private JdbcTemplate jdbcTemplate;

	private DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao;


	@Before
	public void setup() throws Exception {
		if (!initialized) {
			registerApp(ApplicationType.task, "timestamp", "1.2.0.RELEASE");
			initialize();
			createJobExecution(JOB_NAME, BatchStatus.STARTED);
			createJobExecution(JOB_NAME + "1", BatchStatus.STOPPED);


			jdbcTemplate = new JdbcTemplate(this.dataSource);
			jdbcTemplate.afterPropertiesSet();
			jdbcTemplate.update(
					"INSERT into task_deployment(id, object_version, task_deployment_id, task_definition_name, platform_name, created_on) " +
							"values (?,?,?,?,?,?)",
					1, 1, "2", JOB_NAME + "_1", "default", new Date());

			documentation.dontDocument(() -> this.mockMvc.perform(
							post("/tasks/definitions")
									.queryParam("name", "DOCJOB1")
									.queryParam("definition", "timestamp --format='YYYY MM DD'"))
					.andExpect(status().isOk()));

			initialized = true;
		}
	}

	@Test
	public void listJobExecutions() throws Exception {
		this.mockMvc.perform(
						get("/jobs/executions")
								.queryParam("page", "0")
								.queryParam("size", "10"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)")),
						responseFields(
								subsectionWithPath("_embedded.jobExecutionResourceList")
										.description("Contains a collection of Job Executions/"),
								subsectionWithPath("_links.self").description("Link to the job execution resource"),
								subsectionWithPath("page").description("Pagination properties")
						)));
	}

	@Test
	public void listThinJobExecutions() throws Exception {
		this.mockMvc.perform(
						get("/jobs/thinexecutions")
								.queryParam("page", "0")
								.queryParam("size", "10"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)")),
						responseFields(
								subsectionWithPath("_embedded.jobExecutionThinResourceList")
										.description("Contains a collection of Job Executions without step executions included/"),
								subsectionWithPath("_links.self").description("Link to the job execution resource"),
								subsectionWithPath("page").description("Pagination properties")
						)));
	}

	@Test
	public void listThinJobExecutionsByJobInstanceId() throws Exception {
		this.mockMvc.perform(
						get("/jobs/thinexecutions")
								.queryParam("page", "0")
								.queryParam("size", "10")
								.queryParam("jobInstanceId", "1"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)"),
								parameterWithName("jobInstanceId")
										.description("Filter result by the job instance id")),
						responseFields(
								subsectionWithPath("_embedded.jobExecutionThinResourceList")
										.description("Contains a collection of Job Executions without step executions included/"),
								subsectionWithPath("_links.self").description("Link to the job execution resource"),
								subsectionWithPath("page").description("Pagination properties")
						)));
	}

	@Test
	public void listThinJobExecutionsByTaskExecutionId() throws Exception {
		this.mockMvc.perform(
						get("/jobs/thinexecutions")
								.queryParam("page", "0")
								.queryParam("size", "10")
								.queryParam("taskExecutionId", "1"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)"),
								parameterWithName("taskExecutionId")
										.description("Filter result by the task execution id")),
						responseFields(
								subsectionWithPath("_embedded.jobExecutionThinResourceList")
										.description("Contains a collection of Job Executions without step executions included/"),
								subsectionWithPath("_links.self").description("Link to the job execution resource"),
								subsectionWithPath("page").description("Pagination properties")
						)));
	}

	@Test
	public void listThinJobExecutionsByDate() throws Exception {
		this.mockMvc.perform(
						get("/jobs/thinexecutions")
								.queryParam("page", "0")
								.queryParam("size", "10")
								.queryParam("fromDate", "2000-09-24T17:00:45,000")
								.queryParam("toDate", "2050-09-24T18:00:45,000"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)"),
								parameterWithName("fromDate")
										.description("Filter result from a starting date in the format 'yyyy-MM-dd'T'HH:mm:ss,SSS'"),
								parameterWithName("toDate")
										.description("Filter result up to the `to` date in the format 'yyyy-MM-dd'T'HH:mm:ss,SSS'")),
						responseFields(
								subsectionWithPath("_embedded.jobExecutionThinResourceList")
										.description("Contains a collection of Job Executions without step executions included/"),
								subsectionWithPath("_links.self").description("Link to the job execution resource"),
								subsectionWithPath("page").description("Pagination properties")
						)));
	}

	@Test
	public void listJobExecutionsByName() throws Exception {
		this.mockMvc.perform(
						get("/jobs/executions")
								.queryParam("name", JOB_NAME)
								.queryParam("page", "0")
								.queryParam("size", "10"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)"),
								parameterWithName("name")
										.description("The name associated with the job execution")),
						responseFields(
								subsectionWithPath("_embedded.jobExecutionResourceList")
										.description("Contains a collection of Job Executions/"),
								subsectionWithPath("_links.self").description("Link to the job execution resource"),
								subsectionWithPath("page").description("Pagination properties")
						)));
	}

	@Test
	public void listThinJobExecutionsByName() throws Exception {
		this.mockMvc.perform(
						get("/jobs/thinexecutions")
								.queryParam("name", JOB_NAME)
								.queryParam("page", "0")
								.queryParam("size", "10"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)"),
								parameterWithName("name")
										.description("The name associated with the job execution")),
						responseFields(
								subsectionWithPath("_embedded.jobExecutionThinResourceList")
										.description("Contains a collection of Job Executions without step executions included/"),
								subsectionWithPath("_links.self").description("Link to the job execution resource"),
								subsectionWithPath("page").description("Pagination properties")
						)));
	}

	@Test
	public void jobDisplayDetail() throws Exception {
		this.mockMvc.perform(
						get("/jobs/executions/{id}", "2")
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(
								parameterWithName("id").description("The id of an existing job execution (required)")
						),
						responseFields(
								fieldWithPath("executionId").description("The execution ID of the job execution"),
								fieldWithPath("stepExecutionCount").description("the number of step of the job execution"),
								fieldWithPath("jobId").description("The job ID of the job execution"),
								fieldWithPath("taskExecutionId").description("The task execution ID of the job execution"),
								fieldWithPath("name").description("The name of the job execution"),
								fieldWithPath("startDate").description("The start date of the job execution"),
								fieldWithPath("startTime").description("The start time of the job execution"),
								fieldWithPath("duration").description("The duration of the job execution"),
								fieldWithPath("jobParameters").description("The parameters of the job execution"),
								fieldWithPath("jobParametersString").description("The parameters string of the job execution"),
								fieldWithPath("restartable").description("The status restartable of the job execution"),
								fieldWithPath("abandonable").description("The status abandonable of the job execution"),
								fieldWithPath("stoppable").description("The status stoppable of the job execution"),
								fieldWithPath("defined").description("The status defined of the job execution"),
								fieldWithPath("timeZone").description("The time zone of the job execution"),
								subsectionWithPath("jobExecution").description("The details of the job execution"),
								subsectionWithPath("jobParameters").description("The job parameters associated with the job execution"),
								subsectionWithPath("_links.self").description("Link to the stream definition resource"),
								subsectionWithPath("_links.stop").type(JsonFieldType.OBJECT).description("Link to stopping the job").optional(),
								subsectionWithPath("_links.restart").type(JsonFieldType.OBJECT).description("Link to restarting the job").optional()
						)
				));
	}

	@Test
	public void jobStop() throws Exception {
		this.mockMvc.perform(put("/jobs/executions/{id}", "1")
						.queryParam("stop", "true")
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("id")
								.description("The id of an existing job execution (required)"))
						, queryParameters(
								parameterWithName("stop")
										.description("Sends signal to stop the job if set to true"))));
	}

	@Test
	public void jobRestart() throws Exception {
		this.mockMvc.perform(put("/jobs/executions/{id}", "2")
						.queryParam("restart", "true")
						.queryParam("useJsonJobParameters", "true")
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
								pathParameters(parameterWithName("id")
										.description("The id of an existing job execution (required)"))
								, queryParameters(
										parameterWithName("useJsonJobParameters").description("If true dataflow will " +
											"serialize job parameters as JSON.  Default is null, and the default " +
											"configuration will be used to determine serialization method.").optional(),
										parameterWithName("restart")
												.description("Sends signal to restart the job if set to true")
								)
						)
				);
	}

	private void initialize() {
		this.taskExecutionDao = context.getBean(TaskExecutionDao.class);
		this.taskBatchDao = context.getBean(TaskBatchDao.class);
		this.jobRepository = context.getBean(JobRepository.class);
		this.dataflowTaskExecutionMetadataDao = context.getBean(DataflowTaskExecutionMetadataDao.class);
	}

	private void createJobExecution(String name, BatchStatus status) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobRestartException {
		TaskExecution taskExecution = taskExecutionDao.createTaskExecution(name, LocalDateTime.now(), Collections.singletonList("--spring.cloud.data.flow.platformname=default"), null);
		Map<String, JobParameter<?>> jobParameterMap = new HashMap<>();
		JobParameters jobParameters = new JobParameters(jobParameterMap);
		JobExecution jobExecution = this.jobRepository.createJobExecution(name, jobParameters);
		taskBatchDao.saveRelationship(taskExecution, jobExecution);
		jobExecution.setStatus(status);
		jobExecution.setStartTime(LocalDateTime.now());
		this.jobRepository.update(jobExecution);
		final TaskManifest manifest = new TaskManifest();
		manifest.setPlatformName("default");
		assertThat(dataflowTaskExecutionMetadataDao).isNotNull();
		TaskManifest taskManifest = new TaskManifest();
		taskManifest.setPlatformName("default");
		dataflowTaskExecutionMetadataDao.save(taskExecution, taskManifest);
	}
}
