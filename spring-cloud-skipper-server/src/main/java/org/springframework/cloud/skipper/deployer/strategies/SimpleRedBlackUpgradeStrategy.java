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
package org.springframework.cloud.skipper.deployer.strategies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.deployer.AppDeployerDataRepository;
import org.springframework.cloud.skipper.deployer.AppDeploymentRequestFactory;
import org.springframework.cloud.skipper.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.domain.AppDeployerData;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.SpringBootAppKind;
import org.springframework.cloud.skipper.domain.SpringBootAppKindReader;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.repository.DeployerRepository;
import org.springframework.cloud.skipper.repository.ReleaseRepository;

/**
 * Very simple approach to deploying a new application.  All the new apps are deployed and then all the old
 * apps are undeployed. There is no health check done to see if new apps are healthy.
 * @author Mark Pollack
 */
public class SimpleRedBlackUpgradeStrategy implements UpgradeStrategy {

	private final ReleaseRepository releaseRepository;

	private final DeployerRepository deployerRepository;

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final AppDeploymentRequestFactory appDeploymentRequestFactory;

	public SimpleRedBlackUpgradeStrategy(ReleaseRepository releaseRepository,
			DeployerRepository deployerRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			AppDeploymentRequestFactory appDeploymentRequestFactory) {
		this.releaseRepository = releaseRepository;
		this.deployerRepository = deployerRepository;
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.appDeploymentRequestFactory = appDeploymentRequestFactory;
	}

	@Override
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
		AppDeployerData existingAppDeployerData = this.appDeployerDataRepository.findByReleaseNameAndReleaseVersion(
				existingRelease.getName(),
				existingRelease.getVersion());
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

		// TODO update status in DB that the other apps are being deleted.

		this.delete(existingRelease, applicationNamesToUpgrade);

		// Update Status in DB
		Status status = new Status();
		status.setStatusCode(StatusCode.DEPLOYED);
		replacingRelease.getInfo().setStatus(status);
		replacingRelease.getInfo().setDescription("Upgrade complete");

		this.releaseRepository.save(replacingRelease);
		return replacingRelease;
	}

	public Release delete(Release release, List<String> applicationNamesToDelete) {

		AppDeployer appDeployer = this.deployerRepository.findByNameRequired(release.getPlatformName())
				.getAppDeployer();

		AppDeployerData appDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersion(release.getName(), release.getVersion());

		Map<String, String> appNamesAndDeploymentIds = appDeployerData.getDeploymentDataAsMap();

		Status deletingStatus = new Status();
		deletingStatus.setStatusCode(StatusCode.DELETING);
		release.getInfo().setStatus(deletingStatus);
		this.releaseRepository.save(release);

		for (Map.Entry<String, String> appNameAndDeploymentId : appNamesAndDeploymentIds.entrySet()) {
			if (applicationNamesToDelete.contains(appNameAndDeploymentId.getKey())) {
				appDeployer.undeploy(appNameAndDeploymentId.getValue());
			}
		}

		Status deletedStatus = new Status();
		deletedStatus.setStatusCode(StatusCode.DELETED);
		release.getInfo().setStatus(deletedStatus);
		release.getInfo().setDescription("Delete complete");
		this.releaseRepository.save(release);
		return release;
	}

}
