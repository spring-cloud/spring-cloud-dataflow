/*
 * Copyright 2016-2020 the original author or authors.
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDeployment;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.dataflow.server.config.DataflowAsyncAutoConfiguration;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.JobDependencies;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionDaoContainer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for the {@link TaskExecutionController#cleanupAll async cleanup} API.
 *
 * @author Chris Bono
 */
@SpringBootTest(
		properties = "spring.cloud.dataflow.async.enabled=true",
		classes = { JobDependencies.class, TaskExecutionAutoConfiguration.class, DataflowAsyncAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, BatchProperties.class})
@EnableConfigurationProperties({CommonApplicationProperties.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
@EnableDataFlowServer
public class TaskExecutionControllerCleanupAsyncTests {

	@Autowired
	private TaskExecutionDaoContainer daoContainer;

	@Autowired
	private TaskDefinitionRepository taskDefinitionRepository;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private AggregateExecutionSupport aggregateExecutionSupport;

	@Autowired
	private TaskLauncher taskLauncher;

	@Autowired
	private LauncherRepository launcherRepository;

	@Autowired
	private TaskPlatform taskPlatform;

	@Autowired
	private TaskDeploymentRepository taskDeploymentRepository;

	@Autowired
	TaskDefinitionReader taskDefinitionReader;

	@BeforeEach
	public void setupMockMVC() {
		assertThat(this.launcherRepository.findByName("default")).isNull();
		Launcher launcher = new Launcher("default", "local", taskLauncher);
		launcherRepository.save(launcher);
		taskPlatform.setLaunchers(Collections.singletonList(launcher));
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
	}

	@Test
	void cleanupAll() throws Exception {
		String taskExecutionId = "asyncCleanupAllTaskExecId";
		setupTaskExecutions("asyncCleanupAllTaskName", taskExecutionId);
		mockMvc.perform(delete("/tasks/executions"))
				.andExpect(status().is(200));
		verify(taskLauncher, times(0)).cleanup(taskExecutionId);
		Awaitility.await()
				.atMost(Duration.ofSeconds(3))
				.untilAsserted(() -> verify(taskLauncher,  times(2)).cleanup(taskExecutionId));
	}

	private void setupTaskExecutions(String taskName, String taskExecutionId) {
		taskDefinitionRepository.save(new TaskDefinition(taskName, "taskDslGoesHere"));
		SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget(taskName, taskDefinitionReader);
		TaskExecutionDao taskExecutionDao = daoContainer.get(schemaVersionTarget.getName());

		List<String> taskArgs = new ArrayList<>();
		taskArgs.add("foo=bar");
		TaskExecution taskExecution1 = taskExecutionDao.createTaskExecution(taskName, new Date(), taskArgs, taskExecutionId);
		taskExecutionDao.createTaskExecution(taskName, new Date(), taskArgs, taskExecutionId, taskExecution1.getExecutionId());

		TaskDeployment taskDeployment = new TaskDeployment();
		taskDeployment.setTaskDefinitionName(taskName);
		taskDeployment.setTaskDeploymentId(taskExecutionId);
		taskDeployment.setPlatformName("default");
		taskDeployment.setCreatedOn(Instant.now());
		taskDeploymentRepository.save(taskDeployment);
	}

}
