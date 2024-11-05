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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract SQL Paging Query Provider to serve as a base class for all provided SQL paging
 * query providers.
 * <p>
 * Any implementation must provide a way to specify the select clause, from clause and
 * optionally a where clause. It is recommended that there should be an index for the sort
 * key to provide better performance.
 * <p>
 * Provides properties and preparation for the mandatory "selectClause" and "fromClause"
 * as well as for the optional "whereClause".
 *
 * @author Glenn Renfro
 */
public abstract class AbstractSqlPagingQueryProvider implements PagingQueryProvider {

	private String selectClause;

	private String fromClause;

	private String whereClause;

	private Map<String, Order> sortKeys = new LinkedHashMap<String, Order>();

	private int parameterCount;

	private boolean usingNamedParameters;

	/**
	 * @return SQL SELECT clause part of SQL query string
	 */
	protected String getSelectClause() {
		return selectClause;
	}

	/**
	 * @param selectClause SELECT clause part of SQL query string
	 */
	public void setSelectClause(String selectClause) {
		this.selectClause = removeKeyWord("select", selectClause);
	}

	/**
	 * @return SQL FROM clause part of SQL query string
	 */
	protected String getFromClause() {
		return fromClause;
	}

	/**
	 * @param fromClause FROM clause part of SQL query string
	 */
	public void setFromClause(String fromClause) {
		this.fromClause = removeKeyWord("from", fromClause);
	}

	/**
	 * @return SQL WHERE clause part of SQL query string
	 */
	protected String getWhereClause() {
		return whereClause;
	}

	/**
	 * @param whereClause WHERE clause part of SQL query string
	 */
	public void setWhereClause(String whereClause) {
		if (StringUtils.hasText(whereClause)) {
			this.whereClause = removeKeyWord("where", whereClause);
		}
		else {
			this.whereClause = null;
		}
	}

	/**
	 * A Map&lt;String, Order&gt; of sort columns as the key and {@link Order} for
	 * ascending/descending.
	 *
	 * @return sortKey key to use to sort and limit page content
	 */
	@Override
	public Map<String, Order> getSortKeys() {
		return sortKeys;
	}

	/**
	 * @param sortKeys key to use to sort and limit page content
	 */
	public void setSortKeys(Map<String, Order> sortKeys) {
		this.sortKeys = sortKeys;
	}

	@Override
	public int getParameterCount() {
		return parameterCount;
	}

	@Override
	public boolean isUsingNamedParameters() {
		return usingNamedParameters;
	}

	@Override
	public void init(DataSource dataSource) throws Exception {
		Assert.notNull(dataSource, "data source must be specified");
		Assert.hasLength(selectClause, "selectClause must be specified");
		Assert.hasLength(fromClause, "fromClause must be specified");
		Assert.notEmpty(sortKeys, "sortKey must be specified");
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(selectClause);
		sql.append(" FROM ").append(fromClause);
		if (whereClause != null) {
			sql.append(" WHERE ").append(whereClause);
		}
		List<String> namedParameters = new ArrayList<String>();
		parameterCount = JdbcParameterUtils.countParameterPlaceholders(sql.toString(), namedParameters);
		if (namedParameters.size() > 0) {
			if (parameterCount != namedParameters.size()) {
				throw new InvalidDataAccessApiUsageException(
						"You can't use both named parameters and classic \"?\" placeholders: " + sql);
			}
			usingNamedParameters = true;
		}
	}

	private String removeKeyWord(String keyWord, String clause) {
		String temp = clause.trim();
		String keyWordString = keyWord + " ";
		if (temp.toLowerCase(Locale.ROOT).startsWith(keyWordString) && temp.length() > keyWordString.length()) {
			return temp.substring(keyWordString.length());
		}
		else {
			return temp;
		}
	}
}
