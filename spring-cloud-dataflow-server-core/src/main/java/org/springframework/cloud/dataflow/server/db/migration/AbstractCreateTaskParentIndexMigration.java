/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.cloud.dataflow.server.db.migration;

import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.dataflow.common.flyway.AbstractMigration;
import org.springframework.cloud.dataflow.common.flyway.SqlCommand;

/**
 * Provide indexes to improve performance of finding child tasks.
 * @author Corneil du Plessis
 */
public abstract class AbstractCreateTaskParentIndexMigration extends AbstractMigration {
	protected static final String CREATE_TASK_PARENT_INDEX =
		"create index TASK_EXECUTION_PARENT_IX on TASK_EXECUTION(PARENT_EXECUTION_ID)";
	protected static final String CREATE_BOOT3_TASK_PARENT_INDEX =
		"create index BOOT3_TASK_EXECUTION_PARENT_IX on BOOT3_TASK_EXECUTION(PARENT_EXECUTION_ID)";

	public AbstractCreateTaskParentIndexMigration() {
		super(null);
	}

	@Override
	public List<SqlCommand> getCommands() {
		return Arrays.asList(
			SqlCommand.from(CREATE_TASK_PARENT_INDEX),
			SqlCommand.from(CREATE_BOOT3_TASK_PARENT_INDEX)
		);
	}
}