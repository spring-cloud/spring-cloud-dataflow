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

	private final TaskRepository taskRepository;


	public DefaultTaskExecutionRepositoryService(
			TaskRepository taskRepository) {
		Assert.notNull(taskRepository, "taskRepository must not be null");
		this.taskRepository = taskRepository;

	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public TaskExecution createTaskExecution(String taskName, String version) {
		return taskRepository.createTaskExecution(taskName);
	}
}
