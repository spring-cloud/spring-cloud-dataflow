
    create table APP_REGISTRATION (
       id bigint not null,
        object_Version bigint,
        default_Version bit,
        metadata_Uri longtext,
        name varchar(255),
        type integer,
        uri longtext,
        version varchar(255),
        primary key (id)
    );

    create table hibernate_sequence (
        next_val bigint
    );

    insert into hibernate_sequence(next_val) select * from (select 1) as tmp where not exists ( select next_val from hibernate_sequence) limit 1;
