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

create table skipper_manifest (
  id int8 not null,
  object_version int8,
  data text,
  primary key (id)
);

create table skipper_package_file (
  id int8 not null,
  package_bytes oid,
  primary key (id)
);

create table skipper_package_metadata (
  id int8 not null,
  object_version int8,
  api_version varchar(255),
  description text,
  display_name varchar(255),
  icon_url text,
  kind varchar(255),
  maintainer varchar(255),
  name varchar(255),
  origin varchar(255),
  package_home_url text,
  package_source_url text,
  repository_id int8,
  repository_name varchar(255),
  sha256 varchar(255),
  tags text,
  version varchar(255),
  packagefile_id int8,
  primary key (id)
);

create table skipper_release (
  id int8 not null,
  object_version int8,
  config_values_string text,
  name varchar(255),
  package_metadata_id int8,
  pkg_json_string text,
  platform_name varchar(255),
  repository_id int8,
  version int4 not null,
  info_id int8,
  manifest_id int8,
  primary key (id)
);

create table skipper_repository (
  id int8 not null,
  object_version int8,
  description varchar(255),
  local boolean,
  name varchar(255),
  repo_order int4,
  source_url text,
  url text,
  primary key (id)
);

create table skipper_status (
  id int8 not null,
  platform_status text,
  status_code varchar(255),
  primary key (id)
);

create table action (
  id int8 not null,
  name varchar(255),
  spel varchar(255),
  primary key (id)
);

create table deferred_events (
  jpa_repository_state_id int8 not null,
  deferred_events varchar(255)
);

create table guard (
  id int8 not null,
  name varchar(255),
  spel varchar(255),
  primary key (id)
);

create table state (
  id int8 not null,
  initial_state boolean not null,
  kind int4,
  machine_id varchar(255),
  region varchar(255),
  state varchar(255),
  submachine_id varchar(255),
  initial_action_id int8,
  parent_state_id int8,
  primary key (id)
);

create table state_entry_actions (
  jpa_repository_state_id int8 not null,
  entry_actions_id int8 not null,
  primary key (jpa_repository_state_id, entry_actions_id)
);

create table state_exit_actions (
  jpa_repository_state_id int8 not null,
  exit_actions_id int8 not null,
  primary key (jpa_repository_state_id, exit_actions_id)
);

create table state_state_actions (
  jpa_repository_state_id int8 not null,
  state_actions_id int8 not null,
  primary key (jpa_repository_state_id, state_actions_id)
);

create table state_machine (
  machine_id varchar(255) not null,
  state varchar(255),
  state_machine_context oid,
  primary key (machine_id)
);

create table transition (
  id int8 not null,
  event varchar(255),
  kind int4,
  machine_id varchar(255),
  guard_id int8,
  source_id int8,
  target_id int8,
  primary key (id)
);

create table transition_actions (
  jpa_repository_transition_id int8 not null,
  actions_id int8 not null,
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
  add constraint fk_transitin_actions_a
  foreign key (actions_id)
  references action;

alter table transition_actions
  add constraint fk_transition_actions_t
  foreign key (jpa_repository_transition_id)
  references transition;
