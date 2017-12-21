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
package org.springframework.cloud.dataflow.server.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployedException;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployingException;
import org.springframework.cloud.dataflow.server.repository.IncompatibleStreamDeployerException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDeploymentException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.stream.StreamDeployers;
import org.springframework.cloud.dataflow.server.stream.StreamDeploymentRequest;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Performs manipulation on application and deployment properties, expanding shorthand
 * application property values, resolving wildcard deployment properties, and creates a
 * {@link StreamDeploymentRequest}.
 * </p>
 * The {@link AbstractStreamService} deployer is agnostic service. For deploying streams on
 * Skipper use the {@link SkipperStreamService} and for the Simple mode stream deployment use
 * the {@link SimpleStreamService}.
 * </p>
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
public abstract class AbstractStreamService implements StreamService {

	private static Log logger = LogFactory.getLog(AbstractStreamService.class);

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	protected final StreamDefinitionRepository streamDefinitionRepository;

	protected final StreamDeploymentRepository streamDeploymentRepository;

	protected final StreamDeployers streamDeployer;

	public AbstractStreamService(StreamDefinitionRepository streamDefinitionRepository,
			StreamDeploymentRepository streamDeploymentRepository, StreamDeployers streamDeployer) {
		Assert.notNull(streamDefinitionRepository, "StreamDefinitionRepository must not be null");
		Assert.notNull(streamDeploymentRepository, "StreamDeploymentRepository must not be null");
		Assert.notNull(streamDeployer, "StreamDeployer must not be null");
		this.streamDefinitionRepository = streamDefinitionRepository;
		this.streamDeploymentRepository = streamDeploymentRepository;
		this.streamDeployer = streamDeployer;
	}

	@Override
	public void deployStream(String name, Map<String, String> deploymentProperties) {
		if (deploymentProperties == null) {
			deploymentProperties = new HashMap<>();
		}
		doDeployStream(name, deploymentProperties);
	}

	protected abstract void doDeployStream(String name, Map<String, String> deploymentProperties);

	@Override
	public void undeployStream(String streamName) {
		StreamDeployment streamDeployment = this.streamDeploymentRepository.findOne(streamName);
		if (streamDeployment != null) {
			if (this.streamDeployer != StreamDeployers.valueOf(streamDeployment.getDeployerName())) {
				throw new IncompatibleStreamDeployerException(streamDeployer.name());
			}
			doUndeployStream(streamName);
			this.streamDeploymentRepository.delete(streamName);
		}
	}

	protected abstract void doUndeployStream(String streamName);

	@Override
	public void updateStream(String streamName, UpdateStreamRequest updateStreamRequest) {
		updateStream(streamName, updateStreamRequest.getReleaseName(),
				updateStreamRequest.getPackageIdentifier(), updateStreamRequest.getUpdateProperties());
	}

	public void updateStream(String streamName, String releaseName, PackageIdentifier packageIdenfier,
			Map<String, String> updateProperties) {
		StreamDeployment streamDeployment = this.streamDeploymentRepository.findOne(streamName);
		if (streamDeployment == null) {
			throw new NoSuchStreamDeploymentException(streamName);
		}
		if (this.streamDeployer != StreamDeployers.valueOf(streamDeployment.getDeployerName())) {
			throw new IncompatibleStreamDeployerException(streamDeployer.name());
		}

		doUpdateStream(streamName, releaseName, packageIdenfier, updateProperties);
	}

	protected abstract void doUpdateStream(String streamName, String releaseName, PackageIdentifier packageIdenfier,
			Map<String, String> updateProperties);

	@Override
	public void rollbackStream(String streamName, int releaseVersion) {
		Assert.isTrue(StringUtils.hasText(streamName), "Stream name must not be null");
		StreamDeployment streamDeployment = this.streamDeploymentRepository.findOne(streamName);
		if (streamDeployment == null) {
			throw new NoSuchStreamDeploymentException(streamName);
		}
		if (this.streamDeployer != StreamDeployers.valueOf(streamDeployment.getDeployerName())) {
			throw new IncompatibleStreamDeployerException(streamDeployer.name());
		}
		doRollbackStream(streamName, releaseVersion);
	}

	protected abstract void doRollbackStream(String streamName, int releaseVersion);

	protected StreamDefinition createStreamDefinitionForDeploy(String name) {
		StreamDefinition streamDefinition = this.streamDefinitionRepository.findOne(name);
		if (streamDefinition == null) {
			throw new NoSuchStreamDefinitionException(name);
		}

		String status = this.doCalculateStreamState(name);

		if (DeploymentState.deployed.equals(DeploymentState.valueOf(status))) {
			throw new StreamAlreadyDeployedException(name);
		}
		else if (DeploymentState.deploying.equals(DeploymentState.valueOf(status))) {
			throw new StreamAlreadyDeployingException(name);
		}
		return streamDefinition;
	}

	protected abstract String doCalculateStreamState(String name);

	// State
	@Override
	public Map<StreamDefinition, DeploymentState> state(List<StreamDefinition> streamDefinitions) {
		Map<StreamDefinition, DeploymentState> states = new HashMap<>();
		List<StreamDefinition> deployerSpecificStreamDefinitions = new ArrayList<>();
		for (StreamDefinition streamDefinition : streamDefinitions) {
			StreamDeployment streamDeployment = this.streamDeploymentRepository.findOne(streamDefinition.getName());
			if (streamDeployment == null) {
				states.put(streamDefinition, DeploymentState.unknown);
			}
			else if (this.streamDeployer == StreamDeployers.valueOf(streamDeployment.getDeployerName())) {
				deployerSpecificStreamDefinitions.add(streamDefinition);
			}
			else {
				logger.warn("Deployer incompatible stream definition:" + streamDefinition);
			}
		}
		if (!deployerSpecificStreamDefinitions.isEmpty()) {
			states.putAll(this.doState(deployerSpecificStreamDefinitions));
		}
		return states;
	}

	protected abstract Map<StreamDefinition, DeploymentState> doState(List<StreamDefinition> streamDefinitions);
}
