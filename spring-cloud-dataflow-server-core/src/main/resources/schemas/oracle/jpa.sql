
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
