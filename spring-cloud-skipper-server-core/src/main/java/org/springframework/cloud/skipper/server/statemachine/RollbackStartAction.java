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
package org.springframework.cloud.skipper.server.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEventHeaders;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.util.Assert;

/**
 * StateMachine {@link Action} preparing a rollback.
 *
 * @author Janne Valkealahti
 *
 */
public class RollbackStartAction extends AbstractAction {

	private static final Logger log = LoggerFactory.getLogger(RollbackStartAction.class);
	private final ReleaseRepository releaseRepository;

	/**
	 * Instantiates a new rollback start action.
	 *
	 * @param releaseRepository the release repository
	 */
	public RollbackStartAction(ReleaseRepository releaseRepository) {
		super();
		this.releaseRepository = releaseRepository;
	}

	@Override
	protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context) {
		log.debug("Starting to execute action");
		// get from event headers and fall back checking if it's in context
		// in case machine died and we restored
		String releaseName = context.getMessageHeaders().get(SkipperEventHeaders.RELEASE_NAME, String.class);
		Integer rollbackVersion = context.getMessageHeaders().get(SkipperEventHeaders.ROLLBACK_VERSION, Integer.class);
		if (releaseName == null) {
			releaseName = context.getExtendedState().get(SkipperEventHeaders.RELEASE_NAME, String.class);
		}
		if (rollbackVersion == null) {
			rollbackVersion = context.getExtendedState().get(SkipperEventHeaders.ROLLBACK_VERSION, Integer.class);
		}

		Assert.notNull(releaseName, "Release name must not be null");
		Assert.isTrue(rollbackVersion >= 0,
				"Rollback version can not be less than zero.  Value = " + rollbackVersion);

		Release currentRelease = this.releaseRepository.findLatestReleaseForUpdate(releaseName);
		Assert.notNull(currentRelease, "Could not find release = [" + releaseName + "]");

		// Determine with version to rollback to
		int rollbackVersionToUse = rollbackVersion;
		Release releaseToRollback = null;
		if (rollbackVersion == 0) {
			releaseToRollback = this.releaseRepository.findReleaseToRollback(releaseName);
		}
		else {
			releaseToRollback = this.releaseRepository.findByNameAndVersion(releaseName, rollbackVersionToUse);
			StatusCode statusCode = releaseToRollback.getInfo().getStatus().getStatusCode();
			if (!(statusCode.equals(StatusCode.DEPLOYED) || statusCode.equals(StatusCode.DELETED))) {
				throw new SkipperException("Rollback version should either be in deployed or deleted status.");
			}
		}
		Assert.notNull(releaseToRollback, "Could not find Release to rollback to [releaseName,releaseVersion] = ["
				+ releaseName + "," + rollbackVersionToUse + "]");

		log.info("Rolling back releaseName={}.  Current version={}, Target version={}", releaseName,
				currentRelease.getVersion(), rollbackVersionToUse);

		// rollback a release will dispatch to either install or upgrade,
		// thus create install or update request conditionally. we have
		// same package identifier for both.

		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(releaseToRollback.getPkg().getMetadata().getName());
		packageIdentifier.setPackageVersion(releaseToRollback.getPkg().getMetadata().getVersion());
		packageIdentifier.setRepositoryName(releaseToRollback.getPkg().getMetadata().getRepositoryName());

		if (currentRelease.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) {
			InstallRequest installRequest = new InstallRequest();
			InstallProperties installProperties = new InstallProperties();
			installProperties.setConfigValues(releaseToRollback.getConfigValues());
			installProperties.setPlatformName(releaseToRollback.getPlatformName());
			installProperties.setReleaseName(releaseName);
			installRequest.setInstallProperties(installProperties);
			installRequest.setPackageIdentifier(packageIdentifier);
			context.getExtendedState().getVariables().put(SkipperEventHeaders.INSTALL_REQUEST, installRequest);
		}
		else {
			UpgradeRequest upgradeRequest = new UpgradeRequest();
			UpgradeProperties upgradeProperties = new UpgradeProperties();
			upgradeProperties.setReleaseName(releaseName);
			upgradeProperties.setConfigValues(releaseToRollback.getConfigValues());
			upgradeRequest.setUpgradeProperties(upgradeProperties);
			upgradeRequest.setPackageIdentifier(packageIdentifier);
			RollbackRequest rollbackRequest = context.getExtendedState().get(SkipperEventHeaders.ROLLBACK_REQUEST,
					RollbackRequest.class);
			if (rollbackRequest != null) {
				upgradeRequest.setTimeout(rollbackRequest.getTimeout());
			}
			context.getExtendedState().getVariables().put(SkipperEventHeaders.UPGRADE_REQUEST, upgradeRequest);
		}
	}
}
