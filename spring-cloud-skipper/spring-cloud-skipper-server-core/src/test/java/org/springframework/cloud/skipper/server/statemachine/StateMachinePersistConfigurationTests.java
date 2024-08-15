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

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperVariables;
import org.springframework.cloud.skipper.server.statemachine.StateMachinePersistConfiguration.SkipUnwantedVariablesFunction;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.support.DefaultExtendedState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for persist skip function.
 *
 * @author Janne Valkealahti
 * @author Corneil du Plessis
 */
public class StateMachinePersistConfigurationTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testSkipFunction() {
		SkipUnwantedVariablesFunction f = new SkipUnwantedVariablesFunction();

		DefaultExtendedState extendedState = new DefaultExtendedState();
		extendedState.getVariables().put(SkipperVariables.SOURCE_RELEASE, new Object());
		extendedState.getVariables().put(SkipperVariables.TARGET_RELEASE, new Object());
		extendedState.getVariables().put(SkipperVariables.RELEASE, new Object());
		extendedState.getVariables().put(SkipperVariables.RELEASE_ANALYSIS_REPORT, new Object());
		extendedState.getVariables().put(SkipperVariables.ERROR, new Object());
		extendedState.getVariables().put(SkipperVariables.UPGRADE_CUTOFF_TIME, new Object());
		extendedState.getVariables().put(SkipperVariables.UPGRADE_STATUS, new Object());

		StateMachine<SkipperStates, SkipperEvents> stateMachine = Mockito.mock(StateMachine.class);
		Mockito.when(stateMachine.getExtendedState()).thenReturn(extendedState);

		// test that others gets filtered out
		Map<Object, Object> map = f.apply(stateMachine);
		assertThat(map).isNotNull();
		assertThat(map).containsOnlyKeys(SkipperVariables.UPGRADE_CUTOFF_TIME, SkipperVariables.UPGRADE_STATUS);
	}
}
