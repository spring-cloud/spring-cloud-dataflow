
    create table APP_REGISTRATION (
       id bigint not null,
        object_Version bigint,
        default_Version bit,
        metadata_Uri longtext,
        name varchar(255),
        type integer,
        uri longtext,
        version varchar(255),
        primary key (id)
    );

    create table AUDIT_RECORDS (
       id bigint not null,
        audit_Action bigint NOT NULL,
        audit_data longtext,
        audit_Operation bigint NOT NULL,
        correlation_id varchar(255),
        created_by varchar(255),
        created_On datetime,
        primary key (id)
    );

    CREATE INDEX AUDIT_RECORDS_AUDIT_ACTION_IDX ON AUDIT_RECORDS (audit_Action) ;
    CREATE INDEX AUDIT_RECORDS_AUDIT_OPERATION_IDX ON AUDIT_RECORDS (audit_Operation) ;
    CREATE INDEX AUDIT_RECORDS_CORRELATION_ID_IDX ON AUDIT_RECORDS (correlation_id) ;
    CREATE INDEX AUDIT_RECORDS_CREATED_ON_IDX ON AUDIT_RECORDS (created_On) ;

    create table hibernate_sequence (
        next_val bigint
    );

    insert into hibernate_sequence(next_val) select * from (select 1) as tmp where not exists ( select next_val from hibernate_sequence) limit 1;
