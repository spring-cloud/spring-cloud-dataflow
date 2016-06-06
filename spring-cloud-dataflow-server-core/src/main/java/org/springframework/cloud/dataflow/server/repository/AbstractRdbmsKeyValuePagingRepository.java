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

import java.util.ArrayList;
import java.util.Iterator;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract class for RDBMS based repositories.
 *
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public abstract class AbstractRdbmsKeyValuePagingRepository<D> extends AbstractRdbmsKeyValueRepository<D> implements PagingAndSortingRepository<D, String> {


	public AbstractRdbmsKeyValuePagingRepository(DataSource dataSource, String tablePrefix, String tableSuffix,
												 RowMapper<D> rowMapper, String keyColumn, String valueColumn) {
		super(dataSource, tablePrefix, tableSuffix, rowMapper, keyColumn, valueColumn);
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

	@Override
	public Page<D> findAll(Pageable pageable) {
		Assert.notNull(pageable, "pageable must not be null");
		return queryForPageableResults(pageable, selectClause, tableName, null, new Object[] {}, count());
	}


	public <S extends D> Iterable<S> save(Iterable<S> iterableDefinitions) {
		Assert.notNull(iterableDefinitions, "iterableDefinitions must not be null");
		for (S definition : iterableDefinitions) {
			save(definition);
		}
		return iterableDefinitions;
	}

	public Iterable<D> findAll() {
		return jdbcTemplate.query(findAllQuery, rowMapper);
	}

	@Override
	public final Iterable<D> findAll(Iterable<String> names) {
		Assert.notNull(names, "names must not be null");
		List<String> listOfNames = new ArrayList<String>();
		for (String name : names) {
			listOfNames.add(name);
		}

		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue(LIST_OF_NAMES, listOfNames);

		return namedParameterJdbcTemplate.query(findAllWhereInClause, namedParameters, rowMapper);
	}

	public void delete(Iterable<? extends D> definitions) {
		Assert.notNull(definitions, "definitions must not null");
		for (D definition : definitions) {
			delete(definition);
		}
	}

	public void deleteAll() {
		jdbcTemplate.update(deleteFromTableClause);
	}
}
