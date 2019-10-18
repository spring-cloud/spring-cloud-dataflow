/*
 * Copyright 2017-2019 the original author or authors.
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

import org.springframework.cloud.skipper.domain.DeleteProperties;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.domain.ScaleRequest;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEventHeaders;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * StateMachine {@link Action} which simply clears extended state variables.
 *
 * @author Janne Valkealahti
 *
 */
public class ResetVariablesAction implements Action<SkipperStates, SkipperEvents> {

	@Override
	public void execute(StateContext<SkipperStates, SkipperEvents> context) {
		context.getExtendedState().getVariables().clear();

		// storing various requests into context so that those get persisted
		// when machine goes to any parent states (rollback, upgrade, install,
		// delete)

		// for install
		InstallRequest installRequest = context.getMessageHeaders().get(SkipperEventHeaders.INSTALL_REQUEST,
				InstallRequest.class);
		if (installRequest != null) {
			context.getExtendedState().getVariables().put(SkipperEventHeaders.INSTALL_REQUEST, installRequest);
		}

		InstallProperties installProperties = context.getMessageHeaders().get(SkipperEventHeaders.INSTALL_PROPERTIES,
				InstallProperties.class);
		if (installProperties != null) {
			context.getExtendedState().getVariables().put(SkipperEventHeaders.INSTALL_PROPERTIES, installProperties);
		}

		// for rollback and delete
		String releaseName = context.getMessageHeaders().get(SkipperEventHeaders.RELEASE_NAME, String.class);
		if (releaseName != null) {
			context.getExtendedState().getVariables().put(SkipperEventHeaders.RELEASE_NAME, releaseName);
		}

		// for rollback
		Integer rollbackVersion = context.getMessageHeaders().get(SkipperEventHeaders.ROLLBACK_VERSION, Integer.class);
		if (rollbackVersion != null) {
			context.getExtendedState().getVariables().put(SkipperEventHeaders.ROLLBACK_VERSION, rollbackVersion);
		}
		RollbackRequest rollbackRequest = context.getMessageHeaders().get(SkipperEventHeaders.ROLLBACK_REQUEST,
				RollbackRequest.class);
		if (rollbackRequest != null) {
			context.getExtendedState().getVariables().put(SkipperEventHeaders.ROLLBACK_REQUEST, rollbackRequest);
		}

		// for upgrade
		UpgradeRequest upgradeRequest = context.getMessageHeaders().get(SkipperEventHeaders.UPGRADE_REQUEST,
				UpgradeRequest.class);
		if (upgradeRequest != null) {
			context.getExtendedState().getVariables().put(SkipperEventHeaders.UPGRADE_REQUEST, upgradeRequest);
		}

		Long upgradeTimeout = context.getMessageHeaders().get(SkipperEventHeaders.UPGRADE_TIMEOUT, Long.class);
		if (upgradeTimeout != null) {
			context.getExtendedState().getVariables().put(SkipperEventHeaders.UPGRADE_TIMEOUT, upgradeTimeout);
		}

		// for delete
		DeleteProperties deleteProperties = context.getMessageHeaders()
				.get(SkipperEventHeaders.RELEASE_DELETE_PROPERTIES, DeleteProperties.class);
		if (deleteProperties != null) {
			context.getExtendedState().getVariables().put(SkipperEventHeaders.RELEASE_DELETE_PROPERTIES,
					deleteProperties);
		}

		// for scale
		ScaleRequest scaleRequest = context.getMessageHeaders().get(SkipperEventHeaders.SCALE_REQUEST,
				ScaleRequest.class);
		if (scaleRequest != null) {
			context.getExtendedState().getVariables().put(SkipperEventHeaders.SCALE_REQUEST, scaleRequest);
		}
	}
}
