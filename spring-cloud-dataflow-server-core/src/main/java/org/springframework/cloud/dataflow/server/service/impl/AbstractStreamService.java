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
package org.springframework.cloud.dataflow.server.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployedException;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployingException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.stream.StreamDeployers;
import org.springframework.cloud.dataflow.server.stream.StreamDeploymentRequest;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.util.Assert;

/**
 * Performs manipulation on application and deployment properties, expanding shorthand
 * application property values, resolving wildcard deployment properties, and creates a
 * {@link StreamDeploymentRequest}.
 * </p>
 * The {@link AbstractStreamService} deployer is agnostic. For deploying streams on
 * Skipper use the {@link SkipperStreamService} and for the AppDeploy stream deployment use
 * the {@link AppDeployerStreamService}.
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

	protected final StreamDeployers streamDeployer;

	public AbstractStreamService(StreamDefinitionRepository streamDefinitionRepository, StreamDeployers streamDeployer) {
		Assert.notNull(streamDefinitionRepository, "StreamDefinitionRepository must not be null");
		Assert.notNull(streamDeployer, "StreamDeployer must not be null");
		this.streamDefinitionRepository = streamDefinitionRepository;
		this.streamDeployer = streamDeployer;
	}

	@Override
	public void deployStream(String name, Map<String, String> deploymentProperties) {
		if (deploymentProperties == null) {
			deploymentProperties = new HashMap<>();
		}
		StreamDefinition streamDefinition = this.streamDefinitionRepository.findOne(name);
		if (streamDefinition == null) {
			throw new NoSuchStreamDefinitionException(name);
		}

		DeploymentState status = this.doCalculateStreamState(name);

		if (DeploymentState.deployed == status) {
			throw new StreamAlreadyDeployedException(name);
		}
		else if (DeploymentState.deploying  == status) {
			throw new StreamAlreadyDeployingException(name);
		}
		doDeployStream(streamDefinition, deploymentProperties);
	}

	protected abstract void doDeployStream(StreamDefinition streamDefinition, Map<String, String> deploymentProperties);

	protected abstract DeploymentState doCalculateStreamState(String name);
}
