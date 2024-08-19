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
 * Provide indexes to improve aggregate view performance
 * @author Corneil du Plessis
 */
public abstract class AbstractCreateBatchIndexesMigration extends AbstractMigration {
	protected static final String CREATE_BATCH_STEP_EXECUTION_JOB_EXECUTION_ID_INDEX =
		"create index BATCH_STEP_EXECUTION_JOB_EXECUTION_ID_IX on BATCH_STEP_EXECUTION(JOB_EXECUTION_ID)";
	protected static final String CREATE_BOOT3_BATCH_STEP_EXECUTION_JOB_EXECUTION_ID_INDEX =
		"create index BOOT3_BATCH_STEP_EXECUTION_JOB_EXECUTION_ID_IX on BOOT3_BATCH_STEP_EXECUTION(JOB_EXECUTION_ID)";
	protected static final String CREATE_BOOT3_TASK_TASK_BATCH_JOB_EXECUTION_ID_INDEX =
		"create index BOOT3_TASK_TASK_BATCH_JOB_EXECUTION_ID_IX on BOOT3_TASK_TASK_BATCH(JOB_EXECUTION_ID)";
	protected static final String CREATE_TASK_TASK_BATCH_JOB_EXECUTION_ID_INDEX =
		"create index TASK_TASK_BATCH_JOB_EXECUTION_ID_IX on TASK_TASK_BATCH(JOB_EXECUTION_ID)";
	protected static final String CREATE_BATCH_JOB_EXECUTION_START_TIME_INDEX =
		"create index BATCH_JOB_EXECUTION_START_TIME_IX on BATCH_JOB_EXECUTION(START_TIME)";
	protected static final String CREATE_BOOT3_BATCH_JOB_EXECUTION_START_TIME_INDEX =
		"create index BOOT3_BATCH_JOB_EXECUTION_START_TIME_IX on BOOT3_BATCH_JOB_EXECUTION(START_TIME)";

	public AbstractCreateBatchIndexesMigration() {
		super(null);
	}

	@Override
	public List<SqlCommand> getCommands() {
		return Arrays.asList(SqlCommand.from(CREATE_BATCH_STEP_EXECUTION_JOB_EXECUTION_ID_INDEX),
			SqlCommand.from(CREATE_BOOT3_BATCH_STEP_EXECUTION_JOB_EXECUTION_ID_INDEX),
			SqlCommand.from(CREATE_BOOT3_TASK_TASK_BATCH_JOB_EXECUTION_ID_INDEX),
			SqlCommand.from(CREATE_TASK_TASK_BATCH_JOB_EXECUTION_ID_INDEX),
			SqlCommand.from(CREATE_BATCH_JOB_EXECUTION_START_TIME_INDEX),
			SqlCommand.from(CREATE_BOOT3_BATCH_JOB_EXECUTION_START_TIME_INDEX));
	}
}
