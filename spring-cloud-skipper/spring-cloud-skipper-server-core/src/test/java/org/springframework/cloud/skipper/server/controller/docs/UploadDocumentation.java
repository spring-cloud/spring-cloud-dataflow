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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.io.DefaultPackageReader;
import org.springframework.cloud.skipper.io.PackageReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
class UploadDocumentation extends BaseDocumentation {

	@Test
	void uploadRelease() throws Exception {

		final UploadRequest uploadProperties = new UploadRequest();
		uploadProperties.setRepoName("local");
		uploadProperties.setName("log");
		uploadProperties.setVersion("1.0.0");
		uploadProperties.setExtension("zip");
		final Resource resource = new ClassPathResource("/repositories/sources/test/log/log-1.0.0");
		assertThat(resource.exists()).isTrue();
		PackageReader packageReader = new DefaultPackageReader();
		Package pkg = packageReader.read(resource.getFile());
		final byte[] originalPackageBytes = StreamUtils.copyToByteArray(resource.getInputStream());
		assertThat(originalPackageBytes).isNotEmpty();
		Assert.isTrue(originalPackageBytes.length != 0,
				"PackageServiceTests.Assert.isTrue: Package file as bytes must not be empty");
		uploadProperties.setPackageFileAsBytes(originalPackageBytes);

		final MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
				MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

		when(this.packageService.upload(any(UploadRequest.class))).thenReturn(pkg.getMetadata());

		mockMvc.perform(post("/api/package/upload").accept(MediaType.APPLICATION_JSON).contentType(contentType)
				.content(convertObjectToJson(uploadProperties))).andExpect(status().isCreated())
				.andDo(
						this.documentationHandler.document(
								responseFields(
										subsectionWithPath("links").ignored(),
										fieldWithPath("apiVersion")
												.description("The Package Index spec version this file is based on"),
										fieldWithPath("origin")
												.description("Indicates the origin of the repository (free form text)"),
										fieldWithPath("repositoryId")
												.description("The repository ID this Package belongs to."),
										fieldWithPath("repositoryName")
												.description("The repository nane this Package belongs to."),
										fieldWithPath("kind").description("What type of package system is being used"),
										fieldWithPath("name").description("The name of the package"),
										fieldWithPath("displayName").description("The display name of the package"),
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
										fieldWithPath("iconUrl").description("Url location of a icon"))));
	}
}
