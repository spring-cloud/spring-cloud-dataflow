/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.cloud.dataflow.server.db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.common.flyway.SqlCommand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility class that can be used in future to drop columns.
 * This checks for the existence of the column before dropping.
 * @author Corneil du Plessis
 */
public class DropColumnSqlCommands extends SqlCommand {
	private final static Logger logger = LoggerFactory.getLogger(DropColumnSqlCommands.class);

	private final List<String> columnNames = new ArrayList<>();

	public DropColumnSqlCommands(String... columnName) {
		columnNames.addAll(Arrays.asList(columnName));
	}

	@Override
	public void handle(JdbcTemplate jdbcTemplate, Connection connection) {
		for(String name : columnNames) {
			try {
				dropColumn(jdbcTemplate, connection, name);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public boolean canHandleInJdbcTemplate() {
		return true;
	}

	protected void dropColumn(JdbcTemplate jdbcTemplate, Connection connection, String name) throws SQLException {
		logger.debug("dropping:{}", name);
		String [] parts = StringUtils.split(name, ".");
		Assert.notNull(parts, "Expected 2 or more parts from " + name);
		Assert.isTrue(parts.length > 1, "Expected 2 or more parts from " + name);
		String columnName = parts[parts.length - 1];
		String tableName = parts[parts.length - 2];
		String schemaName = parts.length > 2 ? parts[parts.length - 3] : null;
		logger.debug("Searching for {}.{}", tableName, columnName);
		if(hasColumn(connection, schemaName, tableName, columnName)) {
			String sql = String.format("alter table %s drop column %s", tableName, columnName);
			logger.debug("Executing: {}", sql);
			jdbcTemplate.execute(sql);
		}
	}
	protected boolean hasColumn(Connection connection, String schemaName, String tableName, String columnName) throws SQLException {
		String actualSchemaName = null;
		if(StringUtils.hasText(schemaName)) {
			try(ResultSet resultSet = connection.getMetaData().getSchemas()) {
				while (resultSet.next()) {
					String name = resultSet.getString("SCHEMA_NAME");
					// determine the actual name used in specific database metadata. 
					if(name.equalsIgnoreCase(schemaName)) {
						actualSchemaName = name;
						break;
					}
				}
			}
		}
		String actualTableName = tableName;
		try(ResultSet resultSet = connection.getMetaData().getTables(null, actualSchemaName, null, new String[] {"TABLE"})) {
			while (resultSet.next()) {
				String name = resultSet.getString("TABLE_NAME");
				// determine the actual name used in specific database metadata.
				if(name.equalsIgnoreCase(tableName)) {
					actualTableName = name;
					break;
				}
			}
		}
		// actual names need to be same case as reported by meta data query for some databases.
		try (ResultSet resultSet = connection.getMetaData().getColumns(null, actualSchemaName, actualTableName, null)) {
			while (resultSet.next()) {
				String foundColumnName = resultSet.getString("COLUMN_NAME");
				if (foundColumnName.equalsIgnoreCase(columnName)) {
					return true;
				}
			}
		}
		return false;
	}
}
