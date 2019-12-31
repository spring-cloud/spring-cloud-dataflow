/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.integration.test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.integration.test.util.Wait;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 *
 * @author Christian Tzolov
 */
public class DockerComposeTestTask {

	private static final Logger logger = LoggerFactory.getLogger(DockerComposeTestTask.class);

	private static DataFlowTemplate dataFlowOperations;
	private static TaskOperations taskOperations;

	@BeforeClass
	public static void beforeClass() {
		dataFlowOperations = new DataFlowTemplate(URI.create("http://localhost:9393"));
		logger.info("Configured platforms: " + dataFlowOperations.streamOperations().listPlatforms().stream()
				.map(d -> String.format("[%s:%s]", d.getName(), d.getType())).collect(Collectors.joining()));
		taskOperations = dataFlowOperations.taskOperations();
	}

	@Before
	public void before() {
		Wait.on(dataFlowOperations.appRegistryOperations()).until(appRegistry ->
				appRegistry.list().getMetadata().getTotalElements() >= 68L);
	}

	@After
	public void after() {
		dataFlowOperations.taskOperations().destroyAll();
	}

	// -----------------------------------------------------------------------
	//                               TASK TESTS
	// -----------------------------------------------------------------------
	@Test
	public void timestampTask() {
		logger.info("task-timestamp-test");
		String taskDefinitionName = randomTaskName();
		TaskDefinitionResource tdr = taskOperations.create(taskDefinitionName,
				"timestamp", "Test timestamp task");

		long id1 = taskOperations.launch(taskDefinitionName, Collections.EMPTY_MAP, Collections.EMPTY_LIST, null);
		waitTaskCompletion(taskDefinitionName, 1);
		assertCompletionWithExitCode(taskDefinitionName, 0);

		// Launch existing task
		taskOperations.launch(taskDefinitionName, Collections.EMPTY_MAP, Collections.EMPTY_LIST, null);
		waitTaskCompletion(taskDefinitionName, 2);
		assertCompletionWithExitCode(taskDefinitionName, 0);

		taskOperations.destroy(taskDefinitionName);
	}

	@Test
	public void composedTask() {
		logger.info("task-composed-task-test");
		String taskDefinitionName = randomTaskName();
		TaskDefinitionResource tdr = taskOperations.create(taskDefinitionName,
				"a: timestamp && b:timestamp", "Test composedTask");

		taskOperations.launch(taskDefinitionName, Collections.EMPTY_MAP, Collections.EMPTY_LIST, null);
		waitTaskCompletion(taskDefinitionName, 1);
		waitTaskCompletion(taskDefinitionName + "-a", 1);
		waitTaskCompletion(taskDefinitionName + "-b", 1);

		assertCompletionWithExitCode(taskDefinitionName, 0);

		assertThat(taskOperations.list().getContent().size(), is(3));
		taskOperations.destroy(taskDefinitionName);
		assertThat(taskOperations.list().getContent().size(), is(0));
	}

	@Test
	public void multipleComposedTaskWithArguments() {
		logger.info("task-multiple-composed-task-with-arguments-test");
		String taskDefinitionName = randomTaskName();
		TaskDefinitionResource tdr =
				taskOperations.create(taskDefinitionName, "a: timestamp && b:timestamp", "Test multipleComposedTaskhWithArguments");

		List<String> arguments = Arrays.asList("--increment-instance-enabled=true");
		taskOperations.launch(taskDefinitionName, Collections.EMPTY_MAP, arguments, null);
		waitTaskCompletion(taskDefinitionName, 1);
		waitTaskCompletion(taskDefinitionName + "-a", 1);
		waitTaskCompletion(taskDefinitionName + "-b", 1);

		assertCompletionWithExitCode(taskDefinitionName, 0);

		taskOperations.launch(taskDefinitionName, Collections.EMPTY_MAP, arguments, null);
		waitTaskCompletion(taskDefinitionName, 2);
		waitTaskCompletion(taskDefinitionName + "-a", 2);
		waitTaskCompletion(taskDefinitionName + "-b", 2);

		assertCompletionWithExitCode(taskDefinitionName, 0);

		Collection<JobExecutionResource> jobExecutionResources =
				dataFlowOperations.jobOperations().executionListByJobName(taskDefinitionName).getContent();
		assertThat(jobExecutionResources.size(), is(2));

		taskOperations.destroy(taskDefinitionName);
	}

	private void waitTaskCompletion(String taskDefinitionName, int taskExecutionCount) {
		Wait.on(taskDefinitionName).until(taskDefName -> {
			Collection<TaskExecutionResource> taskExecutions =
					taskOperations.executionListByTaskName(taskDefName).getContent();
			return (taskExecutions.size() >= taskExecutionCount) &&
					taskExecutions.stream()
							.map(execution -> execution != null && execution.getEndTime() != null)
							.reduce(Boolean::logicalAnd)
							.orElse(false);
		});
	}

	private void assertCompletionWithExitCode(String taskDefinitionName, int exitCode) {
		taskOperations.executionListByTaskName(taskDefinitionName).getContent().stream()
				.forEach(taskExecution -> assertThat(taskExecution.getExitCode(), is(exitCode)));
	}

	private String randomTaskName() {
		return "task-" + UUID.randomUUID().toString().substring(0, 10);
	}
}
