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
package org.springframework.cloud.dataflow.server.db.migration.postgresql;

import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

import org.springframework.cloud.dataflow.server.db.migration.AbstractBaselineCallback;
import org.springframework.cloud.dataflow.server.db.migration.SqlCommand;

/**
 * Baselining schema setup for {@code postgres}.
 *
 * @author Janne Valkealahti
 *
 */
public class PostgresBeforeBaseline extends AbstractBaselineCallback {

	public final static String DROP_AUDIT_RECORDS_AUDIT_ACTION_IDX_INDEX =
			"drop index if exists AUDIT_RECORDS_AUDIT_ACTION_IDX";

	public final static String DROP_AUDIT_RECORDS_AUDIT_OPERATION_IDX_INDEX =
			"drop index if exists AUDIT_RECORDS_AUDIT_OPERATION_IDX";

	public final static String DROP_AUDIT_RECORDS_CORRELATION_ID_IDX_INDEX =
			"drop index if exists AUDIT_RECORDS_CORRELATION_ID_IDX";

	public final static String DROP_AUDIT_RECORDS_CREATED_ON_IDX_INDEX =
			"drop index if exists AUDIT_RECORDS_CREATED_ON_IDX";

	public final static String CREATE_APP_REGISTRATION_TMP_TABLE =
			V1__Initial_Setup.CREATE_APP_REGISTRATION_TABLE.replaceFirst("app_registration", "app_registration_tmp");

	public final static String INSERT_APP_REGISTRATION_DATA =
			"insert into\n" +
			"  app_registration_tmp (id, object_version, default_version, metadata_uri, name, type, uri, version) \n" +
			"  select id, object_Version, default_Version, metadata_Uri, name, type, uri, version\n" +
			"  from APP_REGISTRATION";

	public final static String DROP_APP_REGISTRATION_TABLE =
			"drop table APP_REGISTRATION";

	public final static String RENAME_APP_REGISTRATION_TMP_TABLE =
			"alter table app_registration_tmp rename to app_registration";

	public final static String CREATE_STREAM_DEFINITIONS_TMP_TABLE =
			V1__Initial_Setup.CREATE_STREAM_DEFINITIONS_TABLE.replaceFirst("stream_definitions", "stream_definitions_tmp");

	public final static String DROP_STREAM_DEFINITIONS_TABLE =
			"drop table STREAM_DEFINITIONS";

	public final static String RENAME_STREAM_DEFINITIONS_TMP_TABLE =
			"alter table stream_definitions_tmp rename to stream_definitions";

	public final static String CREATE_TASK_DEFINITIONS_TMP_TABLE =
			V1__Initial_Setup.CREATE_TASK_DEFINITIONS_TABLE.replaceFirst("task_definitions", "task_definitions_tmp");

	public final static String DROP_TASK_DEFINITIONS_TABLE =
			"drop table TASK_DEFINITIONS";

	public final static String RENAME_TASK_DEFINITIONS_TMP_TABLE =
			"alter table task_definitions_tmp rename to task_definitions";

	public final static String CREATE_AUDIT_RECORDS_TMP_TABLE =
			V1__Initial_Setup.CREATE_AUDIT_RECORDS_TABLE.replaceFirst("audit_records", "audit_records_tmp");

	public final static String INSERT_AUDIT_RECORDS_DATA =
			"insert into\n" +
			"  audit_records_tmp (id, audit_action, audit_data, audit_operation, correlation_id, created_by, created_on)\n" +
			"  select id, audit_Action, audit_data, audit_Operation, correlation_id, created_by, created_On\n" +
			"  from AUDIT_RECORDS";

	public final static String DROP_AUDIT_RECORDS_TABLE =
			"drop table AUDIT_RECORDS";

	public final static String RENAME_AUDIT_RECORDS_TMP_TABLE =
			"alter table audit_records_tmp rename to audit_records";

	public final static String CREATE_AUDIT_RECORDS_AUDIT_ACTION_IDX_INDEX =
			"create index if not exists audit_records_audit_action_idx on audit_records (audit_action)";

	public final static String CREATE_AUDIT_RECORDS_AUDIT_OPERATION_IDX_INDEX =
			"create index if not exists audit_records_audit_operation_idx on audit_records (audit_operation)";

	public final static String CREATE_AUDIT_RECORDS_CORRELATION_ID_IDX_INDEX =
			"create index if not exists audit_records_correlation_id_idx on audit_records (correlation_id)";

	public final static String CREATE_AUDIT_RECORDS_CREATED_ON_IDX_INDEX =
			"create index if not exists audit_records_created_on_idx on audit_records (created_on)";

	/**
	 * Instantiates a new postgres before baseline.
	 */
	public PostgresBeforeBaseline() {
		super(new V1__Initial_Setup());
	}

	@Override
	public boolean canHandleInTransaction(Event event, Context context) {
		// postgresql is one database where a single any error, even query error,
		// results transaction to get aborted in a connection, so tell
		// flyway not to use it. i.e. we check if some tables exists and this
		// will result errors if table to check is not there.
		return false;
	}

	@Override
	public List<SqlCommand> dropIndexes() {
		return Arrays.asList(
				SqlCommand.from(DROP_AUDIT_RECORDS_AUDIT_ACTION_IDX_INDEX),
				SqlCommand.from(DROP_AUDIT_RECORDS_AUDIT_OPERATION_IDX_INDEX),
				SqlCommand.from(DROP_AUDIT_RECORDS_CORRELATION_ID_IDX_INDEX),
				SqlCommand.from(DROP_AUDIT_RECORDS_CREATED_ON_IDX_INDEX));
	}

	@Override
	public List<SqlCommand> changeAppRegistrationTable() {
		return Arrays.asList(
				SqlCommand.from(CREATE_APP_REGISTRATION_TMP_TABLE),
				SqlCommand.from(INSERT_APP_REGISTRATION_DATA),
				SqlCommand.from(DROP_APP_REGISTRATION_TABLE),
				SqlCommand.from(RENAME_APP_REGISTRATION_TMP_TABLE));
	}

	@Override
	public List<SqlCommand> changeUriRegistryTable() {
		return Arrays.asList(
				new PostgresMigrateUriRegistrySqlCommand());
	}

	@Override
	public List<SqlCommand> changeStreamDefinitionsTable() {
		return Arrays.asList(
				SqlCommand.from(CREATE_STREAM_DEFINITIONS_TMP_TABLE),
				// need to run copy command programmatically
				new PostgresMigrateStreamDefinitionsSqlCommand(),
				SqlCommand.from(DROP_STREAM_DEFINITIONS_TABLE),
				SqlCommand.from(RENAME_STREAM_DEFINITIONS_TMP_TABLE));
	}

	@Override
	public List<SqlCommand> changeTaskDefinitionsTable() {
		return Arrays.asList(
				SqlCommand.from(CREATE_TASK_DEFINITIONS_TMP_TABLE),
				// need to run copy command programmatically
				new PostgresMigrateTaskDefinitionsSqlCommand(),
				SqlCommand.from(DROP_TASK_DEFINITIONS_TABLE),
				SqlCommand.from(RENAME_TASK_DEFINITIONS_TMP_TABLE));
	}

	@Override
	public List<SqlCommand> changeAuditRecordsTable() {
		return Arrays.asList(
				SqlCommand.from(CREATE_AUDIT_RECORDS_TMP_TABLE),
				SqlCommand.from(INSERT_AUDIT_RECORDS_DATA),
				SqlCommand.from(DROP_AUDIT_RECORDS_TABLE),
				SqlCommand.from(RENAME_AUDIT_RECORDS_TMP_TABLE));
	}

	@Override
	public List<SqlCommand> createIndexes() {
		return Arrays.asList(
				SqlCommand.from(CREATE_AUDIT_RECORDS_AUDIT_ACTION_IDX_INDEX),
				SqlCommand.from(CREATE_AUDIT_RECORDS_AUDIT_OPERATION_IDX_INDEX),
				SqlCommand.from(CREATE_AUDIT_RECORDS_CORRELATION_ID_IDX_INDEX),
				SqlCommand.from(CREATE_AUDIT_RECORDS_CREATED_ON_IDX_INDEX));
	}

	@Override
	public List<SqlCommand> createTaskLockTable() {
		return Arrays.asList(
				SqlCommand.from(V1__Initial_Setup.CREATE_TASK_LOCK_TABLE));
	}

	@Override
	public List<SqlCommand> createTaskDeploymentTable() {
		return Arrays.asList(SqlCommand.from(
				V1__Initial_Setup.CREATE_TASK_DEPLOYMENT_TABLE));
	}
}
