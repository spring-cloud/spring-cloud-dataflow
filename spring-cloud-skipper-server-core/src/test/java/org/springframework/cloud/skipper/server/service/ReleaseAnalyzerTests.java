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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.domain.ConfigValues;
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

	@Test
	public void releaseAnalyzerTest() throws InterruptedException {
		String releaseName = "logreleaseAnalyzer";
		String packageName = "ticktock";
		String packageVersion = "1.0.0";
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties(releaseName));

		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		installRequest.setPackageIdentifier(packageIdentifier);
		Release installedRelease = install(installRequest);

		assertThat(installedRelease.getName()).isEqualTo(releaseName);
		logger.info("installed release \n" + installedRelease.getManifest());

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

		Release upgradedRelease = upgrade(upgradeRequest);

		assertThat(upgradedRelease.getName()).isEqualTo(releaseName);
		logger.info("upgraded release \n" + upgradedRelease.getManifest());
		ReleaseAnalysisReport releaseAnalysisReport = this.releaseAnalyzer.analyze(installedRelease,
				upgradedRelease);

		assertThat(releaseAnalysisReport.getReleaseDifference()).isNotNull();
		System.out.println(releaseAnalysisReport.getReleaseDifferenceSummary());
		assertThat(releaseAnalysisReport.getReleaseDifferenceSummary())
				.contains("log.level=(DEBUG, error)");
	}

}
