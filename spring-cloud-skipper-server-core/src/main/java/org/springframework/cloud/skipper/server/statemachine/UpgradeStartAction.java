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

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.service.ReleaseReportService;
import org.springframework.cloud.skipper.server.service.ReleaseService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEventHeaders;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperVariables;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * StateMachine {@link Action} preparing upgrage logic and adds {@link ReleaseAnalysisReport}
 * and cutoff time to a context.
 *
 * @author Janne Valkealahti
 *
 */
public class UpgradeStartAction extends AbstractAction {

	private static final Logger log = LoggerFactory.getLogger(UpgradeStartAction.class);
	private static final long DEFAULT_UPGRADE_TIMEOUT = 300000L;
	private final ReleaseReportService releaseReportService;
	private final ReleaseService releaseService;

	/**
	 * Instantiates a new upgrade start action.
	 *
	 * @param releaseReportService the release report service
	 * @param releaseService the release service
	 */
	public UpgradeStartAction(ReleaseReportService releaseReportService, ReleaseService releaseService) {
		super();
		this.releaseReportService = releaseReportService;
		this.releaseService = releaseService;
	}

	@Override
	protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context) {
		setUpgradeCutOffTime(context);

		UpgradeRequest upgradeRequest = context.getMessageHeaders().get(SkipperEventHeaders.UPGRADE_REQUEST, UpgradeRequest.class);
		log.info("upgradeRequest {}", upgradeRequest);
		if (upgradeRequest != null) {
			ReleaseAnalysisReport releaseAnalysisReport = this.releaseReportService.createReport(upgradeRequest);
			log.info("releaseAnalysisReport {}", releaseAnalysisReport);
			context.getExtendedState().getVariables().put(SkipperVariables.RELEASE_ANALYSIS_REPORT, releaseAnalysisReport);
		}
		else {
			Release existingRelease = context.getExtendedState().get(SkipperVariables.SOURCE_RELEASE, Release.class);
			Release replacingRelease = context.getExtendedState().get(SkipperVariables.TARGET_RELEASE, Release.class);
			ReleaseAnalysisReport releaseAnalysisReport = releaseService.createReport(existingRelease, replacingRelease);
			context.getExtendedState().getVariables().put(SkipperVariables.RELEASE_ANALYSIS_REPORT, releaseAnalysisReport);
		}
	}

	private void setUpgradeCutOffTime(StateContext<SkipperStates, SkipperEvents> context) {
		Long upgradeTimeout = context.getMessageHeaders().get(SkipperEventHeaders.UPGRADE_TIMEOUT, Long.class);
		if (upgradeTimeout == null) {
			upgradeTimeout = DEFAULT_UPGRADE_TIMEOUT;
		}
		long cutOffTime = System.currentTimeMillis() + upgradeTimeout;
		context.getExtendedState().getVariables().put(SkipperVariables.UPGRADE_CUTOFF_TIME,
				cutOffTime);
		log.debug("Set cutoff time as {}", cutOffTime);
	}
}
