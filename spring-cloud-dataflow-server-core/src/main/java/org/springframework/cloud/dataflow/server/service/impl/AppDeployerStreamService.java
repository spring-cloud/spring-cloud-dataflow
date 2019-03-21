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
package org.springframework.cloud.dataflow.server.service.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.repository.IncompatibleStreamDeployerException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.stream.AppDeployerStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.StreamDeployers;
import org.springframework.cloud.dataflow.server.stream.StreamDeploymentRequest;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.util.Assert;

import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_KEY_PREFIX;

/**
 * {@link AppDeployerStreamDeployer} specific {@link AbstractStreamService}.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
public class AppDeployerStreamService extends AbstractStreamService {

	private static Log logger = LogFactory.getLog(AppDeployerStreamService.class);

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final AppDeployerStreamDeployer appDeployerStreamDeployer;

	private final AppDeploymentRequestCreator appDeploymentRequestCreator;

	public AppDeployerStreamService(StreamDefinitionRepository streamDefinitionRepository,
			AppDeployerStreamDeployer appDeployerStreamDeployer,
			AppDeploymentRequestCreator appDeploymentRequestCreator) {
		super(streamDefinitionRepository, StreamDeployers.appdeployer);
		Assert.notNull(appDeployerStreamDeployer, "AppDeployerStreamDeployer must not be null");
		Assert.notNull(appDeploymentRequestCreator, "AppDeploymentRequestCreator must not be null");
		this.appDeployerStreamDeployer = appDeployerStreamDeployer;
		this.appDeploymentRequestCreator = appDeploymentRequestCreator;
	}

	@Override
	public DeploymentState doCalculateStreamState(String name) {
		return this.appDeployerStreamDeployer.calculateStreamState(name);
	}

	@Override
	public void doDeployStream(StreamDefinition streamDefinition, Map<String, String> deploymentProperties) {
		Map<String, String> deploymentPropertiesToUse = deploymentProperties.entrySet().stream()
				.filter(mapEntry -> !mapEntry.getKey().startsWith(SKIPPER_KEY_PREFIX))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		List<AppDeploymentRequest> appDeploymentRequests = this.appDeploymentRequestCreator
				.createRequests(streamDefinition, deploymentPropertiesToUse);

		DeploymentPropertiesUtils.validateDeploymentProperties(deploymentPropertiesToUse);

		this.appDeployerStreamDeployer.deployStream(new StreamDeploymentRequest(streamDefinition.getName(),
				streamDefinition.getDslText(), appDeploymentRequests, new HashMap<>()));
	}

	@Override
	public void undeployStream(String streamName) {
		this.appDeployerStreamDeployer.undeployStream(streamName);
	}

	// State
	@Override
	public Map<StreamDefinition, DeploymentState> state(List<StreamDefinition> streamDefinitions) {
		return this.appDeployerStreamDeployer.state(streamDefinitions);
	}

	@Override
	public void updateStream(String streamName, UpdateStreamRequest updateStreamRequest) {
		throw new IncompatibleStreamDeployerException(StreamDeployers.appdeployer.toString());
	}

	@Override
	public void rollbackStream(String streamName, int releaseVersion) {
		throw new IncompatibleStreamDeployerException(StreamDeployers.appdeployer.toString());
	}

	@Override
	public String manifest(String releaseName, int releaseVersion) {
		throw new IncompatibleStreamDeployerException(StreamDeployers.appdeployer.toString());
	}

	@Override
	public Collection<Release> history(String releaseName) {
		throw new IncompatibleStreamDeployerException(StreamDeployers.appdeployer.toString());
	}

	@Override
	public Collection<Deployer> platformList() {
		throw new IncompatibleStreamDeployerException(StreamDeployers.appdeployer.toString());
	}

	@Override
	public StreamDeployment info(String streamName) {
		return this.appDeployerStreamDeployer.getStreamInfo(streamName);
	}
}
