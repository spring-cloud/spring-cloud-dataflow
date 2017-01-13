/*
 * Copyright 2015-2017 the original author or authors.
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
import java.util.Properties;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.DetailedAppRegistrationResource;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles all {@link AppRegistry} related interactions.
 *
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Eric Bottard
 * @author Gary Russell
 * @author Patrick Peralta
 * @author Thomas Risberg
 */
@RestController
@RequestMapping("/apps")
@ExposesResourceFor(AppRegistrationResource.class)
public class AppRegistryController {

	private final Assembler assembler = new Assembler();

	private final AppRegistry appRegistry;

	private ApplicationConfigurationMetadataResolver metadataResolver;

	public AppRegistryController(AppRegistry appRegistry, ApplicationConfigurationMetadataResolver metadataResolver) {
		this.appRegistry = appRegistry;
		this.metadataResolver = metadataResolver;
	}

	/**
	 * List app registrations.
	 */
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<? extends AppRegistrationResource> list(
			PagedResourcesAssembler<AppRegistration> pagedResourcesAssembler,
			@RequestParam(value = "type", required = false) ApplicationType type,
			@RequestParam(value = "detailed", defaultValue = "false") boolean detailed) {

		List<AppRegistration> list = new ArrayList<>(appRegistry.findAll());
		for (Iterator<AppRegistration> iterator = list.iterator(); iterator.hasNext(); ) {
			ApplicationType applicationType = iterator.next().getType();
			if (type != null && applicationType != type) {
				iterator.remove();
			}
		}
		Collections.sort(list);
		return pagedResourcesAssembler.toResource(new PageImpl<>(list), assembler);
	}

	/**
	 * Retrieve detailed information about a particular application.
	 * @param type application type
	 * @param name application name
	 * @return detailed application information
	 */
	@RequestMapping(value = "/{type}/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public DetailedAppRegistrationResource info(
			@PathVariable("type") ApplicationType type,
			@PathVariable("name") String name) {
		AppRegistration registration = appRegistry.find(name, type);
		if (registration == null) {
			throw new NoSuchAppRegistrationException(name, type);
		}
		DetailedAppRegistrationResource result = new DetailedAppRegistrationResource(assembler.toResource(registration));
		Resource resource = registration.getResource();

		List<ConfigurationMetadataProperty> properties = metadataResolver.listProperties(resource);
		for (ConfigurationMetadataProperty property : properties) {
			result.addOption(property);
		}
		return result;
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
			@PathVariable("type") ApplicationType type,
			@PathVariable("name") String name,
			@RequestParam("uri") String uri,
			@RequestParam(value = "force", defaultValue = "false") boolean force) {
		AppRegistration previous = appRegistry.find(name, type);
		if (!force && previous != null) {
			throw new AppAlreadyRegisteredException(previous);
		}
		try {
			appRegistry.save(name, type, new URI(uri));
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Unregister an application by name and type. If the application does not
	 * exist, a {@link NoSuchAppRegistrationException} will be thrown.
	 *
	 * @param type the application type
	 * @param name the application name
	 */
	@RequestMapping(value = "/{type}/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void unregister(@PathVariable("type") ApplicationType type, @PathVariable("name") String name) {
		appRegistry.delete(name, type);
	}

	/**
	 * Register all applications listed in a properties file or provided as key/value pairs.
	 * @param uri         URI for the properties file
	 * @param apps        key/value pairs representing applications, separated by newlines
	 * @param force       if {@code true}, overwrites any pre-existing registrations
	 */
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public PagedResources<? extends AppRegistrationResource> registerAll(
			PagedResourcesAssembler<AppRegistration> pagedResourcesAssembler,
			@RequestParam(value = "uri", required = false) String uri,
			@RequestParam(value = "apps", required = false) Properties apps,
			@RequestParam(value = "force", defaultValue = "false") boolean force) {
		List<AppRegistration> registrations = new ArrayList<>();
		if (StringUtils.hasText(uri)) {
			registrations.addAll(appRegistry.importAll(force, uri));
		}
		else if (!CollectionUtils.isEmpty(apps)) {
			for (String key : apps.stringPropertyNames()) {
				String[] tokens = key.split("\\.", 2);
				if (tokens.length != 2) {
					throw new IllegalArgumentException("Invalid application key: " + key +
							"; the expected format is <name>.<type>");
				}
				String name = tokens[1];
				ApplicationType type = ApplicationType.valueOf(tokens[0]);
				if (force || null == appRegistry.find(name, type)) {
					try {
						registrations.add(appRegistry.save(name, type, new URI(apps.getProperty(key))));
					}
					catch (URISyntaxException e) {
						throw new IllegalArgumentException(e);
					}
				}
			}
		}
		Collections.sort(registrations);
		return pagedResourcesAssembler.toResource(new PageImpl<>(registrations), assembler);
	}

	class Assembler extends ResourceAssemblerSupport<AppRegistration, AppRegistrationResource> {

		public Assembler() {
			super(AppRegistryController.class, AppRegistrationResource.class);
		}

		@Override
		public AppRegistrationResource toResource(AppRegistration registration) {
			return createResourceWithId(String.format("%s/%s", registration.getType(), registration.getName()), registration);
		}

		@Override
		protected AppRegistrationResource instantiateResource(AppRegistration registration) {
			return new AppRegistrationResource(registration.getName(),
					registration.getType().name(), registration.getUri().toString());
		}
	}
}
