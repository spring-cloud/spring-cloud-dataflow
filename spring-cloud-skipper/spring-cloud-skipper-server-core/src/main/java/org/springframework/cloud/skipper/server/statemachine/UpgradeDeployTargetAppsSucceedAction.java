/*
 * Copyright 2017-2024 the original author or authors.
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

import reactor.core.publisher.Mono;

import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * StateMachine {@link Action} which simple sends an event to machine to accept an upgrade.
 *
 * @author Janne Valkealahti
 *
 */
public class UpgradeDeployTargetAppsSucceedAction extends AbstractAction {

	@Override
	protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context) {
		// TODO: when we support other type of strategies, we would not need to just
		//       blindly send accept as we can also cancel upgrade in this stage.
		Message<SkipperEvents> event = MessageBuilder.withPayload(SkipperEvents.UPGRADE_ACCEPT).build();
		// TODO: blindly sending event and subscribing(we can't block here) into it is not
		//       super nice, should look better reactive chain possibly better
		//       reactive support in a statemachine
		context.getStateMachine().sendEvent(Mono.just(event)).subscribe();
	}
}
