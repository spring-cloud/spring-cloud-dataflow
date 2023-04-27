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

package org.springframework.cloud.dataflow.server.rest.documentation;

import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.data.domain.PageRequest;

import java.util.stream.Collectors;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the {@code /tasks/logs} endpoint.
 *
 * @author Ilayaperumal Gopinathan
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TaskLogsDocumentation extends BaseDocumentation {

	@After
	public void tearDown() throws Exception {

		destroyTaskDefinition("taskA");
		destroyTaskDefinition("taskB");

		unregisterApp(ApplicationType.task, "timestamp");
	}

	@Test
	public void getLogsByTaskId() throws Exception {
		registerApp(ApplicationType.task, "timestamp", "1.2.0.RELEASE");
		String taskName = "taskA";
		documentation.dontDocument( () -> this.mockMvc.perform(
				post("/tasks/definitions")
						.param("name", taskName)
						.param("definition", "timestamp --format='yyyy MM dd'"))
				.andExpect(status().isOk()));
		this.mockMvc.perform(
				post("/tasks/executions")
						.param("name", taskName))
				.andExpect(status().isCreated());
		TaskDeploymentRepository taskDeploymentRepository =
				springDataflowServer.getWebApplicationContext().getBean(TaskDeploymentRepository.class);
		Thread.sleep(30000);
		this.mockMvc.perform(
				get("/tasks/logs/"+taskDeploymentRepository.findTopByTaskDefinitionNameOrderByCreatedOnAsc(taskName)
						.getTaskDeploymentId()).param("platformName", "default"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						requestParameters(
								parameterWithName("platformName").description("The name of the platform the task is launched."))
				));
	}
	@Test
	public void getLogsByTaskExecutionId() throws Exception {
		registerApp(ApplicationType.task, "timestamp", "1.2.0.RELEASE");
		String taskName = "taskB";
		documentation.dontDocument( () -> this.mockMvc.perform(
						post("/tasks/definitions")
								.param("name", taskName)
								.param("definition", "timestamp --format='yyyy MM dd'"))
				.andExpect(status().isOk()));
		this.mockMvc.perform(
						post("/tasks/executions")
								.param("name", taskName))
				.andExpect(status().isCreated());
		TaskRepository taskRepository =
				springDataflowServer.getWebApplicationContext().getBean(TaskRepository.class);
		TaskDeploymentRepository taskDeploymentRepository =
				springDataflowServer.getWebApplicationContext().getBean(TaskDeploymentRepository.class);
		TaskExplorer taskExplorer = springDataflowServer.getWebApplicationContext().getBean(TaskExplorer.class);

		Thread.sleep(30000);
		TaskExecution taskExecution = taskExplorer.findTaskExecutionsByName(taskName,
				PageRequest.of(0, 20)).get().collect(Collectors.toList()).get(0);
		taskRepository.updateExternalExecutionId(taskExecution.getExecutionId(),
				taskDeploymentRepository.findTopByTaskDefinitionNameOrderByCreatedOnAsc(taskName).getTaskDeploymentId());
		this.mockMvc.perform(
						get("/tasks/logs/taskexecutionid/"+ taskExecution.getExecutionId()))
				.andExpect(status().isOk());
	}

	private void destroyTaskDefinition(String taskName) throws Exception{
		documentation.dontDocument( () -> this.mockMvc.perform(
						delete("/tasks/definitions/{name}", taskName)));
	}


}
