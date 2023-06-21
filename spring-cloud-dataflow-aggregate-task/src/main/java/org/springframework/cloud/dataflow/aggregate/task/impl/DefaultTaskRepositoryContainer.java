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

package org.springframework.cloud.dataflow.aggregate.task.impl;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.aggregate.task.TaskRepositoryContainer;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.util.Assert;

/**
 * This class manages a collection of TaskRepositories for all schemas.
 * In the future there will be a datasource container for all names datasources.
 *
 * @author Corneil du Plessis
 */
public class DefaultTaskRepositoryContainer implements TaskRepositoryContainer {
	private final static Logger logger = LoggerFactory.getLogger(DefaultTaskRepositoryContainer.class);

	private final Map<String, TaskRepository> taskRepositories = new HashMap<>();

	public DefaultTaskRepositoryContainer(DataSource dataSource, SchemaService schemaService) {
		for (SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean = new TaskExecutionDaoFactoryBean(dataSource, target.getTaskPrefix());
			add(target.getName(), new SimpleTaskRepository(taskExecutionDaoFactoryBean));
		}
	}

	private void add(String schemaTarget, TaskRepository taskRepository) {
		taskRepositories.put(schemaTarget, taskRepository);
	}

	@Override
	public TaskRepository get(String schemaTarget) {
		TaskRepository repository = taskRepositories.get(schemaTarget);
		Assert.notNull(repository, "Expected TaskRepository for " + schemaTarget);
		return repository;
	}

	@PostConstruct
	public void setup() {
		logger.info("created: org.springframework.cloud.dataflow.aggregate.task.impl.DefaultTaskRepositoryContainer");
	}
}
