/*
 * Copyright 2018 the original author or authors.
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

import java.util.List;

import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;

/**
 * Responsible for deleting the provided list of applications and updating the status of
 * the release.
 * @author Mark Pollack
 */
public class CloudFoundryDeleteStep {

	private final Logger logger = LoggerFactory.getLogger(CloudFoundryDeleteStep.class);

	private final ReleaseRepository releaseRepository;

	private final PlatformCloudFoundryOperations platformCloudFoundryOperations;

	public CloudFoundryDeleteStep(ReleaseRepository releaseRepository,
			PlatformCloudFoundryOperations platformCloudFoundryOperations) {
		this.releaseRepository = releaseRepository;
		this.platformCloudFoundryOperations = platformCloudFoundryOperations;
	}

	public Release delete(Release release, AppDeployerData existingAppDeployerData,
			List<String> applicationNamesToDelete) {
		ApplicationManifest applicationManifest = CloudFoundryApplicationManifestUtils.updateApplicationName(release);
		String applicationName = applicationManifest.getName();
		DeleteApplicationRequest deleteApplicationRequest = DeleteApplicationRequest.builder().name(applicationName)
				.build();
		this.platformCloudFoundryOperations.getCloudFoundryOperations(release.getPlatformName()).applications()
				.delete(deleteApplicationRequest)
				.doOnSuccess(v -> logger.info("Successfully undeployed app {}", applicationName))
				.doOnError(e -> logger.error("Failed to undeploy app %s", applicationName)).block();

		Status deletedStatus = new Status();
		deletedStatus.setStatusCode(StatusCode.DELETED);
		release.getInfo().setStatus(deletedStatus);
		release.getInfo().setDescription("Delete complete");
		this.releaseRepository.save(release);
		return release;
	}
}
