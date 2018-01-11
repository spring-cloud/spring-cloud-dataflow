
    alter table jpa_repository_state 
        drop constraint FKl7uw0stk3ta9k0i64ve8viv8b;

    alter table jpa_repository_state 
        drop constraint FK85uu4no99eoivtd6elb2rp9dg;

    alter table jpa_repository_state_deferred_events 
        drop constraint FKoodyqp0kxbmkjtmskj9m79h73;

    alter table jpa_repository_state_entry_actions 
        drop constraint FKp9g3iq1ngku1imrsf5dnmmnww;

    alter table jpa_repository_state_entry_actions 
        drop constraint FKalgdctnelpb0xriggiufbfcd5;

    alter table jpa_repository_state_exit_actions 
        drop constraint FKlhwv3oxyp5hprnlvs56gnyxdh;

    alter table jpa_repository_state_exit_actions 
        drop constraint FKnuahuplj5vp27hxqdult5y2su;

    alter table jpa_repository_state_state_actions 
        drop constraint FK8wgwopqvhfnb1xe5sqf2213pw;

    alter table jpa_repository_state_state_actions 
        drop constraint FKqqpkvnpqb8madraq2l57niagx;

    alter table jpa_repository_transition 
        drop constraint FKrs9l0ayy1i7t5pjnixkohgrlm;

    alter table jpa_repository_transition 
        drop constraint FK4dahkov2dttpljo5mfcid5gxh;

    alter table jpa_repository_transition 
        drop constraint FK6jymhcao9w1786ldrnbdlsacu;

    alter table jpa_repository_transition_actions 
        drop constraint FKhwdl9g5s5htj2jcb1xrkq6wpw;

    alter table jpa_repository_transition_actions 
        drop constraint FK6287nce3o7soy1bjdyi04heih;

    alter table skipper_info 
        drop constraint fk_info_status;

    alter table skipper_release 
        drop constraint fk_release_info;

    drop table if exists jpa_repository_action cascade;

    drop table if exists jpa_repository_guard cascade;

    drop table if exists jpa_repository_state cascade;

    drop table if exists jpa_repository_state_deferred_events cascade;

    drop table if exists jpa_repository_state_entry_actions cascade;

    drop table if exists jpa_repository_state_exit_actions cascade;

    drop table if exists jpa_repository_state_state_actions cascade;

    drop table if exists jpa_repository_state_machine cascade;

    drop table if exists jpa_repository_transition cascade;

    drop table if exists jpa_repository_transition_actions cascade;

    drop table if exists skipper_app_deployer_data cascade;

    drop table if exists skipper_info cascade;

    drop table if exists skipper_package_metadata cascade;

    drop table if exists skipper_release cascade;

    drop table if exists skipper_repository cascade;

    drop table if exists skipper_status cascade;

    drop sequence hibernate_sequence;

    create sequence hibernate_sequence start 1 increment 1;

    create table jpa_repository_action (
        id int8 not null,
        name varchar(255),
        spel varchar(255),
        primary key (id)
    );

    create table jpa_repository_guard (
        id int8 not null,
        name varchar(255),
        spel varchar(255),
        primary key (id)
    );

    create table jpa_repository_state (
        id int8 not null,
        initial boolean not null,
        kind int4,
        machine_id varchar(255),
        region varchar(255),
        state varchar(255),
        submachine_id varchar(255),
        initial_action_id int8,
        parent_state_id int8,
        primary key (id)
    );

    create table jpa_repository_state_deferred_events (
        jpa_repository_state_id int8 not null,
        deferred_events varchar(255)
    );

    create table jpa_repository_state_entry_actions (
        jpa_repository_state_id int8 not null,
        entry_actions_id int8 not null,
        primary key (jpa_repository_state_id, entry_actions_id)
    );

    create table jpa_repository_state_exit_actions (
        jpa_repository_state_id int8 not null,
        exit_actions_id int8 not null,
        primary key (jpa_repository_state_id, exit_actions_id)
    );

    create table jpa_repository_state_state_actions (
        jpa_repository_state_id int8 not null,
        state_actions_id int8 not null,
        primary key (jpa_repository_state_id, state_actions_id)
    );

    create table jpa_repository_state_machine (
        machine_id varchar(255) not null,
        state varchar(255),
        state_machine_context oid,
        primary key (machine_id)
    );

    create table jpa_repository_transition (
        id int8 not null,
        event varchar(255),
        kind int4,
        machine_id varchar(255),
        guard_id int8,
        source_id int8,
        target_id int8,
        primary key (id)
    );

    create table jpa_repository_transition_actions (
        jpa_repository_transition_id int8 not null,
        actions_id int8 not null,
        primary key (jpa_repository_transition_id, actions_id)
    );

    create table skipper_app_deployer_data (
        id int8 not null,
        object_version int8,
        deployment_data text,
        release_name varchar(255),
        release_version int4,
        primary key (id)
    );

    create table skipper_info (
        id int8 not null,
        object_version int8,
        deleted timestamp,
        description varchar(255),
        first_deployed timestamp,
        last_deployed timestamp,
        status_id int8,
        primary key (id)
    );

    create table skipper_package_metadata (
        id int8 not null,
        object_version int8,
        api_version varchar(255),
        description varchar(255),
        display_name varchar(255),
        icon_url varchar(255),
        kind varchar(255),
        maintainer varchar(255),
        name varchar(255),
        origin varchar(255),
        package_file oid,
        package_home_url varchar(255),
        package_source_url varchar(255),
        repository_id int8,
        sha256 varchar(255),
        tags varchar(255),
        version varchar(255),
        primary key (id)
    );

    create table skipper_release (
        id int8 not null,
        object_version int8,
        config_values_string text,
        manifest text,
        name varchar(255),
        pkg_json_string text,
        platform_name varchar(255),
        version int4 not null,
        info_id int8,
        primary key (id)
    );

    create table skipper_repository (
        id int8 not null,
        object_version int8,
        description varchar(255),
        local boolean,
        name varchar(255),
        repo_order int4,
        source_url varchar(255),
        url varchar(255),
        primary key (id)
    );

    create table skipper_status (
        id int8 not null,
        platform_status text,
        status_code varchar(255),
        primary key (id)
    );

    alter table skipper_repository 
        add constraint uk_repository unique (name);

    alter table jpa_repository_state 
        add constraint FKl7uw0stk3ta9k0i64ve8viv8b 
        foreign key (initial_action_id) 
        references jpa_repository_action;

    alter table jpa_repository_state 
        add constraint FK85uu4no99eoivtd6elb2rp9dg 
        foreign key (parent_state_id) 
        references jpa_repository_state;

    alter table jpa_repository_state_deferred_events 
        add constraint FKoodyqp0kxbmkjtmskj9m79h73 
        foreign key (jpa_repository_state_id) 
        references jpa_repository_state;

    alter table jpa_repository_state_entry_actions 
        add constraint FKp9g3iq1ngku1imrsf5dnmmnww 
        foreign key (entry_actions_id) 
        references jpa_repository_action;

    alter table jpa_repository_state_entry_actions 
        add constraint FKalgdctnelpb0xriggiufbfcd5 
        foreign key (jpa_repository_state_id) 
        references jpa_repository_state;

    alter table jpa_repository_state_exit_actions 
        add constraint FKlhwv3oxyp5hprnlvs56gnyxdh 
        foreign key (exit_actions_id) 
        references jpa_repository_action;

    alter table jpa_repository_state_exit_actions 
        add constraint FKnuahuplj5vp27hxqdult5y2su 
        foreign key (jpa_repository_state_id) 
        references jpa_repository_state;

    alter table jpa_repository_state_state_actions 
        add constraint FK8wgwopqvhfnb1xe5sqf2213pw 
        foreign key (state_actions_id) 
        references jpa_repository_action;

    alter table jpa_repository_state_state_actions 
        add constraint FKqqpkvnpqb8madraq2l57niagx 
        foreign key (jpa_repository_state_id) 
        references jpa_repository_state;

    alter table jpa_repository_transition 
        add constraint FKrs9l0ayy1i7t5pjnixkohgrlm 
        foreign key (guard_id) 
        references jpa_repository_guard;

    alter table jpa_repository_transition 
        add constraint FK4dahkov2dttpljo5mfcid5gxh 
        foreign key (source_id) 
        references jpa_repository_state;

    alter table jpa_repository_transition 
        add constraint FK6jymhcao9w1786ldrnbdlsacu 
        foreign key (target_id) 
        references jpa_repository_state;

    alter table jpa_repository_transition_actions 
        add constraint FKhwdl9g5s5htj2jcb1xrkq6wpw 
        foreign key (actions_id) 
        references jpa_repository_action;

    alter table jpa_repository_transition_actions 
        add constraint FK6287nce3o7soy1bjdyi04heih 
        foreign key (jpa_repository_transition_id) 
        references jpa_repository_transition;

    alter table skipper_info 
        add constraint fk_info_status 
        foreign key (status_id) 
        references skipper_status;

    alter table skipper_release 
        add constraint fk_release_info 
        foreign key (info_id) 
        references skipper_info;
