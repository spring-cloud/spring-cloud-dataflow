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

import java.nio.charset.Charset;

import org.junit.Test;

import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 */
@ActiveProfiles("repo-test")
@TestPropertySource(properties = { "spring.cloud.skipper.server.platform.local.accounts[test].key=value",
		"maven.remote-repositories.repo1.url=http://repo.spring.io/libs-snapshot" })
public class UpgradeDocumentation extends BaseDocumentation {

	@Test
	public void upgradeRelease() throws Exception {
		final String releaseName = "myLogRelease";
		final InstallRequest installRequest = new InstallRequest();
		final PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		packageIdentifier.setRepositoryName("notused");
		installRequest.setPackageIdentifier(packageIdentifier);
		final InstallProperties installProperties = new InstallProperties();
		installProperties.setReleaseName(releaseName);
		installProperties.setPlatformName("test");
		installRequest.setInstallProperties(installProperties);

		installPackage(installRequest);

		final String packageVersion = "1.1.0";
		final String packageName = "log";

		final UpgradeRequest upgradeRequest = new UpgradeRequest();
		final UpgradeProperties upgradeProperties = createUpdateProperties(releaseName);
		final PackageIdentifier packageIdentifierForUpgrade = new PackageIdentifier();
		packageIdentifierForUpgrade.setPackageName(packageName);
		packageIdentifierForUpgrade.setPackageVersion(packageVersion);
		upgradeRequest.setPackageIdentifier(packageIdentifierForUpgrade);
		upgradeRequest.setUpgradeProperties(upgradeProperties);

		final MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
				MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

		this.mockMvc.perform(post("/api/upgrade").accept(MediaType.APPLICATION_JSON).contentType(contentType)
			.content(convertObjectToJson(upgradeRequest))).andDo(print())
			.andExpect(status().isCreated())
			.andDo(this.documentationHandler.document(
				responseFields(
					fieldWithPath("name").description("TBD"),
					fieldWithPath("version").description("TBD"),
					fieldWithPath("info.status.statusCode").description("TBD"),
					fieldWithPath("info.status.platformStatus").description("TBD"),
					fieldWithPath("info.firstDeployed").description("TBD"),
					fieldWithPath("info.lastDeployed").description("TBD"),
					fieldWithPath("info.deleted").description("TBD"),
					fieldWithPath("info.description").description("TBD"),
					fieldWithPath("pkg.metadata.apiVersion").description("TBD"),
					fieldWithPath("pkg.metadata.origin").description("TBD"),
					fieldWithPath("pkg.metadata.kind").description("TBD"),
					fieldWithPath("pkg.metadata.name").description("TBD"),
					fieldWithPath("pkg.metadata.version").description("TBD"),
					fieldWithPath("pkg.metadata.packageSourceUrl").description("TBD"),
					fieldWithPath("pkg.metadata.packageHomeUrl").description("TBD"),
					fieldWithPath("pkg.metadata.tags").description("TBD"),
					fieldWithPath("pkg.metadata.maintainer").description("TBD"),
					fieldWithPath("pkg.metadata.description").description("TBD"),
					fieldWithPath("pkg.metadata.sha256").description("TBD"),
					fieldWithPath("pkg.metadata.iconUrl").description("TBD"),
					fieldWithPath("pkg.templates[].name").description("TBD"),
					fieldWithPath("pkg.templates[].data").description("TBD"),
					fieldWithPath("pkg.dependencies").description("TBD"),
					fieldWithPath("pkg.configValues.raw").description("TBD"),
					fieldWithPath("pkg.fileHolders").description("TBD"),
					fieldWithPath("configValues.raw").description("TBD"),
					fieldWithPath("manifest").description("TBD"),
					fieldWithPath("platformName").description("TBD")
				)
			));
	}
}
