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
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployedException;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployingException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDeploymentException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.stream.AppDeployerStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.StreamDeployers;
import org.springframework.cloud.dataflow.server.stream.StreamDeploymentRequest;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_ENABLED_PROPERTY_KEY;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_KEY_PREFIX;

/**
 * Performs manipulation on application and deployment properties, expanding shorthand
 * application property values, resolving wildcard deployment properties, and creates a
 * {@link StreamDeploymentRequest}.
 * </p>
 * If the deployment uses Skipper, delegate to {@link SkipperStreamDeployer}, otherwise
 * use {@link AppDeployerStreamDeployer}.
 * </p>
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@Service
public class DefaultStreamService implements StreamService {

	private static Log logger = LogFactory.getLog(DefaultStreamService.class);

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final StreamDefinitionRepository streamDefinitionRepository;

	private final StreamDeploymentRepository streamDeploymentRepository;

	private final AppDeployerStreamDeployer appDeployerStreamDeployer;

	private final SkipperStreamDeployer skipperStreamDeployer;

	private final AppDeploymentRequestCreator appDeploymentRequestCreator;

	public DefaultStreamService(StreamDefinitionRepository streamDefinitionRepository,
			StreamDeploymentRepository streamDeploymentRepository,
			AppDeployerStreamDeployer appDeployerStreamDeployer,
			SkipperStreamDeployer skipperStreamDeployer,
			AppDeploymentRequestCreator appDeploymentRequestCreator) {
		Assert.notNull(streamDefinitionRepository, "StreamDefinitionRepository must not be null");
		Assert.notNull(streamDeploymentRepository, "StreamDeploymentRepository must not be null");
		Assert.notNull(appDeployerStreamDeployer, "AppDeployerStreamDeployer must not be null");
		Assert.notNull(skipperStreamDeployer, "SkipperStreamDeployer must not be null");
		Assert.notNull(appDeploymentRequestCreator, "AppDeploymentRequestCreator must not be null");
		this.streamDefinitionRepository = streamDefinitionRepository;
		this.streamDeploymentRepository = streamDeploymentRepository;
		this.appDeployerStreamDeployer = appDeployerStreamDeployer;
		this.skipperStreamDeployer = skipperStreamDeployer;
		this.appDeploymentRequestCreator = appDeploymentRequestCreator;
	}

	@Override
	public void deployStream(String name, Map<String, String> deploymentProperties) {
		if (deploymentProperties == null) {
			deploymentProperties = new HashMap<>();
		}
		deployStreamWithDefinition(createStreamDefinitionForDeploy(name, deploymentProperties), deploymentProperties);
	}

	@Override
	public void undeployStream(String streamName) {
		StreamDeployment streamDeployment = this.streamDeploymentRepository.findOne(streamName);
		if (streamDeployment != null) {
			switch (StreamDeployers.valueOf(streamDeployment.getDeployerName())) {
			case appdeployer: {
				this.appDeployerStreamDeployer.undeployStream(streamName);
				break;
			}
			case skipper: {
				this.skipperStreamDeployer.undeployStream(streamName);
				break;
			}
			}
			this.streamDeploymentRepository.delete(streamName);
		}
	}

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
		if (streamDeployment.getDeployerName().equals(StreamDeployers.skipper.name())) {
			String yaml = convertPropertiesToSkipperYaml(streamName, updateProperties);
			this.skipperStreamDeployer.upgradeStream(releaseName, packageIdenfier, yaml);
		}
		else {
			throw new IllegalStateException("Can only update stream when using the Skipper stream deployer.");
		}
	}

	@Override
	public void rollbackStream(String streamName, int releaseVersion) {
		Assert.isTrue(StringUtils.hasText(streamName), "Stream name must not be null");
		StreamDeployment streamDeployment = this.streamDeploymentRepository.findOne(streamName);
		if (streamDeployment == null) {
			throw new NoSuchStreamDeploymentException(streamName);
		}
		if (streamDeployment.getDeployerName().equals(StreamDeployers.skipper.name())) {
			this.skipperStreamDeployer.rollbackStream(streamName, releaseVersion);
		}
		else {
			throw new IllegalStateException("Can only rollback stream when using the Skipper stream deployer.");
		}
	}

	public String convertPropertiesToSkipperYaml(String streamName, Map<String, String> updateProperties) {
		StreamDefinition streamDefinition = this.streamDefinitionRepository.findOne(streamName);
		if (streamDefinition == null) {
			throw new NoSuchStreamDefinitionException(streamName);
		}

		List<AppDeploymentRequest> appDeploymentRequests = this.appDeploymentRequestCreator
				.createUpdateRequests(streamDefinition, updateProperties);
		Map<String, Object> skipperConfigValuesMap = new HashMap<>();
		for (AppDeploymentRequest appDeploymentRequest : appDeploymentRequests) {
			boolean hasProps = false;
			String appName = appDeploymentRequest.getDefinition().getName();
			Map<String, Object> appMap = new HashMap<>();
			Map<String, Object> specMap = new HashMap<>();
			if (!appDeploymentRequest.getDefinition().getProperties().isEmpty()) {
				hasProps = true;
				specMap.put("applicationProperties", appDeploymentRequest.getDefinition().getProperties());
			}
			if (!appDeploymentRequest.getDeploymentProperties().isEmpty()) {
				hasProps = true;
				specMap.put("deploymentProperties", appDeploymentRequest.getDeploymentProperties());
			}
			if (hasProps) {
				appMap.put("spec", specMap);
			}
			if (appDeploymentRequest.getCommandlineArguments().size() == 1) {
				appMap.put("version", appDeploymentRequest.getCommandlineArguments().get(0));
			}
			if (appMap.size() != 0) {
				skipperConfigValuesMap.put(appName, appMap);
			}
		}
		if (!skipperConfigValuesMap.isEmpty()) {
			DumperOptions dumperOptions = new DumperOptions();
			dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			dumperOptions.setPrettyFlow(true);
			Yaml yaml = new Yaml(dumperOptions);
			return yaml.dump(skipperConfigValuesMap);
		} else {
			return "";
		}
	}

	private StreamDefinition createStreamDefinitionForDeploy(String name,
			Map<String, String> deploymentProperties) {
		StreamDefinition streamDefinition = this.streamDefinitionRepository.findOne(name);
		if (streamDefinition == null) {
			throw new NoSuchStreamDefinitionException(name);
		}

		Map<String, String> skipperDeploymentProperties = getSkipperProperties(deploymentProperties);
		String status;
		if (skipperDeploymentProperties.containsKey(SKIPPER_ENABLED_PROPERTY_KEY)) {
			status = this.skipperStreamDeployer.calculateStreamState(name);
		}
		else {
			status = this.appDeployerStreamDeployer.calculateStreamState(name);
		}

		if (DeploymentState.deployed.equals(DeploymentState.valueOf(status))) {
			throw new StreamAlreadyDeployedException(name);
		}
		else if (DeploymentState.deploying.equals(DeploymentState.valueOf(status))) {
			throw new StreamAlreadyDeployingException(name);
		}
		return streamDefinition;
	}

	private Map<String, String> getSkipperProperties(Map<String, String> deploymentProperties) {
		// Extract skipper properties
		return deploymentProperties.entrySet().stream()
				.filter(mapEntry -> mapEntry.getKey().startsWith(SKIPPER_KEY_PREFIX))
				.collect(Collectors.toMap(mapEntry -> mapEntry.getKey(), mapEntry -> mapEntry.getValue()));
	}

	/**
	 * Deploy a stream as defined by its {@link StreamDefinition} and optional deployment
	 * properties.
	 *
	 * @param streamDefinition the stream to deploy
	 * @param streamDeploymentProperties the deployment properties for the stream
	 */
	private void deployStreamWithDefinition(StreamDefinition streamDefinition,
			Map<String, String> streamDeploymentProperties) {

		// Extract skipper properties
		Map<String, String> skipperDeploymentProperties = getSkipperProperties(streamDeploymentProperties);
		// Create map without any skipper properties
		Map<String, String> deploymentPropertiesToUse = streamDeploymentProperties.entrySet().stream()
				.filter(mapEntry -> !mapEntry.getKey().startsWith(SKIPPER_KEY_PREFIX))
				.collect(Collectors.toMap(mapEntry -> mapEntry.getKey(), mapEntry -> mapEntry.getValue()));

		List<AppDeploymentRequest> appDeploymentRequests = this.appDeploymentRequestCreator
				.createRequests(streamDefinition, deploymentPropertiesToUse);


		if (skipperDeploymentProperties.containsKey(SKIPPER_ENABLED_PROPERTY_KEY)) {
			DeploymentPropertiesUtils.validateSkipperDeploymentProperties(deploymentPropertiesToUse);
			this.skipperStreamDeployer.deployStream(new StreamDeploymentRequest(streamDefinition.getName(),
					streamDefinition.getDslText(), appDeploymentRequests, skipperDeploymentProperties));
		}
		else {
			DeploymentPropertiesUtils.validateDeploymentProperties(deploymentPropertiesToUse);
			this.appDeployerStreamDeployer.deployStream(new StreamDeploymentRequest(streamDefinition.getName(),
					streamDefinition.getDslText(), appDeploymentRequests, new HashMap<>()));
		}
	}

	// State

	@Override
	public Map<StreamDefinition, DeploymentState> state(List<StreamDefinition> streamDefinitions) {
		Map<StreamDefinition, DeploymentState> states = new HashMap<>();
		List<StreamDefinition> skipperStreams = new ArrayList<>();
		List<StreamDefinition> appDeployerStreams = new ArrayList<>();
		for (StreamDefinition streamDefinition : streamDefinitions) {
			StreamDeployment streamDeployment = this.streamDeploymentRepository.findOne(streamDefinition.getName());
			if (streamDeployment == null) {
				states.put(streamDefinition, DeploymentState.unknown);
			}
			else {
				if (streamDeployment.getDeployerName().equals(StreamDeployers.skipper.name())) {
					skipperStreams.add(streamDefinition);
				}
				else if (streamDeployment.getDeployerName().equals(StreamDeployers.appdeployer.name())) {
					appDeployerStreams.add(streamDefinition);
				}
				else {
					logger.error("Unknown deployer " + streamDeployment.getDeployerName() +
							" for " + streamDeployment.getStreamName());
				}
			}
		}
		if (!skipperStreams.isEmpty()) {
			states.putAll(this.skipperStreamDeployer.state(skipperStreams));
		}
		if (!appDeployerStreams.isEmpty()) {
			states.putAll(this.appDeployerStreamDeployer.state(appDeployerStreams));
		}
		return states;
	}
}
