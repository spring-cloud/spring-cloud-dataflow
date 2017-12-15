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
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategy;
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
public class UpgradeDeployTargetAppsAction extends AbstractAction {

	private static final Logger log = LoggerFactory.getLogger(UpgradeDeployTargetAppsAction.class);
	private final UpgradeStrategy upgradeStrategy;

	/**
	 * Instantiates a new upgrade deploy target apps action.
	 *
	 * @param upgradeStrategy the upgrade strategy
	 */
	public UpgradeDeployTargetAppsAction(UpgradeStrategy upgradeStrategy) {
		super();
		this.upgradeStrategy = upgradeStrategy;
	}

	@Override
	protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context) {
		log.info("Using UpgradeStrategy {}", upgradeStrategy);
		ReleaseAnalysisReport releaseAnalysisReport = context.getExtendedState().get(SkipperVariables.RELEASE_ANALYSIS_REPORT,
				ReleaseAnalysisReport.class);
		log.info("releaseAnalysisReport {}", releaseAnalysisReport);
		if (releaseAnalysisReport == null) {
			throw new SkipperException("ReleaseAnalysis report is null");
		}
		this.upgradeStrategy.deployApps(releaseAnalysisReport.getExistingRelease(),
				releaseAnalysisReport.getReplacingRelease(), releaseAnalysisReport);
		context.getExtendedState().getVariables().put(SkipperVariables.RELEASE, releaseAnalysisReport.getReplacingRelease());
	}
}
