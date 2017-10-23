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
package org.springframework.cloud.skipper.server.controller;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.AbstractMockMvcTests;
import org.springframework.cloud.skipper.server.config.SkipperServerProperties;
import org.springframework.cloud.skipper.server.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public abstract class AbstractControllerTests extends AbstractMockMvcTests {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	protected PackageMetadataRepository packageMetadataRepository;

	@Autowired
	protected ReleaseRepository releaseRepository;

	@Autowired
	protected SkipperServerProperties skipperServerProperties;

	@Before
	public void cleanupReleaseRepository() {
		this.releaseRepository.deleteAll();
	}

	@After
	public void cleanupReleases() throws Exception {
		// Add a sleep for now to give the local deployer a chance to install the app.
		// This
		// should go away once we introduce spring state machine.
		logger.info("Test Clean up - deleting all releases.");
		Thread.sleep(5000);
		for (Release release : releaseRepository.findAll()) {
			if (release.getInfo().getStatus().getStatusCode() != StatusCode.DELETED) {
				try {
					mockMvc.perform(post("/api/delete/" + release.getName()))
							.andDo(print())
							.andExpect(status().isCreated()).andReturn();
				}
				catch (Exception e) {
					logger.warn("Can not delete release {}-v{}, as it has not yet deployed.", release.getName(),
							release.getVersion());
				}
			}
		}
	}

	protected Release deploy(String packageName, String packageVersion, String releaseName) throws Exception {
		// Deploy
		InstallProperties installProperties = createDeployProperties(releaseName);
		PackageMetadata packageMetadata = this.packageMetadataRepository.findByNameAndVersionByMaxRepoOrder(packageName,
				packageVersion);
		assertThat(packageMetadata).isNotNull();
		mockMvc.perform(post("/api/install/" + packageMetadata.getId())
				.content(convertObjectToJson(installProperties))).andDo(print())
				.andExpect(status().isCreated()).andReturn();
		sleep();
		Release deployedRelease = this.releaseRepository.findByNameAndVersion(releaseName, 1);
		commonReleaseAssertions(releaseName, packageMetadata, deployedRelease);
		return deployedRelease;
	}

	protected void sleep() {
		try {
			Thread.sleep(20000);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected Release installPackage(InstallRequest installRequest) throws Exception {
		mockMvc.perform(post("/api/install")
				.content(convertObjectToJson(installRequest))).andDo(print())
				.andExpect(status().isCreated()).andReturn();
		sleep();
		String releaseName = installRequest.getInstallProperties().getReleaseName();
		Release deployedRelease = this.releaseRepository.findByNameAndVersion(releaseName, 1);
		PackageMetadata packageMetadata = this.packageMetadataRepository.findByNameAndVersionByMaxRepoOrder(
				installRequest.getPackageIdentifier().getPackageName(),
				installRequest.getPackageIdentifier().getPackageVersion());
		commonReleaseAssertions(releaseName, packageMetadata, deployedRelease);
		return deployedRelease;
	}

	protected Release upgrade(String packageName, String packageVersion, String releaseName) throws Exception {
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		UpgradeProperties upgradeProperties = createUpdateProperties(releaseName);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		upgradeRequest.setPackageIdentifier(packageIdentifier);
		upgradeRequest.setUpgradeProperties(upgradeProperties);
		PackageMetadata updatePackageMetadata = this.packageMetadataRepository.findByNameAndVersionByMaxRepoOrder(
				packageName,
				packageVersion);
		assertThat(updatePackageMetadata).isNotNull();
		mockMvc.perform(post("/api/upgrade")
				.content(convertObjectToJson(upgradeRequest))).andDo(print())
				.andExpect(status().isCreated()).andReturn();
		sleep();
		Release updatedRelease = this.releaseRepository.findByNameAndVersion(releaseName, 2);
		commonReleaseAssertions(releaseName, updatePackageMetadata, updatedRelease);
		return updatedRelease;
	}

	protected InstallProperties createDeployProperties(String releaseName) {
		InstallProperties installProperties = new InstallProperties();
		installProperties.setPlatformName("test");
		installProperties.setReleaseName(releaseName);
		return installProperties;
	}

	protected UpgradeProperties createUpdateProperties(String releaseName) {
		UpgradeProperties upgradeProperties = new UpgradeProperties();
		upgradeProperties.setReleaseName(releaseName);
		return upgradeProperties;
	}

	protected void commonReleaseAssertions(String releaseName, PackageMetadata packageMetadata,
			Release deployedRelease) {
		assertThat(deployedRelease.getName()).isEqualTo(releaseName);
		assertThat(deployedRelease.getPlatformName()).isEqualTo("test");
		assertThat(deployedRelease.getPkg().getMetadata()).isEqualToIgnoringGivenFields(packageMetadata,
				"id", "origin", "packageFile", "objectVersion");
		assertThat(deployedRelease.getPkg().getMetadata().equals(packageMetadata));
		assertThat(deployedRelease.getInfo().getStatus().getStatusCode()).isEqualTo(StatusCode.DEPLOYED);
	}

}
