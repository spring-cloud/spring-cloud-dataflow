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
import org.junit.jupiter.api.MethodOrderer.MethodName;
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
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the /tasks/schedules endpoint.
 *
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@SuppressWarnings({"NewClassNamingConvention", "SameParameterValue"})
@TestMethodOrder(MethodName.class)
public class TaskSchedulerDocumentation extends BaseDocumentation {

	@BeforeEach
	public void setup() throws Exception {
		registerApp(ApplicationType.task, "timestamp", "1.2.0.RELEASE");
		createTaskDefinition("mytaskname");
	}

	@AfterEach
	public void tearDown() throws Exception {
		destroyTaskDefinition("mytaskname");
		unregisterApp(ApplicationType.task, "timestamp");
	}

	@Test
	public void createSchedule() throws Exception {
		this.mockMvc.perform(
				post("/tasks/schedules")
						.param("scheduleName", "myschedule")
						.param("taskDefinitionName", "mytaskname")
						.param("platform", "default")
						.param("properties", "scheduler.cron.expression=00 22 17 ? *")
						.param("arguments", "--foo=bar"))
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
						requestParameters(
								parameterWithName("scheduleName").description("The name for the created schedule"),
								parameterWithName("platform").description("The name of the platform the task is launched"),
								parameterWithName("taskDefinitionName")
										.description("The name of the task definition to be scheduled"),
								parameterWithName("properties")
										.description("the properties that are required to schedule and launch the task"),
								parameterWithName("arguments").description("the command line arguments to be used for launching the task"))));
	}

	@Test
	public void deleteSchedule() throws Exception {
		this.mockMvc.perform(
				delete("/tasks/schedules/{scheduleName}", "mytestschedule"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("scheduleName")
								.description("The name of an existing schedule (required)"))));
	}

	@Test
	public void listFilteredSchedules() throws Exception {
		this.mockMvc.perform(
				get("/tasks/schedules/instances/{task-definition-name}", "FOO")
						.param("page", "0")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("task-definition-name")
								.description("Filter schedules based on the specified task definition (required)")),
						requestParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)")),
						responseFields(
								subsectionWithPath("_embedded.scheduleInfoResourceList")
										.description("Contains a collection of Schedules/"),
								subsectionWithPath("_links.self").description("Link to the schedule resource"),
								subsectionWithPath("page").description("Pagination properties"))));
	}

	@Test
	public void listAllSchedules() throws Exception {
		this.mockMvc.perform(
				get("/tasks/schedules")
						.param("page", "0")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						requestParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)")),
						responseFields(
								subsectionWithPath("_embedded.scheduleInfoResourceList")
										.description("Contains a collection of Schedules/"),
								subsectionWithPath("_links.self").description("Link to the schedule resource"),
								subsectionWithPath("page").description("Pagination properties"))));
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
