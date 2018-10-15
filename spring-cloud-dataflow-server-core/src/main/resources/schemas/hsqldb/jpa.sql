
    create sequence hibernate_sequence start with 1 increment by 1;

   create table APP_REGISTRATION (
       id bigint not null,
        object_Version bigint,
        default_Version boolean,
        metadata_Uri clob(16384),
        name varchar(255),
        type integer,
        uri clob(16384),
        version varchar(255),
        primary key (id)
    );

    create table AUDIT_RECORDS (
       id bigint not null,
        audit_Action bigint,
        audit_data longvarchar,
        audit_Operation bigint,
        correlation_id varchar(255),
        created_by varchar(255),
        created_On timestamp,
        primary key (id)
    );
