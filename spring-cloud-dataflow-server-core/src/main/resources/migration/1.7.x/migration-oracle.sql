    create table AUDIT_RECORDS (
       id number(19,0) not null,
        audit_Action number(19,0),
        audit_data clob,
        audit_Operation number(19,0),
        correlation_id varchar2(255 char),
        created_by varchar2(255 char),
        created_On timestamp,
        primary key (id)
    );

    alter table stream_definitions add description varchar(255)
    alter table task_definitions add description varchar(255)
