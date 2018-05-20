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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.deployer.cloudfoundry.PlatformCloudFoundryOperations;
import org.springframework.cloud.skipper.domain.CFApplicationManifestReader;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifest;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifestReader;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.deployer.AppDeploymentRequestFactory;
import org.springframework.cloud.skipper.server.deployer.CFManifestApplicationDeployer;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.cloud.skipper.server.util.ArgumentSanitizer;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.cloud.skipper.server.deployer.CFManifestApplicationDeployer.isNotFoundError;

/**
 * Responsible for taking the ReleaseAnalysisReport and deploying the apps in the
 * replacing release. Step operates in it's own transaction, catches all exceptions so
 * always commits.
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class DeployAppStep {

	private static final Logger logger = LoggerFactory.getLogger(DeployAppStep.class);

	private final DeployerRepository deployerRepository;

	private final AppDeploymentRequestFactory appDeploymentRequestFactory;

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final ReleaseRepository releaseRepository;

	private final SpringCloudDeployerApplicationManifestReader applicationManifestReader;

	private final CFApplicationManifestReader cfApplicationManifestReader;

	private final PlatformCloudFoundryOperations platformCloudFoundryOperations;

	private final CFManifestApplicationDeployer cfManifestApplicationDeployer;

	public DeployAppStep(DeployerRepository deployerRepository, AppDeploymentRequestFactory appDeploymentRequestFactory,
			AppDeployerDataRepository appDeployerDataRepository, ReleaseRepository releaseRepository,
			SpringCloudDeployerApplicationManifestReader applicationManifestReader,
			CFApplicationManifestReader cfApplicationManifestReader,
			PlatformCloudFoundryOperations platformCloudFoundryOperations,
			CFManifestApplicationDeployer cfManifestApplicationDeployer) {
		this.deployerRepository = deployerRepository;
		this.appDeploymentRequestFactory = appDeploymentRequestFactory;
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.releaseRepository = releaseRepository;
		this.applicationManifestReader = applicationManifestReader;
		this.cfApplicationManifestReader = cfApplicationManifestReader;
		this.platformCloudFoundryOperations = platformCloudFoundryOperations;
		this.cfManifestApplicationDeployer = cfManifestApplicationDeployer;
	}

	@Transactional
	public List<String> deployApps(Release existingRelease, Release replacingRelease,
			ReleaseAnalysisReport releaseAnalysisReport) {
		List<String> applicationNamesToUpgrade = new ArrayList<>();
		try {
			applicationNamesToUpgrade = releaseAnalysisReport.getApplicationNamesToUpgrade();
			String releaseManifest = replacingRelease.getManifest().getData();
			if (this.applicationManifestReader.canSupport(releaseManifest)) {
				AppDeployer appDeployer = this.deployerRepository.findByNameRequired(replacingRelease.getPlatformName())
												.getAppDeployer();

				// Deploy the application
				Map<String, String> appNameDeploymentIdMap = deploy(replacingRelease, applicationNamesToUpgrade,
						appDeployer);

				// Carry over the applicationDeployment information for apps that were not updated.
				carryOverAppDeploymentIds(existingRelease, appNameDeploymentIdMap);

				AppDeployerData appDeployerData = new AppDeployerData();
				appDeployerData.setReleaseName(replacingRelease.getName());
				appDeployerData.setReleaseVersion(replacingRelease.getVersion());
				appDeployerData.setDeploymentDataUsingMap(appNameDeploymentIdMap);
				this.appDeployerDataRepository.save(appDeployerData);
			}
			else if (this.cfApplicationManifestReader.canSupport(releaseManifest)) {
				deployCFApp(replacingRelease);
			}
		}
		catch (DataAccessException e) {
			throw e;
		}
		catch (Exception e) {
			Status status = new Status();
			status.setStatusCode(StatusCode.FAILED);
			replacingRelease.getInfo().setStatus(status);
			replacingRelease.getInfo().setStatus(status);
			replacingRelease.getInfo().setDescription("Could not deploy app.");
			this.releaseRepository.save(replacingRelease);
		}
		return applicationNamesToUpgrade;
	}

	private void deployCFApp(Release replacingRelease) {
		ApplicationManifest applicationManifest = this.cfManifestApplicationDeployer.getCFApplicationManifest(replacingRelease);
		logger.debug("Manifest = " + ArgumentSanitizer.sanitizeYml(replacingRelease.getManifest().getData()));
		// Deploy the application
		String applicationName = applicationManifest.getName();
		Map<String, String> appDeploymentData = new HashMap<>();
		appDeploymentData.put(applicationManifest.getName(), applicationManifest.toString());
		this.platformCloudFoundryOperations.getCloudFoundryOperations(replacingRelease.getPlatformName())
				.applications().pushManifest(
				PushApplicationManifestRequest.builder()
						.manifest(applicationManifest)
						.stagingTimeout(CFManifestApplicationDeployer.STAGING_TIMEOUT)
						.startupTimeout(CFManifestApplicationDeployer.STARTUP_TIMEOUT)
						.build())
				.doOnSuccess(v -> logger.info("Done uploading bits for {}", applicationName))
				.doOnError(e -> logger.error(
						String.format("Error creating app %s.  Exception Message %s", applicationName,
								e.getMessage())))
				.timeout(CFManifestApplicationDeployer.PUSH_REQUEST_TIMEOUT)
				.doOnSuccess(item -> {
					logger.info("Successfully deployed {}", applicationName);
					AppDeployerData appDeployerData = new AppDeployerData();
					appDeployerData.setReleaseName(replacingRelease.getName());
					appDeployerData.setReleaseVersion(replacingRelease.getVersion());
					appDeployerData.setDeploymentDataUsingMap(appDeploymentData);
					this.appDeployerDataRepository.save(appDeployerData);
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
	}

	private void carryOverAppDeploymentIds(Release existingRelease, Map<String, String> appNameDeploymentIdMap) {
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
	}

	private Map<String, String> deploy(Release replacingRelease, List<String> applicationNamesToUpgrade,
			AppDeployer appDeployer) {
		List<? extends SpringCloudDeployerApplicationManifest> applicationSpecList = this.applicationManifestReader
				.read(replacingRelease
						.getManifest().getData());

		Map<String, String> appNameDeploymentIdMap = new HashMap<>();
		for (SpringCloudDeployerApplicationManifest applicationManifest : applicationSpecList) {
			if (applicationNamesToUpgrade.contains(applicationManifest.getApplicationName())) {
				AppDeploymentRequest appDeploymentRequest = appDeploymentRequestFactory.createAppDeploymentRequest(
						applicationManifest, replacingRelease.getName(),
						String.valueOf(replacingRelease.getVersion()));
				// =============
				// DEPLOY DEPLOY
				// =============
				String deploymentId = appDeployer.deploy(appDeploymentRequest);
				appNameDeploymentIdMap.put(applicationManifest.getApplicationName(), deploymentId);
			}
		}
		return appNameDeploymentIdMap;
	}
}
