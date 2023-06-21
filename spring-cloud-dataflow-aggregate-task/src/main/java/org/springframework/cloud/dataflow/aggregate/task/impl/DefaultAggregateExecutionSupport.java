/*
 * Copyright 2023-2023 the original author or authors.
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
package org.springframework.cloud.dataflow.aggregate.task.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.task.repository.TaskExecution;

/**
 * Provides support for access to SchemaVersionTarget information and conversion of execution data to composite executions.
 * @author Corneil du Plessis
 */

public class DefaultAggregateExecutionSupport implements AggregateExecutionSupport {
	private static Logger logger = LoggerFactory.getLogger(AggregateExecutionSupport.class);
	private final AppRegistryService registryService;
	private final SchemaService schemaService;

	public DefaultAggregateExecutionSupport(
			AppRegistryService registryService,
			SchemaService schemaService
	) {
		this.registryService = registryService;
		this.schemaService = schemaService;
	}
	@Override public AggregateTaskExecution from(TaskExecution execution, TaskDefinitionReader taskDefinitionReader) {
		SchemaVersionTarget versionTarget = findSchemaVersionTarget(execution.getTaskName(), taskDefinitionReader);
		return from(execution, versionTarget.getName());
	}

	@Override public SchemaVersionTarget findSchemaVersionTarget(String taskName, TaskDefinitionReader taskDefinitionReader) {
		TaskDefinition definition = taskDefinitionReader.findTaskDefinition(taskName);
		String registeredName = definition != null ? definition.getRegisteredAppName() : taskName;
		AppRegistration registration = findTaskAppRegistration(registeredName);
		if(registration == null) {
			logger.warn("Cannot find AppRegistration for {}", taskName);
			return SchemaVersionTarget.defaultTarget();
		}
		final AppRegistration finalRegistration = registration;
		List<SchemaVersionTarget> versionTargets = schemaService.getTargets().getSchemas()
				.stream()
				.filter(target -> target.getSchemaVersion().equals(finalRegistration.getBootVersion()))
				.collect(Collectors.toList());
		if (versionTargets.isEmpty()) {
			logger.warn("Cannot find a SchemaVersionTarget for {}", registration.getBootVersion());
			return SchemaVersionTarget.defaultTarget();
		}
		if (versionTargets.size() > 1) {
			throw new IllegalStateException("Multiple SchemaVersionTargets for " + registration.getBootVersion());
		}
		return versionTargets.get(0);
	}

	@Override public AppRegistration findTaskAppRegistration(String registeredAppName) {
		AppRegistration registration = registryService.find(registeredAppName, ApplicationType.task);
		if(registration == null) {
			registration = registryService.find(registeredAppName, ApplicationType.app);
		}
		return registration;
	}

	@Override public AggregateTaskExecution from(TaskExecution execution, String schemaTarget) {
		if(execution != null) {
			return new AggregateTaskExecution(
					execution.getExecutionId(),
					execution.getExitCode(),
					execution.getTaskName(),
					execution.getStartTime(),
					execution.getEndTime(),
					execution.getExitMessage(),
					execution.getArguments(),
					execution.getErrorMessage(),
					execution.getExternalExecutionId(),
					execution.getParentExecutionId(),
					schemaTarget);
		}
		return null;
	}
}
