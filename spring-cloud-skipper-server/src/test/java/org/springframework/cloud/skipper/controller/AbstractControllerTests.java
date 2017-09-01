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

import org.junit.After;
import org.junit.Before;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.AbstractMockMvcTests;
import org.springframework.cloud.skipper.config.SkipperServerProperties;
import org.springframework.cloud.skipper.domain.DeployRequest;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.skipperpackage.DeployProperties;
import org.springframework.cloud.skipper.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.repository.ReleaseRepository;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Mark Pollack
 */
public class AbstractControllerTests extends AbstractMockMvcTests {

	@Autowired
	protected PackageMetadataRepository packageMetadataRepository;

	@Autowired
	protected ReleaseRepository releaseRepository;

	@Autowired
	protected SkipperServerProperties skipperServerProperties;

	@Before
	public void cleanupPackageDir() {
		this.releaseRepository.deleteAll();
		File packageDirectory = new File(skipperServerProperties.getPackageDir());
		FileSystemUtils.deleteRecursively(new File(skipperServerProperties.getPackageDir()));
		assertThat(packageDirectory).doesNotExist();
	}

	@After
	public void cleanupReleases() throws Exception {
		// Add a sleep for now to give the local deployer a chance to deploy the app. This should
		// go away
		// once we introduce spring state machine.
		Thread.sleep(5000);
		for (Release release : releaseRepository.findAll()) {
			if (release.getInfo().getStatus().getStatusCode() != StatusCode.DELETED) {
				mockMvc.perform(post("/release/undeploy/" + release.getName() + "/" + release.getVersion()))
						.andDo(print())
						.andExpect(status().isCreated()).andReturn();
			}
		}
	}

	protected Release deploy(String packageName, String packageVersion, String releaseName) throws Exception {
		// Deploy
		DeployProperties deployProperties = createDeployProperties(releaseName);
		PackageMetadata packageMetadata = this.packageMetadataRepository.findByNameAndVersion(packageName,
				packageVersion);
		assertThat(packageMetadata).isNotNull();
		mockMvc.perform(post("/package/" + packageMetadata.getId() + "/deploy")
				.content(convertObjectToJson(deployProperties))).andDo(print())
				.andExpect(status().isCreated()).andReturn();
		Release deployedRelease = this.releaseRepository.findByNameAndVersion(releaseName, 1);
		commonReleaseAssertions(releaseName, packageMetadata, deployedRelease);
		return deployedRelease;
	}

	protected Release deploy(DeployRequest deployRequest) throws Exception {
		mockMvc.perform(post("/package/deploy")
				.content(convertObjectToJson(deployRequest))).andDo(print())
				.andExpect(status().isCreated()).andReturn();

		String releaseName = deployRequest.getDeployProperties().getReleaseName();
		Release deployedRelease = this.releaseRepository.findByNameAndVersion(releaseName, 1);
		PackageMetadata packageMetadata = this.packageMetadataRepository.findByNameAndVersion(
				deployRequest.getPackageIdentifier().getPackageName(),
				deployRequest.getPackageIdentifier().getPackageVersion());
		commonReleaseAssertions(releaseName, packageMetadata, deployedRelease);
		return deployedRelease;
	}

	protected Release update(String packageName, String packageVersion, String releaseName) throws Exception {
		DeployProperties newDeployProperties = createDeployProperties(releaseName);
		PackageMetadata updatePackageMetadata = packageMetadataRepository.findByNameAndVersion(packageName,
				packageVersion);
		assertThat(updatePackageMetadata).isNotNull();
		mockMvc.perform(post("/package/" + updatePackageMetadata.getId() + "/update")
				.content(convertObjectToJson(newDeployProperties))).andDo(print())
				.andExpect(status().isCreated()).andReturn();
		Release updatedRelease = this.releaseRepository.findByNameAndVersion(releaseName, 2);
		commonReleaseAssertions(releaseName, updatePackageMetadata, updatedRelease);
		return updatedRelease;
	}

	protected DeployProperties createDeployProperties(String releaseName) {
		DeployProperties deployProperties = new DeployProperties();
		deployProperties.setPlatformName("test");
		deployProperties.setReleaseName(releaseName);
		return deployProperties;
	}

	protected void commonReleaseAssertions(String releaseName, PackageMetadata packageMetadata,
			Release deployedRelease) {
		assertThat(deployedRelease.getName()).isEqualTo(releaseName);
		assertThat(deployedRelease.getPlatformName()).isEqualTo("test");
		assertThat(deployedRelease.getPkg().getMetadata()).isEqualToIgnoringGivenFields(packageMetadata, "id");
		assertThat(deployedRelease.getPkg().getMetadata().equals(packageMetadata));
		assertThat(deployedRelease.getInfo().getStatus().getStatusCode()).isEqualTo(StatusCode.DEPLOYED);
	}

}
