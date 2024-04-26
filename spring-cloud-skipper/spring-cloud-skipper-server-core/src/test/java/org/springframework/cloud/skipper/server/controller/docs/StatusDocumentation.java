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

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.util.StringUtils;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
public class StatusDocumentation extends BaseDocumentation {

	@Test
	public void getStatusOfRelease() throws Exception {
		Release release = createTestRelease();
		when(this.releaseService.status(release.getName())).thenReturn(release.getInfo());
		this.mockMvc.perform(
				get("/api/release/status/{releaseName}", release.getName())).andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						responseFields(
								subsectionWithPath("_links").ignored(),
								fieldWithPath("status.statusCode").description(
										String.format("StatusCode of the release's status (%s)",
												StringUtils.arrayToCommaDelimitedString(StatusCode.values()))),
								fieldWithPath("status.platformStatus")
										.description("Status from the underlying platform"),
								fieldWithPath("firstDeployed").description("Date/Time of first deployment"),
								fieldWithPath("lastDeployed").description("Date/Time of last deployment"),
								fieldWithPath("deleted").description("Date/Time of when the release was deleted"),
								fieldWithPath("description")
										.description("Human-friendly 'log entry' about this release"))));
	}

	@Test
	public void getStatusOfReleaseForVersion() throws Exception {
		Release release = createTestRelease();
		when(this.releaseService.status(release.getName(), release.getVersion())).thenReturn(release.getInfo());
		this.mockMvc.perform(
				get("/api/release/status/{releaseName}/{releaseVersion}",
						release.getName(), release.getVersion()))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						responseFields(
								subsectionWithPath("_links").ignored(),
								fieldWithPath("status.statusCode").description(
										String.format("StatusCode of the release's status (%s)",
												StringUtils.arrayToCommaDelimitedString(StatusCode.values()))),
								fieldWithPath("status.platformStatus")
										.description("Status from the underlying platform"),
								fieldWithPath("firstDeployed").description("Date/Time of first deployment"),
								fieldWithPath("lastDeployed").description("Date/Time of last deployment"),
								fieldWithPath("deleted").description("Date/Time of when the release was deleted"),
								fieldWithPath("description")
										.description("Human-friendly 'log entry' about this release"))));
	}
}
