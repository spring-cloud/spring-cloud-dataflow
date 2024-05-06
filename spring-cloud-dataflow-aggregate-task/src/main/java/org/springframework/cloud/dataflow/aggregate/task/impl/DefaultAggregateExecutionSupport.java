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
import org.springframework.cloud.dataflow.aggregate.task.TaskDeploymentReader;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDeployment;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.util.StringUtils;

/**
 * Provides support for access to SchemaVersionTarget information and conversion of execution data to composite executions.
 *
 * @author Corneil du Plessis
 */

public class DefaultAggregateExecutionSupport implements AggregateExecutionSupport {
	private static final Logger logger = LoggerFactory.getLogger(AggregateExecutionSupport.class);

	private final AppRegistryService registryService;

	private final SchemaService schemaService;

	public DefaultAggregateExecutionSupport(
			AppRegistryService registryService,
			SchemaService schemaService
	) {
		this.registryService = registryService;
		this.schemaService = schemaService;
	}

	@Override
	public AggregateTaskExecution from(TaskExecution execution, TaskDefinitionReader taskDefinitionReader, TaskDeploymentReader taskDeploymentReader) {
		TaskDefinition taskDefinition = taskDefinitionReader.findTaskDefinition(execution.getTaskName());
		TaskDeployment deployment = null;
		if (StringUtils.hasText(execution.getExternalExecutionId())) {
			deployment = taskDeploymentReader.getDeployment(execution.getExternalExecutionId());
		} else {
			if(taskDefinition == null) {
				logger.warn("TaskDefinition not found for " + execution.getTaskName());
			} else {
				deployment = taskDeploymentReader.findByDefinitionName(taskDefinition.getName());
			}
		}
		SchemaVersionTarget versionTarget = findSchemaVersionTarget(execution.getTaskName(), taskDefinition);
		return from(execution, versionTarget.getName(), deployment != null ? deployment.getPlatformName() : null);
	}

	@Override
	public SchemaVersionTarget findSchemaVersionTarget(String taskName, TaskDefinitionReader taskDefinitionReader) {
		logger.debug("findSchemaVersionTarget:{}", taskName);
		TaskDefinition definition = taskDefinitionReader.findTaskDefinition(taskName);
		return findSchemaVersionTarget(taskName, definition);
	}

	@Override
	public SchemaVersionTarget findSchemaVersionTarget(String taskName, String version, TaskDefinitionReader taskDefinitionReader) {
		logger.debug("findSchemaVersionTarget:{}:{}", taskName, version);
		TaskDefinition definition = taskDefinitionReader.findTaskDefinition(taskName);
		return findSchemaVersionTarget(taskName, version, definition);
	}

	@Override
	public SchemaVersionTarget findSchemaVersionTarget(String taskName, TaskDefinition taskDefinition) {
		return findSchemaVersionTarget(taskName, null, taskDefinition);
	}

	@Override
	public SchemaVersionTarget findSchemaVersionTarget(String taskName, String version, TaskDefinition taskDefinition) {
		logger.debug("findSchemaVersionTarget:{}:{}", taskName, version);
		String registeredName = taskDefinition != null ? taskDefinition.getRegisteredAppName() : taskName;
		AppRegistration registration = findTaskAppRegistration(registeredName, version);
		if (registration == null) {
			if(StringUtils.hasLength(version)) {
				logger.warn("Cannot find AppRegistration for {}:{}", taskName, version);
			} else {
				logger.warn("Cannot find AppRegistration for {}", taskName);
			}
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
		SchemaVersionTarget schemaVersionTarget = versionTargets.get(0);
		logger.debug("findSchemaVersionTarget:{}:{}:{}={}", taskName, registeredName, version, schemaVersionTarget);
		return schemaVersionTarget;
	}

	@Override
	public AppRegistration findTaskAppRegistration(String registeredName) {
		return findTaskAppRegistration(registeredName, null);
	}

	@Override
	public AppRegistration findTaskAppRegistration(String registeredAppName, String version) {
		AppRegistration registration = StringUtils.hasLength(version) ?
			registryService.find(registeredAppName, ApplicationType.task, version) :
			registryService.find(registeredAppName, ApplicationType.task);
		if (registration == null) {
			registration = StringUtils.hasLength(version) ?
				registryService.find(registeredAppName, ApplicationType.app, version) :
				registryService.find(registeredAppName, ApplicationType.app);
		}
		logger.debug("findTaskAppRegistration:{}:{}={}", registeredAppName, version, registration);
		return registration;
	}

	@Override
	public AggregateTaskExecution from(TaskExecution execution, String schemaTarget, String platformName) {
		if (execution != null) {
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
					platformName,
					null,
					schemaTarget);
		}
		return null;
	}
}
