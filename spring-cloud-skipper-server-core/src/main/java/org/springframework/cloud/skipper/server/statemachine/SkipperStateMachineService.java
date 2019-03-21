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

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.DeleteProperties;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateContext.Stage;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.transition.Transition;
import org.springframework.statemachine.transition.TransitionKind;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * Service class for state machine hiding its operational logic.
 *
 * @author Janne Valkealahti
 *
 */
public class SkipperStateMachineService {

	private static final Logger log = LoggerFactory.getLogger(SkipperStateMachineService.class);

	public final static String STATEMACHINE_FACTORY_BEAN_NAME = "skipperStateMachineFactory";

	private final StateMachineService<SkipperStates, SkipperEvents> stateMachineService;

	/**
	 * Instantiates a new skipper state machine service.
	 *
	 * @param stateMachineService the state machine service
	 */
	public SkipperStateMachineService(StateMachineService<SkipperStates, SkipperEvents> stateMachineService) {
		Assert.notNull(stateMachineService, "'stateMachineService' must be set");
		this.stateMachineService = stateMachineService;
	}

	/**
	 * Install release.
	 *
	 * @param installRequest the install request
	 * @return the release
	 */
	public Release installRelease(InstallRequest installRequest) {
		return installReleaseInternal(installRequest, null, null);
	}

	/**
	 * Install release.
	 *
	 * @param id the id
	 * @param installProperties the install properties
	 * @return the release
	 */
	public Release installRelease(Long id, InstallProperties installProperties) {
		return installReleaseInternal(null, id, installProperties);
	}

	/**
	 * Upgrade release.
	 *
	 * @param upgradeRequest the upgrade request
	 * @return the release
	 */
	public Release upgradeRelease(UpgradeRequest upgradeRequest) {
		String releaseName = upgradeRequest.getUpgradeProperties().getReleaseName();
		Message<SkipperEvents> message = MessageBuilder
				.withPayload(SkipperEvents.UPGRADE)
				.setHeader(SkipperEventHeaders.UPGRADE_REQUEST, upgradeRequest)
				.setHeader(SkipperEventHeaders.UPGRADE_TIMEOUT, upgradeRequest.getTimeout())
				.build();
		return handleMessageAndWait(message, releaseName, SkipperStates.UPGRADE_WAIT_TARGET_APPS);
	}

	/**
	 * Delete release.
	 *
	 * @param releaseName the release name
	 * @return the release
	 */
	public Release deleteRelease(String releaseName, DeleteProperties deleteProperties) {
		Message<SkipperEvents> message = MessageBuilder
				.withPayload(SkipperEvents.DELETE)
				.setHeader(SkipperEventHeaders.RELEASE_NAME, releaseName)
				.setHeader(SkipperEventHeaders.RELEASE_DELETE_PROPERTIES, deleteProperties)
				.build();
		return handleMessageAndWait(message, releaseName);
	}

	/**
	 * Rollback release.
	 *
	 * @param rollbackRequest the rollback request
	 * @return the release
	 */
	public Release rollbackRelease(RollbackRequest rollbackRequest) {
		Message<SkipperEvents> message = MessageBuilder
				.withPayload(SkipperEvents.ROLLBACK)
				.setHeader(SkipperEventHeaders.RELEASE_NAME, rollbackRequest.getReleaseName())
				.setHeader(SkipperEventHeaders.ROLLBACK_VERSION, rollbackRequest.getVersion())
				.setHeader(SkipperEventHeaders.ROLLBACK_REQUEST, rollbackRequest)
				.build();
		return handleMessageAndWait(message, rollbackRequest.getReleaseName(), SkipperStates.UPGRADE_WAIT_TARGET_APPS,
				SkipperStates.INITIAL);
	}

	/**
	 * Send an event to attempt a cancellation of an existing operation.
	 *
	 * @param releaseName the release name
	 * @return true if event were sent
	 */
	public boolean cancelRelease(String releaseName) {
		Message<SkipperEvents> message = MessageBuilder
				.withPayload(SkipperEvents.UPGRADE_CANCEL)
				.setHeader(SkipperEventHeaders.RELEASE_NAME, releaseName)
				.build();
		return handleMessageAndCheckAccept(message, releaseName);
	}

	private Release installReleaseInternal(InstallRequest installRequest, Long id, InstallProperties installProperties) {
		String releaseName = installRequest != null ? installRequest.getInstallProperties().getReleaseName()
				: installProperties.getReleaseName();
		Message<SkipperEvents> message = MessageBuilder
				.withPayload(SkipperEvents.INSTALL)
				.setHeader(SkipperEventHeaders.INSTALL_REQUEST, installRequest)
				.setHeader(SkipperEventHeaders.INSTALL_ID, id)
				.setHeader(SkipperEventHeaders.INSTALL_PROPERTIES, installProperties)
				.build();
		return handleMessageAndWait(message, releaseName);
	}

	private boolean isInitialTransition(Transition<?, ?> transition) {
		return transition != null && transition.getKind() == TransitionKind.INITIAL;
	}

	private Release handleMessageAndWait(Message<SkipperEvents> message, String machineId) {
		return handleMessageAndWait(message, machineId, SkipperStates.INITIAL);
	}

	private boolean handleMessageAndCheckAccept(Message<SkipperEvents> message, String machineId) {
		StateMachine<SkipperStates, SkipperEvents> stateMachine = stateMachineService.acquireStateMachine(machineId);
		return stateMachine.sendEvent(message);
	}

	private Release handleMessageAndWait(Message<SkipperEvents> message, String machineId, SkipperStates... statesToWait) {
		// machine gets acquired fully started
		StateMachine<SkipperStates, SkipperEvents> stateMachine = stateMachineService.acquireStateMachine(machineId);

		// setup future handling blocking requirement returning release
		SettableListenableFuture<Release> future = new SettableListenableFuture<>();
		StateMachineListener<SkipperStates, SkipperEvents> listener = new StateMachineListenerAdapter<SkipperStates, SkipperEvents>() {

			@Override
			public void stateContext(StateContext<SkipperStates, SkipperEvents> stateContext) {
				if (stateContext.getStage() == Stage.STATE_ENTRY) {
					if (stateContext.getTarget().getId() == SkipperStates.ERROR) {
						Exception exception = stateContext.getExtendedState().get(SkipperVariables.ERROR, Exception.class);
						if (exception != null) {
							// we went through error state, throw if there is an error
							log.info("setting future exception", exception);
							future.setException(exception);
						}
					}
					else if (Arrays.asList(statesToWait).contains(stateContext.getTarget().getId())
							&& !isInitialTransition(stateContext.getTransition())) {
						Release release = (Release) stateContext.getExtendedState().getVariables().get(SkipperVariables.RELEASE);
						// at this point we assume machine logic did set release
						log.info("setting future value {}", release);
						future.set(release);
					}
				}
			}
		};

		// add listener which gets removed eventually
		stateMachine.addStateListener(listener);
		future.addCallback(result -> {
			stateMachine.removeStateListener(listener);
		}, throwable -> {
			stateMachine.removeStateListener(listener);
		});

		// if machine doesn't accept an event, we're on state
		// where a particular message cannot be handled, thus
		// return exception. this simply happens when we are
		// i.e. upgrading and delete request comes in.
		if (stateMachine.sendEvent(message)) {
			try {
				return future.get();
			}
			catch (ExecutionException e) {
				if (e.getCause() instanceof SkipperException) {
					// throw as SkipperException
					throw (SkipperException) e.getCause();
				}
				throw new SkipperException("Error waiting to get Release from a statemachine", e);
			}
			catch (Exception e) {
				throw new SkipperException("Error waiting to get Release from a statemachine", e);
			}
		}
		else {
			throw new SkipperException("Statemachine is not in state ready to do " + message.getPayload());
		}
	}

	/**
	 * Enumeration of all possible states used by a machine.
	 */
	public enum SkipperStates {

		/**
		 * Initial state of a machine where instantiated machine goes.
		 */
		INITIAL,

		/**
		 * Central error handling state.
		 */
		ERROR,

		/**
		 * Central junction where all transitions from main skipper states terminates.
		 */
		ERROR_JUNCTION,

		/**
		 * Parent state of all install related states.
		 */
		INSTALL,

		/**
		 * State where apps deployment happens.
		 */
		INSTALL_INSTALL,

		/**
		 * Pseudostate used as a controlled exit point from {@link #INSTALL}.
		 */
		INSTALL_EXIT,

		/**
		 * Parent state of all upgrade related states.
		 */
		UPGRADE,

		/**
		 * State where all init logic happens before we can go
		 * to state where actual new apps will be deployed.
		 */
		UPGRADE_START,

		/**
		 * State where new apps are getting deployed.
		 */
		UPGRADE_DEPLOY_TARGET_APPS,

		/**
		 * Intermediate state where machine pauses to either doing
		 * a loop via {@link #UPGRADE_CHECK_TARGET_APPS} back to itself
		 * or hopping into {@link #UPGRADE_CANCEL}.
		 */
		UPGRADE_WAIT_TARGET_APPS,

		/**
		 * State where status of a target release is checked.
		 */
		UPGRADE_CHECK_TARGET_APPS,

		/**
		 * State where machine ends up if target release is considered failed.
		 */
		UPGRADE_DEPLOY_TARGET_APPS_FAILED,

		/**
		 * State where machine ends up if target release is considered successful.
		 */
		UPGRADE_DEPLOY_TARGET_APPS_SUCCEED,

		/**
		 * State where machine goes if it is possible to cancel current
		 * upgrade operation.
		 */
		UPGRADE_CANCEL,

		/**
		 * State where source apps are getting deleted.
		 */
		UPGRADE_DELETE_SOURCE_APPS,

		/**
		 * Pseudostate used to chooce between {@link #UPGRADE_DELETE_SOURCE_APPS}
		 * and {@link #UPGRADE_CHECK_TARGET_APPS}
		 */
		UPGRADE_CHECK_CHOICE,

		/**
		 * Pseudostate used as a controlled exit point from {@link #UPGRADE}.
		 */
		UPGRADE_EXIT,

		/**
		 * Parent state of all delete related states.
		 */
		DELETE,

		/**
		 * State where release delete happens.
		 */
		DELETE_DELETE,

		/**
		 * Pseudostate used as a controlled exit point from {@link #DELETE}.
		 */
		DELETE_EXIT,

		/**
		 * Parent state of all rollback related states.
		 */
		ROLLBACK,

		/**
		 * Initialisation state where future branch from {@link #ROLLBACK_CHOICE}
		 * is desided.
		 */
		ROLLBACK_START,

		/**
		 * Pseudostate makind decision between exit points {@link #ROLLBACK_EXIT},
		 * {@link #ROLLBACK_EXIT_INSTALL} and {@link #ROLLBACK_EXIT_UPGRADE}.
		 */
		ROLLBACK_CHOICE,

		/**
		 * Controlled exit into {@link #INSTALL}.
		 */
		ROLLBACK_EXIT_INSTALL,

		/**
		 * Controlled exit into {@link #UPGRADE}.
		 */
		ROLLBACK_EXIT_UPGRADE,

		/**
		 * Controlled exit which acts as a fallback in case either {@link #ROLLBACK_EXIT_INSTALL}
		 * or {@link #ROLLBACK_EXIT_UPGRADE} cannot be chosen for some reason.
		 */
		ROLLBACK_EXIT;
	}

	/**
	 * Enumeration of all possible events used by a machine.
	 */
	public enum SkipperEvents {

		/**
		 * Main level event instructing an install request.
		 */
		INSTALL,

		/**
		 * Main level event instructing a delete request.
		 */
		DELETE,

		/**
		 * Main level event instructing an upgrade request.
		 */
		UPGRADE,

		/**
		 * While being on {@link SkipperStates#UPGRADE}, this event can be used
		 * to try upgrade cancel operation. Cancellation happens if machine
		 * is in a state where it is possible to go into cancel procedure.
		 */
		UPGRADE_CANCEL,

		/**
		 * While being on {@link SkipperStates#UPGRADE}, this event can be used
		 * to try upgrade accept procedure.
		 */
		UPGRADE_ACCEPT,

		/**
		 * Main level event instructing a rollback request.
		 */
		ROLLBACK;
	}

	/**
	 * Definitions of possible event headers used by a machine. Defined as
	 * string constants instead of enums because spring message headers
	 * don't work with enums.
	 */
	public final class SkipperEventHeaders {

		/**
		 * Header for {@link PackageMetadata}.
		 */
		public static final String PACKAGE_METADATA = "PACKAGE_METADATA";

		/**
		 * Header for version integer used in api's.
		 */
		public static final String VERSION = "VERSION";

		/**
		 * Header for id used in install api's.
		 */
		public static final String INSTALL_ID = "INSTALL_ID";

		/**
		 * Header for {@link InstallProperties}.
		 */
		public static final String INSTALL_PROPERTIES = "INSTALL_PROPERTIES";

		/**
		 * Header for {@link InstallRequest}.
		 */
		public static final String INSTALL_REQUEST = "INSTALL_REQUEST";

		/**
		 * Header for {@link UpgradeRequest}.
		 */
		public static final String UPGRADE_REQUEST = "UPGRADE_REQUEST";

		/**
		 * Header for internal timeout value for upgrade.
		 */
		public static final String UPGRADE_TIMEOUT = "UPGRADE_TIMEOUT";

		/**
		 * Header for generic {@code release name} identifier.
		 */
		public static final String RELEASE_NAME = "RELEASE_NAME";

		/**
		 * Header for rollback version.
		 */
		public static final String ROLLBACK_VERSION = "ROLLBACK_VERSION";

		/**
		 * Header for {@link RollbackRequest}.
		 */
		public static final String ROLLBACK_REQUEST = "ROLLBACK_REQUEST";

		/**
		 * SCDF specific extension to allow deletion of
		 */
		public static final String RELEASE_DELETE_PROPERTIES = "RELEASE_DELETE_PROPERTIES";
	}

	/**
	 * Extended state variable names for skipper statemachine.
	 */
	public enum SkipperVariables {

		/**
		 * Global error variable which any component can set to
		 * indicate unprocessed exception.
		 */
		ERROR,

		/**
		 * Variable for release which is returned to a caller
		 * when machine goes back to initial state.
		 */
		RELEASE,

		/**
		 * Variable keeping {@link ReleaseAnalysisReport} in a context.
		 */
		RELEASE_ANALYSIS_REPORT,

		/**
		 * Variable for a {@link Release} where skipper is coming from.
		 */
		SOURCE_RELEASE,

		/**
		 * Variable for a {@link Release} where skipper is going to.
		 */
		TARGET_RELEASE,

		/**
		 * Variable keeping a cutoff time.
		 */
		UPGRADE_CUTOFF_TIME,

		/**
		 * Variable internally used in a an upgrade state for current status.
		 */
		UPGRADE_STATUS;
	}
}
