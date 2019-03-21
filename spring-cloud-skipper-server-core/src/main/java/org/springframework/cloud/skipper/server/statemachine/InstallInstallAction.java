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

import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.server.service.ReleaseService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEventHeaders;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperVariables;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.util.Assert;

/**
 * StateMachine {@link Action} handling calls to {@link ReleaseService} install methods
 * depending if either {@link InstallRequest} or {@link InstallProperties} exists in
 * a message header.
 *
 * @author Janne Valkealahti
 *
 */
public class InstallInstallAction extends AbstractAction {

	private static final Logger log = LoggerFactory.getLogger(InstallInstallAction.class);
	private final ReleaseService releaseService;

	/**
	 * Instantiates a new install install action.
	 *
	 * @param releaseService the release service
	 */
	public InstallInstallAction(ReleaseService releaseService) {
		super();
		Assert.notNull(releaseService, "'releaseService' must be set");
		this.releaseService = releaseService;
	}

	@Override
	protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context) {
		log.debug("Starting to execute action");
		// get from event headers and fall back checking if it's in context
		// in case machine died and we restored
		InstallRequest installRequest = context.getExtendedState().get(SkipperEventHeaders.INSTALL_REQUEST,
				InstallRequest.class);
		InstallProperties installProperties = context.getExtendedState().get(SkipperEventHeaders.INSTALL_PROPERTIES,
				InstallProperties.class);

		if (installRequest != null) {
			// we have an install request
			Release release = this.releaseService.install(installRequest);
			context.getExtendedState().getVariables().put(SkipperVariables.RELEASE, release);
		}
		else if (installProperties != null) {
			// we have install properties
			Long id = context.getMessageHeaders().get(SkipperEventHeaders.INSTALL_ID, Long.class);
			Release release = this.releaseService.install(id, installProperties);
			context.getExtendedState().getVariables().put(SkipperVariables.RELEASE, release);
		}
		else {
			throw new IllegalArgumentException(
					"Neither 'installRequest' or 'installProperties' not known to the system in extended state");
		}
	}
}
