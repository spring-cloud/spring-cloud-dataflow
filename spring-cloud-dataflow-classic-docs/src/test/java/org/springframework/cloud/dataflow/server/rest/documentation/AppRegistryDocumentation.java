/*
 * Copyright 2016-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Creates asciidoc snippets for endpoints exposed by {@literal AppRegistryController}.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
class AppRegistryDocumentation extends BaseDocumentation {

	@Test
	void appDefault() throws Exception {
		registerApp(ApplicationType.source, "http", "4.0.0");
		registerApp(ApplicationType.source, "http", "5.0.0");

		this.mockMvc.perform(RestDocumentationRequestBuilders
			.put("/apps/{type}/{name}/{version:.+}", ApplicationType.source, "http", "4.0.0")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isAccepted())
				.andDo(
						this.documentationHandler.document(
								pathParameters(
										parameterWithName("type").description("The type of application. One of " + Arrays.asList(ApplicationType.values())),
										parameterWithName("name").description("The name of the application"),
										parameterWithName("version").description("The version of the application")
								)
						)
				);
		unregisterApp(ApplicationType.source, "http", "4.0.0");
		unregisterApp(ApplicationType.source, "http", "5.0.0");
	}

	@Test
	void registeringAnApplicationVersion() throws Exception {
		this.mockMvc.perform(
				post("/apps/{type}/{name}/{version:.+}", ApplicationType.source, "http", "4.0.0").queryParam("uri",
						"maven://org.springframework.cloud.stream.app:http-source-rabbit:4.0.0")
			).andExpect(status().isCreated())
				.andDo(
						this.documentationHandler.document(
								pathParameters(
										parameterWithName("type").optional()
												.description("The type of application to register. One of " + Arrays.asList(ApplicationType.values())),
										parameterWithName("name").description("The name of the application to register"),
										parameterWithName("version").description("The version of the application to register")
								),
								queryParameters(
										parameterWithName("uri").description("URI where the application bits reside"),
										parameterWithName("metadata-uri").optional()
												.description("URI where the application metadata jar can be found"),
										parameterWithName("force").optional()
												.description("Must be true if a registration with the same name and type already exists, otherwise an error will occur")
								)
						)
				);

		unregisterApp(ApplicationType.source, "http", "4.0.0");
	}


	@Test
	void bulkRegisteringApps() throws Exception {
		this.mockMvc.perform(
						post("/apps")
							.queryParam("apps", "source.http=maven://org.springframework.cloud.stream.app:http-source-rabbit:4.0.0")
							.queryParam("force", "false")
				)
				.andExpect(status().isCreated())
				.andDo(
						this.documentationHandler.document(
								queryParameters(
										parameterWithName("uri").optional().description("URI where a properties file containing registrations can be fetched. Exclusive with `apps`."),
										parameterWithName("apps").optional().description("Inline set of registrations. Exclusive with `uri`."),
										parameterWithName("force").optional().description("Must be true if a registration with the same name and type already exists, otherwise an error will occur")
								)
						)
				);
		unregisterApp(ApplicationType.source, "http");
	}

	@Test
	void getApplicationsFiltered() throws Exception {
		registerApp(ApplicationType.source, "http", "5.0.0");
		registerApp(ApplicationType.source, "time", "5.0.0");
		this.mockMvc.perform(
						get("/apps")
								.param("search", "")
								.param("type", "source").accept(MediaType.APPLICATION_JSON)
								.param("defaultVersion", "true")
								.param("page", "0")
								.param("size", "10")
								.param("sort", "name,ASC")
				)
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						queryParameters(
								parameterWithName("search").optional()
										.description("The search string performed on the name"),
								parameterWithName("type")
										.description("Restrict the returned apps to the type of the app. One of " + Arrays.asList(ApplicationType.values())),
								parameterWithName("defaultVersion").optional().description("The boolean flag to set to retrieve only the apps of the default versions"),
								parameterWithName("page").optional().description("The zero-based page number"),
								parameterWithName("sort").optional().description("The sort on the list"),
								parameterWithName("size").optional().description("The requested page size")
						),
						responseFields(
								subsectionWithPath("_embedded.appRegistrationResourceList")
										.description("Contains a collection of application"),
								subsectionWithPath("_links.self").description("Link to the applications resource"),
								subsectionWithPath("page").description("Pagination properties")
						)
				));

		unregisterApp(ApplicationType.source, "http");
		unregisterApp(ApplicationType.source, "time");
	}

	@Test
	void getSingleApplication() throws Exception {
		registerApp(ApplicationType.source, "http", "5.0.0");
		this.mockMvc.perform(
						get("/apps/{type}/{name}", ApplicationType.source, "http").accept(MediaType.APPLICATION_JSON)
								.param("exhaustive", "false"))
				.andExpect(status().isOk())
				.andDo(
						this.documentationHandler.document(
								pathParameters(
										parameterWithName("type").description("The type of application to query. One of " + Arrays.asList(ApplicationType.values())),
										parameterWithName("name").description("The name of the application to query")
								),
								queryParameters(
										parameterWithName("exhaustive").optional()
												.description("Return all application properties, including common Spring Boot properties")
								),
								responseFields(
										fieldWithPath("name").description("The name of the application"),
										fieldWithPath("label").description("The label name of the application"),
										fieldWithPath("type").description("The type of the application. One of " + Arrays.asList(ApplicationType.values())),
										fieldWithPath("uri").description("The uri of the application"),
										fieldWithPath("metaDataUri").description("The uri of the application metadata").optional(),
										fieldWithPath("version").description("The version of the application"),
										fieldWithPath("versions").description("All the registered versions of the application"),
										fieldWithPath("defaultVersion").description("If true, the application is the default version"),
										subsectionWithPath("options").description("The options of the application (Array)"),
										fieldWithPath("shortDescription").description("The description of the application"),
										fieldWithPath("inboundPortNames").description("Inbound port names of the application"),
										fieldWithPath("outboundPortNames").description("Outbound port names of the application"),
										fieldWithPath("optionGroups").description("Option groups of the application")
								)
						)
				);
		unregisterApp(ApplicationType.source, "http");
	}

	@Test
	void registeringAnApplication() throws Exception {
		this.mockMvc.perform(
						post("/apps/{type}/{name}", ApplicationType.source, "http")
							.queryParam("uri", "maven://org.springframework.cloud.stream.app:http-source-rabbit:5.0.0")
				)
				.andExpect(status().isCreated())
				.andDo(
						this.documentationHandler.document(
								pathParameters(
										parameterWithName("type").description("The type of application to register. One of " + Arrays.asList(ApplicationType.values())),
										parameterWithName("name").description("The name of the application to register")
								),
								queryParameters(
										parameterWithName("uri").description("URI where the application bits reside"),
										parameterWithName("metadata-uri").optional().description("URI where the application metadata jar can be found"),
										parameterWithName("force").optional().description("Must be true if a registration with the same name and type already exists, otherwise an error will occur")
								)
						)
				);

		unregisterApp(ApplicationType.source, "http");
	}

	@Test
	void unregisteringAnApplication() throws Exception {
		registerApp(ApplicationType.source, "http", "5.0.0");

		this.mockMvc.perform(
				delete("/apps/{type}/{name}/{version}", ApplicationType.source, "http", "5.0.0"))
				.andExpect(status().isOk())
				.andDo(
						this.documentationHandler.document(
								pathParameters(
										parameterWithName("type").description("The type of application to unregister. One of " + Arrays.asList(ApplicationType.values())),
										parameterWithName("name").description("The name of the application to unregister"),
										parameterWithName("version").optional().description("The version of the application to unregister")
								)
						)
				);
	}

	@Test
	void unregisteringAllApplications() throws Exception {
		registerApp(ApplicationType.source, "http", "4.0.0");
		registerApp(ApplicationType.source, "http", "5.0.0");
		this.mockMvc.perform(
						delete("/apps"))
				.andExpect(status().isOk()
				);
	}

}
