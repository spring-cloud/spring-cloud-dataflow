/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.dataflow.server.stream;

import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.server.controller.StreamDefinitionController;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentKey;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.util.Assert;

/**
 * Uses an AppDeployer instance to deploy the stream.
 *
 * @author Mark Pollack
 */
public class AppDeployerStreamDeployer implements StreamDeployer {

	private static Log logger = LogFactory.getLog(AppDeployerStreamDeployer.class);

	private static String deployLoggingString = "Deploying application named [%s] as part of stream named [%s] "
			+ "with resource URI [%s]";

	/**
	 * The deployer this controller will use to deploy stream apps.
	 */
	private final AppDeployer appDeployer;

	/**
	 * The repository this controller will use for deployment IDs.
	 */
	private final DeploymentIdRepository deploymentIdRepository;

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final StreamDefinitionRepository streamDefinitionRepository;

	public AppDeployerStreamDeployer(AppDeployer appDeployer, DeploymentIdRepository deploymentIdRepository,
			StreamDefinitionRepository streamDefinitionRepository) {
		Assert.notNull(appDeployer, "AppDeployer must not be null");
		Assert.notNull(deploymentIdRepository, "DeploymentIdRepository must not be null");
		Assert.notNull(streamDefinitionRepository, "StreamDefinitionRepository must not be null");
		this.appDeployer = appDeployer;
		this.deploymentIdRepository = deploymentIdRepository;
		this.streamDefinitionRepository = streamDefinitionRepository;
	}

	@Override
	public void deployStream(StreamDeploymentRequest streamDeploymentRequest) {

		for (AppDeploymentRequest appDeploymentRequest : streamDeploymentRequest.getAppDeploymentRequests()) {
			try {
				logger.info(String.format(deployLoggingString, appDeploymentRequest.getDefinition().getName(),
						streamDeploymentRequest.getStreamName(), appDeploymentRequest.getResource().getURI()));
				String id = this.appDeployer.deploy(appDeploymentRequest);
				this.deploymentIdRepository.save(DeploymentKey
						.forAppDeploymentRequest(streamDeploymentRequest.getStreamName(),
								appDeploymentRequest.getDefinition()),
						id);
			}
			catch (Exception e) {
				String errorMessage = String.format(
						"[stream name = %s, application name = %s, application properties = %s",
						streamDeploymentRequest.getStreamName(),
						appDeploymentRequest.getDefinition().getName(),
						appDeploymentRequest.getDefinition().getProperties());
				logger.error(
						String.format("Exception when deploying the app %s: %s", errorMessage, e.getMessage()),
						e);
			}
		}
	}

	@Override
	public String calculateStreamState(String streamName) {
		Set<DeploymentState> appStates = EnumSet.noneOf(DeploymentState.class);
		StreamDefinition stream = this.streamDefinitionRepository.findOne(streamName);
		for (StreamAppDefinition appDefinition : stream.getAppDefinitions()) {
			String key = DeploymentKey.forStreamAppDefinition(appDefinition);
			String id = this.deploymentIdRepository.findOne(key);
			if (id != null) {
				AppStatus status = this.appDeployer.status(id);
				appStates.add(status.getState());
			}
			else {
				appStates.add(DeploymentState.undeployed);
			}
		}
		return StreamDefinitionController.aggregateState(appStates).toString();
	}
}
