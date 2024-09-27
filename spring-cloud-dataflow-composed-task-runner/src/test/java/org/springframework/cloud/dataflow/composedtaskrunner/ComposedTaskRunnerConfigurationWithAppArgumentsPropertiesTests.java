/*
 * Copyright 2021 the original author or authors.
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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.common.security.CommonSecurityAutoConfiguration;
import org.springframework.cloud.dataflow.composedtaskrunner.configuration.DataFlowTestConfiguration;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.composedtaskrunner.support.ComposedTaskRunnerTaskletTestUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Janne Valkealahti
 * @author Corneil du Plessis
 */
@SpringJUnitConfig(classes = {EmbeddedDataSourceConfiguration.class,
		DataFlowTestConfiguration.class, StepBeanDefinitionRegistrar.class,
		ComposedTaskRunnerConfiguration.class})
@TestPropertySource(properties = {"graph=ComposedTest-AAA && ComposedTest-BBB && ComposedTest-CCC","max-wait-time=1010",
		"interval-time-between-checks=1100",
        "composed-task-app-arguments.app.AAA.0=--arg1=value1",
        "composed-task-app-arguments.app.AAA.1=--arg2=value2",
        "composed-task-app-arguments.base64_YXBwLiouMA=--arg3=value3",
		"dataflow-server-uri=https://bar", "spring.cloud.task.name=ComposedTest"})
@EnableAutoConfiguration(exclude = { CommonSecurityAutoConfiguration.class})
public class ComposedTaskRunnerConfigurationWithAppArgumentsPropertiesTests {

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
		job.execute(jobExecution);

		assertThat(composedTaskProperties.getComposedTaskProperties()).isNull();
		assertThat(composedTaskProperties.getMaxWaitTime()).isEqualTo(1010);
		assertThat(composedTaskProperties.getIntervalTimeBetweenChecks()).isEqualTo(1100);
		assertThat(composedTaskProperties.getDataflowServerUri().toASCIIString()).isEqualTo("https://bar");
		assertThat(job.getJobParametersIncrementer()).withFailMessage("JobParametersIncrementer must not be null.").isNotNull();

		TaskLauncherTasklet tasklet = ComposedTaskRunnerTaskletTestUtils.getTaskletLauncherTasklet(context, "ComposedTest-AAA_0");
		List<String> result = ComposedTaskRunnerTaskletTestUtils.getTaskletArgumentsViaReflection(tasklet);
		assertThat(result).contains("--arg1=value1", "--arg2=value2", "--arg3=value3");
		assertThat(result).hasSize(3);
		Map<String, String> taskletProperties = ComposedTaskRunnerTaskletTestUtils.getTaskletPropertiesViaReflection(tasklet);
		assertThat(taskletProperties).isEmpty();
	}
}
