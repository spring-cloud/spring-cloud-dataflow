alter table stream_definitions add description varchar(255);

alter table stream_definitions add original_definition varchar(MAX);

alter table task_definitions add description varchar(255);

CREATE TABLE task_execution_metadata (
  id BIGINT NOT NULL PRIMARY KEY,
  task_execution_id BIGINT NOT NULL,
  task_execution_manifest text,
  CONSTRAINT TASK_METADATA_FK FOREIGN KEY (task_execution_id)
  REFERENCES TASK_EXECUTION(TASK_EXECUTION_ID)
);

CREATE TABLE task_execution_metadata_seq (
  ID BIGINT IDENTITY
);
