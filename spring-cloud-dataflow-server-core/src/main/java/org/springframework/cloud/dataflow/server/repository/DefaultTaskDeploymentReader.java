/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.cloud.dataflow.server.repository;

import org.springframework.cloud.dataflow.server.task.TaskDeploymentReader;
import org.springframework.cloud.dataflow.core.TaskDeployment;

/**
 * Provide a simple interface for reading Task deployments when required by Aggregate Task Explorer
 * @author Corneil du Plessis
 */
public class DefaultTaskDeploymentReader implements TaskDeploymentReader {
	private final TaskDeploymentRepository taskDeploymentRepository;

	public DefaultTaskDeploymentReader(TaskDeploymentRepository taskDeploymentRepository) {
		this.taskDeploymentRepository = taskDeploymentRepository;
	}

	@Override
	public TaskDeployment getDeployment(String externalTaskId) {
		return taskDeploymentRepository.findByTaskDeploymentId(externalTaskId);
	}

	@Override
	public TaskDeployment getDeployment(String externalTaskId, String platform) {
		return taskDeploymentRepository.findByTaskDeploymentIdAndPlatformName(externalTaskId, platform);
	}

	@Override
	public TaskDeployment findByDefinitionName(String definitionName) {
		return taskDeploymentRepository.findTopByTaskDefinitionNameOrderByCreatedOnAsc(definitionName);
	}
}
