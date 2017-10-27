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

import org.junit.Test;

import org.springframework.test.context.ActiveProfiles;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 */
@ActiveProfiles("repo-test")
public class PackageMetadataDocumentation extends BaseDocumentation {

	@Test
	public void getAllPackageMetadata() throws Exception {
		this.mockMvc.perform(
				get("/api/packageMetadata")
						.param("page", "0")
						.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						super.paginationRequestParameterProperties,
						super.paginationProperties.and(
								fieldWithPath("_embedded.packageMetadata")
										.description("Contains a collection of Package Metadata items"),
								fieldWithPath("_embedded.packageMetadata[].apiVersion")
										.description("The Package Index spec version this file is based on"),
								fieldWithPath("_embedded.packageMetadata[].origin")
										.description("Indicates the origin of the repository (free form text)"),
								fieldWithPath("_embedded.packageMetadata[].repositoryId")
										.description("The repository ID this Package Index file belongs to"),
								fieldWithPath("_embedded.packageMetadata[].kind")
										.description("What type of package system is being used"),
								fieldWithPath("_embedded.packageMetadata[].name")
										.description("The name of the package"),
								fieldWithPath("_embedded.packageMetadata[].version")
										.description("The version of the package"),
								fieldWithPath("_embedded.packageMetadata[].packageSourceUrl")
										.description("Location to source code for this package"),
								fieldWithPath("_embedded.packageMetadata[].packageHomeUrl")
										.description("The home page of the package"),
								fieldWithPath("_embedded.packageMetadata[].tags")
										.description("A comma separated list of tags to use for searching"),
								fieldWithPath("_embedded.packageMetadata[].maintainer")
										.description("Who is maintaining this package"),
								fieldWithPath("_embedded.packageMetadata[].description")
										.description("Brief description of the package"),
								fieldWithPath("_embedded.packageMetadata[].sha256").description(
										"Hash of package binary that will be downloaded using SHA256 hash algorithm"),
								fieldWithPath("_embedded.packageMetadata[].iconUrl")
										.description("Url location of a icon"),
								fieldWithPath("_embedded.packageMetadata[]._links.self.href").ignored(),
								fieldWithPath("_embedded.packageMetadata[]._links.packageMetadata.href").ignored(),
								fieldWithPath("_embedded.packageMetadata[]._links.packageMetadata.templated").ignored(),
								fieldWithPath("_embedded.packageMetadata[]._links.install.href").ignored())
								.and(super.defaultLinkProperties),
						super.linksForSkipper()));
	}

	@Test
	public void getPackageMetadataDetails() throws Exception {
		install("log", "1.0.0", "test2");

		this.mockMvc.perform(
				get("/api/packageMetadata/{packageMetadataId}", 1))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(
								parameterWithName("packageMetadataId").description("The id of the package to query")),
						responseFields(
								fieldWithPath("apiVersion")
										.description("The Package Index spec version this file is based on"),
								fieldWithPath("origin")
										.description("Indicates the origin of the repository (free form text)"),
								fieldWithPath("repositoryId")
										.description("The repository ID this Package Index file belongs to"),
								fieldWithPath("kind").description("What type of package system is being used"),
								fieldWithPath("name").description("The name of the package"),
								fieldWithPath("version").description("The version of the package"),
								fieldWithPath("packageSourceUrl")
										.description("Location to source code for this package"),
								fieldWithPath("packageHomeUrl").description("The home page of the package"),
								fieldWithPath("tags")
										.description("A comma separated list of tags to use for searching"),
								fieldWithPath("maintainer").description("Who is maintaining this package"),
								fieldWithPath("description").description("Brief description of the package"),
								fieldWithPath("sha256").description(
										"Hash of package binary that will be downloaded using SHA256 hash algorithm"),
								fieldWithPath("iconUrl").description("Url location of a icon"),
								fieldWithPath("_links.packageMetadata.href").ignored(),
								fieldWithPath("_links.packageMetadata.templated").ignored(),
								fieldWithPath("_links.install.href").ignored())
										.and(super.defaultLinkProperties)));
	}
}
