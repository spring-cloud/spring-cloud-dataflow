/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.repository.support;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.StringUtils;

/**
 * Enum representing a database type, such as Postgres or Oracle. The type also contains a
 * product name, which is expected to be the same as the product name provided by the
 * database driver's metadata.
 *
 * @author Glenn Renfro
 */
public enum DatabaseType {

	HSQL("HSQL Database Engine"),
	H2("H2"),
	ORACLE("Oracle"),
	MYSQL("MySQL"),
	POSTGRES("PostgreSQL"),
	SQLSERVER("Microsoft SQL Server"),
	DB2("DB2");

	private static final Map<String, DatabaseType> dbNameMap;

	static {
		dbNameMap = new HashMap<String, DatabaseType>();
		for (DatabaseType type : values()) {
			dbNameMap.put(type.getProductName(), type);
		}
	}

	private final String productName;

	private DatabaseType(String productName) {
		this.productName = productName;
	}

	/**
	 * Convenience method that pulls a database product name from the DataSource's
	 * metadata.
	 *
	 * @param dataSource the datasource used to extact metadata.
	 * @return DatabaseType The database type associated with the datasource.
	 * @throws MetaDataAccessException thrown if failure occurs on metadata lookup.
	 */
	public static DatabaseType fromMetaData(DataSource dataSource) throws MetaDataAccessException {
		String databaseProductName = JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductName").toString();
		if (StringUtils.hasText(databaseProductName) && !databaseProductName.equals("DB2/Linux")
				&& databaseProductName.startsWith("DB2")) {
			String databaseProductVersion = JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductVersion")
					.toString();
			if (!databaseProductVersion.startsWith("SQL")) {
				databaseProductName = "DB2ZOS";
			}
			else {
				databaseProductName = JdbcUtils.commonDatabaseName(databaseProductName);
			}
		}
		else {
			databaseProductName = JdbcUtils.commonDatabaseName(databaseProductName);
		}
		return fromProductName(databaseProductName);
	}

	/**
	 * Static method to obtain a DatabaseType from the provided product name.
	 *
	 * @param productName the name of the database.
	 * @return DatabaseType for given product name.
	 * @throws IllegalArgumentException if none is found.
	 */
	public static DatabaseType fromProductName(String productName) {
		if (!dbNameMap.containsKey(productName)) {
			throw new IllegalArgumentException("DatabaseType not found for product name: [" + productName + "]");
		}
		else {
			return dbNameMap.get(productName);
		}
	}

	private String getProductName() {
		return productName;
	}

}
