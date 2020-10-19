alter table stream_definitions add description varchar(255);

alter table stream_definitions add original_definition clob(255);

alter table task_definitions add description varchar(255);

CREATE TABLE task_execution_metadata (
  id BIGINT NOT NULL PRIMARY KEY,
  task_execution_id BIGINT NOT NULL,
  task_execution_manifest CLOB,
  CONSTRAINT TASK_METADATA_FK FOREIGN KEY (task_execution_id)
  REFERENCES TASK_EXECUTION(TASK_EXECUTION_ID)
);

CREATE SEQUENCE task_execution_metadata_seq AS BIGINT MAXVALUE 9223372036854775807 NO CYCLE;
