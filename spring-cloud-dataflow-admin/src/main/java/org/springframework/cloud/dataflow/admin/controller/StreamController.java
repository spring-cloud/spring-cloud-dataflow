/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.admin.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.dataflow.admin.repository.DuplicateStreamException;
import org.springframework.cloud.dataflow.admin.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.core.BindingProperties;
import org.springframework.cloud.dataflow.core.ModuleCoordinates;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.core.ModuleType;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.cloud.dataflow.module.registry.ModuleRegistration;
import org.springframework.cloud.dataflow.module.registry.ModuleRegistry;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link StreamDefinition}. This
 * includes CRUD and deployment operations.
 *
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/streams")
@ExposesResourceFor(StreamDefinitionResource.class)
public class StreamController {

	private static final Logger logger = LoggerFactory.getLogger(StreamController.class);

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final StreamDefinitionRepository repository;

	/**
	 * The module registry this controller will use to look up modules.
	 */
	private final ModuleRegistry registry;

	/**
	 * The deployer this controller will use to deploy stream modules.
	 */
	private final ModuleDeployer deployer;

	/**
	 * Assembler for {@link StreamDefinitionResource} objects.
	 */
	private final Assembler streamAssembler = new Assembler();

	private static final String DEFAULT_PARTITION_KEY_EXPRESSION = "payload";

	/**
	 * Create a {@code StreamController} that delegates
	 * <ul>
	 *     <li>CRUD operations to the provided {@link StreamDefinitionRepository}</li>
	 *     <li>deployment operations to the provided {@link ModuleDeployer}</li>
	 *     <li>module coordinate retrieval to the provided {@link ModuleRegistry}</li>
	 * </ul>
	 *
	 * @param repository  the repository this controller will use for stream CRUD operations
	 * @param registry    module registry this controller will use to look up modules
	 * @param deployer    the deployer this controller will use to deploy stream modules
	 */
	@Autowired
	public StreamController(StreamDefinitionRepository repository, ModuleRegistry registry,
			@Qualifier("processModuleDeployer") ModuleDeployer deployer) {
		Assert.notNull(repository, "repository must not be null");
		Assert.notNull(registry, "registry must not be null");
		Assert.notNull(deployer, "deployer must not be null");
		this.repository = repository;
		this.registry = registry;
		this.deployer = deployer;
	}

	/**
	 * Return a page-able list of {@link StreamDefinitionResource} defined streams.
	 *
	 * @param pageable   page-able collection of {@code StreamDefinitionResource}.
	 * @param assembler  assembler for {@link StreamDefinition}
	 * @return list of stream definitions
	 */
	@RequestMapping(value = "/definitions", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StreamDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<StreamDefinition> assembler) {
		return assembler.toResource(repository.findAll(pageable), streamAssembler);
	}

	/**
	 * Create a new stream.
	 *
	 * @param name    stream name
	 * @param dsl     DSL definition for stream
	 * @param deploy  if {@code true}, the stream is deployed upon creation
	 */
	@RequestMapping(value = "/definitions", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void save(@RequestParam("name") String name,
			@RequestParam("definition") String dsl,
			@RequestParam(value = "deploy", defaultValue = "true")
			boolean deploy) {
		if (this.repository.exists(name)) {
			throw new DuplicateStreamException(
					String.format("Cannot create stream %s because another one has already " +
							"been created with the same name", name));
		}

		StreamDefinition stream = new StreamDefinition(name, dsl);
		stream = this.repository.save(stream);
		if (deploy) {
			deployStream(stream, null);
		}
	}

	/**
	 * Request removal of an existing stream definition.
	 *
	 * @param name the name of an existing stream definition (required)
	 */
	@RequestMapping(value = "/definitions/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name") String name) throws Exception {
		undeploy(name);
		this.repository.delete(name);
	}

	/**
	 * Request removal of all stream definitions.
	 */
	@RequestMapping(value = "/definitions", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void deleteAll() throws Exception {
		undeployAll();
		this.repository.deleteAll();
	}

	/**
	 * Request un-deployment of an existing stream.
	 *
	 * @param name the name of an existing stream (required)
	 */
	@RequestMapping(value = "/deployments/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name") String name) throws Exception {
		StreamDefinition stream = this.repository.findOne(name);
		Assert.notNull(stream, String.format("no stream defined: %s", name));
		undeployStream(stream);
	}

	/**
	 * Request un-deployment of all streams.
	 */
	@RequestMapping(value = "/deployments", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeployAll() throws Exception {
		for (StreamDefinition stream : this.repository.findAll()) {
			this.undeployStream(stream);
		}
	}

	/**
	 * Request deployment of an existing stream definition. The name must be included in the path.
	 *
	 * @param name the name of an existing stream definition (required)
	 * @param properties the deployment properties for the stream as a comma-delimited list of key=value pairs
	 */
	@RequestMapping(value = "/deployments/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void deploy(@PathVariable("name") String name, @RequestParam(required = false) String properties) throws
			Exception {
		StreamDefinition stream = this.repository.findOne(name);
		Assert.notNull(stream, String.format("no stream defined: %s", name));
		deployStream(stream, DeploymentPropertiesUtils.parse(properties));
	}

	private void deployStream(StreamDefinition stream, Map<String, String> cumulatedDeploymentProperties) {
		if (cumulatedDeploymentProperties == null) {
			cumulatedDeploymentProperties = Collections.emptyMap();
		}

		DownStreamModuleInfo info = new DownStreamModuleInfo(1, false);

		for (StreamDefinition.DeploymentOrderIterator iterator = stream.getDeploymentOrderIterator();
			iterator.hasNext(); ) {
			ModuleDefinition currentModule = iterator.next();

			ModuleCoordinates coordinates = getModuleCoordinates(iterator, currentModule);

			Map<String, String> moduleDeploymentProperties = getModuleDeploymentProperties(currentModule, cumulatedDeploymentProperties);

			info = postProcessPartitioningProperties(stream, cumulatedDeploymentProperties, info, currentModule, moduleDeploymentProperties);

			this.deployer.deploy(new ModuleDeploymentRequest(currentModule, coordinates, moduleDeploymentProperties));
		}
	}

	/**
	 * Maybe update the {@literal currentModule} {@literal moduleDeploymentProperties} with properties related
	 * to partitioning.
	 *
	 * @param stream the whole stream definition
	 * @param cumulatedDeploymentProperties the whole set of stream deployment properties
	 * @param info information about the previous (i.e. downstream) module in the stream
	 * @param currentModule the module whose deployment properties we want to alter
	 * @param moduleDeploymentProperties the deployment properties for the current module
	 * @return information about the current module, to be used as 'previous' in the next invocation
	 */
	private DownStreamModuleInfo postProcessPartitioningProperties(StreamDefinition stream, Map<String, String> cumulatedDeploymentProperties, DownStreamModuleInfo info, ModuleDefinition currentModule, Map<String, String> moduleDeploymentProperties) {
		boolean upstreamModuleSupportsPartition = upstreamModuleHasPartitionInfo(stream, currentModule, cumulatedDeploymentProperties);
		// consumer module partition properties
		if (isPartitionedConsumer(currentModule, moduleDeploymentProperties, upstreamModuleSupportsPartition)) {
			updateConsumerPartitionProperties(moduleDeploymentProperties);
		}
		// producer module partition properties
		if (info.isDownStreamModulePartitioned) {
			updateProducerPartitionProperties(moduleDeploymentProperties, info.nextModuleCount);
		}
		int nextModuleCount = getNextModuleCount(moduleDeploymentProperties);
		boolean isDownStreamModulePartitioned = isPartitionedConsumer(currentModule, moduleDeploymentProperties,
				upstreamModuleSupportsPartition);
		return new DownStreamModuleInfo(nextModuleCount, isDownStreamModulePartitioned);
	}

	private ModuleCoordinates getModuleCoordinates(StreamDefinition.DeploymentOrderIterator iterator, ModuleDefinition currentModule) {
		ModuleType type = determineModuleType(currentModule, iterator.isFirstUpstream(), iterator.isLastDownstream());
		ModuleRegistration registration = this.registry.find(currentModule.getName(), type);
		if (registration == null) {
			throw new IllegalArgumentException(String.format(
					"Module %s of type %s not found in registry", currentModule.getName(), type));
		}
		return registration.getCoordinates();
	}

	/**
	 * Holds composite state about a module in the context of deployment.
	 */
	private static class DownStreamModuleInfo {
		private final int nextModuleCount;
		private final boolean isDownStreamModulePartitioned;

		private DownStreamModuleInfo(int nextModuleCount, boolean isDownStreamModulePartitioned) {
			this.nextModuleCount = nextModuleCount;
			this.isDownStreamModulePartitioned = isDownStreamModulePartitioned;
		}
	}

	private ModuleType determineModuleType(ModuleDefinition moduleDefinition, boolean isFirst, boolean isLast) {
		ModuleType type = null;
		if (isLast) {
			if (moduleDefinition.getParameters().containsKey(BindingProperties.OUTPUT_BINDING_KEY)) {
				if (isFirst) {
					// single module stream with a named channel in the sink position
					type = ModuleType.source;
				}
				else {
					type = ModuleType.processor;
				}
			}
			else {
				// this will handle any case that the module is a sink,
				// even if a single module stream with a named input channel
				type = ModuleType.sink;
			}
		}
		else if (isFirst) {
			if (moduleDefinition.getParameters().containsKey(BindingProperties.INPUT_BINDING_KEY)) {
				// stream begins with a named channel in the source position
				type = ModuleType.processor;
			}
			else { 
				type = ModuleType.source;
			}
		}
		else {
			type = ModuleType.processor;
		}
		return type;
	}

	private Map<String, String> getModuleDeploymentProperties(ModuleDefinition module, Map<String, String> deploymentProperties) {
		Map<String, String> moduleDeploymentProperties = new HashMap<>();
		String wildCardPrefix = "module.*.";
		// first check for wild card prefix
		for (Map.Entry<String, String> entry : deploymentProperties.entrySet()) {
			if (entry.getKey().startsWith(wildCardPrefix)) {
				moduleDeploymentProperties.put(entry.getKey().substring(wildCardPrefix.length()), entry.getValue());
			}
		}
		String modulePrefix = String.format("module.%s.", module.getLabel());
		for (Map.Entry<String, String> entry : deploymentProperties.entrySet()) {
			if (entry.getKey().startsWith(modulePrefix)) {
				moduleDeploymentProperties.put(entry.getKey().substring(modulePrefix.length()), entry.getValue());
			}
		}
		return moduleDeploymentProperties;
	}

	private boolean upstreamModuleHasPartitionInfo(StreamDefinition stream, ModuleDefinition currentModule,
			Map<String, String> cumulatedDeploymentProperties) {
		Iterator<ModuleDefinition> iterator = stream.getDeploymentOrderIterator();
		while (iterator.hasNext()) {
			ModuleDefinition module = iterator.next();
			if (module.equals(currentModule) && iterator.hasNext()) {
				ModuleDefinition prevModule = iterator.next();
				Map<String, String> moduleDeploymentProperties = getModuleDeploymentProperties(prevModule, cumulatedDeploymentProperties);
				return moduleDeploymentProperties.containsKey(BindingProperties.PARTITION_KEY_EXPRESSION) ||
						moduleDeploymentProperties.containsKey(BindingProperties.PARTITION_KEY_EXTRACTOR_CLASS);
			}
		}
		return false;
	}

	private boolean isPartitionedConsumer(ModuleDefinition module, Map<String, String> properties,
			boolean upstreamModuleSupportsPartition) {
		return upstreamModuleSupportsPartition ||
				(module.getParameters().containsKey(BindingProperties.INPUT_BINDING_KEY) &&
				properties.containsKey(BindingProperties.PARTITIONED_PROPERTY) &&
				properties.get(BindingProperties.PARTITIONED_PROPERTY).equalsIgnoreCase("true"));
	}

	private void updateConsumerPartitionProperties(Map<String, String> properties) {
		properties.put(BindingProperties.INPUT_PARTITIONED, "true");
		if (properties.containsKey(BindingProperties.COUNT_PROPERTY)) {
			properties.put(BindingProperties.INSTANCE_COUNT, properties.get(BindingProperties.COUNT_PROPERTY));
		}
	}

	private void updateProducerPartitionProperties(Map<String, String> properties, int nextModuleCount) {
		properties.put(BindingProperties.OUTPUT_PARTITION_COUNT, String.valueOf(nextModuleCount));
		if (properties.containsKey(BindingProperties.PARTITION_KEY_EXPRESSION)) {
			properties.put(BindingProperties.OUTPUT_PARTITION_KEY_EXPRESSION,
					properties.get(BindingProperties.PARTITION_KEY_EXPRESSION));
		}
		else {
			properties.put(BindingProperties.OUTPUT_PARTITION_KEY_EXPRESSION, DEFAULT_PARTITION_KEY_EXPRESSION);
		}
		if (properties.containsKey(BindingProperties.PARTITION_KEY_EXTRACTOR_CLASS)) {
			properties.put(BindingProperties.OUTPUT_PARTITION_KEY_EXTRACTOR_CLASS,
					properties.get(BindingProperties.PARTITION_KEY_EXTRACTOR_CLASS));
		}
		if (properties.containsKey(BindingProperties.PARTITION_SELECTOR_CLASS)) {
			properties.put(BindingProperties.OUTPUT_PARTITION_SELECTOR_CLASS,
					properties.get(BindingProperties.PARTITION_SELECTOR_CLASS));
		}
		if (properties.containsKey(BindingProperties.PARTITION_SELECTOR_EXPRESSION)) {
			properties.put(BindingProperties.OUTPUT_PARTITION_SELECTOR_EXPRESSION,
					properties.get(BindingProperties.PARTITION_SELECTOR_EXPRESSION));
		}
	}

	private int getNextModuleCount(Map<String, String> properties) {
		return (properties.containsKey(BindingProperties.COUNT_PROPERTY)) ?
				Integer.valueOf(properties.get(BindingProperties.COUNT_PROPERTY)) : 1;
	}

	private void undeployStream(StreamDefinition stream) {
		for (ModuleDefinition module : stream.getModuleDefinitions()) {
			ModuleDeploymentId id = ModuleDeploymentId.fromModuleDefinition(module);
			ModuleStatus status = this.deployer.status(id);
			// todo: change from 'unknown' to 'undeployed' when status() does the same
			if (!ModuleStatus.State.unknown.equals(status.getState())) {
				this.deployer.undeploy(id);
			}
		}
	}

	private String calculateStreamState(String name) {
		Set<ModuleStatus.State> moduleStates = new HashSet<>();
		StreamDefinition stream = repository.findOne(name);
		for (ModuleDefinition module : stream.getModuleDefinitions()) {
			ModuleStatus status = deployer.status(ModuleDeploymentId.fromModuleDefinition(module));
			moduleStates.add(status.getState());
		}

		logger.debug("states: {}", moduleStates);

		// todo: this requires more thought...
		if (moduleStates.contains(ModuleStatus.State.failed)) {
			return ModuleStatus.State.failed.toString();
		}
		else if (moduleStates.contains(ModuleStatus.State.incomplete)) {
			return ModuleStatus.State.incomplete.toString();
		}
		else if (moduleStates.contains(ModuleStatus.State.deploying)) {
			return ModuleStatus.State.deploying.toString();
		}
		else if (moduleStates.contains(ModuleStatus.State.deployed) && moduleStates.size() == 1) {
			return ModuleStatus.State.deployed.toString();
		}
		else {
			return ModuleStatus.State.unknown.toString();
		}
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts {@link StreamDefinition}s to {@link StreamDefinitionResource}s.
	 */
	class Assembler extends ResourceAssemblerSupport<StreamDefinition, StreamDefinitionResource> {

		public Assembler() {
			super(StreamController.class, StreamDefinitionResource.class);
		}

		@Override
		public StreamDefinitionResource toResource(StreamDefinition stream) {
			return createResourceWithId(stream.getName(), stream);
		}

		@Override
		public StreamDefinitionResource instantiateResource(StreamDefinition stream) {
			StreamDefinitionResource resource = new StreamDefinitionResource(stream.getName(), stream.getDslText());
			resource.setStatus(calculateStreamState(stream.getName()));
			return resource;
		}
	}

}
