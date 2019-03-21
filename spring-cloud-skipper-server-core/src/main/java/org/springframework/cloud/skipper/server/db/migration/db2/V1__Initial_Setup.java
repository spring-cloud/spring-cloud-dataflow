/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.skipper.server.db.migration.db2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.cloud.skipper.server.db.migration.AbstractInitialSetupMigration;
import org.springframework.cloud.skipper.server.db.migration.SqlCommand;

public class V1__Initial_Setup extends AbstractInitialSetupMigration {

	public final static String CREATE_SKIPPER_APP_DEPLOYER_DATA_TABLE =
			"create table skipper_app_deployer_data (\n" +
			"    id bigint not null,\n" +
			"    object_version bigint,\n" +
			"    deployment_data clob(4096),\n" +
			"    release_name varchar(255),\n" +
			"    release_version integer,\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_INFO_TABLE =
			"create table skipper_info (\n" +
			"    id bigint not null,\n" +
			"    object_version bigint,\n" +
			"    deleted timestamp,\n" +
			"    description varchar(255),\n" +
			"    first_deployed timestamp,\n" +
			"    last_deployed timestamp,\n" +
			"    status_id bigint,\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_MANIFEST_TABLE =
			"create table skipper_manifest (\n" +
			"    id bigint not null,\n" +
			"    object_version bigint,\n" +
			"    data clob(65536),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_PACKAGE_FILE_TABLE =
			"create table skipper_package_file (\n" +
			"    id bigint not null,\n" +
			"    package_bytes blob(1048576),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_PACKAGE_METADATA_TABLE =
			"create table skipper_package_metadata (\n" +
			"    id bigint not null,\n" +
			"    object_version bigint,\n" +
			"    api_version varchar(255),\n" +
			"    description clob(65536),\n" +
			"    display_name varchar(255),\n" +
			"    icon_url clob(16384),\n" +
			"    kind varchar(255),\n" +
			"    maintainer varchar(255),\n" +
			"    name varchar(255),\n" +
			"    origin varchar(255),\n" +
			"    package_home_url clob(16384),\n" +
			"    package_source_url clob(16384),\n" +
			"    repository_id bigint,\n" +
			"    repository_name varchar(255),\n" +
			"    sha256 varchar(255),\n" +
			"    tags clob(16384),\n" +
			"    version varchar(255),\n" +
			"    packagefile_id bigint,\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_RELEASE_TABLE =
			"create table skipper_release (\n" +
			"    id bigint not null,\n" +
			"    object_version bigint,\n" +
			"    config_values_string clob(16384),\n" +
			"    name varchar(255),\n" +
			"    package_metadata_id bigint,\n" +
			"    pkg_json_string clob(65536),\n" +
			"    platform_name varchar(255),\n" +
			"    repository_id bigint,\n" +
			"    version integer not null,\n" +
			"    info_id bigint,\n" +
			"    manifest_id bigint,\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_REPOSITORY_TABLE =
			"create table skipper_repository (\n" +
			"    id bigint not null,\n" +
			"    object_version bigint,\n" +
			"    description varchar(255),\n" +
			"    local smallint,\n" +
			"    name varchar(255),\n" +
			"    repo_order integer,\n" +
			"    source_url clob(16384),\n" +
			"    url clob(16384),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_STATUS_TABLE =
			"create table skipper_status (\n" +
			"    id bigint not null,\n" +
			"    platform_status clob(4096),\n" +
			"    status_code varchar(255),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_ACTION_TABLE =
			"create table action (\n" +
			"    id bigint not null,\n" +
			"    name varchar(255),\n" +
			"    spel varchar(255),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_DEFERRED_EVENTS_TABLE =
			"create table deferred_events (\n" +
			"    jpa_repository_state_id bigint not null,\n" +
			"    deferred_events varchar(255)\n" +
			")";

	public final static String CREATE_STATEMACHINE_GUARD_TABLE =
			"create table guard (\n" +
			"    id bigint not null,\n" +
			"    name varchar(255),\n" +
			"    spel varchar(255),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_STATE_TABLE =
			"create table state (\n" +
			"    id bigint not null,\n" +
			"    initial_state smallint not null,\n" +
			"    kind integer,\n" +
			"    machine_id varchar(255),\n" +
			"    region varchar(255),\n" +
			"    state varchar(255),\n" +
			"    submachine_id varchar(255),\n" +
			"    initial_action_id bigint,\n" +
			"    parent_state_id bigint,\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_STATE_ENTRY_ACTIONS_TABLE =
			"create table state_entry_actions (\n" +
			"    jpa_repository_state_id bigint not null,\n" +
			"    entry_actions_id bigint not null,\n" +
			"    primary key (jpa_repository_state_id, entry_actions_id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_STATE_EXIT_ACTIONS_TABLE =
			"create table state_exit_actions (\n" +
			"    jpa_repository_state_id bigint not null,\n" +
			"    exit_actions_id bigint not null,\n" +
			"    primary key (jpa_repository_state_id, exit_actions_id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_STATE_STATE_ACTIONS_TABLE =
			"create table state_state_actions (\n" +
			"    jpa_repository_state_id bigint not null,\n" +
			"    state_actions_id bigint not null,\n" +
			"    primary key (jpa_repository_state_id, state_actions_id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_STATE_MACHINE_TABLE =
			"create table state_machine (\n" +
			"    machine_id varchar(255) not null,\n" +
			"    state varchar(255),\n" +
			"    state_machine_context blob(1048576),\n" +
			"    primary key (machine_id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_TRANSITION_TABLE =
			"create table transition (\n" +
			"    id bigint not null,\n" +
			"    event varchar(255),\n" +
			"    kind integer,\n" +
			"    machine_id varchar(255),\n" +
			"    guard_id bigint,\n" +
			"    source_id bigint,\n" +
			"    target_id bigint,\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_TRANSITION_ACTIONS_TABLE =
			"create table transition_actions (\n" +
			"    jpa_repository_transition_id bigint not null,\n" +
			"    actions_id bigint not null,\n" +
			"    primary key (jpa_repository_transition_id, actions_id)\n" +
			")";

	public final static String CREATE_INX_PKG_NAME_INDEX =
			"create index idx_pkg_name on skipper_package_metadata (name)";

	public final static String CREATE_INX_REL_NAME_INDEX =
			"create index idx_rel_name on skipper_release (name)";

	public final static String CREATE_INX_REPO_NAME_INDEX =
			"create index idx_repo_name on skipper_repository (name)";

	public final static String CREATE_INX_UK_INDEX =
			"create unique index uk_repository on skipper_repository (name)";

	public final static String ADD_DEFERRED_EVENTS_FK_STATE_DEFERRED_EVENTS_CONSTRAINT =
			"alter table deferred_events\n" +
			"    add constraint fk_state_deferred_events\n" +
			"    foreign key (jpa_repository_state_id)\n" +
			"    references state";

	public final static String ADD_SKIPPER_INFO_FK_INFO_STATUS_CONSTRAINT =
			"alter table skipper_info\n" +
			"    add constraint fk_info_status\n" +
			"    foreign key (status_id)\n" +
			"    references skipper_status";

	public final static String ADD_SKIPPER_PACKAGE_METADATA_FK_PACKAGE_METADATA_PFILE_CONSTRAINT =
			"alter table skipper_package_metadata\n" +
			"    add constraint fk_package_metadata_pfile\n" +
			"    foreign key (packagefile_id)\n" +
			"    references skipper_package_file";

	public final static String ADD_SKIPPER_RELEASE_FK_RELEASE_INFO_CONSTRAINT =
			"alter table skipper_release\n" +
			"    add constraint fk_release_info\n" +
			"    foreign key (info_id)\n" +
			"    references skipper_info";

	public final static String ADD_SKIPPER_RELEASE_FK_RELEASE_MANIFEST_CONSTRAINT =
			"alter table skipper_release\n" +
			"    add constraint fk_release_manifest\n" +
			"    foreign key (manifest_id)\n" +
			"    references skipper_manifest";

	public final static String ADD_STATE_FK_STATE_INITIAL_ACTION_CONSTRAINT =
			"alter table state\n" +
			"    add constraint fk_state_initial_action\n" +
			"    foreign key (initial_action_id)\n" +
			"    references action";

	public final static String ADD_STATE_FK_STATE_PARENT_STATE_CONSTRAINT =
			"alter table state\n" +
			"    add constraint fk_state_parent_state\n" +
			"    foreign key (parent_state_id)\n" +
			"    references state";

	public final static String ADD_STATE_ENTRY_ACTIONS_FK_STATE_ENTRY_ACTIONS_A_CONSTRAINT =
			"alter table state_entry_actions\n" +
			"    add constraint fk_state_entry_actions_a\n" +
			"    foreign key (entry_actions_id)\n" +
			"    references action";

	public final static String ADD_STATE_ENTRY_ACTIONS_FK_STATE_ENTRY_ACTIONS_S_CONSTRAINT =
			"alter table state_entry_actions\n" +
			"    add constraint fk_state_entry_actions_s\n" +
			"    foreign key (jpa_repository_state_id)\n" +
			"    references state";

	public final static String ADD_STATE_EXIT_ACTIONS_FK_STATE_EXIT_ACTIONS_A_CONSTRAINT =
			"alter table state_exit_actions\n" +
			"    add constraint fk_state_exit_actions_a\n" +
			"    foreign key (exit_actions_id)\n" +
			"    references action";

	public final static String ADD_STATE_EXIT_ACTIONS_FK_STATE_EXIT_ACTIONS_S_CONSTRAINT =
			"alter table state_exit_actions\n" +
			"    add constraint fk_state_exit_actions_s\n" +
			"    foreign key (jpa_repository_state_id)\n" +
			"    references state";

	public final static String ADD_STATE_STATE_ACTIONS_FK_STATE_STATE_ACTIONS_A_CONSTRAINT =
			"alter table state_state_actions\n" +
			"    add constraint fk_state_state_actions_a\n" +
			"    foreign key (state_actions_id)\n" +
			"    references action";

	public final static String ADD_STATE_STATE_ACTIONS_FK_STATE_STATE_ACTIONS_S_CONSTRAINT =
			"alter table state_state_actions\n" +
			"    add constraint fk_state_state_actions_s\n" +
			"    foreign key (jpa_repository_state_id)\n" +
			"    references state";

	public final static String ADD_TRANSITION_FK_TRANSITION_GUARD_CONSTRAINT =
			"alter table transition\n" +
			"    add constraint fk_transition_guard\n" +
			"    foreign key (guard_id)\n" +
			"    references guard";

	public final static String ADD_TRANSITION_FK_TRANSITION_SOURCE_CONSTRAINT =
			"alter table transition\n" +
			"    add constraint fk_transition_source\n" +
			"    foreign key (source_id)\n" +
			"    references state";

	public final static String ADD_TRANSITION_FK_TRANSITION_TARGET_CONSTRAINT =
			"alter table transition\n" +
			"    add constraint fk_transition_target\n" +
			"    foreign key (target_id)\n" +
			"    references state";

	public final static String ADD_TRANSITION_ACTIONS_FK_TRANSITION_ACTIONS_A_CONSTRAINT =
			"alter table transition_actions\n" +
			"    add constraint fk_transition_actions_a\n" +
			"    foreign key (actions_id)\n" +
			"    references action";

	public final static String ADD_TRANSITION_ACTIONS_FK_TRANSITION_ACTIONS_T_CONSTRAINT =
			"alter table transition_actions\n" +
			"    add constraint fk_transition_actions_t\n" +
			"    foreign key (jpa_repository_transition_id)\n" +
			"    references transition";

	public V1__Initial_Setup() {
		super(null);
	}

	@Override
	public List<SqlCommand> createHibernateSequence() {
		return Collections.emptyList();
	}

	@Override
	public List<SqlCommand> createSkipperTables() {
		return Arrays.asList(
				SqlCommand.from(CREATE_SKIPPER_APP_DEPLOYER_DATA_TABLE),
				SqlCommand.from(CREATE_SKIPPER_INFO_TABLE),
				SqlCommand.from(CREATE_SKIPPER_MANIFEST_TABLE),
				SqlCommand.from(CREATE_SKIPPER_PACKAGE_FILE_TABLE),
				SqlCommand.from(CREATE_SKIPPER_PACKAGE_METADATA_TABLE),
				SqlCommand.from(CREATE_SKIPPER_RELEASE_TABLE),
				SqlCommand.from(CREATE_SKIPPER_REPOSITORY_TABLE),
				SqlCommand.from(CREATE_SKIPPER_STATUS_TABLE),
				SqlCommand.from(CREATE_INX_PKG_NAME_INDEX),
				SqlCommand.from(CREATE_INX_REL_NAME_INDEX),
				SqlCommand.from(CREATE_INX_REPO_NAME_INDEX),
				SqlCommand.from(CREATE_INX_UK_INDEX)
				);
	}

	@Override
	public List<SqlCommand> createStateMachineTables() {
		return Arrays.asList(
				SqlCommand.from(CREATE_STATEMACHINE_ACTION_TABLE),
				SqlCommand.from(CREATE_STATEMACHINE_DEFERRED_EVENTS_TABLE),
				SqlCommand.from(CREATE_STATEMACHINE_GUARD_TABLE),
				SqlCommand.from(CREATE_STATEMACHINE_STATE_TABLE),
				SqlCommand.from(CREATE_STATEMACHINE_STATE_ENTRY_ACTIONS_TABLE),
				SqlCommand.from(CREATE_STATEMACHINE_STATE_EXIT_ACTIONS_TABLE),
				SqlCommand.from(CREATE_STATEMACHINE_STATE_STATE_ACTIONS_TABLE),
				SqlCommand.from(CREATE_STATEMACHINE_STATE_MACHINE_TABLE),
				SqlCommand.from(CREATE_STATEMACHINE_TRANSITION_TABLE),
				SqlCommand.from(CREATE_STATEMACHINE_TRANSITION_ACTIONS_TABLE),
				SqlCommand.from(ADD_DEFERRED_EVENTS_FK_STATE_DEFERRED_EVENTS_CONSTRAINT),
				SqlCommand.from(ADD_SKIPPER_INFO_FK_INFO_STATUS_CONSTRAINT),
				SqlCommand.from(ADD_SKIPPER_PACKAGE_METADATA_FK_PACKAGE_METADATA_PFILE_CONSTRAINT),
				SqlCommand.from(ADD_SKIPPER_RELEASE_FK_RELEASE_INFO_CONSTRAINT),
				SqlCommand.from(ADD_SKIPPER_RELEASE_FK_RELEASE_MANIFEST_CONSTRAINT),
				SqlCommand.from(ADD_STATE_FK_STATE_INITIAL_ACTION_CONSTRAINT),
				SqlCommand.from(ADD_STATE_FK_STATE_PARENT_STATE_CONSTRAINT),
				SqlCommand.from(ADD_STATE_ENTRY_ACTIONS_FK_STATE_ENTRY_ACTIONS_A_CONSTRAINT),
				SqlCommand.from(ADD_STATE_ENTRY_ACTIONS_FK_STATE_ENTRY_ACTIONS_S_CONSTRAINT),
				SqlCommand.from(ADD_STATE_EXIT_ACTIONS_FK_STATE_EXIT_ACTIONS_A_CONSTRAINT),
				SqlCommand.from(ADD_STATE_EXIT_ACTIONS_FK_STATE_EXIT_ACTIONS_S_CONSTRAINT),
				SqlCommand.from(ADD_STATE_STATE_ACTIONS_FK_STATE_STATE_ACTIONS_A_CONSTRAINT),
				SqlCommand.from(ADD_STATE_STATE_ACTIONS_FK_STATE_STATE_ACTIONS_S_CONSTRAINT),
				SqlCommand.from(ADD_TRANSITION_FK_TRANSITION_GUARD_CONSTRAINT),
				SqlCommand.from(ADD_TRANSITION_FK_TRANSITION_SOURCE_CONSTRAINT),
				SqlCommand.from(ADD_TRANSITION_FK_TRANSITION_TARGET_CONSTRAINT),
				SqlCommand.from(ADD_TRANSITION_ACTIONS_FK_TRANSITION_ACTIONS_A_CONSTRAINT),
				SqlCommand.from(ADD_TRANSITION_ACTIONS_FK_TRANSITION_ACTIONS_T_CONSTRAINT)
				);
	}

}
