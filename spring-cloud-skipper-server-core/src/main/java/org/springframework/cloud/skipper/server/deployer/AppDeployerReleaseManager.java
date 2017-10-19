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
package org.springframework.cloud.skipper.server.deployer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.deployer.strategies.SimpleRedBlackUpgradeStrategy;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.domain.SpringBootAppKind;
import org.springframework.cloud.skipper.server.domain.SpringBootAppKindReader;
import org.springframework.cloud.skipper.server.domain.SpringBootAppSpec;
import org.springframework.cloud.skipper.server.repository.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.cloud.skipper.server.service.ConfigValueUtils;
import org.springframework.cloud.skipper.server.service.ManifestUtils;
import org.springframework.util.StringUtils;

/**
 * A ReleaseManager implementation that uses an AppDeployer.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class AppDeployerReleaseManager implements ReleaseManager {

	private static final Logger logger = LoggerFactory.getLogger(AppDeployerReleaseManager.class);

	public static final String SPRING_CLOUD_DEPLOYER_COUNT = "spring.cloud.deployer.count";

	private final ReleaseRepository releaseRepository;

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final DeployerRepository deployerRepository;

	private final ReleaseAnalyzer releaseAnalyzer;

	private final AppDeploymentRequestFactory appDeploymentRequestFactory;

	public AppDeployerReleaseManager(ReleaseRepository releaseRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			DeployerRepository deployerRepository,
			ReleaseAnalyzer releaseAnalyzer,
			AppDeploymentRequestFactory appDeploymentRequestFactory) {
		this.releaseRepository = releaseRepository;
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.deployerRepository = deployerRepository;
		this.releaseAnalyzer = releaseAnalyzer;
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
			AppDeploymentRequest appDeploymentRequest = this.appDeploymentRequestFactory.createAppDeploymentRequest(
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
		ReleaseAnalysisReport releaseAnalysisReport = this.releaseAnalyzer.analyze(existingRelease, replacingRelease);
		AppDeployerData existingAppDeployerData = this.appDeployerDataRepository.findByReleaseNameAndReleaseVersion(
				existingRelease.getName(), existingRelease.getVersion());
		Map<String, String> existingAppNamesAndDeploymentIds = existingAppDeployerData.getDeploymentDataAsMap();
		List<String> applicationNamesToUpgrade = releaseAnalysisReport.getApplicationNamesToUpgrade();
		List<AppStatus> appStatuses = status(existingRelease).getInfo().getStatus().getAppStatusList();
		Map<String, String> appsCount = new HashMap<>();
		for (Map.Entry<String, String> existingEntry : existingAppNamesAndDeploymentIds.entrySet()) {
			String existingName = existingEntry.getKey();
			if (applicationNamesToUpgrade.contains(existingName)) {
				String deploymentId = existingAppNamesAndDeploymentIds.get(existingName);
				for (AppStatus appStatus : appStatuses) {
					if (appStatus.getDeploymentId().equals(deploymentId)) {
						appsCount.put(existingName, String.valueOf(appStatus.getInstances().size()));
					}
				}
			}
		}
		Map<String, Object> model = ConfigValueUtils.mergeConfigValues(replacingRelease.getPkg(),
				replacingRelease.getConfigValues());
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			if (appsCount.keySet().contains(entry.getKey())) {
				Map<String, Object> modelValue = (Map<String, Object>) model.getOrDefault(entry.getKey(), new TreeMap<String, Object>());
				Map<String, Object> specMap = (Map<String, Object>) modelValue.getOrDefault(SpringBootAppKind.SPEC_STRING,
						new TreeMap<String, Object>());
				Map<String, Object> deploymentPropertiesMap = (Map<String, Object>) specMap
						.getOrDefault(SpringBootAppSpec.DEPLOYMENT_PROPERTIES_STRING, new TreeMap<String, Object>());
				deploymentPropertiesMap.put(SPRING_CLOUD_DEPLOYER_COUNT, appsCount.get(entry.getKey()));
			}
		}
		String manifest = ManifestUtils.createManifest(replacingRelease.getPkg(), model);
		replacingRelease.setManifest(manifest);
		Release release = this.releaseRepository.save(replacingRelease);
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
		logger.debug("Getting status for {} using deploymentIds {}", release,
				StringUtils.collectionToCommaDelimitedString(deploymentIds));

		if (!deploymentIds.isEmpty()) {
			// mainly track deployed and unknown statuses. for any other
			// combination, get more details from instances.
			int deployedCount = 0;
			int unknownCount = 0;
			List<AppStatus> appStatusList = new ArrayList<>();
			for (String deploymentId : deploymentIds) {
				AppStatus appStatus = appDeployer.status(deploymentId);
				logger.debug("App Deployer for deploymentId {} gives status {}", deploymentId, appStatus);
				appStatusList.add(appStatus);

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
			release.getInfo().getStatus().setPlatformStatusAsAppStatusList(appStatusList);
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
