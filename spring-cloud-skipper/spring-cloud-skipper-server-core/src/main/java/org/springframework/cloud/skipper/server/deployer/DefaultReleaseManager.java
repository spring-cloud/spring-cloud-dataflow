/*
 * Copyright 2017-2020 the original author or authors.
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
package org.springframework.cloud.skipper.server.deployer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppScaleRequest;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.app.MultiStateAppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.ReleaseUpgradeException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.LogInfo;
import org.springframework.cloud.skipper.domain.Manifest;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.ScaleRequest;
import org.springframework.cloud.skipper.domain.ScaleRequest.ScaleRequestItem;
import org.springframework.cloud.skipper.domain.SkipperManifestKind;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifest;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifestReader;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationSpec;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.jpa.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.cloud.skipper.server.repository.map.DeployerRepository;
import org.springframework.cloud.skipper.server.util.ArgumentSanitizer;
import org.springframework.cloud.skipper.server.util.ConfigValueUtils;
import org.springframework.cloud.skipper.server.util.ManifestUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A ReleaseManager implementation that uses an AppDeployer and CF manifest based deployer.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings({ "unchecked" })
public class DefaultReleaseManager implements ReleaseManager {

	public static final String SPRING_CLOUD_DEPLOYER_COUNT = "spring.cloud.deployer.count";

	private static final Logger logger = LoggerFactory.getLogger(DefaultReleaseManager.class);
	public static final String SKIPPER_APPLICATION_NAME_ATTRIBUTE = "skipper.application.name";
	public static final String SKIPPER_RELEASE_NAME_ATTRIBUTE = "skipper.release.name";
	public static final String SKIPPER_RELEASE_VERSION_ATTRIBUTE = "skipper.release.version";

	private final ReleaseRepository releaseRepository;

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final DeployerRepository deployerRepository;

	private final ReleaseAnalyzer releaseAnalyzer;

	private final AppDeploymentRequestFactory appDeploymentRequestFactory;

	private final SpringCloudDeployerApplicationManifestReader applicationManifestReader;

	private final LoadingCache<CacheKey, Mono<Map<String, DeploymentState>>> cache = Caffeine.newBuilder()
			.expireAfterWrite(60, TimeUnit.SECONDS)
			.build(k -> {
				logger.debug("Building new deploymentStateMap mono");
				if (k.getAppDeployer() instanceof MultiStateAppDeployer) {
					MultiStateAppDeployer multiAppDeployer = (MultiStateAppDeployer) k.getAppDeployer();
					return multiAppDeployer.statesReactive(k.getDeploymentIds().toArray(new String[0])).cache();
				}
				// it wasn't MultiStateAppDeployer so we just return empty
				return Mono.just(new HashMap<>());
			});


	public DefaultReleaseManager(ReleaseRepository releaseRepository,
			AppDeployerDataRepository appDeployerDataRepository, DeployerRepository deployerRepository,
			ReleaseAnalyzer releaseAnalyzer, AppDeploymentRequestFactory appDeploymentRequestFactory,
			SpringCloudDeployerApplicationManifestReader applicationManifestReader) {
		this.releaseRepository = releaseRepository;
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.deployerRepository = deployerRepository;
		this.releaseAnalyzer = releaseAnalyzer;
		this.appDeploymentRequestFactory = appDeploymentRequestFactory;
		this.applicationManifestReader = applicationManifestReader;
	}

	@Override
	public Collection<String> getSupportedKinds() {
		return Arrays.asList(SkipperManifestKind.SpringBootApp.name(),
				SkipperManifestKind.SpringCloudDeployerApplication.name());
	}

	public Release install(Release newRelease) {
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
				String applicationName = springCloudDeployerApplicationManifest.getApplicationName();
				appNameDeploymentIdMap.put(applicationName, deploymentId);
			} catch (Exception e) {
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

	private void updateInstallComplete(Release release) {
		Status status = new Status();
		status.setStatusCode(StatusCode.DEPLOYED);
		release.getInfo().setStatus(status);
		release.getInfo().setDescription("Install complete");
	}

	private void saveAppDeployerData(Release release, Map<String, String> appNameDeploymentIdMap) {
		logger.debug("saveAppDeployerData:{}:{}", release.getName(), appNameDeploymentIdMap);
		AppDeployerData appDeployerData = new AppDeployerData();
		appDeployerData.setReleaseName(release.getName());
		appDeployerData.setReleaseVersion(release.getVersion());
		appDeployerData.setDeploymentDataUsingMap(appNameDeploymentIdMap);
		this.appDeployerDataRepository.save(appDeployerData);
	}

	@Override
	public ReleaseAnalysisReport createReport(Release existingRelease, Release replacingRelease, boolean initial,
			boolean isForceUpdate, List<String> appNamesToUpgrade) {
		ReleaseAnalysisReport releaseAnalysisReport = this.releaseAnalyzer
				.analyze(existingRelease, replacingRelease, isForceUpdate, appNamesToUpgrade);
		List<String> applicationNamesToUpgrade = releaseAnalysisReport.getApplicationNamesToUpgrade();
		if (releaseAnalysisReport.getReleaseDifference().areEqual() && !isForceUpdate) {
			throw new ReleaseUpgradeException("Package to upgrade has no difference than existing deployed/deleted package. Not upgrading.");
		}
		AppDeployerData existingAppDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersionRequired(
						existingRelease.getName(), existingRelease.getVersion());
		Map<String, String> existingAppNamesAndDeploymentIds = (existingAppDeployerData != null) ?
				existingAppDeployerData.getDeploymentDataAsMap() : Collections.EMPTY_MAP;
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

	public Mono<Map<String, Map<String, DeploymentState>>> deploymentState(List<Release> releases) {
		return Mono.defer(() -> {
				Map<AppDeployer, List<String>> appDeployerDeploymentIds = new HashMap<>();
				Map<String, List<String>> releaseDeploymentIds = new HashMap<>();
				for (Release release: releases) {
					List<String> deploymentIds = null;
					if (!release.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) {
						AppDeployer appDeployer = this.deployerRepository.findByNameRequired(release.getPlatformName())
								.getAppDeployer();
						AppDeployerData appDeployerData = this.appDeployerDataRepository
								.findByReleaseNameAndReleaseVersion(release.getName(), release.getVersion());
						if (appDeployerData == null) {
							logger.warn(String.format("Could not get status for release %s-v%s.  No app deployer data found.",
									release.getName(), release.getVersion()));
						}
						else {
							deploymentIds = appDeployerData.getDeploymentIds();
							if (appDeployerDeploymentIds.containsKey(appDeployer)) {
								appDeployerDeploymentIds.get(appDeployer).addAll(deploymentIds);
							}
							else {
								appDeployerDeploymentIds.put(appDeployer, new ArrayList<>(deploymentIds));
							}
							releaseDeploymentIds.put(release.getName(), new ArrayList<>(deploymentIds));
						}
					}
				}
				return Mono.zip(Mono.just(appDeployerDeploymentIds), Mono.just(releaseDeploymentIds));
			})
			.flatMap(t -> {
				Mono<Map<String, DeploymentState>> deploymentStates = Flux.fromIterable(t.getT1().entrySet())
					.flatMap(e -> {
						Mono<Map<String, DeploymentState>> fallback = Flux.fromIterable(e.getValue())
							.flatMap(ee -> e.getKey().statusReactive(ee))
							.collectMap(AppStatus::getDeploymentId, AppStatus::getState);
						Mono<Map<String, DeploymentState>> cachedEntry = cache.get(CacheKey.of(e.getValue(), e.getKey()));
						return cachedEntry.flatMap(m -> m.isEmpty() ? fallback : Mono.just(m));
					})
					.reduce(new HashMap<String, DeploymentState>(), (to, from) -> {
						to.putAll(from);
						return to;
					});
				return Mono.zip(deploymentStates, Mono.just(t.getT2()));
			})
			.map(t -> {
				Map<String, DeploymentState> deploymentIdsMap = t.getT1();
				Map<String, List<String>> releaseDeploymentIds = t.getT2();
				Map<String, Map<String, DeploymentState>> releasesDeploymentStates = new HashMap<>();
				for (Release release: releases) {
					Map<String, DeploymentState> deploymentStates = new HashMap<>();
					if (releaseDeploymentIds.get(release.getName()) != null) {
						for (String deploymentId: releaseDeploymentIds.get(release.getName())) {
							deploymentStates.put(deploymentId, deploymentIdsMap.get(deploymentId));
						}
					}
					releasesDeploymentStates.put(release.getName(), deploymentStates);
				}
				return releasesDeploymentStates;
			});
	}

	public Mono<Release> statusReactive(Release release) {
		return Mono.defer(() -> {
			if (release.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) {
				return Mono.just(release);
			}
			AppDeployer appDeployer = this.deployerRepository.findByNameRequired(release.getPlatformName())
					.getAppDeployer();
			AppDeployerData appDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersion(release.getName(), release.getVersion());
			if (appDeployerData == null) {
				logger.warn(String.format("Could not get status for release %s-v%s.  No app deployer data found.",
					release.getName(), release.getVersion()));
				return Mono.just(release);
			}
			List<String> deploymentIds = appDeployerData.getDeploymentIds();
			logger.info("Getting status for {} using deploymentIds {}", release,
					StringUtils.collectionToCommaDelimitedString(deploymentIds));

			if (!deploymentIds.isEmpty()) {
				Map<String, String> appNameDeploymentIdMap = appDeployerData.getDeploymentDataAsMap();
				return Flux.fromIterable(appNameDeploymentIdMap.entrySet())
					.flatMap(nameDeploymentId -> {
						String deploymentId = nameDeploymentId.getValue();
						return appDeployer.statusReactive(deploymentId);
					})
					.map(appStatus -> copyStatus(appStatus))
					.flatMap(appStatus -> {
						return Mono.zip(Mono.just(appStatus), cache.get(CacheKey.of(deploymentIds, appDeployer)));
					})
					.map(zip -> {
						AppStatus appStatus = zip.getT1();
						Map<String, DeploymentState> deploymentStateMap = zip.getT2();
						Collection<AppInstanceStatus> instanceStatuses = appStatus.getInstances().values();
						for (AppInstanceStatus instanceStatus : instanceStatuses) {
								instanceStatus.getAttributes().put(SKIPPER_APPLICATION_NAME_ATTRIBUTE,
										getAppNameByDeploymentId(appStatus.getDeploymentId(), appNameDeploymentIdMap));
								instanceStatus.getAttributes().put(SKIPPER_RELEASE_NAME_ATTRIBUTE, release.getName());
								instanceStatus.getAttributes().put(SKIPPER_RELEASE_VERSION_ATTRIBUTE,
										"" + release.getVersion());
						}
						if (appStatus.getState().equals(DeploymentState.failed)
								|| appStatus.getState().equals(DeploymentState.error)) {
							// check if we have 'early' status computed via multiStateAppDeployer
							String deploymentId = appStatus.getDeploymentId();
							if (deploymentStateMap.containsKey(deploymentId)) {
								appStatus = AppStatus.of(deploymentId)
										.generalState(deploymentStateMap.get(deploymentId)).build();
							}
						}
						return appStatus;
					})
					.collectList()
					.map(appStatusList -> {
						release.getInfo().getStatus().setPlatformStatusAsAppStatusList(appStatusList);
						return release;
					});
			}
			return Mono.just(release);
		});
	}


	private String getAppNameByDeploymentId(String deploymentId, Map<String, String> appNameDeploymentIdMap) {
		for(Map.Entry<String, String> appNameDeploymentId: appNameDeploymentIdMap.entrySet()) {
			if (deploymentId.equals(appNameDeploymentId.getValue())) {
				return appNameDeploymentId.getKey();
			}
		}
		// If no app name is found, return the deploymentId alone.
		return deploymentId;
	}

	public Release status(Release release) {
		if (release.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) {
			return release;
		}
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
			logger.debug("Used appDeployer {}", appDeployer);
			if (appDeployer instanceof MultiStateAppDeployer multiStateAppDeployer) {
				logger.debug("Calling multiStateAppDeployer states {}", deploymentIds);
				deploymentStateMap = multiStateAppDeployer.states(StringUtils.toStringArray(deploymentIds));
				logger.debug("Calling multiStateAppDeployer states end {}", deploymentIds);
			}
			List<AppStatus> appStatusList = new ArrayList<>();
			// Key = app name, value = deploymentId
			Map<String, String> appNameDeploymentIdMap = appDeployerData.getDeploymentDataAsMap();

			for (Map.Entry<String, String> nameDeploymentId : appNameDeploymentIdMap.entrySet()) {
				String appName = nameDeploymentId.getKey();
				String deploymentId = nameDeploymentId.getValue();
				// Copy the status to allow instance attribute mutation.
				logger.debug("Calling appDeployer status {}", deploymentId);
				AppStatus appStatus = copyStatus(appDeployer.status(deploymentId));
				logger.debug("Calling appDeployer status end {} {}", deploymentId, appStatus.getState());
				Collection<AppInstanceStatus> instanceStatuses = appStatus.getInstances().values();
				for (AppInstanceStatus instanceStatus : instanceStatuses) {
					instanceStatus.getAttributes().put(SKIPPER_APPLICATION_NAME_ATTRIBUTE, appName);
					instanceStatus.getAttributes().put(SKIPPER_RELEASE_NAME_ATTRIBUTE, release.getName());
					instanceStatus.getAttributes().put(SKIPPER_RELEASE_VERSION_ATTRIBUTE, "" + release.getVersion());
				}
				if (appStatus.getState().equals(DeploymentState.failed) ||
						appStatus.getState().equals(DeploymentState.error)) {
					// check if we have 'early' status computed via multiStateAppDeployer
					if (deploymentStateMap.containsKey(deploymentId)) {
						appStatus = AppStatus.of(deploymentId).generalState(deploymentStateMap.get(deploymentId))
								.build();
						logger.debug("Set from deploymentStateMap {} {}", deploymentId, appStatus.getState());
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
		return release;
	}

	@Override
	public LogInfo getLog(Release release) {
		return getLog(release, null);
	}

	@Override
	public LogInfo getLog(Release release, String appName) {
		if (release.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) {
			return new LogInfo(Collections.EMPTY_MAP);
		}
		AppDeployerData appDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersion(release.getName(), release.getVersion());
		if (appDeployerData == null) {
			return new LogInfo(Collections.EMPTY_MAP);
		}
		AppDeployer appDeployer = this.deployerRepository.findByNameRequired(release.getPlatformName())
				.getAppDeployer();
		Map<String, String> logMap = new HashMap<>();
		Map<String, String> appNameDeploymentIdMap = appDeployerData.getDeploymentDataAsMap();
		Map<String, String> logApps = new HashMap<>();
		if (StringUtils.hasText(appName)) {
			for (Map.Entry<String, String> nameDeploymentId : appNameDeploymentIdMap.entrySet()) {
				if (appName.equalsIgnoreCase(nameDeploymentId.getValue())) {
					logApps.put(nameDeploymentId.getKey(), nameDeploymentId.getValue());
				}
			}
		}
		else {
			logApps = appNameDeploymentIdMap;
		}
		for (Map.Entry<String, String> deploymentIdEntry: logApps.entrySet()) {
			logMap.put(deploymentIdEntry.getValue(), appDeployer.getLog(deploymentIdEntry.getValue()));
		}
		logger.debug("getLog:{}:{}:{}", release.getName(), appName, logMap);
		return new LogInfo(logMap);
	}

	public Release scale(Release release, ScaleRequest scaleRequest) {
		if (release.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) {
			return release;
		}
		AppDeployerData appDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersion(release.getName(), release.getVersion());
		if (appDeployerData == null) {
			return release;
		}
		AppDeployer appDeployer = this.deployerRepository.findByNameRequired(release.getPlatformName())
				.getAppDeployer();
		Map<String, String> appNameDeploymentIdMap = appDeployerData.getDeploymentDataAsMap();

		Map<String, ScaleRequestItem> apps = new HashMap<>();

		for (Map.Entry<String, String> nameDeploymentId : appNameDeploymentIdMap.entrySet()) {
			Optional<ScaleRequestItem> requestItem = scaleRequest.getScale().stream()
				.filter(item -> ObjectUtils.nullSafeEquals(nameDeploymentId.getKey(), item.getName()))
				.findFirst();
			if (requestItem.isPresent()) {
				apps.put(nameDeploymentId.getValue(), requestItem.get());
			}
		}

		for (Map.Entry<String, ScaleRequestItem> deploymentIdEntry: apps.entrySet()) {
			ScaleRequestItem item = deploymentIdEntry.getValue();
			AppScaleRequest request = new AppScaleRequest(deploymentIdEntry.getKey(), item.getCount(),
					item.getProperties());
			appDeployer.scale(request);
		}

		return release;
	}

	public Release delete(Release release) {
		AppDeployer appDeployer = this.deployerRepository.findByNameRequired(release.getPlatformName())
				.getAppDeployer();
		AppDeployerData appDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersionRequired(release.getName(), release.getVersion());
		List<String> deploymentIds = (appDeployerData != null) ? appDeployerData.getDeploymentIds() : Collections.EMPTY_LIST;
		logger.debug("DeploymentIds to undeploy {}", deploymentIds);
		if (!deploymentIds.isEmpty()) {
			for (String deploymentId : deploymentIds) {
				try {
					appDeployer.undeploy(deploymentId);
				}
				catch (Exception e) {
					logger.error(String.format("Exception undeploying the application with the deploymentId %s. "
							+ "Exception message: %s", deploymentId, e.getMessage()));
				}
			}
			Status deletedStatus = new Status();
			deletedStatus.setStatusCode(StatusCode.DELETED);
			release.getInfo().setStatus(deletedStatus);
			release.getInfo().setDescription("Delete complete");
			this.releaseRepository.save(release);
		}
		return release;
	}

	public static AppStatus copyStatus(AppStatus appStatus) {
		AppStatus.Builder builder = AppStatus.of(appStatus.getDeploymentId());
		if (CollectionUtils.isEmpty(appStatus.getInstances())) {
			builder.generalState(appStatus.getState());
		}
		else {
			appStatus.getInstances().entrySet().stream().map(Map.Entry::getValue).forEach(e -> builder.with(
					new MutableAttributesAppInstanceStatus(e.getId(), e.getState(), e.getAttributes())));
		}
		return builder.build();
	}

	private static final class MutableAttributesAppInstanceStatus implements AppInstanceStatus {

		private final String id;

		private final DeploymentState state;

		private final Map<String, String> attributes = new TreeMap<>();

		private MutableAttributesAppInstanceStatus(String id, DeploymentState state, Map<String, String> attributes) {
			this.id = id;
			this.state = state;
			this.attributes.putAll(attributes);
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public DeploymentState getState() {
			return this.state;
		}

		@Override
		public Map<String, String> getAttributes() {
			return this.attributes;
		}
	}

	/**
	 * Used as a cache key with Caffeine and actual key is a 'key'. Deployement ids
	 * and app deployer is just used with this key class so that cache can build
	 * entry as it need those two to create cf operation.
	 */
	private static class CacheKey {
		private final List<String> deploymentIds;
		private final AppDeployer appDeployer;
		private final String key;

		CacheKey(List<String> deploymentIds, AppDeployer appDeployer) {
			Assert.notNull(deploymentIds, "'deploymentIds' cannot be null");
			Assert.notNull(appDeployer, "'appDeployer' cannot be null");
			this.deploymentIds = deploymentIds;
			this.appDeployer = appDeployer;
			List<String> keyList = new ArrayList<>(deploymentIds);
			Collections.sort(keyList);
			this.key = StringUtils.collectionToCommaDelimitedString(keyList);
		}

		static CacheKey of(List<String> deploymentIds, AppDeployer appDeployer) {
			return new CacheKey(deploymentIds, appDeployer);
		}

		public List<String> getDeploymentIds() {
			return deploymentIds;
		}

		public AppDeployer getAppDeployer() {
			return appDeployer;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((appDeployer == null) ? 0 : appDeployer.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			CacheKey other = (CacheKey) obj;
			if (appDeployer == null) {
				if (other.appDeployer != null) {
					return false;
				}
			}
			else if (!appDeployer.equals(other.appDeployer)) {
				return false;
			}
			return true;
		}
	}
}
