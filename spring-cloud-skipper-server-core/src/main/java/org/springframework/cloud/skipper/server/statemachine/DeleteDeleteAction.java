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

import org.springframework.cloud.skipper.domain.DeleteProperties;
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
 * StateMachine {@link Action} handling delete with a {@link ReleaseService}.
 *
 * @author Janne Valkealahti
 * @author Christian Tzolov
 *
 */
public class DeleteDeleteAction extends AbstractAction {

	private static final Logger log = LoggerFactory.getLogger(DeleteDeleteAction.class);

	private final ReleaseService releaseService;

	/**
	 * Instantiates a new delete delete action.
	 *
	 * @param releaseService the release service
	 */
	public DeleteDeleteAction(ReleaseService releaseService) {
		super();
		Assert.notNull(releaseService, "'releaseService' must be set");
		this.releaseService = releaseService;
	}

	@Override
	protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context) {
		log.debug("Starting action " + context);
		String releaseName = context.getMessageHeaders().get(SkipperEventHeaders.RELEASE_NAME, String.class);
		log.info("About to delete {}", releaseName);
		DeleteProperties deleteProperties = context.getExtendedState()
				.get(SkipperEventHeaders.RELEASE_DELETE_PROPERTIES, DeleteProperties.class);
		Assert.notNull(deleteProperties, "'deleteProperties' not known to the system in extended state");
		Release release = this.releaseService.delete(releaseName, deleteProperties.isDeletePackage());
		context.getExtendedState().getVariables().put(SkipperVariables.RELEASE, release);
	}
}
