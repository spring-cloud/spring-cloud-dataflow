/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.server.db.migration.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.postgresql.core.SqlCommand;

import org.springframework.cloud.dataflow.server.db.migration.AbstractMigrateUriRegistrySqlCommand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;

/**
 * {@code postgres} related {@link SqlCommand} for migrating data from
 * {@code URI_REGISTRY} into {@code app_registration}.
 *
 * @author Janne Valkealahti
 *
 */
public class PostgresMigrateUriRegistrySqlCommand extends AbstractMigrateUriRegistrySqlCommand {

	@Override
	public void handle(JdbcTemplate jdbcTemplate, Connection connection) {
		// we need to disable connection autocommit for this operation
		// as with postgres a CLOB cannot be inserted with autocommit enabled.

		// TODO: should think if this same autocommit logic could get extracted to
		//       to some sort of base impl as it's used in PostgresMigrateTaskDefinitionsSqlCommand
		//       and PostgresMigrateStreamDefinitionsSqlCommand in a same way

		Boolean autoCommit = null;
		try {
			autoCommit = connection.getAutoCommit();
		} catch (SQLException e) {
			throw new RuntimeException("cannot access connection autocommit setting", e);
		}
		if (autoCommit != null) {
			try {
				connection.setAutoCommit(false);
			} catch (SQLException e) {
				throw new RuntimeException("cannot access connection autocommit setting", e);
			}
		}
		super.handle(jdbcTemplate, connection);
		if (autoCommit != null) {
			try {
				connection.setAutoCommit(autoCommit);
			} catch (SQLException e) {
				throw new RuntimeException("cannot access connection autocommit setting", e);
			}
		}
	}

	@Override
	protected void updateAppRegistration(JdbcTemplate jdbcTemplate, List<AppRegistrationMigrationData> data) {
		DefaultLobHandler lobHandler = new DefaultLobHandler();
		lobHandler.setWrapAsLob(true);
		for (AppRegistrationMigrationData d : data) {
			Long nextVal = jdbcTemplate.queryForObject("select nextval('hibernate_sequence')", Long.class);
			jdbcTemplate.update(
					"insert into app_registration (id, object_version, default_version, metadata_uri, name, type, uri, version) values (?,?,?,?,?,?,?,?)",
					new Object[] { nextVal, 0, d.isDefaultVersion(), new SqlLobValue(d.getMetadataUri(), lobHandler),
							d.getName(), d.getType(), new SqlLobValue(d.getUri(), lobHandler), d.getVersion() },
					new int[] { Types.BIGINT, Types.BIGINT, Types.BOOLEAN, Types.CLOB, Types.VARCHAR, Types.INTEGER,
							Types.CLOB, Types.VARCHAR });
		}
	}
}
