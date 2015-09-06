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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.ModuleCoordinates;
import org.springframework.cloud.dataflow.core.ModuleType;
import org.springframework.cloud.dataflow.module.registry.ModuleRegistration;
import org.springframework.cloud.dataflow.module.registry.ModuleRegistry;
import org.springframework.cloud.dataflow.rest.resource.DetailedModuleRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.ModuleRegistrationResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles all Module related interactions.
 *
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Eric Bottard
 * @author Gary Russell
 * @author Patrick Peralta
 */
@RestController
@RequestMapping("/modules")
@ExposesResourceFor(ModuleRegistrationResource.class)
public class ModuleController {

	private final Assembler moduleAssembler = new Assembler();

	private final ModuleRegistry registry;

	@Autowired
	public ModuleController(ModuleRegistry registry) {
		this.registry = registry;
	}

	/**
	 * List module registrations.
	 */
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<? extends ModuleRegistrationResource> list(
			PagedResourcesAssembler<ModuleRegistration> assembler,
			@RequestParam(value = "type", required = false) ModuleType type,
			@RequestParam(value = "detailed", defaultValue = "false") boolean detailed) {

		List<ModuleRegistration> list = new ArrayList<>(registry.findAll());
		if (type != null) {
			for (Iterator<ModuleRegistration> iterator = list.iterator(); iterator.hasNext();) {
				if (iterator.next().getType() != type) {
					iterator.remove();
				}
			}
		}
		Collections.sort(list);
		return assembler.toResource(new PageImpl<>(list), moduleAssembler);
	}

	/**
	 * Retrieve detailed information about a particular module.
	 *
	 * @param type  module type
	 * @param name  module name
	 * @return detailed module information
	 */
	@RequestMapping(value = "/{type}/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public DetailedModuleRegistrationResource info(
			@PathVariable("type") ModuleType type,
			@PathVariable("name") String name) {
		ModuleRegistration registration = registry.find(name, type);

		return (registration == null ? null :
				new DetailedModuleRegistrationResource(moduleAssembler.toResource(registration)));
	}

	/**
	 * Register a module name and type with its Maven coordinates.
	 *
	 * @param type  module type
	 * @param name  module name
	 * @param coordinates  Maven coordinates for the module artifact
	 * @param force if {@code true}, overwrites a pre-existing registration
	 */
	@RequestMapping(value = "/{type}/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void register(
			@PathVariable("type") ModuleType type,
			@PathVariable("name") String name,
			@RequestParam("coordinates") String coordinates,
			@RequestParam(value = "force", defaultValue = "false") boolean force) {
		if (!force && registry.find(name, type) != null) {
			return;
		}
		registry.save(new ModuleRegistration(name, type, ModuleCoordinates.parse(coordinates)));
	}

	/**
	 * Unregister a module name and type.
	 *
	 * @param type the module type
	 * @param name the module name
	 */
	@RequestMapping(value = "/{type}/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void unregister(@PathVariable("type") ModuleType type, @PathVariable("name") String name) {
		registry.delete(name, type);
	}

	class Assembler extends ResourceAssemblerSupport<ModuleRegistration, ModuleRegistrationResource> {

		public Assembler() {
			super(ModuleController.class, ModuleRegistrationResource.class);
		}

		@Override
		public ModuleRegistrationResource toResource(ModuleRegistration registration) {
			return new ModuleRegistrationResource(registration.getName(),
					registration.getType().name(), registration.getCoordinates().toString());
		}
	}

}
