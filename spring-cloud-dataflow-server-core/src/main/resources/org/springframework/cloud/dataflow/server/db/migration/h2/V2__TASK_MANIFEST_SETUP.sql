create table task_execution_metadata (
     id bigint not null,
     task_execution_id bigint not null,
     task_execution_manifest clob,
     primary key (id),
     constraint TASK_METADATA_FK foreign key (TASK_EXECUTION_ID)
     references TASK_EXECUTION(TASK_EXECUTION_ID)
);

create sequence task_execution_metadata_seq start with 1 increment by 1;
