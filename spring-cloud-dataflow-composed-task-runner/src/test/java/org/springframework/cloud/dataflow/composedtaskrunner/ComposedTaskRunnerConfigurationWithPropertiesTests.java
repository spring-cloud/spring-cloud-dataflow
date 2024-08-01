/*
 * Copyright 2017-2022 the original author or authors.
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
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.common.security.CommonSecurityAutoConfiguration;
import org.springframework.cloud.dataflow.composedtaskrunner.configuration.DataFlowTestConfiguration;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Glenn Renfro
 */
@SpringJUnitConfig(classes = {EmbeddedDataSourceConfiguration.class,
		DataFlowTestConfiguration.class, StepBeanDefinitionRegistrar.class,
		ComposedTaskRunnerConfiguration.class,
		StepBeanDefinitionRegistrar.class})
@TestPropertySource(properties = {"graph=ComposedTest-AAA && ComposedTest-BBB && ComposedTest-CCC","max-wait-time=1010",
		"composed-task-properties=" + ComposedTaskRunnerConfigurationWithPropertiesTests.COMPOSED_TASK_PROPS ,
		"interval-time-between-checks=1100", "composed-task-arguments=--baz=boo --AAA.foo=bar BBB.que=qui",
		"transaction-isolation-level=ISOLATION_READ_COMMITTED","spring.cloud.task.closecontext-enabled=true",
		"dataflow-server-uri=https://bar", "spring.cloud.task.name=ComposedTest","max-start-wait-time=1011"})
@EnableAutoConfiguration(exclude = { CommonSecurityAutoConfiguration.class})
@ExtendWith(OutputCaptureExtension.class)
public class ComposedTaskRunnerConfigurationWithPropertiesTests {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private Job job;

	@Autowired
	private ComposedTaskProperties composedTaskProperties;

	@Autowired
	private TaskProperties taskProperties;

	@Autowired
	ApplicationContext context;

	protected static final String COMPOSED_TASK_PROPS = "app.ComposedTest-AAA.format=yyyy, "
		+ "app.ComposedTest-AAA.spring.cloud.task.table-prefix=BOOT3_,"
		+ "app.ComposedTest-BBB.spring.cloud.task.tableprefix=BOOT3_,"
		+ "app.ComposedTest-CCC.spring.cloud.task.tablePrefix=BOOT3_,"
		+ "app.ComposedTest-BBB.format=mm, "
		+ "deployer.ComposedTest-AAA.memory=2048m";

	@Test
	@DirtiesContext
	public void testComposedConfiguration(CapturedOutput outputCapture) throws Exception {
		assertThat(composedTaskProperties.isSkipTlsCertificateVerification()).isFalse();

		JobExecution jobExecution = this.jobRepository.createJobExecution(
				"ComposedTest", new JobParameters());
		TaskletStep ctrStep = context.getBean("ComposedTest-AAA_0", TaskletStep.class);
		TaskOperations taskOperations = mock(TaskOperations.class);
		ReflectionTestUtils.setField(ctrStep.getTasklet(), "taskOperations", taskOperations);

		job.execute(jobExecution);

		Map<String, String> props = new HashMap<>(1);
		props.put("format", "yyyy");
		props.put("memory", "2048m");
		props.put("spring.cloud.task.table-prefix", "BOOT3_");

		assertThat(composedTaskProperties.getComposedTaskProperties()).isEqualTo(COMPOSED_TASK_PROPS);
		assertThat(composedTaskProperties.getMaxWaitTime()).isEqualTo(1010);
		assertThat(composedTaskProperties.getMaxStartWaitTime()).isEqualTo(1011);
		assertThat(composedTaskProperties.getIntervalTimeBetweenChecks()).isEqualTo(1100);
		assertThat(composedTaskProperties.getDataflowServerUri().toASCIIString()).isEqualTo("https://bar");
		assertThat(composedTaskProperties.getTransactionIsolationLevel()).isEqualTo("ISOLATION_READ_COMMITTED");
		assertThat(taskProperties.getClosecontextEnabled()).isTrue();

		List<String> args = new ArrayList<>(2);
		args.add("--baz=boo --foo=bar");
		args.add("--spring.cloud.task.parent-execution-id=1");
		args.add("--spring.cloud.task.parent-schema-target=boot2");
		Assert.notNull(job.getJobParametersIncrementer(), "JobParametersIncrementer must not be null.");

		verify(taskOperations).launch("ComposedTest-AAA", props, args);

		String logEntries = outputCapture.toString();
		assertThat(logEntries).contains("taskExplorerContainer:adding:ComposedTest-AAA:BOOT3_");

		assertThat(logEntries).contains("taskExplorerContainer:adding:ComposedTest-BBB:BOOT3_");
		assertThat(logEntries).contains("taskExplorerContainer:adding:ComposedTest-CCC:BOOT3_");

	}
}
