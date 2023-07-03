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

import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.batch.listener.support.JdbcTaskBatchDao;
import org.springframework.util.StringUtils;

public class TaskBatchDaoContainer {
	private final Map<String, TaskBatchDao> taskBatchDaoContainer = new HashMap<>();

	public TaskBatchDaoContainer(DataSource dataSource, SchemaService schemaService) {
		for(SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			taskBatchDaoContainer.put(target.getName(), new JdbcTaskBatchDao(dataSource, target.getTaskPrefix()));
		}
	}
	public TaskBatchDao get(String schemaTarget) {
		if(!StringUtils.hasText(schemaTarget)) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		return taskBatchDaoContainer.get(schemaTarget);
	}
}
