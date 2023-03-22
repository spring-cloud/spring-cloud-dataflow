/*
 * Copyright 2022-2022 the original author or authors.
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
package org.springframework.cloud.dataflow.common.flyway;

import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * Provides utility methods to help with {@link DatabaseDriver} related operations.
 */
public final class DatabaseDriverUtils {

	private static final Logger LOG = LoggerFactory.getLogger(DatabaseDriverUtils.class);

	private DatabaseDriverUtils() {
	}

	/**
	 * Finds a database driver suitable for a datasource.
	 * <p>By default, the jdbc url reported from the database metdata is used to determine
	 * the driver. It also handles the special case where MariaDB reports a 'jdbc:maria'
	 * url eventhough the original url was prefixed with 'jdbc:mysql'.
	 *
	 * @param dataSource the datasource to inspect
	 * @return a database driver suitable for the datasource
	 */
	public static DatabaseDriver getDatabaseDriver(DataSource dataSource) {
		// copied from boot's flyway auto-config to get matching db vendor id (but adjusted
		// to handle the case when MariaDB driver is being used against MySQL database).
		try {
			String url = JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getURL);
			DatabaseDriver databaseDriver = DatabaseDriver.fromJdbcUrl(url);
			if (databaseDriver == DatabaseDriver.MARIADB) {
				// MariaDB reports a 'jdbc:maria' url even when user specified 'jdbc:mysql'.
				// Verify the underlying database is not really MySql.
				String product = JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName);
				if (DatabaseDriver.MYSQL.name().equalsIgnoreCase(product)) {
					LOG.info("Using MariaDB driver against MySQL database - will use MySQL");
					databaseDriver = DatabaseDriver.MYSQL;
				}
			}
			return databaseDriver;
		}
		catch (MetaDataAccessException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
