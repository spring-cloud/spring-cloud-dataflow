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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.data.core.StreamDefinition;
import org.springframework.cloud.data.module.deployer.ModuleDeployer;
import org.springframework.cloud.data.module.registry.ModuleRegistry;
import org.springframework.cloud.data.module.registry.StubModuleRegistry;
import org.springframework.cloud.data.repository.StreamDefinitionRepository;
import org.springframework.cloud.data.repository.StubStreamDefinitionRepository;
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
 * @author Mark Fisher
 */
@RestController
@RequestMapping("/streams")
@ExposesResourceFor(StreamDefinitionResource.class)
public class StreamController {
	private static final Logger logger = LoggerFactory.getLogger(StreamController.class);

	private final StreamDefinitionRepository repository = new StubStreamDefinitionRepository();

	private final ModuleRegistry registry = new StubModuleRegistry();

	private final ModuleDeployer deployer;

	/**
	 * Create a StreamController that delegates to the provided {@link ModuleDeployer}.
	 *
	 * @param moduleDeployer the deployer this controller will use to deploy stream modules.
	 */
	@Autowired
	public StreamController(ModuleDeployer moduleDeployer) {
		Assert.notNull(moduleDeployer, "moduleDeployer must not be null");
		this.deployer = moduleDeployer;
	}

	private final Assembler streamAssembler = new Assembler();

	@RequestMapping(value = "/definitions", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StreamDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<StreamDefinition> assembler) {

		return assembler.toResource(repository.findAll(pageable), streamAssembler);
	}

	@RequestMapping(value = "/definitions", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void save(@RequestParam("name") String name,
			@RequestParam("definition") String dsl,
			@RequestParam(value = "deploy", defaultValue = "true")
			boolean deploy) throws Exception {

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
