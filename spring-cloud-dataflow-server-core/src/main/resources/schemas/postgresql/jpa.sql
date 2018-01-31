
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
