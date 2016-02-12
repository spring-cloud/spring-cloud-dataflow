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

package org.springframework.cloud.dataflow.server.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistration;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistry;
import org.springframework.cloud.dataflow.core.ArtifactCoordinates;
import org.springframework.cloud.dataflow.core.ArtifactType;
import org.springframework.cloud.dataflow.rest.resource.LibraryRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.ModuleRegistrationResource;
import org.springframework.cloud.stream.module.resolver.ModuleResolver;
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
 * Handles all Library related interactions.
 *
 * @author Eric Bottard
 */
@RestController
@RequestMapping("/libraries")
@ExposesResourceFor(LibraryRegistrationResource.class)
public class LibraryController {

	private final Assembler libraryAssembler = new Assembler();

	private final ArtifactRegistry registry;

	@Autowired
	private ModuleResolver moduleResolver;

	@Autowired
	public LibraryController(ArtifactRegistry registry) {
		this.registry = registry;
	}

	/**
	 * List library registrations.
	 */
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<? extends ModuleRegistrationResource> list(
			PagedResourcesAssembler<ArtifactRegistration> assembler) {
		List<ArtifactRegistration> list = new ArrayList<>(registry.findAll());
		for (Iterator<ArtifactRegistration> iterator = list.iterator(); iterator.hasNext(); ) {
			if (iterator.next().getType() != ArtifactType.library) {
				iterator.remove();
			}
		}
		Collections.sort(list);
		return assembler.toResource(new PageImpl<>(list), libraryAssembler);
	}

	/**
	 * Retrieve detailed information about a particular library.
	 *
	 * @param name library name
	 * @return library information
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public LibraryRegistrationResource info(
			@PathVariable("name") String name) {
		ArtifactRegistration registration = registry.find(name, ArtifactType.library);
		if (registration == null) {
			return null;
		}
		return libraryAssembler.toResource(registration);
	}

	/**
	 * Register a library name with its Maven coordinates.
	 *
	 * @param name        library name
	 * @param coordinates Maven coordinates for the library artifact (BOM)
	 * @param force       if {@code true}, overwrites a pre-existing registration
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void register(
			@PathVariable("name") String name,
			@RequestParam("coordinates") String coordinates,
			@RequestParam(value = "force", defaultValue = "false") boolean force) {
		ArtifactRegistration previous = registry.find(name, ArtifactType.library);
		if (!force && previous != null) {
			throw new LibraryAlreadyRegisteredException(previous);
		}
		registry.save(new ArtifactRegistration(name, ArtifactType.library, ArtifactCoordinates.parse(coordinates)));
	}

	/**
	 * Unregister a library given its name.
	 *
	 * @param name the module name
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void unregister(@PathVariable("name") String name) {
		registry.delete(name, ArtifactType.library);
	}

	class Assembler extends ResourceAssemblerSupport<ArtifactRegistration, LibraryRegistrationResource> {

		public Assembler() {
			super(LibraryController.class, LibraryRegistrationResource.class);
		}

		@Override
		public LibraryRegistrationResource toResource(ArtifactRegistration registration) {
			return createResourceWithId(registration.getName(), registration);
		}

		@Override
		protected LibraryRegistrationResource instantiateResource(ArtifactRegistration registration) {
			return new LibraryRegistrationResource(registration.getName(),
					registration.getType().name(), registration.getCoordinates().toString());
		}
	}

	@ResponseStatus(HttpStatus.CONFLICT)
	public static class LibraryAlreadyRegisteredException extends IllegalStateException {
		private final ArtifactRegistration previous;

		public LibraryAlreadyRegisteredException(ArtifactRegistration previous) {
			this.previous = previous;
		}

		@Override
		public String getMessage() {
			return String.format("The '%s' library is already registered as %s", previous.getName(), previous.getCoordinates());
		}

		public ArtifactRegistration getPrevious() {
			return previous;
		}
	}

}
