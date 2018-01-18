   create sequence hibernate_sequence start with 1 increment by 1;

    create table APP_REGISTRATION (
        id bigint not null,
        object_version bigint,
        default_version boolean,
        metadata_uri varchar(255),
        name varchar(255),
        type integer,
        uri varchar(255),
        version varchar(255),
        primary key (id)
    );
