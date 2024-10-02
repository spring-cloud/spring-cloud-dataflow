/*
 * Copyright 2018 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.dataflow.rest.resource.DeploymentStateResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.cloud.dataflow.server.controller.support.ControllerUtils;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
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
 * Controller for deployment operations on {@link StreamDefinition}s. Support for stream
 * update, rollback, and update history by delegating to {@link StreamService}.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 * @author Janne Valkealahti
 * @author Christian Tzolov
 * @author Gunnar Hillert
 * @author Christian Tzolov
 */
@RestController
@RequestMapping("/streams/deployments")
@ExposesResourceFor(StreamDeploymentResource.class)
public class StreamDeploymentController {

	private static final Logger logger = LoggerFactory.getLogger(StreamDeploymentController.class);

	private final StreamService streamService;

	private final StreamDefinitionService streamDefinitionService;

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final StreamDefinitionRepository repository;

	private final ArgumentSanitizer sanitizer = new ArgumentSanitizer();

	/**
	 * Construct a new UpdatableStreamDeploymentController, given a
	 * {@link StreamDeploymentController} and {@link StreamService} and {@link StreamDefinitionService}
	 *
	 * @param repository              the repository this controller will use for stream CRUD operations
	 * @param streamService           the underlying UpdatableStreamService to deploy the stream
	 * @param streamDefinitionService the StreamDefinitionService
	 */
	public StreamDeploymentController(StreamDefinitionRepository repository,
									  StreamService streamService,
									  StreamDefinitionService streamDefinitionService) {

		Assert.notNull(repository, "StreamDefinitionRepository must not be null");
		Assert.notNull(streamService, "StreamService must not be null");
		Assert.notNull(streamDefinitionService, "StreamDefinitionService must not be null");

		this.repository = repository;
		this.streamService = streamService;
		this.streamDefinitionService = streamDefinitionService;
	}

	/**
	 * Scale application instances in a deployed stream.
	 *
	 * @param streamName the name of an existing stream definition (required)
	 * @param appName    in stream application name to scale (required)
	 * @param count      number of instances for the selected stream application (required)
	 * @param properties scale deployment specific properties (optional)
	 * @return response without a body
	 */
	@PostMapping("/scale/{streamName}/{appName}/instances/{count}")
	public ResponseEntity<Void> scaleApplicationInstances(
			@PathVariable String streamName,
			@PathVariable String appName,
			@PathVariable Integer count,
			@RequestBody(required = false) Map<String, String> properties) {

		logger.info("Scale stream: {}, apps: {} instances to {}", streamName, appName, count);
		this.streamService.scaleApplicationInstances(streamName, appName, count, properties);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/update/{name}")
	public ResponseEntity<Void> update(@PathVariable String name,
									   @RequestBody UpdateStreamRequest updateStreamRequest) {
		this.streamService.updateStream(name, updateStreamRequest);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/rollback/{name}/{version}")
	public ResponseEntity<Void> rollback(@PathVariable String name, @PathVariable Integer version) {
		this.streamService.rollbackStream(name, version);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@GetMapping("/manifest/{name}/{version}")
	public ResponseEntity<String> manifest(@PathVariable String name,
										   @PathVariable Integer version) {
		return new ResponseEntity<>(this.streamService.manifest(name, version), HttpStatus.OK);
	}

	@GetMapping("/history/{name}")
	@ResponseStatus(HttpStatus.OK)
	public Collection<Release> history(@PathVariable("name") String releaseName) {
		return this.streamService.history(releaseName)
				.stream()
				.map(this::sanitizeRelease)
				.collect(Collectors.toList());
	}

	private Release sanitizeRelease(Release release) {
		if (release.getConfigValues() != null && StringUtils.hasText(release.getConfigValues().getRaw())) {
			release.getConfigValues().setRaw(sanitizer.sanitizeJsonOrYamlString(release.getConfigValues().getRaw()));
		}
		return release;
	}

	@GetMapping("/platform/list")
	@ResponseStatus(HttpStatus.OK)
	public Collection<Deployer> platformList() {
		return this.streamService.platformList();
	}

	/**
	 * Request un-deployment of an existing stream.
	 *
	 * @param name the name of an existing stream (required)
	 * @return response without a body
	 */
	@DeleteMapping("/{name}")
	public ResponseEntity<Void> undeploy(@PathVariable String name) {
		this.repository.findById(name)
				.orElseThrow(() -> new NoSuchStreamDefinitionException(name));
		this.streamService.undeployStream(name);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * Request un-deployment of all streams.
	 *
	 * @return instance of {@link ResponseEntity}
	 */
	@DeleteMapping("")
	public ResponseEntity<Void> undeployAll() {
		for (StreamDefinition stream : this.repository.findAll()) {
			this.streamService.undeployStream(stream.getName());
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * Request deployment of an existing stream definition.
	 *
	 * @param name                      the name of an existing stream definition (required)
	 * @param reuseDeploymentProperties Indicator to re-use deployment properties.
	 * @return The stream deployment
	 */
	@GetMapping("/{name}")
	@ResponseStatus(HttpStatus.OK)
	public StreamDeploymentResource info(
			@PathVariable String name,
			@RequestParam(value = "reuse-deployment-properties", required = false) boolean reuseDeploymentProperties
	) {
		StreamDefinition streamDefinition = this.repository.findById(name)
				.orElseThrow(() -> new NoSuchStreamDefinitionException(name));
		StreamDeployment streamDeployment = this.streamService.info(name);
		Map<StreamDefinition, DeploymentState> streamDeploymentStates = this.streamService
				.state(Arrays.asList(streamDefinition));
		DeploymentState deploymentState = streamDeploymentStates.get(streamDefinition);
		String status = "";
		if (deploymentState != null) {
			final DeploymentStateResource deploymentStateResource = ControllerUtils.mapState(deploymentState);
			status = deploymentStateResource.getKey();
		}
		return new Assembler(streamDefinition.getDslText(), streamDefinition.getDescription(), status, reuseDeploymentProperties)
				.toModel(streamDeployment);
	}

	/**
	 * Request deployment of an existing stream definition.
	 *
	 * @param name       the name of an existing stream definition (required)
	 * @param properties the deployment properties for the stream as a comma-delimited list of
	 *                   key=value pairs
	 * @return response without a body
	 */
	@PostMapping("/{name}")
	public ResponseEntity<Void> deploy(@PathVariable String name,
									   @RequestBody(required = false) Map<String, String> properties) {
		this.streamService.deployStream(name, properties);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	/**
	 * {@link org.springframework.hateoas.server.RepresentationModelAssembler} implementation that
	 * converts {@link StreamDeployment}s to {@link StreamDeploymentResource}s.
	 */
	class Assembler extends RepresentationModelAssemblerSupport<StreamDeployment, StreamDeploymentResource> {

		private final String dslText;

		private final String status;

		private final String description;

		private boolean reuseDeploymentProperties;

		public Assembler(String dslText, String description, String status, boolean reuseDeploymentProperties) {
			super(StreamDeploymentController.class, StreamDeploymentResource.class);
			this.dslText = dslText;
			this.description = description;
			this.status = status;
			this.reuseDeploymentProperties = reuseDeploymentProperties;
		}

		@Override
		public StreamDeploymentResource toModel(StreamDeployment streamDeployment) {
			try {
				return createModelWithId(streamDeployment.getStreamName(), streamDeployment);
			} catch (IllegalStateException e) {
				logger.warn("Failed to create StreamDeploymentResource. " + e.getMessage());
			}
			return null;
		}

		@Override
		public StreamDeploymentResource instantiateModel(StreamDeployment streamDeployment) {
			String deploymentProperties = "";
			if (this.reuseDeploymentProperties ||
					(StringUtils.hasText(streamDeployment.getDeploymentProperties()) && canDisplayDeploymentProperties())) {
				deploymentProperties = streamDeployment.getDeploymentProperties();
			}
			return new StreamDeploymentResource(streamDeployment.getStreamName(),
					streamDefinitionService.redactDsl(new StreamDefinition(streamDeployment.getStreamName(), this.dslText)),
					this.description,
					deploymentProperties, this.status);
		}

		private boolean canDisplayDeploymentProperties() {
			return StringUtils.hasText(this.status) &&
					(this.status.equals(DeploymentState.deployed.name()))
					|| this.status.equals(DeploymentState.deploying.name());
		}

	}
}
