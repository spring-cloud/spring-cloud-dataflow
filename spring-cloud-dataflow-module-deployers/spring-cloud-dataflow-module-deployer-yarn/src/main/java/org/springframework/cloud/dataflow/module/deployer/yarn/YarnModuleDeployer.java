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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.ArtifactCoordinates;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.cloud.dataflow.module.deployer.yarn.YarnCloudAppStateMachine.Events;
import org.springframework.cloud.dataflow.module.deployer.yarn.YarnCloudAppStateMachine.States;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;

/**
 * {@link ModuleDeployer} which communicates with a Yarn app running
 * on a Hadoop cluster waiting for deployment requests. This app
 * uses Spring Yarn's container grouping functionality to create a
 * new group per module type. This allows all modules to share the
 * same settings and the group itself can controlled, i.e. ramp up/down
 * or shutdown/destroy a whole group.
 *
 * @author Janne Valkealahti
 */
public class YarnModuleDeployer implements ModuleDeployer {

	private static final Logger logger = LoggerFactory.getLogger(YarnModuleDeployer.class);
	private final YarnCloudAppService yarnCloudAppService;
	private final StateMachine<States, Events> stateMachine;

	/**
	 * Instantiates a new yarn module deployer.
	 *
	 * @param yarnCloudAppService the yarn cloud app service
	 * @param stateMachine the state machine
	 */
	public YarnModuleDeployer(YarnCloudAppService yarnCloudAppService, StateMachine<States, Events> stateMachine) {
		this.yarnCloudAppService = yarnCloudAppService;
		this.stateMachine = stateMachine;
	}

	@Override
	public ModuleDeploymentId deploy(ModuleDeploymentRequest request) {
		int count = request.getCount();
		ArtifactCoordinates coordinates = request.getCoordinates();
		ModuleDefinition definition = request.getDefinition();
		ModuleDeploymentId id = ModuleDeploymentId.fromModuleDefinition(definition);
		String clusterId = moduleDeploymentIdToClusterId(id);
		String module = coordinates.toString();
		Map<String, String> definitionParameters = definition.getParameters();
		Map<String, String> deploymentProperties = request.getDeploymentProperties();

		logger.info("deploying request for definition: " + definition);
		logger.info("deploying module: " + module);
		logger.info("definitionParameters: " + definitionParameters);
		logger.info("deploymentProperties: " + deploymentProperties);

		// TODO: using default app name "app" until we start to customise
		//       via deploymentProperties
		Message<Events> message = MessageBuilder.withPayload(Events.DEPLOY)
				.setHeader(YarnCloudAppStateMachine.HEADER_APP_VERSION, "app")
				.setHeader(YarnCloudAppStateMachine.HEADER_CLUSTER_ID, clusterId)
				.setHeader(YarnCloudAppStateMachine.HEADER_COUNT, count)
				.setHeader(YarnCloudAppStateMachine.HEADER_MODULE, module)
				.setHeader(YarnCloudAppStateMachine.HEADER_DEFINITION_PARAMETERS, definitionParameters)
				.build();

		stateMachine.sendEvent(message);
		return id;
	}

	@Override
	public void undeploy(ModuleDeploymentId id) {
		String clusterId = moduleDeploymentIdToClusterId(id);
		Message<Events> message = MessageBuilder.withPayload(Events.UNDEPLOY)
				.setHeader(YarnCloudAppStateMachine.HEADER_CLUSTER_ID, clusterId)
				.build();
		stateMachine.sendEvent(message);
	}

	@Override
	public ModuleStatus status(ModuleDeploymentId id) {
		ModuleStatus status = status().get(id);
		if (status == null) {
			status = ModuleStatus.of(id)
					.with(new YarnModuleInstanceStatus(id.toString(), false,
							Collections.<String, String>emptyMap()))
					.build();
		}
		return status;
	}

	@Override
	public Map<ModuleDeploymentId, ModuleStatus> status() {
		HashMap<ModuleDeploymentId, ModuleStatus> statuses = new HashMap<ModuleDeploymentId, ModuleStatus>();
		for (Entry<String, String> entry : yarnCloudAppService.getClustersStates().entrySet()) {
			ModuleDeploymentId id = clusterIdToModuleDeploymentId(entry.getKey());
			YarnModuleInstanceStatus status = new YarnModuleInstanceStatus(id.toString(), entry
					.getValue().equals("RUNNING"), null);
			statuses.put(id, ModuleStatus.of(id).with(status).build());
		}
		return statuses;
	}

	private static String moduleDeploymentIdToClusterId(ModuleDeploymentId id) {
		return id.getGroup() + ":" + id.getLabel();
	}

	private static ModuleDeploymentId clusterIdToModuleDeploymentId(String clusterId) {
		String[] split = clusterId.split(":");
		if (split.length == 2) {
			return new ModuleDeploymentId(split[0], split[1]);
		} else {
			throw new IllegalArgumentException("Invalid clusterId=[" + clusterId + "]");
		}
	}

}
