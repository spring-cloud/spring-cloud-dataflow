/*
 * Copyright 2017-2020 the original author or authors.
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
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.JdbcDataflowTaskExecutionMetadataDao;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.batch.listener.support.JdbcTaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
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
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { EmbeddedDataSourceConfiguration.class })
@DirtiesContext
public class JobExecutionsDocumentation extends BaseDocumentation {

	private final static String JOB_NAME = "DOCJOB";

	private static boolean initialized;
	private JobRepository jobRepository;
	private TaskExecutionDao dao;
	private TaskBatchDao taskBatchDao;
	private JdbcTemplate jdbcTemplate;


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
							.param("name", "DOCJOB1")
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
						.param("page", "0")
						.param("size", "10")
						.param("jobInstanceId", "1"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
				requestParameters(
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
						.param("page", "0")
						.param("size", "10")
						.param("taskExecutionId", "1"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
				requestParameters(
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
						.param("page", "0")
						.param("size", "10")
						.param("fromDate", "2000-09-24T17:00:45,000")
						.param("toDate", "2050-09-24T18:00:45,000"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
				requestParameters(
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
						subsectionWithPath("_embedded.jobExecutionThinResourceList")
								.description("Contains a collection of Job Executions without step executions included/"),
						subsectionWithPath("_links.self").description("Link to the job execution resource"),
						subsectionWithPath("page").description("Pagination properties")
				)));
	}

	@Test
	public void jobDisplayDetail() throws Exception {
		this.mockMvc.perform(
			get("/jobs/executions/{id}", "2"))
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
					subsectionWithPath("_links.self").description("Link to the stream definition resource")
				)
			));
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
		TaskExecution taskExecution = this.dao.createTaskExecution(name, new Date(), Collections.singletonList("--spring.cloud.data.flow.platformname=default"), null);
		Map<String, JobParameter> jobParameterMap = new HashMap<>();
		JobParameters jobParameters = new JobParameters(jobParameterMap);
		JobExecution jobExecution = this.jobRepository.createJobExecution(this.jobRepository.createJobInstance(name, new JobParameters()), jobParameters, null);
		this.taskBatchDao.saveRelationship(taskExecution, jobExecution);
		jobExecution.setStatus(status);
		jobExecution.setStartTime(new Date());
		this.jobRepository.update(jobExecution);
		TaskManifest manifest = new TaskManifest();
		manifest.setPlatformName("default");
		DataFieldMaxValueIncrementerFactory incrementerFactory = new DefaultDataFieldMaxValueIncrementerFactory(dataSource);

		DataflowTaskExecutionMetadataDao metadataDao = new JdbcDataflowTaskExecutionMetadataDao(
				dataSource, incrementerFactory.getIncrementer("h2", "task_execution_metadata_seq"));
		TaskManifest taskManifest = new TaskManifest();
		taskManifest.setPlatformName("default");
		metadataDao.save(taskExecution, taskManifest);
	}

}
