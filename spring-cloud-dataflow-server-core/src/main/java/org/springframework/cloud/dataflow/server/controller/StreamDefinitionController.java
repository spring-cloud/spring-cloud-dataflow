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

import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.server.repository.AppDeploymentKey;
import org.springframework.cloud.dataflow.server.repository.AppDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.DuplicateStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
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
	 * The repository this controller will use for app deployment operations.
	 */
	private final AppDeploymentRepository appDeploymentRepository;

	/**
	 * The deployer this controller will use to compute stream deployment status.
	 */
	private final AppDeployer deployer;

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
	 * <li>deployment operations to the provided {@link StreamDeploymentController}</li>
	 * <li>deployment status computation to the provided {@link AppDeployer}</li>
	 * </ul>
	 * @param repository           the repository this controller will use for stream CRUD operations
	 * @param appDeploymentRepository the repository this controller will use for app deployment operations
	 * @param deploymentController the deployment controller to delegate deployment operations
	 * @param deployer             the deployer this controller will use to compute deployment status
	 */
	public StreamDefinitionController(StreamDefinitionRepository repository, AppDeploymentRepository appDeploymentRepository,
			StreamDeploymentController deploymentController, AppDeployer deployer) {
		Assert.notNull(repository, "repository must not be null");
		Assert.notNull(appDeploymentRepository, "appDeploymentRepository must not be null");
		Assert.notNull(deploymentController, "deploymentController must not be null");
		Assert.notNull(deployer, "deployer must not be null");
		this.deploymentController = deploymentController;
		this.appDeploymentRepository = appDeploymentRepository;
		this.repository = repository;
		this.deployer = deployer;
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
	 * @param deploy if {@code true}, the stream is deployed upon creation
	 * @throws DuplicateStreamDefinitionException if a stream definition with the same name already exists
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void save(@RequestParam("name") String name,
					@RequestParam("definition") String dsl,
					@RequestParam(value = "deploy", defaultValue = "true")
					boolean deploy) {
		StreamDefinition stream = new StreamDefinition(name, dsl);
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
		Set<DeploymentState> moduleStates = EnumSet.noneOf(DeploymentState.class);
		StreamDefinition stream = repository.findOne(name);
		for (ModuleDefinition module : stream.getModuleDefinitions()) {
			AppDeploymentKey key = new AppDeploymentKey(stream, module);
			String id = appDeploymentRepository.findOne(key);
			AppStatus status = deployer.status(id);
			moduleStates.add(status.getState());
		}

		logger.debug("Module states for stream {}: {}", name, moduleStates);
		return aggregateState(moduleStates).toString();
	}

	/**
	 * Aggregate the set of module states into a single state for a stream.
	 * @param states set of states for modules of a stream
	 * @return stream state based on module states
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
