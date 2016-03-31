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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.app.resolver.Coordinates;
import org.springframework.cloud.dataflow.app.resolver.ModuleResolver;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistration;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistry;
import org.springframework.cloud.dataflow.core.ArtifactCoordinates;
import org.springframework.cloud.dataflow.core.ArtifactType;
import org.springframework.cloud.dataflow.rest.resource.DetailedModuleRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.ModuleRegistrationResource;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.stream.configuration.metadata.ModuleConfigurationMetadataResolver;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
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

	private final ArtifactRegistry artifactRegistry;

	private final UriRegistry registry;

	private final ModuleResolver moduleResolver;

	private ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver;

	public ModuleController(ArtifactRegistry artifactRegistry, UriRegistry registry, ModuleResolver moduleResolver,
			ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver) {
		this.artifactRegistry = artifactRegistry;
		this.registry = registry;
		this.moduleResolver = moduleResolver;
		this.moduleConfigurationMetadataResolver = moduleConfigurationMetadataResolver;
	}

	/**
	 * List module registrations.
	 */
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<? extends ModuleRegistrationResource> list(
			PagedResourcesAssembler<ArtifactRegistration> assembler,
			@RequestParam(value = "type", required = false) ArtifactType type,
			@RequestParam(value = "detailed", defaultValue = "false") boolean detailed) {

		List<ArtifactRegistration> list = new ArrayList<>(artifactRegistry.findAll());
		for (Iterator<ArtifactRegistration> iterator = list.iterator(); iterator.hasNext(); ) {
			ArtifactType artifactType = iterator.next().getType();
			if ((type != null && artifactType != type) || artifactType == ArtifactType.library) {
				iterator.remove();
			}
		}
		Collections.sort(list);
		return assembler.toResource(new PageImpl<>(list), moduleAssembler);
	}

	/**
	 * Retrieve detailed information about a particular module.
	 * @param type module type
	 * @param name module name
	 * @return detailed module information
	 */
	@RequestMapping(value = "/{type}/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public DetailedModuleRegistrationResource info(
			@PathVariable("type") ArtifactType type,
			@PathVariable("name") String name) {
		Assert.isTrue(type != ArtifactType.library, "Only modules are supported by this endpoint");
		ArtifactRegistration registration = artifactRegistry.find(name, type);
		if (registration == null) {
			return null;
		}
		DetailedModuleRegistrationResource result = new DetailedModuleRegistrationResource(moduleAssembler.toResource(registration));
		Resource resource = moduleResolver.resolve(adapt(registration.getCoordinates()));

		List<ConfigurationMetadataProperty> properties = moduleConfigurationMetadataResolver.listProperties(resource);
		for (ConfigurationMetadataProperty property : properties) {
			result.addOption(property);
		}
		return result;
	}

	private Coordinates adapt(ArtifactCoordinates coordinates) {
		return new Coordinates(coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getExtension(), "exec", coordinates.getVersion());
	}

	/**
	 * Register a module name and type with its URI.
	 * @param type        module type
	 * @param name        module name
	 * @param uri         URI for the module artifact (e.g. {@literal maven://group:artifact:version})
	 * @param force       if {@code true}, overwrites a pre-existing registration
	 */
	@RequestMapping(value = "/{type}/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void register(
			@PathVariable("type") ArtifactType type,
			@PathVariable("name") String name,
			@RequestParam("uri") String uri,
			@RequestParam(value = "force", defaultValue = "false") boolean force) {
		Assert.isTrue(type != ArtifactType.library, "Only modules are supported by this endpoint");
		ArtifactRegistration previous = artifactRegistry.find(name, type);
		if (!force && previous != null) {
			throw new ModuleAlreadyRegisteredException(previous);
		}
		if (uri.startsWith("maven:")) {
			String coordinates = uri.replaceFirst("maven:\\/*", "");
			artifactRegistry.save(new ArtifactRegistration(name, type, ArtifactCoordinates.parse(coordinates)));
		}
		if (this.registry != null) {
			try {
				this.registry.register(String.format("%s.%s", type, name), new URI(uri));
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	/**
	 * Unregister a module name and type.
	 * @param type the module type
	 * @param name the module name
	 */
	@RequestMapping(value = "/{type}/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void unregister(@PathVariable("type") ArtifactType type, @PathVariable("name") String name) {
		Assert.isTrue(type != ArtifactType.library, "Only modules are supported by this endpoint");
		artifactRegistry.delete(name, type);
		if (this.registry != null) {
			this.registry.unregister(String.format("%s.%s", type, name));
		}
	}

	class Assembler extends ResourceAssemblerSupport<ArtifactRegistration, ModuleRegistrationResource> {

		public Assembler() {
			super(ModuleController.class, ModuleRegistrationResource.class);
		}

		@Override
		public ModuleRegistrationResource toResource(ArtifactRegistration registration) {
			return createResourceWithId(String.format("%s/%s", registration.getType(), registration.getName()), registration);
		}

		@Override
		protected ModuleRegistrationResource instantiateResource(ArtifactRegistration registration) {
			return new ModuleRegistrationResource(registration.getName(),
					registration.getType().name(), registration.getCoordinates().toString());
		}
	}

	@ResponseStatus(HttpStatus.CONFLICT)
	public static class ModuleAlreadyRegisteredException extends IllegalStateException {
		private final ArtifactRegistration previous;

		public ModuleAlreadyRegisteredException(ArtifactRegistration previous) {
			this.previous = previous;
		}

		@Override
		public String getMessage() {
			return String.format("The '%s:%s' module is already registered as %s", previous.getType(), previous.getName(), previous.getCoordinates());
		}

		public ArtifactRegistration getPrevious() {
			return previous;
		}
	}

}
