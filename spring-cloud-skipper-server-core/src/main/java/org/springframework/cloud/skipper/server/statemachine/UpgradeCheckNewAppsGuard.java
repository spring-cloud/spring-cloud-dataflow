/*
 * Copyright 2017 the original author or authors.
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

import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperVariables;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

/**
 * A {@link Guard} using extended state variable {@link SkipperVariables#UPGRADE_STATUS}
 * to determine condition based on initialised {@code upgradeStatus} flag. Value of a
 * {@link SkipperVariables#UPGRADE_STATUS} is set in {@link UpgradeCheckTargetAppsAction}
 * and as this same guard is used to protect 'succeed' and 'failure' transitions,
 * {@code upgradeStatus} is simply used to differentiate between the two.
 *
 * @author Janne Valkealahti
 *
 */
public class UpgradeCheckNewAppsGuard implements Guard<SkipperStates, SkipperEvents> {

	private static final Logger log = LoggerFactory.getLogger(UpgradeCheckNewAppsGuard.class);
	private final boolean upgradeStatus;

	/**
	 * Instantiates a new upgrade check new apps guard.
	 *
	 * @param upgradeStatus the upgrade status flag
	 */
	public UpgradeCheckNewAppsGuard(boolean upgradeStatus) {
		this.upgradeStatus = upgradeStatus;
	}

	@Override
	public boolean evaluate(StateContext<SkipperStates, SkipperEvents> context) {
		Integer status = context.getExtendedState().get(SkipperVariables.UPGRADE_STATUS, Integer.class);
		log.debug("Checking condition {} with upgradeStatus {}", status, upgradeStatus);
		if (status == null || status == 0) {
			return false;
		}
		else if (upgradeStatus && status > 0) {
			return true;
		}
		else if (!upgradeStatus && status < 0) {
			return true;
		}
		return false;
	}
}
