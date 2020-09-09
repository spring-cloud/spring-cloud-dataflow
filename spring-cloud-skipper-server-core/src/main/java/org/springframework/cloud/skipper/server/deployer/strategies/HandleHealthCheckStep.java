/*
 * Copyright 2017-2018 the original author or authors.
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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.jpa.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsible for taking action based on the health of the latest deployed release. If
 * healthy, then delete applications from the previous release. Otherwise delete the
 * latest deployed release. Delegates to {@link DeleteStep}.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class HandleHealthCheckStep {

	private final Logger logger = LoggerFactory.getLogger(HandleHealthCheckStep.class);

	private final ReleaseRepository releaseRepository;

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final DeleteStep deleteStep;


	public HandleHealthCheckStep(ReleaseRepository releaseRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			DeleteStep deleteStep) {
		this.releaseRepository = releaseRepository;
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.deleteStep = deleteStep;
	}

	@Transactional
	public void handleHealthCheck(boolean healthy, Release existingRelease,
			List<String> applicationNamesToUpgrade,
			Release replacingRelease, Long timeout, boolean cancel, boolean rollback) {
		if (healthy) {
			updateReplacingReleaseState(replacingRelease, rollback);
			deleteExistingRelease(existingRelease, applicationNamesToUpgrade);
		}
		else {
			deleteReplacingRelease(replacingRelease, timeout, cancel, applicationNamesToUpgrade);
		}
	}

	private void updateReplacingReleaseState(Release replacingRelease, boolean rollback) {
		// Update Status in DB
		Status status = new Status();
		status.setStatusCode(StatusCode.DEPLOYED);
		replacingRelease.getInfo().setStatus(status);
		replacingRelease.getInfo().setDescription(rollback ? "Rollback complete" : "Upgrade complete");
		this.releaseRepository.save(replacingRelease);
		logger.info("Release {}-v{} has been DEPLOYED", replacingRelease.getName(),
				replacingRelease.getVersion());
		logger.info("Apps in release {}-v{} are healthy.", replacingRelease.getName(),
				replacingRelease.getVersion());
	}

	private void deleteReplacingRelease(Release replacingRelease, Long timeout, boolean cancel, List<String> applicationNamesToUpgrade) {
		try {
			if (!cancel) {
				logger.error("New release " + replacingRelease.getName() + " was not detected as healthy after " + timeout
						+ " milliseconds.  " + "Keeping existing release, and Deleting apps of replacing release");
			}

			// Delete only changed app and do not set status with delete step as it's done here
			AppDeployerData existingAppDeployerData = this.appDeployerDataRepository
					.findByReleaseNameAndReleaseVersionRequired(
						replacingRelease.getName(), replacingRelease.getVersion());
			logger.info("Deleting changed applications from replacing release {}-v{}",
				replacingRelease.getName(),
				replacingRelease.getVersion());
			this.deleteStep.delete(replacingRelease, existingAppDeployerData, applicationNamesToUpgrade, false);

			Status status = new Status();
			status.setStatusCode(StatusCode.FAILED);
			replacingRelease.getInfo().setStatus(status);
			String desc = cancel ? "Cancelled after " + timeout + " ms."
					: "Did not detect apps in replacing release as healthy after " + timeout + " ms.";
			replacingRelease.getInfo()
					.setDescription(desc);
			this.releaseRepository.save(replacingRelease);
		}
		catch (DataAccessException e) {
			logger.debug("Error1 deleteReplacingRelease {}", e);
			throw e;
		}
		catch (Exception e) {
			logger.debug("Error2 deleteReplacingRelease {}", e);
			// Update Status in DB
			Status status = new Status();
			status.setStatusCode(StatusCode.FAILED);
			replacingRelease.getInfo().setStatus(status);
			replacingRelease.getInfo().setDescription("Could not delete replacing release application, " +
					"Manual intervention needed.  Sorry it didn't work out.");
			this.releaseRepository.save(replacingRelease);
			logger.info("Release {}-v{} could not be deleted.", replacingRelease.getName(),
					replacingRelease.getVersion());
		}
	}

	private void deleteExistingRelease(Release existingRelease, List<String> applicationNamesToUpgrade) {
		try {
			AppDeployerData existingAppDeployerData = this.appDeployerDataRepository
					.findByReleaseNameAndReleaseVersionRequired(
							existingRelease.getName(), existingRelease.getVersion());
			logger.info("Deleting changed applications from existing release {}-v{}",
					existingRelease.getName(),
					existingRelease.getVersion());
			this.deleteStep.delete(existingRelease, existingAppDeployerData, applicationNamesToUpgrade, true);
		}
		catch (DataAccessException e) {
			logger.debug("Error1 deleteExistingRelease {}", e);
			throw e;
		}
		catch (Exception e) {
			logger.debug("Error2 deleteExistingRelease {}", e);
			// Update Status in DB
			Status status = new Status();
			status.setStatusCode(StatusCode.FAILED);
			existingRelease.getInfo().setStatus(status);
			existingRelease.getInfo().setDescription("Could not delete existing application, " +
					"manual intervention needed.  Sorry it didn't work out.");
			this.releaseRepository.save(existingRelease);
			logger.info("Release {}-v{} could not be deleted.", existingRelease.getName(),
					existingRelease.getVersion());
		}
	}
}
