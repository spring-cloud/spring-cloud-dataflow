    create table hibernate_sequence (
     next_val bigint
    );

    insert into hibernate_sequence with (rowlock) (next_val) select 1 where not exists (select next_val from hibernate_sequence);

    create table APP_REGISTRATION (
       id bigint not null,
        object_Version bigint,
        default_Version bit,
        metadata_Uri varchar(MAX),
        name varchar(255),
        type int,
        uri varchar(MAX),
        version varchar(255),
        primary key (id)
    );

