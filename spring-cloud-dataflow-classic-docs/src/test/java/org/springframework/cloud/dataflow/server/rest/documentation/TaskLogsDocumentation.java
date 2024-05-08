/*
 * Copyright 2019-2023 the original author or authors.
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

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the {@code /tasks/logs} endpoint.
 *
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
@TestMethodOrder(MethodName.class)
public class TaskLogsDocumentation extends BaseDocumentation {

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
		TaskExecutionService service = springDataflowServer.getWebApplicationContext().getBean(TaskExecutionService.class);
		AggregateExecutionSupport aggregateExecutionSupport = springDataflowServer.getWebApplicationContext().getBean(AggregateExecutionSupport.class);
		TaskDefinitionReader taskDefinitionReader = springDataflowServer.getWebApplicationContext().getBean(TaskDefinitionReader.class);
		SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget(taskName, taskDefinitionReader);
		Awaitility.await().atMost(Duration.ofMillis(30000)).until(() -> service.getLog("default",
				taskDeploymentRepository.findTopByTaskDefinitionNameOrderByCreatedOnAsc(taskName).getTaskDeploymentId(),
				schemaVersionTarget.getName()).length() > 0);
		this.mockMvc.perform(
				get("/tasks/logs/"+taskDeploymentRepository.findTopByTaskDefinitionNameOrderByCreatedOnAsc(taskName)
						.getTaskDeploymentId()).param("platformName", "default"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						requestParameters(
								parameterWithName("platformName").description("The name of the platform the task is launched."))
				));
	}

}
