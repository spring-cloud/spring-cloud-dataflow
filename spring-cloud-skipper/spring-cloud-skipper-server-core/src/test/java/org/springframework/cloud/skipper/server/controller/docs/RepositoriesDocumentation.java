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

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ActiveProfiles;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
@ActiveProfiles({"repository"})
class RepositoriesDocumentation extends BaseDocumentation {

	@Test
	void getAllRepositories() throws Exception {
		this.mockMvc.perform(
				get("/api/repositories")
						.param("page", "0")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						super.paginationRequestParameterProperties,
						super.paginationProperties.and(
								fieldWithPath("_embedded.repositories")
										.description("Contains a collection of Repositories"),
								fieldWithPath("_embedded.repositories[].name").description("Name of the Repository"),
								fieldWithPath("_embedded.repositories[].url").description("Url of the Repository"),
								fieldWithPath("_embedded.repositories[].sourceUrl")
										.description("Source Url of the repository"),
								fieldWithPath("_embedded.repositories[].description")
										.description("Description of the Repository"),
								fieldWithPath("_embedded.repositories[].local").description("Is the repo local?"),
								fieldWithPath("_embedded.repositories[].repoOrder")
										.description("Order of the Repository"),
								fieldWithPath("_embedded.repositories[]._links.self.href").ignored(),
								fieldWithPath("_embedded.repositories[]._links.repository.href").ignored())
								.and(super.defaultLinkProperties)));
	}

	@Test
	void getSingleRepository() throws Exception {

		this.mockMvc.perform(
				get("/api/repositories/search/findByName?name={name}", "local"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						responseFields(
								fieldWithPath("name").description("Name of the Repository"),
								fieldWithPath("url").description("URL of the Repository"),
								fieldWithPath("description").description("Description of the Repository"),
								fieldWithPath("local").description("Is the repo local?"),
								fieldWithPath("repoOrder").description("Order of the Repository"),
								fieldWithPath("sourceUrl").description("Source URL of the repository"))
								.and(super.defaultLinkProperties)));
	}
}
