/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl;

import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.aggregate.task.TaskRepositoryContainer;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.server.service.TaskExecutionCreationService;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Default implementation of TaskExecutionCreationService that creates a {@link TaskExecution}
 * with a transaction propagation setting of {@link Propagation#REQUIRES_NEW}
 */
@Transactional
public class DefaultTaskExecutionRepositoryService implements TaskExecutionCreationService {

	private final TaskRepositoryContainer taskRepositoryContainer;
	private final AggregateExecutionSupport aggregateExecutionSupport;

	private final TaskDefinitionReader taskDefinitionReader;

	public DefaultTaskExecutionRepositoryService(
			TaskRepositoryContainer taskRepositoryContainer,
			AggregateExecutionSupport aggregateExecutionSupport,
			TaskDefinitionReader taskDefinitionReader
	) {
		Assert.notNull(taskRepositoryContainer, "taskRepository must not be null");
		Assert.notNull(aggregateExecutionSupport, "aggregateExecutionSupport must not be null");
		Assert.notNull(taskDefinitionReader, "taskDefinitionReader must not be null");
		this.taskRepositoryContainer = taskRepositoryContainer;
		this.aggregateExecutionSupport = aggregateExecutionSupport;
		this.taskDefinitionReader = taskDefinitionReader;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public TaskExecution createTaskExecution(String taskName, String version) {
		SchemaVersionTarget schemaVersionTarget = this.aggregateExecutionSupport.findSchemaVersionTarget(taskName, version, taskDefinitionReader);
		TaskRepository taskRepository = this.taskRepositoryContainer.get(schemaVersionTarget.getName());
		return taskRepository.createTaskExecution(taskName);
	}
}
