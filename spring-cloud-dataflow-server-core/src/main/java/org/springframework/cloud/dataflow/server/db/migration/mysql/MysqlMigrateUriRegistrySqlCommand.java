/*
 * Copyright 2019-2022 the original author or authors.
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
package org.springframework.cloud.dataflow.server.db.migration.mysql;

import java.util.List;

import org.postgresql.core.SqlCommand;

import org.springframework.cloud.dataflow.server.db.migration.AbstractMigrateUriRegistrySqlCommand;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@code mysql} related {@link SqlCommand} for migrating data from
 * {@code URI_REGISTRY} into {@code app_registration}.
 *
 * @author Janne Valkealahti
 * @author Chris Bono
 */
public class MysqlMigrateUriRegistrySqlCommand extends AbstractMigrateUriRegistrySqlCommand {

	@Override
	protected void updateAppRegistration(JdbcTemplate jdbcTemplate, List<AppRegistrationMigrationData> data) {
		// get value from hibernate sequence table and update it later
		// depending on how many updates we did
		long nextVal = jdbcTemplate.queryForObject("select next_val as id_val from hibernate_sequence", Long.class);
		long nextValUpdates = nextVal;
		for (AppRegistrationMigrationData d : data) {
			jdbcTemplate.update(
					"insert into app_registration (id, object_version, default_version, metadata_uri, name, type, uri, version) values (?,?,?,?,?,?,?,?)",
					nextValUpdates++, 0, d.isDefaultVersion() ? 1 : 0, d.getMetadataUri(), d.getName(), d.getType(),
					d.getUri(), d.getVersion());
		}
		jdbcTemplate.update("update hibernate_sequence set next_val= ? where next_val=?", nextValUpdates, nextVal);
	}
}
