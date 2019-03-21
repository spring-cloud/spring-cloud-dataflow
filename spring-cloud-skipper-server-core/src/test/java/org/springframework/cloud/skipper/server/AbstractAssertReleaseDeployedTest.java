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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.InstallProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Mark Pollack
 */
public abstract class AbstractAssertReleaseDeployedTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected void assertReleaseIsDeployedSuccessfully(String releaseName, int releaseVersion)
			throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		long startTime = System.currentTimeMillis();
		while (!isDeployed(releaseName, releaseVersion)) {
			if ((System.currentTimeMillis() - startTime) > 180000) {
				logger.info("Stopping polling for deployed status after 3 minutes for release={} version={}",
						releaseName, releaseVersion);
				fail("Could not determine if release " + releaseName + "-v" + releaseVersion +
						" was deployed successfully, timed out polling after 3 minutes.");
			}
			Thread.sleep(10000);
		}
		if (isDeployed(releaseName, releaseVersion)) {
			logger.info("Deployed! release={} version={}", releaseName, releaseVersion);
			latch.countDown();
		}
		assertThat(latch.await(1, TimeUnit.SECONDS)).describedAs("Status check timed out").isTrue();
	}

	protected boolean allAppsDeployed(List<AppStatus> appStatusList) {
		boolean allDeployed = true;
		for (AppStatus appStatus : appStatusList) {
			if (appStatus.getState() != DeploymentState.deployed) {
				allDeployed = false;
				break;
			}
		}
		return allDeployed;
	}

	protected abstract boolean isDeployed(String releaseName, int releaseVersion);

	protected InstallProperties createInstallProperties(String releaseName) {
		InstallProperties installProperties = new InstallProperties();
		installProperties.setReleaseName(releaseName);
		installProperties.setPlatformName("default");
		return installProperties;
	}
}
