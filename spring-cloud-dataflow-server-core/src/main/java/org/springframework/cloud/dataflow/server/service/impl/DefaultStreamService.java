/*
 * Copyright 2017-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.audit.service.AuditServiceUtils;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.core.dsl.ParseException;
import org.springframework.cloud.dataflow.core.dsl.StreamNode;
import org.springframework.cloud.dataflow.rest.SkipperStream;
import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployedException;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployingException;
import org.springframework.cloud.dataflow.server.controller.support.InvalidStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.DuplicateStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.service.StreamValidationService;
import org.springframework.cloud.dataflow.server.service.ValidationStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Performs manipulation on application and deployment properties, expanding shorthand
 * application property values, resolving wildcard deployment properties, and creates a
 * {@link StreamDeploymentRequest}.
 * <p>
 * The {@link StreamService} deployer is agnostic. For deploying streams on Skipper use
 * the {@link DefaultStreamService}.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @author Gunnar Hillert
 * @author Chris Schaefer
 */
@Transactional
public class DefaultStreamService implements StreamService {

	public static final String DEFAULT_SKIPPER_PACKAGE_VERSION = "1.0.0";

	private static final Logger logger = LoggerFactory.getLogger(DefaultStreamService.class);

	private static final Pattern STREAM_NAME_PATTERN = Pattern.compile("[a-zA-Z]([-a-zA-Z0-9]*[a-zA-Z0-9])?");
	private static final String STREAM_NAME_VALIDATION_MSG = "Stream name must consist of alphanumeric characters " +
			"or '-', start with an alphabetic character, and end with an alphanumeric character (e.g. 'my-name', " +
			"or 'abc-123')";

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	protected final StreamDefinitionRepository streamDefinitionRepository;

	protected final AuditRecordService auditRecordService;

	protected final AuditServiceUtils auditServiceUtils;

	protected final StreamValidationService streamValidationService;

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final SkipperStreamDeployer skipperStreamDeployer;

	private final AppDeploymentRequestCreator appDeploymentRequestCreator;

	private final StreamDefinitionService streamDefinitionService;

	public DefaultStreamService(StreamDefinitionRepository streamDefinitionRepository,
			SkipperStreamDeployer skipperStreamDeployer,
			AppDeploymentRequestCreator appDeploymentRequestCreator,
			StreamValidationService streamValidationService,
			AuditRecordService auditRecordService,
			StreamDefinitionService streamDefinitionService) {

		Assert.notNull(skipperStreamDeployer, "SkipperStreamDeployer must not be null");
		Assert.notNull(appDeploymentRequestCreator, "AppDeploymentRequestCreator must not be null");
		Assert.notNull(streamDefinitionRepository, "StreamDefinitionRepository must not be null");
		Assert.notNull(streamValidationService, "StreamValidationService must not be null");
		Assert.notNull(auditRecordService, "AuditRecordService must not be null");
		Assert.notNull(streamDefinitionService, "StreamDefinitionService must not be null");

		this.skipperStreamDeployer = skipperStreamDeployer;
		this.appDeploymentRequestCreator = appDeploymentRequestCreator;
		this.streamDefinitionRepository = streamDefinitionRepository;
		this.streamValidationService = streamValidationService;
		this.auditRecordService = auditRecordService;
		this.auditServiceUtils = new AuditServiceUtils();
		this.streamDefinitionService = streamDefinitionService;

	}

	/**
	 * Deploy a stream as defined by its stream name and optional deployment properties.
	 *
	 * @param streamDefinition the stream definition to deploy
	 * @param deploymentProperties the deployment properties for the stream
	 * @return return a skipper release {@link Release}
	 */
	private Release doDeployStream(StreamDefinition streamDefinition, Map<String, String> deploymentProperties) {
		// Extract skipper properties
		Map<String, String> skipperDeploymentProperties = getSkipperProperties(deploymentProperties);

		if (!skipperDeploymentProperties.containsKey(SkipperStream.SKIPPER_PACKAGE_VERSION)) {
			skipperDeploymentProperties.put(SkipperStream.SKIPPER_PACKAGE_VERSION, DEFAULT_SKIPPER_PACKAGE_VERSION);
		}

		// Create map without any skipper properties
		Map<String, String> deploymentPropertiesToUse = deploymentProperties.entrySet().stream()
				.filter(mapEntry -> !mapEntry.getKey().startsWith(SkipperStream.SKIPPER_KEY_PREFIX))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		final String platformName = skipperDeploymentProperties.getOrDefault(SkipperStream.SKIPPER_PLATFORM_NAME, "default");
		String platformType = this.platformList().stream()
				.filter(deployer -> deployer.getName().equalsIgnoreCase(platformName))
				.map(Deployer::getType)
				.findFirst()
				.orElse("unknown");

		List<AppDeploymentRequest> appDeploymentRequests = this.appDeploymentRequestCreator
				.createRequests(streamDefinition, deploymentPropertiesToUse, platformType);

		DeploymentPropertiesUtils.validateSkipperDeploymentProperties(deploymentPropertiesToUse);

		StreamDeploymentRequest streamDeploymentRequest = new StreamDeploymentRequest(streamDefinition.getName(),
				streamDefinition.getDslText(), appDeploymentRequests, skipperDeploymentProperties);

		Release release = this.skipperStreamDeployer.deployStream(streamDeploymentRequest);
		if (release != null) {
			updateStreamDefinitionFromReleaseManifest(streamDefinition.getName(), release.getManifest().getData());
		}
		else {
			logger.error("Missing skipper release after Stream deploy!");
		}

		return release;
	}

	public DeploymentState doCalculateStreamState(String name) {
		return this.skipperStreamDeployer.streamState(name);
	}

	@Override
	public void undeployStream(String streamName) {
		StreamDefinition streamDefinition = this.streamDefinitionRepository.findById(streamName)
				.orElseThrow(() -> new NoSuchStreamDefinitionException(streamName));

		this.skipperStreamDeployer.undeployStream(streamName);

		auditRecordService.populateAndSaveAuditRecord(
				AuditOperationType.STREAM, AuditActionType.UNDEPLOY,
				streamDefinition.getName(), this.streamDefinitionService.redactDsl(streamDefinition), null);
	}

	private void updateStreamDefinitionFromReleaseManifest(String streamName, String releaseManifest) {

		List<SpringCloudDeployerApplicationManifest> appManifests = new SpringCloudDeployerApplicationManifestReader()
				.read(releaseManifest);

		Map<String, SpringCloudDeployerApplicationManifest> appManifestMap = new HashMap<>();

		for (SpringCloudDeployerApplicationManifest am : appManifests) {
			String name = am.getSpec().getApplicationProperties().get(DataFlowPropertyKeys.STREAM_APP_LABEL);
			appManifestMap.put(name, am);
		}

		StreamDefinition streamDefinition = this.streamDefinitionRepository.findById(streamName)
				.orElseThrow(() -> new NoSuchStreamDefinitionException(streamName));

		LinkedList<StreamAppDefinition> updatedStreamAppDefinitions = new LinkedList<>();
		for (StreamAppDefinition appDefinition : this.streamDefinitionService.getAppDefinitions(streamDefinition)) {
			StreamAppDefinition.Builder appDefinitionBuilder = StreamAppDefinition.Builder.from(appDefinition);
			SpringCloudDeployerApplicationManifest applicationManifest = appManifestMap.get(appDefinition.getName());
			// overrides app definition properties with those from the release manifest
			appDefinitionBuilder.setProperties(applicationManifest.getSpec().getApplicationProperties());
			updatedStreamAppDefinitions.addLast(appDefinitionBuilder.build(streamDefinition.getName()));
		}

		StreamDefinition updatedStreamDefinition = new StreamDefinition(streamName,
				this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), updatedStreamAppDefinitions),
				streamDefinition.getOriginalDslText(), streamDefinition.getDescription());
		logger.debug("Updated StreamDefinition: " + updatedStreamDefinition);

		// TODO consider adding an explicit UPDATE method to the streamDefRepository
		// Note: Not transactional and can lead to loosing the stream definition
		this.streamDefinitionRepository.delete(updatedStreamDefinition);
		this.streamDefinitionRepository.save(updatedStreamDefinition);
		this.auditRecordService.populateAndSaveAuditRecord(
				AuditOperationType.STREAM, AuditActionType.UPDATE, streamName,
				updatedStreamDefinition.getDslText(), null);
	}

	@Override
	public void scaleApplicationInstances(String streamName, String appName, int count,
			Map<String, String> properties) {
		// Skipper expects app names / labels not deployment ids
		logger.info(String.format("Scale %s:%s to %s with properties: %s", streamName, appName, count, properties));
		this.skipperStreamDeployer.scale(streamName, appName, count, properties);
	}

	@Override
	public void updateStream(String streamName, UpdateStreamRequest updateStreamRequest) {
		updateStream(streamName, updateStreamRequest.getReleaseName(),
				updateStreamRequest.getPackageIdentifier(), updateStreamRequest.getUpdateProperties(),
				updateStreamRequest.isForce(), updateStreamRequest.getAppNames());
	}

	public void updateStream(String streamName, String releaseName, PackageIdentifier packageIdentifier,
			Map<String, String> updateProperties, boolean force, List<String> appNames) {

		StreamDefinition streamDefinition = this.streamDefinitionRepository.findById(streamName)
				.orElseThrow(() -> new NoSuchStreamDefinitionException(streamName));

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
					streamName, auditedData, release.getPlatformName());
		}
		else {
			logger.error("Missing release after Stream Update!");
		}

	}

	@Override
	public void rollbackStream(String streamName, int releaseVersion) {
		Assert.isTrue(StringUtils.hasText(streamName), "Stream name must not be null");
		Release release = this.skipperStreamDeployer.rollbackStream(streamName, releaseVersion);
		String platformName = null;

		if (release != null) {
			platformName = release.getPlatformName();
			if (release.getManifest() != null) {
				updateStreamDefinitionFromReleaseManifest(streamName, release.getManifest().getData());
			}
		}

		this.auditRecordService.populateAndSaveAuditRecord(AuditOperationType.STREAM, AuditActionType.ROLLBACK,
				streamName, "Rollback to version: " + releaseVersion, platformName);
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

	/**
	 * Create a new stream.
	 *
	 * @param streamName stream name
	 * @param dsl DSL definition for stream
	 * @param description description of the stream definition
	 * @param deploy if {@code true}, the stream is deployed upon creation (default is
	 *     {@code false})
	 * @return the created stream definition already exists
	 * @throws InvalidStreamDefinitionException if there are errors in parsing the stream DSL,
	 *     resolving the name, or type of applications in the stream
	 */
	public StreamDefinition createStream(String streamName, String dsl, String description, boolean deploy) {
		StreamDefinition streamDefinition = createStreamDefinition(streamName, dsl, description);
		List<String> errorMessages = new ArrayList<>();

		for (StreamAppDefinition streamAppDefinition : this.streamDefinitionService.getAppDefinitions(streamDefinition)) {
			final String appName = streamAppDefinition.getRegisteredAppName();
			ApplicationType applicationType = streamAppDefinition.getApplicationType();
			if (!streamValidationService.isRegistered(appName, applicationType)) {
				errorMessages.add(
						String.format("Application name '%s' with type '%s' does not exist in the app registry.",
								appName, applicationType));
			}
		}

		if (!STREAM_NAME_PATTERN.matcher(streamName).matches()) {
			errorMessages.add(STREAM_NAME_VALIDATION_MSG);
		}

		if (!errorMessages.isEmpty()) {
			throw new InvalidStreamDefinitionException(
					StringUtils.collectionToDelimitedString(errorMessages, "\n"));
		}

		if (this.streamDefinitionRepository.existsById(streamName)) {
			throw new DuplicateStreamDefinitionException(String.format(
					"Cannot create stream %s because another one has already " + "been created with the same name",
					streamName));
		}
		final StreamDefinition savedStreamDefinition = this.streamDefinitionRepository.save(streamDefinition);

		if (deploy) {
			this.deployStream(streamName, new HashMap<>());
		}

		auditRecordService.populateAndSaveAuditRecord(
				AuditOperationType.STREAM, AuditActionType.CREATE, streamDefinition.getName(),
				this.streamDefinitionService.redactDsl(streamDefinition), null);

		return streamDefinition;

	}

	public StreamDefinition createStreamDefinition(String streamName, String dsl, String description) {
		try {
			StreamDefinition streamDefinition = new StreamDefinition(streamName, dsl, dsl, description);
			this.streamDefinitionService.parse(streamDefinition);
			return streamDefinition;
		}
		catch (ParseException ex) {
			throw new InvalidStreamDefinitionException(ex.getMessage());
		}
	}

	/**
	 * Deploys the stream with the user provided deployment properties. Implementations are
	 * responsible for expanding deployment wildcard expressions.
	 * @param streamName the name of the stream
	 * @param deploymentProperties deployment properties to use as passed in from the client.
	 */
	public void deployStream(String streamName, Map<String, String> deploymentProperties) {
		if (deploymentProperties == null) {
			deploymentProperties = new HashMap<>();
		}
		StreamDefinition streamDefinition = this.streamDefinitionRepository.findById(streamName)
				.orElseThrow(() -> new NoSuchStreamDefinitionException(streamName));

		DeploymentState status = this.doCalculateStreamState(streamName);

		if (DeploymentState.deployed == status) {
			throw new StreamAlreadyDeployedException(streamName);
		}
		else if (DeploymentState.deploying == status) {
			throw new StreamAlreadyDeployingException(streamName);
		}
		Release deploymentRelease = doDeployStream(streamDefinition, deploymentProperties);
		String platformName = deploymentRelease == null ? null : deploymentRelease.getPlatformName();

		auditRecordService.populateAndSaveAuditRecordUsingMapData(
				AuditOperationType.STREAM, AuditActionType.DEPLOY,
				streamDefinition.getName(),
				this.auditServiceUtils.convertStreamDefinitionToAuditData(
						this.streamDefinitionService.redactDsl(streamDefinition), deploymentProperties),
				platformName);
	}

	/**
	 * Delete the stream, including undeloying.
	 * @param streamName the name of the stream to delete
	 */
	public void deleteStream(String streamName) {
		StreamDefinition streamDefinition = this.streamDefinitionRepository.findById(streamName)
				.orElseThrow(() -> new NoSuchStreamDefinitionException(streamName));
		this.undeployStream(streamName);
		this.streamDefinitionRepository.deleteById(streamName);

		auditRecordService.populateAndSaveAuditRecord(
				AuditOperationType.STREAM, AuditActionType.DELETE,
				streamDefinition.getName(),
				this.streamDefinitionService.redactDsl(streamDefinition), null);
	}

	/**
	 * Delete all streams, including undeploying.
	 */
	public void deleteAll() {
		final Iterable<StreamDefinition> streamDefinitions = this.streamDefinitionRepository.findAll();
		for (StreamDefinition streamDefinition : streamDefinitions) {
			this.undeployStream(streamDefinition.getName());
		}
		this.streamDefinitionRepository.deleteAll();

		for (StreamDefinition streamDefinition : streamDefinitions) {
			auditRecordService.populateAndSaveAuditRecord(
					AuditOperationType.STREAM, AuditActionType.DELETE,
					streamDefinition.getName(),
					this.streamDefinitionService.redactDsl(streamDefinition), null);
		}
	}

	/**
	 * Find streams related to the given stream name.
	 * @param streamName name of the stream
	 * @param nested if should recursively findByTaskNameContains for related stream
	 *     definitions
	 * @return a list of related stream definitions
	 */
	public List<StreamDefinition> findRelatedStreams(String streamName, boolean nested) {
		Set<StreamDefinition> relatedDefinitions = new LinkedHashSet<>();
		StreamDefinition currentStreamDefinition = this.streamDefinitionRepository.findById(streamName)
				.orElseThrow(() -> new NoSuchStreamDefinitionException(streamName));
		Iterable<StreamDefinition> definitions = streamDefinitionRepository.findAll();
		List<StreamDefinition> result = new ArrayList<>(findRelatedDefinitions(currentStreamDefinition, definitions,
				relatedDefinitions, nested));
		return result;
	}

	private Set<StreamDefinition> findRelatedDefinitions(StreamDefinition currentStreamDefinition,
			Iterable<StreamDefinition> definitions,
			Set<StreamDefinition> relatedDefinitions,
			boolean nested) {
		relatedDefinitions.add(currentStreamDefinition);
		String currentStreamName = currentStreamDefinition.getName();
		String indexedStreamName = currentStreamName + ".";
		for (StreamDefinition definition : definitions) {
			StreamNode sn = this.streamDefinitionService.parse(definition);
			if (sn.getSourceDestinationNode() != null) {
				String nameComponent = sn.getSourceDestinationNode().getDestinationName();
				if (nameComponent.equals(currentStreamName) || nameComponent.startsWith(indexedStreamName)) {
					boolean isNewEntry = relatedDefinitions.add(definition);
					if (nested && isNewEntry) {
						findRelatedDefinitions(definition, definitions, relatedDefinitions, true);
					}
				}
			}
		}
		return relatedDefinitions;
	}

	/**
	 * Find stream definitions where the findByTaskNameContains parameter
	 * @param pageable Pagination information
	 * @param search the findByTaskNameContains parameter to use
	 * @return Page of stream definitions
	 */
	public Page<StreamDefinition> findDefinitionByNameContains(Pageable pageable, String search) {
		Page<StreamDefinition> streamDefinitions;
		if (search != null) {
			streamDefinitions = streamDefinitionRepository.findByNameContains(search, pageable);
		}
		else {
			streamDefinitions = streamDefinitionRepository.findAll(pageable);
		}
		return streamDefinitions;
	}

	/**
	 * Find a stream definition by name.
	 * @param streamDefinitionName the name of the stream definition
	 * @return the stream definition
	 * @throws NoSuchStreamDefinitionException if the definition can not be found.
	 */
	public StreamDefinition findOne(String streamDefinitionName) {
		return streamDefinitionRepository.findById(streamDefinitionName)
				.orElseThrow(() -> new NoSuchStreamDefinitionException(streamDefinitionName));
	}

	/**
	 * Verifies that all apps in the stream are valid.
	 * @param name the name of the definition
	 * @return {@link ValidationStatus} for a stream.
	 */
	public ValidationStatus validateStream(String name) {
		return this.streamValidationService.validateStream(name);
	}
}
