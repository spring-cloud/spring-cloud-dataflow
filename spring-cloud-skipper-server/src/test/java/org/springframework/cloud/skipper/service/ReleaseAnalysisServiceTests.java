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
package org.springframework.cloud.skipper.service;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.AbstractIntegrationTest;
import org.springframework.cloud.skipper.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.deployer.ReleaseAnalysisService;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.repository.DeployerRepository;
import org.springframework.cloud.skipper.repository.ReleaseRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
@ActiveProfiles("repo-test")
@TestPropertySource(properties = { "maven.remote-repositories.repo1.url=http://repo.spring.io/libs-snapshot" })
public class ReleaseAnalysisServiceTests extends AbstractIntegrationTest {

	@Autowired
	DeployerRepository deployerRepository;

	@Autowired
	ReleaseService releaseService;

	@Autowired
	ReleaseRepository releaseRepository;

	@Autowired
	ReleaseAnalysisService releaseAnalysisService;

	@Test
	public void test() throws InterruptedException {

		String platformName = "default";

		String releaseName = "logrelease";
		String packageName = "ticktock";
		String packageVersion = "1.0.0";
		InstallProperties installProperties = new InstallProperties();
		installProperties.setReleaseName(releaseName);
		installProperties.setPlatformName(platformName);
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(installProperties);

		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		installRequest.setPackageIdentifier(packageIdentifier);
		Release installedRelease = releaseService.install(installRequest);

		assertThat(installedRelease.getName()).isEqualTo(releaseName);
		System.out.println("installed release \n" + installedRelease.getManifest());

		System.out.println("sleeping for 10 seconds ----------------");
		Thread.sleep(10000);

		UpgradeProperties upgradeProperties = new UpgradeProperties();
		ConfigValues configValues = new ConfigValues();
		// TODO must be a release that exists in a maven repo....
		configValues.setRaw("log:\n  spec:\n    applicationProperties:\n      log.level: error\n");
		// configValues.setRaw("log:\n version: 1.2.0.RELEASE\n");
		upgradeProperties.setConfigValues(configValues);
		upgradeProperties.setReleaseName(releaseName);
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		upgradeRequest.setUpgradeProperties(upgradeProperties);

		packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		upgradeRequest.setPackageIdentifier(packageIdentifier);

		Release upgradedRelease = releaseService.upgrade(upgradeRequest);

		assertThat(upgradedRelease.getName()).isEqualTo(releaseName);
		System.out.println("upgraded relerase \n" + upgradedRelease.getManifest());
		ReleaseAnalysisReport releaseAnalysisReport = this.releaseAnalysisService.analyze(installedRelease,
				upgradedRelease);

		System.out.println("sleeping for 5 seconds ----------------");
		Thread.sleep(5000);
	}

}
