/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.rest.documentation;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.RequestDispatcher;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.springframework.restdocs.JUnitRestDocumentation;

/**
 * @author Gunnar Hillert
 */
public class ApiDocumentation extends BaseDocumentation {

	@ClassRule
	public static TestRule springDataflowServer = localDataflowResource;

	@Rule
	public JUnitRestDocumentation restDocumentation =
			new JUnitRestDocumentation(TARGET_DIRECTORY);

	@Before
	public void setupMocks() {
		super.prepareDocumentationTests(restDocumentation);
	}

	@Test
	public void headers() throws Exception {
		this.mockMvc
			.perform(get("/"))
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				responseHeaders(
					headerWithName("Content-Type").description("The Content-Type of the payload, e.g. `application/hal+json`"))));
	}

	@Test
	public void errors() throws Exception {
		this.mockMvc
			.perform(get("/error")
				.requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
				.requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/apps")
				.requestAttr(RequestDispatcher.ERROR_MESSAGE, "The app 'http://localhost:8080/apps/123' does not exist"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("error", is("Bad Request")))
			.andExpect(jsonPath("timestamp", is(notNullValue())))
			.andExpect(jsonPath("status", is(400)))
			.andExpect(jsonPath("path", is(notNullValue())))
			.andDo(this.documentationHandler.document(
				responseFields(
					fieldWithPath("error").description("The HTTP error that occurred, e.g. `Bad Request`"),
					fieldWithPath("message").description("A description of the cause of the error"),
					fieldWithPath("path").description("The path to which the request was made"),
					fieldWithPath("status").description("The HTTP status code, e.g. `400`"),
					fieldWithPath("timestamp").description("The time, in milliseconds, at which the error occurred"))));
	}

	@Test
	public void index() throws Exception {
		this.mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				links(
					linkWithRel("apps").description("Handle registered applications"),
					linkWithRel("completions/stream").description("Exposes the DSL completion features"),
					linkWithRel("jobs/executions").description("The JobExecution resource"),
					linkWithRel("jobs/executions/execution").description("Details for a specific JobExecution"),
					linkWithRel("jobs/executions/execution/steps").description("All steps for a JobExecution"),
					linkWithRel("jobs/executions/execution/steps/step").description("Details for a specific step"),
					linkWithRel("jobs/executions/execution/steps/step/progress").description("Progress information for a specific step"),
					linkWithRel("jobs/executions/name").description("Retrieve Job Executions by Job name"),
					linkWithRel("jobs/instances/instance").description("Job instance resource for a specific job instance"),
					linkWithRel("jobs/instances/name").description("Job instance resource for a specific job name"),
					linkWithRel("runtime/apps").description("Runtime application resource"),
					linkWithRel("runtime/apps/app").description("Exposes the runtime status for a specific app"),
					linkWithRel("runtime/apps/instances").description("Status for an individual app instance"),
					linkWithRel("tasks/definitions").description("Task definition resource"),
					linkWithRel("tasks/definitions/definition").description("A specific task definition"),
					linkWithRel("tasks/deployments").description("Resource for deployment operations"),
					linkWithRel("tasks/deployments/deployment").description("Launch a task"),
					linkWithRel("tasks/executions").description("Returns Task executions"),
					linkWithRel("tasks/executions/name").description("Returns all task executions for a given Task name"),
					linkWithRel("tasks/executions/execution").description("Details for a specific task execution"),
					linkWithRel("streams/definitions").description("The Streams resource"),
					linkWithRel("streams/definitions/definition").description("Handle a specific Stream definition"),
					linkWithRel("streams/deployments").description("Resource for Stream deployment operations"),
					linkWithRel("streams/deployments/deployment").description("Request (un-)deployment of an existing stream definition"),
					linkWithRel("counters").description("Counter resource"),
					linkWithRel("counters/counter").description(""),
					linkWithRel("aggregate-counters").description(""),
					linkWithRel("aggregate-counters/counter").description(""),
					linkWithRel("field-value-counters").description(""),
					linkWithRel("field-value-counters/counter").description("")),
				responseFields(
					fieldWithPath("_links").description("Links to other resources"))));
	}
}
