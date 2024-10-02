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
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

/**
 * Provides for converting text or longtext fields in PostgreSQL to OID.
 *
 * @author Corneil du Plessis
 */
public class PostgreSQLTextToOID {
	private final static Logger logger = LoggerFactory.getLogger(PostgreSQLTextToOID.class);

	private final static String ADD_TMP_OID_COL = "alter table %s add column %s oid";

	private final static String ADD_TMP_TEXT_COL = "alter table %s add column %s text";

	private final static String UPDATE_TMP_OID_COL = "update %s set %s = lo_from_bytea(0, %s::bytea), %s = null where %s in (select %s from %s where %s is null and %s is not null limit 100)";

	private final static String UPDATE_TMP_TEXT_COL = "update %s set %s = convert_from(lo_get(cast(%s as bigint)),'UTF8'), %s = null where %s in (select %s from %s where %s is null and %s is not null limit 100)";

	private final static String DROP_ORIGINAL_COL = "alter table %s drop column %s";

	private final static String RENAME_TMP_COL = "alter table %s rename column %s to %s";

	public static void convertColumnToOID(String table, String id, String column, DataSource dataSource) {

		try (Connection connection = dataSource.getConnection()) {
			String tableName = table;
			try(ResultSet tables = connection.getMetaData().getTables(null, null, null, null)) {
				while(tables.next()) {
					String name = tables.getString("TABLE_NAME");
					if(name.equalsIgnoreCase(table)) {
						tableName = name;
						break;
					}
				}
			}
			logger.debug("searching:{}", tableName);
			try (ResultSet resultSet = connection.getMetaData().getColumns(null, null, tableName, null)) {
				int count = 0;
				while (resultSet.next()) {
					String columnName = resultSet.getString("COLUMN_NAME");
					if(columnName.equalsIgnoreCase(column)) {
						count++;
						int dataType = resultSet.getInt("DATA_TYPE");
						logger.info("Found {}:{}:{}", table, column, JDBCType.valueOf(dataType));
						if (dataType == Types.BIGINT) {
							return;
						}
					}
				}
				Assert.isTrue(count > 0, "Cannot find " + table + ":" + column);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		JdbcTemplate template = new JdbcTemplate(dataSource);
		final String tmp_col = column + "_tmp";
		String sqlTmp = String.format(ADD_TMP_OID_COL, table, tmp_col);
		logger.debug("Executing:{}", sqlTmp);
		template.update(sqlTmp);
		int total = 0;
		do {
			String sql = String.format(UPDATE_TMP_OID_COL, table, tmp_col, column, column, id, id, table, tmp_col, column);
			logger.debug("Executing:{}", sql);
			int count = template.update(sql);
			total += count;
			if (count <= 0) {
				logger.info("Updated {} rows of {} in {}", total, column, table);
				break;
			}
		} while (true);
		String sqlDrop = String.format(DROP_ORIGINAL_COL, table, column);
		logger.debug("Executing:{}", sqlDrop);
		template.update(sqlDrop);
		String sqlRename = String.format(RENAME_TMP_COL, table, tmp_col, column);
		logger.debug("Executing:{}", sqlRename);
		template.update(sqlRename);
	}

	public static void convertColumnFromOID(String table, String id, String column, DataSource dataSource) {
		try (Connection connection = dataSource.getConnection()) {
			String tableName = table;
			try(ResultSet tables = connection.getMetaData().getTables(null, null, null, null)) {
				while(tables.next()) {
					String name = tables.getString("TABLE_NAME");
					if(name.equalsIgnoreCase(table)) {
						tableName = name;
						break;
					}
				}
			}
			logger.debug("searching:{}", tableName);
			try (ResultSet resultSet = connection.getMetaData().getColumns(null, null, tableName, null)) {
				int count = 0;
				while (resultSet.next()) {
					String columnName = resultSet.getString("COLUMN_NAME");
					if(columnName.equalsIgnoreCase(column)) {
						count++;
						int dataType = resultSet.getInt("DATA_TYPE");
						logger.info("Found {}:{}:{}", table, column, JDBCType.valueOf(dataType));
						if (dataType != Types.BIGINT) {
							return;
						}
					}
				}
				Assert.isTrue(count > 0, "Cannot find " + table + ":" + column);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		JdbcTemplate template = new JdbcTemplate(dataSource);
		final String tmp_col = column + "_tmp";
		String sqlTmp = String.format(ADD_TMP_TEXT_COL, table, tmp_col);
		logger.debug("Executing:{}", sqlTmp);
		template.update(sqlTmp);
		int total = 0;
		do {
			String sql = String.format(UPDATE_TMP_TEXT_COL, table, tmp_col, column, column, id, id, table, tmp_col, column);
			logger.debug("Executing:{}", sql);
			int count = template.update(sql);
			total += count;
			if (count <= 0) {
				logger.info("Updated {} rows of {} in {}", total, column, table);
				break;
			}
		} while (true);
		String sqlDrop = String.format(DROP_ORIGINAL_COL, table, column);
		logger.debug("Executing:{}", sqlDrop);
		template.update(sqlDrop);
		String sqlRename = String.format(RENAME_TMP_COL, table, tmp_col, column);
		logger.debug("Executing:{}", sqlRename);
		template.update(sqlRename);
	}
}
