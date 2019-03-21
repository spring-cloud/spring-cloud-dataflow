/*
 * Copyright 2017 the original author or authors.
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
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import org.springframework.cloud.dataflow.core.ApplicationType;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the /tasks/executions endpoint.
 *
 * @author Eric Bottard
 * @author Glenn Renfro
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TaskExecutionsDocumentation extends BaseDocumentation {

	@Before
	public void setup() throws Exception {
		registerApp(ApplicationType.task, "timestamp");
		createTaskDefinition("taskA");
		createTaskDefinition("taskB");

	}

	@After
	public void tearDown() throws Exception {
		destroyTaskDefinition("taskA");
		destroyTaskDefinition("taskB");
		unregisterApp(ApplicationType.task, "timestamp");
	}

	@Test
	public void launchTask() throws Exception {
		this.mockMvc.perform(
				post("/tasks/executions")
						.param("name", "taskA")
						.param("properties", "app.my-task.foo=bar,deployer.my-task.something-else=3")
						.param("arguments", "--server.port=8080 --foo=bar"))
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
						requestParameters(
								parameterWithName("name").description("The name of the task definition to launch"),
								parameterWithName("properties").optional()
										.description("Application and Deployer properties to use while launching"),
								parameterWithName("arguments").optional()
										.description("Command line arguments to pass to the task"))));
	}

	@Test
	public void listTaskExecutions() throws Exception {
		documentation.dontDocument( () -> this.mockMvc.perform(
				post("/tasks/executions")
						.param("name", "taskB")
						.param("properties", "app.my-task.foo=bar,deployer.my-task.something-else=3")
						.param("arguments", "--server.port=8080 --foo=bar"))
				.andExpect(status().isCreated()));

		this.mockMvc.perform(
				get("/tasks/executions")
						.param("page", "0")
						.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
				requestParameters(
						parameterWithName("page")
								.description("The zero-based page number (optional)"),
						parameterWithName("size")
								.description("The requested page size (optional)")),
				responseFields(
						fieldWithPath("_embedded.taskExecutionResourceList")
								.description("Contains a collection of Task Executions/"),
						fieldWithPath("_links.self").description("Link to the task execution resource"),
						fieldWithPath("page").description("Pagination properties"))));
	}

	@Test
	public void listTaskExecutionsByName() throws Exception {
		this.mockMvc.perform(
				get("/tasks/executions")
						.param("name", "taskB")
						.param("page", "0")
						.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(
				requestParameters(
						parameterWithName("page")
								.description("The zero-based page number (optional)"),
						parameterWithName("size")
								.description("The requested page size (optional)"),
						parameterWithName("name")
								.description("The name associated with the task execution")),
				responseFields(
						fieldWithPath("_embedded.taskExecutionResourceList")
								.description("Contains a collection of Task Executions/"),
						fieldWithPath("_links.self").description("Link to the task execution resource"),
						fieldWithPath("page").description("Pagination properties"))));
	}

	@Test
	public void launchTaskDisplayDetail() throws Exception {
		this.mockMvc.perform(
				get("/tasks/executions/{id}","1"))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("id")
								.description("The id of an existing task execution (required)"))));
	}


	@Test
	public void removeTaskExecution() throws Exception {
		this.mockMvc.perform(
				delete("/tasks/executions/{id}", "1"))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("id")
								.description("The id of an existing task execution (required)"))
				));
	}

	private void createTaskDefinition(String taskName) throws Exception{
		documentation.dontDocument( () -> this.mockMvc.perform(
				post("/tasks/definitions")
						.param("name", taskName)
						.param("definition", "timestamp --format='yyyy MM dd'"))
				.andExpect(status().isOk()));
	}

	private void destroyTaskDefinition(String taskName) throws Exception{
		documentation.dontDocument( () -> this.mockMvc.perform(
				delete("/tasks/definitions/{name}", taskName))
				.andExpect(status().isOk()));
	}
}
