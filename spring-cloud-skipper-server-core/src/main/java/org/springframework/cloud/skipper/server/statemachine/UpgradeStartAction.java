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
import org.springframework.cloud.skipper.server.service.ReleaseReportService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * StateMachine {@link Action} preparing upgrage logic and adds {@link ReleaseAnalysisReport}
 * and cutoff time to a context.
 *
 * @author Janne Valkealahti
 *
 */
public class UpgradeStartAction extends AbstractUpgradeStartAction {

	private static final Logger log = LoggerFactory.getLogger(UpgradeStartAction.class);

	/**
	 * Instantiates a new upgrade start action.
	 *
	 * @param releaseReportService the release report service
	 */
	public UpgradeStartAction(ReleaseReportService releaseReportService) {
		super(releaseReportService);
	}

	@Override
	protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context) {
		super.executeInternal(context);
		log.debug("Starting to execute action");
		// get from event headers and fall back checking if it's in context
		// in case machine died and we restored
	}

	@Override
	protected boolean handlesInitialReport() {
		return true;
	}
}
