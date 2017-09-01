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

import org.junit.Test;

import org.springframework.cloud.skipper.domain.DeployRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.skipperpackage.DeployProperties;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@ActiveProfiles("repo-test")
@TestPropertySource(properties = { "spring.cloud.skipper.server.synchonizeIndexOnContextRefresh=true",
		"spring.cloud.skipper.server.platform.local.accounts[test].key=value",
		"maven.remote-repositories.repo1.url=http://repo.spring.io/libs-snapshot" })
public class PackageControllerTests extends AbstractControllerTests {

	@Test
	public void deployTickTock() throws Exception {
		String releaseName = "myTicker";
		Release release = deploy("ticktock", "1.0.0", "myTicker");
		assertReleaseIsDeployedSuccessfully(releaseName, "1");
		assertThat(release.getVersion()).isEqualTo(1);
	}

	@Test
	public void packageDeployRequest() throws Exception {
		String releaseName = "myLog";
		DeployRequest deployRequest = new DeployRequest();
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		packageIdentifier.setRepositoryName("notused");
		deployRequest.setPackageIdentifier(packageIdentifier);
		DeployProperties deployProperties = new DeployProperties();
		deployProperties.setReleaseName(releaseName);
		deployProperties.setPlatformName("test");
		deployRequest.setDeployProperties(deployProperties);

		Release release = deploy(deployRequest);
		assertReleaseIsDeployedSuccessfully(releaseName, "1");
		assertThat(release.getVersion()).isEqualTo(1);
	}

	@Test
	public void packageDeployAndUpdate() throws Exception {
		String releaseName = "myLog";
		Release release = deploy("log", "1.0.0", releaseName);
		assertReleaseIsDeployedSuccessfully(releaseName, "1");
		assertThat(release.getVersion()).isEqualTo(1);

		// Update
		release = update("log", "1.1.0", releaseName);
		assertReleaseIsDeployedSuccessfully(releaseName, "2");
		assertThat(release.getVersion()).isEqualTo(2);

	}

}
