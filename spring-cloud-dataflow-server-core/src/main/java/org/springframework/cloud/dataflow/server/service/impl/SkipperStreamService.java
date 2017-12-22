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

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionToDslConverter;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.StreamDeployers;
import org.springframework.cloud.dataflow.server.stream.StreamDeploymentRequest;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifest;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifestReader;
import org.springframework.util.Assert;

import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_KEY_PREFIX;

/**
 * {@link SkipperStreamDeployer} specific {@link AbstractStreamService}.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
public class SkipperStreamService extends AbstractStreamService {

	private static Log logger = LogFactory.getLog(SkipperStreamService.class);

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final SkipperStreamDeployer skipperStreamDeployer;

	private final AppDeploymentRequestCreator appDeploymentRequestCreator;

	private final AppRegistryService appRegistryService;

	public SkipperStreamService(StreamDefinitionRepository streamDefinitionRepository,
			StreamDeploymentRepository streamDeploymentRepository,
			AppRegistryService appRegistryService,
			SkipperStreamDeployer skipperStreamDeployer,
			AppDeploymentRequestCreator appDeploymentRequestCreator) {

		super(streamDefinitionRepository, streamDeploymentRepository, StreamDeployers.skipper);

		Assert.notNull(appRegistryService, "AppRegistryService must not be null");
		Assert.notNull(skipperStreamDeployer, "SkipperStreamDeployer must not be null");
		Assert.notNull(appDeploymentRequestCreator, "AppDeploymentRequestCreator must not be null");
		this.appRegistryService = appRegistryService;
		this.skipperStreamDeployer = skipperStreamDeployer;
		this.appDeploymentRequestCreator = appDeploymentRequestCreator;
	}

	/**
	 * Deploy a stream as defined by its stream name and optional deployment properties.
	 *
	 * @param name the stream name to deploy
	 * @param deploymentProperties the deployment properties for the stream
	 */
	@Override
	public void doDeployStream(String name, Map<String, String> deploymentProperties) {

		StreamDefinition streamDefinition = createStreamDefinitionForDeploy(name);

		// Extract skipper properties
		Map<String, String> skipperDeploymentProperties = getSkipperProperties(deploymentProperties);
		// Create map without any skipper properties
		Map<String, String> deploymentPropertiesToUse = deploymentProperties.entrySet().stream()
				.filter(mapEntry -> !mapEntry.getKey().startsWith(SKIPPER_KEY_PREFIX))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		List<AppDeploymentRequest> appDeploymentRequests = this.appDeploymentRequestCreator
				.createRequests(streamDefinition, deploymentPropertiesToUse);

		DeploymentPropertiesUtils.validateSkipperDeploymentProperties(deploymentPropertiesToUse);
		this.skipperStreamDeployer.deployStream(new StreamDeploymentRequest(streamDefinition.getName(),
				streamDefinition.getDslText(), appDeploymentRequests, skipperDeploymentProperties));
	}

	@Override
	public String doCalculateStreamState(String name) {
		return this.skipperStreamDeployer.calculateStreamState(name);
	}

	@Override
	public void doUndeployStream(String streamName) {
		this.skipperStreamDeployer.undeployStream(streamName);
	}

	@Override
	public void doUpdateStream(String streamName, String releaseName, PackageIdentifier packageIdentifier, Map<String,
			String> updateProperties) {
		String yamlProperties = convertPropertiesToSkipperYaml(streamName, updateProperties);
		Release release = this.skipperStreamDeployer.upgradeStream(releaseName, packageIdentifier, yamlProperties);

		if (release != null) {
			updateStreamDefinitionFromReleaseManifest(streamName, release.getManifest());
		}
		else {
			logger.warn("Missing release after Stream Update!");
		}
	}

	private void updateStreamDefinitionFromReleaseManifest(String streamName, String releaseManifest) {

		List<SpringCloudDeployerApplicationManifest> appManifests =
				new SpringCloudDeployerApplicationManifestReader().read(releaseManifest);

		Map<String, SpringCloudDeployerApplicationManifest> appManifestMap = new HashMap<>();

		for (SpringCloudDeployerApplicationManifest am : appManifests) {
			String name = am.getSpec().getApplicationProperties().get("spring.cloud.dataflow.stream.app.label");
			appManifestMap.put(name, am);
		}

		StreamDefinition streamDefinition = this.streamDefinitionRepository.findOne(streamName);

		LinkedList<StreamAppDefinition> updatedStreamAppDefinitions = new LinkedList<>();
		for (StreamAppDefinition appDefinition : streamDefinition.getAppDefinitions()) {
			StreamAppDefinition.Builder appDefinitionBuilder = StreamAppDefinition.Builder.from(appDefinition);
			SpringCloudDeployerApplicationManifest applicationManifest = appManifestMap.get(appDefinition.getName());
			// overrides app definition properties with those from the release manifest
			appDefinitionBuilder.setProperties(applicationManifest.getSpec().getApplicationProperties());
			updatedStreamAppDefinitions.addLast(appDefinitionBuilder.build(streamDefinition.getName()));

			//Check and register any missing app version
			updateAppVersionIfChanged(appDefinition, applicationManifest);
		}

		String dslText = new StreamDefinitionToDslConverter().toDsl(updatedStreamAppDefinitions);

		StreamDefinition updatedStreamDefinition = new StreamDefinition(streamName, dslText);
		logger.debug("Updated StreamDefinition: " + updatedStreamDefinition);

		// TODO consider adding an explicit UPDATE method to the streamDefRepository
		// Note: Not transactional and can lead to loosing the stream definition
		this.streamDefinitionRepository.delete(updatedStreamDefinition);
		this.streamDefinitionRepository.save(updatedStreamDefinition);
	}

	private void updateAppVersionIfChanged(StreamAppDefinition appDefinition,
			SpringCloudDeployerApplicationManifest appManifest) {

		String version = appManifest.getSpec().getVersion();
		//String name = appManifest.getSpec().getApplicationProperties().get("spring.cloud.dataflow.stream.app.label");
		String name = appDefinition.getRegisteredAppName();
		ApplicationType type = ApplicationType.valueOf(
				appManifest.getSpec().getApplicationProperties().get("spring.cloud.dataflow.stream.app.type"));
		String resource = appManifest.getSpec().getApplicationProperties().get("spring.cloud.dataflow.stream.app.type");

		if (!this.appRegistryService.appExist(name, type, version)) {
			URI metadataUri = null; // TODO Skipper should provide the metadata URI as well
			this.appRegistryService.save(name, type, version, URI.create(resource), metadataUri);
		}
	}

	@Override
	public void doRollbackStream(String streamName, int releaseVersion) {
		this.skipperStreamDeployer.rollbackStream(streamName, releaseVersion);
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
		}
		else {
			return "";
		}
	}

	private Map<String, String> getSkipperProperties(Map<String, String> deploymentProperties) {
		// Extract skipper properties
		return deploymentProperties.entrySet().stream()
				.filter(mapEntry -> mapEntry.getKey().startsWith(SKIPPER_KEY_PREFIX))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	// State
	@Override
	public Map<StreamDefinition, DeploymentState> doState(List<StreamDefinition> streamDefinitions) {
		return this.skipperStreamDeployer.state(streamDefinitions);
	}
}
