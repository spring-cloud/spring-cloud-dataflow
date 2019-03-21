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

import static org.springframework.cloud.dataflow.core.ApplicationType.task;
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
 * Documentation for the /tasks/definitions endpoint.
 *
 * @author Eric Bottard
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TaskDefinitionsDocumentation extends BaseDocumentation {

	@Before
	public void setup() throws Exception {
		registerApp(task, "timestamp");
	}

	@After
	public void tearDown() throws Exception {
		unregisterApp(task, "timestamp");
	}

	@Test
	public void createDefinition() throws Exception {
		this.mockMvc.perform(
				post("/tasks/definitions")
						.param("name", "my-task")
						.param("definition", "timestamp --format='YYYY MM DD'"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						requestParameters(
								parameterWithName("name").description("The name for the created task definition"),
								parameterWithName("definition")
										.description("The definition for the task, using Data Flow DSL"))));
	}

	@Test
	public void listAllTaskDefinitions() throws Exception {
		this.mockMvc.perform(
				get("/tasks/definitions")
						.param("page", "0")
						.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						requestParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)")),
						responseFields(
								fieldWithPath("_embedded.taskDefinitionResourceList")
										.description("Contains a collection of Task Definitions/"),
								fieldWithPath("_links.self").description("Link to the task definitions resource"),
								fieldWithPath("page").description("Pagination properties"))));
	}

	@Test
	public void displayDetail() throws Exception {
		this.mockMvc.perform(
				get("/tasks/definitions/{my-task}","my-task"))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("my-task")
								.description("The name of an existing task definition (required)"))));
	}

	@Test
	public void taskDefinitionDelete() throws Exception {
		this.mockMvc.perform(
				delete("/tasks/definitions/{my-task}", "my-task"))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("my-task")
								.description("The name of an existing task definition (required)"))
				));
	}

}
