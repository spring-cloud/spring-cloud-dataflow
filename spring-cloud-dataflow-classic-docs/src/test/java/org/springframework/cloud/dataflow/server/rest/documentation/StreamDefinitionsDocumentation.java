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

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.test.annotation.DirtiesContext;

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

/**
 * Documentation for the /streams/definitions endpoint.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
@TestMethodOrder(MethodName.class)
@DirtiesContext
class StreamDefinitionsDocumentation extends BaseDocumentation {
	@BeforeEach
	void setup() throws Exception {
		this.mockMvc.perform(
			post("/apps/{type}/time", "source")
					.queryParam("uri", "maven://org.springframework.cloud.stream.app:time-source-rabbit:5.0.0"))
			.andExpect(status().isCreated());
		this.mockMvc.perform(
			post("/apps/{type}/log", "sink")
					.queryParam("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:5.0.0"))
			.andExpect(status().isCreated());
	}

	@Test
	void createDefinition() throws Exception {
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
					parameterWithName("deploy").optional()
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
	void listAllStreamDefinitions() throws Exception {
		this.documentation.dontDocument(
				() -> this.mockMvc
					.perform(post("/streams/definitions").queryParam("name", "timelog")
						.queryParam("definition", "time --format='YYYY MM DD' | log")
						.queryParam("description", "Demo stream for testing")
						.queryParam("deploy", "false"))
					.andExpect(status().isCreated()));
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
					parameterWithName("page").optional().description("The zero-based page number"),
					parameterWithName("search").optional().description("The search string performed on the name"),
					parameterWithName("sort").optional().description("The sort on the list"),
					parameterWithName("size").optional().description("The requested page size")),
				responseFields(
					subsectionWithPath("_embedded.streamDefinitionResourceList")
						.description("Contains a collection of Stream Definitions"),
					subsectionWithPath("_links.self").description("Link to the stream definitions resource"),
					subsectionWithPath("page").description("Pagination properties"))));
	}

	@Test
	void getStreamDefinition() throws Exception {
		this.documentation.dontDocument(
				() -> this.mockMvc
					.perform(post("/streams/definitions").queryParam("name", "timelog")
						.queryParam("definition", "time --format='YYYY MM DD' | log")
						.queryParam("description", "Demo stream for testing")
						.queryParam("deploy", "false"))
					.andExpect(status().isCreated()));
		this.mockMvc.perform(
				get("/streams/definitions/{name}", "timelog"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(
								parameterWithName("name").description("The name of the stream definition to query")
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
	void getStreamApplications() throws Exception {
		createStream("mysamplestream", "time | log", false);
		this.mockMvc.perform(
				get("/streams/definitions/{name}/applications", "mysamplestream"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(
								parameterWithName("name").description("The name of the stream definition to query")
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
	void listRelatedStreamDefinitions() throws Exception {
		this.documentation.dontDocument(
				() -> this.mockMvc
					.perform(post("/streams/definitions").queryParam("name", "timelog")
						.queryParam("definition", "time --format='YYYY MM DD' | log")
						.queryParam("description", "Demo stream for testing")
						.queryParam("deploy", "false"))
					.andExpect(status().isCreated()));
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
                    parameterWithName("nested").optional()
                        .description("Should we recursively findByTaskNameContains for related stream definitions"),
                    parameterWithName("page").optional().description("The zero-based page number"),
                    parameterWithName("search").optional().description("The search string performed on the name"),
                    parameterWithName("sort").optional().description("The sort on the list"),
                    parameterWithName("size").optional().description("The requested page size")),
                pathParameters(parameterWithName("name")
                    .description("The name of an existing stream definition")),
                responseFields(
                    subsectionWithPath("_embedded.streamDefinitionResourceList")
                        .description("Contains a collection of Stream Definitions"),
                    subsectionWithPath("_links.self").description("Link to the stream definitions resource"),
                    subsectionWithPath("page").description("Pagination properties"))
            ));
	}

	@Test
	void streamDefinitionDelete1() throws Exception {
		this.documentation.dontDocument(
				() -> this.mockMvc
					.perform(post("/streams/definitions").queryParam("name", "timelog")
						.queryParam("definition", "time --format='YYYY MM DD' | log")
						.queryParam("description", "Demo stream for testing")
						.queryParam("deploy", "false"))
					.andExpect(status().isCreated()));
		this.mockMvc.perform(
			delete("/streams/definitions/{name}", "timelog"))
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				pathParameters(parameterWithName("name")
					.description("The name of an existing stream definition"))
			));
	}

	@Test
	void streamDefinitionDeleteAll() throws Exception {
		this.documentation.dontDocument(
				() -> this.mockMvc
					.perform(post("/streams/definitions").queryParam("name", "timelog")
						.queryParam("definition", "time --format='YYYY MM DD' | log")
						.queryParam("description", "Demo stream for testing")
						.queryParam("deploy", "false"))
					.andExpect(status().isCreated()));
		this.mockMvc.perform(
			delete("/streams/definitions"))
			.andExpect(status().isOk());
	}

}
