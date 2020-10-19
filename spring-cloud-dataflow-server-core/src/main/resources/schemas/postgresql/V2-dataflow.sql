alter table stream_definitions add column description varchar(255);

alter table stream_definitions add column original_definition text;

alter table task_definitions add column description varchar(255);

CREATE TABLE task_execution_metadata (
  id int8 NOT NULL,
  task_execution_id int8 NOT NULL,
  task_execution_manifest TEXT,
  primary key (id),
  CONSTRAINT TASK_METADATA_FK FOREIGN KEY (task_execution_id)
  REFERENCES TASK_EXECUTION(TASK_EXECUTION_ID)
);

CREATE SEQUENCE task_execution_metadata_seq MAXVALUE 9223372036854775807 NO CYCLE;
