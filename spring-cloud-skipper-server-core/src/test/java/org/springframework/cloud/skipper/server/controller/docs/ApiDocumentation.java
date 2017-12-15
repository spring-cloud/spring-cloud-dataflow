/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.skipper.server.controller.docs;

import javax.servlet.RequestDispatcher;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
public class ApiDocumentation extends BaseDocumentation {

	@Test
	public void headers() throws Exception {
		this.mockMvc.perform(get("/api")).andExpect(status().isOk())
				.andDo(this.documentationHandler.document(responseHeaders(headerWithName("Content-Type")
						.description("The Content-Type of the payload, e.g. " + "`application/hal+json`"))));
	}

	@Test
	public void errors() throws Exception {
		this.mockMvc
				.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
						.requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/path/not/there").requestAttr(
								RequestDispatcher.ERROR_MESSAGE,
								"The path 'http://localhost:8080/path/not/there' does not exist"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("error", is("Bad Request")))
				.andExpect(jsonPath("timestamp", is(notNullValue()))).andExpect(jsonPath("status", is(400)))
				.andExpect(jsonPath("path", is(notNullValue())))
				.andDo(this.documentationHandler.document(responseFields(
						fieldWithPath("error").description(
								"The HTTP error that occurred, e.g. `Bad Request`"),
						fieldWithPath("message").description("A description of the cause of the error"),
						fieldWithPath("path").description("The path to which the request was made"),
						fieldWithPath("status").description("The HTTP status code, e.g. `400`"),
						fieldWithPath("timestamp")
								.description("The time, in milliseconds, at which the error occurred"))));
	}

	@Test
	public void index() throws Exception {
		this.mockMvc.perform(get("/api")).andExpect(status().isOk()).andDo(this.documentationHandler.document(links(
				linkWithRel("about").description("Provides meta information of the server"),
				linkWithRel("upload").description("Uploads a package"),
				linkWithRel("install").description("Installs a package"),
				linkWithRel("install/id").description("Installs a package by also providing the package id"),
				linkWithRel("appDeployerDatas").description("Exposes App Deployer Data"),
				linkWithRel("repositories").description("Exposes package repositories"),
				linkWithRel("deployers").description("Exposes deployer"),
				linkWithRel("releases").description("Exposes release information"),
				linkWithRel("packageMetadata").description("Provides details for Package Metadata"),
				linkWithRel("jpaRepositoryStates").description(""),
				linkWithRel("jpaRepositoryGuards").description(""),
				linkWithRel("jpaRepositoryTransitions").description(""),
				linkWithRel("jpaRepositoryStateMachines").description(""),
				linkWithRel("jpaRepositoryActions").description(""),
				linkWithRel("profile").description(
						"Entrypoint to provide ALPS metadata defining simple descriptions of application-level semantics"),
				linkWithRel("status/name")
						.description("Get the status for the last known release version of the release "
								+ "by the given release name"),
				linkWithRel("status/name/version")
						.description("Get the status for the release by the given release name "
								+ "and version"),
				linkWithRel("manifest").description("Get a release's manifest"),
				linkWithRel("manifest/name/version")
						.description("Get a release's manifest by providing name and version"),
				linkWithRel("upgrade").description("Upgrade a release"),
				linkWithRel("rollback").description("Rollback the release to a previous or a specific release"),
				linkWithRel("delete").description("Delete the release"),
				linkWithRel("history").description("List the history of versions for a given release"),
				linkWithRel("list")
						.description("List the latest version of releases with status of deployed or failed"),
				linkWithRel("list/name")
						.description("List the latest version of releases by release name with status of "
								+ "deployed or failed"))));
	}
}
