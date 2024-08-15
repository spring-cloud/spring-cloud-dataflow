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

package org.springframework.cloud.dataflow.composedtaskrunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes={EmbeddedDataSourceConfiguration.class,
	DataFlowTestConfiguration.class,StepBeanDefinitionRegistrar.class,
	ComposedTaskRunnerConfiguration.class})
@TestPropertySource(properties = {"graph=ComposedTest-l1 && ComposedTest-l2 && ComposedTest-l11","max-wait-time=1010",
	"composed-task-app-properties.app.l1.AAA.format=yyyy",
	"composed-task-app-properties.app.l11.AAA.format=yyyy",
	"composed-task-app-properties.app.l2.AAA.format=yyyy",
	"interval-time-between-checks=1100",
	"composed-task-arguments=--baz=boo",
	"dataflow-server-uri=https://bar", "spring.cloud.task.name=ComposedTest"})
@EnableAutoConfiguration(exclude = { CommonSecurityAutoConfiguration.class})
public class ComposedTaskRunnerConfigurationWithPropertiesWithLabelTests {
	private static final Logger logger = LoggerFactory.getLogger(ComposedTaskRunnerConfigurationWithPropertiesWithLabelTests.class);
	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private Job job;

	@Autowired
	private ComposedTaskProperties composedTaskProperties;

	@Autowired
	private ApplicationContext context;

	@Test
	@DirtiesContext
	public void testComposedConfiguration() throws Exception {
		JobExecution jobExecution = this.jobRepository.createJobExecution(
			"ComposedTest", new JobParameters());
		job.execute(jobExecution);

		Map<String, String> props = new HashMap<>(1);
		props.put("app.l1.AAA.format", "yyyy");
		props.put("app.l2.AAA.format", "yyyy");
		props.put("app.l11.AAA.format", "yyyy");
		Map<String, String> composedTaskAppProperties = new HashMap<>();
		composedTaskAppProperties.put("app.l1.AAA.format", "yyyy");
		composedTaskAppProperties.put("app.l2.AAA.format", "yyyy");
		composedTaskAppProperties.put("app.l11.AAA.format", "yyyy");

		assertThat(composedTaskProperties.getComposedTaskAppProperties()).isEqualTo(composedTaskAppProperties);
		assertThat(composedTaskProperties.getMaxWaitTime()).isEqualTo(1010);
		assertThat(composedTaskProperties.getIntervalTimeBetweenChecks()).isEqualTo(1100);
		assertThat(composedTaskProperties.getDataflowServerUri().toASCIIString()).isEqualTo("https://bar");
		List<String> args = new ArrayList<>(1);
		args.add("--baz=boo");
		Assert.notNull(job.getJobParametersIncrementer(), "JobParametersIncrementer must not be null.");
		TaskLauncherTasklet tasklet = ComposedTaskRunnerTaskletTestUtils.getTaskletLauncherTasklet(context, "ComposedTest-l1_0");
		List<String> result = ComposedTaskRunnerTaskletTestUtils.getTaskletArgumentsViaReflection(tasklet);
		assertThat(result).contains("--baz=boo");
		assertThat(result).hasSize(1);
		Map<String, String> taskletProperties = ComposedTaskRunnerTaskletTestUtils.getTaskletPropertiesViaReflection(tasklet);
		logger.info("taskletProperties:{}", taskletProperties);
		assertThat(taskletProperties.keySet()).containsExactly("app.l1.AAA.format");
		assertThat(taskletProperties).hasSize(1);
		assertThat(taskletProperties).containsEntry("app.l1.AAA.format", "yyyy");
	}
}
