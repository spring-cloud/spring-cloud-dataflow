/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.dsl.ParseException;
import org.springframework.cloud.dataflow.core.dsl.StreamNode;
import org.springframework.cloud.dataflow.core.dsl.StreamParser;
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.rest.resource.DeploymentStateResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.server.DataFlowServerUtil;
import org.springframework.cloud.dataflow.server.controller.support.ArgumentSanitizer;
import org.springframework.cloud.dataflow.server.controller.support.ControllerUtils;
import org.springframework.cloud.dataflow.server.controller.support.InvalidStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.DuplicateStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.support.SearchPageable;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.support.CannotDetermineApplicationTypeException;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link StreamDefinition}. This includes CRUD and optional
 * deployment operations.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * @author Oleg Zhurakousky
 * @author Glenn Renfro
 */
@RestController
@RequestMapping("/streams/definitions")
@ExposesResourceFor(StreamDefinitionResource.class)
public class StreamDefinitionController {

	private static final Logger logger = LoggerFactory.getLogger(StreamDefinitionController.class);

	/**
	 * The streamDefinitionRepository this controller will use for stream CRUD operations.
	 */
	private final StreamDefinitionRepository streamDefinitionRepository;

	/**
	 * The app registry this controller will use to lookup apps.
	 */
	private final AppRegistryCommon appRegistry;

	/**
	 * The service that is responsible for deploying streams.
	 */
	private final StreamService streamService;

	/**
	 * Create a {@code StreamDefinitionController} that delegates
	 * <ul>
	 * <li>CRUD operations to the provided {@link StreamDefinitionRepository}</li>
	 * <li>deployment operations and status computation via {@link StreamService}</li>
	 * </ul>
	 *
	 * @param repository the streamDefinitionRepository this controller will use for stream
	 * CRUD operations
	 * @param appRegistry the app registry to look up registered apps
	 * @param streamService the stream service to use to delegate stream operations such as
	 * deploy/status.
	 */
	public StreamDefinitionController(StreamDefinitionRepository repository, AppRegistryCommon appRegistry,
			StreamService streamService) {
		Assert.notNull(repository, "StreamDefinitionRepository must not be null");
		Assert.notNull(appRegistry, "AppRegistry must not be null");
		Assert.notNull(streamService, "StreamService must not be null");
		this.streamDefinitionRepository = repository;
		this.appRegistry = appRegistry;
		this.streamService = streamService;
	}

	/**
	 * Aggregate the set of app states into a single state for a stream.
	 *
	 * @param states set of states for apps of a stream
	 * @return the stream state based on app states
	 */
	public static DeploymentState aggregateState(Set<DeploymentState> states) {
		if (states.size() == 1) {
			DeploymentState state = states.iterator().next();
			logger.debug("aggregateState: Deployment State Set Size = 1.  Deployment State " + state);
			// a stream which is known to the stream definition streamDefinitionRepository
			// but unknown to deployers is undeployed
			if (state == DeploymentState.unknown) {
				logger.debug("aggregateState: Returning " + DeploymentState.undeployed);
				return DeploymentState.undeployed;
			}
			else {
				logger.debug("aggregateState: Returning " + state);
				return state;
			}
		}
		if (states.isEmpty() || states.contains(DeploymentState.error)) {
			logger.debug("aggregateState: Returning " + DeploymentState.error);
			return DeploymentState.error;
		}
		if (states.contains(DeploymentState.deployed) && states.contains(DeploymentState.failed)) {
			logger.debug("aggregateState: Returning " + DeploymentState.partial);
			return DeploymentState.partial;
		}
		if (states.contains(DeploymentState.failed)) {
			logger.debug("aggregateState: Returning " + DeploymentState.failed);
			return DeploymentState.failed;
		}
		if (states.contains(DeploymentState.deploying)) {
			logger.debug("aggregateState: Returning " + DeploymentState.deploying);
			return DeploymentState.deploying;
		}

		logger.debug("aggregateState: Returing " + DeploymentState.partial);
		return DeploymentState.partial;
	}

	/**
	 * Return a page-able list of {@link StreamDefinitionResource} defined streams.
	 *
	 * @param pageable page-able collection of {@code StreamDefinitionResource}s.
	 * @param assembler assembler for {@link StreamDefinition}
	 * @param search optional search parameter
	 * @return list of stream definitions
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StreamDefinitionResource> list(Pageable pageable,
			@RequestParam(required = false) String search, PagedResourcesAssembler<StreamDefinition> assembler) {
		Page<StreamDefinition> streamDefinitions;
		if (search != null) {
			final SearchPageable searchPageable = new SearchPageable(pageable, search);
			searchPageable.addColumns("DEFINITION_NAME", "DEFINITION");
			streamDefinitions = streamDefinitionRepository.search(searchPageable);
			long count = streamDefinitions.getContent().size();
			long to = Math.min(count, pageable.getOffset() + pageable.getPageSize());
			streamDefinitions = new PageImpl<>(streamDefinitions.getContent(), pageable,
					streamDefinitions.getTotalElements());
		}
		else {
			streamDefinitions = streamDefinitionRepository.findAll(pageable);
		}
		return assembler.toResource(streamDefinitions, new Assembler(streamDefinitions));
	}

	/**
	 * Create a new stream.
	 *
	 * @param name stream name
	 * @param dsl DSL definition for stream
	 * @param deploy if {@code true}, the stream is deployed upon creation (default is
	 * {@code false})
	 * @return the created stream definition
	 * @throws DuplicateStreamDefinitionException if a stream definition with the same name
	 * already exists
	 * @throws InvalidStreamDefinitionException if there errors in parsing the stream DSL,
	 * resolving the name, or type of applications in the stream
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public StreamDefinitionResource save(@RequestParam("name") String name, @RequestParam("definition") String dsl,
			@RequestParam(value = "deploy", defaultValue = "false") boolean deploy) {
		StreamDefinition streamDefinition;
		try {
			streamDefinition = new StreamDefinition(name, dsl);
		}
		catch (ParseException ex) {
			throw new InvalidStreamDefinitionException(ex.getMessage());
		}
		List<String> errorMessages = new ArrayList<>();
		for (StreamAppDefinition streamAppDefinition : streamDefinition.getAppDefinitions()) {
			final String appName = streamAppDefinition.getRegisteredAppName();
			final ApplicationType appType;
			try {
				appType = DataFlowServerUtil.determineApplicationType(streamAppDefinition);
			}
			catch (CannotDetermineApplicationTypeException e) {
				errorMessages.add(String.format("Cannot determine application type for application '%s': %s", appName,
						e.getMessage()));
				continue;
			}
			if (!appRegistry.appExist(appName, appType)) {
				errorMessages.add(
						String.format("Application name '%s' with type '%s' does not exist in the app " + "registry.",
								appName, appType));
			}
		}
		if (!errorMessages.isEmpty()) {
			throw new InvalidStreamDefinitionException(
					StringUtils.collectionToDelimitedString(errorMessages, "\n"));
		}
		this.streamDefinitionRepository.save(streamDefinition);
		if (deploy) {
			this.streamService.deployStream(name, new HashMap<>());
		}
		return new Assembler(new PageImpl<>(Collections.singletonList(streamDefinition))).toResource(streamDefinition);
	}

	/**
	 * Request removal of an existing stream definition.
	 *
	 * @param name the name of an existing stream definition (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name") String name) {
		if (this.streamDefinitionRepository.findOne(name) == null) {
			throw new NoSuchStreamDefinitionException(name);
		}
		this.streamService.undeployStream(name);
		this.streamDefinitionRepository.delete(name);
	}

	/**
	 * Return a list of related stream definition resources based on the given stream name.
	 * Related streams include the main stream and the tap stream(s) on the main stream.
	 *
	 * @param name the name of an existing stream definition (required)
	 * @param nested if should recursively search for related stream definitions
	 * @param assembler resource assembler for stream definition
	 * @return a list of related stream definitions
	 */
	@RequestMapping(value = "/{name}/related", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StreamDefinitionResource> listRelated(Pageable pageable,
			@PathVariable("name") String name,
			@RequestParam(value = "nested", required = false, defaultValue = "false") boolean nested,
			PagedResourcesAssembler<StreamDefinition> assembler) {
		Set<StreamDefinition> relatedDefinitions = new LinkedHashSet<>();
		StreamDefinition currentStreamDefinition = streamDefinitionRepository.findOne(name);
		if (currentStreamDefinition == null) {
			throw new NoSuchStreamDefinitionException(name);
		}
		Iterable<StreamDefinition> definitions = streamDefinitionRepository.findAll();
		List<StreamDefinition> result = new ArrayList<>(findRelatedDefinitions(currentStreamDefinition, definitions,
				relatedDefinitions, nested));
		Page<StreamDefinition> page = new PageImpl<>(result, pageable,
				definitions.spliterator().getExactSizeIfKnown());
		return assembler.toResource(page, new Assembler(page));
	}

	private Set<StreamDefinition> findRelatedDefinitions(StreamDefinition currentStreamDefinition,
			Iterable<StreamDefinition> definitions, Set<StreamDefinition> relatedDefinitions, boolean nested) {
		relatedDefinitions.add(currentStreamDefinition);
		String currentStreamName = currentStreamDefinition.getName();
		String indexedStreamName = currentStreamName + ".";
		for (StreamDefinition definition : definitions) {
			StreamNode sn = new StreamParser(definition.getName(), definition.getDslText()).parse();
			if (sn.getSourceDestinationNode() != null) {
				String nameComponent = sn.getSourceDestinationNode().getDestinationName();
				if (nameComponent.equals(currentStreamName) || nameComponent.startsWith(indexedStreamName)) {
					relatedDefinitions.add(definition);
					if (nested) {
						findRelatedDefinitions(definition, definitions, relatedDefinitions, true);
					}
				}
			}
		}
		return relatedDefinitions;
	}

	/**
	 * Return a given stream definition resource.
	 *
	 * @param name the name of an existing stream definition (required)
	 * @return the stream definition
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public StreamDefinitionResource display(@PathVariable("name") String name) {
		StreamDefinition definition = streamDefinitionRepository.findOne(name);
		if (definition == null) {
			throw new NoSuchStreamDefinitionException(name);
		}
		return new Assembler(new PageImpl<>(Collections.singletonList(definition))).toResource(definition);
	}

	/**
	 * Request removal of all stream definitions.
	 */
	@RequestMapping(value = "", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void deleteAll() {
		for (StreamDefinition streamDefinition : this.streamDefinitionRepository.findAll()) {
			this.streamService.undeployStream(streamDefinition.getName());
		}
		this.streamDefinitionRepository.deleteAll();
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation that converts
	 * {@link StreamDefinition}s to {@link StreamDefinitionResource}s.
	 */
	class Assembler extends ResourceAssemblerSupport<StreamDefinition, StreamDefinitionResource> {

		private final Map<StreamDefinition, DeploymentState> streamDeploymentStates;

		public Assembler(Page<StreamDefinition> streamDefinitions) {
			super(StreamDefinitionController.class, StreamDefinitionResource.class);
			streamDeploymentStates = StreamDefinitionController.this.streamService
					.state(streamDefinitions.getContent());

		}

		@Override
		public StreamDefinitionResource toResource(StreamDefinition stream) {
			try {
				return createResourceWithId(stream.getName(), stream);
			}
			catch (IllegalStateException e) {
				logger.warn("Failed to create StreamDefinitionResource. " + e.getMessage());
			}
			return null;
		}

		@Override
		public StreamDefinitionResource instantiateResource(StreamDefinition stream) {
			final StreamDefinitionResource resource = new StreamDefinitionResource(stream.getName(),
					ArgumentSanitizer.sanitizeStream(stream.getDslText()));
			DeploymentState deploymentState = streamDeploymentStates.get(stream);
			if (deploymentState != null) {
				final DeploymentStateResource deploymentStateResource = ControllerUtils
						.mapState(deploymentState);
				resource.setStatus(deploymentStateResource.getKey());
				resource.setStatusDescription(deploymentStateResource.getDescription());
			}
			return resource;
		}

	}
}
