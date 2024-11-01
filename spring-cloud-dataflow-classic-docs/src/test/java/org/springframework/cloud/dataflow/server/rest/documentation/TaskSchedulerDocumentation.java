/*
 * Copyright 2018 the original author or authors.
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
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the /tasks/schedules endpoint.
 *
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@SuppressWarnings({"NewClassNamingConvention","SameParameterValue"})
@TestMethodOrder(MethodOrderer.MethodName.class)
class TaskSchedulerDocumentation extends BaseDocumentation {

	@BeforeEach
	void setup() throws Exception {
		registerApp(ApplicationType.task, "timestamp", "3.0.0");
		createTaskDefinition("mytaskname");
	}

	@AfterEach
	void tearDown() throws Exception {
		destroyTaskDefinition("mytaskname");
		unregisterApp(ApplicationType.task, "timestamp");
	}

	@Test
	void createSchedule() throws Exception {
		this.mockMvc.perform(
				post("/tasks/schedules")
						.queryParam("scheduleName", "myschedule")
						.queryParam("taskDefinitionName", "mytaskname")
						.queryParam("platform", "default")
						.queryParam("properties", "deployer.cron.expression=00 22 17 ? *")
						.queryParam("arguments", "--foo=bar"))
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("scheduleName").description("The name for the created schedule"),
								parameterWithName("platform").optional().description("The name of the platform the task is launched"),
								parameterWithName("taskDefinitionName")
										.description("The name of the task definition to be scheduled"),
								parameterWithName("properties")
										.description("the properties that are required to schedule and launch the task"),
								parameterWithName("arguments").optional().description("the command line arguments to be used for launching the task"))));
	}

	@Test
	void deleteSchedule() throws Exception {
		this.mockMvc.perform(
				delete("/tasks/schedules/{scheduleName}", "mytestschedule"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("scheduleName")
								.description("The name of an existing schedule"))));
	}

	@Test
	void listFilteredSchedules() throws Exception {
		this.mockMvc.perform(
				get("/tasks/schedules/instances/{task-definition-name}", "FOO")
						.queryParam("page", "0")
						.queryParam("size", "10"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("task-definition-name")
								.description("Filter schedules based on the specified task definition")),
						queryParameters(
								parameterWithName("page").optional()
										.description("The zero-based page number"),
								parameterWithName("size").optional()
										.description("The requested page size")),
						responseFields(
								subsectionWithPath("_embedded.scheduleInfoResourceList")
										.description("Contains a collection of Schedules/"),
								subsectionWithPath("_links.self").description("Link to the schedule resource"),
								subsectionWithPath("page").description("Pagination properties"))));
	}

	@Test
	void listAllSchedules() throws Exception {
		this.mockMvc.perform(
				get("/tasks/schedules")
						.queryParam("page", "0")
						.queryParam("size", "10"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("page").optional()
										.description("The zero-based page number"),
								parameterWithName("size").optional()
										.description("The requested page size")),
						responseFields(
								subsectionWithPath("_embedded.scheduleInfoResourceList")
										.description("Contains a collection of Schedules/"),
								subsectionWithPath("_links.self").description("Link to the schedule resource"),
								subsectionWithPath("page").description("Pagination properties"))));
	}

	private void createTaskDefinition(String taskName) throws Exception{
		documentation.dontDocument( () -> this.mockMvc.perform(
				post("/tasks/definitions")
						.queryParam("name", taskName)
						.queryParam("definition", "timestamp --format='yyyy MM dd'"))
				.andExpect(status().isOk()));
	}

	private void destroyTaskDefinition(String taskName) throws Exception{
		documentation.dontDocument( () -> this.mockMvc.perform(
				delete("/tasks/definitions/{name}", taskName))
				.andExpect(status().isOk()));
	}
}
