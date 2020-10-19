alter table stream_definitions add description varchar2(255);

alter table stream_definitions add original_definition clob;

alter table task_definitions add description varchar2(255);

CREATE TABLE task_execution_metadata (
  id number(19,0) NOT NULL,
  task_execution_id  number(19,0) NOT NULL,
  task_execution_manifest CLOB,
  primary key (id),
  CONSTRAINT TASK_METADATA_FK FOREIGN KEY (task_execution_id)
  REFERENCES TASK_EXECUTION(TASK_EXECUTION_ID)
);

CREATE SEQUENCE task_execution_metadata_seq START WITH 0 MINVALUE 0 MAXVALUE 9223372036854775807 NOCACHE NOCYCLE;
