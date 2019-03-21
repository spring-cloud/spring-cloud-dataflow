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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionToDslConverter;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.rest.SkipperStream;
import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.audit.domain.AuditActionType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditOperationType;
import org.springframework.cloud.dataflow.server.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.SkipperStreamService;
import org.springframework.cloud.dataflow.server.service.StreamValidationService;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.StreamDeploymentRequest;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifest;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifestReader;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationSpec;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An implementation of {@link SkipperStreamService}.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @author Gunnar Hillert
 */
@Transactional
public class DefaultSkipperStreamService extends AbstractStreamService implements SkipperStreamService {

	public static final String DEFAULT_SKIPPER_PACKAGE_VERSION = "1.0.0";

	private static Log logger = LogFactory.getLog(DefaultSkipperStreamService.class);

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final SkipperStreamDeployer skipperStreamDeployer;

	private final AppDeploymentRequestCreator appDeploymentRequestCreator;

	public DefaultSkipperStreamService(StreamDefinitionRepository streamDefinitionRepository,
			SkipperStreamDeployer skipperStreamDeployer,
			AppDeploymentRequestCreator appDeploymentRequestCreator,
			StreamValidationService streamValidationService,
			AuditRecordService auditRecordService) {
		super(streamDefinitionRepository, streamValidationService, auditRecordService);
		Assert.notNull(skipperStreamDeployer, "SkipperStreamDeployer must not be null");
		Assert.notNull(appDeploymentRequestCreator, "AppDeploymentRequestCreator must not be null");
		this.skipperStreamDeployer = skipperStreamDeployer;
		this.appDeploymentRequestCreator = appDeploymentRequestCreator;
	}

	/**
	 * Deploy a stream as defined by its stream name and optional deployment properties.
	 *
	 * @param streamDefinition the stream definition to deploy
	 * @param deploymentProperties the deployment properties for the stream
	 */
	@Override
	public void doDeployStream(StreamDefinition streamDefinition, Map<String, String> deploymentProperties) {
		// Extract skipper properties
		Map<String, String> skipperDeploymentProperties = getSkipperProperties(deploymentProperties);

		if (!skipperDeploymentProperties.containsKey(SkipperStream.SKIPPER_PACKAGE_VERSION)) {
			skipperDeploymentProperties.put(SkipperStream.SKIPPER_PACKAGE_VERSION, DEFAULT_SKIPPER_PACKAGE_VERSION);
		}

		// Create map without any skipper properties
		Map<String, String> deploymentPropertiesToUse = deploymentProperties.entrySet().stream()
				.filter(mapEntry -> !mapEntry.getKey().startsWith(SkipperStream.SKIPPER_KEY_PREFIX))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		List<AppDeploymentRequest> appDeploymentRequests = this.appDeploymentRequestCreator
				.createRequests(streamDefinition, deploymentPropertiesToUse);

		DeploymentPropertiesUtils.validateSkipperDeploymentProperties(deploymentPropertiesToUse);

		StreamDeploymentRequest streamDeploymentRequest = new StreamDeploymentRequest(streamDefinition.getName(),
				streamDefinition.getDslText(), appDeploymentRequests, skipperDeploymentProperties);

		Release release = this.skipperStreamDeployer.deployStream(streamDeploymentRequest);

		this.auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.STREAM,
				AuditActionType.DEPLOY,
				streamDefinition.getName(), this.auditServiceUtils.convertStreamDefinitionToAuditData(streamDefinition, deploymentProperties));

		if (release != null) {
			updateStreamDefinitionFromReleaseManifest(streamDefinition.getName(), release.getManifest().getData());
		}
		else {
			logger.error("Missing skipper release after Stream deploy!");
		}
	}

	@Override
	public DeploymentState doCalculateStreamState(String name) {
		return this.skipperStreamDeployer.streamState(name);
	}

	@Override
	public void undeployStream(String streamName) {
		final StreamDefinition streamDefinition = this.streamDefinitionRepository.findOne(streamName);

		this.skipperStreamDeployer.undeployStream(streamName);

		auditRecordService.populateAndSaveAuditRecord(
				AuditOperationType.STREAM, AuditActionType.UNDEPLOY,
				streamDefinition.getName(), this.auditServiceUtils.convertStreamDefinitionToAuditData(streamDefinition));
	}

	private void updateStreamDefinitionFromReleaseManifest(String streamName, String releaseManifest) {

		List<SpringCloudDeployerApplicationManifest> appManifests = new SpringCloudDeployerApplicationManifestReader()
				.read(releaseManifest);

		Map<String, SpringCloudDeployerApplicationManifest> appManifestMap = new HashMap<>();

		for (SpringCloudDeployerApplicationManifest am : appManifests) {
			String name = am.getSpec().getApplicationProperties().get(DataFlowPropertyKeys.STREAM_APP_LABEL);
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
		}

		String dslText = new StreamDefinitionToDslConverter().toDsl(updatedStreamAppDefinitions);

		StreamDefinition updatedStreamDefinition = new StreamDefinition(streamName, dslText);
		logger.debug("Updated StreamDefinition: " + updatedStreamDefinition);

		// TODO consider adding an explicit UPDATE method to the streamDefRepository
		// Note: Not transactional and can lead to loosing the stream definition
		this.streamDefinitionRepository.delete(updatedStreamDefinition);
		this.streamDefinitionRepository.save(updatedStreamDefinition);
		this.auditRecordService.populateAndSaveAuditRecord(
				AuditOperationType.STREAM, AuditActionType.UPDATE, streamName, this.auditServiceUtils.convertStreamDefinitionToAuditData(streamDefinition));
	}

	@Override
	public void updateStream(String streamName, UpdateStreamRequest updateStreamRequest) {
		updateStream(streamName, updateStreamRequest.getReleaseName(),
				updateStreamRequest.getPackageIdentifier(), updateStreamRequest.getUpdateProperties(),
				updateStreamRequest.isForce(), updateStreamRequest.getAppNames());
	}

	public void updateStream(String streamName, String releaseName, PackageIdentifier packageIdentifier,
			Map<String, String> updateProperties, boolean force, List<String> appNames) {

		StreamDefinition streamDefinition = this.streamDefinitionRepository.findOne(streamName);
		if (streamDefinition == null) {
			throw new NoSuchStreamDefinitionException(streamName);
		}

		String updateYaml = convertPropertiesToSkipperYaml(streamDefinition, updateProperties);
		Release release = this.skipperStreamDeployer.upgradeStream(releaseName, packageIdentifier, updateYaml,
				force, appNames);
		if (release != null) {
			updateStreamDefinitionFromReleaseManifest(streamName, release.getManifest().getData());

			final String sanatizedUpdateYaml = convertPropertiesToSkipperYaml(streamDefinition,
					this.auditServiceUtils.sanitizeProperties(updateProperties));

			final Map<String, Object> auditedData = new HashMap<>(3);
			auditedData.put("releaseName", releaseName);
			auditedData.put("packageIdentifier", packageIdentifier);
			auditedData.put("updateYaml", sanatizedUpdateYaml);

			this.auditRecordService.populateAndSaveAuditRecordUsingMapData(
					AuditOperationType.STREAM, AuditActionType.UPDATE,
					streamName, auditedData);
		}
		else {
			logger.error("Missing release after Stream Update!");
		}

	}

	@Override
	public void rollbackStream(String streamName, int releaseVersion) {
		Assert.isTrue(StringUtils.hasText(streamName), "Stream name must not be null");
		Release release = this.skipperStreamDeployer.rollbackStream(streamName, releaseVersion);
		if (release != null && release.getManifest() != null) {
			updateStreamDefinitionFromReleaseManifest(streamName, release.getManifest().getData());
		}
		this.auditRecordService.populateAndSaveAuditRecord(AuditOperationType.STREAM, AuditActionType.ROLLBACK,
				streamName, "Rollback to version: " + releaseVersion);
	}

	String convertPropertiesToSkipperYaml(StreamDefinition streamDefinition,
			Map<String, String> updateProperties) {

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
				specMap.put(SpringCloudDeployerApplicationSpec.APPLICATION_PROPERTIES_STRING,
						appDeploymentRequest.getDefinition().getProperties());
			}
			if (!appDeploymentRequest.getDeploymentProperties().isEmpty()) {
				hasProps = true;
				specMap.put(SpringCloudDeployerApplicationSpec.DEPLOYMENT_PROPERTIES_STRING,
						appDeploymentRequest.getDeploymentProperties());
			}
			if (appDeploymentRequest.getCommandlineArguments().size() == 1) {
				hasProps = true;
				String version = appDeploymentRequest.getCommandlineArguments().get(0);
				this.skipperStreamDeployer.validateAppVersionIsRegistered(streamDefinition, appDeploymentRequest,
						version);
				specMap.put("version", version);
			}
			if (hasProps) {
				appMap.put(SpringCloudDeployerApplicationManifest.SPEC_STRING, specMap);
			}
			if (appMap.size() != 0) {
				skipperConfigValuesMap.put(appName, appMap);
			}
		}
		if (!skipperConfigValuesMap.isEmpty()) {
			DumperOptions dumperOptions = new DumperOptions();
			dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			dumperOptions.setPrettyFlow(true);
			dumperOptions.setLineBreak(DumperOptions.LineBreak.getPlatformLineBreak());
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
				.filter(mapEntry -> mapEntry.getKey().startsWith(SkipperStream.SKIPPER_KEY_PREFIX))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public Map<StreamDefinition, DeploymentState> state(List<StreamDefinition> streamDefinitions) {
		return this.skipperStreamDeployer.streamsStates(streamDefinitions);
	}

	@Override
	public String manifest(String name, int version) {
		return this.skipperStreamDeployer.manifest(name, version);
	}

	@Override
	public Collection<Release> history(String releaseName) {
		return this.skipperStreamDeployer.history(releaseName);
	}

	@Override
	public Collection<Deployer> platformList() {
		return this.skipperStreamDeployer.platformList();
	}

	@Override
	public StreamDeployment info(String streamName) {
		return this.skipperStreamDeployer.getStreamInfo(streamName);
	}
}
