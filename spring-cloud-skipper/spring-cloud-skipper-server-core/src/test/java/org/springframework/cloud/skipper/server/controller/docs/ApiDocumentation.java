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

package org.springframework.cloud.skipper.server.controller.docs;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@ActiveProfiles("repository")
public class ApiDocumentation extends BaseDocumentation {

	@Test
	public void headers() throws Exception {
		this.mockMvc.perform(get("/api")).andExpect(status().isOk())
				.andDo(this.documentationHandler.document(responseHeaders(headerWithName("Content-Type")
						.description("The `Content-Type` of the payload (for example `application/hal+json`)."))));
	}

	@Test
	public void errors() throws Exception {
		this.mockMvc
				.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
						.requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalArgumentException())
						.requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/path/not/there")
						.requestAttr(RequestDispatcher.ERROR_MESSAGE,
								"The path, 'http://localhost:8080/path/not/there', does not exist."))
				.andDo(print())
				.andExpect(status().isBadRequest()).andExpect(jsonPath("error", is("Bad Request")))
				.andExpect(jsonPath("timestamp", is(notNullValue()))).andExpect(jsonPath("status", is(400)))
				.andExpect(jsonPath("path", is(notNullValue())))
				.andDo(this.documentationHandler.document(responseFields(
						fieldWithPath("error").description(
								"The HTTP error that occurred (for example, `Bad Request`)."),
						fieldWithPath("message").description("A description of the cause of the error."),
						fieldWithPath("exception").description("An exception class."),
						fieldWithPath("path").description("The path to which the request was made."),
						fieldWithPath("status").description("The HTTP status code (for example `400`)."),
						fieldWithPath("timestamp")
								.description("The time, in milliseconds, at which the error occurred."))));
	}

	@Test
	public void index() throws Exception {
		this.mockMvc.perform(get("/api")).andExpect(status().isOk()).andDo(this.documentationHandler.document(links(
				//TODO investigate
				linkWithRel("jpaRepositoryStates").ignored(),
				linkWithRel("jpaRepositoryGuards").ignored(),
				linkWithRel("jpaRepositoryTransitions").ignored(),
				linkWithRel("jpaRepositoryStateMachines").ignored(),
				linkWithRel("jpaRepositoryActions").ignored(),

				linkWithRel("repositories").description("Exposes the 'package repository' repository."),
				linkWithRel("deployers").description("Exposes the deployer repository."),
				linkWithRel("packageMetadata").description("Exposes the package metadata repository."),


				linkWithRel("releases").description("Exposes the release repository."),
				linkWithRel("profile").description(
						"Entrypoint to provide ALPS metadata that defines simple descriptions of application-level semantics."),
				linkWithRel("about").description("Provides meta information about the server."),
				linkWithRel("release").description("Exposes the release resource."),
				linkWithRel("package").description("Exposes the package resource."))));
	}
}
