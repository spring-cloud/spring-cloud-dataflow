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
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.server.AbstractIntegrationTest;
import org.springframework.test.context.ActiveProfiles;

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
}
