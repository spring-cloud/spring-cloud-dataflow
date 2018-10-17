
    create sequence hibernate_sequence start 1 increment 1;

    create table APP_REGISTRATION (
       id int8 not null,
        object_Version int8,
        default_Version boolean,
        metadata_Uri text,
        name varchar(255),
        type int4,
        uri text,
        version varchar(255),
        primary key (id)
    );

    create table AUDIT_RECORDS (
       id int8 not null,
        audit_Action int8 not null,
        audit_data text,
        audit_Operation int8 not null,
        correlation_id varchar(255),
        created_by varchar(255),
        created_On timestamp,
        primary key (id)
    );

    CREATE INDEX IF NOT EXISTS AUDIT_RECORDS_AUDIT_ACTION_IDX ON AUDIT_RECORDS (audit_Action) ;
    CREATE INDEX IF NOT EXISTS AUDIT_RECORDS_AUDIT_OPERATION_IDX ON AUDIT_RECORDS (audit_Operation) ;
    CREATE INDEX IF NOT EXISTS AUDIT_RECORDS_CORRELATION_ID_IDX ON AUDIT_RECORDS (correlation_id) ;
    CREATE INDEX IF NOT EXISTS AUDIT_RECORDS_CREATED_ON_IDX ON AUDIT_RECORDS (created_On) ;
