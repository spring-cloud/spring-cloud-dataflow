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

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;

import org.springframework.cloud.dataflow.core.ApplicationType;

/**
 * Documentation for the /streams/definitions endpoint.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Disabled("find error")
public class StreamDefinitionsDocumentation extends BaseDocumentation {

	private static boolean setUpIsDone = false;

	@BeforeEach
	public void setup() throws Exception {
		if (setUpIsDone) {
			return;
		}

		this.mockMvc.perform(
			post("/apps/{type}/time", "source")
					.queryParam("uri", "maven://org.springframework.cloud.stream.app:time-source-rabbit:1.2.0.RELEASE"))
			.andExpect(status().isCreated());
		this.mockMvc.perform(
			post("/apps/{type}/log", "sink")
					.queryParam("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE"))
			.andExpect(status().isCreated());
		setUpIsDone = true;
	}

	@Test
	public void createDefinition() throws Exception {
		this.mockMvc.perform(
			post("/streams/definitions")
					.queryParam("name", "timelog")
					.queryParam("definition", "time --format='YYYY MM DD' | log")
					.queryParam("description", "Demo stream for testing")
					.queryParam("deploy", "false"))
			.andExpect(status().isCreated())
			.andDo(this.documentationHandler.document(
				queryParameters(
					parameterWithName("name").description("The name for the created task definitions"),
					parameterWithName("definition").description("The definition for the stream, using Data Flow DSL"),
					parameterWithName("description").description("The description of the stream definition"),
					parameterWithName("deploy")
						.description("If true, the stream is deployed upon creation (default is false)")),
				responseFields(
						fieldWithPath("name").description("The name of the created stream definition"),
						fieldWithPath("dslText").description("The DSL of the created stream definition"),
						fieldWithPath("originalDslText").description("The original DSL of the created stream definition"),
						fieldWithPath("status").description("The status of the created stream definition"),
						fieldWithPath("description").description("The description of the stream definition"),
						fieldWithPath("statusDescription")
								.description("The status description of the created stream definition"),
						subsectionWithPath("_links.self").description("Link to the created stream definition resource")
				)
			));
	}

	@Test
	public void listAllStreamDefinitions() throws Exception {
		this.mockMvc.perform(
			get("/streams/definitions")
				.queryParam("page", "0")
				.queryParam("sort", "name,ASC")
				.queryParam("search", "")
				.queryParam("size", "10"))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				queryParameters(
					parameterWithName("page").description("The zero-based page number (optional)"),
					parameterWithName("search").description("The search string performed on the name (optional)"),
					parameterWithName("sort").description("The sort on the list (optional)"),
					parameterWithName("size").description("The requested page size (optional)")),
				responseFields(
					subsectionWithPath("_embedded.streamDefinitionResourceList")
						.description("Contains a collection of Stream Definitions"),
					subsectionWithPath("_links.self").description("Link to the stream definitions resource"),
					subsectionWithPath("page").description("Pagination properties"))));
	}

	@Test
	public void getStreamDefinition() throws Exception {
		this.mockMvc.perform(
				get("/streams/definitions/{name}", "timelog"))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(
								parameterWithName("name").description("The name of the stream definition to query (required)")
						),
						responseFields(
								fieldWithPath("name").description("The name of the stream definition"),
								fieldWithPath("dslText").description("The DSL of the stream definition"),
								fieldWithPath("originalDslText").description("The original DSL of the stream definition"),
								fieldWithPath("status").description("The status of the stream definition"),
								fieldWithPath("description").description("The description of the stream definition"),
								fieldWithPath("statusDescription")
										.description("The status description of the stream definition"),
								subsectionWithPath("_links.self").description("Link to the stream definition resource")
						)));
	}

	@Test
	public void getStreamApplications() throws Exception {
		createStream("mysamplestream", "time | log", false);
		this.mockMvc.perform(
				get("/streams/definitions/{name}/applications", "mysamplestream"))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(
								parameterWithName("name").description("The name of the stream definition to query (required)")
						),
						responseFields(
								fieldWithPath("[]").description("An array of applications"),
								fieldWithPath("[].name").description("The name of the application"),
								fieldWithPath("[].label").description("The label given for the application"),
								fieldWithPath("[].type").description("The type of the application. One of " + Arrays
										.asList(ApplicationType.values())),
								fieldWithPath("[].uri").description("The uri of the application"),
								fieldWithPath("[].version").description("The version of the application"),
								fieldWithPath("[].defaultVersion").description("If true, the application is the default version"),
								fieldWithPath("[].versions").description("All the registered versions of the application"),
								fieldWithPath("[]._links.self.href").description("Link to the application resource")
						)));
	}

	@Test
	public void listRelatedStreamDefinitions() throws Exception {
		this.mockMvc.perform(
			get("/streams/definitions/{name}/related", "timelog")
                    .queryParam("page", "0")
                    .queryParam("sort", "name,ASC")
                    .queryParam("search", "")
                    .queryParam("size", "10")
					.queryParam("nested", "true"))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
                queryParameters(
                    parameterWithName("nested")
                        .description("Should we recursively findByTaskNameContains for related stream definitions (optional)"),
                    parameterWithName("page").description("The zero-based page number (optional)"),
                    parameterWithName("search").description("The search string performed on the name (optional)"),
                    parameterWithName("sort").description("The sort on the list (optional)"),
                    parameterWithName("size").description("The requested page size (optional)")),
                pathParameters(parameterWithName("name")
                    .description("The name of an existing stream definition (required)")),
                responseFields(
                    subsectionWithPath("_embedded.streamDefinitionResourceList")
                        .description("Contains a collection of Stream Definitions"),
                    subsectionWithPath("_links.self").description("Link to the stream definitions resource"),
                    subsectionWithPath("page").description("Pagination properties"))
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
