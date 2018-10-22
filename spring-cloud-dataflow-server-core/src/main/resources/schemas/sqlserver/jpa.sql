    create table hibernate_sequence (
     next_val bigint
    );

    insert into hibernate_sequence with (rowlock) (next_val) select 1 where not exists (select next_val from hibernate_sequence);

    create table APP_REGISTRATION (
       id bigint not null,
        object_Version bigint,
        default_Version bit,
        metadata_Uri varchar(MAX),
        name varchar(255),
        type int,
        uri varchar(MAX),
        version varchar(255),
        primary key (id)
    );

    create table AUDIT_RECORDS (
       id bigint not null,
        audit_Action bigint not null,
        audit_data varchar(MAX),
        audit_Operation bigint not null,
        correlation_id varchar(255),
        created_by varchar(255),
        created_On datetime2,
        primary key (id)
    );

    CREATE INDEX AUDIT_RECORDS_AUDIT_ACTION_IDX ON AUDIT_RECORDS (audit_Action) ;
    CREATE INDEX AUDIT_RECORDS_AUDIT_OPERATION_IDX ON AUDIT_RECORDS (audit_Operation) ;
    CREATE INDEX AUDIT_RECORDS_CORRELATION_ID_IDX ON AUDIT_RECORDS (correlation_id) ;
    CREATE INDEX AUDIT_RECORDS_CREATED_ON_IDX ON AUDIT_RECORDS (created_On) ;
