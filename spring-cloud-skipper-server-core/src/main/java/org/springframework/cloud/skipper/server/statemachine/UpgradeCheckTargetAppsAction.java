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

import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategy;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategyFactory;
import org.springframework.cloud.skipper.server.service.ReleaseReportService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperVariables;
import org.springframework.cloud.skipper.server.util.ManifestUtils;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * StateMachine {@link Action} checking upgrade status with an {@link UpgradeStrategy}.
 *
 * @author Janne Valkealahti
 *
 */
public class UpgradeCheckTargetAppsAction extends AbstractUpgradeStartAction {

	private static final Logger log = LoggerFactory.getLogger(UpgradeCheckTargetAppsAction.class);
	private final UpgradeStrategyFactory upgradeStrategyFactory;

	/**
	 * Instantiates a new upgrade check target apps action.
	 *
	 * @param releaseReportService the release report service
	 * @param upgradeStrategyFactory the upgrade strategy factory
	 */
	public UpgradeCheckTargetAppsAction(ReleaseReportService releaseReportService, UpgradeStrategyFactory upgradeStrategyFactory) {
		super(releaseReportService);
		this.upgradeStrategyFactory = upgradeStrategyFactory;
	}

	@Override
	protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context) {
		super.executeInternal(context);
		ReleaseAnalysisReport releaseAnalysisReport = getReleaseAnalysisReport(context);

		int upgradeStatus = 0;
		// TODO: should check both releases
		String kind = ManifestUtils.resolveKind(releaseAnalysisReport.getReplacingRelease().getManifest().getData());
		UpgradeStrategy upgradeStrategy = this.upgradeStrategyFactory.getUpgradeStrategy(kind);
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
		if (cutOffTime == null) {
			// missing cutoff, indicate exceed
			return true;
		}
		log.debug("Testing cutOffTime {} to now {}", cutOffTime, now);
		return now > cutOffTime;
	}
}
