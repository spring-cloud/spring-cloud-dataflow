/*
 * Copyright 2018 the original author or authors.
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
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.service.ReleaseReportService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEventHeaders;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperVariables;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.util.Assert;

/**
 * Base class for upgrade related {@link Action}s wanting some shared functionality.
 *
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractUpgradeStartAction extends AbstractAction {

	private final ReleaseReportService releaseReportService;

	/**
	 * Instantiates a new abstract upgrade start action.
	 *
	 * @param releaseReportService the release report service
	 */
	public AbstractUpgradeStartAction(ReleaseReportService releaseReportService) {
		super();
		Assert.notNull(releaseReportService, "'releaseReportService' must be set");
		this.releaseReportService = releaseReportService;
	}

	@Override
	protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context) {
		UpgradeRequest upgradeRequest = context.getExtendedState().get(SkipperEventHeaders.UPGRADE_REQUEST,
				UpgradeRequest.class);
		RollbackRequest rollbackRequest = context.getExtendedState().get(SkipperEventHeaders.ROLLBACK_REQUEST,
				RollbackRequest.class);
		Assert.notNull(upgradeRequest, "'upgradeRequest' not known to the system in extended state");
		ReleaseAnalysisReport releaseAnalysisReport = this.getReleaseReportService().createReport(upgradeRequest,
				rollbackRequest, handlesInitialReport());
		context.getExtendedState().getVariables().put(SkipperVariables.RELEASE_ANALYSIS_REPORT, releaseAnalysisReport);
	}

	protected ReleaseAnalysisReport getReleaseAnalysisReport(
			StateContext<SkipperStates, SkipperEvents> context) {
		ReleaseAnalysisReport releaseAnalysisReport = context.getExtendedState()
				.get(SkipperVariables.RELEASE_ANALYSIS_REPORT, ReleaseAnalysisReport.class);
		Assert.notNull(releaseAnalysisReport, "'releaseAnalysisReport' not known to the system in extended state");
		return releaseAnalysisReport;
	}

	protected boolean handlesInitialReport() {
		return false;
	}

	protected ReleaseReportService getReleaseReportService() {
		return releaseReportService;
	}
}
