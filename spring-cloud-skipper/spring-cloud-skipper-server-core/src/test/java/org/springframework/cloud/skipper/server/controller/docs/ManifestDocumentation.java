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

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
class ManifestDocumentation extends BaseDocumentation {

	@Test
	void getManifestOfRelease() throws Exception {
		Release release = createTestRelease();
		when(this.releaseService.manifest(release.getName())).thenReturn(release.getManifest());
		final MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
				MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

		this.mockMvc.perform(
				get("/api/release/manifest/{releaseName}", release.getName()).accept(MediaType.APPLICATION_JSON)
						.contentType(contentType))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("releaseName").description("The name of the release")),
						responseBody()));
	}

	@Test
	void getManifestOfReleaseForVersion() throws Exception {
		Release release = createTestRelease();

		when(this.releaseService.manifest(release.getName(), 1)).thenReturn(release.getManifest());

		this.mockMvc.perform(
				get("/api/release/manifest/{releaseName}/{releaseVersion}",
						release.getName(), release.getVersion()))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(
							parameterWithName("releaseName").description("The name of the release"),
							parameterWithName("releaseVersion").description("The version of the release")
						),
						responseBody()));
	}
}
