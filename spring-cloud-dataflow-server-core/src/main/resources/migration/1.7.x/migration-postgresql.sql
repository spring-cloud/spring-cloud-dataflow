    create table AUDIT_RECORDS (
       id int8 not null,
        audit_Action int8,
        audit_data text,
        audit_Operation int8,
        correlation_id varchar(255),
        created_by varchar(255),
        created_On timestamp,
        primary key (id)
    );