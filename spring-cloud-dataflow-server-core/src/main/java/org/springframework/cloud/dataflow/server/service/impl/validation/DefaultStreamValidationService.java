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
package org.springframework.cloud.dataflow.server.service.impl.validation;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamValidationService;
import org.springframework.cloud.dataflow.server.service.ValidationStatus;
import org.springframework.cloud.dataflow.server.service.impl.NodeStatus;
import org.springframework.util.Assert;

/**
 * Implementation of the StreamValidationService that delegates to the stream definition repository.
 * @author Mark Pollack
 */
public class DefaultStreamValidationService extends DefaultValidationService implements StreamValidationService {

	private final StreamDefinitionRepository streamDefinitionRepository;

	private final StreamDefinitionService streamDefinitionService;

	public DefaultStreamValidationService(AppRegistryService appRegistry,
			DockerValidatorProperties dockerValidatorProperties, StreamDefinitionRepository streamDefinitionRepository,
			StreamDefinitionService streamDefinitionService) {
		super(appRegistry, dockerValidatorProperties);
		Assert.notNull(streamDefinitionRepository, "StreamDefinitionRepository must not be null");
		Assert.notNull(streamDefinitionService, "StreamDefinitionService must not be null");
		this.streamDefinitionRepository = streamDefinitionRepository;
		this.streamDefinitionService = streamDefinitionService;
	}

	@Override
	public ValidationStatus validateStream(String name) {
		StreamDefinition streamDefinition = streamDefinitionRepository.findById(name)
				.orElseThrow(() -> new NoSuchStreamDefinitionException(name));
		ValidationStatus validationStatus = new ValidationStatus(
				streamDefinition.getName(),
				streamDefinition.getDslText(),
				streamDefinition.getDescription());
		for (StreamAppDefinition streamAppDefinition : this.streamDefinitionService.getAppDefinitions(streamDefinition)) {
			ApplicationType appType = streamAppDefinition.getApplicationType();
			boolean status = this.validate(streamAppDefinition.getName(), appType);
			validationStatus.getAppsStatuses().put(
					String.format("%s:%s", appType.name(), streamAppDefinition.getName()),
					(status) ? NodeStatus.valid.name() : NodeStatus.invalid.name());
		}
		return validationStatus;
	}
}
