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
package org.springframework.cloud.skipper.server.db.migration.oracle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.cloud.dataflow.common.flyway.SqlCommand;
import org.springframework.cloud.skipper.server.db.migration.AbstractInitialSetupMigration;

public class V1__Initial_Setup extends AbstractInitialSetupMigration {

	public final static String CREATE_SKIPPER_APP_DEPLOYER_DATA_TABLE =
			"create table skipper_app_deployer_data (\n" +
			"    id number(19,0) not null,\n" +
			"    object_version number(19,0),\n" +
			"    deployment_data clob,\n" +
			"    release_name varchar2(255 char),\n" +
			"    release_version number(10,0),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_INFO_TABLE =
			"create table skipper_info (\n" +
			"    id number(19,0) not null,\n" +
			"    object_version number(19,0),\n" +
			"    deleted timestamp,\n" +
			"    description varchar2(255 char),\n" +
			"    first_deployed timestamp,\n" +
			"    last_deployed timestamp,\n" +
			"    status_id number(19,0),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_MANIFEST_TABLE =
			"create table skipper_manifest (\n" +
			"    id number(19,0) not null,\n" +
			"    object_version number(19,0),\n" +
			"    data clob,\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_PACKAGE_FILE_TABLE =
			"create table skipper_package_file (\n" +
			"    id number(19,0) not null,\n" +
			"    package_bytes blob,\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_PACKAGE_METADATA_TABLE =
			"create table skipper_package_metadata (\n" +
			"    id number(19,0) not null,\n" +
			"    object_version number(19,0),\n" +
			"    api_version varchar2(255 char),\n" +
			"    description clob,\n" +
			"    display_name varchar2(255 char),\n" +
			"    icon_url clob,\n" +
			"    kind varchar2(255 char),\n" +
			"    maintainer varchar2(255 char),\n" +
			"    name varchar2(255 char),\n" +
			"    origin varchar2(255 char),\n" +
			"    package_home_url clob,\n" +
			"    package_source_url clob,\n" +
			"    repository_id number(19,0),\n" +
			"    repository_name varchar2(255 char),\n" +
			"    sha256 varchar2(255 char),\n" +
			"    tags clob,\n" +
			"    version varchar2(255 char),\n" +
			"    packagefile_id number(19,0),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_RELEASE_TABLE =
			"create table skipper_release (\n" +
			"    id number(19,0) not null,\n" +
			"    object_version number(19,0),\n" +
			"    config_values_string clob,\n" +
			"    name varchar2(255 char),\n" +
			"    package_metadata_id number(19,0),\n" +
			"    pkg_json_string clob,\n" +
			"    platform_name varchar2(255 char),\n" +
			"    repository_id number(19,0),\n" +
			"    version number(10,0) not null,\n" +
			"    info_id number(19,0),\n" +
			"    manifest_id number(19,0),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_REPOSITORY_TABLE =
			"create table skipper_repository (\n" +
			"    id number(19,0) not null,\n" +
			"    object_version number(19,0),\n" +
			"    description varchar2(255 char),\n" +
			"    local number(1,0),\n" +
			"    name varchar2(255 char),\n" +
			"    repo_order number(10,0),\n" +
			"    source_url clob,\n" +
			"    url clob,\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_SKIPPER_STATUS_TABLE =
			"create table skipper_status (\n" +
			"    id number(19,0) not null,\n" +
			"    platform_status clob,\n" +
			"    status_code varchar2(255 char),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_ACTION_TABLE =
			"create table action (\n" +
			"    id number(19,0) not null,\n" +
			"    name varchar2(255 char),\n" +
			"    spel varchar2(255 char),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_DEFERRED_EVENTS_TABLE =
			"create table deferred_events (\n" +
			"    jpa_repository_state_id number(19,0) not null,\n" +
			"    deferred_events varchar2(255 char)\n" +
			")";

	public final static String CREATE_STATEMACHINE_GUARD_TABLE =
			"create table guard (\n" +
			"    id number(19,0) not null,\n" +
			"    name varchar2(255 char),\n" +
			"    spel varchar2(255 char),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_STATE_TABLE =
			"create table state (\n" +
			"    id number(19,0) not null,\n" +
			"    initial_state number(1,0) not null,\n" +
			"    kind number(10,0),\n" +
			"    machine_id varchar2(255 char),\n" +
			"    region varchar2(255 char),\n" +
			"    state varchar2(255 char),\n" +
			"    submachine_id varchar2(255 char),\n" +
			"    initial_action_id number(19,0),\n" +
			"    parent_state_id number(19,0),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_STATE_ENTRY_ACTIONS_TABLE =
			"create table state_entry_actions (\n" +
			"    jpa_repository_state_id number(19,0) not null,\n" +
			"    entry_actions_id number(19,0) not null,\n" +
			"    primary key (jpa_repository_state_id, entry_actions_id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_STATE_EXIT_ACTIONS_TABLE =
			"create table state_exit_actions (\n" +
			"    jpa_repository_state_id number(19,0) not null,\n" +
			"    exit_actions_id number(19,0) not null,\n" +
			"    primary key (jpa_repository_state_id, exit_actions_id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_STATE_STATE_ACTIONS_TABLE =
			"create table state_state_actions (\n" +
			"    jpa_repository_state_id number(19,0) not null,\n" +
			"    state_actions_id number(19,0) not null,\n" +
			"    primary key (jpa_repository_state_id, state_actions_id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_STATE_MACHINE_TABLE =
			"create table state_machine (\n" +
			"    machine_id varchar2(255 char) not null,\n" +
			"    state varchar2(255 char),\n" +
			"    state_machine_context blob,\n" +
			"    primary key (machine_id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_TRANSITION_TABLE =
			"create table transition (\n" +
			"    id number(19,0) not null,\n" +
			"    event varchar2(255 char),\n" +
			"    kind number(10,0),\n" +
			"    machine_id varchar2(255 char),\n" +
			"    guard_id number(19,0),\n" +
			"    source_id number(19,0),\n" +
			"    target_id number(19,0),\n" +
			"    primary key (id)\n" +
			")";

	public final static String CREATE_STATEMACHINE_TRANSITION_ACTIONS_TABLE =
			"create table transition_actions (\n" +
			"    jpa_repository_transition_id number(19,0) not null,\n" +
			"    actions_id number(19,0) not null,\n" +
			"    primary key (jpa_repository_transition_id, actions_id)\n" +
			")";

	public final static String CREATE_INX_PKG_NAME_INDEX =
			"create index idx_pkg_name on skipper_package_metadata (name)";

	public final static String CREATE_INX_REL_NAME_INDEX =
			"create index idx_rel_name on skipper_release (name)";

	public final static String CREATE_INX_REPO_NAME_INDEX =
			"create index idx_repo_name on skipper_repository (name)";

	public final static String ADD_SKIPPER_REPOSITORY_UK_REPOSITORY_CONSTRAINT =
			"alter table skipper_repository\n" +
			"    add constraint uk_repository unique (name)";

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
				SqlCommand.from(CREATE_INX_REPO_NAME_INDEX));
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
				SqlCommand.from(ADD_SKIPPER_REPOSITORY_UK_REPOSITORY_CONSTRAINT),
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
				SqlCommand.from(ADD_TRANSITION_ACTIONS_FK_TRANSITION_ACTIONS_T_CONSTRAINT));
	}
}
