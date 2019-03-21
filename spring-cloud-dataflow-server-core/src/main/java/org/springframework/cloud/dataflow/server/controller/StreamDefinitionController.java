/*
 * Copyright 2015-2017 the original author or authors.
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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.dsl.StreamNode;
import org.springframework.cloud.dataflow.core.dsl.StreamParser;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.server.DataFlowServerUtil;
import org.springframework.cloud.dataflow.server.controller.support.InvalidStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentKey;
import org.springframework.cloud.dataflow.server.repository.DuplicateStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.support.SearchPageable;
import org.springframework.cloud.dataflow.server.support.CannotDetermineApplicationTypeException;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.app.MultiStateAppDeployer;
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
 * Controller for operations on {@link StreamDefinition}. This
 * includes CRUD and optional deployment operations.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
@RestController
@RequestMapping("/streams/definitions")
@ExposesResourceFor(StreamDefinitionResource.class)
public class StreamDefinitionController {

	private static final Logger logger = LoggerFactory.getLogger(StreamDefinitionController.class);

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final StreamDefinitionRepository repository;

	/**
	 * The repository this controller will use for deployment IDs.
	 */
	private final DeploymentIdRepository deploymentIdRepository;

	/**
	 * The deployer this controller will use to compute stream deployment status.
	 */
	private final AppDeployer deployer;

	/**
	 * The app registry this controller will use to lookup apps.
	 */
	private final AppRegistry appRegistry;

	/**
	 * This deployment controller is used as a delegate when stream creation is immediately followed by deployment.
	 */
	private final StreamDeploymentController deploymentController;

	private static final Map<DeploymentState, String> PRETTY_STATES = new EnumMap<>(DeploymentState.class);
	static {
		PRETTY_STATES.put(DeploymentState.deployed, "Deployed");
		PRETTY_STATES.put(DeploymentState.deploying, "Deploying");
		PRETTY_STATES.put(DeploymentState.undeployed, "Undeployed");
		PRETTY_STATES.put(DeploymentState.error, "Error retrieving state");
		PRETTY_STATES.put(DeploymentState.failed, "Failed");
		PRETTY_STATES.put(DeploymentState.partial, "Partial");
		// unknown not mapped on purpose
		Assert.isTrue(PRETTY_STATES.size() == DeploymentState.values().length - 1);
	}

	/**
	 * Create a {@code StreamDefinitionController} that delegates
	 * <ul>
	 * <li>CRUD operations to the provided {@link StreamDefinitionRepository}</li>
	 * <li>deployment ID operations to the provided {@link DeploymentIdRepository}</li>
	 * <li>deployment operations to the provided {@link StreamDeploymentController}</li>
	 * <li>deployment status computation to the provided {@link AppDeployer}</li>
	 * </ul>
	 * @param repository           the repository this controller will use for stream CRUD operations
	 * @param deploymentIdRepository the repository this controller will use for deployment IDs
	 * @param deploymentController the deployment controller to delegate deployment operations
	 * @param deployer             the deployer this controller will use to compute deployment status
	 * @param appRegistry          the app registry to look up registered apps
	 */
	public StreamDefinitionController(StreamDefinitionRepository repository, DeploymentIdRepository deploymentIdRepository,
			StreamDeploymentController deploymentController, AppDeployer deployer, AppRegistry appRegistry) {
		Assert.notNull(repository, "StreamDefinitionRepository must not be null");
		Assert.notNull(deploymentIdRepository, "DeploymentIdRepository must not be null");
		Assert.notNull(deploymentController, "StreamDeploymentController must not be null");
		Assert.notNull(deployer, "AppDeployer must not be null");
		Assert.notNull(appRegistry, "AppRegistry must not be null");
		this.deploymentController = deploymentController;
		this.deploymentIdRepository = deploymentIdRepository;
		this.repository = repository;
		this.deployer = deployer;
		this.appRegistry = appRegistry;
	}

	/**
	 * Return a page-able list of {@link StreamDefinitionResource} defined streams.
	 * @param pageable  page-able collection of {@code StreamDefinitionResource}s.
	 * @param assembler assembler for {@link StreamDefinition}
	 * @param search optional search parameter
	 * @return list of stream definitions
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StreamDefinitionResource> list(Pageable pageable, @RequestParam(required=false) String search,
			PagedResourcesAssembler<StreamDefinition> assembler) {
		Page<StreamDefinition> streamDefinitions;
		if (search != null) {
			final SearchPageable searchPageable = new SearchPageable(pageable, search);
			searchPageable.addColumns("DEFINITION_NAME", "DEFINITION");
			streamDefinitions = repository.search(searchPageable);
		}
		else {
			streamDefinitions = repository.findAll(pageable);
		}
		return assembler.toResource(streamDefinitions, new Assembler(streamDefinitions));
	}

	/**
	 * Create a new stream.
	 *
	 * @param name   stream name
	 * @param dsl    DSL definition for stream
	 * @param deploy if {@code true}, the stream is deployed upon creation (default is {@code false})
	 * @throws DuplicateStreamDefinitionException if a stream definition with the same name already exists
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void save(@RequestParam("name") String name,
					 @RequestParam("definition") String dsl,
					 @RequestParam(value = "deploy", defaultValue = "false")
					 boolean deploy) {
		StreamDefinition stream = new StreamDefinition(name, dsl);
		List<String> errorMessages = new ArrayList<>();
		for (StreamAppDefinition streamAppDefinition: stream.getAppDefinitions()) {
			final String appName = streamAppDefinition.getRegisteredAppName();
			final ApplicationType appType;
			try {
				appType = DataFlowServerUtil.determineApplicationType(streamAppDefinition);
			}
			catch (CannotDetermineApplicationTypeException e) {
				errorMessages.add(String.format("Cannot determine application type for application '%s': %s",
						appName, e.getMessage()));
				continue;
			}
			if (appRegistry.find(appName, appType) == null) {
				errorMessages.add(String.format("Application name '%s' with type '%s' does not exist in the app registry.",
						appName, appType));
			}
		}
		if (!errorMessages.isEmpty()) {
			throw new InvalidStreamDefinitionException(StringUtils.collectionToDelimitedString(errorMessages, System.lineSeparator()));
		}
		this.repository.save(stream);
		if (deploy) {
			deploymentController.deploy(name, null);
		}
	}

	/**
	 * Request removal of an existing stream definition.
	 * @param name the name of an existing stream definition (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name") String name) {
		if (repository.findOne(name) == null) {
			throw new NoSuchStreamDefinitionException(name);
		}
		deploymentController.undeploy(name);
		this.repository.delete(name);
	}

	/**
	 * Return a list of related stream definition resources based on the given stream name.
	 * Related streams include the main stream and the tap stream(s) on the main stream.
	 *
	 * @param name the name of an existing stream definition (required)
	 */
	@RequestMapping(value = "/{name}/related", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StreamDefinitionResource> listRelated(@PathVariable("name") String name,
																@RequestParam(value="nested", required = false, defaultValue = "false") boolean nested,
																PagedResourcesAssembler<StreamDefinition> assembler) {
		Set<StreamDefinition> relatedDefinitions = new LinkedHashSet<>();
		StreamDefinition currentStreamDefinition = repository.findOne(name);
		if (currentStreamDefinition == null) {
			throw new NoSuchStreamDefinitionException(name);
		}
		Iterable<StreamDefinition> definitions = repository.findAll();
		List<StreamDefinition> result = new ArrayList<>(findRelatedDefinitions(currentStreamDefinition, definitions,
				relatedDefinitions, nested));
		Page<StreamDefinition> page = new PageImpl<>(result);
		return assembler.toResource(page, new Assembler(page));
	}

	private Set<StreamDefinition> findRelatedDefinitions(StreamDefinition currentStreamDefinition, Iterable<StreamDefinition> definitions,
														  Set<StreamDefinition> relatedDefinitions, boolean nested) {
		relatedDefinitions.add(currentStreamDefinition);
		String currentStreamName = currentStreamDefinition.getName();
		String indexedStreamName = currentStreamName + ".";
		for (StreamDefinition definition: definitions) {
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
	 * @param name the name of an existing stream definition (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public StreamDefinitionResource display(@PathVariable("name") String name) {
		StreamDefinition definition = repository.findOne(name);
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
	public void deleteAll() throws Exception {
		deploymentController.undeployAll();
		this.repository.deleteAll();
	}

	/**
	 * Aggregate the set of app states into a single state for a stream.
	 * @param states set of states for apps of a stream
	 * @return stream state based on app states
	 */
	static DeploymentState aggregateState(Set<DeploymentState> states) {
		if (states.size() == 1) {
			DeploymentState state = states.iterator().next();
			logger.debug("aggregateState: Deployment State Set Size = 1.  Deployment State " + state);
			// a stream which is known to the stream definition repository
			// but unknown to deployers is undeployed
			if (state == DeploymentState.unknown) {
				logger.debug("aggregateState: Returning " + DeploymentState.undeployed );
				return  DeploymentState.undeployed;
			} else {
				logger.debug("aggregateState: Returning " + state );
				return state;
			}
		}
		if (states.isEmpty() || states.contains(DeploymentState.error)) {
			logger.debug("aggregateState: Returning " + DeploymentState.error );
			return DeploymentState.error;
		}
		if (states.contains(DeploymentState.failed)) {
			logger.debug("aggregateState: Returning " + DeploymentState.failed );
			return DeploymentState.failed;
		}
		if (states.contains(DeploymentState.deploying)) {
			logger.debug("aggregateState: Returning " + DeploymentState.deploying );
			return DeploymentState.deploying;
		}

		logger.debug("aggregateState: Returing " + DeploymentState.partial );
		return DeploymentState.partial;
	}

	private Map<String, DeploymentState> gatherDeploymentStates(String... ids) {
		if (deployer instanceof MultiStateAppDeployer) {
			return ((MultiStateAppDeployer) deployer).states(ids);
		} else {
			return Arrays.stream(ids).collect(
				Collectors.toMap(Function.identity(), id -> deployer.status(id).getState())
			);
		}
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts {@link StreamDefinition}s to {@link StreamDefinitionResource}s.
	 */
	class Assembler extends ResourceAssemblerSupport<StreamDefinition, StreamDefinitionResource> {


		private final Map<StreamDefinition, DeploymentState> streamDeploymentStates;

		public Assembler(Page<StreamDefinition> streamDefinitions) {
			super(StreamDefinitionController.class, StreamDefinitionResource.class);

			Map<StreamDefinition, List<String>> deploymentIdsPerStream = streamDefinitions.getContent().stream()
				.collect(
					Collectors.toMap(
						Function.identity(),
						sd -> sd.getAppDefinitions().stream()
							.map(sad -> deploymentIdRepository.findOne(DeploymentKey.forStreamAppDefinition(sad)))
							.collect(Collectors.toList())
					)
				);

			// Map from app deployment id to state
			Map<String, DeploymentState> statePerApp = gatherDeploymentStates(
				deploymentIdsPerStream.values().stream()
					.flatMap(Collection::stream)
					.filter(Objects::nonNull)
					.toArray(String[]::new));

			// Map from SCDF Stream to aggregate state
			streamDeploymentStates = deploymentIdsPerStream.entrySet().stream()
				.map(kv -> new AbstractMap.SimpleImmutableEntry<>(kv.getKey(),
					aggregateState(kv.getValue().stream()
						.map(deploymentId -> statePerApp.getOrDefault(deploymentId, DeploymentState.unknown))
						.collect(Collectors.toSet())
					))
				).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		}

		@Override
		public StreamDefinitionResource toResource(StreamDefinition stream) {
			return createResourceWithId(stream.getName(), stream);
		}

		@Override
		public StreamDefinitionResource instantiateResource(StreamDefinition stream) {
			StreamDefinitionResource resource = new StreamDefinitionResource(stream.getName(), stream.getDslText());
			resource.setStatus(mapState(streamDeploymentStates.get(stream)));
			return resource;
		}

		private String mapState(DeploymentState state) {
			String result = PRETTY_STATES.get(state);
			Assert.notNull(result, "Trying to display a DeploymentState that should not appear here: " + state);
			return result;
		}
	}
}
