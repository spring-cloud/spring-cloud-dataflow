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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.BindingPropertyKeys;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamPropertyKeys;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.DataFlowServerUtil;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployedException;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployingException;
import org.springframework.cloud.dataflow.server.controller.WhitelistProperties;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.stream.AppDeployerStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.StreamDeploymentRequest;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import static org.springframework.cloud.deployer.spi.app.AppDeployer.COUNT_PROPERTY_KEY;

/**
 * Performs manipulation on application and deployment properties, expanding shorthand
 * application property values, resolving wildcard deployment properties, and creates a
 * {@link StreamDeploymentRequest}.
 * </p>
 * If the deployment uses Skipper, delegate to {@link SkipperStreamDeployer}, otherwise
 * use {@link AppDeployerStreamDeployer}.
 * </p>
 * @author Mark Pollack
 */
@Service
public class DefaultStreamService implements StreamService {

	/**
	 * This is the spring boot property key that Spring Cloud Stream uses to filter the
	 * metrics to import when the specific Spring Cloud Stream "applicaiton" trigger is fired
	 * for metrics export.
	 */
	private static final String METRICS_TRIGGER_INCLUDES = "spring.metrics.export.triggers.application.includes";

	private static final String DEFAULT_PARTITION_KEY_EXPRESSION = "payload";

	private static Log logger = LogFactory.getLog(DefaultStreamService.class);

	private final WhitelistProperties whitelistProperties;

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final StreamDefinitionRepository streamDefinitionRepository;

	/**
	 * The app registry this controller will use to lookup apps.
	 */
	private final AppRegistry appRegistry;

	private final AppDeployerStreamDeployer appDeployerStreamDeployer;

	private final SkipperStreamDeployer skipperStreamDeployer;

	/**
	 * General properties to be applied to applications on deployment.
	 */
	private final CommonApplicationProperties commonApplicationProperties;

	public DefaultStreamService(AppRegistry appRegistry,
			CommonApplicationProperties commonApplicationProperties,
			ApplicationConfigurationMetadataResolver metadataResolver,
			StreamDefinitionRepository streamDefinitionRepository,
			AppDeployerStreamDeployer appDeployerStreamDeployer,
			SkipperStreamDeployer skipperStreamDeployer) {
		Assert.notNull(appRegistry, "AppRegistry must not be null");
		Assert.notNull(commonApplicationProperties, "CommonApplicationProperties must not be null");
		Assert.notNull(metadataResolver, "MetadataResolver must not be null");
		Assert.notNull(streamDefinitionRepository, "StreamDefinitionRepository must not be null");
		Assert.notNull(appDeployerStreamDeployer, "AppDeployerStreamDeployer must not be null");
		Assert.notNull(skipperStreamDeployer, "SkipperStreamDeployer must not be null");
		this.appRegistry = appRegistry;
		this.commonApplicationProperties = commonApplicationProperties;
		this.whitelistProperties = new WhitelistProperties(metadataResolver);
		this.streamDefinitionRepository = streamDefinitionRepository;
		this.appDeployerStreamDeployer = appDeployerStreamDeployer;
		this.skipperStreamDeployer = skipperStreamDeployer;
	}

	@Override
	public void deployStream(String name, Map<String, String> deploymentProperties) {
		if (deploymentProperties == null) {
			deploymentProperties = new HashMap<>();
		}
		deployStreamWithDefinition(createStreamDefinitionForDeploy(name, deploymentProperties), deploymentProperties);
	}

	private StreamDefinition createStreamDefinitionForDeploy(String name,
			Map<String, String> deploymentProperties) {
		StreamDefinition streamDefinition = this.streamDefinitionRepository.findOne(name);
		if (streamDefinition == null) {
			throw new NoSuchStreamDefinitionException(name);
		}

		Map<String, String> skipperDeploymentProperties = getSkipperProperties(deploymentProperties);
		String status;
		if (skipperDeploymentProperties.containsKey(SkipperStreamDeployer.SKIPPER_ENABLED_PROPERTY_KEY)) {
			status = skipperStreamDeployer.calculateStreamState(name);
		}
		else {
			status = appDeployerStreamDeployer.calculateStreamState(name);
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
				.filter(mapEntry -> mapEntry.getKey().startsWith(SkipperStreamDeployer.SKIPPER_KEY_PREFIX))
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
				.filter(mapEntry -> !mapEntry.getKey().startsWith(SkipperStreamDeployer.SKIPPER_KEY_PREFIX))
				.collect(Collectors.toMap(mapEntry -> mapEntry.getKey(), mapEntry -> mapEntry.getValue()));

		List<AppDeploymentRequest> appDeploymentRequests = createRequests(streamDefinition,
				deploymentPropertiesToUse);

		if (skipperDeploymentProperties.containsKey(SkipperStreamDeployer.SKIPPER_ENABLED_PROPERTY_KEY)) {
			skipperStreamDeployer.deployStream(new StreamDeploymentRequest(streamDefinition.getName(),
					streamDefinition.getDslText(), appDeploymentRequests, skipperDeploymentProperties));
		}
		else {
			appDeployerStreamDeployer.deployStream(new StreamDeploymentRequest(streamDefinition.getName(),
					streamDefinition.getDslText(), appDeploymentRequests, new HashMap<>()));
		}
	}

	private List<AppDeploymentRequest> createRequests(StreamDefinition stream,
			Map<String, String> streamDeploymentProperties) {
		List<AppDeploymentRequest> appDeploymentRequests = new ArrayList<>();
		if (streamDeploymentProperties == null) {
			streamDeploymentProperties = Collections.emptyMap();
		}
		Iterator<StreamAppDefinition> iterator = stream.getDeploymentOrderIterator();
		int nextAppCount = 0;
		boolean isDownStreamAppPartitioned = false;
		while (iterator.hasNext()) {
			StreamAppDefinition currentApp = iterator.next();
			ApplicationType type = DataFlowServerUtil.determineApplicationType(currentApp);

			AppRegistration registration = this.appRegistry.find(currentApp.getRegisteredAppName(), type);
			Assert.notNull(registration, String.format("no application '%s' of type '%s' exists in the registry",
					currentApp.getName(), type));

			Map<String, String> appDeployTimeProperties = extractAppProperties(currentApp, streamDeploymentProperties);
			Map<String, String> deployerDeploymentProperties = DeploymentPropertiesUtils
					.extractAndQualifyDeployerProperties(streamDeploymentProperties, currentApp.getName());
			deployerDeploymentProperties.put(AppDeployer.GROUP_PROPERTY_KEY, currentApp.getStreamName());

			boolean upstreamAppSupportsPartition = upstreamAppHasPartitionInfo(stream, currentApp,
					streamDeploymentProperties);
			// Set instance count property
			if (deployerDeploymentProperties.containsKey(COUNT_PROPERTY_KEY)) {
				appDeployTimeProperties.put(StreamPropertyKeys.INSTANCE_COUNT,
						deployerDeploymentProperties.get(COUNT_PROPERTY_KEY));
			}
			if (!type.equals(ApplicationType.source)) {
				deployerDeploymentProperties.put(AppDeployer.INDEXED_PROPERTY_KEY, "true");
			}

			// consumer app partition properties
			if (upstreamAppSupportsPartition) {
				updateConsumerPartitionProperties(appDeployTimeProperties);
			}

			// producer app partition properties
			if (isDownStreamAppPartitioned) {
				updateProducerPartitionProperties(appDeployTimeProperties, nextAppCount);
			}

			nextAppCount = getInstanceCount(deployerDeploymentProperties);
			isDownStreamAppPartitioned = isPartitionedConsumer(appDeployTimeProperties, upstreamAppSupportsPartition);

			logger.info(String.format("Downloading resource URI [%s]", registration.getUri()));
			Resource appResource = registration.getResource();
			Resource metadataResource = registration.getMetadataResource();

			// add properties needed for metrics system
			appDeployTimeProperties.put(DataFlowPropertyKeys.STREAM_NAME, currentApp.getStreamName());
			appDeployTimeProperties.put(DataFlowPropertyKeys.STREAM_APP_LABEL, currentApp.getName());
			appDeployTimeProperties.put(DataFlowPropertyKeys.STREAM_APP_TYPE, type.toString());
			StringBuilder sb = new StringBuilder().append(currentApp.getStreamName()).append(".")
					.append(currentApp.getName()).append(".").append("${spring.cloud.application.guid}");
			appDeployTimeProperties.put(StreamPropertyKeys.METRICS_KEY, sb.toString());

			// Merge *definition time* app properties with *deployment time* properties
			// and expand them to their long form if applicable
			AppDefinition revisedDefinition = mergeAndExpandAppProperties(currentApp, metadataResource,
					appDeployTimeProperties);

			AppDeploymentRequest request = new AppDeploymentRequest(revisedDefinition, appResource,
					deployerDeploymentProperties);

			appDeploymentRequests.add(request);
		}
		DeploymentPropertiesUtils.ensureJustDeploymentProperties(streamDeploymentProperties);
		return appDeploymentRequests;
	}

	/**
	 * Extract and return a map of properties for a specific app within the deployment
	 * properties of a stream.
	 *
	 * @param appDefinition the {@link StreamAppDefinition} for which to return a map of
	 * properties
	 * @param streamDeploymentProperties deployment properties for the stream that the app is
	 * defined in
	 * @return map of properties for an app
	 */
	private Map<String, String> extractAppProperties(StreamAppDefinition appDefinition,
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

	/**
	 * Return {@code true} if the upstream app (the app that appears before the provided app)
	 * contains partition related properties.
	 *
	 * @param stream stream for the app
	 * @param currentApp app for which to determine if the upstream app has partition
	 * properties
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
				Map<String, String> appDeploymentProperties = extractAppProperties(prevApp, streamDeploymentProperties);
				return appDeploymentProperties.containsKey(BindingPropertyKeys.OUTPUT_PARTITION_KEY_EXPRESSION)
						|| appDeploymentProperties
								.containsKey(BindingPropertyKeys.OUTPUT_PARTITION_KEY_EXTRACTOR_CLASS);
			}
		}
		return false;
	}

	private void parseAndPopulateProperties(Map<String, String> streamDeploymentProperties,
			Map<String, String> appDeploymentProperties, String producerPropertyPrefix,
			String consumerPropertyPrefix,
			String appPrefix) {
		for (Map.Entry<String, String> entry : streamDeploymentProperties.entrySet()) {
			if (entry.getKey().startsWith(appPrefix)) {
				if (entry.getKey().startsWith(producerPropertyPrefix)) {
					appDeploymentProperties.put(BindingPropertyKeys.OUTPUT_BINDING_KEY_PREFIX
							+ entry.getKey().substring(appPrefix.length()), entry.getValue());
				}
				else if (entry.getKey().startsWith(consumerPropertyPrefix)) {
					appDeploymentProperties.put(
							BindingPropertyKeys.INPUT_BINDING_KEY_PREFIX + entry.getKey().substring(appPrefix.length()),
							entry.getValue());
				}
				else {
					appDeploymentProperties.put(entry.getKey().substring(appPrefix.length()), entry.getValue());
				}
			}
		}
	}

	/**
	 * Return a new app definition where definition-time and deploy-time properties have been
	 * merged and short form parameters have been expanded to their long form (amongst the
	 * whitelisted supported properties of the app) if applicable.
	 */
	/* default */ AppDefinition mergeAndExpandAppProperties(StreamAppDefinition original, Resource metadataResource,
			Map<String, String> appDeployTimeProperties) {
		Map<String, String> merged = new HashMap<>(original.getProperties());
		merged.putAll(appDeployTimeProperties);
		merged = whitelistProperties.qualifyProperties(merged, metadataResource);

		merged.putIfAbsent(StreamPropertyKeys.METRICS_PROPERTIES, "spring.application.name,spring.application.index,"
				+ "spring.cloud.application.*,spring.cloud.dataflow.*");
		merged.putIfAbsent(METRICS_TRIGGER_INCLUDES, "integration**");

		return new AppDefinition(original.getName(), merged);
	}

	/**
	 * Add app properties for producing partitioned data to the provided properties.
	 *
	 * @param properties properties to update
	 * @param nextInstanceCount the number of instances for the next (downstream) app in the
	 * stream
	 */
	private void updateProducerPartitionProperties(Map<String, String> properties, int nextInstanceCount) {
		properties.put(BindingPropertyKeys.OUTPUT_PARTITION_COUNT, String.valueOf(nextInstanceCount));
		if (!properties.containsKey(BindingPropertyKeys.OUTPUT_PARTITION_KEY_EXPRESSION)) {
			properties.put(BindingPropertyKeys.OUTPUT_PARTITION_KEY_EXPRESSION, DEFAULT_PARTITION_KEY_EXPRESSION);
		}
	}

	/**
	 * Add app properties for consuming partitioned data to the provided properties.
	 *
	 * @param properties properties to update
	 */
	private void updateConsumerPartitionProperties(Map<String, String> properties) {
		properties.put(BindingPropertyKeys.INPUT_PARTITIONED, "true");
	}

	/**
	 * Return the app instance count indicated in the provided properties.
	 *
	 * @param properties deployer properties for the app for which to determine the count
	 * @return instance count indicated in the provided properties; if the properties do not
	 * contain a count, a value of {@code 1} is returned
	 */
	private int getInstanceCount(Map<String, String> properties) {
		return Integer.valueOf(properties.getOrDefault(COUNT_PROPERTY_KEY, "1"));
	}

	/**
	 * Return {@code true} if an app is a consumer of partitioned data. This is determined
	 * either by the deployment properties for the app or whether the previous (upstream) app
	 * is publishing partitioned data.
	 *
	 * @param appDeploymentProperties deployment properties for the app
	 * @param upstreamAppSupportsPartition if true, previous (upstream) app in the stream
	 * publishes partitioned data
	 * @return true if the app consumes partitioned data
	 */
	private boolean isPartitionedConsumer(Map<String, String> appDeploymentProperties,
			boolean upstreamAppSupportsPartition) {
		return upstreamAppSupportsPartition
				|| (appDeploymentProperties.containsKey(BindingPropertyKeys.INPUT_PARTITIONED)
						&& appDeploymentProperties.get(BindingPropertyKeys.INPUT_PARTITIONED).equalsIgnoreCase("true"));
	}

	// State

	@Override
	public Map<StreamDefinition, DeploymentState> state(List<StreamDefinition> streamDefinitions) {

		if (System.getProperty("USE_SKIPPER") != null) {
			return this.skipperStreamDeployer.state(streamDefinitions);
		} else {
			return this.appDeployerStreamDeployer.state(streamDefinitions);
		}
	}
}
