
    create sequence hibernate_sequence start with 1 increment by 1;

    create table APP_REGISTRATION (
       id number(19,0) not null,
        object_Version number(19,0),
        default_Version number(1,0),
        metadata_Uri clob,
        name varchar2(255 char),
        type number(10,0),
        uri clob,
        version varchar2(255 char),
        primary key (id)
    );

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
