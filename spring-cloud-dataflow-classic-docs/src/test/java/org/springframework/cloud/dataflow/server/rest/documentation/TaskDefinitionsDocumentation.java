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

package org.springframework.cloud.dataflow.server.rest.documentation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.cloud.dataflow.core.ApplicationType;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the /tasks/definitions endpoint.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings("NewClassNamingConvention")
@TestMethodOrder(MethodName.class)
public class TaskDefinitionsDocumentation extends BaseDocumentation {

	@BeforeEach
	public void setup() throws Exception {
		registerApp(ApplicationType.task, "timestamp", "1.2.0.RELEASE");
	}

	@AfterEach
	public void tearDown() throws Exception {
		unregisterApp(ApplicationType.task, "timestamp");
	}

	@Test
	public void createDefinition() throws Exception {
		this.mockMvc.perform(
			post("/tasks/definitions")
				.param("name", "my-task")
				.param("definition", "timestamp --format='YYYY MM DD'")
				.param("description", "Demo task definition for testing"))
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				requestParameters(
					parameterWithName("name").description("The name for the created task definition"),
					parameterWithName("definition").description("The definition for the task, using Data Flow DSL"),
					parameterWithName("description").description("The description of the task definition")
				),
				responseFields(
					fieldWithPath("name").description("The name of the created task definition"),
					fieldWithPath("dslText").description("The DSL of the created task definition"),
					fieldWithPath("description").description("The description of the task definition"),
					fieldWithPath("composed").description("The compose attribute of the created task definition"),
					fieldWithPath("lastTaskExecution")
							.description("The last task execution of the created task definition"),
					fieldWithPath("status").description("The status of the created task definition"),
					fieldWithPath("composedTaskElement").description("specifies whether a definition is member of a composed task"),
					subsectionWithPath("_links.self").description("Link to the created task definition resource")
				)
			));
	}

	@Test
	public void listAllTaskDefinitions() throws Exception {
		documentation.dontDocument(()->this.mockMvc.perform(
				post("/tasks/definitions")
					.param("name", "my-task")
					.param("definition", "timestamp --format='YYYY MM DD'")
					.param("description", "Demo task definition for testing"))
			.andExpect(status().isOk()));
		this.mockMvc.perform(
			get("/tasks/definitions")
				.param("page", "0")
				.param("size", "10")
				.param("sort", "taskName,ASC")
				.param("search", "")
				.param("manifest", "true")
			)
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				requestParameters(
					parameterWithName("page").description("The zero-based page number (optional)"),
					parameterWithName("size").description("The requested page size (optional)"),
					parameterWithName("search").description("The search string performed on the name (optional)"),
					parameterWithName("sort").description("The sort on the list (optional)"),
					parameterWithName("manifest").description("The flag to include the task manifest into the latest task execution (optional)")
				),
				responseFields(
					subsectionWithPath("_embedded.taskDefinitionResourceList")
						.description("Contains a collection of Task Definitions/"),
					subsectionWithPath("_links.self").description("Link to the task definitions resource"),
					subsectionWithPath("page").description("Pagination properties"))));
	}

	@Test
	public void displayDetail() throws Exception {
		documentation.dontDocument(()->this.mockMvc.perform(
				post("/tasks/definitions")
					.param("name", "my-task")
					.param("definition", "timestamp --format='YYYY MM DD'")
					.param("description", "Demo task definition for testing"))
			.andExpect(status().isOk()));
		this.mockMvc.perform(
			get("/tasks/definitions/{my-task}","my-task")
			.param("manifest", "true"))
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				pathParameters(
					parameterWithName("my-task").description("The name of an existing task definition (required)")
				),
				requestParameters(
					parameterWithName("manifest").description("The flag to include the task manifest into the latest task execution (optional)")
				),
				responseFields(
					fieldWithPath("name").description("The name of the created task definition"),
					fieldWithPath("dslText").description("The DSL of the created task definition"),
					fieldWithPath("description").description("The description of the task definition"),
					fieldWithPath("composed").description("The compose attribute of the created task definition"),
					fieldWithPath("lastTaskExecution")
						.description("The last task execution of the created task definition"),
					fieldWithPath("status").description("The status of the created task definition"),
					fieldWithPath("composedTaskElement").description("specifies whether a definition is member of a composed task"),
					subsectionWithPath("_links.self").description("Link to the created task definition resource")
				)
			));
	}

	@Test
	public void taskDefinitionDelete() throws Exception {
		documentation.dontDocument(()->this.mockMvc.perform(
				post("/tasks/definitions")
					.param("name", "my-task")
					.param("definition", "timestamp --format='YYYY MM DD'")
					.param("description", "Demo task definition for testing"))
			.andExpect(status().isOk()));
		this.mockMvc.perform(
			delete("/tasks/definitions/{my-task}", "my-task")
			.param("cleanup", "true"))
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				pathParameters(
					parameterWithName("my-task").description("The name of an existing task definition (required)")),
				requestParameters(
						parameterWithName("cleanup").description("The flag to indicate if the associated task executions needed to be cleaned up")
				)
			));
	}

}
