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

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

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
 * Documentation for the /streams/definitions endpoint.
 *
 * @author Gunnar Hillert
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StreamDefinitionsDocumentation extends BaseDocumentation {

	private static boolean setUpIsDone = false;

	@Before
	public void setup() throws Exception {
		if (setUpIsDone) {
			return;
		}

		this.mockMvc.perform(
			post("/apps/{type}/time", "source")
					.param("uri", "maven://org.springframework.cloud.stream.app:time-source-rabbit:1.2.0.RELEASE"))
			.andExpect(status().isCreated());
		this.mockMvc.perform(
			post("/apps/{type}/log", "sink")
					.param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE"))
			.andExpect(status().isCreated());
		setUpIsDone = true;
	}

	@Test
	public void createDefinition() throws Exception {
		this.mockMvc.perform(
			post("/streams/definitions")
					.param("name", "timelog")
					.param("definition", "time --format='YYYY MM DD' | log")
					.param("deploy", "false"))
			.andExpect(status().isCreated())
			.andDo(this.documentationHandler.document(
					requestParameters(
						parameterWithName("name")
							.description("The name for the created task definitions"),
						parameterWithName("definition")
							.description("The definition for the stream, using Data Flow DSL"),
						parameterWithName("deploy")
							.description("If true, the stream is deployed upon creation (default is false"))));
	}

	@Test
	public void listAllStreamDefinitions() throws Exception {
		this.mockMvc.perform(
			get("/streams/definitions")
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
				fieldWithPath("_embedded.streamDefinitionResourceList")
					.description("Contains a collection of Stream Definitions/"),
				fieldWithPath("_links.self").description("Link to the stream definitions resource"),
				fieldWithPath("page").description("Pagination properties"))));
	}

	@Test
	public void listRelatedStreamDefinitions() throws Exception {
		this.mockMvc.perform(
			get("/streams/definitions/{name}/related", "timelog").param("nested", "true"))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
					requestParameters(
							parameterWithName("nested")
								.description("Should we recursively findByNameLike for related stream definitions (optional)")),
					pathParameters(parameterWithName("name")
						.description("The name of an existing stream definition (required)"))
				));
	}

	@Test
	public void streamDefinitionDelete1() throws Exception {
		this.mockMvc.perform(
			delete("/streams/definitions/{name}", "timelog"))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				pathParameters(parameterWithName("name")
					.description("The name of an existing stream definition (required)"))
			));
	}

	@Test
	public void streamDefinitionDeleteAll() throws Exception {
		this.mockMvc.perform(
			delete("/streams/definitions"))
			.andDo(print())
			.andExpect(status().isOk());
	}

}
