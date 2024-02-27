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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperVariables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.jpa.JpaRepositoryStateMachinePersist;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.kryo.KryoStateMachineSerialisationService;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.util.ObjectUtils;

/**
 * Persistence config for statemachine. Keeping all these separate from main machine
 * config allows to run tests tests in isolation without adding persistence layer.
 *
 * @author Janne Valkealahti
 *
 */
@Configuration
public class StateMachinePersistConfiguration {

	@Bean
	public StateMachineRuntimePersister<SkipperStates, SkipperEvents, String> stateMachineRuntimePersister(
			JpaStateMachineRepository jpaStateMachineRepository) {
		// create these manually to be able to add extended state variable filter
		KryoStateMachineSerialisationService<SkipperStates, SkipperEvents> serialisationService = new KryoStateMachineSerialisationService<SkipperStates, SkipperEvents>();
		JpaRepositoryStateMachinePersist<SkipperStates, SkipperEvents> persist = new JpaRepositoryStateMachinePersist<SkipperStates, SkipperEvents>(
				jpaStateMachineRepository, serialisationService);

		JpaPersistingStateMachineInterceptor<SkipperStates, SkipperEvents, String> interceptor = new JpaPersistingStateMachineInterceptor<>(
				persist);
		interceptor.setExtendedStateVariablesFunction(new SkipUnwantedVariablesFunction());
		return interceptor;
	}

	static class SkipUnwantedVariablesFunction
			implements Function<StateMachine<SkipperStates, SkipperEvents>, Map<Object, Object>> {

		@Override
		public Map<Object, Object> apply(StateMachine<SkipperStates, SkipperEvents> stateMachine) {
			return stateMachine.getExtendedState().getVariables().entrySet().stream().filter(e -> {
				return !(ObjectUtils.nullSafeEquals(e.getKey(), SkipperVariables.SOURCE_RELEASE)
						|| ObjectUtils.nullSafeEquals(e.getKey(), SkipperVariables.TARGET_RELEASE)
						|| ObjectUtils.nullSafeEquals(e.getKey(), SkipperVariables.RELEASE)
						|| ObjectUtils.nullSafeEquals(e.getKey(), SkipperVariables.RELEASE_ANALYSIS_REPORT)
						|| ObjectUtils.nullSafeEquals(e.getKey(), SkipperVariables.ERROR));
			}).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
		}
	}
}
