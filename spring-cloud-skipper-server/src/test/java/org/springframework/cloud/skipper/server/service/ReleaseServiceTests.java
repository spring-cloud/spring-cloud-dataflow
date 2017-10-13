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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.AbstractIntegrationTest;
import org.springframework.test.context.ActiveProfiles;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@ActiveProfiles("repo-test")
public class ReleaseServiceTests extends AbstractIntegrationTest {

	@Autowired
	private ReleaseService releaseService;

	@Test
	public void testBadArguments() {
		assertThatThrownBy(() -> releaseService.install("badId", new InstallProperties()))
				.isInstanceOf(SkipperException.class)
				.hasMessageContaining("can not be found");

		assertThatThrownBy(() -> releaseService.install("badId", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Deploy properties can not be null");

		assertThatThrownBy(() -> releaseService.install((String) null, new InstallProperties()))
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
	public void testInstall() {
		InstallProperties installProperties = new InstallProperties();
		installProperties.setReleaseName("logrelease");
		installProperties.setPlatformName("default");
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(installProperties);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		installRequest.setPackageIdentifier(packageIdentifier);
		Release release = releaseService.install(installRequest);
		assertThat(release).isNotNull();
		assertThat(release.getPkg().getMetadata().getVersion()).isEqualTo("1.0.0");
	}

	@Test
	public void testInstallByLatestPackage() {
		InstallProperties installProperties = new InstallProperties();
		installProperties.setReleaseName("latestPackage");
		installProperties.setPlatformName("default");
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(installProperties);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		installRequest.setPackageIdentifier(packageIdentifier);
		Release release = releaseService.install(installRequest);
		assertThat(release).isNotNull();
		assertThat(release.getPkg().getMetadata().getVersion()).isEqualTo("2.0.0");
	}

	@Test(expected = ReleaseNotFoundException.class)
	public void testStatusReleaseDoesNotExist() {
		releaseService.status("notexist");
	}

	@Test(expected = SkipperException.class)
	public void testPackageNotFound() {
		this.releaseService.getPackageMetadata("random", "1.2.4");
	}

	@Test
	public void testStatusReleaseExist() {
		InstallProperties installProperties = new InstallProperties();
		installProperties.setReleaseName("testexists");
		installProperties.setPlatformName("default");
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(installProperties);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		installRequest.setPackageIdentifier(packageIdentifier);
		releaseService.install(installRequest);
		Info info = releaseService.status("testexists");
		assertThat(info).isNotNull();
	}

	@Test
	public void testUpdatePackageNotFound() {
		PackageMetadata packageMetadata1 = new PackageMetadata();
		packageMetadata1.setName("package1");
		packageMetadata1.setVersion("1.0.0");
		Package pkg1 = new Package();
		pkg1.setMetadata(packageMetadata1);

		Info deployedInfo = new Info();
		Status deployedStatus = new Status();
		deployedStatus.setPlatformStatus("Deployed successfully");
		deployedStatus.setStatusCode(StatusCode.DEPLOYED);
		deployedInfo.setStatus(deployedStatus);

		String releaseName = "existing";
		Release release1 = new Release();
		release1.setName(releaseName);
		release1.setVersion(1);
		release1.setPlatformName("platform1");
		release1.setPkg(pkg1);
		release1.setInfo(deployedInfo);
		this.releaseRepository.save(release1);

		UpgradeProperties upgradeProperties = new UpgradeProperties();
		upgradeProperties.setReleaseName(releaseName);
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		upgradeRequest.setUpgradeProperties(upgradeProperties);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		String packageName = "random";
		String packageVersion = "1.0.0";
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		upgradeRequest.setPackageIdentifier(packageIdentifier);
		try {
			Release release = releaseService.upgrade(upgradeRequest);
			fail("Expected to throw SkipperException");
		}
		catch (SkipperException e) {
			assertThat(e.getMessage()).isEqualTo(String.format("Can not find package '%s', version '%s'",
					packageName, packageVersion));
		}
	}
}
