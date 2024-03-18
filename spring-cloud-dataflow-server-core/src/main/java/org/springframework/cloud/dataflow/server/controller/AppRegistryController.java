/*
 * Copyright 2015-2021 the original author or authors.
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.registry.service.DefaultAppRegistryService;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.cloud.dataflow.rest.SkipperStream;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.DetailedAppRegistrationResource;
import org.springframework.cloud.dataflow.server.controller.assembler.AppRegistrationAssemblerProvider;
import org.springframework.cloud.dataflow.server.repository.InvalidApplicationNameException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
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
 * @author Chris Schaefer
 * @author Corneil du Plessis
 */
@RestController
@RequestMapping("/apps")
@ExposesResourceFor(AppRegistrationResource.class)
public class AppRegistryController {

	private static final Logger logger = LoggerFactory.getLogger(AppRegistryController.class);

	private final StreamDefinitionRepository streamDefinitionRepository;

	private final AppRegistryService appRegistryService;

	private final StreamService streamService;

	private ApplicationConfigurationMetadataResolver metadataResolver;

	private ForkJoinPool forkJoinPool;

	private StreamDefinitionService streamDefinitionService;

	private final RepresentationModelAssembler<AppRegistration, ? extends AppRegistrationResource> appRegistryAssembler;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	public AppRegistryController(Optional<StreamDefinitionRepository> streamDefinitionRepository,
			Optional<StreamService> streamService,
			AppRegistryService appRegistryService,
			ApplicationConfigurationMetadataResolver metadataResolver,
			ForkJoinPool forkJoinPool,
			StreamDefinitionService streamDefinitionService,
			AppRegistrationAssemblerProvider<? extends AppRegistrationResource> appRegistrationAssemblerProvider) {
		this.streamDefinitionRepository = streamDefinitionRepository.isPresent() ? streamDefinitionRepository.get() : null;
		this.streamService = streamService.isPresent() ? streamService.get() : null;
		this.appRegistryService = appRegistryService;
		this.metadataResolver = metadataResolver;
		this.forkJoinPool = forkJoinPool;
		this.streamDefinitionService = streamDefinitionService;
		this.appRegistryAssembler = appRegistrationAssemblerProvider.getAppRegistrationAssembler();
	}

	/**
	 * List app registrations. Optional type and findByTaskNameContains parameters can be passed to do
	 * filtering. Search parameter only filters by {@code AppRegistration} name field.
	 *
	 * @param pageable Pagination information
	 * @param pagedResourcesAssembler the resource assembler for app registrations
	 * @param type the application type: source, sink, processor, task
	 * @param version optional application version
	 * @param search optional findByTaskNameContains parameter
	 * @param defaultVersion Indicator to use default version.
	 * @return the list of registered applications
	 */
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public PagedModel<? extends AppRegistrationResource> list(
			Pageable pageable,
			PagedResourcesAssembler<AppRegistration> pagedResourcesAssembler,
			@RequestParam(value = "type", required = false) ApplicationType type,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) String version,
			@RequestParam(required = false) boolean defaultVersion) {

		Page<AppRegistration> pagedRegistrations = this.appRegistryService
				.findAllByTypeAndNameIsLikeAndVersionAndDefaultVersion(type, search, version, defaultVersion, pageable);

		return pagedResourcesAssembler.toModel(pagedRegistrations, this.appRegistryAssembler);
	}

	/**
	 * Retrieve detailed information about a particular application.
	 *
	 * @param type application type
	 * @param name application name
	 * @param version application version
	 * @param exhaustive if set to true all properties are returned
	 * @return detailed application information
	 */
	@RequestMapping(value = "/{type}/{name}/{version:.+}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public DetailedAppRegistrationResource info(@PathVariable("type") ApplicationType type,
			@PathVariable("name") String name, @PathVariable("version") String version,
			@RequestParam(required = false, name = "exhaustive") boolean exhaustive) {

		return getInfo(type, name, version, exhaustive);
	}

	@Deprecated
	@RequestMapping(value = "/{type}/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public DetailedAppRegistrationResource info(
			@PathVariable("type") ApplicationType type, @PathVariable("name") String name,
			@RequestParam(required = false, name = "exhaustive") boolean exhaustive) {
		if (!this.appRegistryService.appExist(name, type)) {
			throw new NoSuchAppRegistrationException(name, type);
		}

		String defaultVersion = this.appRegistryService.getDefaultApp(name, type).getVersion();
		return getInfo(type, name, defaultVersion, exhaustive);
	}

	private DetailedAppRegistrationResource getInfo(ApplicationType type,
			String name, String version, Boolean allProperties) {

		AppRegistration registration = appRegistryService.find(name, type, version);
		if (registration == null) {
			throw new NoSuchAppRegistrationException(name, type, version);
		}
		DetailedAppRegistrationResource result = new DetailedAppRegistrationResource(this.appRegistryAssembler.toModel(registration));
		List<ConfigurationMetadataProperty> properties = this.metadataResolver
				.listProperties(this.appRegistryService.getAppMetadataResource(registration), allProperties);
		for (ConfigurationMetadataProperty property : properties) {
			result.addOption(property);
		}
		Map<String, Set<String>> portsMap = this.metadataResolver.listPortNames(this.appRegistryService.getAppMetadataResource(registration));
		if (portsMap != null && !portsMap.isEmpty()) {
			for (Map.Entry<String, Set<String>> entry: portsMap.entrySet()) {
				if (entry.getKey().equals("inbound")) {
					for (String portName: entry.getValue()) {
						result.addInboundPortName(portName);
					}
				}
				else if (entry.getKey().equals("outbound")) {
					for (String portName: entry.getValue()) {
						result.addOutboundPortName(portName);
					}
				}
			}
		}
		Map<String, Set<String>> groupingsMap = this.metadataResolver
				.listOptionGroups(this.appRegistryService.getAppMetadataResource(registration));
		result.getOptionGroups().putAll(groupingsMap);
		return result;
	}

	/**
	 * Register a module name and type with its URI.
	 *
	 * @param type module type
	 * @param name module name
	 * @param version module version
	 * @param bootVersion module boot version or {@code null} to use the default
	 * @param uri URI for the module artifact (e.g. {@literal maven://group:artifact:version})
	 * @param metadataUri URI for the metadata artifact
	 * @param force if {@code true}, overwrites a pre-existing registration
	 */
	@RequestMapping(value = "/{type}/{name}/{version:.+}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void register(
			@PathVariable("type") ApplicationType type,
			@PathVariable("name") String name,
			@PathVariable("version") String version,
			@RequestParam(name = "bootVersion", required = false) String bootVersion,
			@RequestParam("uri") String uri,
			@RequestParam(name = "metadata-uri", required = false) String metadataUri,
			@RequestParam(value = "force", defaultValue = "false") boolean force) {
		validateApplicationName(name);
		appRegistryService.validate(appRegistryService.getDefaultApp(name, type), uri, version);
		AppRegistration previous = appRegistryService.find(name, type, version);
		if (!force && previous != null) {
			throw new AppAlreadyRegisteredException(previous);
		}
		try {
			AppRegistration registration = this.appRegistryService.save(
					name,
					type,
					version,
					new URI(uri),
					metadataUri != null ? new URI(metadataUri) : null
			);
			prefetchMetadata(Collections.singletonList(registration));
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Deprecated
	@RequestMapping(value = "/{type}/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void register(
			@PathVariable("type") ApplicationType type,
			@PathVariable("name") String name,
			@RequestParam(name = "bootVersion", required = false) String bootVersion,
			@RequestParam("uri") String uri,
			@RequestParam(name = "metadata-uri", required = false) String metadataUri,
			@RequestParam(value = "force", defaultValue = "false") boolean force) {
		String version = this.appRegistryService.getResourceVersion(uri);
		this.register(
				type,
				name,
				version,
				bootVersion,
				uri,
				metadataUri,
				force
		);
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

		if (type != ApplicationType.task) {
			String streamWithApp = findStreamContainingAppOf(type, name, version);
			if (streamWithApp != null) {
				throw new UnregisterAppException(String.format("The app [%s:%s:%s] you're trying to unregister is " +
						"currently used in stream '%s'.", name, type, version, streamWithApp));
			}
		}

		if (!this.appRegistryService.appExist(name, type, version)) {
			throw new NoSuchAppRegistrationException(name, type, version);
		}

		appRegistryService.delete(name, type, version);
	}

	/**
	 * Given the application type, name, and version, determine if it is being used in a deployed stream definition.
	 *
	 * @param appType the application type
	 * @param appName the application name
	 * @param appVersion application version
	 * @return the name of the deployed stream where the app is being used.  If the app is not deployed in a stream,
	 * return {@code null}.
	 */
	private String findStreamContainingAppOf(ApplicationType appType, String appName, String appVersion) {
		if (this.streamDefinitionRepository == null || this.streamService == null) {
			return null;
		}
		Iterable<StreamDefinition> streamDefinitions = streamDefinitionRepository.findAll();
		for (StreamDefinition streamDefinition : streamDefinitions) {
			StreamDeployment streamDeployment = this.streamService.info(streamDefinition.getName());
			for (StreamAppDefinition streamAppDefinition : this.streamDefinitionService.getAppDefinitions(streamDefinition)) {
				final String streamAppName = streamAppDefinition.getRegisteredAppName();
				final ApplicationType streamAppType = streamAppDefinition.getApplicationType();
				if (appType != streamAppType) {
					continue;
				}
				Map<String, Map<String, String>> streamDeploymentPropertiesMap;
				String streamDeploymentPropertiesString = streamDeployment.getDeploymentProperties();
				if (!StringUtils.hasText(streamDeploymentPropertiesString)) {
					continue;
				}
				ObjectMapper objectMapper = new ObjectMapper();
				try {
					streamDeploymentPropertiesMap = objectMapper.readValue(streamDeploymentPropertiesString,
							new TypeReference<Map<String, Map<String, String>>>() {
							});
				}
				catch (IOException e) {
					throw new RuntimeException("Can not deserialize Stream Deployment Properties JSON '"
							+ streamDeploymentPropertiesString + "'");
				}
				if (streamDeploymentPropertiesMap.containsKey(appName)) {
					Map<String, String> appDeploymentProperties = streamDeploymentPropertiesMap.get(streamAppName);
					if (appDeploymentProperties.containsKey(SkipperStream.SKIPPER_SPEC_VERSION)) {
						String version = appDeploymentProperties.get(SkipperStream.SKIPPER_SPEC_VERSION);
						if (version != null && version.equals(appVersion)) {
							return streamDefinition.getName();
						}
					}
				}
			}
		}
		return null;
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

	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void unregisterAll() {
		List<AppRegistration> appRegistrations = appRegistryService.findAll();
		List<AppRegistration> appRegistrationsToUnregister = new ArrayList<>();

		for (AppRegistration appRegistration : appRegistrations) {
			String applicationName = appRegistration.getName();
			String applicationVersion = appRegistration.getVersion();
			ApplicationType applicationType = appRegistration.getType();

			if (applicationType != ApplicationType.task) {
				String streamWithApp = findStreamContainingAppOf(applicationType, applicationName, applicationVersion);

				if (streamWithApp == null) {
					appRegistrationsToUnregister.add(appRegistration);
				}
			} else {
				appRegistrationsToUnregister.add(appRegistration);
			}
		}

		if (!appRegistrationsToUnregister.isEmpty()) {
			appRegistryService.deleteAll(appRegistrationsToUnregister);
		}
	}

	/**
	 * Register all applications listed in a properties file or provided as key/value pairs.
	 *
	 * @param pageable Pagination information
	 * @param pagedResourcesAssembler the resource assembly for app registrations
	 * @param uri URI for the properties file
	 * @param apps key/value pairs representing applications, separated by newlines
	 * @param force if {@code true}, overwrites any pre-existing registrations
	 * @return the collection of registered applications
	 */
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public PagedModel<? extends AppRegistrationResource> registerAll(
			Pageable pageable,
			PagedResourcesAssembler<AppRegistration> pagedResourcesAssembler,
			@RequestParam(value = "uri", required = false) String uri,
			@RequestParam(value = "apps", required = false) String apps,
			@RequestParam(value = "force", defaultValue = "false") boolean force) {
		List<AppRegistration> registrations = new ArrayList<>();

		if (StringUtils.hasText(uri)) {
			registrations.addAll(this.appRegistryService.importAll(force, this.resourceLoader.getResource(uri)));
		}
		else if (StringUtils.hasLength(apps)) {
			ByteArrayResource bar = new ByteArrayResource(apps.getBytes());
			registrations.addAll(this.appRegistryService.importAll(force, bar));
		}

		Collections.sort(registrations);
		prefetchMetadata(registrations);
		return pagedResourcesAssembler.toModel(new PageImpl<>(registrations, pageable, registrations.size()),
				this.appRegistryAssembler);
	}

	/**
	 * Trigger early resolution of the metadata resource of registrations that have an
	 * explicit metadata artifact. This assumes usage of
	 * {@link org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader}.
	 */
	private void prefetchMetadata(List<AppRegistration> appRegistrations) {
		forkJoinPool.execute(
				() -> appRegistrations.stream().filter(r -> r.getMetadataUri() != null).parallel().forEach(r -> {
					logger.info("Eagerly fetching {}", r.getMetadataUri());
					try {
						this.appRegistryService.getAppMetadataResource(r);
					}
					catch (Exception e) {
						logger.warn("Could not fetch {}", r.getMetadataUri(), e);
					}
				}));
	}

	private void validateApplicationName(String name) {

		// Check for length of name to be less than 256 character.
		if (name.length() > 255) {
			throw new InvalidApplicationNameException("Length of application name must be less than 256 characters");
		}

		// Check for invalid characters.
		char[] invalidWildCards = new char[]{':'};
		StringBuilder invalidChars = new StringBuilder();
		for (char invalidWildCard : invalidWildCards) {
			if (name.contains(Character.toString(invalidWildCard))) {
				invalidChars.append("'").append(invalidWildCard).append("'");
			}
		}

		if (!invalidChars.toString().equals("")) {
			throw new InvalidApplicationNameException("Application name: '" + name + "' cannot contain: " + invalidChars);
		}
	}
}
