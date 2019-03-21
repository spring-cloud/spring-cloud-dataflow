/*
 * Copyright 2016-2017 the original author or authors.
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

import org.junit.Test;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.http.MediaType;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Creates asciidoc snippets for endpoints exposed by {@literal AppRegistryController}.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 */
public class AppRegistryDocumentation extends BaseDocumentation {

	@Test
	public void getApplicationsFiltered() throws Exception {
		registerApp(ApplicationType.source, "http");
		registerApp(ApplicationType.source, "time");

		this.mockMvc.perform(get("/apps").param("type", "source").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(this.documentationHandler.document(requestParameters(
						parameterWithName("type").description("Restrict the returned apps to the type of the app. One of " + Arrays.asList(ApplicationType.values())))));

		unregisterApp(ApplicationType.source, "http");
		unregisterApp(ApplicationType.source, "time");
	}

	@Test
	public void getSingleApplication() throws Exception {
		registerApp(ApplicationType.source, "http");

		this.mockMvc.perform(get("/apps/{type}/{name}", ApplicationType.source, "http").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(
				this.documentationHandler.document(
					pathParameters(
						parameterWithName("type").description("The type of application to query. One of " + Arrays.asList(ApplicationType.values())),
						parameterWithName("name").description("The name of the application to query")
					)
				)
			);

		unregisterApp(ApplicationType.source, "http");
	}

	@Test
	public void registeringAnApplication() throws Exception {
		this.mockMvc.perform(
			post("/apps/{type}/{name}", ApplicationType.source, "http")
				.param("uri", "maven://org.springframework.cloud.stream.app:http-source-rabbit:1.1.0.RELEASE"))
			.andExpect(status().isCreated())
			.andDo(
				this.documentationHandler.document(
					pathParameters(
						parameterWithName("type").description("The type of application to register. One of " + Arrays.asList(ApplicationType.values())),
						parameterWithName("name").description("The name of the application to register")
					),
					requestParameters(
						parameterWithName("uri").description("URI where the application bits reside"),
						parameterWithName("metadata-uri").optional().description("URI where the application metadata jar can be found"),
						parameterWithName("force").optional().description("Must be true if a registration with the same name and type already exists, otherwise an error will occur")
					)
				)
			);

		unregisterApp(ApplicationType.source, "http");
	}

	@Test
	public void unregisteringAnApplication() throws Exception {
		registerApp(ApplicationType.source, "http");

		this.mockMvc.perform(
			delete("/apps/{type}/{name}", ApplicationType.source, "http"))
			.andExpect(status().isOk())
			.andDo(
				this.documentationHandler.document(
					pathParameters(
						parameterWithName("type").description("The type of application to unregister. One of " + Arrays.asList(ApplicationType.values())),
						parameterWithName("name").description("The name of the application to unregister")
					)
				)
			);
	}

	@Test
	public void bulkRegisteringApps() throws Exception {
		this.mockMvc.perform(
			post("/apps")
				.param("apps", "source.http=maven://org.springframework.cloud.stream.app:http-source-rabbit:1.1.0.RELEASE")
				.param("force", "false")
			)
			.andExpect(status().isCreated())
			.andDo(
				this.documentationHandler.document(
					requestParameters(
						parameterWithName("uri").optional().description("URI where a properties file containing registrations can be fetched. Exclusive with `apps`."),
						parameterWithName("apps").optional().description("Inline set of registrations. Exclusive with `uri`."),
						parameterWithName("force").optional().description("Must be true if a registration with the same name and type already exists, otherwise an error will occur")
					)
				)
			);

		unregisterApp(ApplicationType.source, "http");

	}

}
