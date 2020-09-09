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
package org.springframework.cloud.skipper.server.deployer.strategies;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifestReader;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.jpa.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.map.DeployerRepository;

/**
 * Checks if the apps in the Replacing release are healthy. Health polling values are set
 * using {@link HealthCheckProperties}
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class HealthCheckStep {

	private final Logger logger = LoggerFactory.getLogger(HealthCheckStep.class);

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final DeployerRepository deployerRepository;

	public HealthCheckStep(AppDeployerDataRepository appDeployerDataRepository, DeployerRepository deployerRepository,
			SpringCloudDeployerApplicationManifestReader applicationManifestReader) {
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.deployerRepository = deployerRepository;
	}

	public boolean isHealthy(Release replacingRelease) {
		AppDeployerData replacingAppDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersionRequired(
						replacingRelease.getName(), replacingRelease.getVersion());
		Map<String, String> appNamesAndDeploymentIds = (replacingAppDeployerData!= null) ?
				replacingAppDeployerData.getDeploymentDataAsMap() : Collections.emptyMap();
		AppDeployer appDeployer = this.deployerRepository
				.findByNameRequired(replacingRelease.getPlatformName())
				.getAppDeployer();
		logger.debug("Getting status for apps in replacing release {}-v{}", replacingRelease.getName(),
				replacingRelease.getVersion());
		// Check all apps and don't go beyond first failed as that is not needed,
		// assume healthy otherwise
		return appNamesAndDeploymentIds.entrySet().stream()
			.map(e -> {
				logger.debug("Checking status for appName={}, deploymentId={}", e.getKey(), e.getValue());
				AppStatus status = appDeployer.status(e.getValue());
				logger.debug("Got status {} for appName={}, deploymentId={}",
						status != null ? status.getState() : null, e.getKey(), e.getValue());
				return status.getState() == DeploymentState.deployed;
			})
			.filter(deployed -> !deployed)
			.findFirst()
			.orElse(true);
	}
}
