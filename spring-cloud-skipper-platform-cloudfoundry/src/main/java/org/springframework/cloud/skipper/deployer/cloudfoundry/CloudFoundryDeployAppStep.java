/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.cloud.skipper.deployer.cloudfoundry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.jpa.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.cloud.skipper.server.util.ArgumentSanitizer;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsible for taking the ReleaseAnalysisReport and deploying the apps in the
 * replacing release. Step operates in it's own transaction, catches all exceptions so
 * always commits.
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class CloudFoundryDeployAppStep {

	private static final Logger logger = LoggerFactory.getLogger(CloudFoundryDeployAppStep.class);

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final ReleaseRepository releaseRepository;

	private final PlatformCloudFoundryOperations platformCloudFoundryOperations;

	private final CloudFoundryManifestApplicationDeployer cfManifestApplicationDeployer;

	public CloudFoundryDeployAppStep(AppDeployerDataRepository appDeployerDataRepository,
			ReleaseRepository releaseRepository, PlatformCloudFoundryOperations platformCloudFoundryOperations,
			CloudFoundryManifestApplicationDeployer cfManifestApplicationDeployer) {
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.releaseRepository = releaseRepository;
		this.platformCloudFoundryOperations = platformCloudFoundryOperations;
		this.cfManifestApplicationDeployer = cfManifestApplicationDeployer;
	}

	@Transactional
	public List<String> deployApps(Release existingRelease, Release replacingRelease,
			ReleaseAnalysisReport releaseAnalysisReport) {
		List<String> applicationNamesToUpgrade = new ArrayList<>();
		try {
			applicationNamesToUpgrade = releaseAnalysisReport.getApplicationNamesToUpgrade();
			deployCFApp(replacingRelease);
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
						.stagingTimeout(CloudFoundryManifestApplicationDeployer.STAGING_TIMEOUT)
						.startupTimeout(CloudFoundryManifestApplicationDeployer.STARTUP_TIMEOUT)
						.build())
				.doOnSuccess(v -> logger.info("Done uploading bits for {}", applicationName))
				.doOnError(e -> logger.error(
						String.format("Error creating app %s.  Exception Message %s", applicationName,
								e.getMessage())))
				.timeout(CloudFoundryManifestApplicationDeployer.PUSH_REQUEST_TIMEOUT)
				.doOnSuccess(item -> {
					logger.info("Successfully deployed {}", applicationName);
					AppDeployerData appDeployerData = new AppDeployerData();
					appDeployerData.setReleaseName(replacingRelease.getName());
					appDeployerData.setReleaseVersion(replacingRelease.getVersion());
					appDeployerData.setDeploymentDataUsingMap(appDeploymentData);
					this.appDeployerDataRepository.save(appDeployerData);
				})
				.doOnError(error -> {
					if (CloudFoundryManifestApplicationDeployer.isNotFoundError().test(error)) {
						logger.warn("Unable to deploy application. It may have been destroyed before start completed: " + error.getMessage());
					}
					else {
						logger.error(String.format("Failed to deploy %s", applicationName + ". " + error.getMessage()));
					}
				})
				.block();
	}
}
