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

package org.springframework.cloud.dataflow.server.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.cloud.dataflow.server.repository.support.Order;
import org.springframework.cloud.dataflow.server.repository.support.PagingQueryProvider;
import org.springframework.cloud.dataflow.server.repository.support.SearchPageable;
import org.springframework.cloud.dataflow.server.repository.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract class for RDBMS based repositories.
 *
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
public abstract class AbstractRdbmsKeyValueRepository<D> implements PagingAndSortingRepository<D, String> {

	protected final String LIST_OF_NAMES = "listnames";

	protected final RowMapper<D> rowMapper;

	protected String keyColumn;

	protected String valueColumn;

	protected String selectClause;

	protected String tableName = "%PREFIX%%SUFFIX% ";

	protected String whereClauseByKey;

	protected String inClauseByKey;

	protected String saveRow;

	protected String tablePrefix;

	protected String tableSuffix;

	protected JdbcOperations jdbcTemplate;

	protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	protected DataSource dataSource;

	protected LinkedHashMap<String, Order> orderMap;

	private String findAllQuery;

	private String findAllWhereClauseByKey;

	private String countAll;

	private String countByKey;

	private String findAllWhereInClause = findAllQuery + whereClauseByKey;

	private String deleteFromTableClause = "DELETE FROM " + tableName;

	protected String deleteFromTableByKey = deleteFromTableClause + whereClauseByKey;

	public AbstractRdbmsKeyValueRepository(DataSource dataSource, String tablePrefix, String tableSuffix,
			RowMapper<D> rowMapper, String keyColumn, String valueColumn) {
		Assert.notNull(dataSource, "dataSource mut not be null");
		Assert.notNull(rowMapper, "rowMapper must not be null");
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		this.dataSource = dataSource;
		this.orderMap = new LinkedHashMap<>();
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
		countAll = "SELECT COUNT(*) FROM " + tableName;
		countByKey = "SELECT COUNT(*) FROM " + tableName + whereClauseByKey;
		findAllWhereInClause = findAllQuery + inClauseByKey;
		deleteFromTableClause = "DELETE FROM " + tableName;
		deleteFromTableByKey = deleteFromTableClause + whereClauseByKey;
	}

	@Override
	public Iterable<D> findAll(Sort sort) {
		Assert.notNull(sort, "sort must not be null");
		Iterator<Sort.Order> iter = sort.iterator();
		String query = findAllQuery + "ORDER BY ";

		while (iter.hasNext()) {
			Sort.Order order = iter.next();
			query = query + order.getProperty() + " " + order.getDirection();
			if (iter.hasNext()) {
				query = query + ", ";
			}
		}
		return jdbcTemplate.query(query, rowMapper);
	}

	public Page<D> search(SearchPageable searchPageable) {
		Assert.notNull(searchPageable, "searchPageable must not be null.");

		final StringBuilder whereClause = new StringBuilder("WHERE ");
		final List<String> params = new ArrayList<>();
		final Iterator<String> columnIterator = searchPageable.getColumns().iterator();

		while (columnIterator.hasNext()) {
			whereClause.append("lower(" + columnIterator.next()).append(") like ").append("lower(?)");
			params.add("%" + searchPageable.getSearchQuery() + "%");
			if (columnIterator.hasNext()) {
				whereClause.append(" OR ");
			}
		}
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(" ").append(selectClause);
		sql.append(" FROM ").append(tableName);
		sql.append(whereClause == null ? "" : whereClause);

		String query = sql.toString();
		List<D> result = jdbcTemplate.query(query, params.toArray(), rowMapper);
		return queryForPageableResults(searchPageable.getPageable(), selectClause, tableName, whereClause.toString(),
				params.toArray(), result.size());
	}

	@Override
	public Page<D> findAll(Pageable pageable) {
		Assert.notNull(pageable, "pageable must not be null");
		return queryForPageableResults(pageable, selectClause, tableName, null, new Object[] {}, count());
	}

	@Override
	public <S extends D> Iterable<S> save(Iterable<S> iterableDefinitions) {
		Assert.notNull(iterableDefinitions, "iterableDefinitions must not be null");
		for (S definition : iterableDefinitions) {
			save(definition);
		}
		return iterableDefinitions;
	}

	@Override
	public D findOne(String name) {
		Assert.hasText(name, "name must not be empty nor null");
		try {
			return jdbcTemplate.queryForObject(findAllWhereClauseByKey, rowMapper, name);
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public boolean exists(String name) {
		Assert.hasText(name, "name must not be empty nor null");
		boolean result;
		try {
			result = (jdbcTemplate.queryForObject(countByKey, new Object[] { name }, Long.class) > 0) ? true : false;
		}
		catch (EmptyResultDataAccessException e) {
			result = false;
		}
		return result;
	}

	@Override
	public Iterable<D> findAll() {
		return jdbcTemplate.query(findAllQuery, rowMapper);
	}

	@Override
	public Iterable<D> findAll(Iterable<String> names) {
		Assert.notNull(names, "names must not be null");
		List<String> listOfNames = new ArrayList<String>();
		for (String name : names) {
			listOfNames.add(name);
		}

		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue(LIST_OF_NAMES, listOfNames);

		return namedParameterJdbcTemplate.query(findAllWhereInClause, namedParameters, rowMapper);
	}

	@Override
	public long count() {
		try {
			return jdbcTemplate.queryForObject(countAll, new Object[] {}, Long.class);
		}
		catch (EmptyResultDataAccessException e) {
			return 0;
		}
	}

	@Override
	public void delete(String name) {
		Assert.hasText(name, "name must not be empty nor null");
		jdbcTemplate.update(deleteFromTableByKey, name);
	}

	@Override
	public void delete(Iterable<? extends D> definitions) {
		Assert.notNull(definitions, "definitions must not null");
		for (D definition : definitions) {
			delete(definition);
		}
	}

	@Override
	public void deleteAll() {
		jdbcTemplate.update(deleteFromTableClause);
	}

	private String updatePrefixSuffix(String base) {
		String updatedPrefix = StringUtils.replace(base, "%PREFIX%", tablePrefix);
		return StringUtils.replace(updatedPrefix, "%SUFFIX%", tableSuffix);
	}

	private Page<D> queryForPageableResults(Pageable pageable, String selectClause, String tableName,
			String whereClause, Object[] queryParam, long totalCount) {
		// FIXME Possible performance improvement refactoring so factory isn't called
		// every time.
		SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
		factoryBean.setSelectClause(selectClause);
		factoryBean.setFromClause(tableName);
		if (StringUtils.hasText(whereClause)) {
			factoryBean.setWhereClause(whereClause);
		}

		final Sort sort = pageable.getSort();
		final LinkedHashMap<String, Order> sortOrderMap = new LinkedHashMap<>();

		if (sort != null) {
			for (Sort.Order sortOrder : sort) {
				sortOrderMap.put(sortOrder.getProperty(), sortOrder.isAscending() ? Order.ASCENDING : Order.DESCENDING);
			}
		}

		if (!CollectionUtils.isEmpty(sortOrderMap)) {
			factoryBean.setSortKeys(sortOrderMap);
		}
		else {
			factoryBean.setSortKeys(this.orderMap);
		}

		factoryBean.setDataSource(this.dataSource);
		PagingQueryProvider pagingQueryProvider;
		try {
			pagingQueryProvider = factoryBean.getObject();
			pagingQueryProvider.init(this.dataSource);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		String query = pagingQueryProvider.getPageQuery(pageable);
		List<D> resultList = jdbcTemplate.query(query, queryParam, rowMapper);
		return new PageImpl<>(resultList, pageable, totalCount);
	}
}
