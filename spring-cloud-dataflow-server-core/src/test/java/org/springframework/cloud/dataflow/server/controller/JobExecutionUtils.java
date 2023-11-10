/*
 * Copyright 2018-2023 the original author or authors.
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.rest.support.jackson.ISO8601DateFormatWithMilliSeconds;
import org.springframework.cloud.dataflow.rest.support.jackson.Jackson2DataflowModule;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.server.repository.JobRepositoryContainer;
import org.springframework.cloud.dataflow.server.repository.TaskBatchDaoContainer;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionDaoContainer;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Utilities that are used to create {@link JobExecution}s for tests.
 *
 * @author Glenn Renfro
 *
 * @since 2.0
 */
class JobExecutionUtils
{
	final static String BASE_JOB_NAME = "myJob";

	final static String JOB_NAME_ORIG = BASE_JOB_NAME + "_ORIG";

	final static String JOB_NAME_ORIG_WITH_PARAM = BASE_JOB_NAME + "_ORIG_WITH_PARAM";

	final static String JOB_NAME_STOPPED = BASE_JOB_NAME + "_FOO_STOPPED";

	final static String JOB_NAME_STARTED = BASE_JOB_NAME + "_FOO_STARTED";

	final static String JOB_NAME_FOOBAR = BASE_JOB_NAME + "_FOOBAR";

	final static String JOB_NAME_FAILED1 = BASE_JOB_NAME + "1_FAILED";

	final static String JOB_NAME_FAILED2 = BASE_JOB_NAME + "2_FAILED";

	final static String JOB_NAME_COMPLETED = BASE_JOB_NAME + "_FOO_COMPLETED";

	private final static String JOB_NAME_FOO = BASE_JOB_NAME + "_FOO";


	static MockMvc createBaseJobExecutionMockMvc(
			JobRepositoryContainer jobRepositoryContainer,
			TaskBatchDaoContainer taskBatchDaoContainer,
			TaskExecutionDaoContainer taskExecutionDaoContainer,
			AggregateExecutionSupport aggregateExecutionSupport,
			TaskDefinitionReader taskDefinitionReader,
			WebApplicationContext wac,
			RequestMappingHandlerAdapter adapter) {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		JobExecutionUtils.createSampleJob(jobRepositoryContainer, taskBatchDaoContainer, taskExecutionDaoContainer, aggregateExecutionSupport, JOB_NAME_ORIG, 1, taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepositoryContainer, taskBatchDaoContainer, taskExecutionDaoContainer, aggregateExecutionSupport, JOB_NAME_FOO, 1, taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepositoryContainer, taskBatchDaoContainer, taskExecutionDaoContainer, aggregateExecutionSupport,JOB_NAME_FOOBAR, 2, taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepositoryContainer, taskBatchDaoContainer, taskExecutionDaoContainer, aggregateExecutionSupport, JOB_NAME_COMPLETED, 1, BatchStatus.COMPLETED, taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepositoryContainer, taskBatchDaoContainer, taskExecutionDaoContainer, aggregateExecutionSupport, JOB_NAME_STARTED, 1, BatchStatus.STARTED, taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepositoryContainer, taskBatchDaoContainer, taskExecutionDaoContainer, aggregateExecutionSupport, JOB_NAME_STOPPED, 1, BatchStatus.STOPPED, taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepositoryContainer, taskBatchDaoContainer, taskExecutionDaoContainer, aggregateExecutionSupport, JOB_NAME_FAILED1, 1, BatchStatus.FAILED, taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepositoryContainer, taskBatchDaoContainer, taskExecutionDaoContainer, aggregateExecutionSupport, JOB_NAME_FAILED2, 1, BatchStatus.FAILED, taskDefinitionReader);

		Map<String, JobParameter> jobParameterMap = new HashMap<>();
		String dateInString = "7-Jun-2023";
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
		Date date = null;
		try {
			date = formatter.parse(dateInString);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		jobParameterMap.put("javaUtilDate", new JobParameter(date));
		JobExecutionUtils.createSampleJob(jobRepositoryContainer, taskBatchDaoContainer, taskExecutionDaoContainer, aggregateExecutionSupport, JOB_NAME_ORIG_WITH_PARAM, 1, BatchStatus.UNKNOWN, taskDefinitionReader, new JobParameters(jobParameterMap));


		for (HttpMessageConverter<?> converter : adapter.getMessageConverters()) {
			if (converter instanceof MappingJackson2HttpMessageConverter) {
				final MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
				jacksonConverter.getObjectMapper().registerModule(new Jackson2DataflowModule());
				jacksonConverter.getObjectMapper().setDateFormat(new ISO8601DateFormatWithMilliSeconds());
			}
		}
		return mockMvc;
	}

	private static void createSampleJob(
			JobRepositoryContainer jobRepositoryContainer,
			TaskBatchDaoContainer taskBatchDaoContainer,
			TaskExecutionDaoContainer taskExecutionDaoContainer,
			AggregateExecutionSupport aggregateExecutionSupport,
			String jobName,
			int jobExecutionCount,
			TaskDefinitionReader taskDefinitionReader
	) {
		createSampleJob(
				jobRepositoryContainer,
				taskBatchDaoContainer,
				taskExecutionDaoContainer,
				aggregateExecutionSupport,
				jobName,
				jobExecutionCount,
				BatchStatus.UNKNOWN,
				taskDefinitionReader,
				new JobParameters()
		);
	}

	private static void createSampleJob(
		JobRepositoryContainer jobRepositoryContainer,
		TaskBatchDaoContainer taskBatchDaoContainer,
		TaskExecutionDaoContainer taskExecutionDaoContainer,
		AggregateExecutionSupport aggregateExecutionSupport,
		String jobName,
		int jobExecutionCount,
		BatchStatus status,
		TaskDefinitionReader taskDefinitionReader
	) {
		createSampleJob(
			jobRepositoryContainer,
			taskBatchDaoContainer,
			taskExecutionDaoContainer,
			aggregateExecutionSupport,
			jobName,
			jobExecutionCount,
			status,
			taskDefinitionReader,
			new JobParameters()
		);
	}

	private static void createSampleJob(
			JobRepositoryContainer jobRepositoryContainer,
			TaskBatchDaoContainer taskBatchDaoContainer,
			TaskExecutionDaoContainer taskExecutionDaoContainer,
			AggregateExecutionSupport aggregateExecutionSupport,
			String jobName,
			int jobExecutionCount,
			BatchStatus status,
			TaskDefinitionReader taskDefinitionReader,
			JobParameters jobParameters
	) {
		SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget(jobName, taskDefinitionReader);
		JobRepository jobRepository = jobRepositoryContainer.get(schemaVersionTarget.getName());
		JobInstance instance = jobRepository.createJobInstance(jobName, jobParameters);
		TaskExecutionDao taskExecutionDao = taskExecutionDaoContainer.get(schemaVersionTarget.getName());
		TaskExecution taskExecution = taskExecutionDao.createTaskExecution(jobName, new Date(), new ArrayList<>(), null);
		JobExecution jobExecution;
		TaskBatchDao taskBatchDao = taskBatchDaoContainer.get(schemaVersionTarget.getName());
		for (int i = 0; i < jobExecutionCount; i++) {
			jobExecution = jobRepository.createJobExecution(instance, jobParameters, null);
			StepExecution stepExecution = new StepExecution("foo", jobExecution, 1L);
			stepExecution.setId(null);
			jobRepository.add(stepExecution);
			taskBatchDao.saveRelationship(taskExecution, jobExecution);
			jobExecution.setStatus(status);
			jobExecution.setStartTime(new Date());
			if (BatchStatus.STOPPED.equals(status)) {
				jobExecution.setEndTime(new Date());
			}
			jobRepository.update(jobExecution);
		}
	}
}
