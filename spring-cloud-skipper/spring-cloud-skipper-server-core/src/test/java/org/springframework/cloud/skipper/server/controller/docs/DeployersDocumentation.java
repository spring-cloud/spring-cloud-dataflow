/*
 * Copyright 2017-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ActiveProfiles;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
@ActiveProfiles({"repository", "local"})
class DeployersDocumentation extends BaseDocumentation {

	@Test
	void getAllDeployers() throws Exception {
		this.mockMvc.perform(
				get("/api/deployers")
						.param("page", "0")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						super.paginationRequestParameterProperties,
						super.paginationProperties.and(
								fieldWithPath("_embedded.deployers").description("Array containing Deployer objects"),
								fieldWithPath("_embedded.deployers[].name").description("Name of the deployer"),
								fieldWithPath("_embedded.deployers[].type")
										.description("Type of the deployer (e.g. 'local')"),
								fieldWithPath("_embedded.deployers[].description")
										.description("Description providing some deployer properties"),
								fieldWithPath("_embedded.deployers[]._links.self.href").ignored(),
								fieldWithPath("_embedded.deployers[].options")
										.description("Array containing Deployer deployment properties"),
								fieldWithPath("_embedded.deployers[].options[].id")
										.description("Deployment property id"),
								fieldWithPath("_embedded.deployers[].options[].name")
										.description("Deployment property name"),
								fieldWithPath("_embedded.deployers[].options[].type")
										.description("Deployment property type").optional(),
								fieldWithPath("_embedded.deployers[].options[].description")
										.description("Deployment property description").optional(),
								fieldWithPath("_embedded.deployers[].options[].shortDescription")
										.description("Deployment property short description").optional(),
								fieldWithPath("_embedded.deployers[].options[].defaultValue")
										.description("Deployment property default value").optional(),
								fieldWithPath("_embedded.deployers[].options[].hints")
										.description("Object containing deployment property hints"),
								subsectionWithPath("_embedded.deployers[].options[].hints.keyHints")
										.description("Deployment property key hints"),
								subsectionWithPath("_embedded.deployers[].options[].hints.keyProviders")
										.description("Deployment property key hint providers"),
								subsectionWithPath("_embedded.deployers[].options[].hints.valueHints")
										.description("Deployment property value hints"),
								subsectionWithPath("_embedded.deployers[].options[].hints.valueProviders")
										.description("Deployment property value hint providers"),
								fieldWithPath("_embedded.deployers[].options[].deprecation").description(""),
								fieldWithPath("_embedded.deployers[].options[].deprecated").description(""),
								fieldWithPath("_embedded.deployers[]._links.deployer.href").ignored())
								.and(super.defaultLinkProperties),
						linksForSkipper()));
	}
}
