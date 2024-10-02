/*
 * Copyright 2021 the original author or authors.
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.cloud.dataflow.core.ApplicationType;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the /tasks/info endpoint.
 *
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
@TestMethodOrder(MethodOrderer.MethodName.class)
class TasksInfoDocumentation extends BaseDocumentation {

	@BeforeEach
	void setup() throws Exception {
		registerApp(ApplicationType.task, "timestamp", "3.0.0");
		createTaskDefinition("taskA");
		createTaskDefinition("taskB");

	}

	@AfterEach
	void tearDown() throws Exception {
		destroyTaskDefinition("taskA");
		destroyTaskDefinition("taskB");
		unregisterApp(ApplicationType.task, "timestamp");
	}

	@Test
	void getTaskExecutionsInfo() throws Exception {
		this.mockMvc.perform(
				get("/tasks/info/executions?completed=false"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						responseFields(
								fieldWithPath("totalExecutions").description("Total number of task executions"),
								subsectionWithPath("_links.self").description("Link to the task executions resource")
						)
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
