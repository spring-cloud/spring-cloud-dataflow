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
package org.springframework.cloud.skipper.server.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEventHeaders;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperVariables;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.util.Assert;

/**
 * StateMachine {@link Action} preparing a rollback.
 *
 * @author Janne Valkealahti
 *
 */
public class RollbackStartAction  extends AbstractAction {

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
		String releaseName = context.getMessageHeaders().get(SkipperEventHeaders.RELEASE_NAME, String.class);
		Integer rollbackVersion = context.getMessageHeaders().get(SkipperEventHeaders.ROLLBACK_VERSION, Integer.class);


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

		Release newRollbackRelease = new Release();
		newRollbackRelease.setName(releaseName);
		newRollbackRelease.setPkg(releaseToRollback.getPkg());
		newRollbackRelease.setManifest(releaseToRollback.getManifest());
		newRollbackRelease.setVersion(currentRelease.getVersion() + 1);
		newRollbackRelease.setPlatformName(releaseToRollback.getPlatformName());
		newRollbackRelease.setConfigValues(releaseToRollback.getConfigValues());
		newRollbackRelease.setInfo(Info.createNewInfo("Initial install underway"));

		context.getExtendedState().getVariables().put(SkipperVariables.TARGET_RELEASE, newRollbackRelease);

		if (!currentRelease.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) {
			context.getExtendedState().getVariables().put(SkipperVariables.SOURCE_RELEASE, currentRelease);
		}
	}

}
