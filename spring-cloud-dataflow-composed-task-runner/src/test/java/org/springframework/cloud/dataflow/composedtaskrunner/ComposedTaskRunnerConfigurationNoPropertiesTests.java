/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.common.security.CommonSecurityAutoConfiguration;
import org.springframework.cloud.dataflow.composedtaskrunner.configuration.DataFlowTestConfiguration;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Glenn Renfro
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EmbeddedDataSourceConfiguration.class,
		DataFlowTestConfiguration.class, StepBeanDefinitionRegistrar.class,
		ComposedTaskRunnerConfiguration.class,
		StepBeanDefinitionRegistrar.class})
@TestPropertySource(properties = {"graph=AAA && BBB && CCC", "max-wait-time=1000", "spring.cloud.task.name=foo"})
@EnableAutoConfiguration(exclude = {CommonSecurityAutoConfiguration.class})
public class ComposedTaskRunnerConfigurationNoPropertiesTests {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private Job job;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ComposedTaskProperties composedTaskProperties;

	@Test
	@DirtiesContext
	public void testComposedConfiguration() throws Exception {
		JobExecution jobExecution = this.jobRepository.createJobExecution(
				"ComposedTest", new JobParameters());
		TaskletStep ctrStep = context.getBean("AAA_0", TaskletStep.class);
		TaskOperations taskOperations = mock(TaskOperations.class);
		ReflectionTestUtils.setField(ctrStep.getTasklet(), "taskOperations", taskOperations);
		job.execute(jobExecution);
		assertThat(composedTaskProperties.getTransactionIsolationLevel()).isEqualTo("ISOLATION_REPEATABLE_READ");

		Assert.notNull(job.getJobParametersIncrementer(), "JobParametersIncrementer must not be null.");

		verify(taskOperations).launch(
				"AAA",
				Collections.emptyMap(),
				Arrays.asList("--spring.cloud.task.parent-execution-id=1")
		);
	}
}
