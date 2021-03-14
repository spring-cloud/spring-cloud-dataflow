/*
 * Copyright 2017-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.common.security.CommonSecurityAutoConfiguration;
import org.springframework.cloud.dataflow.composedtaskrunner.configuration.DataFlowTestConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes={EmbeddedDataSourceConfiguration.class,
		DataFlowTestConfiguration.class,
		ComposedTaskRunnerConfiguration.class,
		StepBeanDefinitionRegistrar.class})
@EnableAutoConfiguration(exclude = { CommonSecurityAutoConfiguration.class})
@TestPropertySource(properties = {"graph=AAA && BBB && CCC",
		"increment-instance-enabled=true",
		"spring.cloud.task.name=" + ComposedTaskRunnerConfigurationJobIncrementerTests.JOB_NAME})
public class ComposedTaskRunnerConfigurationJobIncrementerTests {

	public static final String JOB_NAME = "footest";
	private static final String RUN_ID_KEY = "run.id";

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JobExplorer jobExplorer;

	@Autowired
	protected Job job;

	@Test
	public void testWithoutPreviousExecution() {
		JobParameters jobParameters =
				new JobParametersBuilder(this.jobExplorer).getNextJobParameters(job).toJobParameters();

		assertThat(jobParameters.getParameters()).containsOnlyKeys(RUN_ID_KEY);
		assertThat(jobParameters.getLong(RUN_ID_KEY)).isEqualTo(1L);
	}

	@Test
	@DirtiesContext
	public void testWithPreviousExecution() throws Exception {
		JobParameters previousParameters = new JobParametersBuilder()
				.addLong(RUN_ID_KEY, 42L)
				.addString("someKey", "someValue")
				.toJobParameters();
		this.jobRepository.createJobExecution(JOB_NAME, previousParameters);

		JobParameters jobParameters =
				new JobParametersBuilder(this.jobExplorer).getNextJobParameters(job).toJobParameters();

		assertThat(jobParameters.getParameters()).containsOnlyKeys(RUN_ID_KEY);
		assertThat(jobParameters.getLong(RUN_ID_KEY)).isEqualTo(43L);
	}
}
