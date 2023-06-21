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
package org.springframework.cloud.dataflow.server.db.migration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.dataflow.common.flyway.AbstractMigration;
import org.springframework.cloud.dataflow.common.flyway.SqlCommand;

/**
 * Base implementation for initial Boot 3 schema
 *
 * @author Chris Bono
 */
public abstract class AbstractBoot3InitialSetupMigration extends AbstractMigration {
	public final static String CREATE_AGGREGATE_TASK_EXECUTION_VIEW = "CREATE VIEW AGGREGATE_TASK_EXECUTION AS\n" +
			"    SELECT TASK_EXECUTION_ID, START_TIME, END_TIME, TASK_NAME, EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID, 'boot2' AS SCHEMA_TARGET FROM TASK_EXECUTION\n" +
			"UNION ALL\n" +
			"    SELECT TASK_EXECUTION_ID, START_TIME, END_TIME, TASK_NAME, EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID, 'boot3' AS SCHEMA_TARGET FROM BOOT3_TASK_EXECUTION";

	public final static String CREATE_AGGREGATE_TASK_EXECUTION_PARAMS_VIEW = "CREATE VIEW AGGREGATE_TASK_EXECUTION_PARAMS AS\n" +
			"    SELECT TASK_EXECUTION_ID, TASK_PARAM, 'boot2' AS SCHEMA_TARGET FROM TASK_EXECUTION_PARAMS\n" +
			"UNION ALL\n" +
			"    SELECT TASK_EXECUTION_ID, TASK_PARAM, 'boot3' AS SCHEMA_TARGET FROM BOOT3_TASK_EXECUTION_PARAMS";
	public final static String CREATE_AGGREGATE_JOB_EXECUTION_VIEW = "CREATE VIEW AGGREGATE_JOB_EXECUTION AS\n" +
			"    SELECT JOB_EXECUTION_ID, VERSION, JOB_INSTANCE_ID, CREATE_TIME, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, LAST_UPDATED, 'boot2' AS SCHEMA_TARGET FROM BATCH_JOB_EXECUTION\n" +
			"UNION ALL\n" +
			"    SELECT JOB_EXECUTION_ID, VERSION, JOB_INSTANCE_ID, CREATE_TIME, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, LAST_UPDATED, 'boot3' AS SCHEMA_TARGET FROM BOOT3_BATCH_JOB_EXECUTION";
	public final static String CREATE_AGGREGATE_JOB_INSTANCE_VIEW = "CREATE VIEW AGGREGATE_JOB_INSTANCE AS\n" +
			"    SELECT JOB_INSTANCE_ID, VERSION, JOB_NAME, JOB_KEY, 'boot2' AS SCHEMA_TARGET FROM BATCH_JOB_INSTANCE\n" +
			"UNION ALL\n" +
			"    SELECT JOB_INSTANCE_ID, VERSION, JOB_NAME, JOB_KEY, 'boot3' AS SCHEMA_TARGET FROM BOOT3_BATCH_JOB_INSTANCE";
	public final static String CREATE_AGGREGATE_TASK_BATCH_VIEW = "CREATE VIEW AGGREGATE_TASK_BATCH AS\n" +
			"    SELECT TASK_EXECUTION_ID, JOB_EXECUTION_ID, 'boot2' AS SCHEMA_TARGET FROM TASK_TASK_BATCH\n" +
			"UNION ALL\n" +
			"    SELECT TASK_EXECUTION_ID, JOB_EXECUTION_ID, 'boot3' AS SCHEMA_TARGET FROM BOOT3_TASK_TASK_BATCH";
	public final static String CREATE_AGGREGATE_STEP_EXECUTION_VIEW = "CREATE VIEW AGGREGATE_STEP_EXECUTION AS\n" +
			"    SELECT STEP_EXECUTION_ID, VERSION, STEP_NAME, JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, COMMIT_COUNT, READ_COUNT, FILTER_COUNT, WRITE_COUNT, READ_SKIP_COUNT, WRITE_SKIP_COUNT, PROCESS_SKIP_COUNT, ROLLBACK_COUNT, EXIT_CODE, EXIT_MESSAGE, LAST_UPDATED, 'boot2' AS SCHEMA_TARGET FROM BATCH_STEP_EXECUTION\n" +
			"UNION ALL\n" +
			"    SELECT STEP_EXECUTION_ID, VERSION, STEP_NAME, JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, COMMIT_COUNT, READ_COUNT, FILTER_COUNT, WRITE_COUNT, READ_SKIP_COUNT, WRITE_SKIP_COUNT, PROCESS_SKIP_COUNT, ROLLBACK_COUNT, EXIT_CODE, EXIT_MESSAGE, LAST_UPDATED, 'boot3' AS SCHEMA_TARGET FROM BOOT3_BATCH_STEP_EXECUTION";

	public AbstractBoot3InitialSetupMigration() {
		super(null);
	}

	@Override
	public List<SqlCommand> getCommands() {
		List<SqlCommand> commands = new ArrayList<>();
		commands.addAll(createTask3Tables());
		commands.addAll(createBatch5Tables());
		commands.addAll(createAggregateViews());
		return commands;
	}

	/**
	 * Creates the spring-cloud-task V3 tables.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createTask3Tables();

	/**
	 * Creates the spring-batch V5 tables.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createBatch5Tables();

	public abstract List<SqlCommand> createAggregateViews();
}
