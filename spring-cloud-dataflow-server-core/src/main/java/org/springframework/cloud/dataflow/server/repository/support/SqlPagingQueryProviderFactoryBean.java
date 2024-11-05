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

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.dataflow.core.database.support.DatabaseType;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for {@link PagingQueryProvider} interface. The database type will be
 * determined from the data source if not provided explicitly. Valid types are given by
 * the {@link DatabaseType} enum.
 *
 * @author Glenn Renfro
 */
public class SqlPagingQueryProviderFactoryBean implements FactoryBean<PagingQueryProvider> {

	private DataSource dataSource;

	private String databaseType;

	private String fromClause;

	private String whereClause;

	private String selectClause;

	private Map<String, Order> sortKeys;

	private final static Map<DatabaseType, AbstractSqlPagingQueryProvider> providers;

	static {
		Map<DatabaseType, AbstractSqlPagingQueryProvider> providerMap = new HashMap<DatabaseType, AbstractSqlPagingQueryProvider>();
		providerMap.put(DatabaseType.HSQL, new HsqlPagingQueryProvider());
		providerMap.put(DatabaseType.H2, new H2PagingQueryProvider());
		providerMap.put(DatabaseType.MYSQL, new MySqlPagingQueryProvider());
		providerMap.put(DatabaseType.MARIADB, new MariaDBPagingQueryProvider());
		providerMap.put(DatabaseType.POSTGRES, new PostgresPagingQueryProvider());
		providerMap.put(DatabaseType.ORACLE, new OraclePagingQueryProvider());
		providerMap.put(DatabaseType.SQLSERVER, new SqlServerPagingQueryProvider());
		providerMap.put(DatabaseType.DB2, new Db2PagingQueryProvider());
		providers = Collections.unmodifiableMap(providerMap);
	}

	/**
	 * @param databaseType the databaseType to set
	 */
	public void setDatabaseType(String databaseType) {
		Assert.hasText(databaseType, "databaseType must not be empty nor null");
		this.databaseType = databaseType;
	}

	/**
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		Assert.notNull(dataSource, "dataSource must not be null");
		this.dataSource = dataSource;
	}

	/**
	 * @param fromClause the fromClause to set
	 */
	public void setFromClause(String fromClause) {
		Assert.hasText(fromClause, "fromClause must not be empty nor null");
		this.fromClause = fromClause;
	}

	/**
	 * @param whereClause the whereClause to set
	 */
	public void setWhereClause(String whereClause) {
		this.whereClause = whereClause;
	}

	/**
	 * @param selectClause the selectClause to set
	 */
	public void setSelectClause(String selectClause) {
		Assert.hasText(selectClause, "selectClause must not be empty nor null");
		this.selectClause = selectClause;
	}

	/**
	 * @param sortKeys the sortKeys to set
	 */
	public void setSortKeys(Map<String, Order> sortKeys) {
		this.sortKeys = sortKeys;
	}

	/**
	 * Get a {@link PagingQueryProvider} instance using the provided properties and
	 * appropriate for the given database type.
	 *
	 * @see FactoryBean#getObject()
	 */
	@Override
	public PagingQueryProvider getObject() throws Exception {

		DatabaseType type;
		try {
			type = databaseType != null ? DatabaseType.valueOf(databaseType.toUpperCase(Locale.ROOT))
					: DatabaseType.fromMetaData(dataSource);
		}
		catch (MetaDataAccessException e) {
			throw new IllegalArgumentException(
					"Could not inspect meta data for database type.  You have to supply it explicitly.", e);
		}

		AbstractSqlPagingQueryProvider provider = providers.get(type);
		Assert.state(provider != null, "Should not happen: missing PagingQueryProvider for DatabaseType=" + type);

		provider.setFromClause(fromClause);
		provider.setWhereClause(whereClause);
		provider.setSortKeys(sortKeys);
		if (StringUtils.hasText(selectClause)) {
			provider.setSelectClause(selectClause);
		}
		provider.init(dataSource);

		return provider;

	}

	/**
	 * Always returns {@link PagingQueryProvider}.
	 *
	 * @see FactoryBean#getObjectType()
	 */
	@Override
	public Class<PagingQueryProvider> getObjectType() {
		return PagingQueryProvider.class;
	}

	/**
	 * Always returns true.
	 *
	 * @see FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return true;
	}
}
