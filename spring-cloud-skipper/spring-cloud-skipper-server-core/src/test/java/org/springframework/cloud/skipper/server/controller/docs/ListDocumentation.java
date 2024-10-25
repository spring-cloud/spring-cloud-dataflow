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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.util.StringUtils;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
class ListDocumentation extends BaseDocumentation {

	@Test
	void listRelease() throws Exception {
		List<Release> releaseList = new ArrayList<>();
		releaseList.add(createTestRelease());
		when(this.releaseService.list()).thenReturn(releaseList);
		this.mockMvc.perform(
				get("/api/release/list")).andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						responseFields(
								subsectionWithPath("_embedded.releases[]._links").ignored(),
								fieldWithPath("_embedded.releases[].name").description("Name of the release"),
								fieldWithPath("_embedded.releases[].version").description("Version of the release"),
								fieldWithPath("_embedded.releases[].info.status.statusCode").description(
										String.format("StatusCode of the release's status (%s)",
												StringUtils.arrayToCommaDelimitedString(StatusCode.values()))),
								fieldWithPath("_embedded.releases[].info.status.platformStatus")
										.description("Status from the underlying platform"),
								fieldWithPath("_embedded.releases[].info.firstDeployed").description("Date/Time of first deployment"),
								fieldWithPath("_embedded.releases[].info.lastDeployed").description("Date/Time of last deployment"),
								fieldWithPath("_embedded.releases[].info.deleted")
										.description("Date/Time of when the release was deleted"),
								fieldWithPath("_embedded.releases[].info.description")
										.description("Human-friendly 'log entry' about this release"),
								fieldWithPath("_embedded.releases[].pkg.metadata.apiVersion")
										.description("The Package Index spec version this file is based on"),
								fieldWithPath("_embedded.releases[].pkg.metadata.origin")
										.description("Indicates the origin of the repository (free form text)"),
								fieldWithPath("_embedded.releases[].pkg.metadata.repositoryId")
										.description("The repository ID this Package belongs to"),
								fieldWithPath("_embedded.releases[].pkg.metadata.repositoryName")
										.description("The repository name this Package belongs to."),
								fieldWithPath("_embedded.releases[].pkg.metadata.kind")
										.description("What type of package system is being used"),
								fieldWithPath("_embedded.releases[].pkg.metadata.name").description("The name of the package"),
								fieldWithPath("_embedded.releases[].pkg.metadata.displayName").description("Display name of the release"),
								fieldWithPath("_embedded.releases[].pkg.metadata.version").description("The version of the package"),
								fieldWithPath("_embedded.releases[].pkg.metadata.packageSourceUrl")
										.description("Location to source code for this package"),
								fieldWithPath("_embedded.releases[].pkg.metadata.packageHomeUrl")
										.description("The home page of the package"),
								fieldWithPath("_embedded.releases[].pkg.metadata.tags")
										.description("A comma separated list of tags to use for searching"),
								fieldWithPath("_embedded.releases[].pkg.metadata.maintainer")
										.description("Who is maintaining this package"),
								fieldWithPath("_embedded.releases[].pkg.metadata.description")
										.description("Brief description of the package"),
								fieldWithPath("_embedded.releases[].pkg.metadata.sha256").description(
										"Hash of package binary that will be downloaded using SHA256 hash algorithm"),
								fieldWithPath("_embedded.releases[].pkg.metadata.iconUrl").description("Url location of a icon"),
								fieldWithPath("_embedded.releases[].pkg.templates[].name")
										.description("Name is the path-like name of the template"),
								fieldWithPath("_embedded.releases[].pkg.templates[].data")
										.description("Data is the template as string data"),
								fieldWithPath("_embedded.releases[].pkg.dependencies")
										.description("The packages that this package depends upon"),
								fieldWithPath("_embedded.releases[].pkg.configValues.raw")
										.description("The raw YAML string of configuration values"),
								fieldWithPath("_embedded.releases[].pkg.fileHolders")
										.description("Miscellaneous files in a package, e.g. README, LICENSE, etc."),
								fieldWithPath("_embedded.releases[].configValues.raw")
										.description("The raw YAML string of configuration values"),
								fieldWithPath("_embedded.releases[].manifest.data").description("The manifest of the release"),
								fieldWithPath("_embedded.releases[].platformName").description("Platform name of the release"))

				));
	}

	@Test
	void listReleasesByReleaseName() throws Exception {
		Release release = createTestRelease();
		List<Release> releaseList = new ArrayList<>();
		releaseList.add(release);
		when(this.releaseService.list(release.getName())).thenReturn(releaseList);
		this.mockMvc.perform(
				get("/api/release/list/{releaseName}", release.getName())).andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("releaseName").description("Name of the releases to list")),
						responseFields(
								subsectionWithPath("_embedded.releases[]._links").ignored(),
								fieldWithPath("_embedded.releases[].name").description("Name of the release"),
								fieldWithPath("_embedded.releases[].version").description("Version of the release"),
								fieldWithPath("_embedded.releases[].info.status.statusCode").description(
										String.format("StatusCode of the release's status (%s)",
												StringUtils.arrayToCommaDelimitedString(StatusCode.values()))),
								fieldWithPath("_embedded.releases[].info.status.platformStatus")
										.description("Status from the underlying platform"),
								fieldWithPath("_embedded.releases[].info.firstDeployed").description("Date/Time of first deployment"),
								fieldWithPath("_embedded.releases[].info.lastDeployed").description("Date/Time of last deployment"),
								fieldWithPath("_embedded.releases[].info.deleted")
										.description("Date/Time of when the release was deleted"),
								fieldWithPath("_embedded.releases[].info.description")
										.description("Human-friendly 'log entry' about this release"),
								fieldWithPath("_embedded.releases[].pkg.metadata.apiVersion")
										.description("The Package Index spec version this file is based on"),
								fieldWithPath("_embedded.releases[].pkg.metadata.origin")
										.description("Indicates the origin of the repository (free form text)"),
								fieldWithPath("_embedded.releases[].pkg.metadata.repositoryId")
										.description("The repository ID this Package belongs to"),
								fieldWithPath("_embedded.releases[].pkg.metadata.repositoryName")
										.description("The repository name this Package belongs to."),
								fieldWithPath("_embedded.releases[].pkg.metadata.kind")
										.description("What type of package system is being used"),
								fieldWithPath("_embedded.releases[].pkg.metadata.name").description("The name of the package"),
								fieldWithPath("_embedded.releases[].pkg.metadata.displayName").description("Display name of the release"),
								fieldWithPath("_embedded.releases[].pkg.metadata.version").description("The version of the package"),
								fieldWithPath("_embedded.releases[].pkg.metadata.packageSourceUrl")
										.description("Location to source code for this package"),
								fieldWithPath("_embedded.releases[].pkg.metadata.packageHomeUrl")
										.description("The home page of the package"),
								fieldWithPath("_embedded.releases[].pkg.metadata.tags")
										.description("A comma separated list of tags to use for searching"),
								fieldWithPath("_embedded.releases[].pkg.metadata.maintainer")
										.description("Who is maintaining this package"),
								fieldWithPath("_embedded.releases[].pkg.metadata.description")
										.description("Brief description of the package"),
								fieldWithPath("_embedded.releases[].pkg.metadata.sha256").description(
										"Hash of package binary that will be downloaded using SHA256 hash algorithm"),
								fieldWithPath("_embedded.releases[].pkg.metadata.iconUrl").description("Url location of a icon"),
								fieldWithPath("_embedded.releases[].pkg.templates[].name")
										.description("Name is the path-like name of the template"),
								fieldWithPath("_embedded.releases[].pkg.templates[].data")
										.description("Data is the template as string data"),
								fieldWithPath("_embedded.releases[].pkg.dependencies")
										.description("The packages that this package depends upon"),
								fieldWithPath("_embedded.releases[].pkg.configValues.raw")
										.description("The raw YAML string of configuration values"),
								fieldWithPath("_embedded.releases[].pkg.fileHolders")
										.description("Miscellaneous files in a package, e.g. README, LICENSE, etc."),
								fieldWithPath("_embedded.releases[].configValues.raw")
										.description("The raw YAML string of configuration values"),
								fieldWithPath("_embedded.releases[].manifest.data").description("The manifest of the release"),
								fieldWithPath("_embedded.releases[].platformName").description("Platform name of the release"))));
	}
}
