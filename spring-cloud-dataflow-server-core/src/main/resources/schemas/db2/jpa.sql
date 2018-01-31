    create sequence hibernate_sequence start with 1 increment by 1;

    create table APP_REGISTRATION (
       id bigint not null,
        object_Version bigint,
        default_Version smallint,
        metadata_Uri clob(16384),
        name varchar(255),
        type integer,
        uri clob(16384),
        version varchar(255),
        primary key (id)
    );
