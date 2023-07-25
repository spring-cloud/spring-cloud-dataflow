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

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Provides for converting text or longtext fields in PostgreSQL to OID.
 *
 * @author Corneil du Plessis
 */
public class PostgreSQLTextToOID {
	private final static Logger logger = LoggerFactory.getLogger(PostgreSQLTextToOID.class);

	private final static String ADD_TMP_OID_COL = "alter table %s add column %s oid";

	private final static String UPDATE_TMP_OID_COL = "update %s set %s = lo_from_bytea(0, %s::bytea), %s = null where %s in (select %s from %s where %s is null and %s is not null limit 100)";

	private final static String DROP_ORIGINAL_COL = "alter table %s drop column %s";

	private final static String RENAME_OID_COL = "alter table %s rename column %s to %s";

	public static void convertColumn(String table, String id, String column, DataSource dataSource) {
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
		String sqlRename = String.format(RENAME_OID_COL, table, tmp_col, column);
		logger.debug("Executing:{}", sqlRename);
		template.update(sqlRename);
	}
}
