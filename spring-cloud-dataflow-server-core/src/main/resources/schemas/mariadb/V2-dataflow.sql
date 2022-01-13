alter table stream_definitions add description varchar(255);

alter table stream_definitions add original_definition longtext;

alter table task_definitions add description varchar(255);

CREATE TABLE task_execution_metadata (
  id BIGINT NOT NULL,
  task_execution_id BIGINT NOT NULL,
  task_execution_manifest LONGTEXT,
  primary key (id),
  CONSTRAINT TASK_METADATA_FK FOREIGN KEY (task_execution_id)
  REFERENCES TASK_EXECUTION(TASK_EXECUTION_ID)
);

CREATE TABLE task_execution_metadata_seq (
  ID BIGINT NOT NULL,
  UNIQUE_KEY CHAR(1) NOT NULL,
  constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)
);

INSERT INTO task_execution_metadata_seq (ID, UNIQUE_KEY) select * from (select 0 as ID, '0' as UNIQUE_KEY) as tmp where not exists(select * from task_execution_metadata_seq);
