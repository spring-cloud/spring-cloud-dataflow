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
package org.springframework.cloud.skipper.server.deployer.strategies;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;

/**
 * Checks if the apps in the Replacing release are healthy. Health polling values are set
 * using {@link HealthCheckProperties}
 * @author Mark Pollack
 */
public class HealthCheckStep {

	private final Logger logger = LoggerFactory.getLogger(HealthCheckStep.class);

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final HealthCheckProperties healthCheckProperties;

	private final DeployerRepository deployerRepository;

	public HealthCheckStep(AppDeployerDataRepository appDeployerDataRepository,
			DeployerRepository deployerRepository,
			HealthCheckProperties healthCheckProperties) {
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.deployerRepository = deployerRepository;
		this.healthCheckProperties = healthCheckProperties;
	}

	public boolean isHealthy(Release replacingRelease) {
		AppDeployerData replacingAppDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersionRequired(
						replacingRelease.getName(), replacingRelease.getVersion());

		Map<String, String> appNamesAndDeploymentIds = replacingAppDeployerData.getDeploymentDataAsMap();

		AppDeployer appDeployer = this.deployerRepository
				.findByNameRequired(replacingRelease.getPlatformName())
				.getAppDeployer();

		logger.debug("Getting status for apps in replacing release {}-v{}", replacingRelease.getName(),
				replacingRelease.getVersion());
		for (Map.Entry<String, String> appNameAndDeploymentId : appNamesAndDeploymentIds.entrySet()) {
			AppStatus status = appDeployer.status(appNameAndDeploymentId.getValue());
			if (status.getState() == DeploymentState.deployed) {
				return true;
			}
		}

		return false;
	}

	public boolean waitForNewAppsToDeploy(Release replacingRelease) {
		AppDeployerData replacingAppDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersionRequired(
						replacingRelease.getName(), replacingRelease.getVersion());

		logger.info("Waiting for apps in release {}-v{} to be healthy.", replacingRelease.getName(),
				replacingRelease.getVersion());

		// Poll for health of replacing release.
		return this.isHealthy(replacingRelease, replacingAppDeployerData);
	}

	private boolean isHealthy(Release replacingRelease, AppDeployerData replacingAppDeployerData) {
		boolean isHealthy = false;
		try {
			long timeout = System.currentTimeMillis() + this.healthCheckProperties.getTimeoutInMillis();
			Map<String, String> appNamesAndDeploymentIds = replacingAppDeployerData.getDeploymentDataAsMap();

			while (!isHealthy && System.currentTimeMillis() < timeout) {
				try {
					logger.debug("Health check for replacing release {}-v{}, sleeping for {} milliseconds.",
							replacingRelease.getName(),
							replacingRelease.getVersion(),
							this.healthCheckProperties.getTimeoutInMillis());
					sleep();
					AppDeployer appDeployer = this.deployerRepository
							.findByNameRequired(replacingRelease.getPlatformName())
							.getAppDeployer();

					logger.debug("Getting status for apps in replacing release {}-v{}", replacingRelease.getName(),
							replacingRelease.getVersion());
					for (Map.Entry<String, String> appNameAndDeploymentId : appNamesAndDeploymentIds.entrySet()) {
						AppStatus status = appDeployer.status(appNameAndDeploymentId.getValue());
						if (status.getState() == DeploymentState.deployed) {
							isHealthy = true;
						}
					}
				}
				catch (Exception e) {
					logger.error("Exception while checking for replacing release status.", e);
				}
			}
		}
		catch (Exception e) {
			logger.error("Exception while checking for replacing release status.", e);
		}
		return isHealthy;
	}

	private void sleep() {
		try {
			Thread.currentThread().sleep(healthCheckProperties.getSleepInMillis());
		}
		catch (InterruptedException e) {
			logger.info("Interrupted exception ", e);
			Thread.currentThread().interrupt();
		}
	}

}
