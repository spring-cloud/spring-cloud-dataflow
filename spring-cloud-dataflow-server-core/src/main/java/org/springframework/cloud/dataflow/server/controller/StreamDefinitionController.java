/*
 * Copyright 2015-2016 the original author or authors.
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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.BindingPropertyKeys;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.server.DataFlowServerUtil;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentKey;
import org.springframework.cloud.dataflow.server.repository.DuplicateStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
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
	 * Assembler for {@link StreamDefinitionResource} objects.
	 */
	private final Assembler streamDefinitionAssembler = new Assembler();

	/**
	 * This deployment controller is used as a delegate when stream creation is immediately followed by deployment.
	 */
	private final StreamDeploymentController deploymentController;

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
	 * @return list of stream definitions
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StreamDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<StreamDefinition> assembler) {
		return assembler.toResource(repository.findAll(pageable), streamDefinitionAssembler);
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
			String appName = streamAppDefinition.getRegisteredAppName();
			ApplicationType appType = DataFlowServerUtil.determineApplicationType(streamAppDefinition);
			if (appRegistry.find(appName, appType) == null) {
				errorMessages.add(String.format("Application name '%s' with type '%s' does not exist in the app registry.",
						appName, appType));
			}
		}
		if (!errorMessages.isEmpty()) {
			throw new IllegalArgumentException(StringUtils.collectionToDelimitedString(errorMessages, System.lineSeparator()));
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
		return streamDefinitionAssembler.toResource(definition);
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
	 * Return a string that describes the state of the given stream.
	 * @param name name of stream to determine state for
	 * @return stream state
	 * @see DeploymentState
	 */
	private String calculateStreamState(String name) {
		Set<DeploymentState> appStates = EnumSet.noneOf(DeploymentState.class);
		StreamDefinition stream = repository.findOne(name);
		for (StreamAppDefinition appDefinition : stream.getAppDefinitions()) {
			String key = DeploymentKey.forStreamAppDefinition(appDefinition);
			String id = deploymentIdRepository.findOne(key);
			if (id != null) {
				AppStatus status = deployer.status(id);
				appStates.add(status.getState());
			}
			else {
				appStates.add(DeploymentState.undeployed);
			}
		}

		logger.debug("Application states for stream {}: {}", name, appStates);
		return aggregateState(appStates).toString();
	}

	/**
	 * Aggregate the set of app states into a single state for a stream.
	 * @param states set of states for apps of a stream
	 * @return stream state based on app states
	 */
	static DeploymentState aggregateState(Set<DeploymentState> states) {
		if (states.size() == 1) {
			DeploymentState state = states.iterator().next();

			// a stream which is known to the stream definition repository
			// but unknown to deployers is undeployed
			return state == DeploymentState.unknown ? DeploymentState.undeployed : state;
		}
		if (states.isEmpty() || states.contains(DeploymentState.error)) {
			return DeploymentState.error;
		}
		if (states.contains(DeploymentState.failed)) {
			return DeploymentState.failed;
		}
		if (states.contains(DeploymentState.deploying)) {
			return DeploymentState.deploying;
		}

		return DeploymentState.partial;
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts {@link StreamDefinition}s to {@link StreamDefinitionResource}s.
	 */
	class Assembler extends ResourceAssemblerSupport<StreamDefinition, StreamDefinitionResource> {

		public Assembler() {
			super(StreamDefinitionController.class, StreamDefinitionResource.class);
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
