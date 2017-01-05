/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.BindingPropertyKeys;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamPropertyKeys;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.DataFlowServerUtil;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentKey;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for deployment operations on {@link StreamDefinition}.
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 * @author Janne Valkealahti
 */
@RestController
@RequestMapping("/streams/deployments")
@ExposesResourceFor(StreamDeploymentResource.class)
public class StreamDeploymentController {

	private static Log loggger = LogFactory.getLog(StreamDeploymentController.class);

	private static final String DEFAULT_PARTITION_KEY_EXPRESSION = "payload";

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final StreamDefinitionRepository repository;

	/**
	 * The repository this controller will use for deployment IDs.
	 */
	private final DeploymentIdRepository deploymentIdRepository;

	/**
	 * The app registry this controller will use to lookup apps.
	 */
	private final AppRegistry registry;

	/**
	 * The deployer this controller will use to deploy stream apps.
	 */
	private final AppDeployer deployer;

	private final WhitelistProperties whitelistProperties;

	/**
	 * General properties to be applied to applications on deployment.
	 */
	private final CommonApplicationProperties commonApplicationProperties;

	/**
	 * Create a {@code StreamDeploymentController} that delegates
	 * <ul>
	 * <li>CRUD operations to the provided {@link StreamDefinitionRepository}</li>
	 * <li>app retrieval to the provided {@link AppRegistry}</li>
	 * <li>deployment operations to the provided {@link AppDeployer}</li>
	 * </ul>
	 * @param repository             the repository this controller will use for stream CRUD operations
	 * @param deploymentIdRepository the repository this controller will use for deployment IDs
	 * @param registry               the registry this controller will use to lookup apps
	 * @param deployer               the deployer this controller will use to deploy stream apps
	 * @param commonProperties         common set of application properties
	 */
	public StreamDeploymentController(StreamDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository, AppRegistry registry, AppDeployer deployer,
			ApplicationConfigurationMetadataResolver metadataResolver, CommonApplicationProperties commonProperties) {
		Assert.notNull(repository, "StreamDefinitionRepository must not be null");
		Assert.notNull(deploymentIdRepository, "DeploymentIdRepository must not be null");
		Assert.notNull(registry, "AppRegistry must not be null");
		Assert.notNull(deployer, "AppDeployer must not be null");
		Assert.notNull(metadataResolver, "MetadataResolver must not be null");
		Assert.notNull(commonProperties, "CommonApplicationProperties must not be null");
		this.repository = repository;
		this.deploymentIdRepository = deploymentIdRepository;
		this.registry = registry;
		this.deployer = deployer;
		this.whitelistProperties = new WhitelistProperties(metadataResolver);
		this.commonApplicationProperties = commonProperties;
	}

	/**
	 * Request un-deployment of an existing stream.
	 * @param name the name of an existing stream (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name") String name) {
		StreamDefinition stream = this.repository.findOne(name);
		if (stream == null) {
			throw new NoSuchStreamDefinitionException(name);
		}
		undeployStream(stream);
	}

	/**
	 * Request un-deployment of all streams.
	 */
	@RequestMapping(value = "", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeployAll() {
		for (StreamDefinition stream : this.repository.findAll()) {
			this.undeployStream(stream);
		}
	}

	/**
	 * Request deployment of an existing stream definition.
	 * @param name       the name of an existing stream definition (required)
	 * @param properties the deployment properties for the stream as a comma-delimited list of key=value pairs
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void deploy(@PathVariable("name") String name,
			@RequestParam(required = false) String properties) {
		StreamDefinition stream = this.repository.findOne(name);
		if (stream == null) {
			throw new NoSuchStreamDefinitionException(name);
		}
		String status = calculateStreamState(name);
		if (DeploymentState.deployed.equals(DeploymentState.valueOf(status))) {
			throw new StreamAlreadyDeployedException(name);
		}
		else if (DeploymentState.deploying.equals(DeploymentState.valueOf(status))) {
			throw new StreamAlreadyDeployingException(name);
		}
		deployStream(stream, DeploymentPropertiesUtils.parse(properties));
	}

	private String calculateStreamState(String name) {
		Set<DeploymentState> appStates = EnumSet.noneOf(DeploymentState.class);
		StreamDefinition stream = this.repository.findOne(name);
		for (StreamAppDefinition appDefinition : stream.getAppDefinitions()) {
			String key = DeploymentKey.forStreamAppDefinition(appDefinition);
			String id = this.deploymentIdRepository.findOne(key);
			if (id != null) {
				AppStatus status = this.deployer.status(id);
				appStates.add(status.getState());
			}
			else {
				appStates.add(DeploymentState.undeployed);
			}
		}
		return StreamDefinitionController.aggregateState(appStates).toString();
	}

	/**
	 * Deploy a stream as defined by its {@link StreamDefinition} and optional deployment properties.
	 * @param stream                     the stream to deploy
	 * @param streamDeploymentProperties the deployment properties for the stream
	 */
	private void deployStream(StreamDefinition stream, Map<String, String> streamDeploymentProperties) {
		if (streamDeploymentProperties == null) {
			streamDeploymentProperties = Collections.emptyMap();
		}
		Iterator<StreamAppDefinition> iterator = stream.getDeploymentOrderIterator();
		int nextAppCount = 0;
		boolean isDownStreamAppPartitioned = false;
		while (iterator.hasNext()) {
			StreamAppDefinition currentApp = iterator.next();
			ApplicationType type = DataFlowServerUtil.determineApplicationType(currentApp);
			Map<String, String> appDeploymentProperties = extractAppDeploymentProperties(currentApp, streamDeploymentProperties);
			appDeploymentProperties.put(AppDeployer.GROUP_PROPERTY_KEY, currentApp.getStreamName());
			boolean upstreamAppSupportsPartition = upstreamAppHasPartitionInfo(stream, currentApp, streamDeploymentProperties);
			// Set instance count property
			if (appDeploymentProperties.containsKey(AppDeployer.COUNT_PROPERTY_KEY)) {
				appDeploymentProperties.put(StreamPropertyKeys.INSTANCE_COUNT, appDeploymentProperties.get(AppDeployer.COUNT_PROPERTY_KEY));
			}
			if (!type.equals(ApplicationType.source)) {
				appDeploymentProperties.put(AppDeployer.INDEXED_PROPERTY_KEY, "true");
			}
			// consumer app partition properties
			if (upstreamAppSupportsPartition) {
				updateConsumerPartitionProperties(appDeploymentProperties);
			}
			// producer app partition properties
			if (isDownStreamAppPartitioned) {
				updateProducerPartitionProperties(appDeploymentProperties, nextAppCount);
			}
			nextAppCount = getInstanceCount(appDeploymentProperties);
			isDownStreamAppPartitioned = isPartitionedConsumer(appDeploymentProperties,
					upstreamAppSupportsPartition);
			AppRegistration registration = this.registry.find(currentApp.getRegisteredAppName(), type);
			Assert.notNull(registration, String.format("no application '%s' of type '%s' exists in the registry",
					currentApp.getName(), type));
			Resource resource = registration.getResource();
			currentApp = qualifyProperties(currentApp, resource);
			AppDeploymentRequest request = currentApp.createDeploymentRequest(resource,
					whitelistProperties.qualifyProperties(appDeploymentProperties, resource));
			try {
				String id = this.deployer.deploy(request);
				this.deploymentIdRepository.save(DeploymentKey.forStreamAppDefinition(currentApp), id);
			}
			// If the deployer implementation handles the deployment request synchronously, log error message if
			// any exception is thrown out of the deployment and proceed to the next deployment.
			catch (Exception e) {
				loggger.error(String.format("Exception when deploying the app %s: %s", currentApp, e.getMessage()), e);
			}
		}
	}

	/**
	 * Return a copy of a given app definition where short form parameters have been expanded to their long form
	 * (amongst the whitelisted supported properties of the app) if applicable.
	 */
	/*default*/ StreamAppDefinition qualifyProperties(StreamAppDefinition original, Resource resource) {
		StreamAppDefinition.Builder builder = StreamAppDefinition.Builder.from(original);
		return builder.setProperties(whitelistProperties.qualifyProperties(original.getProperties(), resource)).build(original.getStreamName());
	}

	/**
	 * Extract and return a map of properties for a specific app within the
	 * deployment properties of a stream.
	 * @param appDefinition              the {@link StreamAppDefinition} for which to return a map of properties
	 * @param streamDeploymentProperties deployment properties for the stream that the app is defined in
	 * @return map of properties for an app
	 */
	private Map<String, String> extractAppDeploymentProperties(StreamAppDefinition appDefinition,
			Map<String, String> streamDeploymentProperties) {
		Map<String, String> appDeploymentProperties = new HashMap<>();
		// add common properties first
		appDeploymentProperties.putAll(this.commonApplicationProperties.getStream());
		// add properties with wild card prefix
		String wildCardProducerPropertyPrefix = "app.*.producer.";
		String wildCardConsumerPropertyPrefix = "app.*.consumer.";
		String wildCardPrefix = "app.*.";
		parseAndPopulateProperties(streamDeploymentProperties, appDeploymentProperties, wildCardProducerPropertyPrefix,
				wildCardConsumerPropertyPrefix, wildCardPrefix);
		// add application specific properties
		String producerPropertyPrefix = String.format("app.%s.producer.", appDefinition.getName());
		String consumerPropertyPrefix = String.format("app.%s.consumer.", appDefinition.getName());
		String appPrefix = String.format("app.%s.", appDefinition.getName());
		parseAndPopulateProperties(streamDeploymentProperties, appDeploymentProperties, producerPropertyPrefix,
				consumerPropertyPrefix, appPrefix);
		return appDeploymentProperties;
	}

	private void parseAndPopulateProperties(Map<String, String> streamDeploymentProperties,
			Map<String, String> appDeploymentProperties, String producerPropertyPrefix, String consumerPropertyPrefix,
			String appPrefix) {
		for (Map.Entry<String, String> entry : streamDeploymentProperties.entrySet()) {
			if (entry.getKey().startsWith(appPrefix)) {
				if (entry.getKey().startsWith(producerPropertyPrefix)) {
					appDeploymentProperties.put(BindingPropertyKeys.OUTPUT_BINDING_KEY_PREFIX +
							entry.getKey().substring(appPrefix.length()), entry.getValue());
				}
				else if (entry.getKey().startsWith(consumerPropertyPrefix)) {
					appDeploymentProperties.put(BindingPropertyKeys.INPUT_BINDING_KEY_PREFIX +
							entry.getKey().substring(appPrefix.length()), entry.getValue());
				}
				else if ((appPrefix + "count").equals(entry.getKey())) {
					appDeploymentProperties.put(AppDeployer.COUNT_PROPERTY_KEY, entry.getValue());
				}
				else {
					appDeploymentProperties.put(entry.getKey().substring(appPrefix.length()), entry.getValue());
				}
			}
		}
	}

	/**
	 * Return {@code true} if the upstream app (the app that appears before
	 * the provided app) contains partition related properties.
	 * @param stream                     stream for the app
	 * @param currentApp                 app for which to determine if the upstream app
	 *                                   has partition properties
	 * @param streamDeploymentProperties deployment properties for the stream
	 * @return true if the upstream app has partition properties
	 */
	private boolean upstreamAppHasPartitionInfo(StreamDefinition stream, StreamAppDefinition currentApp,
			Map<String, String> streamDeploymentProperties) {
		Iterator<StreamAppDefinition> iterator = stream.getDeploymentOrderIterator();
		while (iterator.hasNext()) {
			StreamAppDefinition app = iterator.next();
			if (app.equals(currentApp) && iterator.hasNext()) {
				StreamAppDefinition prevApp = iterator.next();
				Map<String, String> appDeploymentProperties = extractAppDeploymentProperties(prevApp, streamDeploymentProperties);
				return appDeploymentProperties.containsKey(BindingPropertyKeys.OUTPUT_PARTITION_KEY_EXPRESSION) ||
						appDeploymentProperties.containsKey(BindingPropertyKeys.OUTPUT_PARTITION_KEY_EXTRACTOR_CLASS);
			}
		}
		return false;
	}

	/**
	 * Return {@code true} if an app is a consumer of partitioned data.
	 * This is determined either by the deployment properties for the app
	 * or whether the previous (upstream) app is publishing partitioned data.
	 * @param appDeploymentProperties      deployment properties for the app
	 * @param upstreamAppSupportsPartition if true, previous (upstream) app
	 *                                     in the stream publishes partitioned data
	 * @return true if the app consumes partitioned data
	 */
	private boolean isPartitionedConsumer(Map<String, String> appDeploymentProperties,
			boolean upstreamAppSupportsPartition) {
		return upstreamAppSupportsPartition ||
				(appDeploymentProperties.containsKey(BindingPropertyKeys.INPUT_PARTITIONED) &&
						appDeploymentProperties.get(BindingPropertyKeys.INPUT_PARTITIONED).equalsIgnoreCase("true"));
	}

	/**
	 * Add app properties for consuming partitioned data to the provided properties.
	 * @param properties properties to update
	 */
	private void updateConsumerPartitionProperties(Map<String, String> properties) {
		properties.put(BindingPropertyKeys.INPUT_PARTITIONED, "true");
	}

	/**
	 * Add app properties for producing partitioned data to the provided properties.
	 * @param properties        properties to update
	 * @param nextInstanceCount the number of instances for the next (downstream) app in the stream
	 */
	private void updateProducerPartitionProperties(Map<String, String> properties, int nextInstanceCount) {
		properties.put(BindingPropertyKeys.OUTPUT_PARTITION_COUNT, String.valueOf(nextInstanceCount));
		if (!properties.containsKey(BindingPropertyKeys.OUTPUT_PARTITION_KEY_EXPRESSION)) {
			properties.put(BindingPropertyKeys.OUTPUT_PARTITION_KEY_EXPRESSION, DEFAULT_PARTITION_KEY_EXPRESSION);
		}
	}

	/**
	 * Return the app instance count indicated in the provided properties.
	 * @param properties properties for the app for which to determine the count
	 * @return instance count indicated in the provided properties;
	 * if the properties do not contain a count, a value of {@code 1} is returned
	 */
	private int getInstanceCount(Map<String, String> properties) {
		return (properties.containsKey(AppDeployer.COUNT_PROPERTY_KEY)) ?
				Integer.valueOf(properties.get(AppDeployer.COUNT_PROPERTY_KEY)) : 1;
	}

	/**
	 * Undeploy the given stream.
	 * @param stream stream to undeploy
	 */
	private void undeployStream(StreamDefinition stream) {
		for (StreamAppDefinition appDefinition : stream.getAppDefinitions()) {
			String key = DeploymentKey.forStreamAppDefinition(appDefinition);
			String id = this.deploymentIdRepository.findOne(key);
			// if id is null, assume nothing is deployed
			if (id != null) {
				AppStatus status = this.deployer.status(id);
				if (!EnumSet.of(DeploymentState.unknown, DeploymentState.undeployed)
						.contains(status.getState())) {
					this.deployer.undeploy(id);
				}
				this.deploymentIdRepository.delete(key);
			}
		}
	}

}
