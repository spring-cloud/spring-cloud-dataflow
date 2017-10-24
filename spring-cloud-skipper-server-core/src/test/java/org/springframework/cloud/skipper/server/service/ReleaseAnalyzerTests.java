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
package org.springframework.cloud.skipper.server.service;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.AbstractIntegrationTest;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalyzer;
import org.springframework.cloud.skipper.server.deployer.strategies.HealthCheckProperties;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@ActiveProfiles("repo-test")
@TestPropertySource(properties = { "maven.remote-repositories.repo1.url=http://repo.spring.io/libs-snapshot" })
public class ReleaseAnalyzerTests extends AbstractIntegrationTest {

	private final Logger logger = LoggerFactory.getLogger(ReleaseAnalyzerTests.class);

	@Autowired
	DeployerRepository deployerRepository;

	@Autowired
	ReleaseService releaseService;

	@Autowired
	ReleaseRepository releaseRepository;

	@Autowired
	ReleaseAnalyzer releaseAnalyzer;

	@Autowired
	private HealthCheckProperties healthCheckProperties;

	@After
	public void cleanup() {
		// Sleep until asynchronous health check to be done for the upgrade - the max time would be the configured
		// timeout
		try {
			Thread.sleep(this.healthCheckProperties.getTimeoutInMillis());
		}
		catch (InterruptedException e) {
			logger.info("Interrupted exception ", e);
			Thread.currentThread().interrupt();
		}
	}

	@Test
	public void releaseAnalyzerTest() throws InterruptedException {

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
		logger.info("installed release \n" + installedRelease.getManifest());

		logger.info("sleeping for 10 seconds ----------------");
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
		System.out.println("upgraded release \n" + upgradedRelease.getManifest());
		ReleaseAnalysisReport releaseAnalysisReport = this.releaseAnalyzer.analyze(installedRelease,
				upgradedRelease);

		System.out.println("sleeping for 5 seconds ----------------");
		Thread.sleep(5000);
		assertThat(releaseAnalysisReport.getReleaseDifference()).isNotNull();
		assertThat(releaseAnalysisReport.getReleaseDifference().getDifferenceSummary())
				.contains("log.level=(DEBUG, error)");
	}

}
