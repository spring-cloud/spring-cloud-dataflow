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

import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategy;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategyFactory;
import org.springframework.cloud.skipper.server.service.ReleaseReportService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEventHeaders;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.util.ManifestUtils;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * StateMachine {@link Action} accepting app with an {@link UpgradeStrategy}.
 *
 * @author Janne Valkealahti
 *
 */
public class UpgradeDeleteSourceAppsAction extends AbstractUpgradeStartAction {

	private final UpgradeStrategyFactory upgradeStrategyFactory;

	/**
	 * Instantiates a new upgrade delete source apps action.
	 *
	 * @param releaseReportService the release report service
	 * @param upgradeStrategyFactory the upgrade strategy factory
	 */
	public UpgradeDeleteSourceAppsAction(ReleaseReportService releaseReportService, UpgradeStrategyFactory upgradeStrategyFactory) {
		super(releaseReportService);
		this.upgradeStrategyFactory = upgradeStrategyFactory;
	}

	@Override
	protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context) {
		super.executeInternal(context);
		ReleaseAnalysisReport releaseAnalysisReport = getReleaseAnalysisReport(context);

		// check if we're doing rollback and pass flag to strategy
		RollbackRequest rollbackRequest = context.getExtendedState().get(SkipperEventHeaders.ROLLBACK_REQUEST,
				RollbackRequest.class);

		// TODO: should check both releases
		String kind = ManifestUtils.resolveKind(releaseAnalysisReport.getExistingRelease().getManifest().getData());
		UpgradeStrategy upgradeStrategy = this.upgradeStrategyFactory.getUpgradeStrategy(kind);
		upgradeStrategy.accept(releaseAnalysisReport.getExistingRelease(), releaseAnalysisReport.getReplacingRelease(),
				releaseAnalysisReport, rollbackRequest != null);
	}
}
