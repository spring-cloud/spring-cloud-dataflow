/*
 * Copyright 2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Date;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.cloud.dataflow.rest.support.jackson.ExecutionContextJacksonMixIn;
import org.springframework.cloud.dataflow.rest.support.jackson.ISO8601DateFormatWithMilliSeconds;
import org.springframework.cloud.dataflow.rest.support.jackson.StepExecutionJacksonMixIn;
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

	final static String JOB_NAME_STOPPED = BASE_JOB_NAME + "_FOO_STOPPED";

	final static String JOB_NAME_STARTED = BASE_JOB_NAME + "_FOO_STARTED";

	final static String JOB_NAME_FOOBAR = BASE_JOB_NAME + "_FOOBAR";

	private final static String JOB_NAME_FOO = BASE_JOB_NAME + "_FOO";

	private final static String JOB_NAME_COMPLETED = BASE_JOB_NAME + "_FOO_COMPLETED";

	static MockMvc createBaseJobExecutionMockMvc(JobRepository jobRepository, TaskBatchDao taskBatchDao,
			TaskExecutionDao taskExecutionDao, WebApplicationContext wac, RequestMappingHandlerAdapter adapter) {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, JOB_NAME_ORIG, 1);
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, JOB_NAME_FOO, 1);
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, JOB_NAME_FOOBAR, 2);
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, JOB_NAME_COMPLETED, 1, BatchStatus.COMPLETED);
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, JOB_NAME_STARTED, 1, BatchStatus.STARTED);
		JobExecutionUtils.createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, JOB_NAME_STOPPED, 1, BatchStatus.STOPPED);
		for (HttpMessageConverter<?> converter : adapter.getMessageConverters()) {
			if (converter instanceof MappingJackson2HttpMessageConverter) {
				final MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
				jacksonConverter.getObjectMapper().addMixIn(StepExecution.class, StepExecutionJacksonMixIn.class);
				jacksonConverter.getObjectMapper().addMixIn(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
				jacksonConverter.getObjectMapper().setDateFormat(new ISO8601DateFormatWithMilliSeconds());
			}
		}
		return mockMvc;
	}

	private static void createSampleJob(JobRepository jobRepository,
			TaskBatchDao taskBatchDao, TaskExecutionDao taskExecutionDao,
			String jobName, int jobExecutionCount) {
		createSampleJob(jobRepository, taskBatchDao, taskExecutionDao, jobName,
				jobExecutionCount, BatchStatus.UNKNOWN);
	}

	private static void createSampleJob(JobRepository jobRepository, TaskBatchDao taskBatchDao,
			TaskExecutionDao taskExecutionDao, String jobName,
			int jobExecutionCount, BatchStatus status) {
		JobInstance instance = jobRepository.createJobInstance(jobName, new JobParameters());
		TaskExecution taskExecution = taskExecutionDao.createTaskExecution(jobName, new Date(), new ArrayList<>(), null);
		JobExecution jobExecution;

		for (int i = 0; i < jobExecutionCount; i++) {
			jobExecution = jobRepository.createJobExecution(instance, new JobParameters(), null);
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
