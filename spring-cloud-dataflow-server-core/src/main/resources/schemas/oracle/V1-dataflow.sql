create sequence hibernate_sequence start with 1 increment by 1;

create table app_registration (
  id number(19,0) not null,
  object_version number(19,0),
  default_version number(1,0),
  metadata_uri clob,
  name varchar2(255 char),
  type number(10,0),
  uri clob,
  version varchar2(255 char),
  primary key (id)
) segment creation immediate;

create table task_deployment (
  id number(19,0) not null,
  object_version number(19,0),
  task_deployment_id varchar2(255 char) not null,
  task_definition_name varchar2(255 char) not null,
  platform_name varchar2(255 char) not null,
  created_on timestamp,
  primary key (id)
) segment creation immediate;

create table audit_records (
  id number(19,0) not null,
  audit_action number(19,0),
  audit_data long,
  audit_operation number(19,0),
  correlation_id varchar2(255 char),
  created_by varchar2(255 char),
  created_on timestamp,
  primary key (id)
) segment creation immediate;

create table stream_definitions (
  definition_name varchar2(255 char) not null,
  definition clob,
  primary key (definition_name)
) segment creation immediate;

create table task_definitions (
  definition_name varchar2(255 char) not null,
  definition clob,
  primary key (definition_name)
) segment creation immediate;

CREATE TABLE TASK_EXECUTION (
  TASK_EXECUTION_ID NUMBER NOT NULL PRIMARY KEY,
  START_TIME TIMESTAMP DEFAULT NULL,
  END_TIME TIMESTAMP DEFAULT NULL,
  TASK_NAME  VARCHAR2(100),
  EXIT_CODE INTEGER,
  EXIT_MESSAGE VARCHAR2(2500),
  ERROR_MESSAGE VARCHAR2(2500),
  LAST_UPDATED TIMESTAMP,
  EXTERNAL_EXECUTION_ID VARCHAR2(255),
  PARENT_EXECUTION_ID NUMBER
) segment creation immediate;

CREATE TABLE TASK_EXECUTION_PARAMS (
  TASK_EXECUTION_ID NUMBER NOT NULL,
  TASK_PARAM VARCHAR2(2500),
  constraint TASK_EXEC_PARAMS_FK foreign key (TASK_EXECUTION_ID)
  references TASK_EXECUTION(TASK_EXECUTION_ID)
) segment creation immediate;

CREATE TABLE TASK_TASK_BATCH (
  TASK_EXECUTION_ID NUMBER NOT NULL,
  JOB_EXECUTION_ID NUMBER NOT NULL,
  constraint TASK_EXEC_BATCH_FK foreign key (TASK_EXECUTION_ID)
  references TASK_EXECUTION(TASK_EXECUTION_ID)
) segment creation immediate;

CREATE SEQUENCE TASK_SEQ START WITH 0 MINVALUE 0 MAXVALUE 9223372036854775807 NOCACHE NOCYCLE;

CREATE TABLE TASK_LOCK (
  LOCK_KEY CHAR(36) NOT NULL,
  REGION VARCHAR(100) NOT NULL,
  CLIENT_ID CHAR(36),
  CREATED_DATE TIMESTAMP NOT NULL,
  constraint LOCK_PK primary key (LOCK_KEY, REGION)
) segment creation immediate;

CREATE TABLE BATCH_JOB_INSTANCE (
  JOB_INSTANCE_ID NUMBER(19,0) NOT NULL PRIMARY KEY,
  VERSION NUMBER(19,0),
  JOB_NAME VARCHAR2(100 char) NOT NULL,
  JOB_KEY VARCHAR2(32 char) NOT NULL,
  constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
) segment creation immediate;

CREATE TABLE BATCH_JOB_EXECUTION (
  JOB_EXECUTION_ID NUMBER(19,0) NOT NULL PRIMARY KEY,
  VERSION NUMBER(19,0),
  JOB_INSTANCE_ID NUMBER(19,0) NOT NULL,
  CREATE_TIME TIMESTAMP NOT NULL,
  START_TIME TIMESTAMP DEFAULT NULL,
  END_TIME TIMESTAMP DEFAULT NULL,
  STATUS VARCHAR2(10 char),
  EXIT_CODE VARCHAR2(2500 char),
  EXIT_MESSAGE VARCHAR2(2500 char),
  LAST_UPDATED TIMESTAMP,
  JOB_CONFIGURATION_LOCATION VARCHAR(2500 char) NULL,
  constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
  references BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
) segment creation immediate;

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS (
  JOB_EXECUTION_ID NUMBER(19,0) NOT NULL,
  TYPE_CD VARCHAR2(6 char) NOT NULL,
  KEY_NAME VARCHAR2(100 char) NOT NULL,
  STRING_VAL VARCHAR2(250 char),
  DATE_VAL TIMESTAMP DEFAULT NULL,
  LONG_VAL NUMBER(19,0),
  DOUBLE_VAL NUMBER,
  IDENTIFYING CHAR(1) NOT NULL,
  constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
  references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) segment creation immediate;

CREATE TABLE BATCH_STEP_EXECUTION (
  STEP_EXECUTION_ID NUMBER(19,0) NOT NULL PRIMARY KEY,
  VERSION NUMBER(19,0) NOT NULL,
  STEP_NAME VARCHAR2(100 char) NOT NULL,
  JOB_EXECUTION_ID NUMBER(19,0) NOT NULL,
  START_TIME TIMESTAMP NOT NULL,
  END_TIME TIMESTAMP DEFAULT NULL,
  STATUS VARCHAR2(10 char),
  COMMIT_COUNT NUMBER(19,0),
  READ_COUNT NUMBER(19,0),
  FILTER_COUNT NUMBER(19,0),
  WRITE_COUNT NUMBER(19,0),
  READ_SKIP_COUNT NUMBER(19,0),
  WRITE_SKIP_COUNT NUMBER(19,0),
  PROCESS_SKIP_COUNT NUMBER(19,0),
  ROLLBACK_COUNT NUMBER(19,0),
  EXIT_CODE VARCHAR2(2500 char),
  EXIT_MESSAGE VARCHAR2(2500 char),
  LAST_UPDATED TIMESTAMP,
  constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)
  references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) segment creation immediate;

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT (
  STEP_EXECUTION_ID NUMBER(19,0) NOT NULL PRIMARY KEY,
  SHORT_CONTEXT VARCHAR2(2500 char) NOT NULL,
  SERIALIZED_CONTEXT CLOB,
  constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
  references BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
) segment creation immediate;

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT (
  JOB_EXECUTION_ID NUMBER(19,0) NOT NULL PRIMARY KEY,
  SHORT_CONTEXT VARCHAR2(2500 char) NOT NULL,
  SERIALIZED_CONTEXT CLOB,
  constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)
  references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) segment creation immediate;

CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ START WITH 0 MINVALUE 0 MAXVALUE 9223372036854775807 NOCYCLE;

CREATE SEQUENCE BATCH_JOB_EXECUTION_SEQ START WITH 0 MINVALUE 0 MAXVALUE 9223372036854775807 NOCYCLE;

CREATE SEQUENCE BATCH_JOB_SEQ START WITH 0 MINVALUE 0 MAXVALUE 9223372036854775807 NOCYCLE;
