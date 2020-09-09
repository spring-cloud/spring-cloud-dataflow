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
package org.springframework.cloud.skipper.server.deployer.strategies;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.cloud.skipper.server.repository.map.DeployerRepository;

/**
 * Responsible for deleting the provided list of applications and updating the status of
 * the release.
 * @author Mark Pollack
 */
public class DeleteStep {

	private final Logger logger = LoggerFactory.getLogger(DeleteStep.class);

	private final ReleaseRepository releaseRepository;

	private final DeployerRepository deployerRepository;

	public DeleteStep(ReleaseRepository releaseRepository, DeployerRepository deployerRepository) {
		this.releaseRepository = releaseRepository;
		this.deployerRepository = deployerRepository;
	}

	public Release delete(Release release, AppDeployerData existingAppDeployerData,
			List<String> applicationNamesToDelete, boolean setStatus) {
		AppDeployer appDeployer = this.deployerRepository.findByNameRequired(release.getPlatformName())
				.getAppDeployer();

		Map<String, String> appNamesAndDeploymentIds = (existingAppDeployerData!= null) ?
				existingAppDeployerData.getDeploymentDataAsMap() : Collections.emptyMap();

		for (Map.Entry<String, String> appNameAndDeploymentId : appNamesAndDeploymentIds.entrySet()) {
			if (applicationNamesToDelete.contains(appNameAndDeploymentId.getKey())) {
				logger.debug("For Release name {}, undeploying existing app {}", release.getName(),
						appNameAndDeploymentId.getKey());
				// simply attempt to undeploy and let caller stack to handle errors if any
				appDeployer.undeploy(appNameAndDeploymentId.getValue());
			}
		}

		if (setStatus) {
			Status deletedStatus = new Status();
			deletedStatus.setStatusCode(StatusCode.DELETED);
			release.getInfo().setStatus(deletedStatus);
			release.getInfo().setDescription("Delete complete");
			this.releaseRepository.save(release);
		}
		return release;
	}
}
