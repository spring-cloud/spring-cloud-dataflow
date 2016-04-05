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

import org.springframework.cloud.dataflow.artifact.registry.AppRegistry;
import org.springframework.cloud.dataflow.core.ArtifactType;
import org.springframework.cloud.dataflow.core.BindingPropertyKeys;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamPropertyKeys;
import org.springframework.cloud.dataflow.module.DeploymentState;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.repository.AppDeploymentKey;
import org.springframework.cloud.dataflow.server.repository.AppDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
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
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 */
@RestController
@RequestMapping("/streams/deployments")
@ExposesResourceFor(StreamDeploymentResource.class)
public class StreamDeploymentController {

	private static final String INSTANCE_COUNT_PROPERTY_KEY = "count";

	private static final String DEFAULT_PARTITION_KEY_EXPRESSION = "payload";

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final StreamDefinitionRepository repository;

	/**
	 * The repository this controller will use for app deployment operations.
	 */
	private final AppDeploymentRepository appDeploymentRepository;

	/**
	 * The app registry this controller will use to lookup apps.
	 */
	private final AppRegistry registry;

	/**
	 * The deployer this controller will use to deploy stream apps.
	 */
	private final AppDeployer deployer;

	/**
	 * Create a {@code StreamDeploymentController} that delegates
	 * <ul>
	 *     <li>CRUD operations to the provided {@link StreamDefinitionRepository}</li>
	 *     <li>app retrieval to the provided {@link AppRegistry}</li>
	 *     <li>deployment operations to the provided {@link AppDeployer}</li>
	 * </ul>
	 *
	 * @param repository       the repository this controller will use for stream CRUD operations
	 * @param appDeploymentRepository the repository this controller will use for app deployment operations
	 * @param registry         the registry this controller will use to lookup apps
	 * @param deployer         the deployer this controller will use to deploy stream apps
	 */
	public StreamDeploymentController(StreamDefinitionRepository repository, AppDeploymentRepository appDeploymentRepository,
			AppRegistry registry, AppDeployer deployer) {
		Assert.notNull(repository, "repository must not be null");
		Assert.notNull(appDeploymentRepository, "appDeploymentRepository must not be null");
		Assert.notNull(registry, "registry must not be null");
		Assert.notNull(deployer, "deployer must not be null");
		this.repository = repository;
		this.appDeploymentRepository = appDeploymentRepository;
		this.registry = registry;
		this.deployer = deployer;
	}

	/**
	 * Request un-deployment of an existing stream.
	 *
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
	 *
	 * @param name the name of an existing stream definition (required)
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
		deployStream(stream, DeploymentPropertiesUtils.parse(properties));
	}

	/**
	 * Deploy a stream as defined by its {@link StreamDefinition} and optional deployment properties.
	 *
	 * @param stream the stream to deploy
	 * @param streamDeploymentProperties the deployment properties for the stream
	 */
	private void deployStream(StreamDefinition stream, Map<String, String> streamDeploymentProperties) {
		if (streamDeploymentProperties == null) {
			streamDeploymentProperties = Collections.emptyMap();
		}
		Iterator<ModuleDefinition> iterator = stream.getDeploymentOrderIterator();
		int nextModuleCount = 0;
		boolean isDownStreamModulePartitioned = false;
		long timestamp = System.currentTimeMillis();
		while (iterator.hasNext()) {
			ModuleDefinition currentModule = iterator.next();
			ArtifactType type = determineModuleType(currentModule);
			Map<String, String> moduleDeploymentProperties = extractModuleDeploymentProperties(currentModule, streamDeploymentProperties);
			moduleDeploymentProperties.put(AppDeployer.GROUP_PROPERTY_KEY, currentModule.getGroup());
			moduleDeploymentProperties.put(ModuleDeployer.GROUP_DEPLOYMENT_ID, currentModule.getGroup() + "-" + timestamp);
			if (moduleDeploymentProperties.containsKey(INSTANCE_COUNT_PROPERTY_KEY)) {
				moduleDeploymentProperties.put(AppDeployer.COUNT_PROPERTY_KEY,
						moduleDeploymentProperties.get(INSTANCE_COUNT_PROPERTY_KEY));
			}
			boolean upstreamModuleSupportsPartition = upstreamModuleHasPartitionInfo(stream, currentModule, streamDeploymentProperties);
			// consumer module partition properties
			if (upstreamModuleSupportsPartition) {
				updateConsumerPartitionProperties(moduleDeploymentProperties);
			}
			// producer module partition properties
			if (isDownStreamModulePartitioned) {
				updateProducerPartitionProperties(moduleDeploymentProperties, nextModuleCount);
			}
			nextModuleCount = getModuleCount(moduleDeploymentProperties);
			isDownStreamModulePartitioned = isPartitionedConsumer(currentModule, moduleDeploymentProperties,
					upstreamModuleSupportsPartition);
			AppDefinition definition = new AppDefinition(currentModule.getLabel(), currentModule.getParameters());
			Resource resource = this.registry.find(currentModule.getName(), type).getResource();
			AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, moduleDeploymentProperties);
			String id = this.deployer.deploy(request);
			appDeploymentRepository.save(new AppDeploymentKey(stream, currentModule), id);
		}
	}

	/**
	 * Return the {@link ArtifactType} for a {@link ModuleDefinition} in the context
	 * of a defined stream.
	 *
	 * @param moduleDefinition the module for which to determine the type
	 * @return {@link ArtifactType} for the given module
	 */
	private ArtifactType determineModuleType(ModuleDefinition moduleDefinition) {
		// Parser has already taken care of source/sink destinations, etc
		boolean hasOutput = moduleDefinition.getParameters().containsKey(BindingPropertyKeys.OUTPUT_DESTINATION);
		boolean hasInput = moduleDefinition.getParameters().containsKey(BindingPropertyKeys.INPUT_DESTINATION);
		if (hasInput && hasOutput) {
			return ArtifactType.processor;
		}
		else if (hasInput) {
			return ArtifactType.sink;
		}
		else if (hasOutput) {
			return ArtifactType.source;
		}
		else {
			throw new IllegalStateException(moduleDefinition + " had neither input nor output set");
		}
	}

	/**
	 * Extract and return a map of properties for a specific module within the
	 * deployment properties of a stream.
	 *
	 * @param module module for which to return a map of properties
	 * @param streamDeploymentProperties deployment properties for the stream that the module is defined in
	 * @return map of properties for a module
	 */
	private Map<String, String> extractModuleDeploymentProperties(ModuleDefinition module,
			Map<String, String> streamDeploymentProperties) {
		Map<String, String> moduleDeploymentProperties = new HashMap<>();
		String wildCardProducerPropertyPrefix = "module.*.producer.";
		String wildCardConsumerPropertyPrefix = "module.*.consumer.";
		String wildCardPrefix = "module.*.";
		// first check for wild card prefix
		for (Map.Entry<String, String> entry : streamDeploymentProperties.entrySet()) {
			if (entry.getKey().startsWith(wildCardPrefix)) {
				if (entry.getKey().startsWith(wildCardProducerPropertyPrefix)) {
					moduleDeploymentProperties.put(BindingPropertyKeys.OUTPUT_BINDING_KEY_PREFIX +
							entry.getKey().substring(wildCardPrefix.length()), entry.getValue());
				}
				else if (entry.getKey().startsWith(wildCardConsumerPropertyPrefix)) {
					moduleDeploymentProperties.put(BindingPropertyKeys.INPUT_BINDING_KEY_PREFIX +
							entry.getKey().substring(wildCardPrefix.length()), entry.getValue());
				}
				else {
					moduleDeploymentProperties.put(entry.getKey().substring(wildCardPrefix.length()), entry.getValue());
				}
			}
		}
		String producerPropertyPrefix = String.format("module.%s.producer.", module.getLabel());
		String consumerPropertyPrefix = String.format("module.%s.consumer.", module.getLabel());
		String modulePrefix = String.format("module.%s.", module.getLabel());
		for (Map.Entry<String, String> entry : streamDeploymentProperties.entrySet()) {
			if (entry.getKey().startsWith(modulePrefix)) {
				if (entry.getKey().startsWith(producerPropertyPrefix)) {
					moduleDeploymentProperties.put(BindingPropertyKeys.OUTPUT_BINDING_KEY_PREFIX +
							entry.getKey().substring(modulePrefix.length()), entry.getValue());
				}
				else if (entry.getKey().startsWith(consumerPropertyPrefix)) {
					moduleDeploymentProperties.put(BindingPropertyKeys.INPUT_BINDING_KEY_PREFIX +
							entry.getKey().substring(modulePrefix.length()), entry.getValue());
				}
				else {
					moduleDeploymentProperties.put(entry.getKey().substring(modulePrefix.length()), entry.getValue());
				}
			}
		}
		return moduleDeploymentProperties;
	}

	/**
	 * Return {@code true} if the upstream module (the module that appears before
	 * the provided module) contains partition related properties.
	 *
	 * @param stream        stream for the module
	 * @param currentModule module for which to determine if the upstream module
	 *                      has partition properties
	 * @param streamDeploymentProperties deployment properties for the stream
	 * @return true if the upstream module has partition properties
	 */
	private boolean upstreamModuleHasPartitionInfo(StreamDefinition stream, ModuleDefinition currentModule,
			Map<String, String> streamDeploymentProperties) {
		Iterator<ModuleDefinition> iterator = stream.getDeploymentOrderIterator();
		while (iterator.hasNext()) {
			ModuleDefinition module = iterator.next();
			if (module.equals(currentModule) && iterator.hasNext()) {
				ModuleDefinition prevModule = iterator.next();
				Map<String, String> moduleDeploymentProperties = extractModuleDeploymentProperties(prevModule, streamDeploymentProperties);
				return moduleDeploymentProperties.containsKey(BindingPropertyKeys.OUTPUT_PARTITION_KEY_EXPRESSION) ||
						moduleDeploymentProperties.containsKey(BindingPropertyKeys.OUTPUT_PARTITION_KEY_EXTRACTOR_CLASS);
			}
		}
		return false;
	}

	/**
	 * Return {@code true} if the provided module is a consumer of partitioned data.
	 * This is determined either by the deployment properties for the module
	 * or whether the previous (upstream) module is publishing partitioned data.
	 *
	 * @param module module for which to determine if it is consuming partitioned data
	 * @param moduleDeploymentProperties deployment properties for the module
	 * @param upstreamModuleSupportsPartition if true, previous (upstream) module
	 * in the stream publishes partitioned data
	 * @return true if this module consumes partitioned data
	 */
	private boolean isPartitionedConsumer(ModuleDefinition module,
			Map<String, String> moduleDeploymentProperties,
			boolean upstreamModuleSupportsPartition) {
		return upstreamModuleSupportsPartition ||
				(moduleDeploymentProperties.containsKey(BindingPropertyKeys.INPUT_PARTITIONED) &&
						moduleDeploymentProperties.get(BindingPropertyKeys.INPUT_PARTITIONED).equalsIgnoreCase("true"));
	}

	/**
	 * Add module properties for consuming partitioned data to the provided properties.
	 *
	 * @param properties properties to update
	 */
	private void updateConsumerPartitionProperties(Map<String, String> properties) {
		properties.put(BindingPropertyKeys.INPUT_PARTITIONED, "true");
		if (properties.containsKey(INSTANCE_COUNT_PROPERTY_KEY)) {
			properties.put(StreamPropertyKeys.INSTANCE_COUNT, properties.get(INSTANCE_COUNT_PROPERTY_KEY));
		}
	}

	/**
	 * Add module properties for producing partitioned data to the provided properties.
	 *
	 * @param properties properties to update
	 * @param nextModuleCount the number of module instances for the next (downstream) module in the stream
	 */
	private void updateProducerPartitionProperties(Map<String, String> properties, int nextModuleCount) {
		properties.put(BindingPropertyKeys.OUTPUT_PARTITION_COUNT, String.valueOf(nextModuleCount));
		if (!properties.containsKey(BindingPropertyKeys.OUTPUT_PARTITION_KEY_EXPRESSION)) {
			properties.put(BindingPropertyKeys.OUTPUT_PARTITION_KEY_EXPRESSION, DEFAULT_PARTITION_KEY_EXPRESSION);
		}
	}

	/**
	 * Return the module count indicated in the provided properties.
	 *
	 * @param properties properties for the module for which to determine the count
	 * @return module count indicated in the provided properties;
	 * if the properties do not contain a count a value of {@code 1} is returned
	 */
	private int getModuleCount(Map<String, String> properties) {
		return (properties.containsKey(INSTANCE_COUNT_PROPERTY_KEY)) ?
				Integer.valueOf(properties.get(INSTANCE_COUNT_PROPERTY_KEY)) : 1;
	}

	/**
	 * Undeploy the given stream.
	 *
	 * @param stream stream to undeploy
	 */
	private void undeployStream(StreamDefinition stream) {
		for (ModuleDefinition module : stream.getModuleDefinitions()) {
			AppDeploymentKey key = new AppDeploymentKey(stream, module);
			String id = appDeploymentRepository.findOne(key);
			AppStatus status = this.deployer.status(id);
			if (!EnumSet.of(DeploymentState.unknown, DeploymentState.undeployed)
					.contains(status.getState())) {
				this.deployer.undeploy(id);
			}
		}
	}

}
