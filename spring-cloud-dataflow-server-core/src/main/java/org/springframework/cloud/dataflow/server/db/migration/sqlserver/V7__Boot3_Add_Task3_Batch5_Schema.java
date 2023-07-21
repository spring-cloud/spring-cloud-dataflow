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
package org.springframework.cloud.dataflow.server.db.migration.sqlserver;

import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.dataflow.common.flyway.SqlCommand;
import org.springframework.cloud.dataflow.server.db.migration.AbstractBoot3InitialSetupMigration;

/**
 * Adds the <a href="https://github.com/spring-cloud/spring-cloud-task/blob/main/spring-cloud-task-core/src/main/resources/org/springframework/cloud/task/schema-mariadb.sql">spring-cloud-task V3</a>
 * and <a href="https://github.com/spring-projects/spring-batch/blob/main/spring-batch-core/src/main/resources/org/springframework/batch/core/schema-mariadb.sql">spring-batch V5</a>
 * schemas to support Boot3 compatability.
 * <p>Schemas have added table prefix of {@code "BOOT3_"}.
 *
 * @author Chris Bono
 */
public class V7__Boot3_Add_Task3_Batch5_Schema extends AbstractBoot3InitialSetupMigration {

	public final static String CREATE_TASK_EXECUTION_TABLE =
			"CREATE TABLE BOOT3_TASK_EXECUTION (\n" +
					"  TASK_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,\n" +
					"  START_TIME DATETIME2 DEFAULT NULL,\n" +
					"  END_TIME DATETIME2 DEFAULT NULL,\n" +
					"  TASK_NAME  VARCHAR(100),\n" +
					"  EXIT_CODE INTEGER,\n" +
					"  EXIT_MESSAGE VARCHAR(2500),\n" +
					"  ERROR_MESSAGE VARCHAR(2500),\n" +
					"  LAST_UPDATED DATETIME2,\n" +
					"  EXTERNAL_EXECUTION_ID VARCHAR(255),\n" +
					"  PARENT_EXECUTION_ID BIGINT\n" +
					")";

	public final static String CREATE_TASK_EXECUTION_PARAMS_TABLE =
			"CREATE TABLE BOOT3_TASK_EXECUTION_PARAMS (\n" +
					"  TASK_EXECUTION_ID BIGINT NOT NULL,\n" +
					"  TASK_PARAM VARCHAR(2500),\n" +
					"  constraint BOOT3_TASK_EXEC_PARAMS_FK foreign key (TASK_EXECUTION_ID)\n" +
					"  references BOOT3_TASK_EXECUTION(TASK_EXECUTION_ID)\n" +
					")";

	public final static String CREATE_TASK_TASK_BATCH =
			"CREATE TABLE BOOT3_TASK_TASK_BATCH (\n" +
					"  TASK_EXECUTION_ID BIGINT NOT NULL,\n" +
					"  JOB_EXECUTION_ID BIGINT NOT NULL,\n" +
					"  constraint BOOT3_EXEC_BATCH_FK foreign key (TASK_EXECUTION_ID)\n" +
					"  references BOOT3_TASK_EXECUTION(TASK_EXECUTION_ID)\n" +
					")";

	public final static String CREATE_TASK_LOCK_TABLE =
			"CREATE TABLE BOOT3_TASK_LOCK (\n" +
					"  LOCK_KEY CHAR(36) NOT NULL,\n" +
					"  REGION VARCHAR(100) NOT NULL,\n" +
					"  CLIENT_ID CHAR(36),\n" +
					"  CREATED_DATE DATETIME2 NOT NULL,\n" +
					"  constraint BOOT3_LOCK_PK primary key (LOCK_KEY, REGION)\n" +
					")";

	public final static String CREATE_TASK_EXECUTION_METADATA_TABLE =
			"CREATE TABLE BOOT3_TASK_EXECUTION_METADATA (\n" +
					"    ID                      BIGINT       NOT NULL,\n" +
					"    TASK_EXECUTION_ID       BIGINT       NOT NULL,\n" +
					"    TASK_EXECUTION_MANIFEST VARCHAR(MAX) NULL,\n" +
					"    primary key (ID),\n" +
					"    CONSTRAINT BOOT3_TASK_METADATA_FK FOREIGN KEY (TASK_EXECUTION_ID) REFERENCES BOOT3_TASK_EXECUTION (TASK_EXECUTION_ID)\n" +
					")";

	public final static String CREATE_TASK_EXECUTION_METADATA_SEQ =
			"CREATE SEQUENCE BOOT3_TASK_EXECUTION_METADATA_SEQ START WITH 0 MINVALUE 0 MAXVALUE 9223372036854775807 NO CACHE NO CYCLE";

	private final static String CREATE_TASK_SEQ_SEQUENCE =
			"CREATE SEQUENCE BOOT3_TASK_SEQ START WITH 0 MINVALUE 0 MAXVALUE 9223372036854775807 NO CACHE NO CYCLE";

	public final static String CREATE_BATCH_JOB_INSTANCE_TABLE =
			"CREATE TABLE BOOT3_BATCH_JOB_INSTANCE (\n" +
					"  JOB_INSTANCE_ID BIGINT  NOT NULL PRIMARY KEY,\n" +
					"  VERSION BIGINT,\n" +
					"  JOB_NAME VARCHAR(100) NOT NULL,\n" +
					"  JOB_KEY VARCHAR(32) NOT NULL,\n" +
					"  constraint BOOT3_JOB_INST_UN unique (JOB_NAME, JOB_KEY)\n" +
					")";

	public final static String CREATE_BATCH_JOB_EXECUTION_TABLE =
			"CREATE TABLE BOOT3_BATCH_JOB_EXECUTION (\n" +
					"  JOB_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY,\n" +
					"  VERSION BIGINT,\n" +
					"  JOB_INSTANCE_ID BIGINT NOT NULL,\n" +
					"  CREATE_TIME DATETIME2 NOT NULL,\n" +
					"  START_TIME DATETIME2 DEFAULT NULL,\n" +
					"  END_TIME DATETIME2 DEFAULT NULL,\n" +
					"  STATUS VARCHAR(10),\n" +
					"  EXIT_CODE VARCHAR(2500),\n" +
					"  EXIT_MESSAGE VARCHAR(2500),\n" +
					"  LAST_UPDATED DATETIME2,\n" +
					"  JOB_CONFIGURATION_LOCATION VARCHAR(2500) NULL,\n" +
					"  constraint BOOT3_JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)\n" +
					"  references BOOT3_BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)\n" +
					")";

	public final static String CREATE_BATCH_JOB_EXECUTION_PARAMS_TABLE =
			"CREATE TABLE BOOT3_BATCH_JOB_EXECUTION_PARAMS (\n" +
					"  JOB_EXECUTION_ID BIGINT NOT NULL,\n" +
					"  PARAMETER_NAME VARCHAR(100) NOT NULL,\n" +
					"  PARAMETER_TYPE VARCHAR(100) NOT NULL,\n" +
					"  PARAMETER_VALUE VARCHAR(2500),\n" +
					"  IDENTIFYING CHAR(1) NOT NULL,\n" +
					"  constraint BOOT3_JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)\n" +
					"  references BOOT3_BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)\n" +
					")";

	public final static String CREATE_BATCH_STEP_EXECUTION_TABLE =
			"CREATE TABLE BOOT3_BATCH_STEP_EXECUTION (\n" +
					"  STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,\n" +
					"  VERSION BIGINT NOT NULL,\n" +
					"  STEP_NAME VARCHAR(100) NOT NULL,\n" +
					"  JOB_EXECUTION_ID BIGINT NOT NULL,\n" +
					"  CREATE_TIME DATETIME2 NOT NULL,\n" +
					"  START_TIME DATETIME2 NOT NULL,\n" +
					"  END_TIME DATETIME2 DEFAULT NULL,\n" +
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
					"  LAST_UPDATED DATETIME2,\n" +
					"  constraint BOOT3_JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)\n" +
					"  references BOOT3_BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)\n" +
					")";

	public final static String CREATE_BATCH_STEP_EXECUTION_CONTEXT_TABLE =
			"CREATE TABLE BOOT3_BATCH_STEP_EXECUTION_CONTEXT (\n" +
					"  STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,\n" +
					"  SHORT_CONTEXT VARCHAR(2500) NOT NULL,\n" +
					"  SERIALIZED_CONTEXT TEXT,\n" +
					"  constraint BOOT3_STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)\n" +
					"  references BOOT3_BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)\n" +
					")";

	public final static String CREATE_BATCH_JOB_EXECUTION_CONTEXT_TABLE =
			"CREATE TABLE BOOT3_BATCH_JOB_EXECUTION_CONTEXT (\n" +
					"  JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,\n" +
					"  SHORT_CONTEXT VARCHAR(2500) NOT NULL,\n" +
					"  SERIALIZED_CONTEXT TEXT,\n" +
					"  constraint BOOT3_JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)\n" +
					"  references BOOT3_BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)\n" +
					")";

	public final static String CREATE_BATCH_STEP_EXECUTION_SEQUENCE =
			"CREATE SEQUENCE BOOT3_BATCH_STEP_EXECUTION_SEQ START WITH 0 MINVALUE 0 MAXVALUE 9223372036854775807 NO CACHE NO CYCLE";

	public final static String CREATE_BATCH_JOB_EXECUTION_SEQUENCE =
			"CREATE SEQUENCE BOOT3_BATCH_JOB_EXECUTION_SEQ START WITH 0 MINVALUE 0 MAXVALUE 9223372036854775807 NO CACHE NO CYCLE";

	public final static String CREATE_BATCH_JOB_SEQUENCE =
			"CREATE SEQUENCE BOOT3_BATCH_JOB_SEQ START WITH 0 MINVALUE 0 MAXVALUE 9223372036854775807 NO CACHE NO CYCLE";

	@Override
	public List<SqlCommand> createTask3Tables() {
		return Arrays.asList(
				SqlCommand.from(CREATE_TASK_EXECUTION_TABLE),
				SqlCommand.from(CREATE_TASK_EXECUTION_PARAMS_TABLE),
				SqlCommand.from(CREATE_TASK_TASK_BATCH),
				SqlCommand.from(CREATE_TASK_SEQ_SEQUENCE),
				SqlCommand.from(CREATE_TASK_LOCK_TABLE),
				SqlCommand.from(CREATE_TASK_EXECUTION_METADATA_TABLE),
				SqlCommand.from(CREATE_TASK_EXECUTION_METADATA_SEQ));
	}

	@Override
	public List<SqlCommand> createBatch5Tables() {
		return Arrays.asList(
				SqlCommand.from(CREATE_BATCH_JOB_INSTANCE_TABLE),
				SqlCommand.from(CREATE_BATCH_JOB_EXECUTION_TABLE),
				SqlCommand.from(CREATE_BATCH_JOB_EXECUTION_PARAMS_TABLE),
				SqlCommand.from(CREATE_BATCH_STEP_EXECUTION_TABLE),
				SqlCommand.from(CREATE_BATCH_STEP_EXECUTION_CONTEXT_TABLE),
				SqlCommand.from(CREATE_BATCH_JOB_EXECUTION_CONTEXT_TABLE),
				SqlCommand.from(CREATE_BATCH_STEP_EXECUTION_SEQUENCE),
				SqlCommand.from(CREATE_BATCH_JOB_EXECUTION_SEQUENCE),
				SqlCommand.from(CREATE_BATCH_JOB_SEQUENCE));
	}
}
