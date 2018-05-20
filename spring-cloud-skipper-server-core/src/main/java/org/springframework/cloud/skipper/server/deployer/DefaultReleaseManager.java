/*
 * Copyright 2017-2018 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.app.MultiStateAppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.deployer.cloudfoundry.PlatformCloudFoundryOperations;
import org.springframework.cloud.skipper.domain.CFApplicationManifestReader;
import org.springframework.cloud.skipper.domain.Manifest;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifest;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifestReader;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationSpec;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.cloud.skipper.server.util.ArgumentSanitizer;
import org.springframework.cloud.skipper.server.util.ConfigValueUtils;
import org.springframework.cloud.skipper.server.util.ManifestUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.skipper.server.deployer.CFManifestApplicationDeployer.PUSH_REQUEST_TIMEOUT;
import static org.springframework.cloud.skipper.server.deployer.CFManifestApplicationDeployer.STAGING_TIMEOUT;
import static org.springframework.cloud.skipper.server.deployer.CFManifestApplicationDeployer.STARTUP_TIMEOUT;
import static org.springframework.cloud.skipper.server.deployer.CFManifestApplicationDeployer.isNotFoundError;

/**
 * A ReleaseManager implementation that uses an AppDeployer and CF manifest based deployer.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings({ "unchecked", "deprecation" })
public class DefaultReleaseManager implements ReleaseManager {

	public static final String SPRING_CLOUD_DEPLOYER_COUNT = "spring.cloud.deployer.count";

	private static final Logger logger = LoggerFactory.getLogger(DefaultReleaseManager.class);

	private final ReleaseRepository releaseRepository;

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final DeployerRepository deployerRepository;

	private final ReleaseAnalyzer releaseAnalyzer;

	private final AppDeploymentRequestFactory appDeploymentRequestFactory;

	private final SpringCloudDeployerApplicationManifestReader applicationManifestReader;

	private final CFApplicationManifestReader cfApplicationManifestReader;

	private final PlatformCloudFoundryOperations platformCloudFoundryOperations;

	private final CFManifestApplicationDeployer cfManifestApplicationDeployer;

	public DefaultReleaseManager(ReleaseRepository releaseRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			DeployerRepository deployerRepository,
			ReleaseAnalyzer releaseAnalyzer,
			AppDeploymentRequestFactory appDeploymentRequestFactory,
			SpringCloudDeployerApplicationManifestReader applicationManifestReader,
			CFApplicationManifestReader cfApplicationManifestReader,
			PlatformCloudFoundryOperations platformCloudFoundryOperations,
			CFManifestApplicationDeployer cfManifestApplicationDeployer) {
		this.releaseRepository = releaseRepository;
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.deployerRepository = deployerRepository;
		this.releaseAnalyzer = releaseAnalyzer;
		this.appDeploymentRequestFactory = appDeploymentRequestFactory;
		this.applicationManifestReader = applicationManifestReader;
		this.cfApplicationManifestReader = cfApplicationManifestReader;
		this.platformCloudFoundryOperations = platformCloudFoundryOperations;
		this.cfManifestApplicationDeployer = cfManifestApplicationDeployer;
	}

	public Release install(Release newRelease) {
		if (this.applicationManifestReader.canSupport(newRelease.getManifest().getData())) {
			Release release = this.releaseRepository.save(newRelease);
			logger.debug("Manifest = " + ArgumentSanitizer.sanitizeYml(newRelease.getManifest().getData()));
			// Deploy the application
			List<? extends SpringCloudDeployerApplicationManifest> applicationSpecList = this.applicationManifestReader
					.read(release.getManifest().getData());
			AppDeployer appDeployer = this.deployerRepository.findByNameRequired(release.getPlatformName())
					.getAppDeployer();
			Map<String, String> appNameDeploymentIdMap = new HashMap<>();
			for (SpringCloudDeployerApplicationManifest springCloudDeployerApplicationManifest : applicationSpecList) {
				AppDeploymentRequest appDeploymentRequest = this.appDeploymentRequestFactory.createAppDeploymentRequest(
						springCloudDeployerApplicationManifest,
						release.getName(),
						String.valueOf(release.getVersion()));
				try {
					String deploymentId = appDeployer.deploy(appDeploymentRequest);
					appNameDeploymentIdMap
							.put(springCloudDeployerApplicationManifest.getApplicationName(), deploymentId);
				}
				catch (Exception e) {
					// Update Status in DB
					Status status = new Status();
					status.setStatusCode(StatusCode.FAILED);
					release.getInfo().setStatus(status);
					release.getInfo().setDescription("Install failed");
					throw new SkipperException(String.format("Could not install AppDeployRequest [%s] " +
									" to platform [%s].  Error Message = [%s]",
							appDeploymentRequest.toString(),
							release.getPlatformName(),
							e.getMessage()), e);
				}
			}

			saveAppDeployerData(release, appNameDeploymentIdMap);

			// Update Status in DB
			updateInstallComplete(release);

			// Store updated state in in DB and compute status
			return status(this.releaseRepository.save(release));
		}
		else if (this.cfApplicationManifestReader.canSupport(newRelease.getManifest().getData())) {
			Release release = this.releaseRepository.save(newRelease);
			ApplicationManifest applicationManifest = this.cfManifestApplicationDeployer.getCFApplicationManifest(release);
			Assert.isTrue(applicationManifest != null, "CF Application Manifest must be set");
			logger.debug("Manifest = " + ArgumentSanitizer.sanitizeYml(newRelease.getManifest().getData()));
			// Deploy the application
			String applicationName = applicationManifest.getName();
			Map<String, String> appDeploymentData = new HashMap<>();
			appDeploymentData.put(applicationManifest.getName(), applicationManifest.toString());
			this.platformCloudFoundryOperations.getCloudFoundryOperations(newRelease.getPlatformName())
					.applications().pushManifest(
					PushApplicationManifestRequest.builder()
							.manifest(applicationManifest)
							.stagingTimeout(STAGING_TIMEOUT)
							.startupTimeout(STARTUP_TIMEOUT)
							.build())
					.doOnSuccess(v -> logger.info("Done uploading bits for {}", applicationName))
					.doOnError(e -> logger.error(
							String.format("Error creating app %s.  Exception Message %s", applicationName,
									e.getMessage())))
					.timeout(PUSH_REQUEST_TIMEOUT)
					.doOnSuccess(item -> {
						logger.info("Successfully deployed {}", applicationName);
						saveAppDeployerData(release, appDeploymentData);

						// Update Status in DB
						updateInstallComplete(release);
					})
					.doOnError(error -> {
						if (isNotFoundError().test(error)) {
							logger.warn("Unable to deploy application. It may have been destroyed before start completed: " + error.getMessage());
						}
						else {
							logger.error(String.format("Failed to deploy %s", applicationName));
						}
					})
					.block();
			// Store updated state in in DB and compute status
			return status(this.releaseRepository.save(release));
		}
		else {
			throw new SkipperException("Release Manager not available for the given release manifest");
		}
	}

	private void updateInstallComplete(Release release) {
		Status status = new Status();
		status.setStatusCode(StatusCode.DEPLOYED);
		release.getInfo().setStatus(status);
		release.getInfo().setDescription("Install complete");
	}

	private void saveAppDeployerData(Release release, Map<String, String> appNameDeploymentIdMap) {
		AppDeployerData appDeployerData = new AppDeployerData();
		appDeployerData.setReleaseName(release.getName());
		appDeployerData.setReleaseVersion(release.getVersion());
		appDeployerData.setDeploymentDataUsingMap(appNameDeploymentIdMap);
		this.appDeployerDataRepository.save(appDeployerData);
	}

	@Override
	public ReleaseAnalysisReport createReport(Release existingRelease, Release replacingRelease, boolean initial) {
		if (this.applicationManifestReader.canSupport(replacingRelease.getManifest().getData())) {
			ReleaseAnalysisReport releaseAnalysisReport = this.releaseAnalyzer
					.analyze(existingRelease, replacingRelease);
			if (releaseAnalysisReport.getReleaseDifference().areEqual()) {
				throw new SkipperException(
						"Package to upgrade has no difference than existing deployed/deleted package. Not upgrading.");
			}
			AppDeployerData existingAppDeployerData = this.appDeployerDataRepository
					.findByReleaseNameAndReleaseVersionRequired(
							existingRelease.getName(), existingRelease.getVersion());
			Map<String, String> existingAppNamesAndDeploymentIds = existingAppDeployerData.getDeploymentDataAsMap();
			List<String> applicationNamesToUpgrade = releaseAnalysisReport.getApplicationNamesToUpgrade();
			List<AppStatus> appStatuses = status(existingRelease).getInfo().getStatus().getAppStatusList();

			Map<String, Object> model = calculateAppCountsForRelease(replacingRelease, existingAppNamesAndDeploymentIds,
					applicationNamesToUpgrade, appStatuses);

			String manifestData = ManifestUtils.createManifest(replacingRelease.getPkg(), model);
			Manifest manifest = new Manifest();
			manifest.setData(manifestData);
			replacingRelease.setManifest(manifest);
			if (initial) {
				this.releaseRepository.save(replacingRelease);
			}
			return releaseAnalysisReport;
		}
		else if (this.cfApplicationManifestReader.canSupport(replacingRelease.getManifest().getData())) {
			ReleaseAnalysisReport releaseAnalysisReport = this.releaseAnalyzer
					.analyze(existingRelease, replacingRelease);
			ApplicationManifest applicationManifest = this.cfManifestApplicationDeployer
					.getCFApplicationManifest(replacingRelease);
			Map<String, ?> configValues = CFApplicationManifestUtils.getCFManifestMap(applicationManifest);
			String manifestData = ManifestUtils.createManifest(replacingRelease.getPkg(), configValues);
			logger.debug("Replacing Release Manifest = " + ArgumentSanitizer.sanitizeYml(manifestData));
			Manifest manifest = new Manifest();
			manifest.setData(manifestData);
			replacingRelease.setManifest(manifest);
			if (initial) {
				this.releaseRepository.save(replacingRelease);
			}
			return new ReleaseAnalysisReport(releaseAnalysisReport.getApplicationNamesToUpgrade(),
					releaseAnalysisReport.getReleaseDifference(), existingRelease, replacingRelease);
		}
		else {
			throw new SkipperException("Release Manager not available for the given release manifest");
		}
	}

	private Map<String, Object> calculateAppCountsForRelease(Release replacingRelease,
			Map<String, String> existingAppNamesAndDeploymentIds, List<String> applicationNamesToUpgrade,
			List<AppStatus> appStatuses) {
		Map<String, Object> model = ConfigValueUtils.mergeConfigValues(replacingRelease.getPkg(),
				replacingRelease.getConfigValues());
		for (Map.Entry<String, String> existingEntry : existingAppNamesAndDeploymentIds.entrySet()) {
			String existingName = existingEntry.getKey();
			if (applicationNamesToUpgrade.contains(existingName)) {
				String deploymentId = existingAppNamesAndDeploymentIds.get(existingName);
				for (AppStatus appStatus : appStatuses) {
					if (appStatus.getDeploymentId().equals(deploymentId)) {
						String appsCount = String.valueOf(appStatus.getInstances().size());
						if (replacingRelease.getPkg().getDependencies().isEmpty()) {
							updateCountProperty(model, appsCount);
						}
						else {
							for (Map.Entry<String, Object> entry : model.entrySet()) {
								if (existingName.contains(entry.getKey())) {
									Map<String, Object> appModel = (Map<String, Object>) model.getOrDefault(
											entry.getKey(),
											new TreeMap<String, Object>());
									updateCountProperty(appModel, appsCount);
								}
							}
						}
					}
				}
			}
		}

		return model;
	}

	private void updateCountProperty(Map<String, Object> model, String appsCount) {
		Map<String, Object> specMap = (Map<String, Object>) model.getOrDefault(
				SpringCloudDeployerApplicationManifest.SPEC_STRING,
				new TreeMap<String, Object>());
		Map<String, Object> deploymentPropertiesMap = (Map<String, Object>) specMap
				.get(SpringCloudDeployerApplicationSpec.DEPLOYMENT_PROPERTIES_STRING);
		// explicit null check instead of getOrDefault is required as deploymentProperties could
		// have been explicitly
		// set to null.
		if (deploymentPropertiesMap == null) {
			deploymentPropertiesMap = new TreeMap<String, Object>();
		}
		deploymentPropertiesMap.put(SPRING_CLOUD_DEPLOYER_COUNT, appsCount);
	}

	public Release status(Release release) {
		if (this.applicationManifestReader.canSupport(release.getManifest().getData())) {
			AppDeployer appDeployer = this.deployerRepository.findByNameRequired(release.getPlatformName())
					.getAppDeployer();
			AppDeployerData appDeployerData = this.appDeployerDataRepository
					.findByReleaseNameAndReleaseVersion(release.getName(), release.getVersion());
			if (appDeployerData == null) {
				logger.warn(String.format("Could not get status for release %s-v%s.  No app deployer data found.",
						release.getName(), release.getVersion()));
				return release;
			}
			List<String> deploymentIds = appDeployerData.getDeploymentIds();
			logger.debug("Getting status for {} using deploymentIds {}", release,
					StringUtils.collectionToCommaDelimitedString(deploymentIds));

			if (!deploymentIds.isEmpty()) {
				// mainly track deployed and unknown statuses. for any other
				// combination, get more details from instances.
				int deployedCount = 0;
				int unknownCount = 0;
				Map<String, DeploymentState> deploymentStateMap = new HashMap<>();
				if (appDeployer instanceof MultiStateAppDeployer) {
					MultiStateAppDeployer multiStateAppDeployer = (MultiStateAppDeployer) appDeployer;
					deploymentStateMap = multiStateAppDeployer.states(StringUtils.toStringArray(deploymentIds));
				}
				List<AppStatus> appStatusList = new ArrayList<>();
				for (String deploymentId : deploymentIds) {
					AppStatus appStatus = appDeployer.status(deploymentId);

					if (appStatus.getState().equals(DeploymentState.failed) ||
							appStatus.getState().equals(DeploymentState.error)) {
						// check if we have 'early' status computed via multiStateAppDeployer
						if (deploymentStateMap.containsKey(deploymentId)) {
							appStatus = AppStatus.of(deploymentId).generalState(deploymentStateMap.get(deploymentId))
									.build();
						}
					}
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
		}
		else if (this.cfApplicationManifestReader.canSupport(release.getManifest().getData())) {
			release.getInfo().getStatus().setPlatformStatusAsAppStatusList(
					Collections.singletonList(this.cfManifestApplicationDeployer.status(release)));
		}
		else {
			throw new SkipperException("Release Manager not available for the given release manifest");
		}
		return release;
	}

	public Release delete(Release release) {
		if (this.applicationManifestReader.canSupport(release.getManifest().getData())) {
			AppDeployer appDeployer = this.deployerRepository.findByNameRequired(release.getPlatformName())
					.getAppDeployer();

			AppDeployerData appDeployerData = this.appDeployerDataRepository
					.findByReleaseNameAndReleaseVersionRequired(release.getName(), release.getVersion());
			List<String> deploymentIds = appDeployerData.getDeploymentIds();
			if (!deploymentIds.isEmpty()) {
				for (String deploymentId : deploymentIds) {

					// don't error trying trying to undeploy something
					// which is not deployed
					AppStatus appStatus = appDeployer.status(deploymentId);
					if (appStatus.getState().equals(DeploymentState.deployed)) {
						appDeployer.undeploy(deploymentId);
					}
					else {
						logger.warn("For Release name {}, did not undeploy existing app {} as its status is not "
								+ "'deployed'.", release.getName(), deploymentId);
					}
				}
				Status deletedStatus = new Status();
				deletedStatus.setStatusCode(StatusCode.DELETED);
				release.getInfo().setStatus(deletedStatus);
				release.getInfo().setDescription("Delete complete");
				this.releaseRepository.save(release);
			}
		}
		else if (this.cfApplicationManifestReader.canSupport(release.getManifest().getData())) {
			this.releaseRepository.save(this.cfManifestApplicationDeployer.delete(release));
		}
		else {
			throw new SkipperException("Release Manager not available for the given release manifest");
		}
		return release;
	}

}
