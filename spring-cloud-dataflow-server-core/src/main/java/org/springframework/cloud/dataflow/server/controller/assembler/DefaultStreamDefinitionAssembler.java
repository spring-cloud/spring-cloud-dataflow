/*
 * Copyright 2015-2020 the original author or authors.
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
package org.springframework.cloud.dataflow.server.controller.assembler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.rest.resource.DeploymentStateResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.server.controller.StreamDefinitionController;
import org.springframework.cloud.dataflow.server.controller.support.ControllerUtils;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;

/**
 * {@link org.springframework.hateoas.server.RepresentationModelAssembler} implementation that converts
 * {@link StreamDefinition}s to {@link StreamDefinitionResource}s.
 *
 * @author Ilayaperumal Gopinathan
 */
public class DefaultStreamDefinitionAssembler<R extends StreamDefinitionResource> extends
		RepresentationModelAssemblerSupport<StreamDefinition, R> {

	private static final Logger logger = LoggerFactory.getLogger(DefaultStreamDefinitionAssembler.class);

	protected final StreamDefinitionService streamDefinitionService;

	protected final StreamService streamService;

	private Map<StreamDefinition, DeploymentState> streamDeploymentStates = new HashMap<>();

	public DefaultStreamDefinitionAssembler(StreamDefinitionService streamDefinitionService, StreamService streamService,
			List<StreamDefinition> streamDefinitions, Class<R> classType) {
		super(StreamDefinitionController.class, classType);
		this.streamDefinitionService = streamDefinitionService;
		this.streamService = streamService;
		if (!streamDefinitions.isEmpty()) {
			this.streamDeploymentStates = this.streamService.state(streamDefinitions);
		}
	}

	@Override
	public R toModel(StreamDefinition stream) {
		try {
			return createModelWithId(stream.getName(), stream);
		}
		catch (IllegalStateException e) {
			logger.warn("Failed to create StreamDefinitionResource. " + e.getMessage());
		}
		return null;
	}

	@Override
	public R instantiateModel(StreamDefinition streamDefinition) {
		final StreamDefinitionResource resource = new StreamDefinitionResource(streamDefinition.getName(),
				this.streamDefinitionService.redactDsl(streamDefinition),
				this.streamDefinitionService.redactDsl(
						new StreamDefinition(streamDefinition.getName(), streamDefinition.getOriginalDslText())),
				streamDefinition.getDescription());
		DeploymentState deploymentState = this.streamDeploymentStates.get(streamDefinition);
		if (deploymentState != null) {
			final DeploymentStateResource deploymentStateResource = ControllerUtils
					.mapState(deploymentState);
			resource.setStatus(deploymentStateResource.getKey());
			resource.setStatusDescription(deploymentStateResource.getDescription());
		}
		return (R) resource;
	}
}
