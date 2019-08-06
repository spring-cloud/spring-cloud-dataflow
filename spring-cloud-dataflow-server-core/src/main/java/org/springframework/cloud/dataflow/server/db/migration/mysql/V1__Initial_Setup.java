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
package org.springframework.cloud.dataflow.server.db.migration.mysql;

import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.dataflow.server.db.migration.AbstractInitialSetupMigration;
import org.springframework.cloud.dataflow.server.db.migration.SqlCommand;

/**
 * Initial schema setup for {@code mysql}.
 *
 * @author Janne Valkealahti
 *
 */
public class V1__Initial_Setup extends AbstractInitialSetupMigration {

	public final static String CREATE_HIBERNATE_SEQUENCE_TABLE =
			"create table if not exists hibernate_sequence (\n" +
			"    next_val bigint\n" +
			")";

	public final static String INSERT_HIBERNATE_SEQUENCE_TABLE =
			"insert into hibernate_sequence (next_val)\n" +
			"    select * from (select 1 as next_val) as temp\n" +
			"    where not exists(select * from hibernate_sequence)";

	public final static String CREATE_APP_REGISTRATION_TABLE =
			"create table app_registration (\n" +
			"  id bigint not null,\n" +
			"  object_version bigint,\n" +
			"  default_version bit,\n" +
			"  metadata_uri longtext,\n" +
			"  name varchar(255),\n" +
			"  type integer,\n" +
			"  uri longtext,\n" +
			"  version varchar(255),\n" +
			"  primary key (id)\n" +
			")";

	public final static String CREATE_AUDIT_RECORDS_TABLE =
			"create table audit_records (\n" +
			"  id bigint not null,\n" +
			"  audit_action bigint,\n" +
			"  audit_data longtext,\n" +
			"  audit_operation bigint,\n" +
			"  correlation_id varchar(255),\n" +
			"  created_by varchar(255),\n" +
			"  created_on datetime,\n" +
			"  primary key (id)\n" +
			")";

	public final static String CREATE_STREAM_DEFINITIONS_TABLE =
			"create table stream_definitions (\n" +
			"  definition_name varchar(255) not null,\n" +
			"  definition longtext,\n" +
			"  description varchar(255),\n" +
			"  primary key (definition_name)\n" +
			")";

	public final static String CREATE_TASK_DEFINITIONS_TABLE =
			"create table task_definitions (\n" +
			"  definition_name varchar(255) not null,\n" +
			"  definition longtext,\n" +
			"  description varchar(255),\n" +
			"  primary key (definition_name)\n" +
			")";

	public final static String CREATE_TASK_DEPLOYMENT_TABLE =
			"create table task_deployment (\n" +
			"  id bigint not null,\n" +
			"  object_version bigint,\n" +
			"  task_deployment_id varchar(255) not null,\n" +
			"  task_definition_name varchar(255) not null,\n" +
			"  platform_name varchar(255) not null,\n" +
			"  created_on datetime,\n" +
			"  primary key (id)\n" +
			")";

	public final static String CREATE_TASK_EXECUTION_TABLE =
			"CREATE TABLE TASK_EXECUTION (\n" +
			"  TASK_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,\n" +
			"  START_TIME DATETIME DEFAULT NULL,\n" +
			"  END_TIME DATETIME DEFAULT NULL,\n" +
			"  TASK_NAME  VARCHAR(100),\n" +
			"  EXIT_CODE INTEGER,\n" +
			"  EXIT_MESSAGE VARCHAR(2500),\n" +
			"  ERROR_MESSAGE VARCHAR(2500),\n" +
			"  LAST_UPDATED TIMESTAMP,\n" +
			"  EXTERNAL_EXECUTION_ID VARCHAR(255),\n" +
			"  PARENT_EXECUTION_ID BIGINT\n" +
			")";

	public final static String CREATE_TASK_EXECUTION_PARAMS_TABLE =
			"CREATE TABLE TASK_EXECUTION_PARAMS (\n" +
			"  TASK_EXECUTION_ID BIGINT NOT NULL,\n" +
			"  TASK_PARAM VARCHAR(2500),\n" +
			"  constraint TASK_EXEC_PARAMS_FK foreign key (TASK_EXECUTION_ID)\n" +
			"  references TASK_EXECUTION(TASK_EXECUTION_ID)\n" +
			")";

	public final static String CREATE_TASK_TASK_BATCH =
			"CREATE TABLE TASK_TASK_BATCH (\n" +
			"  TASK_EXECUTION_ID BIGINT NOT NULL,\n" +
			"  JOB_EXECUTION_ID BIGINT NOT NULL,\n" +
			"  constraint TASK_EXEC_BATCH_FK foreign key (TASK_EXECUTION_ID)\n" +
			"  references TASK_EXECUTION(TASK_EXECUTION_ID)\n" +
			")";

	private final static String CREATE_TASK_SEQ_SEQUENCE =
			"CREATE TABLE TASK_SEQ (\n" +
			"  ID BIGINT NOT NULL,\n" +
			"  UNIQUE_KEY CHAR(1) NOT NULL,\n" +
			"  constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)\n" +
			")";

	private final static String INSERT_TASK_SEQ_SEQUENCE =
			"INSERT INTO TASK_SEQ (ID, UNIQUE_KEY) select * from (select 0 as ID, '0' as UNIQUE_KEY) as tmp";

	public final static String CREATE_TASK_LOCK_TABLE =
			"CREATE TABLE TASK_LOCK (\n" +
			"  LOCK_KEY CHAR(36) NOT NULL,\n" +
			"  REGION VARCHAR(100) NOT NULL,\n" +
			"  CLIENT_ID CHAR(36),\n" +
			"  CREATED_DATE DATETIME(6) NOT NULL,\n" +
			"  constraint LOCK_PK primary key (LOCK_KEY, REGION)\n" +
			")";

	public final static String CREATE_BATCH_JOB_INSTANCE_TABLE =
			"CREATE TABLE BATCH_JOB_INSTANCE (\n" +
			"  JOB_INSTANCE_ID BIGINT  NOT NULL PRIMARY KEY,\n" +
			"  VERSION BIGINT,\n" +
			"  JOB_NAME VARCHAR(100) NOT NULL,\n" +
			"  JOB_KEY VARCHAR(32) NOT NULL,\n" +
			"  constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)\n" +
			")";

	public final static String CREATE_BATCH_JOB_EXECUTION_TABLE =
			"CREATE TABLE BATCH_JOB_EXECUTION (\n" +
			"  JOB_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY,\n" +
			"  VERSION BIGINT,\n" +
			"  JOB_INSTANCE_ID BIGINT NOT NULL,\n" +
			"  CREATE_TIME DATETIME NOT NULL,\n" +
			"  START_TIME DATETIME DEFAULT NULL,\n" +
			"  END_TIME DATETIME DEFAULT NULL,\n" +
			"  STATUS VARCHAR(10),\n" +
			"  EXIT_CODE VARCHAR(2500),\n" +
			"  EXIT_MESSAGE VARCHAR(2500),\n" +
			"  LAST_UPDATED DATETIME,\n" +
			"  JOB_CONFIGURATION_LOCATION VARCHAR(2500) NULL,\n" +
			"  constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)\n" +
			"  references BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)\n" +
			")";

	public final static String CREATE_BATCH_JOB_EXECUTION_PARAMS_TABLE =
			"CREATE TABLE BATCH_JOB_EXECUTION_PARAMS (\n" +
			"  JOB_EXECUTION_ID BIGINT NOT NULL,\n" +
			"  TYPE_CD VARCHAR(6) NOT NULL,\n" +
			"  KEY_NAME VARCHAR(100) NOT NULL,\n" +
			"  STRING_VAL VARCHAR(250),\n" +
			"  DATE_VAL DATETIME DEFAULT NULL,\n" +
			"  LONG_VAL BIGINT,\n" +
			"  DOUBLE_VAL DOUBLE PRECISION,\n" +
			"  IDENTIFYING CHAR(1) NOT NULL,\n" +
			"  constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)\n" +
			"  references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)\n" +
			")";

	public final static String CREATE_BATCH_STEP_EXECUTION_TABLE =
			"CREATE TABLE BATCH_STEP_EXECUTION (\n" +
			"  STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,\n" +
			"  VERSION BIGINT NOT NULL,\n" +
			"  STEP_NAME VARCHAR(100) NOT NULL,\n" +
			"  JOB_EXECUTION_ID BIGINT NOT NULL,\n" +
			"  START_TIME DATETIME NOT NULL,\n" +
			"  END_TIME DATETIME DEFAULT NULL,\n" +
			"  STATUS VARCHAR(10),\n" +
			"  COMMIT_COUNT BIGINT,\n" +
			"  READ_COUNT BIGINT,\n" +
			"  FILTER_COUNT BIGINT,\n" +
			"  WRITE_COUNT BIGINT,\n" +
			"  READ_SKIP_COUNT BIGINT,\n" +
			"  WRITE_SKIP_COUNT BIGINT,\n" +
			"  PROCESS_SKIP_COUNT BIGINT,\n" +
			"  ROLLBACK_COUNT BIGINT,\n" +
			"  EXIT_CODE VARCHAR(2500),\n" +
			"  EXIT_MESSAGE VARCHAR(2500),\n" +
			"  LAST_UPDATED DATETIME,\n" +
			"  constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)\n" +
			"  references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)\n" +
			")";

	public final static String CREATE_BATCH_STEP_EXECUTION_CONTEXT_TABLE =
			"CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT (\n" +
			"  STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,\n" +
			"  SHORT_CONTEXT VARCHAR(2500) NOT NULL,\n" +
			"  SERIALIZED_CONTEXT TEXT,\n" +
			"  constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)\n" +
			"  references BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)\n" +
			")";

	public final static String CREATE_BATCH_JOB_EXECUTION_CONTEXT_TABLE =
			"CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT (\n" +
			"  JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,\n" +
			"  SHORT_CONTEXT VARCHAR(2500) NOT NULL,\n" +
			"  SERIALIZED_CONTEXT TEXT,\n" +
			"  constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)\n" +
			"  references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)\n" +
			")";

	public final static String CREATE_BATCH_STEP_EXECUTION_SEQUENCE =
			"CREATE TABLE BATCH_STEP_EXECUTION_SEQ (\n" +
			"  ID BIGINT NOT NULL,\n" +
			"  UNIQUE_KEY CHAR(1) NOT NULL,\n" +
			"  constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)\n" +
			")";

	public final static String INSERT_BATCH_STEP_EXECUTION_SEQUENCE =
			"INSERT INTO BATCH_STEP_EXECUTION_SEQ (ID, UNIQUE_KEY) select * from (select 0 as ID, '0' as UNIQUE_KEY) as tmp where not exists(select * from BATCH_STEP_EXECUTION_SEQ)";

	public final static String CREATE_BATCH_JOB_EXECUTION_SEQUENCE =
			"CREATE TABLE BATCH_JOB_EXECUTION_SEQ (\n" +
			"  ID BIGINT NOT NULL,\n" +
			"  UNIQUE_KEY CHAR(1) NOT NULL,\n" +
			"  constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)\n" +
			")";

	public final static String INSERT_BATCH_JOB_EXECUTION_SEQUENCE =
			"INSERT INTO BATCH_JOB_EXECUTION_SEQ (ID, UNIQUE_KEY) select * from (select 0 as ID, '0' as UNIQUE_KEY) as tmp where not exists(select * from BATCH_JOB_EXECUTION_SEQ)";

	public final static String CREATE_BATCH_JOB_SEQUENCE =
			"CREATE TABLE BATCH_JOB_SEQ (\n" +
			"  ID BIGINT NOT NULL,\n" +
			"  UNIQUE_KEY CHAR(1) NOT NULL,\n" +
			"  constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)\n" +
			")";

	public final static String INSERT_BATCH_JOB_SEQUENCE =
			"INSERT INTO BATCH_JOB_SEQ (ID, UNIQUE_KEY) select * from (select 0 as ID, '0' as UNIQUE_KEY) as tmp where not exists(select * from BATCH_JOB_SEQ)";

	public V1__Initial_Setup() {
		super(null);
	}

	@Override
	public List<SqlCommand> createHibernateSequence() {
		return Arrays.asList(
				SqlCommand.from(CREATE_HIBERNATE_SEQUENCE_TABLE),
				SqlCommand.from(INSERT_HIBERNATE_SEQUENCE_TABLE));
	}

	@Override
	public List<SqlCommand> createAppRegistrationTable() {
		return Arrays.asList(
				SqlCommand.from(CREATE_APP_REGISTRATION_TABLE));
	}

	@Override
	public List<SqlCommand> createTaskDeploymentTable() {
		return Arrays.asList(
				SqlCommand.from(CREATE_TASK_DEPLOYMENT_TABLE));
	}

	@Override
	public List<SqlCommand> createAuditRecordsTable() {
		return Arrays.asList(
				SqlCommand.from(CREATE_AUDIT_RECORDS_TABLE));
	}

	@Override
	public List<SqlCommand> createStreamDefinitionsTable() {
		return Arrays.asList(
				SqlCommand.from(CREATE_STREAM_DEFINITIONS_TABLE));
	}

	@Override
	public List<SqlCommand> createTaskDefinitionsTable() {
		return Arrays.asList(
				SqlCommand.from(CREATE_TASK_DEFINITIONS_TABLE));
	}

	@Override
	public List<SqlCommand> createTaskTables() {
		return Arrays.asList(
				SqlCommand.from(CREATE_TASK_EXECUTION_TABLE),
				SqlCommand.from(CREATE_TASK_EXECUTION_PARAMS_TABLE),
				SqlCommand.from(CREATE_TASK_TASK_BATCH),
				SqlCommand.from(CREATE_TASK_SEQ_SEQUENCE),
				SqlCommand.from(INSERT_TASK_SEQ_SEQUENCE),
				SqlCommand.from(CREATE_TASK_LOCK_TABLE));
	}

	@Override
	public List<SqlCommand> createBatchTables() {
		return Arrays.asList(
				SqlCommand.from(CREATE_BATCH_JOB_INSTANCE_TABLE),
				SqlCommand.from(CREATE_BATCH_JOB_EXECUTION_TABLE),
				SqlCommand.from(CREATE_BATCH_JOB_EXECUTION_PARAMS_TABLE),
				SqlCommand.from(CREATE_BATCH_STEP_EXECUTION_TABLE),
				SqlCommand.from(CREATE_BATCH_STEP_EXECUTION_CONTEXT_TABLE),
				SqlCommand.from(CREATE_BATCH_JOB_EXECUTION_CONTEXT_TABLE),
				SqlCommand.from(CREATE_BATCH_STEP_EXECUTION_SEQUENCE),
				SqlCommand.from(INSERT_BATCH_STEP_EXECUTION_SEQUENCE),
				SqlCommand.from(CREATE_BATCH_JOB_EXECUTION_SEQUENCE),
				SqlCommand.from(INSERT_BATCH_JOB_EXECUTION_SEQUENCE),
				SqlCommand.from(CREATE_BATCH_JOB_SEQUENCE),
				SqlCommand.from(INSERT_BATCH_JOB_SEQUENCE));
	}
}
