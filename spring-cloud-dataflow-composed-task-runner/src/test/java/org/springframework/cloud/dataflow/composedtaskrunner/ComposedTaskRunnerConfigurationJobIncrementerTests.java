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

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.common.security.CommonSecurityAutoConfiguration;
import org.springframework.cloud.dataflow.composedtaskrunner.configuration.DataFlowTestConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.Assert;

/**
 * @author Glenn Renfro
 */
@SpringJUnitConfig(classes = {EmbeddedDataSourceConfiguration.class,
		DataFlowTestConfiguration.class, StepBeanDefinitionRegistrar.class,
		ComposedTaskRunnerConfiguration.class,
		StepBeanDefinitionRegistrar.class})
@EnableAutoConfiguration(exclude = { CommonSecurityAutoConfiguration.class})
@TestPropertySource(properties = {"graph=AAA && BBB && CCC","max-wait-time=1000", "increment-instance-enabled=true", "spring.cloud.task.name=footest"})
public class ComposedTaskRunnerConfigurationJobIncrementerTests {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	protected Job job;

	@Test
	@DirtiesContext
	public void testComposedConfigurationWithJobIncrementer() throws Exception {
		this.jobRepository.createJobExecution(
				"ComposedTest", new JobParameters());
		Assert.notNull(job.getJobParametersIncrementer(), "JobParametersIncrementer must not be null.");
	}
}
