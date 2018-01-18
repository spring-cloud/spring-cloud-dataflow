
    create sequence hibernate_sequence start with 1 increment by 1;

    create table APP_REGISTRATION (
        id number(19,0) not null,
        object_version number(19,0),
        default_version number(1,0),
        metadata_uri varchar2(255 char),
        name varchar2(255 char),
        type number(10,0),
        uri varchar2(255 char),
        version varchar2(255 char),
        primary key (id)
    );
