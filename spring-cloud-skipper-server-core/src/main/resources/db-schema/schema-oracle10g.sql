
    drop table jpa_repository_action cascade constraints;

    drop table jpa_repository_guard cascade constraints;

    drop table jpa_repository_state cascade constraints;

    drop table jpa_repository_state_deferred_events cascade constraints;

    drop table jpa_repository_state_entry_actions cascade constraints;

    drop table jpa_repository_state_exit_actions cascade constraints;

    drop table jpa_repository_state_state_actions cascade constraints;

    drop table jpa_repository_state_machine cascade constraints;

    drop table jpa_repository_transition cascade constraints;

    drop table jpa_repository_transition_actions cascade constraints;

    drop table skipper_app_deployer_data cascade constraints;

    drop table skipper_info cascade constraints;

    drop table skipper_package_metadata cascade constraints;

    drop table skipper_release cascade constraints;

    drop table skipper_repository cascade constraints;

    drop table skipper_status cascade constraints;

    drop sequence hibernate_sequence;

    create sequence hibernate_sequence start with 1 increment by 1;

    create table jpa_repository_action (
        id number(19,0) not null,
        name varchar2(255 char),
        spel varchar2(255 char),
        primary key (id)
    );

    create table jpa_repository_guard (
        id number(19,0) not null,
        name varchar2(255 char),
        spel varchar2(255 char),
        primary key (id)
    );

    create table jpa_repository_state (
        id number(19,0) not null,
        initial number(1,0) not null,
        kind number(10,0),
        machine_id varchar2(255 char),
        region varchar2(255 char),
        state varchar2(255 char),
        submachine_id varchar2(255 char),
        initial_action_id number(19,0),
        parent_state_id number(19,0),
        primary key (id)
    );

    create table jpa_repository_state_deferred_events (
        jpa_repository_state_id number(19,0) not null,
        deferred_events varchar2(255 char)
    );

    create table jpa_repository_state_entry_actions (
        jpa_repository_state_id number(19,0) not null,
        entry_actions_id number(19,0) not null,
        primary key (jpa_repository_state_id, entry_actions_id)
    );

    create table jpa_repository_state_exit_actions (
        jpa_repository_state_id number(19,0) not null,
        exit_actions_id number(19,0) not null,
        primary key (jpa_repository_state_id, exit_actions_id)
    );

    create table jpa_repository_state_state_actions (
        jpa_repository_state_id number(19,0) not null,
        state_actions_id number(19,0) not null,
        primary key (jpa_repository_state_id, state_actions_id)
    );

    create table jpa_repository_state_machine (
        machine_id varchar2(255 char) not null,
        state varchar2(255 char),
        state_machine_context blob,
        primary key (machine_id)
    );

    create table jpa_repository_transition (
        id number(19,0) not null,
        event varchar2(255 char),
        kind number(10,0),
        machine_id varchar2(255 char),
        guard_id number(19,0),
        source_id number(19,0),
        target_id number(19,0),
        primary key (id)
    );

    create table jpa_repository_transition_actions (
        jpa_repository_transition_id number(19,0) not null,
        actions_id number(19,0) not null,
        primary key (jpa_repository_transition_id, actions_id)
    );

    create table skipper_app_deployer_data (
        id number(19,0) not null,
        object_version number(19,0),
        deployment_data clob,
        release_name varchar2(255 char),
        release_version number(10,0),
        primary key (id)
    );

    create table skipper_info (
        id number(19,0) not null,
        object_version number(19,0),
        deleted timestamp,
        description varchar2(255 char),
        first_deployed timestamp,
        last_deployed timestamp,
        status_id number(19,0),
        primary key (id)
    );

    create table skipper_package_metadata (
        id number(19,0) not null,
        object_version number(19,0),
        api_version varchar2(255 char),
        description varchar2(255 char),
        display_name varchar2(255 char),
        icon_url varchar2(255 char),
        kind varchar2(255 char),
        maintainer varchar2(255 char),
        name varchar2(255 char),
        origin varchar2(255 char),
        package_file blob,
        package_home_url varchar2(255 char),
        package_source_url varchar2(255 char),
        repository_id number(19,0),
        sha256 varchar2(255 char),
        tags varchar2(255 char),
        version varchar2(255 char),
        primary key (id)
    );

    create table skipper_release (
        id number(19,0) not null,
        object_version number(19,0),
        config_values_string clob,
        manifest clob,
        name varchar2(255 char),
        pkg_json_string clob,
        platform_name varchar2(255 char),
        version number(10,0) not null,
        info_id number(19,0),
        primary key (id)
    );

    create table skipper_repository (
        id number(19,0) not null,
        object_version number(19,0),
        description varchar2(255 char),
        local number(1,0),
        name varchar2(255 char),
        repo_order number(10,0),
        source_url varchar2(255 char),
        url varchar2(255 char),
        primary key (id)
    );

    create table skipper_status (
        id number(19,0) not null,
        platform_status clob,
        status_code varchar2(255 char),
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
