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
package org.springframework.cloud.skipper.server.service;

import org.junit.jupiter.api.Test;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@ActiveProfiles({"repo-test", "local"})
@TestPropertySource(properties = {
		"maven.remote-repositories.repo1.url=https://repo.spring.io/snapshot",
		"maven.remote-repositories.repo2.url=https://repo.spring.io/milestone"
})
class ReleaseAnalyzerTests extends AbstractIntegrationTest {

	private final Logger logger = LoggerFactory.getLogger(ReleaseAnalyzerTests.class);

	@Autowired
	ReleaseService releaseService;

	@Autowired
	ReleaseAnalyzer releaseAnalyzer;

	@Test
	void releaseAnalyzerAndAdditiveUpgradeTest() throws InterruptedException {
		// NOTE must be a release that exists in a maven repo....
		String releaseName = "logreleaseAnalyzer";
		String packageName = "ticktock";
		String packageVersion = "1.0.0";
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties(releaseName));

		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		installRequest.setPackageIdentifier(packageIdentifier);

		// Install Release
		Release installedRelease = install(installRequest);


		assertThat(installedRelease.getName()).isEqualTo(releaseName);
		logger.info("Initial Release Manifest: \n" + installedRelease.getManifest().getData());

		UpgradeProperties upgradeProperties = new UpgradeProperties();
		ConfigValues configValues = new ConfigValues();
		// TODO must be a release that exists in a maven repo....
		configValues.setRaw("log:\n  spec:\n    applicationProperties:\n      log.level: error\n      foo: bar\n");
		upgradeProperties.setConfigValues(configValues);
		upgradeProperties.setReleaseName(releaseName);
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		upgradeRequest.setUpgradeProperties(upgradeProperties);

		packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		upgradeRequest.setPackageIdentifier(packageIdentifier);


		// Upgrade Release
		Release upgradedRelease = upgrade(upgradeRequest);
		ReleaseAnalysisReport releaseAnalysisReport = this.releaseAnalyzer.analyze(installedRelease,
				upgradedRelease, false, null);
		String releaseDifferenceSummary = releaseAnalysisReport.getReleaseDifferenceSummary();
		String manifest = upgradedRelease.getManifest().getData();


		assertThat(upgradedRelease.getName()).isEqualTo(releaseName);
		assertThat(releaseAnalysisReport.getReleaseDifference()).isNotNull();

		logger.info("Release Manifest v2: \n" + upgradedRelease.getManifest().getData());

		assertThat(releaseDifferenceSummary).contains("log.level=(DEBUG, error)");
		assertThat(releaseDifferenceSummary).contains("foo=(bar)");
		assertThat(manifest).contains("\"foo\": \"bar\"");
		assertThat(manifest).contains("\"log.level\": \"error\"");


		// Upgrade using a new property, assert that old properties are carried over.
		configValues = new ConfigValues();
		configValues.setRaw("log:\n  spec:\n    applicationProperties:\n      foo2: bar2\n");
		upgradeProperties.setConfigValues(configValues);
		upgradeRequest.setUpgradeProperties(upgradeProperties);

		// Upgrade Release to V3, ensure application property foo=bar and foo2=bar2 are both present.
		Release upgradedReleaseV3 = upgrade(upgradeRequest);
		releaseAnalysisReport = this.releaseAnalyzer.analyze(upgradedRelease, upgradedReleaseV3, false, null);
		releaseDifferenceSummary = releaseAnalysisReport.getReleaseDifferenceSummary();
		manifest = upgradedReleaseV3.getManifest().getData();

		logger.info("Release Manifest v3: \n" + manifest);

		assertThat(releaseDifferenceSummary).contains("foo2=(bar2)");
		assertThat(manifest).contains("\"foo\": \"bar\"");
		assertThat(manifest).contains("\"log.level\": \"error\"");
		assertThat(manifest).contains("\"foo2\": \"bar2\"");

		// Upgrade Release to V4, ensure `force` upgrade is set.
		upgradeRequest.setForce(true);
		Release upgradedReleaseV4 = upgrade(upgradeRequest);
		releaseAnalysisReport = this.releaseAnalyzer.analyze(upgradedReleaseV3, upgradedReleaseV4, true, null);
		manifest = upgradedReleaseV4.getManifest().getData();

		logger.info("Release Manifest v4: \n" + manifest);

		assertThat(releaseAnalysisReport.getReleaseDifference().areEqual()).isTrue();
	}

}
