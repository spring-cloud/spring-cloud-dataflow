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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.server.deployer.AppDeploymentRequestFactory;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.domain.SpringBootAppKind;
import org.springframework.cloud.skipper.server.domain.SpringBootAppKindReader;
import org.springframework.cloud.skipper.server.repository.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

/**
 * Very simple approach to deploying a new application. All the new apps are deployed and
 * then all the old apps are undeployed. There is no health check done to see if new apps
 * are healthy.
 * @author Mark Pollack
 */
public class SimpleRedBlackUpgradeStrategy implements UpgradeStrategy {

	private final Logger logger = LoggerFactory.getLogger(SimpleRedBlackUpgradeStrategy.class);

	private final ReleaseRepository releaseRepository;

	private final DeployerRepository deployerRepository;

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final AppDeploymentRequestFactory appDeploymentRequestFactory;

	private final HealthCheckAndDeleteStep healthCheckAndDeleteStep;

	public SimpleRedBlackUpgradeStrategy(ReleaseRepository releaseRepository,
			DeployerRepository deployerRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			AppDeploymentRequestFactory appDeploymentRequestFactory,
			HealthCheckAndDeleteStep healthCheckAndDeleteStep) {
		this.releaseRepository = releaseRepository;
		this.deployerRepository = deployerRepository;
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.appDeploymentRequestFactory = appDeploymentRequestFactory;
		this.healthCheckAndDeleteStep = healthCheckAndDeleteStep;
	}

	@Override
	@Async("skipperThreadPoolTaskExecutor")
	@Transactional
	public Release upgrade(Release existingRelease, Release replacingRelease,
			ReleaseAnalysisReport releaseAnalysisReport) {

		List<String> applicationNamesToUpgrade = releaseAnalysisReport.getApplicationNamesToUpgrade();
		AppDeployer appDeployer = this.deployerRepository.findByNameRequired(replacingRelease.getPlatformName())
				.getAppDeployer();

		// Update status in the db "To be upgraded [time, log]. Upgrading [log].

		// Deploy the application
		List<SpringBootAppKind> springBootAppKindList = SpringBootAppKindReader.read(replacingRelease.getManifest());

		Map<String, String> appNameDeploymentIdMap = new HashMap<>();
		for (SpringBootAppKind springBootAppKind : springBootAppKindList) {
			if (applicationNamesToUpgrade.contains(springBootAppKind.getApplicationName())) {
				AppDeploymentRequest appDeploymentRequest = appDeploymentRequestFactory.createAppDeploymentRequest(
						springBootAppKind, replacingRelease.getName(),
						String.valueOf(replacingRelease.getVersion()));
				String deploymentId = appDeployer.deploy(appDeploymentRequest);
				appNameDeploymentIdMap.put(springBootAppKind.getApplicationName(), deploymentId);
			}
		}

		// Carry over the applicationDeployment information for apps that were not updated.
		AppDeployerData existingAppDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersionRequired(
						existingRelease.getName(), existingRelease.getVersion());
		Map<String, String> existingAppNamesAndDeploymentIds = existingAppDeployerData.getDeploymentDataAsMap();

		for (Map.Entry<String, String> existingEntry : existingAppNamesAndDeploymentIds.entrySet()) {
			String existingName = existingEntry.getKey();
			if (!appNameDeploymentIdMap.containsKey(existingName)) {
				appNameDeploymentIdMap.put(existingName, existingEntry.getValue());
			}
		}

		AppDeployerData appDeployerData = new AppDeployerData();
		appDeployerData.setReleaseName(replacingRelease.getName());
		appDeployerData.setReleaseVersion(replacingRelease.getVersion());
		appDeployerData.setDeploymentDataUsingMap(appNameDeploymentIdMap);
		this.appDeployerDataRepository.save(appDeployerData);

		// AppDeployerData replacingAppDeployerData = this.appDeployerDataRepository
		// .findByReleaseNameAndReleaseVersionRequired(
		// replacingRelease.getName(), replacingRelease.getVersion());

		this.healthCheckAndDeleteStep.waitForNewAppsToDeploy(existingRelease,
				applicationNamesToUpgrade, replacingRelease);

		return replacingRelease;
	}

}
