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
package org.springframework.cloud.dataflow.server.db.migration.db2;

import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.dataflow.common.flyway.SqlCommand;
import org.springframework.cloud.dataflow.server.db.migration.AbstractRemoveBatch4Task2Tables;

/**
 * Prefixes Task V2 tables and V4 Batch tables with a V2_ prefix as well as remove the BOOT3_ prefix for V3 task and v5 batch tables.
 *
 * @author Glenn Renfro
 */
public class V12__Remove_Task2_Batch4_Support extends AbstractRemoveBatch4Task2Tables {

	/*
	 * Scripts to remove views used for Task V2/Batch V4 Task V3/Batch V5 queries.
	 */
	private final static String DROP_VIEW_AGGREGATE_TASK_EXECUTION =
		"DROP VIEW AGGREGATE_TASK_EXECUTION";

	private final static String DROP_VIEW_AGGREGATE_TASK_EXECUTION_PARAMS =
		"DROP VIEW AGGREGATE_TASK_EXECUTION_PARAMS";

	private final static String DROP_VIEW_AGGREGATE_JOB_EXECUTION =
		"DROP VIEW AGGREGATE_JOB_EXECUTION";

	private final static String DROP_VIEW_AGGREGATE_JOB_INSTANCE =
		"DROP VIEW AGGREGATE_JOB_INSTANCE";

	private final static String DROP_VIEW_AGGREGATE_TASK_BATCH =
		"DROP VIEW AGGREGATE_TASK_BATCH";

	private final static String DROP_VIEW_AGGREGATE_STEP_EXECUTION =
		"DROP VIEW AGGREGATE_STEP_EXECUTION";

	/*
	 * Scripts to rename table Task V2 tables removing BOOT_ prefix.
	 */
	private final static String RENAME_TASK_EXECUTION_V2_TABLE =
		"""
			CREATE TABLE V2_TASK_EXECUTION (
				TASK_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
				START_TIME TIMESTAMP DEFAULT NULL,
				END_TIME TIMESTAMP DEFAULT NULL,
				TASK_NAME VARCHAR(100),
				EXIT_CODE INTEGER,
				EXIT_MESSAGE VARCHAR(2500),
				ERROR_MESSAGE VARCHAR(2500),
				LAST_UPDATED TIMESTAMP,
				EXTERNAL_EXECUTION_ID VARCHAR(255),
				PARENT_EXECUTION_ID BIGINT
			   );
			   """;
	private final static String POPULATE_TASK_EXECUTION_V2_TABLE =
		"""
			INSERT INTO V2_TASK_EXECUTION
			SELECT * FROM TASK_EXECUTION;
			""";

	private final static String CLEANUP_TASK_EXECUTION_V2_TABLE =
		"""
   			DROP TABLE TASK_EXECUTION;
			""";
	private final static String RENAME_TASK_EXECUTION_PARAMS_V2_TABLE =
		"""
			CREATE  TABLE V2_TASK_EXECUTION_PARAMS (
				TASK_EXECUTION_ID BIGINT NOT NULL,
				TASK_PARAM VARCHAR(2500),
				CONSTRAINT TASK_EXEC_PARAMS_FK FOREIGN KEY (TASK_EXECUTION_ID) REFERENCES V2_TASK_EXECUTION(TASK_EXECUTION_ID)
			)
			""";
	private final static String POPULATE_TASK_EXECUTION_PARAMS_V2_TABLE =
		"""
			INSERT INTO V2_TASK_EXECUTION_PARAMS
			SELECT * FROM TASK_EXECUTION_PARAMS;
			""";
	private final static String CLEANUP_TASK_EXECUTION_PARAMS_V2_TABLE =
		"""
   			DROP TABLE TASK_EXECUTION_PARAMS;
			""";

	private final static String RENAME_TASK_TASK_BATCH_V2_TABLE =
		"""
			CREATE  TABLE V2_TASK_TASK_BATCH (
				TASK_EXECUTION_ID BIGINT NOT NULL,
				JOB_EXECUTION_ID BIGINT NOT NULL,
				CONSTRAINT TASK_EXEC_BATCH_FK FOREIGN KEY (TASK_EXECUTION_ID) REFERENCES V2_TASK_EXECUTION(TASK_EXECUTION_ID)
			);
			""";
	private final static String POPULATE_TASK_TASK_BATCH_V2_TABLE =
		"""
			INSERT INTO V2_TASK_TASK_BATCH
			SELECT * FROM TASK_TASK_BATCH;
			""";

	private final static String CLEANUP_TASK_TASK_BATCH_V2_TABLE =
		"""
   			DROP TABLE TASK_TASK_BATCH;
			""";
	private final static String RENAME_TASK_LOCK_V2_TABLE =
		"""
			CREATE  TABLE V2_TASK_LOCK (
				LOCK_KEY CHAR(36) NOT NULL,
				REGION VARCHAR(100) NOT NULL,
				CLIENT_ID CHAR(36),
				CREATED_DATE TIMESTAMP NOT NULL,
				CONSTRAINT LOCK_PK PRIMARY KEY (LOCK_KEY, REGION)
			);
			""";
	private final static String POPULATE_TASK_LOCK_V2_TABLE =
		"""
 			INSERT INTO V2_TASK_LOCK
			SELECT * FROM TASK_LOCK;
 			""";

	private final static String CLEANUP_TASK_LOCK_V2_TABLE =
		"""
			DROP TABLE TASK_LOCK;
			""";
	private final static String RENAME_TASK_V2_SEQ =
		"""
			begin
				declare newSequenceStart int;
				set newSequenceStart =  next value for TASK_SEQ;
				execute immediate 'CREATE sequence V2_TASK_SEQ  start with ' || newSequenceStart;
			end;
			""";
	private final static String CLEANUP_TASK_V2_SEQ =
		"""
   			DROP SEQUENCE TASK_SEQ;
			""";

	private final static String RENAME_TASK_EXECUTION_METADATA_V2_TABLE =
		"""
			CREATE TABLE V2_TASK_EXECUTION_METADATA (
			    ID BIGINT NOT NULL,
			    TASK_EXECUTION_ID BIGINT NOT NULL,
			    TASK_EXECUTION_MANIFEST CLOB,
			    PRIMARY KEY (ID),
			    CONSTRAINT V2_TASK_METADATA_FK FOREIGN KEY (TASK_EXECUTION_ID) REFERENCES V2_TASK_EXECUTION (TASK_EXECUTION_ID)
			);
			""";
	private final static String POPULATE_TASK_EXECUTION_METADATA_V2_TABLE =
		"""
   			INSERT INTO V2_TASK_EXECUTION_METADATA (ID, TASK_EXECUTION_ID, TASK_EXECUTION_MANIFEST)
			SELECT ID, TASK_EXECUTION_ID, TASK_EXECUTION_MANIFEST
			FROM TASK_EXECUTION_METADATA;
			""";
	private final static String CLEANUP_TASK_EXECUTION_METADATA_V2_TABLE =
		"""
   			DROP TABLE TASK_EXECUTION_METADATA;
			""";

	private final static String RENAME_TASK_EXECUTION_METADATA_V2_SEQ =
		"""
			begin
			    declare newSequenceStart int;
			    set newSequenceStart =  next value for TASK_EXECUTION_METADATA_SEQ;
			    execute immediate 'CREATE sequence V2_TASK_EXECUTION_METADATA_SEQ  start with ' || newSequenceStart;
			end;
			""";
	private final static String CLEANUP_TASK_EXECUTION_METADATA_V2_SEQ =
		"""
   			DROP SEQUENCE TASK_EXECUTION_METADATA_SEQ;
			""";

	/*
	 * Scripts to rename table Batch V5 tables removing BOOT_ prefix.
	 */

	private final static String RENAME_BATCH_JOB_INSTANCE_V4_TABLE =
		"""
			CREATE TABLE V2_BATCH_JOB_INSTANCE (
			    JOB_INSTANCE_ID BIGINT NOT NULL PRIMARY KEY,
			    VERSION BIGINT,
			    JOB_NAME VARCHAR(100) NOT NULL,
			    JOB_KEY VARCHAR(32) NOT NULL,
			    CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
			);
			""";
	private final static String POPULATE_BATCH_JOB_INSTANCE_V4_TABLE =
		"""
			INSERT INTO V2_BATCH_JOB_INSTANCE
			SELECT * FROM BATCH_JOB_INSTANCE;
			""";

	private final static String CLEANUP_BATCH_JOB_INSTANCE_V4_TABLE =
		"""
   			DROP TABLE BATCH_JOB_INSTANCE;
			""";
	private final static String RENAME_BATCH_JOB_EXECUTION_V4_TABLE =
		"""
			CREATE TABLE V2_BATCH_JOB_EXECUTION (
			    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
			    VERSION BIGINT,
			    JOB_INSTANCE_ID BIGINT NOT NULL,
			    CREATE_TIME TIMESTAMP NOT NULL,
			    START_TIME TIMESTAMP DEFAULT NULL,
			    END_TIME TIMESTAMP DEFAULT NULL,
			    STATUS VARCHAR(10),
			    EXIT_CODE VARCHAR(2500),
			    EXIT_MESSAGE VARCHAR(2500),
			    LAST_UPDATED TIMESTAMP,
			    JOB_CONFIGURATION_LOCATION VARCHAR(2500) DEFAULT NULL,
			    CONSTRAINT JOB_INST_EXEC_FK FOREIGN KEY (JOB_INSTANCE_ID) REFERENCES V2_BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
			);
			""";
	private final static String POPULATE_BATCH_JOB_EXECUTION_V4_TABLE =
		"""
			INSERT INTO V2_BATCH_JOB_EXECUTION
			SELECT * FROM BATCH_JOB_EXECUTION;
			""";

	private final static String CLEANUP_BATCH_JOB_EXECUTION_V4_TABLE =
		"""
   			DROP TABLE BATCH_JOB_EXECUTION;
			""";

	private final static String RENAME_BATCH_JOB_EXECUTION_PARAMS_V4_TABLE =
		"""
			CREATE TABLE V2_BATCH_JOB_EXECUTION_PARAMS (
			    JOB_EXECUTION_ID BIGINT NOT NULL,
			    TYPE_CD VARCHAR(6) NOT NULL,
			    KEY_NAME VARCHAR(100) NOT NULL,
			    STRING_VAL VARCHAR(250),
			    DATE_VAL TIMESTAMP DEFAULT NULL,
			    LONG_VAL BIGINT,
			    DOUBLE_VAL DOUBLE PRECISION,
			    IDENTIFYING CHAR(1) NOT NULL,
			    CONSTRAINT JOB_EXEC_PARAMS_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES V2_BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
			);
			""";
	private final static String POPULATE_BATCH_JOB_EXECUTION_PARAMS_V4_TABLE =
		"""
   			INSERT INTO V2_BATCH_JOB_EXECUTION_PARAMS
			SELECT * FROM BATCH_JOB_EXECUTION_PARAMS;
			""";

	private final static String CLEANUP_BATCH_JOB_EXECUTION_PARAMS_V4_TABLE =
		"""
   			DROP TABLE BATCH_JOB_EXECUTION_PARAMS;
			""";
	private final static String RENAME_BATCH_STEP_EXECUTION_V4_TABLE =
		"""
			CREATE TABLE V2_BATCH_STEP_EXECUTION (
			    STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
			    VERSION BIGINT NOT NULL,
			    STEP_NAME VARCHAR(100) NOT NULL,
			    JOB_EXECUTION_ID BIGINT NOT NULL,
			    START_TIME TIMESTAMP NOT NULL,
			    END_TIME TIMESTAMP DEFAULT NULL,
			    STATUS VARCHAR(10),
			    COMMIT_COUNT BIGINT,
			    READ_COUNT BIGINT,
			    FILTER_COUNT BIGINT,
			    WRITE_COUNT BIGINT,
			    READ_SKIP_COUNT BIGINT,
			    WRITE_SKIP_COUNT BIGINT,
			    PROCESS_SKIP_COUNT BIGINT,
			    ROLLBACK_COUNT BIGINT,
			    EXIT_CODE VARCHAR(2500),
			    EXIT_MESSAGE VARCHAR(2500),
			    LAST_UPDATED TIMESTAMP,
			    CONSTRAINT JOB_EXEC_STEP_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES V2_BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
			);
			""";
	private final static String POPULATE_BATCH_STEP_EXECUTION_V4_TABLE =
		"""
   			INSERT INTO V2_BATCH_STEP_EXECUTION
			SELECT * FROM BATCH_STEP_EXECUTION;
			""";

	private final static String CLEANUP_BATCH_STEP_EXECUTION_V4_TABLE =
		"""
   			DROP TABLE BATCH_STEP_EXECUTION;
			""";

	private final static String RENAME_BATCH_STEP_EXECUTION_CONTEXT_V4_TABLE =
		"""
			CREATE TABLE V2_BATCH_STEP_EXECUTION_CONTEXT (
			    STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
			    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
			    SERIALIZED_CONTEXT CLOB,
			    CONSTRAINT STEP_EXEC_CTX_FK FOREIGN KEY (STEP_EXECUTION_ID) REFERENCES V2_BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
			);
			""";
	private final static String POPULATE_BATCH_STEP_EXECUTION_CONTEXT_V4_TABLE =
		"""
			INSERT INTO V2_BATCH_STEP_EXECUTION_CONTEXT
			SELECT * FROM BATCH_STEP_EXECUTION_CONTEXT;
			""";
	private final static String CLEANUP_BATCH_STEP_EXECUTION_CONTEXT_V4_TABLE =
		"""
  			 DROP TABLE BATCH_STEP_EXECUTION_CONTEXT;
			""";

	private final static String RENAME_BATCH_JOB_EXECUTION_CONTEXT_V4_TABLE =
		"""
			CREATE TABLE V2_BATCH_JOB_EXECUTION_CONTEXT (
			    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
			    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
			    SERIALIZED_CONTEXT CLOB,
			    CONSTRAINT JOB_EXEC_CTX_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES V2_BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
			);
			""";
	private final static String POPULATE_BATCH_JOB_EXECUTION_CONTEXT_V4_TABLE =
		"""
   			INSERT INTO V2_BATCH_JOB_EXECUTION_CONTEXT
			SELECT * FROM BATCH_JOB_EXECUTION_CONTEXT;
			""";

	private final static String CLEANUP_BATCH_JOB_EXECUTION_CONTEXT_V4_TABLE =
		"""
   			DROP TABLE BATCH_JOB_EXECUTION_CONTEXT;
			""";
	private final static String RENAME_BATCH_STEP_EXECUTION_V4_SEQ =
		"""
			begin
			    declare newSequenceStart int;
			    set newSequenceStart =  next value for BATCH_STEP_EXECUTION_SEQ;
			    execute immediate 'CREATE sequence V2_BATCH_STEP_EXECUTION_SEQ  start with ' || newSequenceStart;
			end;
			""";
	private final static String CLEANUP_BATCH_STEP_EXECUTION_V4_SEQ =
		"""
   			DROP SEQUENCE BATCH_STEP_EXECUTION_SEQ;
			""";
	private final static String RENAME_BATCH_JOB_EXECUTION_V4_SEQ =
		"""
			begin
			    declare newSequenceStart int;
			    set newSequenceStart =  next value for BATCH_JOB_EXECUTION_SEQ;
			    execute immediate 'CREATE sequence V2_BATCH_JOB_EXECUTION_SEQ  start with ' || newSequenceStart;
			end;  
			""";

	private final static String CLEANUP_BATCH_JOB_EXECUTION_V4_SEQ =
		"""
   			DROP SEQUENCE BATCH_JOB_EXECUTION_SEQ;
			""";
	private final static String RENAME_BATCH_JOB_V4_SEQ =
		"""
			begin
			    declare newSequenceStart int;
			    set newSequenceStart =  next value for BATCH_JOB_SEQ;
			    execute immediate 'CREATE sequence V2_BATCH_JOB_SEQ  start with ' || newSequenceStart;
			end;
			""";
	private final static String CLEANUP_BATCH_JOB_V4_SEQ =
		"""
   			DROP SEQUENCE BATCH_JOB_SEQ;
			""";
	/*
	 * Scripts to rename table Task V3 tables removing BOOT_ prefix.
	 */
	private final static String RENAME_TASK_EXECUTION_V3_TABLE =
		"""
			CREATE TABLE TASK_EXECUTION
			(
			    TASK_EXECUTION_ID     BIGINT NOT NULL PRIMARY KEY,
			    START_TIME            TIMESTAMP(9) DEFAULT NULL,
			    END_TIME              TIMESTAMP(9) DEFAULT NULL,
			    TASK_NAME             VARCHAR(100),
			    EXIT_CODE             INTEGER,
			    EXIT_MESSAGE          VARCHAR(2500),
			    ERROR_MESSAGE         VARCHAR(2500),
			    LAST_UPDATED          TIMESTAMP(9),
			    EXTERNAL_EXECUTION_ID VARCHAR(255),
			    PARENT_EXECUTION_ID   BIGINT
			);
			""";

	private final static String POPULATE_TASK_EXECUTION_V3_TABLE =
		"""
			INSERT INTO TASK_EXECUTION
			SELECT * FROM BOOT3_TASK_EXECUTION;
			""";

	private final static String CLEANUP_TASK_EXECUTION_V3_TABLE =
		"""
			DROP TABLE BOOT3_TASK_EXECUTION;
			""";

	private final static String RENAME_TASK_EXECUTION_PARAMS_V3_TABLE =
		"""
			CREATE TABLE TASK_EXECUTION_PARAMS
			(
			    TASK_EXECUTION_ID BIGINT NOT NULL,
			    TASK_PARAM        VARCHAR(2500),
			    constraint TASK_EXEC_PARAMS_FK foreign key (TASK_EXECUTION_ID)
			        references TASK_EXECUTION (TASK_EXECUTION_ID)
			);
			""";
	private final static String POPULATE_TASK_EXECUTION_PARAMS_V3_TABLE =
		"""
			INSERT INTO TASK_EXECUTION_PARAMS
			SELECT * FROM BOOT3_TASK_EXECUTION_PARAMS;
			""";

	private final static String CLEANUP_TASK_EXECUTION_PARAMS_V3_TABLE =
		"""
			DROP TABLE BOOT3_TASK_EXECUTION_PARAMS;
			""";
	private final static String RENAME_TASK_TASK_BATCH_V3_TABLE =
		"""
			CREATE TABLE TASK_TASK_BATCH
			(
			    TASK_EXECUTION_ID BIGINT NOT NULL,
			    JOB_EXECUTION_ID  BIGINT NOT NULL,
			    constraint TASK_EXEC_BATCH_FK foreign key (TASK_EXECUTION_ID)
			        references TASK_EXECUTION (TASK_EXECUTION_ID)
			);
			""";

	private final static String POPULATE_TASK_TASK_BATCH_V3_TABLE =
		"""
			INSERT INTO TASK_TASK_BATCH
			SELECT * FROM BOOT3_TASK_TASK_BATCH;
			""";

	private final static String CLEANUP_TASK_TASK_BATCH_V3_TABLE =
		"""
			DROP TABLE BOOT3_TASK_TASK_BATCH;
			""";
	private final static String RENAME_TASK_LOCK_V3_TABLE =
		"""
			CREATE TABLE TASK_LOCK
			(
			    LOCK_KEY     CHAR(36)     NOT NULL,
			    REGION       VARCHAR(100) NOT NULL,
			    CLIENT_ID    CHAR(36),
			    CREATED_DATE TIMESTAMP(9) NOT NULL,
			    constraint LOCK_PK primary key (LOCK_KEY, REGION)
			);
			""";

	private final static String POPULATE_TASK_LOCK_V3_TABLE =
		"""
			INSERT INTO TASK_LOCK
			SELECT * FROM BOOT3_TASK_LOCK;
			""";

	private final static String CLEANUP_TASK_LOCK_V3_TABLE =
		"""
			DROP TABLE BOOT3_TASK_LOCK;
			""";
	private final static String RENAME_TASK_V3_SEQ =
		"""
			begin
			    declare newSequenceStart int;
			    set newSequenceStart =  next value for BOOT3_TASK_SEQ;
			    execute immediate 'CREATE sequence TASK_SEQ  start with ' || newSequenceStart;
			end;
			""";

	private final static String CLEANUP_TASK_V3_SEQ =
		"""
   			DROP SEQUENCE BOOT3_TASK_SEQ;
			""";
	private final static String RENAME_TASK_EXECUTION_METADATA_V3_TABLE =
		"""
			CREATE TABLE TASK_EXECUTION_METADATA
			(
			    ID                      BIGINT NOT NULL,
			    TASK_EXECUTION_ID       BIGINT NOT NULL,
			    TASK_EXECUTION_MANIFEST CLOB,
			    primary key (ID),
			    CONSTRAINT TASK_METADATA_FK FOREIGN KEY (TASK_EXECUTION_ID) REFERENCES TASK_EXECUTION (TASK_EXECUTION_ID)
			);
			""";
	private final static String POPULATE_TASK_EXECUTION_METADATA_V3_TABLE =
		"""
			INSERT INTO BOOT3_TASK_EXECUTION_METADATA
			SELECT * FROM TASK_EXECUTION_METADATA;
			""";

	private final static String CLEANUP_TASK_EXECUTION_METADATA_V3_TABLE =
		"""
			DROP TABLE BOOT3_TASK_EXECUTION_METADATA;
			""";

	private final static String RENAME_TASK_EXECUTION_METADATA_V3_SEQ =
		"""
			begin
			    declare newSequenceStart int;
			    set newSequenceStart =  next value for BOOT3_TASK_EXECUTION_METADATA_SEQ;
			    execute immediate 'CREATE sequence TASK_EXECUTION_METADATA_SEQ  start with ' || newSequenceStart;
			end;
			""";

	private final static String CLEANUP_TASK_EXECUTION_METADATA_V3_SEQ =
		"""
   			DROP SEQUENCE BOOT3_TASK_EXECUTION_METADATA_SEQ;
			""";
	/*
	 * Scripts to rename table Batch V5 tables removing BOOT_ prefix.
	 */

	private final static String RENAME_BATCH_JOB_INSTANCE_V5_TABLE =
		"""
			CREATE TABLE BATCH_JOB_INSTANCE
			(
			    JOB_INSTANCE_ID BIGINT       NOT NULL PRIMARY KEY,
			    VERSION         BIGINT,
			    JOB_NAME        VARCHAR(100) NOT NULL,
			    JOB_KEY         VARCHAR(32)  NOT NULL,
			    constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
			);
			""";

	private final static String POPULATE_BATCH_JOB_INSTANCE_V5_TABLE =
		"""
			INSERT INTO BATCH_JOB_INSTANCE
			SELECT * FROM BOOT3_BATCH_JOB_INSTANCE;
			""";

	private final static String CLEANUP_BATCH_JOB_INSTANCE_V5_TABLE =
		"""
			DROP TABLE BOOT3_BATCH_JOB_INSTANCE;
			""";

	private final static String RENAME_BATCH_JOB_EXECUTION_V5_TABLE =
		"""
			CREATE TABLE BATCH_JOB_EXECUTION
			(
			    JOB_EXECUTION_ID BIGINT       NOT NULL PRIMARY KEY,
			    VERSION          BIGINT,
			    JOB_INSTANCE_ID  BIGINT       NOT NULL,
			    CREATE_TIME      TIMESTAMP(9) NOT NULL,
			    START_TIME       TIMESTAMP(9) DEFAULT NULL,
			    END_TIME         TIMESTAMP(9) DEFAULT NULL,
			    STATUS           VARCHAR(10),
			    EXIT_CODE        VARCHAR(2500),
			    EXIT_MESSAGE     VARCHAR(2500),
			    LAST_UPDATED     TIMESTAMP(9),
			    constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
			        references BATCH_JOB_INSTANCE (JOB_INSTANCE_ID)
			);
			""";

	private final static String POPULATE_BATCH_JOB_EXECUTION_V5_TABLE =
		"""
			INSERT INTO BATCH_JOB_EXECUTION
			SELECT * FROM BOOT3_BATCH_JOB_EXECUTION;
			""";

	private final static String CLEANUP_BATCH_JOB_EXECUTION_V5_TABLE =
		"""
			DROP TABLE BOOT3_BATCH_JOB_EXECUTION;
			""";

	private final static String RENAME_BATCH_JOB_EXECUTION_PARAMS_V5_TABLE =
		"""
			CREATE TABLE BATCH_JOB_EXECUTION_PARAMS
			(
			    JOB_EXECUTION_ID BIGINT       NOT NULL,
			    PARAMETER_NAME   VARCHAR(100) NOT NULL,
			    PARAMETER_TYPE   VARCHAR(100) NOT NULL,
			    PARAMETER_VALUE  VARCHAR(2500),
			    IDENTIFYING      CHAR(1)      NOT NULL,
			    constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
			        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
			);
			""";

	private final static String POPULATE_BATCH_JOB_EXECUTION_PARAMS_V5_TABLE =
		"""
			INSERT INTO BOOT3_BATCH_JOB_EXECUTION_PARAMS
			SELECT * FROM BOOT3_BATCH_JOB_EXECUTION_PARAMS;
			""";

	private final static String CLEANUP_BATCH_JOB_EXECUTION_PARAMS_V5_TABLE =
		"""
			DROP TABLE BOOT3_BATCH_JOB_EXECUTION_PARAMS;
			""";

	private final static String RENAME_BATCH_STEP_EXECUTION_V5_TABLE =
		"""
			CREATE TABLE BATCH_STEP_EXECUTION
			(
			    STEP_EXECUTION_ID  BIGINT       NOT NULL PRIMARY KEY,
			    VERSION            BIGINT       NOT NULL,
			    STEP_NAME          VARCHAR(100) NOT NULL,
			    JOB_EXECUTION_ID   BIGINT       NOT NULL,
			    CREATE_TIME        TIMESTAMP(9) NOT NULL,
			    START_TIME         TIMESTAMP(9) DEFAULT NULL,
			    END_TIME           TIMESTAMP(9) DEFAULT NULL,
			    STATUS             VARCHAR(10),
			    COMMIT_COUNT       BIGINT,
			    READ_COUNT         BIGINT,
			    FILTER_COUNT       BIGINT,
			    WRITE_COUNT        BIGINT,
			    READ_SKIP_COUNT    BIGINT,
			    WRITE_SKIP_COUNT   BIGINT,
			    PROCESS_SKIP_COUNT BIGINT,
			    ROLLBACK_COUNT     BIGINT,
			    EXIT_CODE          VARCHAR(2500),
			    EXIT_MESSAGE       VARCHAR(2500),
			    LAST_UPDATED       TIMESTAMP(9),
			    constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)
			        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
			);
			""";

	private final static String POPULATE_BATCH_STEP_EXECUTION_V5_TABLE =
		"""
			INSERT INTO BATCH_STEP_EXECUTION
			SELECT * FROM BOOT3_BATCH_STEP_EXECUTION;
			""";

	private final static String CLEANUP_BATCH_STEP_EXECUTION_V5_TABLE =
		"""
			DROP TABLE BOOT3_BATCH_STEP_EXECUTION;
			""";

	private final static String RENAME_BATCH_STEP_EXECUTION_CONTEXT_V5_TABLE =
		"""
			CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT
			(
			    STEP_EXECUTION_ID  BIGINT        NOT NULL PRIMARY KEY,
			    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
			    SERIALIZED_CONTEXT CLOB,
			    constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
			        references BATCH_STEP_EXECUTION (STEP_EXECUTION_ID)
			);
			""";

	private final static String POPULATE_BATCH_STEP_EXECUTION_CONTEXT_V5_TABLE =
		"""
			INSERT INTO BATCH_STEP_EXECUTION_CONTEXT
			SELECT * FROM BOOT3_BATCH_STEP_EXECUTION_CONTEXT;
			""";

	private final static String CLEANUP_BATCH_STEP_EXECUTION_CONTEXT_V5_TABLE =
		"""
			DROP TABLE BOOT3_BATCH_STEP_EXECUTION_CONTEXT;
			""";
	private final static String RENAME_BATCH_JOB_EXECUTION_CONTEXT_V5_TABLE =
		"""
			CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT
			(
			    JOB_EXECUTION_ID   BIGINT        NOT NULL PRIMARY KEY,
			    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
			    SERIALIZED_CONTEXT CLOB,
			    constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)
			        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
			);
			""";

	private final static String POPULATE_BATCH_JOB_EXECUTION_CONTEXT_V5_TABLE =
		"""
			INSERT INTO BATCH_JOB_EXECUTION_CONTEXT
			SELECT * FROM BOOT3_BATCH_JOB_EXECUTION_CONTEXT;
			""";

	private final static String CLEANUP_BATCH_JOB_EXECUTION_CONTEXT_V5_TABLE =
		"""
			DROP TABLE BOOT3_BATCH_JOB_EXECUTION_CONTEXT;
			""";

	private final static String RENAME_BATCH_STEP_EXECUTION_V5_SEQ =
		"""
			begin
			    declare newSequenceStart int;
			    set newSequenceStart =  next value for BOOT3_BATCH_STEP_EXECUTION_SEQ;
			    execute immediate 'CREATE sequence BATCH_STEP_EXECUTION_SEQ  start with ' || newSequenceStart;
			end;
			""";

	private final static String CLEANUP_BATCH_STEP_EXECUTION_V5_SEQ =
		"""
			DROP SEQUENCE BOOT3_BATCH_STEP_EXECUTION_SEQ;
			""";

	private final static String RENAME_BATCH_JOB_EXECUTION_V5_SEQ =
		"""
			begin
			    declare newSequenceStart int;
			    set newSequenceStart =  next value for BOOT3_BATCH_JOB_EXECUTION_SEQ;
			    execute immediate 'CREATE sequence BATCH_JOB_EXECUTION_SEQ  start with ' || newSequenceStart;
			end;
			""";

	private final static String CLEANUP_BATCH_JOB_EXECUTION_V5_SEQ =
		"""
			DROP SEQUENCE BOOT3_BATCH_JOB_EXECUTION_SEQ;
			""";


	private final static String RENAME_BATCH_JOB_V5_SEQ =
		"""
			begin
			    declare newSequenceStart int;
			    set newSequenceStart =  next value for BOOT3_BATCH_JOB_SEQ;
			    execute immediate 'CREATE sequence BATCH_JOB_SEQ  start with ' || newSequenceStart;
			end;
			""";

	private final static String CLEANUP_BATCH_JOB_V5_SEQ =
		"""
			DROP SEQUENCE BOOT3_BATCH_JOB_SEQ;
			""";

	@Override
	public List<SqlCommand> dropBoot3Boot2Views() {
		return Arrays.asList(
			SqlCommand.from(DROP_VIEW_AGGREGATE_TASK_EXECUTION),
			SqlCommand.from(DROP_VIEW_AGGREGATE_TASK_EXECUTION_PARAMS),
			SqlCommand.from(DROP_VIEW_AGGREGATE_JOB_EXECUTION),
			SqlCommand.from(DROP_VIEW_AGGREGATE_JOB_INSTANCE),
			SqlCommand.from(DROP_VIEW_AGGREGATE_TASK_BATCH),
			SqlCommand.from(DROP_VIEW_AGGREGATE_STEP_EXECUTION)
		);
	}

	@Override
	public List<SqlCommand> renameTask3Tables() {
		return Arrays.asList(
			SqlCommand.from(RENAME_TASK_EXECUTION_V3_TABLE),
			SqlCommand.from(RENAME_TASK_EXECUTION_PARAMS_V3_TABLE),
			SqlCommand.from(RENAME_TASK_TASK_BATCH_V3_TABLE),
			SqlCommand.from(RENAME_TASK_V3_SEQ),
			SqlCommand.from(RENAME_TASK_LOCK_V3_TABLE),
			SqlCommand.from(RENAME_TASK_EXECUTION_METADATA_V3_TABLE),
			SqlCommand.from(RENAME_TASK_EXECUTION_METADATA_V3_SEQ),
			SqlCommand.from(POPULATE_TASK_EXECUTION_V3_TABLE),
			SqlCommand.from(POPULATE_TASK_EXECUTION_PARAMS_V3_TABLE),
			SqlCommand.from(POPULATE_TASK_TASK_BATCH_V3_TABLE),
			SqlCommand.from(POPULATE_TASK_LOCK_V3_TABLE),
			SqlCommand.from(POPULATE_TASK_EXECUTION_METADATA_V3_TABLE),
			SqlCommand.from(CLEANUP_TASK_EXECUTION_V3_TABLE),
			SqlCommand.from(CLEANUP_TASK_EXECUTION_PARAMS_V3_TABLE),
			SqlCommand.from(CLEANUP_TASK_TASK_BATCH_V3_TABLE),
			SqlCommand.from(CLEANUP_TASK_V3_SEQ),
			SqlCommand.from(CLEANUP_TASK_LOCK_V3_TABLE),
			SqlCommand.from(CLEANUP_TASK_EXECUTION_METADATA_V3_TABLE),
			SqlCommand.from(CLEANUP_TASK_EXECUTION_METADATA_V3_SEQ)
		);
	}

	@Override
	public List<SqlCommand> renameBatch5Tables() {
		return Arrays.asList(
			SqlCommand.from(RENAME_BATCH_JOB_INSTANCE_V5_TABLE),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_V5_TABLE),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_PARAMS_V5_TABLE),
			SqlCommand.from(RENAME_BATCH_STEP_EXECUTION_V5_TABLE),
			SqlCommand.from(RENAME_BATCH_STEP_EXECUTION_CONTEXT_V5_TABLE),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_CONTEXT_V5_TABLE),
			SqlCommand.from(RENAME_BATCH_STEP_EXECUTION_V5_SEQ),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_V5_SEQ),
			SqlCommand.from(RENAME_BATCH_JOB_V5_SEQ),

			SqlCommand.from(POPULATE_BATCH_JOB_INSTANCE_V5_TABLE),
			SqlCommand.from(POPULATE_BATCH_JOB_EXECUTION_V5_TABLE),
			SqlCommand.from(POPULATE_BATCH_JOB_EXECUTION_PARAMS_V5_TABLE),
			SqlCommand.from(POPULATE_BATCH_STEP_EXECUTION_V5_TABLE),
			SqlCommand.from(POPULATE_BATCH_STEP_EXECUTION_CONTEXT_V5_TABLE),
			SqlCommand.from(POPULATE_BATCH_JOB_EXECUTION_CONTEXT_V5_TABLE),

			SqlCommand.from(CLEANUP_BATCH_JOB_INSTANCE_V5_TABLE),
			SqlCommand.from(CLEANUP_BATCH_JOB_EXECUTION_V5_TABLE),
			SqlCommand.from(CLEANUP_BATCH_JOB_EXECUTION_PARAMS_V5_TABLE),
			SqlCommand.from(CLEANUP_BATCH_STEP_EXECUTION_V5_TABLE),
			SqlCommand.from(CLEANUP_BATCH_STEP_EXECUTION_CONTEXT_V5_TABLE),
			SqlCommand.from(CLEANUP_BATCH_JOB_EXECUTION_CONTEXT_V5_TABLE),
			SqlCommand.from(CLEANUP_BATCH_STEP_EXECUTION_V5_SEQ),
			SqlCommand.from(CLEANUP_BATCH_JOB_EXECUTION_V5_SEQ),
			SqlCommand.from(CLEANUP_BATCH_JOB_V5_SEQ)
		);
	}

	@Override
	public List<SqlCommand> renameTask2Tables() {
		return Arrays.asList(
			SqlCommand.from(RENAME_TASK_EXECUTION_V2_TABLE),
			SqlCommand.from(POPULATE_TASK_EXECUTION_V2_TABLE),
			SqlCommand.from(RENAME_TASK_EXECUTION_PARAMS_V2_TABLE),
			SqlCommand.from(POPULATE_TASK_EXECUTION_PARAMS_V2_TABLE),
			SqlCommand.from(RENAME_TASK_TASK_BATCH_V2_TABLE),
			SqlCommand.from(POPULATE_TASK_TASK_BATCH_V2_TABLE),
			SqlCommand.from(RENAME_TASK_V2_SEQ),
			SqlCommand.from(RENAME_TASK_LOCK_V2_TABLE),
			SqlCommand.from(POPULATE_TASK_LOCK_V2_TABLE),
			SqlCommand.from(RENAME_TASK_EXECUTION_METADATA_V2_TABLE),
			SqlCommand.from(POPULATE_TASK_EXECUTION_METADATA_V2_TABLE),
			SqlCommand.from(RENAME_TASK_EXECUTION_METADATA_V2_SEQ),
			SqlCommand.from(CLEANUP_TASK_EXECUTION_PARAMS_V2_TABLE),
			SqlCommand.from(CLEANUP_TASK_TASK_BATCH_V2_TABLE),
			SqlCommand.from(CLEANUP_TASK_LOCK_V2_TABLE),
			SqlCommand.from(CLEANUP_TASK_EXECUTION_METADATA_V2_TABLE),
			SqlCommand.from(CLEANUP_TASK_EXECUTION_V2_TABLE),
			SqlCommand.from(CLEANUP_TASK_EXECUTION_METADATA_V2_SEQ),
			SqlCommand.from(CLEANUP_TASK_V2_SEQ)
		);
	}

	@Override
	public List<SqlCommand> renameBatch4Tables() {
		return Arrays.asList(
			SqlCommand.from(RENAME_BATCH_JOB_INSTANCE_V4_TABLE),
			SqlCommand.from(POPULATE_BATCH_JOB_INSTANCE_V4_TABLE),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_V4_TABLE),
			SqlCommand.from(POPULATE_BATCH_JOB_EXECUTION_V4_TABLE),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_PARAMS_V4_TABLE),
			SqlCommand.from(POPULATE_BATCH_JOB_EXECUTION_PARAMS_V4_TABLE),
			SqlCommand.from(RENAME_BATCH_STEP_EXECUTION_V4_TABLE),
			SqlCommand.from(POPULATE_BATCH_STEP_EXECUTION_V4_TABLE),
			SqlCommand.from(RENAME_BATCH_STEP_EXECUTION_CONTEXT_V4_TABLE),
			SqlCommand.from(POPULATE_BATCH_STEP_EXECUTION_CONTEXT_V4_TABLE),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_CONTEXT_V4_TABLE),
			SqlCommand.from(POPULATE_BATCH_JOB_EXECUTION_CONTEXT_V4_TABLE),
			SqlCommand.from(RENAME_BATCH_STEP_EXECUTION_V4_SEQ),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_V4_SEQ),
			SqlCommand.from(RENAME_BATCH_JOB_V4_SEQ),

			SqlCommand.from(CLEANUP_BATCH_JOB_INSTANCE_V4_TABLE),
			SqlCommand.from(CLEANUP_BATCH_JOB_EXECUTION_V4_TABLE),
			SqlCommand.from(CLEANUP_BATCH_JOB_EXECUTION_PARAMS_V4_TABLE),
			SqlCommand.from(CLEANUP_BATCH_STEP_EXECUTION_V4_TABLE),
			SqlCommand.from(CLEANUP_BATCH_STEP_EXECUTION_CONTEXT_V4_TABLE),
			SqlCommand.from(CLEANUP_BATCH_JOB_EXECUTION_CONTEXT_V4_TABLE),
			SqlCommand.from(CLEANUP_BATCH_STEP_EXECUTION_V4_SEQ),
			SqlCommand.from(CLEANUP_BATCH_JOB_EXECUTION_V4_SEQ),
			SqlCommand.from(CLEANUP_BATCH_JOB_V4_SEQ)

		);
	}

}
