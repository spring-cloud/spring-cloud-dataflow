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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.rest.resource.DeploymentStateResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.cloud.dataflow.server.controller.support.ControllerUtils;
import org.springframework.cloud.dataflow.server.controller.support.InvalidStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.DuplicateStreamDefinitionException;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

	/**
	 * Create a {@code StreamDefinitionController} that delegates to {@link StreamService}.
	 *
	 * @param streamService the stream service to use
	 */
	public StreamDefinitionController(StreamService streamService) {
		Assert.notNull(streamService, "StreamService must not be null");
		this.streamService = streamService;
	}

	/**
	 * Return a page-able list of {@link StreamDefinitionResource} defined streams.
	 *
	 * @param pageable Pagination information
	 * @param assembler assembler for {@link StreamDefinition}
	 * @param search optional findByTaskNameContains parameter
	 * @return list of stream definitions
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StreamDefinitionResource> list(Pageable pageable,
			@RequestParam(required = false) String search, PagedResourcesAssembler<StreamDefinition> assembler) {
		Page<StreamDefinition> streamDefinitions = this.streamService.findDefinitionByNameContains(pageable, search);
		return assembler.toResource(streamDefinitions, new Assembler(streamDefinitions));
	}

	/**
	 * Create a new stream.
	 *
	 * @param name stream name
	 * @param dsl DSL definition for stream
	 * @param deploy if {@code true}, the stream is deployed upon creation (default is
	 * {@code false})
	 * @return the created stream definition
	 * @throws DuplicateStreamDefinitionException if a stream definition with the same name
	 * already exists
	 * @throws InvalidStreamDefinitionException if there errors in parsing the stream DSL,
	 * resolving the name, or type of applications in the stream
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public StreamDefinitionResource save(@RequestParam("name") String name, @RequestParam("definition") String dsl,
			@RequestParam(value = "deploy", defaultValue = "false") boolean deploy) {

		StreamDefinition streamDefinition = this.streamService.createStream(name, dsl, deploy);
		return new Assembler(new PageImpl<>(Collections.singletonList(streamDefinition))).toResource(streamDefinition);
	}

	/**
	 * Request removal of an existing stream definition.
	 *
	 * @param name the name of an existing stream definition (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name") String name) {
		this.streamService.deleteStream(name);
	}

	/**
	 * Return a list of related stream definition resources based on the given stream name.
	 * Related streams include the main stream and the tap stream(s) on the main stream.
	 *
	 * @param pageable Pagination information
	 * @param name the name of an existing stream definition (required)
	 * @param nested if should recursively findByTaskNameContains for related stream definitions
	 * @param assembler resource assembler for stream definition
	 * @return a list of related stream definitions
	 */
	@RequestMapping(value = "/{name}/related", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StreamDefinitionResource> listRelated(Pageable pageable,
			@PathVariable("name") String name,
			@RequestParam(value = "nested", required = false, defaultValue = "false") boolean nested,
			PagedResourcesAssembler<StreamDefinition> assembler) {
		List<StreamDefinition> result = this.streamService.findRelatedStreams(name, nested);
		Page<StreamDefinition> page = new PageImpl<>(result, pageable, result.size());
		return assembler.toResource(page, new Assembler(page));
	}


	/**
	 * Return a given stream definition resource.
	 *
	 * @param name the name of an existing stream definition (required)
	 * @return the stream definition
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public StreamDefinitionResource display(@PathVariable("name") String name) {
		StreamDefinition definition = this.streamService.findOne(name);
		return new Assembler(new PageImpl<>(Collections.singletonList(definition))).toResource(definition);
	}

	/**
	 * Request removal of all stream definitions.
	 */
	@RequestMapping(value = "", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void deleteAll() {
		this.streamService.deleteAll();
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation that converts
	 * {@link StreamDefinition}s to {@link StreamDefinitionResource}s.
	 */
	class Assembler extends ResourceAssemblerSupport<StreamDefinition, StreamDefinitionResource> {

		private final Map<StreamDefinition, DeploymentState> streamDeploymentStates;

		public Assembler(Page<StreamDefinition> streamDefinitions) {
			super(StreamDefinitionController.class, StreamDefinitionResource.class);
			streamDeploymentStates = StreamDefinitionController.this.streamService
					.state(streamDefinitions.getContent());

		}

		@Override
		public StreamDefinitionResource toResource(StreamDefinition stream) {
			try {
				return createResourceWithId(stream.getName(), stream);
			}
			catch (IllegalStateException e) {
				logger.warn("Failed to create StreamDefinitionResource. " + e.getMessage());
			}
			return null;
		}

		@Override
		public StreamDefinitionResource instantiateResource(StreamDefinition stream) {
			final StreamDefinitionResource resource = new StreamDefinitionResource(stream.getName(),
					new ArgumentSanitizer().sanitizeStream(stream));
			DeploymentState deploymentState = streamDeploymentStates.get(stream);
			if (deploymentState != null) {
				final DeploymentStateResource deploymentStateResource = ControllerUtils
						.mapState(deploymentState);
				resource.setStatus(deploymentStateResource.getKey());
				resource.setStatusDescription(deploymentStateResource.getDescription());
			}
			return resource;
		}

	}
}
