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
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.resource.support.LRUCleaningResourceLoader;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.AbstractIntegrationTest;
import org.springframework.cloud.skipper.server.repository.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.PackageMetadataRepository;
import org.springframework.test.context.ActiveProfiles;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests ReleaseService methods.
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 */
@ActiveProfiles("repo-test")
public class ReleaseServiceTests extends AbstractIntegrationTest {

	private final Logger logger = LoggerFactory.getLogger(ReleaseServiceTests.class);

	@Autowired
	private PackageMetadataRepository packageMetadataRepository;

	@Autowired
	private AppDeployerDataRepository appDeployerDataRepository;

	@Autowired
	private DelegatingResourceLoader delegatingResourceLoader;

	@Test
	public void testResourceLoaderInstance() {
		assertThat(this.delegatingResourceLoader).isNotNull();
		assertThat(this.delegatingResourceLoader instanceof LRUCleaningResourceLoader).isTrue();
	}

	@Test
	public void testBadArguments() {
		assertThatThrownBy(() -> releaseService.install(123L, new InstallProperties()))
				.isInstanceOf(SkipperException.class)
				.hasMessageContaining("can not be found");

		assertThatThrownBy(() -> releaseService.install(123L, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Deploy properties can not be null");

		assertThatThrownBy(() -> releaseService.install((Long) null, new InstallProperties()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Package id can not be null");

		assertThatThrownBy(() -> releaseService.rollback("badId", -1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("less than zero");

		assertThatThrownBy(() -> releaseService.rollback("badId", 1))
				.isInstanceOf(ReleaseNotFoundException.class)
				.hasMessageContaining("Release with the name [badId] doesn't exist");

		assertThatThrownBy(() -> releaseService.delete(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testInstallAndUpdatePackageNotFound() throws InterruptedException {
		String releaseName = "logrelease";
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties(releaseName));
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		installRequest.setPackageIdentifier(packageIdentifier);
		Release release = install(installRequest);
		installRequest.setPackageIdentifier(packageIdentifier);
		assertThat(release).isNotNull();
		assertThat(release.getPkg().getMetadata().getVersion()).isEqualTo("1.0.0");
		Info info = this.releaseService.status(releaseName);
		assertThat(info).isNotNull();

		UpgradeProperties upgradeProperties = new UpgradeProperties();
		upgradeProperties.setReleaseName(releaseName);
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		upgradeRequest.setUpgradeProperties(upgradeProperties);
		packageIdentifier = new PackageIdentifier();
		String packageName = "random";
		String packageVersion = "1.0.0";
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		upgradeRequest.setPackageIdentifier(packageIdentifier);
		try {
			releaseService.upgrade(upgradeRequest);
			fail("Expected to throw SkipperException");
		}
		catch (SkipperException e) {
			assertThat(e.getMessage()).isEqualTo(String.format("Can not find package '%s', version '%s'",
					packageName, packageVersion));
		}

		delete(release.getName());
	}

	@Test
	public void testInstallByLatestPackage() throws InterruptedException {
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties("latestPackage"));
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		installRequest.setPackageIdentifier(packageIdentifier);
		Release release = install(installRequest);
		assertThat(release).isNotNull();
		assertThat(release.getPkg().getMetadata().getVersion()).isEqualTo("2.0.0");
		delete(release.getName());

	}

	@Test(expected = ReleaseNotFoundException.class)
	public void testStatusReleaseDoesNotExist() {
		releaseService.status("notexist");
	}

	@Test
	public void testPackageNotFound() {
		boolean exceptionFired = false;
		try {
			this.packageMetadataRepository.findByNameAndOptionalVersionRequired("random", "1.2.4");
		}
		catch (SkipperException se) {
			assertThat(se.getMessage()).isEqualTo("Can not find package 'random', version '1.2.4'");
			exceptionFired = true;
		}
		assertThat(exceptionFired).isTrue();
	}

	@Test
	public void testInstallPackageNotFound() {
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties("latestPackage"));
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("random");
		installRequest.setPackageIdentifier(packageIdentifier);
		try {
			releaseService.install(installRequest);
			fail("SkipperException is expected for non existing package");
		}
		catch (Exception se) {
			assertThat(se.getMessage()).isEqualTo("Can not find a package named 'random'");
		}
	}

	@Test
	public void testLatestPackageByName() {
		String packageName = "log";
		PackageMetadata packageMetadata = this.packageMetadataRepository.findFirstByNameOrderByVersionDesc(packageName);
		PackageMetadata latestPackageMetadata = this.packageMetadataRepository
				.findByNameAndOptionalVersionRequired(packageName, null);
		assertThat(packageMetadata).isEqualTo(latestPackageMetadata);
	}

	@Test
	public void testInstallReleaseThatIsNotDeleted() throws InterruptedException {
		String releaseName = "installDeployedRelease";
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties(releaseName));
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		installRequest.setPackageIdentifier(packageIdentifier);
		Release release = install(installRequest);
		assertThat(release).isNotNull();

		// Now let's install it a second time.
		try {
			install(installRequest);
			fail("Expected to fail when installing already deployed release.");
		}
		catch (SkipperException e) {
			assertThat(e.getMessage()).isEqualTo("Release with the name [" + releaseName + "] already exists "
					+ "and it is not deleted.");
		}
	}

	@Test
	public void testInstallDeletedRelease() throws InterruptedException {
		String releaseName = "deletedRelease";
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties(releaseName));
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		installRequest.setPackageIdentifier(packageIdentifier);
		// Install
		Release release = install(installRequest);
		assertThat(release).isNotNull();
		// Delete
		delete(releaseName);
		// Install again
		Release release2 = install(installRequest);
		assertThat(release2.getVersion()).isEqualTo(2);
	}

	@Test
	public void testRollbackDeletedRelease() throws InterruptedException {
		String releaseName = "rollbackDeletedRelease";
		InstallRequest installRequest = new InstallRequest();
		InstallProperties installProperties = createInstallProperties(releaseName);
		ConfigValues installConfig = new ConfigValues();
		installConfig.setRaw("log:\n  version: 1.2.0.RC1\ntime:\n  version: 1.2.0.RC1\n");
		installProperties.setConfigValues(installConfig);
		installRequest.setInstallProperties(installProperties);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		installRequest.setPackageIdentifier(packageIdentifier);
		// Install
		logger.info("Installing log 1.0.0 package");
		Release release = install(installRequest);
		assertThat(release).isNotNull();
		assertThat(release.getVersion()).isEqualTo(1);
		this.appDeployerDataRepository.findByReleaseNameAndReleaseVersionRequired(releaseName, 1);

		// Upgrade
		UpgradeProperties upgradeProperties = new UpgradeProperties();
		upgradeProperties.setReleaseName(releaseName);
		ConfigValues upgradeConfig = new ConfigValues();
		upgradeConfig.setRaw("log:\n  version: 1.2.0.RELEASE\ntime:\n  version: 1.2.0.RELEASE\n");
		upgradeProperties.setConfigValues(upgradeConfig);
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		upgradeRequest.setUpgradeProperties(upgradeProperties);
		packageIdentifier = new PackageIdentifier();
		String packageName = "log";
		String packageVersion = "2.0.0";
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		upgradeRequest.setPackageIdentifier(packageIdentifier);
		logger.info("Upgrading to log 2.0.0 package");
		Release upgradedRelease = upgrade(upgradeRequest);

		assertThat(upgradedRelease.getVersion()).isEqualTo(2);
		assertThat(upgradedRelease.getConfigValues()).isEqualTo(upgradeRequest.getUpgradeProperties().getConfigValues());
		this.appDeployerDataRepository.findByReleaseNameAndReleaseVersionRequired(releaseName, 2);

		// Delete
		delete(releaseName);

		Release deletedRelease = releaseRepository.findByNameAndVersion(releaseName, 2);
		assertThat(deletedRelease.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED));

		// Rollback
		logger.info("Rolling back the release " + release);

		Release rolledBackRelease = rollback(releaseName, 0);

		assertThat(rolledBackRelease.getManifest()).isEqualTo(release.getManifest());
		assertThat(rolledBackRelease.getConfigValues().getRaw()).isEqualTo(release.getConfigValues().getRaw());
		assertThat(rolledBackRelease.getInfo().getStatus().getStatusCode().equals(StatusCode.DEPLOYED));

		deletedRelease = releaseRepository.findByNameAndVersion(releaseName, 2);
		assertThat(deletedRelease.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED));

		delete(rolledBackRelease.getName());
	}

}
