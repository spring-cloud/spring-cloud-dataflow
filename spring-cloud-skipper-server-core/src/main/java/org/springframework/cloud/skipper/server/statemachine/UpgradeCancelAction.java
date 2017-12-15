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

import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategy;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperVariables;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * StateMachine {@link Action}
 *
 * @author Janne Valkealahti
 *
 */
public class UpgradeCancelAction extends AbstractAction {

	private final UpgradeStrategy upgradeStrategy;

	/**
	 * Instantiates a new upgrade cancel action.
	 *
	 * @param upgradeStrategy the upgrade strategy
	 */
	public UpgradeCancelAction(UpgradeStrategy upgradeStrategy) {
		super();
		this.upgradeStrategy = upgradeStrategy;
	}

	@Override
	protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context) {
		ReleaseAnalysisReport releaseAnalysisReport = context.getExtendedState().get(SkipperVariables.RELEASE_ANALYSIS_REPORT,
				ReleaseAnalysisReport.class);
		upgradeStrategy.cancel(releaseAnalysisReport.getExistingRelease(), releaseAnalysisReport.getReplacingRelease(),
				releaseAnalysisReport);
	}
}
