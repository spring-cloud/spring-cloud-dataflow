
   create table APP_REGISTRATION (
        id bigint not null,
        object_version bigint,
        default_version bit,
        metadata_uri varchar(255),
        name varchar(255),
        type integer,
        uri varchar(255),
        version varchar(255),
        primary key (id)
    );

    create table hibernate_sequence (
        next_val bigint
    );

    insert into hibernate_sequence(next_val) select * from (select 1) as tmp where not exists ( select next_val from hibernate_sequence) limit 1;
