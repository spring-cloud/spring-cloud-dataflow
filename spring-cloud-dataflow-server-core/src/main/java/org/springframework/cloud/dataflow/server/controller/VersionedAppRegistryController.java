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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.registry.service.DefaultAppRegistryService;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.cloud.dataflow.registry.support.ResourceUtils;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.DetailedAppRegistrationResource;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
 * Handles all {@link DefaultAppRegistryService} related interactions.
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
public class VersionedAppRegistryController {

	private static final Logger logger = LoggerFactory.getLogger(VersionedAppRegistryController.class);

	private final Assembler assembler = new Assembler();

	private final AppRegistryService appRegistryService;

	private ApplicationConfigurationMetadataResolver metadataResolver;

	private ForkJoinPool forkJoinPool;

	private final MavenProperties mavenProperties;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	public VersionedAppRegistryController(AppRegistryService appRegistryService,
			ApplicationConfigurationMetadataResolver metadataResolver,
			ForkJoinPool forkJoinPool, MavenProperties mavenProperties) {
		this.appRegistryService = appRegistryService;
		this.metadataResolver = metadataResolver;
		this.forkJoinPool = forkJoinPool;
		this.mavenProperties = mavenProperties;
	}

	/**
	 * List app registrations. Optional type and search parameters can be passed to do
	 * filtering. Search parameter only filters by {@code AppRegistration} name field.
	 *
	 * @param pagedResourcesAssembler the resource assembler for app registrations
	 * @param type the application type: source, sink, processor, task
	 * @param search optional search parameter
	 * @return the list of registered applications
	 */
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<? extends AppRegistrationResource> list(
			Pageable pageable,
			PagedResourcesAssembler<AppRegistration> pagedResourcesAssembler,
			@RequestParam(value = "type", required = false) ApplicationType type,
			@RequestParam(required = false) String search) {

		Page<AppRegistration> pagedRegistrations = this.appRegistryService.findAllByTypeAndNameIsLike(type, search,
				pageable);

		return pagedResourcesAssembler.toResource(pagedRegistrations, this.assembler);
	}

	/**
	 * Retrieve detailed information about a particular application.
	 *
	 * @param type application type
	 * @param name application name
	 * @param version application version
	 * @return detailed application information
	 */
	@RequestMapping(value = "/{type}/{name}/{version:.+}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public DetailedAppRegistrationResource info(@PathVariable("type") ApplicationType type,
			@PathVariable("name") String name, @PathVariable("version") String version) {
		AppRegistration registration = appRegistryService.find(name, type, version);
		if (registration == null) {
			throw new NoSuchAppRegistrationException(name, type, version);
		}
		DetailedAppRegistrationResource result = new DetailedAppRegistrationResource(
				assembler.toResource(registration));
		List<ConfigurationMetadataProperty> properties = metadataResolver
				.listProperties(appRegistryService.getAppMetadataResource(registration));
		for (ConfigurationMetadataProperty property : properties) {
			result.addOption(property);
		}
		return result;
	}

	@Deprecated
	@RequestMapping(value = "/{type}/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public DetailedAppRegistrationResource info(@PathVariable("type") ApplicationType type,
			@PathVariable("name") String name) {
		if (!this.appRegistryService.appExist(name, type)) {
			throw new NoSuchAppRegistrationException(name, type);
		}
		if (this.appRegistryService.getDefaultApp(name, type) == null) {
			throw new RuntimeException(String.format("No default version exists for the app [%s:%s]", name, type));
		}
		String defaultVersion = this.appRegistryService.getDefaultApp(name, type).getVersion();
		return info(type, name, defaultVersion);
	}

	/**
	 * Register a module name and type with its URI.
	 *
	 * @param type module type
	 * @param name module name
	 * @param version module version
	 * @param uri URI for the module artifact (e.g. {@literal maven://group:artifact:version})
	 * @param metadataUri URI for the metadata artifact
	 * @param force if {@code true}, overwrites a pre-existing registration
	 */
	@RequestMapping(value = "/{type}/{name}/{version:.+}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void register(@PathVariable("type") ApplicationType type, @PathVariable("name") String name,
			@PathVariable("version") String version,
			@RequestParam("uri") String uri, @RequestParam(name = "metadata-uri", required = false) String metadataUri,
			@RequestParam(value = "force", defaultValue = "false") boolean force) {
		AppRegistration previous = appRegistryService.find(name, type, version);
		if (!force && previous != null) {
			throw new AppAlreadyRegisteredException(previous);
		}
		try {
			AppRegistration registration = this.appRegistryService.save(name, type, version, new URI(uri),
					metadataUri != null ? new URI(metadataUri) : null);
			prefetchMetadata(Arrays.asList(registration));
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Deprecated
	@RequestMapping(value = "/{type}/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void register(@PathVariable("type") ApplicationType type, @PathVariable("name") String name,
			@RequestParam("uri") String uri, @RequestParam(name = "metadata-uri", required = false) String metadataUri,
			@RequestParam(value = "force", defaultValue = "false") boolean force) {
		String version = ResourceUtils.getResourceVersion(uri, this.mavenProperties);
		this.register(type, name, version, uri, metadataUri, force);
	}

	/**
	 * Set a module version as default
	 *
	 * @param type module type
	 * @param name module name
	 * @param version module version
	 */
	@RequestMapping(value = "/{type}/{name}/{version:.+}", method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void makeDefault(@PathVariable("type") ApplicationType type, @PathVariable("name") String name,
			@PathVariable("version") String version) {
		this.appRegistryService.setDefaultApp(name, type, version);
	}

	/**
	 * Unregister an application by name and type. If the application does not exist, a
	 * {@link NoSuchAppRegistrationException} will be thrown.
	 *
	 * @param type the application type
	 * @param name the application name
	 * @param version application version
	 */
	@RequestMapping(value = "/{type}/{name}/{version:.+}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void unregister(@PathVariable("type") ApplicationType type, @PathVariable("name") String name,
			@PathVariable("version") String version) {
		appRegistryService.delete(name, type, version);
	}

	@Deprecated
	@RequestMapping(value = "/{type}/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void unregister(@PathVariable("type") ApplicationType type, @PathVariable("name") String name) {
		if (this.appRegistryService.find(name, type) == null) {
			throw new NoSuchAppRegistrationException(name, type);
		}
		AppRegistration appRegistration = this.appRegistryService.getDefaultApp(name, type);
		if (appRegistration == null) {
			throw new RuntimeException(String.format("No default version exists for the app [%s:%s]", name, type));
		}
		this.unregister(type, name, appRegistration.getVersion());
	}

	/**
	 * Register all applications listed in a properties file or provided as key/value pairs.
	 *
	 * @param pagedResourcesAssembler the resource asembly for app registrations
	 * @param uri URI for the properties file
	 * @param apps key/value pairs representing applications, separated by newlines
	 * @param force if {@code true}, overwrites any pre-existing registrations
	 * @return the collection of registered applications
	 * @throws IOException if can't store the Properties object to byte output stream
	 */
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public PagedResources<? extends AppRegistrationResource> registerAll(
			Pageable pageable,
			PagedResourcesAssembler<AppRegistration> pagedResourcesAssembler,
			@RequestParam(value = "uri", required = false) String uri,
			@RequestParam(value = "apps", required = false) Properties apps,
			@RequestParam(value = "force", defaultValue = "false") boolean force) throws IOException {
		List<AppRegistration> registrations = new ArrayList<>();

		if (StringUtils.hasText(uri)) {
			registrations.addAll(appRegistryService.importAll(force, this.resourceLoader.getResource(uri)));
		}
		else if (!CollectionUtils.isEmpty(apps)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			apps.store(baos, "");
			ByteArrayResource bar = new ByteArrayResource(baos.toByteArray(), "Inline properties");
			registrations.addAll(appRegistryService.importAll(force, bar));
		}

		Collections.sort(registrations);
		prefetchMetadata(registrations);
		return pagedResourcesAssembler.toResource(
				new PageImpl<>(registrations, pageable, appRegistryService.findAll().size()),
				assembler);
	}

	/**
	 * Trigger early resolution of the metadata resource of registrations that have an
	 * explicit metadata artifact. This assumes usage of
	 * {@link org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader}.
	 */
	private void prefetchMetadata(List<AppRegistration> appRegistrations) {
		forkJoinPool.execute(() -> {
			appRegistrations.stream().filter(r -> r.getMetadataUri() != null).parallel().forEach(r -> {
				logger.info("Eagerly fetching {}", r.getMetadataUri());
				try {
					appRegistryService.getAppMetadataResource(r);
				}
				catch (Exception e) {
					logger.warn("Could not fetch " + r.getMetadataUri(), e);
				}
			});
		});
	}

	class Assembler extends ResourceAssemblerSupport<AppRegistration, AppRegistrationResource> {

		public Assembler() {
			super(VersionedAppRegistryController.class, AppRegistrationResource.class);
		}

		@Override
		public AppRegistrationResource toResource(AppRegistration registration) {
			return createResourceWithId(String.format("%s/%s/%s", registration.getType(), registration.getName(),
					registration.getVersion()), registration);
		}

		@Override
		protected AppRegistrationResource instantiateResource(AppRegistration registration) {
			return new AppRegistrationResource(registration.getName(), registration.getType().name(),
					registration.getVersion(), registration.getUri().toString(), registration.isDefaultVersion());
		}
	}
}
