
    create sequence hibernate_sequence start 1 increment 1;

    create table APP_REGISTRATION (
        id int8 not null,
        object_version int8,
        default_version boolean,
        metadata_uri varchar(255),
        name varchar(255),
        type int4,
        uri varchar(255),
        version varchar(255),
        primary key (id)
    );
