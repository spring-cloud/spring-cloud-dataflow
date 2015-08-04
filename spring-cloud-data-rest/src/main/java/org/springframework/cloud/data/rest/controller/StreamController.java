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

package org.springframework.cloud.data.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.data.core.StreamDefinition;
import org.springframework.cloud.data.module.deployer.ModuleDeployer;
import org.springframework.cloud.data.module.registry.ModuleRegistry;
import org.springframework.cloud.data.repository.StreamDefinitionRepository;
import org.springframework.cloud.data.rest.resource.StreamDefinitionResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
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
 */
@RestController
@RequestMapping("/streams")
@ExposesResourceFor(StreamDefinitionResource.class)
public class StreamController {

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
			ModuleDeployer deployer) {
		Assert.notNull(repository, "repository must not be null");
		Assert.notNull(registry, "registry must not be null");
		Assert.notNull(deployer, "deployer must not be null");
		this.repository = repository;
		this.registry = registry;
		this.deployer = deployer;
	}

	/**
	 * Return a page-able list of {@link StreamDefinitionResource defined streams}.
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

		StreamDefinition definition = new StreamDefinition(name, dsl);
		this.repository.save(definition);

		// todo: deploy
	}


	/**
	 * Extension of {@link StreamDefinitionResource.Assembler} that
	 * assembles {@link StreamDefinitionResource}s with stream status.
	 */
	class Assembler extends StreamDefinitionResource.Assembler {
		@Override
		public StreamDefinitionResource toResource(StreamDefinition entity) {

			StreamDefinitionResource resource = super.toResource(entity);
			// todo: set stream status based on SPI status
			resource.setStatus("undeployed");
			return resource;
		}
	}
}
