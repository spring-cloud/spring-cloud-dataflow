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
package org.springframework.cloud.skipper.controller;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.AbstractMockMvcTests;
import org.springframework.cloud.skipper.config.SkipperServerProperties;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.skipperpackage.DeployProperties;
import org.springframework.cloud.skipper.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.repository.ReleaseRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@ActiveProfiles("repo-test")
@TestPropertySource(properties = { "spring.cloud.skipper.server.synchonizeIndexOnContextRefresh=true",
		"spring.cloud.skipper.server.platform.local.accounts[test].key=value",
		"maven.remote-repositories.repo1.url=http://repo.spring.io/libs-snapshot" })
public class PackageControllerTests extends AbstractMockMvcTests {

	@Autowired
	private PackageMetadataRepository packageMetadataRepository;

	@Autowired
	private ReleaseRepository releaseRepository;

	@Autowired
	private SkipperServerProperties skipperServerProperties;

	@Before
	public void cleanupPackageDir() {
		this.releaseRepository.deleteAll();
		File packageDirectory = new File(skipperServerProperties.getPackageDir());
		FileSystemUtils.deleteRecursively(new File(skipperServerProperties.getPackageDir()));
		assertThat(packageDirectory).doesNotExist();
	}

	@Test
	public void packageDeployAndUpdate() throws Exception {
		String packageName = "log";
		String releaseName = "log-sink-app";
		// Deploy
		String packageVersion = "1.0.0";
		DeployProperties deployProperties = new DeployProperties();
		deployProperties.setPlatformName("test");
		deployProperties.setReleaseName(releaseName);
		PackageMetadata packageMetadata = this.packageMetadataRepository.findByNameAndVersion(packageName,
				packageVersion);
		mockMvc.perform(post("/package/" + packageMetadata.getId() + "/deploy")
				.content(convertObjectToJson(deployProperties))).andDo(print())
				.andExpect(status().isCreated()).andReturn();
		Release deployedRelease = this.releaseRepository.findByNameAndVersion(releaseName, 1);
		assertThat(deployedRelease.getName()).isEqualTo(releaseName);
		assertThat(deployedRelease.getPlatformName()).isEqualTo("test");
		assertThat(deployedRelease.getVersion()).isEqualTo(1);
		assertThat(deployedRelease.getPkg().getMetadata().equals(packageMetadata)).isTrue();
		assertThat(deployedRelease.getInfo().getStatus().getStatusCode()).isEqualTo(StatusCode.DEPLOYED);
		// Update
		String updatePackageVersion = "1.0.1";
		PackageMetadata updatePackageMetadata = packageMetadataRepository.findByNameAndVersion(packageName,
				updatePackageVersion);
		DeployProperties newDeployProperties = new DeployProperties();
		newDeployProperties.setPlatformName("test");
		newDeployProperties.setReleaseName(releaseName);
		mockMvc.perform(post("/package/" + updatePackageMetadata.getId() + "/update")
				.content(convertObjectToJson(newDeployProperties))).andDo(print())
				.andExpect(status().isCreated()).andReturn();
		Release updatedRelease = this.releaseRepository.findByNameAndVersion(releaseName, 2);
		assertThat(updatedRelease.getName()).isEqualTo(releaseName);
		assertThat(updatedRelease.getPlatformName()).isEqualTo("test");
		assertThat(updatedRelease.getVersion()).isEqualTo(2);
		assertThat(updatedRelease.getPkg().getMetadata().equals(updatePackageMetadata)).isTrue();
		assertThat(updatedRelease.getInfo().getStatus().getStatusCode()).isEqualTo(StatusCode.DEPLOYED);
	}

}
