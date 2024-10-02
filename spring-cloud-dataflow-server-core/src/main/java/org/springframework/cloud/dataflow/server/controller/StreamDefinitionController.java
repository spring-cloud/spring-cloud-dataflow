/*
 * Copyright 2015-2019 the original author or authors.
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.server.controller.assembler.AppRegistrationAssemblerProvider;
import org.springframework.cloud.dataflow.server.controller.assembler.StreamDefinitionAssemblerProvider;
import org.springframework.cloud.dataflow.server.controller.support.InvalidStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.DuplicateStreamDefinitionException;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link StreamDefinition}. This includes CRUD and optional
 * deployment operations.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * @author Oleg Zhurakousky
 * @author Glenn Renfro
 * @author Christian Tzolov
 * @author Andy Clement
 */
@RestController
@RequestMapping("/streams/definitions")
@ExposesResourceFor(StreamDefinitionResource.class)
public class StreamDefinitionController {

	private static final Logger logger = LoggerFactory.getLogger(StreamDefinitionController.class);

	/**
	 * The service that is responsible for deploying streams.
	 */
	private final StreamService streamService;

	private final StreamDefinitionService streamDefinitionService;

	private final AppRegistryService appRegistryService;

	private final StreamDefinitionAssemblerProvider<? extends StreamDefinitionResource> streamDefinitionAssemblerProvider;

	private final RepresentationModelAssembler<AppRegistration, ? extends AppRegistrationResource> appRegistryAssembler;

	/**
	 * Create a {@code StreamDefinitionController} that delegates to {@link StreamService}.
	 *
	 * @param streamService                     the stream service to use
	 * @param streamDefinitionService           the stream definition service to use
	 * @param appRegistryService                the app registry service to use
	 * @param streamDefinitionAssemblerProvider the stream definition assembler provider to use
	 * @param appRegistrationAssemblerProvider  the app registry assembler provider to use
	 */
	public StreamDefinitionController(StreamService streamService, StreamDefinitionService streamDefinitionService,
									  AppRegistryService appRegistryService,
									  StreamDefinitionAssemblerProvider<? extends StreamDefinitionResource> streamDefinitionAssemblerProvider,
									  AppRegistrationAssemblerProvider<? extends AppRegistrationResource> appRegistrationAssemblerProvider) {
		Assert.notNull(streamService, "StreamService must not be null");
		Assert.notNull(streamDefinitionService, "StreamDefinitionService must not be null");
		Assert.notNull(appRegistryService, "AppRegistryService must not be null");
		Assert.notNull(streamDefinitionAssemblerProvider, "StreamDefinitionAssemblerProvider must not be null");
		Assert.notNull(appRegistrationAssemblerProvider, "AppRegistrationAssemblerProvider must not be null");
		this.streamService = streamService;
		this.streamDefinitionService = streamDefinitionService;
		this.appRegistryService = appRegistryService;
		this.streamDefinitionAssemblerProvider = streamDefinitionAssemblerProvider;
		this.appRegistryAssembler = appRegistrationAssemblerProvider.getAppRegistrationAssembler();
	}

	/**
	 * Return a page-able list of {@link StreamDefinitionResource} defined streams.
	 *
	 * @param pageable  Pagination information
	 * @param assembler assembler for {@link StreamDefinition}
	 * @param search    optional findByTaskNameContains parameter
	 * @return list of stream definitions
	 */
	@GetMapping("")
	@ResponseStatus(HttpStatus.OK)
	public PagedModel<? extends StreamDefinitionResource> list(
			Pageable pageable,
			@RequestParam(required = false) String search,
			PagedResourcesAssembler<StreamDefinition> assembler
	) {
		Page<StreamDefinition> streamDefinitions = this.streamService.findDefinitionByNameContains(pageable, search);
		return assembler.toModel(streamDefinitions,
				this.streamDefinitionAssemblerProvider.getStreamDefinitionAssembler(streamDefinitions.getContent()));
	}

	/**
	 * Create a new stream and optionally deploy it.
	 * <p>
	 * Differs from {@link #saveWithDeployProps} by accepting deployment properties and consuming
	 * {@link MediaType#APPLICATION_FORM_URLENCODED} request content (required by the Dataflow Shell).
	 *
	 * @param name        stream name
	 * @param dsl         DSL definition for stream
	 * @param deploy      if {@code true}, the stream is deployed upon creation (default is
	 *                    {@code false})
	 * @param description description of the stream definition
	 * @return the created stream definition
	 * @throws DuplicateStreamDefinitionException if a stream definition with the same name
	 *                                            already exists
	 * @throws InvalidStreamDefinitionException   if there are errors parsing the stream DSL,
	 *                                            resolving the name, or type of applications in the stream
	 */
	@PostMapping(value = "", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public StreamDefinitionResource save(
			@RequestParam String name,
			@RequestParam("definition") String dsl,
			@RequestParam(defaultValue = "") String description,
			@RequestParam(defaultValue = "false") boolean deploy
	) {
		StreamDefinition streamDefinition = this.streamService.createStream(name, dsl, description, deploy, null);
		return ((RepresentationModelAssembler<StreamDefinition, ? extends StreamDefinitionResource>)
				this.streamDefinitionAssemblerProvider.getStreamDefinitionAssembler(Collections.singletonList(streamDefinition))).toModel(streamDefinition);
	}

	/**
	 * Create a new stream and optionally deploy it.
	 * <p>
	 * Differs from {@link #save} by accepting deployment properties and consuming
	 * {@link MediaType#APPLICATION_JSON} request content.
	 *
	 * @param name                 stream name
	 * @param dsl                  DSL definition for stream
	 * @param deploy               if {@code true}, the stream is deployed upon creation (default is
	 *                             {@code false})
	 * @param deploymentProperties the optional deployment properties to use when the stream is deployed upon creation
	 * @param description          description of the stream definition
	 * @return the created stream definition
	 * @throws DuplicateStreamDefinitionException if a stream definition with the same name
	 *                                            already exists
	 * @throws InvalidStreamDefinitionException   if there are errors parsing the stream DSL,
	 *                                            resolving the name, or type of applications in the stream
	 */
	@PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public StreamDefinitionResource saveWithDeployProps(
			@RequestParam String name,
			@RequestParam("definition") String dsl,
			@RequestParam(defaultValue = "") String description,
			@RequestParam(defaultValue = "false") boolean deploy,
			@RequestBody(required = false) Map<String, String> deploymentProperties
	) {
		StreamDefinition streamDefinition = this.streamService.createStream(name, dsl, description, deploy, deploymentProperties);
		return ((RepresentationModelAssembler<StreamDefinition, ? extends StreamDefinitionResource>)
				this.streamDefinitionAssemblerProvider.getStreamDefinitionAssembler(Collections.singletonList(streamDefinition))).toModel(streamDefinition);
	}

	/**
	 * Request removal of an existing stream definition.
	 *
	 * @param name the name of an existing stream definition (required)
	 */
	@DeleteMapping("/{name}")
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable String name) {
		this.streamService.deleteStream(name);
	}

	/**
	 * Return a list of related stream definition resources based on the given stream name.
	 * Related streams include the main stream and the tap stream(s) on the main stream.
	 *
	 * @param pageable  Pagination information
	 * @param name      the name of an existing stream definition (required)
	 * @param nested    if should recursively findByTaskNameContains for related stream definitions
	 * @param assembler resource assembler for stream definition
	 * @return a list of related stream definitions
	 */
	@GetMapping("/{name}/related")
	@ResponseStatus(HttpStatus.OK)
	public PagedModel<? extends StreamDefinitionResource> listRelated(
			Pageable pageable,
			@PathVariable String name,
			@RequestParam(required = false, defaultValue = "false") boolean nested,
			PagedResourcesAssembler<StreamDefinition> assembler
	) {
		List<StreamDefinition> result = this.streamService.findRelatedStreams(name, nested);
		Page<StreamDefinition> page = new PageImpl<>(result, pageable, result.size());
		return assembler.toModel(page,
				this.streamDefinitionAssemblerProvider.getStreamDefinitionAssembler(page.getContent()));
	}


	/**
	 * Return a given stream definition resource.
	 *
	 * @param name the name of an existing stream definition (required)
	 * @return the stream definition
	 */
	@GetMapping("/{name}")
	@ResponseStatus(HttpStatus.OK)
	public StreamDefinitionResource display(@PathVariable String name) {
		StreamDefinition streamDefinition = this.streamService.findOne(name);
		return this.streamDefinitionAssemblerProvider.getStreamDefinitionAssembler(Collections.singletonList(streamDefinition)).toModel(streamDefinition);
	}


	@GetMapping("/{name}/applications")
	@ResponseStatus(HttpStatus.OK)
	public List<? extends AppRegistrationResource> listApplications(@PathVariable String name) {
		StreamDefinition definition = this.streamService.findOne(name);
		LinkedList<StreamAppDefinition> streamAppDefinitions = this.streamDefinitionService.getAppDefinitions(definition);
		List<AppRegistrationResource> appRegistrations = new ArrayList<>();
		for (StreamAppDefinition streamAppDefinition : streamAppDefinitions) {
			AppRegistrationResource appRegistrationResource = this.appRegistryAssembler.toModel(this.appRegistryService.find(streamAppDefinition.getRegisteredAppName(),
					streamAppDefinition.getApplicationType()));
			appRegistrationResource.setLabel(streamAppDefinition.getName());
			appRegistrations.add(appRegistrationResource);
		}
		return appRegistrations;
	}

	/**
	 * Request removal of all stream definitions.
	 */
	@DeleteMapping("")
	@ResponseStatus(HttpStatus.OK)
	public void deleteAll() {
		this.streamService.deleteAll();
	}
}
