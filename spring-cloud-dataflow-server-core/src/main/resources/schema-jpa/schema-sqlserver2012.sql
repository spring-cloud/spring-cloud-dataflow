
    drop table app_registration;

    drop sequence hibernate_sequence;

    create sequence hibernate_sequence start with 1 increment by 1;

    create table app_registration (
        id bigint not null,
        object_version bigint,
        default_version bit,
        metadata_uri varchar(255),
        name varchar(255),
        type int,
        uri varchar(255),
        version varchar(255),
        primary key (id)
    );
