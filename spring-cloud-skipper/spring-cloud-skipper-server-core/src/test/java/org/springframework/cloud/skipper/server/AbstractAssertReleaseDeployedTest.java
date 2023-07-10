/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.server;

import java.time.Duration;
import java.util.List;

import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.InstallProperties;

/**
 * @author Mark Pollack
 * @author Corneil du Plessis
 */
public abstract class AbstractAssertReleaseDeployedTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected void assertReleaseIsDeployedSuccessfully(String releaseName, int releaseVersion) {
		logger.info("Awaiting release={} version={}", releaseName, releaseVersion);
		Awaitility.await("Release " + releaseName + "-v" + releaseVersion)
				.atMost(Duration.ofMinutes(5))
				.pollInterval(Duration.ofSeconds(10))
				.until(() -> isDeployed(releaseName, releaseVersion));
	}

	protected boolean allAppsDeployed(List<AppStatus> appStatusList) {
		return appStatusList.stream()
				.allMatch(appStatus -> appStatus.getState() == DeploymentState.deployed);
	}

	protected abstract boolean isDeployed(String releaseName, int releaseVersion);

	protected InstallProperties createInstallProperties(String releaseName) {
		InstallProperties installProperties = new InstallProperties();
		installProperties.setReleaseName(releaseName);
		installProperties.setPlatformName("default");
		return installProperties;
	}
}
