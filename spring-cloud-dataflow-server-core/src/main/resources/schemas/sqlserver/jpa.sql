    create table hibernate_sequence (
     next_val bigint
    );

    insert into hibernate_sequence with (rowlock) (next_val) select 1 where not exists (select next_val from hibernate_sequence);

    create table APP_REGISTRATION (
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
