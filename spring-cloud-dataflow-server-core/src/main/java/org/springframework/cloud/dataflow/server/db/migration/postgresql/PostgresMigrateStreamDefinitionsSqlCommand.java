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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.cloud.dataflow.server.db.migration.SqlCommand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;

/**
 * {@link SqlCommand} copying data from {@code STREAM_DEFINITIONS} table into
 * {@code stream_definitions_tmp} table handling correct way to use CLOB with
 * postgres.
 *
 * @author Janne Valkealahti
 *
 */
public class PostgresMigrateStreamDefinitionsSqlCommand extends SqlCommand {

	@Override
	public boolean canHandleInJdbcTemplate() {
		return true;
	}

	@Override
	public void handle(JdbcTemplate jdbcTemplate, Connection connection) {
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

		Map<String, String> data = new HashMap<>();

		jdbcTemplate.query("select DEFINITION_NAME, DEFINITION from STREAM_DEFINITIONS", rs -> {
			data.put(rs.getString(1), rs.getString(2));
		});

		DefaultLobHandler lobHandler = new DefaultLobHandler();
		lobHandler.setWrapAsLob(true);

		for (Entry<String, String> d : data.entrySet()) {
			jdbcTemplate.update("insert into stream_definitions_tmp (definition_name, definition) values (?,?)",
					new Object[] { d.getKey(), new SqlLobValue(d.getValue(), lobHandler) },
					new int[] { Types.VARCHAR, Types.CLOB });
		}

		if (autoCommit != null) {
			try {
				connection.setAutoCommit(autoCommit);
			} catch (SQLException e) {
				throw new RuntimeException("cannot access connection autocommit setting", e);
			}
		}
	}
}
