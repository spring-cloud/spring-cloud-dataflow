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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.server.deployer.strategies.HealthCheckProperties;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategyFactory;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.cloud.skipper.server.service.ReleaseReportService;
import org.springframework.cloud.skipper.server.service.ReleaseService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEventHeaders;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperVariables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.TransitionConflictPolicy;

/**
 * Statemachine(s) related configurations.
 *
 * @author Janne Valkealahti
 *
 */
@Configuration
public class StateMachineConfiguration {

	private static final Logger log = LoggerFactory.getLogger(StateMachineConfiguration.class);

	private static long adjustTimerPeriod(HealthCheckProperties healthCheckProperties) {
		// keep hard coded default as 1000ms and use sleepInMillis from documented
		// setting for now. Just make sure value is positive as otherwise
		// machine would go crazy.
		if (healthCheckProperties != null && healthCheckProperties.getSleepInMillis() > 0) {
			return healthCheckProperties.getSleepInMillis();
		}
		else {
			return 1000;
		}
	}

	/**
	 * Configuration defining {@link StateMachineFactory} for skipper release handling.
	 */
	@EnableStateMachineFactory(name = SkipperStateMachineService.STATEMACHINE_FACTORY_BEAN_NAME)
	@Configuration
	public static class SkipperStateMachineFactoryConfig extends StateMachineConfigurerAdapter<SkipperStates, SkipperEvents> {

		@Autowired
		private ReleaseService releaseService;

		@Autowired
		private ReleaseReportService releaseReportService;

		@Autowired
		private ReleaseRepository releaseRepository;

		@Autowired
		private UpgradeStrategyFactory upgradeStrategyFactory;

		@Autowired
		private HealthCheckProperties healthCheckProperties;

		@Autowired
		private StateMachineRuntimePersister<SkipperStates, SkipperEvents, String> stateMachineRuntimePersister;

		@Override
		public void configure(StateMachineConfigurationConfigurer<SkipperStates, SkipperEvents> config) throws Exception {
			config
				.withConfiguration()
					// this is to simply add logging for state enters
					.listener(new StateMachineListenerAdapter<SkipperStates, SkipperEvents>() {
						@Override
						public void stateEntered(State<SkipperStates, SkipperEvents> state) {
							log.info("Entering state {}", state);
						}
					})
					.transitionConflictPolicy(TransitionConflictPolicy.PARENT)
				.and()
				.withPersistence()
					.runtimePersister(stateMachineRuntimePersister);
		}

		@Override
		public void configure(StateMachineStateConfigurer<SkipperStates, SkipperEvents> states) throws Exception {
			states
				// there's no need to explicitly define supported states
				// as every state id used in below config, adds it as supported state
				.withStates()
					// define main states
					.initial(SkipperStates.INITIAL)
					.stateEntry(SkipperStates.ERROR, errorAction())
					// clear memory for stored variables
					.stateExit(SkipperStates.INITIAL, resetVariablesAction())
					.state(SkipperStates.INSTALL)
					.state(SkipperStates.DELETE)
					.state(SkipperStates.SCALE)
					.state(SkipperStates.UPGRADE)
					.state(SkipperStates.ROLLBACK)
					.junction(SkipperStates.ERROR_JUNCTION)
					.and()
					.withStates()
						// substates for install
						.parent(SkipperStates.INSTALL)
						.initial(SkipperStates.INSTALL_INSTALL)
						.stateEntry(SkipperStates.INSTALL_INSTALL, installInstallAction())
						.exit(SkipperStates.INSTALL_EXIT)
						.and()
					.withStates()
						// substates for upgrade
						.parent(SkipperStates.UPGRADE)
						.initial(SkipperStates.UPGRADE_START)
						.stateEntry(SkipperStates.UPGRADE_START, upgradeStartAction())
						.stateEntry(SkipperStates.UPGRADE_DEPLOY_TARGET_APPS, upgradeDeployTargetAppsAction())
						.state(SkipperStates.UPGRADE_WAIT_TARGET_APPS)
						.state(SkipperStates.UPGRADE_CHECK_TARGET_APPS, SkipperEvents.UPGRADE_CANCEL)
						.stateEntry(SkipperStates.UPGRADE_CHECK_TARGET_APPS, upgradeCheckTargetAppsAction())
						.stateEntry(SkipperStates.UPGRADE_DEPLOY_TARGET_APPS_SUCCEED, upgradeDeployTargetAppsSucceedAction())
						.stateEntry(SkipperStates.UPGRADE_DEPLOY_TARGET_APPS_FAILED, upgradeDeployTargetAppsFailedAction())
						.stateEntry(SkipperStates.UPGRADE_CANCEL, upgradeCancelAction())
						.stateEntry(SkipperStates.UPGRADE_DELETE_SOURCE_APPS, upgradeDeleteSourceAppsAction())
						.choice(SkipperStates.UPGRADE_CHECK_CHOICE)
						.exit(SkipperStates.UPGRADE_EXIT)
						.and()
					.withStates()
						// substates for scale
						.parent(SkipperStates.SCALE)
						.initial(SkipperStates.SCALE_SCALE)
						.stateEntry(SkipperStates.SCALE_SCALE, scaleScaleAction())
						.exit(SkipperStates.SCALE_EXIT)
						.and()
					.withStates()
						// substates for delete
						.parent(SkipperStates.DELETE)
						.initial(SkipperStates.DELETE_DELETE)
						.stateEntry(SkipperStates.DELETE_DELETE, deleteDeleteAction())
						.exit(SkipperStates.DELETE_EXIT)
						.and()
					.withStates()
						// substates for rollback
						.parent(SkipperStates.ROLLBACK)
						.initial(SkipperStates.ROLLBACK_START)
						.stateEntry(SkipperStates.ROLLBACK_START, rollbackStartAction())
						.choice(SkipperStates.ROLLBACK_CHOICE)
						.exit(SkipperStates.ROLLBACK_EXIT_UPGRADE)
						.exit(SkipperStates.ROLLBACK_EXIT_INSTALL)
						.exit(SkipperStates.ROLLBACK_EXIT);
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<SkipperStates, SkipperEvents> transitions) throws Exception {
			transitions

				// transitions around error handling outside of main skipper states
				// all controlled exit points go via error junction as well
				// anonymous transitions from all main states go to error junction
				// having error guard. error state leads back to initial state and
				// we're back to for processing next command.
				.withJunction()
					.source(SkipperStates.ERROR_JUNCTION)
					.first(SkipperStates.ERROR, errorGuard())
					.last(SkipperStates.INITIAL)
					.and()
				.withExternal()
					.source(SkipperStates.ERROR).target(SkipperStates.INITIAL)
					.and()
				.withExternal()
					.source(SkipperStates.INSTALL).target(SkipperStates.ERROR_JUNCTION)
					.guard(errorGuard())
					.and()
				.withExternal()
					.source(SkipperStates.UPGRADE).target(SkipperStates.ERROR_JUNCTION)
					.guard(errorGuard())
					.and()
				.withExternal()
					.source(SkipperStates.DELETE).target(SkipperStates.ERROR_JUNCTION)
					.guard(errorGuard())
					.and()
				.withExternal()
					.source(SkipperStates.SCALE).target(SkipperStates.ERROR_JUNCTION)
					.guard(errorGuard())
					.and()
				.withExternal()
					.source(SkipperStates.ROLLBACK).target(SkipperStates.ERROR_JUNCTION)
					.guard(errorGuard())
					.and()

				// install transitions
				.withExternal()
					.source(SkipperStates.INITIAL).target(SkipperStates.INSTALL)
					.event(SkipperEvents.INSTALL)
					.and()
				.withExternal()
					.source(SkipperStates.INSTALL_INSTALL).target(SkipperStates.INSTALL_EXIT)
					.and()
				.withExit()
					.source(SkipperStates.INSTALL_EXIT).target(SkipperStates.ERROR_JUNCTION)
					.and()

				// upgrade transitions
				.withExternal()
					.source(SkipperStates.INITIAL).target(SkipperStates.UPGRADE)
					.event(SkipperEvents.UPGRADE)
					.and()
				.withExternal()
					.source(SkipperStates.UPGRADE_START).target(SkipperStates.UPGRADE_DEPLOY_TARGET_APPS)
					.and()
				.withExternal()
					.source(SkipperStates.UPGRADE_DEPLOY_TARGET_APPS).target(SkipperStates.UPGRADE_WAIT_TARGET_APPS)
					.and()
				.withExternal()
					.source(SkipperStates.UPGRADE_WAIT_TARGET_APPS).target(SkipperStates.UPGRADE_CHECK_CHOICE)
					.timer(adjustTimerPeriod(healthCheckProperties))
					.and()
				.withExternal()
					.source(SkipperStates.UPGRADE_CHECK_TARGET_APPS).target(SkipperStates.UPGRADE_WAIT_TARGET_APPS)
					.and()
				.withExternal()
					// define transition which allows to break out from wait/check loop
					// if machine is in state where this can happen
					.source(SkipperStates.UPGRADE_WAIT_TARGET_APPS).target(SkipperStates.UPGRADE_CANCEL)
					.event(SkipperEvents.UPGRADE_CANCEL)
					.and()
				.withChoice()
					.source(SkipperStates.UPGRADE_CHECK_CHOICE)
					.first(SkipperStates.UPGRADE_DEPLOY_TARGET_APPS_SUCCEED, upgradeOkGuard())
					.then(SkipperStates.UPGRADE_DEPLOY_TARGET_APPS_FAILED, upgradeFailedGuard())
					.last(SkipperStates.UPGRADE_CHECK_TARGET_APPS)
					.and()
				.withExternal()
					.source(SkipperStates.UPGRADE_DEPLOY_TARGET_APPS_SUCCEED).target(SkipperStates.UPGRADE_DELETE_SOURCE_APPS)
					.event(SkipperEvents.UPGRADE_ACCEPT)
					.and()
				.withExternal()
					.source(SkipperStates.UPGRADE_DEPLOY_TARGET_APPS_SUCCEED).target(SkipperStates.UPGRADE_CANCEL)
					.event(SkipperEvents.UPGRADE_CANCEL)
					.and()
				.withExternal()
					.source(SkipperStates.UPGRADE_DEPLOY_TARGET_APPS_FAILED).target(SkipperStates.UPGRADE_CANCEL)
					.and()
				.withExternal()
					.source(SkipperStates.UPGRADE_CANCEL).target(SkipperStates.UPGRADE_EXIT)
					.and()
				.withExternal()
					.source(SkipperStates.UPGRADE_DELETE_SOURCE_APPS).target(SkipperStates.UPGRADE_EXIT)
					.and()
				.withExit()
					.source(SkipperStates.UPGRADE_EXIT).target(SkipperStates.ERROR_JUNCTION)
					.and()

				// delete transitions
				.withExternal()
					.source(SkipperStates.INITIAL).target(SkipperStates.DELETE)
					.event(SkipperEvents.DELETE)
					.and()
				.withExternal()
					.source(SkipperStates.DELETE_DELETE).target(SkipperStates.DELETE_EXIT)
					.and()
				.withExit()
					.source(SkipperStates.DELETE_EXIT).target(SkipperStates.ERROR_JUNCTION)
					.and()

				// scale transitions
				.withExternal()
					.source(SkipperStates.INITIAL).target(SkipperStates.SCALE)
					.event(SkipperEvents.SCALE)
					.and()
				.withExternal()
					.source(SkipperStates.SCALE_SCALE).target(SkipperStates.SCALE_EXIT)
					.and()
				.withExit()
					.source(SkipperStates.SCALE_EXIT).target(SkipperStates.ERROR_JUNCTION)
					.and()

				// rollback transitions
				.withExternal()
					.source(SkipperStates.INITIAL).target(SkipperStates.ROLLBACK)
					.event(SkipperEvents.ROLLBACK)
					.and()
				.withExternal()
					.source(SkipperStates.ROLLBACK_START).target(SkipperStates.ROLLBACK_CHOICE)
					.and()
				.withChoice()
					.source(SkipperStates.ROLLBACK_CHOICE)
					.first(SkipperStates.ROLLBACK_EXIT_UPGRADE, rollbackUpgradeGuard())
					.then(SkipperStates.ROLLBACK_EXIT_INSTALL, rollbackInstallGuard())
					.last(SkipperStates.ROLLBACK_EXIT)
					.and()
				.withExit()
					.source(SkipperStates.ROLLBACK_EXIT).target(SkipperStates.ERROR_JUNCTION)
					.and()
				.withExit()
					.source(SkipperStates.ROLLBACK_EXIT_UPGRADE).target(SkipperStates.UPGRADE)
					.and()
				.withExit()
					.source(SkipperStates.ROLLBACK_EXIT_INSTALL).target(SkipperStates.INSTALL);
		}

		@Bean
		public ResetVariablesAction resetVariablesAction() {
			return new ResetVariablesAction();
		}

		@Bean
		public Guard<SkipperStates, SkipperEvents> errorGuard() {
			return context -> context.getExtendedState().getVariables().containsKey(SkipperVariables.ERROR);
		}

		@Bean
		public ErrorAction errorAction() {
			return new ErrorAction();
		}

		@Bean
		public InstallInstallAction installInstallAction() {
			return new InstallInstallAction(releaseService);
		}

		@Bean
		public UpgradeStartAction upgradeStartAction() {
			return new UpgradeStartAction(releaseReportService);
		}

		@Bean
		public UpgradeDeployTargetAppsAction upgradeDeployTargetAppsAction() {
			return new UpgradeDeployTargetAppsAction(releaseReportService, upgradeStrategyFactory, healthCheckProperties);
		}

		@Bean
		public UpgradeCheckTargetAppsAction upgradeCheckTargetAppsAction() {
			return new UpgradeCheckTargetAppsAction(releaseReportService, upgradeStrategyFactory);
		}

		@Bean
		public UpgradeCheckNewAppsGuard upgradeOkGuard() {
			return new UpgradeCheckNewAppsGuard(true);
		}

		@Bean
		public UpgradeCheckNewAppsGuard upgradeFailedGuard() {
			return new UpgradeCheckNewAppsGuard(false);
		}

		@Bean
		public UpgradeDeployTargetAppsSucceedAction upgradeDeployTargetAppsSucceedAction() {
			return new UpgradeDeployTargetAppsSucceedAction();
		}

		@Bean
		public UpgradeDeployTargetAppsFailedAction upgradeDeployTargetAppsFailedAction() {
			return new UpgradeDeployTargetAppsFailedAction();
		}

		@Bean
		public UpgradeCancelAction upgradeCancelAction() {
			return new UpgradeCancelAction(releaseReportService, upgradeStrategyFactory);
		}

		@Bean
		public UpgradeDeleteSourceAppsAction upgradeDeleteSourceAppsAction() {
			return new UpgradeDeleteSourceAppsAction(releaseReportService, upgradeStrategyFactory);
		}

		@Bean
		public ScaleScaleAction scaleScaleAction() {
			return new ScaleScaleAction(releaseService);
		}

		@Bean
		public DeleteDeleteAction deleteDeleteAction() {
			return new DeleteDeleteAction(releaseService);
		}

		@Bean
		public RollbackStartAction rollbackStartAction() {
			return new RollbackStartAction(releaseRepository);
		}

		@Bean
		public Guard<SkipperStates, SkipperEvents> rollbackInstallGuard() {
			return context -> {
				return context.getExtendedState().getVariables().containsKey(SkipperEventHeaders.INSTALL_REQUEST);
			};
		}

		@Bean
		public Guard<SkipperStates, SkipperEvents> rollbackUpgradeGuard() {
			return context -> {
				return context.getExtendedState().getVariables().containsKey(SkipperEventHeaders.UPGRADE_REQUEST);
			};
		}
	}

	/**
	 * Configuration related to {@link SkipperStateMachineService}.
	 */
	@Configuration
	public static class StateMachineServiceConfig {

		@Bean
		public StateMachineService<SkipperStates, SkipperEvents> stateMachineService(
				StateMachineFactory<SkipperStates, SkipperEvents> stateMachineFactory,
				StateMachinePersist<SkipperStates, SkipperEvents, String> stateMachinePersist) {
			return new DefaultStateMachineService<>(stateMachineFactory, stateMachinePersist);
		}

		@Bean
		public SkipperStateMachineService skipperStateMachineService(StateMachineService<SkipperStates, SkipperEvents> stateMachineService) {
			return new SkipperStateMachineService(stateMachineService);
		}
	}
}
