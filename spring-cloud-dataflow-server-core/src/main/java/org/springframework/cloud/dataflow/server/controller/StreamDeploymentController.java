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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.rest.resource.DeploymentStateResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.rest.support.ArgumentSanitizer;
import org.springframework.cloud.dataflow.server.controller.support.ControllerUtils;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for deployment operations on {@link StreamDefinition}.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 * @author Janne Valkealahti
 * @author Christian Tzolov
 * @author Gunnar Hillert
 */
@RestController
@RequestMapping("/streams/deployments")
@ExposesResourceFor(StreamDeploymentResource.class)
public class StreamDeploymentController {

	private static final Logger logger = LoggerFactory.getLogger(StreamDeploymentController.class);

	private final StreamService streamService;

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final StreamDefinitionRepository repository;

	/**
	 * Create a {@code StreamDeploymentController} that delegates
	 * <ul>
	 * <li>CRUD operations to the provided {@link StreamDefinitionRepository}</li>
	 * <li>deployment operations to the provided {@link AppDeployer} via
	 * {@link StreamService}</li>
	 * </ul>
	 *
	 * @param repository the repository this controller will use for stream CRUD operations
	 * @param streamService the underlying StreamService to deploy the stream
	 */
	public StreamDeploymentController(StreamDefinitionRepository repository, StreamService streamService) {
		Assert.notNull(repository, "StreamDefinitionRepository must not be null");
		Assert.notNull(streamService, "StreamService must not be null");
		this.repository = repository;
		this.streamService = streamService;
	}

	/**
	 * Request un-deployment of an existing stream.
	 *
	 * @param name the name of an existing stream (required)
	 * @return response without a body
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> undeploy(@PathVariable("name") String name) {
		StreamDefinition stream = this.repository.findOne(name);
		if (stream == null) {
			throw new NoSuchStreamDefinitionException(name);
		}
		this.streamService.undeployStream(name);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * Request un-deployment of all streams.
	 */
	@RequestMapping(value = "", method = RequestMethod.DELETE)
	public ResponseEntity<Void> undeployAll() {
		for (StreamDefinition stream : this.repository.findAll()) {
			this.streamService.undeployStream(stream.getName());
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * Request deployment of an existing stream definition.
	 * @param name the name of an existing stream definition (required)
	 * @return The stream deployment
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.CREATED)
	public StreamDeploymentResource info(@PathVariable("name") String name) {
		StreamDefinition streamDefinition = this.repository.findOne(name);
		if (streamDefinition == null) {
			throw new NoSuchStreamDefinitionException(name);
		}
		StreamDeployment streamDeployment = this.streamService.info(name);
		Map<StreamDefinition, DeploymentState> streamDeploymentStates =
				this.streamService.state(Arrays.asList(streamDefinition));
		DeploymentState deploymentState = streamDeploymentStates.get(streamDefinition);
		String status = "";
		if (deploymentState != null) {
			final DeploymentStateResource deploymentStateResource = ControllerUtils.mapState(deploymentState);
			status = deploymentStateResource.getKey();
		}
		return new Assembler(streamDefinition.getDslText(), status).toResource(streamDeployment);
	}

	/**
	 * Request deployment of an existing stream definition.
	 * @param name the name of an existing stream definition (required)
	 * @param properties the deployment properties for the stream as a comma-delimited list of
	 * key=value pairs
	 * @return response without a body
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.POST)
	public ResponseEntity<Void> deploy(@PathVariable("name") String name,
			@RequestBody(required = false) Map<String, String> properties) {
		this.streamService.deployStream(name, properties);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation that converts
	 * {@link StreamDeployment}s to {@link StreamDeploymentResource}s.
	 */
	class Assembler extends ResourceAssemblerSupport<StreamDeployment, StreamDeploymentResource> {

		private final String dslText;

		private final String status;

		public Assembler(String dslText, String status) {
			super(StreamDeploymentController.class, StreamDeploymentResource.class);
			this.dslText = dslText;
			this.status = status;
		}

		@Override
		public StreamDeploymentResource toResource(StreamDeployment streamDeployment) {
			try {
				return createResourceWithId(streamDeployment.getStreamName(), streamDeployment);
			}
			catch (IllegalStateException e) {
				logger.warn("Failed to create StreamDeploymentResource. " + e.getMessage());
			}
			return null;
		}

		@Override
		public StreamDeploymentResource instantiateResource(StreamDeployment streamDeployment) {
			String deploymentProperties = "";
			if (StringUtils.hasText(streamDeployment.getDeploymentProperties()) && canDisplayDeploymentProperties()) {
				deploymentProperties = streamDeployment.getDeploymentProperties();
			}
			return new StreamDeploymentResource(streamDeployment.getStreamName(),
					new ArgumentSanitizer().sanitizeStream(new StreamDefinition(streamDeployment.getStreamName(), this.dslText)), deploymentProperties, this.status);
		}

		private boolean canDisplayDeploymentProperties() {
			return StringUtils.hasText(this.status) &&
					(this.status.equals(DeploymentState.deployed.name()))
					|| this.status.equals(DeploymentState.deploying.name());
		}

	}
}
