create table if not exists hibernate_sequence (
  next_val bigint
);

insert into hibernate_sequence (next_val)
  select * from (select 1 as next_val) as temp
  where not exists(select * from hibernate_sequence);

create table skipper_app_deployer_data (
  id bigint not null,
  object_version bigint,
  deployment_data longtext,
  release_name varchar(255),
  release_version integer,
  primary key (id)
);

create table skipper_info (
  id bigint not null,
  object_version bigint,
  deleted datetime,
  description varchar(255),
  first_deployed datetime,
  last_deployed datetime,
  status_id bigint,
  primary key (id)
);

create table skipper_manifest (
  id bigint not null,
  object_version bigint,
  data longtext,
  primary key (id)
);

create table skipper_package_file (
  id bigint not null,
  package_bytes longblob,
  primary key (id)
);

create table skipper_package_metadata (
  id bigint not null,
  object_version bigint,
  api_version varchar(255),
  description longtext,
  display_name varchar(255),
  icon_url longtext,
  kind varchar(255),
  maintainer varchar(255),
  name varchar(255),
  origin varchar(255),
  package_home_url longtext,
  package_source_url longtext,
  repository_id bigint,
  repository_name varchar(255),
  sha256 varchar(255),
  tags longtext,
  version varchar(255),
  packagefile_id bigint,
  primary key (id)
);

create table skipper_release (
  id bigint not null,
  object_version bigint,
  config_values_string longtext,
  name varchar(255),
  package_metadata_id bigint,
  pkg_json_string longtext,
  platform_name varchar(255),
  repository_id bigint,
  version integer not null,
  info_id bigint,
  manifest_id bigint,
  primary key (id)
);

create table skipper_repository (
  id bigint not null,
  object_version bigint,
  description varchar(255),
  local bit,
  name varchar(255),
  repo_order integer,
  source_url longtext,
  url longtext,
  primary key (id)
);

create table skipper_status (
  id bigint not null,
  platform_status longtext,
  status_code varchar(255),
  primary key (id)
);

create table action (
  id bigint not null,
  name varchar(255),
  spel varchar(255),
  primary key (id)
);

create table deferred_events (
  jpa_repository_state_id bigint not null,
  deferred_events varchar(255)
);

create table guard (
  id bigint not null,
  name varchar(255),
  spel varchar(255),
  primary key (id)
);

create table state (
  id bigint not null,
  initial_state bit not null,
  kind integer,
  machine_id varchar(255),
  region varchar(255),
  state varchar(255),
  submachine_id varchar(255),
  initial_action_id bigint,
  parent_state_id bigint,
  primary key (id)
);

create table state_entry_actions (
  jpa_repository_state_id bigint not null,
  entry_actions_id bigint not null,
  primary key (jpa_repository_state_id, entry_actions_id)
);

create table state_exit_actions (
  jpa_repository_state_id bigint not null,
  exit_actions_id bigint not null,
  primary key (jpa_repository_state_id, exit_actions_id)
);

create table state_state_actions (
  jpa_repository_state_id bigint not null,
  state_actions_id bigint not null,
  primary key (jpa_repository_state_id, state_actions_id)
);

create table state_machine (
  machine_id varchar(255) not null,
  state varchar(255),
  state_machine_context longblob,
  primary key (machine_id)
);

create table transition (
  id bigint not null,
  event varchar(255),
  kind integer,
  machine_id varchar(255),
  guard_id bigint,
  source_id bigint,
  target_id bigint,
  primary key (id)
);

create table transition_actions (
  jpa_repository_transition_id bigint not null,
  actions_id bigint not null,
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
  references state (id);

alter table skipper_info
  add constraint fk_info_status
  foreign key (status_id)
  references skipper_status (id);

alter table skipper_package_metadata
  add constraint fk_package_metadata_pfile
  foreign key (packagefile_id)
  references skipper_package_file (id);

alter table skipper_release
  add constraint fk_release_info
  foreign key (info_id)
  references skipper_info (id);

alter table skipper_release
  add constraint fk_release_manifest
  foreign key (manifest_id)
  references skipper_manifest (id);

alter table state
  add constraint fk_state_initial_action
  foreign key (initial_action_id)
  references action (id);

alter table state
  add constraint fk_state_parent_state
  foreign key (parent_state_id)
  references state (id);

alter table state_entry_actions
  add constraint fk_state_entry_actions_a
  foreign key (entry_actions_id)
  references action (id);

alter table state_entry_actions
  add constraint fk_state_entry_actions_s
  foreign key (jpa_repository_state_id)
  references state (id);

alter table state_exit_actions
  add constraint fk_state_exit_actions_a
  foreign key (exit_actions_id)
  references action (id);

alter table state_exit_actions
  add constraint fk_state_exit_actions_s
  foreign key (jpa_repository_state_id)
  references state (id);

alter table state_state_actions
  add constraint fk_state_state_actions_a
  foreign key (state_actions_id)
  references action (id);

alter table state_state_actions
  add constraint fk_state_state_actions_s
  foreign key (jpa_repository_state_id)
  references state (id);

alter table transition
  add constraint fk_transition_guard
  foreign key (guard_id)
  references guard (id);

alter table transition
  add constraint fk_transition_source
  foreign key (source_id)
  references state (id);

alter table transition
  add constraint fk_transition_target
  foreign key (target_id)
  references state (id);

alter table transition_actions
  add constraint fk_transition_actions_a
  foreign key (actions_id)
  references action (id);

alter table transition_actions
  add constraint fk_transition_actions_t
  foreign key (jpa_repository_transition_id)
  references transition (id);
