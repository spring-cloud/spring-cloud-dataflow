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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.cloud.dataflow.server.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.rest.support.jackson.ISO8601DateFormatWithMilliSeconds;
import org.springframework.cloud.dataflow.rest.support.jackson.Jackson2DataflowModule;
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
			JobRepository jobRepository,
			TaskBatchDao taskBatchDao,
			TaskExecutionDao taskExecutionDao,
			TaskDefinitionReader taskDefinitionReader,
			WebApplicationContext wac,
			RequestMappingHandlerAdapter adapter)
		throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobRestartException {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, JOB_NAME_ORIG, 1, taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, JOB_NAME_FOO, 1, taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao,JOB_NAME_FOOBAR, 2, BatchStatus.COMPLETED,taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, JOB_NAME_COMPLETED, 1, BatchStatus.COMPLETED, taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, JOB_NAME_STARTED, 1, BatchStatus.STARTED, taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, JOB_NAME_STOPPED, 1, BatchStatus.STOPPED, taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, JOB_NAME_FAILED1, 1, BatchStatus.FAILED, taskDefinitionReader);
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, JOB_NAME_FAILED2, 1, BatchStatus.FAILED, taskDefinitionReader);

		Map<String, JobParameter<?>> jobParameterMap = new HashMap<>();
		String dateInString = "07-Jun-2023";
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.US);
		LocalDateTime date = LocalDate.parse(dateInString, formatter).atStartOfDay();
		jobParameterMap.put("javaUtilDate", new JobParameter( date, LocalDateTime.class,true));
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao,
			JOB_NAME_ORIG_WITH_PARAM, 1, BatchStatus.UNKNOWN, taskDefinitionReader,
			new JobParameters(jobParameterMap));

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
			JobRepository jobRepository,
			TaskBatchDao taskBatchDao,
			TaskExecutionDao taskExecutionDao,
			String jobName,
			int jobExecutionCount,
			TaskDefinitionReader taskDefinitionReader
	) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobRestartException {
		createSampleJob(
				jobRepository,
				taskBatchDao,
				taskExecutionDao,
				jobName,
				jobExecutionCount,
				BatchStatus.UNKNOWN,
				taskDefinitionReader,
				new JobParameters()
		);
	}

	private static void createSampleJob(
		JobRepository jobRepository,
		TaskBatchDao taskBatchDao,
		TaskExecutionDao taskExecutionDao,
		String jobName,
		int jobExecutionCount,
		BatchStatus status,
		TaskDefinitionReader taskDefinitionReader
	) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobRestartException {
		createSampleJob(
			jobRepository,
			taskBatchDao,
			taskExecutionDao,
			jobName,
			jobExecutionCount,
			status,
			taskDefinitionReader,
			new JobParameters()
		);
	}

	private static void createSampleJob(
			JobRepository jobRepository,
			TaskBatchDao taskBatchDao,
			TaskExecutionDao taskExecutionDao,
			String jobName,
			int jobExecutionCount,
			BatchStatus status,
			TaskDefinitionReader taskDefinitionReader,
			JobParameters jobParameters
	) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobRestartException {
		TaskExecution taskExecution = taskExecutionDao.createTaskExecution(jobName, LocalDateTime.now(), new ArrayList<>(), null);
		JobExecution jobExecution;
		for (int i = 0; i < jobExecutionCount; i++) {
			jobExecution = jobRepository.createJobExecution(jobName, jobParameters);
			StepExecution stepExecution = new StepExecution("foo", jobExecution, 1L);
			stepExecution.setId(null);
			jobRepository.add(stepExecution);
			taskBatchDao.saveRelationship(taskExecution, jobExecution);
			jobExecution.setStatus(status);
			jobExecution.setStartTime(LocalDateTime.now());
			if (BatchStatus.STOPPED.equals(status)) {
				jobExecution.setEndTime(LocalDateTime.now());
			}
			jobRepository.update(jobExecution);
		}
	}
}
