/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.repository;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.springframework.cloud.dataflow.server.repository.support.Order;
import org.springframework.cloud.dataflow.server.repository.support.PagingQueryProvider;
import org.springframework.cloud.dataflow.server.repository.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract class for RDBMS based repositories.
 *
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public abstract class AbstractRdbmsKeyValueRepository<D> {

	protected String keyColumn;

	protected String valueColumn;

	protected String selectClause;

	protected String tableName = "%PREFIX%%SUFFIX% ";

	protected final String LIST_OF_NAMES = "listnames";

	protected String whereClauseByKey;

	protected String inClauseByKey;

	protected String findAllQuery;

	protected String findAllWhereClauseByKey;

	protected String saveRow;

	protected String updateValue;

	protected String countAll;

	protected String countByKey;

	protected String findAllWhereInClause = findAllQuery + whereClauseByKey;

	protected String deleteFromTableClause = "DELETE FROM " + tableName;

	protected String deleteFromTableByKey = deleteFromTableClause + whereClauseByKey;

	protected String tablePrefix;

	protected String tableSuffix;

	protected JdbcOperations jdbcTemplate;

	protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	protected DataSource dataSource;

	protected Map<String, Order> orderMap;

	protected final RowMapper<D> rowMapper;

	public AbstractRdbmsKeyValueRepository(DataSource dataSource, String tablePrefix, String tableSuffix,
			RowMapper<D> rowMapper, String keyColumn, String valueColumn) {
		Assert.notNull(dataSource);
		Assert.notNull(rowMapper);
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		this.dataSource = dataSource;
		this.orderMap = new TreeMap<>();
		this.orderMap.put(keyColumn, Order.ASCENDING);
		this.tablePrefix = tablePrefix;
		this.tableSuffix = tableSuffix;
		this.rowMapper = rowMapper;
		this.keyColumn = keyColumn;
		this.valueColumn = valueColumn;
		tableName = updatePrefixSuffix("%PREFIX%%SUFFIX% ");
		selectClause = keyColumn + ", " + valueColumn + " ";
		whereClauseByKey = "where " + keyColumn + " = ? ";
		inClauseByKey = "where " + keyColumn + " in ( :" + LIST_OF_NAMES + ") ";
		findAllQuery = "SELECT " + selectClause + "FROM " + tableName;
		findAllWhereClauseByKey = findAllQuery + whereClauseByKey;
		saveRow = "INSERT into " + tableName + "(" + keyColumn + ", " + valueColumn + ")" + "values (?, ?)";
		updateValue = "UPDATE " + tableName + "SET " + valueColumn + "= ? WHERE " + keyColumn + "= ?";
		countAll = "SELECT COUNT(*) FROM " + tableName;
		countByKey = "SELECT COUNT(*) FROM " + tableName + whereClauseByKey;
		findAllWhereInClause = findAllQuery + inClauseByKey;
		deleteFromTableClause = "DELETE FROM " + tableName;
		deleteFromTableByKey = deleteFromTableClause + whereClauseByKey;
	}

	public Iterable<D> findAllEntries() {
		return jdbcTemplate.query(findAllQuery, rowMapper);
	}

	private String updatePrefixSuffix(String base) {
		String updatedPrefix = StringUtils.replace(base, "%PREFIX%", tablePrefix);
		return StringUtils.replace(updatedPrefix, "%SUFFIX%", tableSuffix);
	}

	protected Page<D> queryForPageableResults(Pageable pageable, String selectClause, String tableName,
			String whereClause, Object[] queryParam, long totalCount) {
		// Possible performance improvement refactoring so factory isn't called everytime.
		SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
		factoryBean.setSelectClause(selectClause);
		factoryBean.setFromClause(tableName);
		if (StringUtils.hasText(whereClause)) {
			factoryBean.setWhereClause(whereClause);
		}
		factoryBean.setSortKeys(orderMap);
		factoryBean.setDataSource(dataSource);
		PagingQueryProvider pagingQueryProvider;
		try {
			pagingQueryProvider = factoryBean.getObject();
			pagingQueryProvider.init(dataSource);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		String query = pagingQueryProvider.getPageQuery(pageable);
		List<D> resultList = jdbcTemplate.query(query, queryParam, rowMapper);
		return new PageImpl<>(resultList, pageable, totalCount);
	}
}
