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
package org.springframework.cloud.skipper.deployer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.deployer.strategies.SimpleRedBlackUpgradeStrategy;
import org.springframework.cloud.skipper.domain.AppDeployerData;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.SpringBootAppKind;
import org.springframework.cloud.skipper.domain.SpringBootAppKindReader;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.repository.DeployerRepository;
import org.springframework.cloud.skipper.repository.ReleaseRepository;
import org.springframework.cloud.skipper.service.ReleaseManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


/**
 * A ReleaseManager implementation that uses an AppDeployer.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@Service
public class AppDeployerReleaseManager implements ReleaseManager {

	private static final Logger logger = LoggerFactory.getLogger(AppDeployerReleaseManager.class);

	private final ReleaseRepository releaseRepository;

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final DeployerRepository deployerRepository;

	private final ReleaseAnalysisService releaseAnalysisService;

	private final AppDeploymentRequestFactory appDeploymentRequestFactory;

	@Autowired
	public AppDeployerReleaseManager(ReleaseRepository releaseRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			DeployerRepository deployerRepository,
			ReleaseAnalysisService releaseAnalysisService,
			AppDeploymentRequestFactory appDeploymentRequestFactory) {
		this.releaseRepository = releaseRepository;
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.deployerRepository = deployerRepository;
		this.releaseAnalysisService = releaseAnalysisService;
		this.appDeploymentRequestFactory = appDeploymentRequestFactory;
	}

	public Release install(Release releaseInput) {

		Release release = this.releaseRepository.save(releaseInput);
		logger.debug("Manifest = " + releaseInput.getManifest());
		// Deploy the application
		List<SpringBootAppKind> springBootAppKindList = SpringBootAppKindReader.read(release.getManifest());
		AppDeployer appDeployer = this.deployerRepository.findByNameRequired(release.getPlatformName())
				.getAppDeployer();
		Map<String, String> appNameDeploymentIdMap = new HashMap<>();
		for (SpringBootAppKind springBootAppKind : springBootAppKindList) {
			AppDeploymentRequest appDeploymentRequest = appDeploymentRequestFactory.createAppDeploymentRequest(
					springBootAppKind,
					release.getName(),
					String.valueOf(release.getVersion()));
			String deploymentId = appDeployer.deploy(appDeploymentRequest);
			appNameDeploymentIdMap.put(springBootAppKind.getApplicationName(), deploymentId);
		}

		AppDeployerData appDeployerData = new AppDeployerData();
		appDeployerData.setReleaseName(release.getName());
		appDeployerData.setReleaseVersion(release.getVersion());
		appDeployerData.setDeploymentDataUsingMap(appNameDeploymentIdMap);

		this.appDeployerDataRepository.save(appDeployerData);

		// Update Status in DB
		Status status = new Status();
		status.setStatusCode(StatusCode.DEPLOYED);
		release.getInfo().setStatus(status);
		release.getInfo().setDescription("Install complete");

		// Store updated state in in DB and compute status
		return status(this.releaseRepository.save(release));
	}

	@Override
	public Release upgrade(Release existingRelease, Release replacingRelease, String upgradeStrategyName) {

		Release release = this.releaseRepository.save(replacingRelease);

		ReleaseAnalysisReport releaseAnalysisReport = this.releaseAnalysisService.analyze(existingRelease,
				replacingRelease);

		if (!releaseAnalysisReport.getReleaseDifference().areEqual()) {
			logger.info("Difference report for upgrade of release " + replacingRelease.getName());
			logger.info(releaseAnalysisReport.getReleaseDifference().getDifferenceSummary());
			// Do upgrades
			if (upgradeStrategyName.equals("simple")) {
				SimpleRedBlackUpgradeStrategy simpleRedBlackUpdateStrategy = createRedBlackUpdateStrategy();
				release = simpleRedBlackUpdateStrategy.upgrade(existingRelease, release, releaseAnalysisReport);
			}
			else {
				throw new SkipperException("Unsupported Upgrade Strategy Name [" + upgradeStrategyName + "]");
			}
		}
		else {
			throw new SkipperException(
					"Package to upgrade has not difference than existing deployed package. Not upgrading.");
		}

		return status(release);
	}

	private SimpleRedBlackUpgradeStrategy createRedBlackUpdateStrategy() {
		return new SimpleRedBlackUpgradeStrategy(
				this.releaseRepository,
				this.deployerRepository,
				this.appDeployerDataRepository,
				this.appDeploymentRequestFactory);
	}

	public Release status(Release release) {
		AppDeployer appDeployer = this.deployerRepository.findByNameRequired(release.getPlatformName())
				.getAppDeployer();
		AppDeployerData appDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersion(release.getName(), release.getVersion());
		List<String> deploymentIds = appDeployerData.getDeploymentIds();
		deploymentIds.addAll(StringUtils.commaDelimitedListToSet(appDeployerData.getDeploymentData()));
		logger.debug("Getting status for {} using deploymentIds {}", release,
				StringUtils.collectionToCommaDelimitedString(deploymentIds));


		if (!deploymentIds.isEmpty()) {
			// mainly track deployed and unknown statuses. for any other
			// combination, get more details from instances.
			int deployedCount = 0;
			int unknownCount = 0;
			List<String> platformStatusMessages = new ArrayList<>();
			for (String deploymentId : deploymentIds) {
				AppStatus appStatus = appDeployer.status(deploymentId);
				logger.debug("App Deployer for deploymentId {} gives status {}", deploymentId, appStatus);

				StringBuffer statusMsg = new StringBuffer(deploymentId + "=[");
				for (AppInstanceStatus appInstanceStatus : appStatus.getInstances().values()) {
					statusMsg.append(appInstanceStatus.getId() + "=" + appInstanceStatus.getState() + "\n");
				}
				statusMsg.setLength(statusMsg.length() - 1);
				statusMsg.append("]");
				platformStatusMessages.add(statusMsg.toString());

				switch (appStatus.getState()) {
					case deployed:
						deployedCount++;
						break;
					case unknown:
						unknownCount++;
						break;
					case deploying:
					case undeployed:
					case partial:
					case failed:
					case error:
					default:
						break;
				}
			}
			if (deployedCount == deploymentIds.size()) {
				release.getInfo().getStatus().setPlatformStatus("All the applications are deployed successfully.\n"
						+ StringUtils.collectionToCommaDelimitedString(platformStatusMessages));
			}
			else if (unknownCount == deploymentIds.size()) {
				release.getInfo().getStatus().setPlatformStatus("All applications unknown to system.\n");
			}
			else {
				release.getInfo().getStatus().setPlatformStatus("State of applications:\n"
						+ StringUtils.collectionToCommaDelimitedString(platformStatusMessages));
			}
		}
		return release;
	}

	public Release delete(Release release) {
		AppDeployer appDeployer = this.deployerRepository.findByNameRequired(release.getPlatformName())
				.getAppDeployer();

		AppDeployerData appDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersion(release.getName(), release.getVersion());
		List<String> deploymentIds = appDeployerData.getDeploymentIds();
		if (!deploymentIds.isEmpty()) {
			Status deletingStatus = new Status();
			deletingStatus.setStatusCode(StatusCode.DELETING);
			release.getInfo().setStatus(deletingStatus);
			this.releaseRepository.save(release);
			for (String deploymentId : deploymentIds) {
				appDeployer.undeploy(deploymentId);
			}
			Status deletedStatus = new Status();
			deletedStatus.setStatusCode(StatusCode.DELETED);
			release.getInfo().setStatus(deletedStatus);
			release.getInfo().setDescription("Delete complete");
			this.releaseRepository.save(release);
		}
		return release;
	}

}
