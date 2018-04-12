/*
 * Copyright 2017-2018 the original author or authors.
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
import org.springframework.cloud.skipper.server.service.ReleaseReportService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEventHeaders;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperVariables;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * StateMachine {@link Action} deploying app with an {@link UpgradeStrategy}.
 *
 * @author Janne Valkealahti
 *
 */
public class UpgradeDeployTargetAppsAction extends AbstractUpgradeStartAction {

	private static final Logger log = LoggerFactory.getLogger(UpgradeDeployTargetAppsAction.class);
	private static final long DEFAULT_UPGRADE_TIMEOUT = 300000L;
	private final UpgradeStrategy upgradeStrategy;

	/**
	 * Instantiates a new upgrade deploy target apps action.
	 *
	 * @param releaseReportService the release report service
	 * @param upgradeStrategy the upgrade strategy
	 */
	public UpgradeDeployTargetAppsAction(ReleaseReportService releaseReportService, UpgradeStrategy upgradeStrategy) {
		super(releaseReportService);
		this.upgradeStrategy = upgradeStrategy;
	}

	@Override
	protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context) {
		super.executeInternal(context);
		ReleaseAnalysisReport releaseAnalysisReport = getReleaseAnalysisReport(context);
		log.info("Using UpgradeStrategy {}", upgradeStrategy);
		setUpgradeCutOffTime(context);
		this.upgradeStrategy.deployApps(releaseAnalysisReport.getExistingRelease(),
				releaseAnalysisReport.getReplacingRelease(), releaseAnalysisReport);
		context.getExtendedState().getVariables().put(SkipperVariables.RELEASE, releaseAnalysisReport.getReplacingRelease());
	}

	private void setUpgradeCutOffTime(StateContext<SkipperStates, SkipperEvents> context) {
		Long upgradeTimeout = context.getExtendedState().get(SkipperEventHeaders.UPGRADE_TIMEOUT, Long.class);
		if (upgradeTimeout == null) {
			upgradeTimeout = DEFAULT_UPGRADE_TIMEOUT;
		}
		long cutOffTime = System.currentTimeMillis() + upgradeTimeout;
		context.getExtendedState().getVariables().put(SkipperVariables.UPGRADE_CUTOFF_TIME,
				cutOffTime);
		log.debug("Set cutoff time as {}", cutOffTime);
	}
}
