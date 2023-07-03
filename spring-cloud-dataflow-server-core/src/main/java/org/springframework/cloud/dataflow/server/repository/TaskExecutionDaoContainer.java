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

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.core.database.support.MultiSchemaTaskExecutionDaoFactoryBean;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.util.StringUtils;

public class TaskExecutionDaoContainer {
	private final Map<String, TaskExecutionDao> taskExecutionDaoContainer = new HashMap<>();

	public TaskExecutionDaoContainer(DataSource dataSource, SchemaService schemaService) {
		for(SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			TaskExecutionDaoFactoryBean factoryBean = new MultiSchemaTaskExecutionDaoFactoryBean(dataSource, target.getTaskPrefix());
			try {
				this.taskExecutionDaoContainer.put(target.getName(), factoryBean.getObject());
			} catch (Throwable x) {
				throw  new RuntimeException("Exception creating TaskExecutionDao for " + target.getName(), x);
			}
		}
	}

	public TaskExecutionDao get(String schemaTarget) {
		if(!StringUtils.hasText(schemaTarget)) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		return taskExecutionDaoContainer.get(schemaTarget);
	}
}
