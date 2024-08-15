/*
 * Copyright 2017-2023 the original author or authors.
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

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Documentation for the /tasks/executions endpoint.
 *
 * @author Eric Bottard
 * @author Glenn Renfro
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TaskExecutionsDocumentation extends BaseDocumentation {
	@BeforeEach
	public void setup() throws Exception {
		registerApp(ApplicationType.task, "timestamp", "1.2.0.RELEASE");
		createTaskDefinition("taskA");
		createTaskDefinition("taskB");
		executeTask("taskA");
		executeTask("taskB");
	}


	@AfterEach
	public void tearDown() throws Exception {
		cleanupTaskExecutions("taskA");
		cleanupTaskExecutions("taskB");
		destroyTaskDefinition("taskA");
		destroyTaskDefinition("taskB");
		unregisterApp(ApplicationType.task, "timestamp");
	}

	@Test
	public void launchTaskBoot3() throws Exception {
		this.mockMvc.perform(
						post("/tasks/executions/launch")
								.queryParam("name", "taskA")
								.queryParam("properties", "app.my-task.foo=bar,deployer.my-task.something-else=3")
								.queryParam("arguments", "--server.port=8080 --foo=bar")
				)
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
								queryParameters(
										parameterWithName("name").description("The name of the task definition to launch"),
										parameterWithName("properties")
												.description("Application and Deployer properties to use while launching. (optional)"),
										parameterWithName("arguments")
												.description("Command line arguments to pass to the task. (optional)")),
								responseFields(
										fieldWithPath("executionId").description("The id of the task execution"),
										subsectionWithPath("_links.self").description("Link to the task execution resource"),
										subsectionWithPath("_links.tasks/logs").type(fieldWithPath("_links.tasks/logs").ignored().optional()).description("Link to the task execution logs").optional()
								)
						)
				);
	}

	@Test
	public void launchTask() throws Exception {
		this.mockMvc.perform(
						post("/tasks/executions")
								.queryParam("name", "taskA")
								.queryParam("properties", "app.my-task.foo=bar,deployer.my-task.something-else=3")
								.queryParam("arguments", "--server.port=8080 --foo=bar")
				)
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
								queryParameters(
										parameterWithName("name").description("The name of the task definition to launch"),
										parameterWithName("properties")
												.description("Application and Deployer properties to use while launching. (optional)"),
										parameterWithName("arguments")
												.description("Command line arguments to pass to the task. (optional)")
								)
						)
				);
	}

	@Test
	public void getTaskCurrentCount() throws Exception {
		this.mockMvc.perform(
						get("/tasks/executions/current")
				)
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						responseFields(
								fieldWithPath("[].name").description("The name of the platform instance (account)"),
								fieldWithPath("[].type").description("The platform type"),
								fieldWithPath("[].maximumTaskExecutions").description("The number of maximum task execution"),
								fieldWithPath("[].runningExecutionCount").description("The number of running executions")
						)
				));
	}

	@Test
	public void getTaskDisplayDetail() throws Exception {
		this.mockMvc.perform(
						get("/tasks/executions/{id}", "1")
				)
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(
								parameterWithName("id").description("The id of an existing task execution (required)")
						),
						responseFields(
								fieldWithPath("executionId").description("The id of the task execution"),
								fieldWithPath("exitCode").description("The exit code of the task execution"),
								fieldWithPath("taskName").description("The task name related to the task execution"),
								fieldWithPath("startTime").description("The start time of the task execution"),
								fieldWithPath("endTime").description("The end time of the task execution"),
								fieldWithPath("exitMessage").description("The exit message of the task execution"),
								fieldWithPath("arguments").description("The arguments of the task execution"),
								fieldWithPath("jobExecutionIds").description("The job executions ids of the task executions"),
								fieldWithPath("errorMessage").description("The error message of the task execution"),
								fieldWithPath("externalExecutionId").description("The external id of the task execution"),
								fieldWithPath("taskExecutionStatus").description("The status of the task execution"),
								fieldWithPath("parentExecutionId").description("The id of parent task execution, " +
										"null if task execution does not have parent"),
								fieldWithPath("resourceUrl").description("The resource URL that defines the task that was executed"),
								subsectionWithPath("appProperties").description("The application properties of the task execution"),
								subsectionWithPath("deploymentProperties").description("The deployment properties of the task execution"),
								subsectionWithPath("platformName").description("The platform selected for the task execution"),
								subsectionWithPath("_links.self").description("Link to the task execution resource"),
								subsectionWithPath("_links.tasks/logs").description("Link to the task execution logs")
						)
				));
	}

	@Test
	public void getTaskDisplayDetailByExternalId() throws Exception {
		final AtomicReference<String> externalExecutionId = new AtomicReference<>(null);
		documentation.dontDocument(() -> {
			MvcResult mvcResult = this.mockMvc.perform(
							get("/tasks/executions")
									.queryParam("page", "0")
									.queryParam("size", "20"))
					.andExpect(status().isOk()).andReturn();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(mvcResult.getResponse().getContentAsString());
			JsonNode list = node.get("_embedded").get("taskExecutionResourceList");
			JsonNode first = list.get(0);
			externalExecutionId.set(first.get("externalExecutionId").asText());
			return externalExecutionId.get();
		});

		this.mockMvc.perform(
						get("/tasks/executions/external/{externalExecutionId}", externalExecutionId.get()).queryParam("platform", "default")
				)
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(
								parameterWithName("externalExecutionId").description("The external ExecutionId of an existing task execution (required)")
						),
						queryParameters(
								parameterWithName("platform").description("The name of the platform.")
						),
						responseFields(
								fieldWithPath("executionId").description("The id of the task execution"),
								fieldWithPath("exitCode").description("The exit code of the task execution"),
								fieldWithPath("taskName").description("The task name related to the task execution"),
								fieldWithPath("startTime").description("The start time of the task execution"),
								fieldWithPath("endTime").description("The end time of the task execution"),
								fieldWithPath("exitMessage").description("The exit message of the task execution"),
								fieldWithPath("arguments").description("The arguments of the task execution"),
								fieldWithPath("jobExecutionIds").description("The job executions ids of the task executions"),
								fieldWithPath("errorMessage").description("The error message of the task execution"),
								fieldWithPath("externalExecutionId").description("The external id of the task execution"),
								fieldWithPath("taskExecutionStatus").description("The status of the task execution"),
								fieldWithPath("parentExecutionId").description("The id of parent task execution, " +
										"null if task execution does not have parent"),
								fieldWithPath("resourceUrl").description("The resource URL that defines the task that was executed"),
								subsectionWithPath("appProperties").description("The application properties of the task execution"),
								subsectionWithPath("deploymentProperties").description("The deployment properties of the task execution"),
								subsectionWithPath("platformName").description("The platform selected for the task execution"),
								subsectionWithPath("_links.self").description("Link to the task execution resource"),
								subsectionWithPath("_links.tasks/logs").description("Link to the task execution logs")
						)
				));
	}
	@Test
	@Disabled("find error")
	public void listTaskExecutions() throws Exception {
		documentation.dontDocument(() -> this.mockMvc.perform(
						post("/tasks/executions")
								.queryParam("name", "taskB")
								.queryParam("properties", "app.my-task.foo=bar,deployer.my-task.something-else=3")
								.queryParam("arguments", "--server.port=8080 --foo=bar")
				)
				.andExpect(status().isCreated()));

		this.mockMvc.perform(
						get("/tasks/executions")
								.queryParam("page", "1")
								.queryParam("size", "2"))
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)")
						),
						responseFields(
								subsectionWithPath("_embedded.taskExecutionResourceList")
										.description("Contains a collection of Task Executions/"),
								subsectionWithPath("_links.self").description("Link to the task execution resource"),
								subsectionWithPath("_links.first").description("Link to the first page of task execution resources").optional(),
								subsectionWithPath("_links.last").description("Link to the last page of task execution resources").optional(),
								subsectionWithPath("_links.next").description("Link to the next page of task execution resources").optional(),
								subsectionWithPath("_links.prev").description("Link to the previous page of task execution resources").optional(),
								subsectionWithPath("page").description("Pagination properties"))));
	}

	@Test
	@Disabled("find error")
	public void listTaskThinExecutions() throws Exception {
		documentation.dontDocument(() -> this.mockMvc.perform(
				post("/tasks/executions")
					.queryParam("name", "taskB")
					.queryParam("properties", "app.my-task.foo=bar,deployer.my-task.something-else=3")
					.queryParam("arguments", "--server.port=8080 --foo=bar")
			)
			.andExpect(status().isCreated()));

		this.mockMvc.perform(
				get("/tasks/thinexecutions")
					.queryParam("page", "1")
					.queryParam("size", "2"))
			.andExpect(status().isOk()).andDo(this.documentationHandler.document(
				queryParameters(
					parameterWithName("page")
						.description("The zero-based page number (optional)"),
					parameterWithName("size")
						.description("The requested page size (optional)")
				),
				responseFields(
					subsectionWithPath("_embedded.taskExecutionThinResourceList")
						.description("Contains a collection of thin Task Executions/"),
					subsectionWithPath("_links.self").description("Link to the task execution resource"),
					subsectionWithPath("_links.first").description("Link to the first page of task execution resources").optional(),
					subsectionWithPath("_links.last").description("Link to the last page of task execution resources").optional(),
					subsectionWithPath("_links.next").description("Link to the next page of task execution resources").optional(),
					subsectionWithPath("_links.prev").description("Link to the previous page of task execution resources").optional(),
					subsectionWithPath("page").description("Pagination properties"))));
	}

	@Test
	public void listTaskExecutionsByName() throws Exception {
		this.mockMvc.perform(
						get("/tasks/executions")
								.queryParam("name", "taskB")
								.queryParam("page", "0")
								.queryParam("size", "10")
				)
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)"),
								parameterWithName("name")
										.description("The name associated with the task execution")),
						responseFields(
								subsectionWithPath("_embedded.taskExecutionResourceList")
										.description("Contains a collection of Task Executions/"),
								subsectionWithPath("_links.self").description("Link to the task execution resource"),
								subsectionWithPath("page").description("Pagination properties"))));
	}

	@Test
	public void stopTask() throws Exception {
		this.mockMvc.perform(
						post("/tasks/executions")
								.queryParam("name", "taskA")
								.queryParam("properties", "app.my-task.foo=bar,deployer.my-task.something-else=3")
								.queryParam("arguments", "--server.port=8080 --foo=bar")
				)
				.andExpect(status().isCreated());
		this.mockMvc.perform(
						post("/tasks/executions/{id}", 1)
				)
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
								pathParameters(
										parameterWithName("id").description("The ids of an existing task execution (required)")
								)
						)
				);
	}

	@Test
	public void taskExecutionRemove() throws Exception {

		documentation.dontDocument(() -> this.mockMvc.perform(
						post("/tasks/executions")
								.queryParam("name", "taskB")
								.queryParam("properties", "app.my-task.foo=bar,deployer.my-task.something-else=3")
								.queryParam("arguments", "--server.port=8080 --foo=bar"))
				.andExpect(status().isCreated()));

		this.mockMvc.perform(
						delete("/tasks/executions/{ids}?action=CLEANUP", "1"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						queryParameters(parameterWithName("action").description("Optional. Defaults to: CLEANUP.")),
						pathParameters(parameterWithName("ids")
								.description("The id of an existing task execution (required). Multiple comma separated values are accepted."))
				));
	}

	@Test
	public void taskExecutionRemoveAndTaskDataRemove() throws Exception {
		this.mockMvc.perform(
						delete("/tasks/executions/{ids}?action=CLEANUP,REMOVE_DATA", "1,2"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("action").description("Using both actions CLEANUP and REMOVE_DATA simultaneously.")
						),
						pathParameters(parameterWithName("ids")
								.description("Providing 2 comma separated task execution id values.")
						)
				));

	}

	private void createTaskDefinition(String taskName) throws Exception {
		documentation.dontDocument(() ->
			this.mockMvc.perform(
				post("/tasks/definitions")
						.queryParam("name", taskName)
						.queryParam("definition", "timestamp --format='yyyy MM dd'")
				)
		);
	}
	private void cleanupTaskExecutions(String taskName) throws Exception {
		documentation.dontDocument(() -> this.mockMvc.perform(
				delete("/tasks/executions")
						.queryParam("name", taskName)
			)
		);
	}
	private void destroyTaskDefinition(String taskName) throws Exception {
		documentation.dontDocument(() ->
			this.mockMvc.perform(
						delete("/tasks/definitions/{name}", taskName)
			)
		);
	}

	private void executeTask(String taskName) throws Exception {
		documentation.dontDocument(() ->
				this.mockMvc.perform(
						post("/tasks/executions")
								.queryParam("name", taskName)
								.queryParam("arguments", "--server.port=8080 --foo=bar")
				)
		);
	}
}
