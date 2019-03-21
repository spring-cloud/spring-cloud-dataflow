/*
 * Copyright 2017 the original author or authors.
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

import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

/**
 * Base class for {@link Action}s wanting to automatically wrap its execution in
 * try/catch and add exception into extended state for further processing for
 * interested parties.
 *
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractAction implements Action<SkipperStates, SkipperEvents> {

	private static final Logger log = LoggerFactory.getLogger(AbstractAction.class);

	@Override
	public final void execute(StateContext<SkipperStates, SkipperEvents> context) {
		try {
			executeInternal(context);
		}
		catch (Exception e) {
			// any error here will lead to adding exception into
			// extended state, thus allowing machine to break up
			// from executing state.
			log.error("Action execution failed class=[" + getClass() + "]", e);
			context.getExtendedState().getVariables().put(SkipperStateMachineService.SkipperVariables.ERROR, e);
		}
	}

	/**
	 * Internal execution similar to {@link Action#execute(StateContext)}. This
	 * execution is wrapped in try/catch and possible error added to an extended
	 * state.
	 *
	 * @param context the context
	 */
	abstract protected void executeInternal(StateContext<SkipperStates, SkipperEvents> context);
}
