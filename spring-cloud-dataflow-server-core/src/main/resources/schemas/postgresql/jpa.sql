
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
        audit_Action int8,
        audit_data text,
        audit_Operation int8,
        correlation_id varchar(255),
        created_by varchar(255),
        created_On timestamp,
        primary key (id)
    );