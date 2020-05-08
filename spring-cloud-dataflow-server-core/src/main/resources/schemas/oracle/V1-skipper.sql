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

create table skipper_manifest (
  id number(19,0) not null,
  object_version number(19,0),
  data clob,
  primary key (id)
);

create table skipper_package_file (
  id number(19,0) not null,
  package_bytes blob,
  primary key (id)
);

create table skipper_package_metadata (
  id number(19,0) not null,
  object_version number(19,0),
  api_version varchar2(255 char),
  description clob,
  display_name varchar2(255 char),
  icon_url clob,
  kind varchar2(255 char),
  maintainer varchar2(255 char),
  name varchar2(255 char),
  origin varchar2(255 char),
  package_home_url clob,
  package_source_url clob,
  repository_id number(19,0),
  repository_name varchar2(255 char),
  sha256 varchar2(255 char),
  tags clob,
  version varchar2(255 char),
  packagefile_id number(19,0),
  primary key (id)
);

create table skipper_release (
  id number(19,0) not null,
  object_version number(19,0),
  config_values_string clob,
  name varchar2(255 char),
  package_metadata_id number(19,0),
  pkg_json_string clob,
  platform_name varchar2(255 char),
  repository_id number(19,0),
  version number(10,0) not null,
  info_id number(19,0),
  manifest_id number(19,0),
  primary key (id)
);

create table skipper_repository (
  id number(19,0) not null,
  object_version number(19,0),
  description varchar2(255 char),
  local number(1,0),
  name varchar2(255 char),
  repo_order number(10,0),
  source_url clob,
  url clob,
  primary key (id)
);

create table skipper_status (
  id number(19,0) not null,
  platform_status clob,
  status_code varchar2(255 char),
  primary key (id)
);

create table action (
  id number(19,0) not null,
  name varchar2(255 char),
  spel varchar2(255 char),
  primary key (id)
);

create table deferred_events (
  jpa_repository_state_id number(19,0) not null,
  deferred_events varchar2(255 char)
);

create table guard (
  id number(19,0) not null,
  name varchar2(255 char),
  spel varchar2(255 char),
  primary key (id)
);

create table state (
  id number(19,0) not null,
  initial_state number(1,0) not null,
  kind number(10,0),
  machine_id varchar2(255 char),
  region varchar2(255 char),
  state varchar2(255 char),
  submachine_id varchar2(255 char),
  initial_action_id number(19,0),
  parent_state_id number(19,0),
  primary key (id)
);

create table state_entry_actions (
  jpa_repository_state_id number(19,0) not null,
  entry_actions_id number(19,0) not null,
  primary key (jpa_repository_state_id, entry_actions_id)
);

create table state_exit_actions (
  jpa_repository_state_id number(19,0) not null,
  exit_actions_id number(19,0) not null,
  primary key (jpa_repository_state_id, exit_actions_id)
);

create table state_state_actions (
  jpa_repository_state_id number(19,0) not null,
  state_actions_id number(19,0) not null,
  primary key (jpa_repository_state_id, state_actions_id)
);

create table state_machine (
  machine_id varchar2(255 char) not null,
  state varchar2(255 char),
  state_machine_context blob,
  primary key (machine_id)
);

create table transition (
  id number(19,0) not null,
  event varchar2(255 char),
  kind number(10,0),
  machine_id varchar2(255 char),
  guard_id number(19,0),
  source_id number(19,0),
  target_id number(19,0),
  primary key (id)
);

create table transition_actions (
  jpa_repository_transition_id number(19,0) not null,
  actions_id number(19,0) not null,
  primary key (jpa_repository_transition_id, actions_id)
);

create index idx_pkg_name on skipper_package_metadata (name);

create index idx_rel_name on skipper_release (name);

create index idx_repo_name on skipper_repository (name);

alter table skipper_repository
  add constraint uk_repository unique (name);

alter table deferred_events
  add constraint fk_state_deferred_events
  foreign key (jpa_repository_state_id)
  references state;

alter table skipper_info
  add constraint fk_info_status
  foreign key (status_id)
  references skipper_status;

alter table skipper_package_metadata
  add constraint fk_package_metadata_pfile
  foreign key (packagefile_id)
  references skipper_package_file;

alter table skipper_release
  add constraint fk_release_info
  foreign key (info_id)
  references skipper_info;

alter table skipper_release
  add constraint fk_release_manifest
  foreign key (manifest_id)
  references skipper_manifest;

alter table state
  add constraint fk_state_initial_action
  foreign key (initial_action_id)
  references action;

alter table state
  add constraint fk_state_parent_state
  foreign key (parent_state_id)
  references state;

alter table state_entry_actions
  add constraint fk_state_entry_actions_a
  foreign key (entry_actions_id)
  references action;

alter table state_entry_actions
  add constraint fk_state_entry_actions_s
  foreign key (jpa_repository_state_id)
  references state;

alter table state_exit_actions
  add constraint fk_state_exit_actions_a
  foreign key (exit_actions_id)
  references action;

alter table state_exit_actions
  add constraint fk_state_exit_actions_s
  foreign key (jpa_repository_state_id)
  references state;

alter table state_state_actions
  add constraint fk_state_state_actions_a
  foreign key (state_actions_id)
  references action;

alter table state_state_actions
  add constraint fk_state_state_actions_s
  foreign key (jpa_repository_state_id)
  references state;

alter table transition
  add constraint fk_transition_guard
  foreign key (guard_id)
  references guard;

alter table transition
  add constraint fk_transition_source
  foreign key (source_id)
  references state;

alter table transition
  add constraint fk_transition_target
  foreign key (target_id)
  references state;

alter table transition_actions
  add constraint fk_transition_actions_a
  foreign key (actions_id)
  references action;

alter table transition_actions
  add constraint fk_transition_actions_t
  foreign key (jpa_repository_transition_id)
  references transition;
