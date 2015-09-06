/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.module.deployer.yarn;

import java.util.Collection;
import java.util.Map;

import org.springframework.cloud.dataflow.module.deployer.yarn.YarnCloudAppService.CloudAppInfo;
import org.springframework.cloud.dataflow.module.deployer.yarn.YarnCloudAppService.CloudAppInstanceInfo;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.config.StateMachineBuilder.Builder;
import org.springframework.statemachine.guard.Guard;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Class keeping all {@link StateMachine} logic in one place and is used
 * to dynamically build a machine.
 *
 * @author Janne Valkealahti
 */
public class YarnCloudAppStateMachine {

	static final String VAR_APP_VERSION = "appVersion";
	static final String VAR_APPLICATION_ID = "applicationId";
	static final String HEADER_APP_VERSION = "appVersion";
	static final String HEADER_CLUSTER_ID = "clusterId";
	static final String HEADER_COUNT = "count";
	static final String HEADER_MODULE = "module";
	static final String HEADER_DEFINITION_PARAMETERS = "definitionParameters";
	static final String HEADER_ERROR = "error";

	private final YarnCloudAppService yarnCloudAppService;
	private final TaskExecutor taskExecutor;

	/**
	 * Instantiates a new yarn cloud app state machine.
	 *
	 * @param yarnCloudAppService the yarn cloud app service
	 * @param taskExecutor the task executor
	 */
	public YarnCloudAppStateMachine(YarnCloudAppService yarnCloudAppService, TaskExecutor taskExecutor) {
		Assert.notNull(yarnCloudAppService, "YarnCloudAppService must be set");
		Assert.notNull(taskExecutor, "TaskExecutor must be set");
		this.yarnCloudAppService = yarnCloudAppService;
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Builds the state machine and instructs it to start automatically.
	 *
	 * @return the state machine
	 * @throws Exception the exception
	 */
	public StateMachine<States, Events> buildStateMachine() throws Exception {
		return buildStateMachine(true);
	}

	/**
	 * Builds the state machine.
	 *
	 * @param autoStartup the auto startup
	 * @return the state machine
	 * @throws Exception the exception
	 */
	public StateMachine<States, Events> buildStateMachine(boolean autoStartup) throws Exception {
		Builder<States, Events> builder = StateMachineBuilder.builder();

		builder.configureConfiguration()
			.withConfiguration()
				.autoStartup(autoStartup)
				.taskExecutor(taskExecutor);

		builder.configureStates()
			.withStates()
				.initial(States.READY)
				.state(States.ERROR)
				.state(States.DEPLOYMODULE, new ResetVariablesAction(), null)
				.state(States.DEPLOYMODULE, Events.DEPLOY, Events.UNDEPLOY)
				.state(States.UNDEPLOYMODULE, new ResetVariablesAction(), null)
				.state(States.UNDEPLOYMODULE, Events.DEPLOY, Events.UNDEPLOY)
				.and()
				.withStates()
					.parent(States.DEPLOYMODULE)
					.initial(States.CHECKAPP)
					.state(States.CHECKAPP, new CheckAppAction(), null)
					.choice(States.PUSHAPPCHOICE)
					.state(States.PUSHAPP, new PushAppAction(), null)
					.state(States.CHECKINSTANCE, new CheckInstanceAction(), null)
					.choice(States.STARTINSTANCECHOICE)
					.state(States.STARTINSTANCE, new StartInstanceAction(), null)
					.state(States.CREATECLUSTER, new CreateClusterAction(), null)
					.state(States.STARTCLUSTER, new StartClusterAction(), null)
					.and()
				.withStates()
					.parent(States.UNDEPLOYMODULE)
					.initial(States.STOPCLUSTER)
					.state(States.STOPCLUSTER, new StopClusterAction(), null)
					.state(States.DESTROYCLUSTER, new DestroyClusterAction(), null);

		builder.configureTransitions()
			.withExternal()
				.source(States.DEPLOYMODULE).target(States.ERROR)
				.event(Events.ERROR)
				.and()
			.withExternal()
				.source(States.DEPLOYMODULE).target(States.READY)
				.event(Events.CONTINUE)
				.and()
			.withExternal()
				.source(States.UNDEPLOYMODULE).target(States.READY)
				.event(Events.CONTINUE)
				.and()
			.withExternal()
				.source(States.READY).target(States.DEPLOYMODULE)
				.event(Events.DEPLOY)
				.and()
			.withExternal()
				.source(States.READY).target(States.UNDEPLOYMODULE)
				.event(Events.UNDEPLOY)
				.and()
			.withExternal()
				.source(States.CHECKAPP).target(States.PUSHAPPCHOICE)
				.and()
			.withChoice()
				.source(States.PUSHAPPCHOICE)
				.first(States.PUSHAPP, new PushAppGuard())
				.last(States.CHECKINSTANCE)
				.and()
			.withExternal()
				.source(States.PUSHAPP).target(States.CHECKINSTANCE)
				.and()
			.withExternal()
				.source(States.CHECKINSTANCE).target(States.STARTINSTANCECHOICE)
				.and()
			.withChoice()
				.source(States.STARTINSTANCECHOICE)
				.first(States.STARTINSTANCE, new StartInstanceGuard())
				.last(States.CREATECLUSTER)
				.and()
			.withExternal()
				.source(States.STARTINSTANCE).target(States.CREATECLUSTER)
				.and()
			.withExternal()
				.source(States.CREATECLUSTER).target(States.STARTCLUSTER)
				.and()
			.withExternal()
				.source(States.STOPCLUSTER).target(States.DESTROYCLUSTER);

		return builder.build();
	}

	/**
	 * {@link Action} which clears existing extended state variables.
	 */
	private class ResetVariablesAction implements Action<States, Events> {

		@Override
		public void execute(StateContext<States, Events> context) {
			context.getExtendedState().getVariables().clear();
		}
	}

	/**
	 * {@link Action} which queries {@link YarnCloudAppService} and checks if
	 * passed {@code appVersion} from event headers exists and sends {@code ERROR}
	 * event into state machine if it doesn't exist. Add to be used {@code appVersion}
	 * into extended state variables which later used by other guards and actions.
	 */
	private class CheckAppAction implements Action<States, Events> {

		@Override
		public void execute(StateContext<States, Events> context) {
			String appVersion = (String) context.getMessageHeader(HEADER_APP_VERSION);

			if (!StringUtils.hasText(appVersion)) {
				context.getStateMachine().sendEvent(
						MessageBuilder.withPayload(Events.ERROR).setHeader(HEADER_ERROR, "appVersion not defined")
								.build());
			} else {
				Collection<CloudAppInfo> appInfos = yarnCloudAppService.getApplications();
				for (CloudAppInfo appInfo : appInfos) {
					if (appInfo.getName().equals(appVersion)) {
						context.getExtendedState().getVariables().put(VAR_APP_VERSION, appVersion);
					}
				}
			}
		}
	}

	/**
	 * {@link Guard} which is used to protect state where application push
	 * into hdfs would happen. Assumes that if {@code appVersion} variable
	 * exists, application is installed.
	 */
	private class PushAppGuard implements Guard<States, Events> {

		@Override
		public boolean evaluate(StateContext<States, Events> context) {
			return !context.getExtendedState().getVariables().containsKey(VAR_APP_VERSION);
		}
	}

	/**
	 * {@link Action} which pushes application version into hdfs found
	 * from variable {@code appVersion}.
	 */
	private class PushAppAction implements Action<States, Events> {

		@Override
		public void execute(StateContext<States, Events> context) {
			String appVersion = (String) context.getMessageHeader(HEADER_APP_VERSION);
			yarnCloudAppService.pushApplication(appVersion);
		}
	}

	/**
	 * {@link Action} which queries {@link YarnCloudAppService} for existing
	 * running instances.
	 */
	private class CheckInstanceAction implements Action<States, Events> {

		@Override
		public void execute(StateContext<States, Events> context) {
			Collection<CloudAppInstanceInfo> appInstanceInfos = yarnCloudAppService.getInstances();
			for (CloudAppInstanceInfo appInstanceInfo : appInstanceInfos) {
				if (appInstanceInfo.getAddress().contains("http")) {
					context.getExtendedState().getVariables().put(VAR_APPLICATION_ID, appInstanceInfo.getApplicationId());
					break;
				}
			}

		}
	}

	/**
	 * {@link Guard} which protects state {@code STARTINSTANCE} in choice state
	 * {@code STARTINSTANCECHOICE}.
	 */
	private class StartInstanceGuard implements Guard<States, Events> {

		@Override
		public boolean evaluate(StateContext<States, Events> context) {
			return !context.getExtendedState().getVariables().containsKey(VAR_APPLICATION_ID);
		}
	}

	/**
	 * {@link Action} which launches new application instance.
	 */
	private class StartInstanceAction implements Action<States, Events> {

		@Override
		public void execute(StateContext<States, Events> context) {
			String appVersion = (String) context.getMessageHeader(HEADER_APP_VERSION);
			String applicationId = yarnCloudAppService.submitApplication(appVersion);
			context.getExtendedState().getVariables().put(VAR_APPLICATION_ID, applicationId);

			// TODO: for now just loop until we get proper handling
			//       via looping in a state machine itself.
			Exception error = null;
			for (int i = 0; i < 60; i++) {
				try {
					if (isRunning()) {
						return;
					}
					else {
						Thread.sleep(1000);
					}
				}
				catch (InterruptedException e) {
					error = e;
					Thread.currentThread().interrupt();
					break;
				}
				catch (Exception e) {
					error = e;
					break;
				}
			}
			// TODO: we don't yet handle errors
			if (error != null) {
				context.getStateMachine().sendEvent(
						MessageBuilder.withPayload(Events.ERROR)
								.setHeader(HEADER_ERROR, "failed starting app " + error).build());
			}
		}

		private boolean isRunning() {
			for (CloudAppInstanceInfo instanceInfo : yarnCloudAppService.getInstances()) {
				if (instanceInfo.getAddress().contains("http")) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * {@link Action} which creates a new container cluster.
	 */
	private class CreateClusterAction implements Action<States, Events> {

		@SuppressWarnings("unchecked")
		@Override
		public void execute(StateContext<States, Events> context) {
			yarnCloudAppService.createCluster(context.getExtendedState().get(VAR_APPLICATION_ID, String.class), context
					.getMessageHeaders().get(HEADER_CLUSTER_ID, String.class),
					context.getMessageHeaders().get(HEADER_COUNT, Integer.class),
					context.getMessageHeaders().get(HEADER_MODULE, String.class),
					context.getMessageHeaders().get(HEADER_DEFINITION_PARAMETERS, Map.class));
		}
	}

	/**
	 * {@link Action} which starts existing container cluster.
	 */
	private class StartClusterAction implements Action<States, Events> {

		@Override
		public void execute(StateContext<States, Events> context) {
			yarnCloudAppService.startCluster(context.getExtendedState().get(VAR_APPLICATION_ID, String.class), context
					.getMessageHeaders().get(HEADER_CLUSTER_ID, String.class));
			context.getStateMachine().sendEvent(Events.CONTINUE);
		}
	}

	/**
	 * {@link Action} which stops existing container cluster.
	 */
	private class StopClusterAction implements Action<States, Events> {

		@Override
		public void execute(StateContext<States, Events> context) {
			String clusterId = context.getMessageHeaders().get(HEADER_CLUSTER_ID, String.class);
			for (CloudAppInstanceInfo instanceInfo : yarnCloudAppService.getInstances()) {
				for (String cluster : yarnCloudAppService.getClusters(instanceInfo.getApplicationId())) {
					if (cluster.equals(clusterId)) {
						yarnCloudAppService.stopCluster(instanceInfo.getApplicationId(), clusterId);
						return;
					}
				}
			}
		}
	}

	/**
	 * {@link Action} which destroys existing container cluster.
	 */
	private class DestroyClusterAction implements Action<States, Events> {

		@Override
		public void execute(StateContext<States, Events> context) {
			String clusterId = context.getMessageHeaders().get(HEADER_CLUSTER_ID, String.class);
			for (CloudAppInstanceInfo instanceInfo : yarnCloudAppService.getInstances()) {
				for (String cluster : yarnCloudAppService.getClusters(instanceInfo.getApplicationId())) {
					if (cluster.equals(clusterId)) {
						yarnCloudAppService.destroyCluster(instanceInfo.getApplicationId(), clusterId);
						context.getStateMachine().sendEvent(Events.CONTINUE);
						return;
					}
				}
			}
			context.getStateMachine().sendEvent(Events.CONTINUE);
		}
	}

	/**
	 * Enumeration of module handling states.
	 */
	public enum States {

		/** Main state where machine is ready for either deploy or undeploy requests. */
		READY,

		/** State where possible errors are handled. */
		ERROR,

		/** Super state of all other states handling deployment. */
		DEPLOYMODULE,

		/** State where app presence in hdfs is checked. */
		CHECKAPP,

		/** Pseudostate where choice to enter {@code PUSHAPP} is made. */
		PUSHAPPCHOICE,

		/** State where application is pushed into hdfs. */
		PUSHAPP,

		/** State where app instance running status is checked. */
		CHECKINSTANCE,

		/** Pseudostate where choice to enter {@code STARTINSTANCE} is made. */
		STARTINSTANCECHOICE,

		/** State where app instance is started. */
		STARTINSTANCE,

		/** State where container cluster is created. */
		CREATECLUSTER,

		/** State where container cluster is started. */
		STARTCLUSTER,

		/** Super state of all other states handling undeployment. */
		UNDEPLOYMODULE,

		/** State where container cluster is stopped. */
		STOPCLUSTER,

		/** State where container cluster is destroyed. */
		DESTROYCLUSTER;
	}

	/**
	 * Enumeration of module handling events.
	 */
	public enum Events {

		/** Event indicating that machine should handle deploy request. */
		DEPLOY,

		/** Event indicating that machine should handle undeploy request. */
		UNDEPLOY,

		/** Event indicating that machine should move into error handling logic. */
		ERROR,

		/** Event indicating that machine should move back into ready state. */
		CONTINUE
	}

}
