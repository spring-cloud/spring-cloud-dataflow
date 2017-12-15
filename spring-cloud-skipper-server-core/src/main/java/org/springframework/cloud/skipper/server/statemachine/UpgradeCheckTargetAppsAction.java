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

import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategy;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperVariables;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * StateMachine {@link Action} checking upgrade status with an {@link UpgradeStrategy}.
 *
 * @author Janne Valkealahti
 *
 */
public class UpgradeCheckTargetAppsAction extends AbstractAction {

	private static final Logger log = LoggerFactory.getLogger(UpgradeCheckTargetAppsAction.class);
	private final UpgradeStrategy upgradeStrategy;

	/**
	 * Instantiates a new upgrade check target apps action.
	 *
	 * @param upgradeStrategy the upgrade strategy
	 */
	public UpgradeCheckTargetAppsAction(UpgradeStrategy upgradeStrategy) {
		super();
		this.upgradeStrategy = upgradeStrategy;
	}

	@Override
	protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context) {
		ReleaseAnalysisReport releaseAnalysisReport = context.getExtendedState().get(SkipperVariables.RELEASE_ANALYSIS_REPORT,
				ReleaseAnalysisReport.class);
		int upgradeStatus = 0;
		boolean ok = upgradeStrategy.checkStatus(releaseAnalysisReport.getReplacingRelease());
		log.debug("upgradeStrategy checkStatus {}", ok);
		if (ok) {
			upgradeStatus = 1;
		}
		else if (!ok && cutOffTimeExceed(context)) {
			upgradeStatus = -1;
		}
		log.debug("Setting upgradeStatus {}", upgradeStatus);
		context.getExtendedState().getVariables().put(SkipperVariables.UPGRADE_STATUS, upgradeStatus);
	}

	private boolean cutOffTimeExceed(StateContext<SkipperStates, SkipperEvents> context) {
		long now = System.currentTimeMillis();
		Long cutOffTime = context.getExtendedState().get(SkipperVariables.UPGRADE_CUTOFF_TIME, Long.class);
		log.debug("Testing cutOffTime {} to now {}", cutOffTime, now);
		return now > cutOffTime;
	}
}
