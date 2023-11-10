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
package org.springframework.cloud.dataflow.server.db.migration.oracle;

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
					"TASK_EXECUTION_ID NUMBER NOT NULL PRIMARY KEY ,\n" +
					"START_TIME TIMESTAMP(9) DEFAULT NULL,\n" +
					"END_TIME TIMESTAMP(9) DEFAULT NULL,\n" +
					"TASK_NAME  VARCHAR2(100),\n" +
					"EXIT_CODE INTEGER,\n" +
					"EXIT_MESSAGE VARCHAR2(2500),\n" +
					"ERROR_MESSAGE VARCHAR2(2500),\n" +
					"LAST_UPDATED TIMESTAMP(9),\n" +
					"EXTERNAL_EXECUTION_ID VARCHAR2(255),\n" +
					"PARENT_EXECUTION_ID NUMBER\n" +
					")SEGMENT CREATION IMMEDIATE";

	public final static String CREATE_TASK_EXECUTION_PARAMS_TABLE =
			"CREATE TABLE BOOT3_TASK_EXECUTION_PARAMS (\n" +
					"TASK_EXECUTION_ID NUMBER NOT NULL,\n" +
					"TASK_PARAM VARCHAR2(2500),\n" +
					"    constraint BOOT3_TASK_EXEC_PARAMS_FK foreign key (TASK_EXECUTION_ID)\n" +
					"    references BOOT3_TASK_EXECUTION (TASK_EXECUTION_ID)\n" +
					")SEGMENT CREATION IMMEDIATE";

	public final static String CREATE_TASK_TASK_BATCH =
			"CREATE TABLE BOOT3_TASK_TASK_BATCH (\n" +
					"TASK_EXECUTION_ID NUMBER NOT NULL,\n" +
					"JOB_EXECUTION_ID NUMBER NOT NULL,\n" +
					"  constraint BOOT3_EXEC_BATCH_FK foreign key (TASK_EXECUTION_ID)\n" +
					"  references BOOT3_TASK_EXECUTION(TASK_EXECUTION_ID)\n" +
					") SEGMENT CREATION IMMEDIATE";

	public final static String CREATE_TASK_LOCK_TABLE =
			"CREATE TABLE BOOT3_TASK_LOCK (\n" +
					"LOCK_KEY VARCHAR2(36) NOT NULL,\n" +
					"REGION VARCHAR2(100) NOT NULL,\n" +
					"CLIENT_ID VARCHAR2(36),\n" +
					"CREATED_DATE TIMESTAMP(9) NOT NULL,\n" +
					"constraint BOOT3_LOCK_PK primary key (LOCK_KEY, REGION)\n" +
					")SEGMENT CREATION IMMEDIATE";

	private final static String CREATE_TASK_SEQ_SEQUENCE =
			"CREATE SEQUENCE BOOT3_TASK_SEQ START WITH 1 MINVALUE 1 MAXVALUE 9223372036854775806 INCREMENT BY 1 NOCACHE NOCYCLE";

	private final static String CREATE_TASK_EXECUTION_METADATA_TABLE = "CREATE TABLE BOOT3_TASK_EXECUTION_METADATA (\n" +
			"    ID                      NUMBER NOT NULL,\n" +
			"    TASK_EXECUTION_ID       NUMBER NOT NULL,\n" +
			"    TASK_EXECUTION_MANIFEST CLOB,\n" +
			"    primary key (ID),\n" +
			"    CONSTRAINT BOOT3_TASK_METADATA_FK FOREIGN KEY (TASK_EXECUTION_ID) REFERENCES BOOT3_TASK_EXECUTION (TASK_EXECUTION_ID)\n" +
			") SEGMENT CREATION IMMEDIATE";

	private final static String CREATE_TASK_EXECUTION_METADATA_SEQ = "CREATE SEQUENCE BOOT3_TASK_EXECUTION_METADATA_SEQ START WITH 1 MINVALUE 1 MAXVALUE 9223372036854775807 ORDER NOCYCLE";

	public final static String CREATE_BATCH_JOB_INSTANCE_TABLE =
			"CREATE TABLE BOOT3_BATCH_JOB_INSTANCE (\n" +
					"JOB_INSTANCE_ID NUMBER(19,0)  NOT NULL PRIMARY KEY ,\n" +
					"VERSION NUMBER(19,0) ,\n" +
					"JOB_NAME VARCHAR2(100 char) NOT NULL,\n" +
					"JOB_KEY VARCHAR2(32 char) NOT NULL,\n" +
					"  constraint BOOT3_JOB_INST_UN unique (JOB_NAME, JOB_KEY)\n" +
					") SEGMENT CREATION IMMEDIATE";

	public final static String CREATE_BATCH_JOB_EXECUTION_TABLE =
			"CREATE TABLE BOOT3_BATCH_JOB_EXECUTION (\n" +
					"JOB_EXECUTION_ID NUMBER(19,0)  NOT NULL PRIMARY KEY,\n" +
					"VERSION NUMBER(19,0),\n" +
					"JOB_INSTANCE_ID NUMBER(19,0) NOT NULL,\n" +
					"CREATE_TIME TIMESTAMP(9) NOT NULL,\n" +
					"START_TIME TIMESTAMP(9) DEFAULT NULL,\n" +
					"END_TIME TIMESTAMP(9) DEFAULT NULL,\n" +
					"STATUS VARCHAR2(10 char),\n" +
					"EXIT_CODE VARCHAR2(2500 char),\n" +
					"EXIT_MESSAGE VARCHAR2(2500 char),\n" +
					"LAST_UPDATED TIMESTAMP(9),\n" +
					"JOB_CONFIGURATION_LOCATION VARCHAR(2500 char) NULL,\n" +
					" constraint BOOT3_JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)\n" +
					"references BOOT3_BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)\n" +
					") SEGMENT CREATION IMMEDIATE";

	public final static String CREATE_BATCH_JOB_EXECUTION_PARAMS_TABLE =
			"CREATE TABLE BOOT3_BATCH_JOB_EXECUTION_PARAMS (\n" +
					"JOB_EXECUTION_ID NUMBER(19,0) NOT NULL,\n" +
					"TYPE_CD VARCHAR2(6 char) NOT NULL,\n" +
					"KEY_NAME VARCHAR2(100 char) NOT NULL,\n" +
					"STRING_VAL VARCHAR2(250 char),\n" +
					"DATE_VAL TIMESTAMP(9) DEFAULT NULL,\n" +
					"LONG_VAL NUMBER(19,0),\n" +
					"DOUBLE_VAL NUMBER,\n" +
					"IDENTIFYING CHAR(1) NOT NULL,\n" +
					"  constraint BOOT3_JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)\n" +
					"  references BOOT3_BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)\n" +
					") SEGMENT CREATION IMMEDIATE";

	public final static String CREATE_BATCH_STEP_EXECUTION_TABLE =
			"CREATE TABLE BOOT3_BATCH_STEP_EXECUTION (\n" +
					"STEP_EXECUTION_ID NUMBER(19,0) NOT NULL PRIMARY KEY,\n" +
					"VERSION NUMBER(19,0) NOT NULL,\n" +
					"STEP_NAME VARCHAR2(100 char) NOT NULL,\n" +
					"JOB_EXECUTION_ID NUMBER(19,0) NOT NULL,\n" +
					"CREATE_TIME TIMESTAMP(9) NOT NULL,\n" +
					"START_TIME TIMESTAMP(9) DEFAULT NULL,\n" +
					"END_TIME TIMESTAMP(9) DEFAULT NULL,\n" +
					"STATUS VARCHAR2(10 char),\n" +
					"COMMIT_COUNT NUMBER(19,0),\n" +
					"READ_COUNT NUMBER(19,0),\n" +
					"FILTER_COUNT NUMBER(19,0),\n" +
					"WRITE_COUNT NUMBER(19,0),\n" +
					"READ_SKIP_COUNT NUMBER(19,0),\n" +
					"WRITE_SKIP_COUNT NUMBER(19,0),\n" +
					"PROCESS_SKIP_COUNT NUMBER(19,0),\n" +
					"ROLLBACK_COUNT NUMBER(19,0),\n" +
					"EXIT_CODE VARCHAR2(2500 char),\n" +
					"EXIT_MESSAGE VARCHAR2(2500 char),\n" +
					"LAST_UPDATED TIMESTAMP(9),\n" +
					"  constraint BOOT3_JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)\n" +
					"  references BOOT3_BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)\n" +
					") SEGMENT CREATION IMMEDIATE";

	public final static String CREATE_BATCH_STEP_EXECUTION_CONTEXT_TABLE =
			"CREATE TABLE BOOT3_BATCH_STEP_EXECUTION_CONTEXT (\n" +
					"STEP_EXECUTION_ID NUMBER(19,0) NOT NULL PRIMARY KEY,\n" +
					"SHORT_CONTEXT VARCHAR2(2500 char) NOT NULL,\n" +
					"SERIALIZED_CONTEXT CLOB,\n" +
					"  constraint BOOT3_STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)\n" +
					"  references BOOT3_BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)\n" +
					") SEGMENT CREATION IMMEDIATE";

	public final static String CREATE_BATCH_JOB_EXECUTION_CONTEXT_TABLE =
			"CREATE TABLE BOOT3_BATCH_JOB_EXECUTION_CONTEXT (\n" +
					"JOB_EXECUTION_ID NUMBER(19,0) NOT NULL PRIMARY KEY,\n" +
					"SHORT_CONTEXT VARCHAR2(2500 char) NOT NULL,\n" +
					"SERIALIZED_CONTEXT CLOB,\n" +
					"  constraint BOOT3_JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)\n" +
					"  references BOOT3_BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)\n" +
					") SEGMENT CREATION IMMEDIATE";

	public final static String CREATE_BATCH_STEP_EXECUTION_SEQUENCE =
			"CREATE SEQUENCE BOOT3_BATCH_STEP_EXECUTION_SEQ START WITH 1 MINVALUE 1 MAXVALUE 9223372036854775806 INCREMENT BY 1 NOCACHE NOCYCLE";

	public final static String CREATE_BATCH_JOB_EXECUTION_SEQUENCE =
			"CREATE SEQUENCE BOOT3_BATCH_JOB_EXECUTION_SEQ START WITH 1 MINVALUE 1 MAXVALUE 9223372036854775806 INCREMENT BY 1 NOCACHE NOCYCLE";

	public final static String CREATE_BATCH_JOB_SEQUENCE =
			"CREATE SEQUENCE BOOT3_BATCH_JOB_SEQ START WITH 1 MINVALUE 1 MAXVALUE 9223372036854775806 INCREMENT BY 1 NOCACHE NOCYCLE";

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
