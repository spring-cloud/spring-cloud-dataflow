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
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskValidationService;
import org.springframework.cloud.dataflow.server.service.ValidationStatus;
import org.springframework.cloud.dataflow.server.service.impl.NodeStatus;
import org.springframework.cloud.dataflow.server.service.impl.TaskServiceUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of the TaskValidationService that delegates to the task definition repository and is able to validate
 * composed task definitions.
 *
 * @author Glenn Renfro
 * @author Mark Pollack
 */
public class DefaultTaskValidationService extends DefaultValidationService implements TaskValidationService {

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final String composedTaskRunnerName;

	public DefaultTaskValidationService(AppRegistryService appRegistry,
			DockerValidatorProperties dockerValidatorProperties, TaskDefinitionRepository taskDefinitionRepository,
			String composedTaskRunnerName) {
		super(appRegistry, dockerValidatorProperties);
		Assert.notNull(taskDefinitionRepository, "TaskDefinitionRepository must not be null");
		Assert.isTrue(StringUtils.hasText(composedTaskRunnerName), "ComposedTaskRunnerName must have a value");
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.composedTaskRunnerName = composedTaskRunnerName;
	}

	@Override
	public ValidationStatus validateTask(String name) {
		TaskDefinition definition = this.taskDefinitionRepository.findByTaskName(name);
		ValidationStatus validationStatus = new ValidationStatus(
				definition.getName(),
				definition.getDslText());
		ApplicationType appType = ApplicationType.task;
		if (TaskServiceUtils.isComposedTaskDefinition(definition.getDslText())) {
			// First verify that CTR is valid

			if (this.validate(this.composedTaskRunnerName, ApplicationType.task)) {
				TaskParser taskParser = new TaskParser(name, definition.getDslText(), true, true);
				TaskNode taskNode = taskParser.parse();
				String childTaskPrefix = TaskNode.getTaskPrefix(name);
				taskNode.getTaskApps().stream().forEach(task -> {
					TaskDefinition childDefinition = this.taskDefinitionRepository.findByTaskName(childTaskPrefix + task.getName());
					boolean status = this.validate(childDefinition.getRegisteredAppName(), ApplicationType.task);
					validationStatus.getAppsStatuses().put(
							String.format("%s:%s", appType.name(), childDefinition.getName()),
							(status) ? NodeStatus.valid.name() : NodeStatus.invalid.name());
				});
			}
			else {
				validationStatus.getAppsStatuses().put(
						String.format("%s:%s", appType.name(), this.composedTaskRunnerName),
						NodeStatus.invalid.name());
			}
		}
		else {
			boolean status = this.validate(definition.getRegisteredAppName(), ApplicationType.task);
			validationStatus.getAppsStatuses().put(
					String.format("%s:%s", appType.name(), definition.getName()),
					(status) ? NodeStatus.valid.name() : NodeStatus.invalid.name());
		}
		return validationStatus;
	}
}
