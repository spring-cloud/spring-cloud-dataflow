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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.sql.DataSource;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.server.repository.support.Order;
import org.springframework.cloud.dataflow.server.repository.support.PagingQueryProvider;
import org.springframework.cloud.dataflow.server.repository.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * RDBMS implementation of {@link TaskDefinitionRepository}.
 *
 * @author Glenn Renfro
 */
public class RdbmsTaskDefinitionRepository implements TaskDefinitionRepository {

	public static final String DEFAULT_TABLE_PREFIX = "TASK_";

	public static final String DEFINITION_NAME_COLUMN = "DEFINITION_NAME";

	public static final String DEFINITION_COLUMN = "DEFINITION";

	public static final String SELECT_CLAUSE = DEFINITION_NAME_COLUMN + ", " + DEFINITION_COLUMN + " ";

	public static final String FROM_CLAUSE = "%PREFIX%DEFINITION ";

	public static final String LIST_OF_NAMES = "listnames";

	public static final String TASK_DEFINITION_NAME_WHERE_CLAUSE = "where DEFINITION_NAME = ? ";

	public static final String TASK_DEFINITION_NAME_IN_CLAUSE = "where DEFINITION_NAME in ( :" + LIST_OF_NAMES + ") ";

	private static final String FIND_ALL_DEFINITIONS = "SELECT " + SELECT_CLAUSE + "FROM "
			+ FROM_CLAUSE;

	private static final String FIND_DEFINITIONS_WHERE_IN_CLAUSE = FIND_ALL_DEFINITIONS
			+ TASK_DEFINITION_NAME_IN_CLAUSE;

	private static final String SAVE_TASK_DEFINITION = "INSERT into %PREFIX%DEFINITION"
			+ "(DEFINITION_NAME, DEFINITION)"
			+ "values (?, ?)";

	private static final String TASK_DEFINITION_COUNT = "SELECT COUNT(*) FROM "
			+ FROM_CLAUSE;

	private static final String TASK_DEFINITION_COUNT_BY_NAME = "SELECT COUNT(*) FROM " + FROM_CLAUSE
			+ TASK_DEFINITION_NAME_WHERE_CLAUSE;

	private static final String FIND_TASK_DEFINITION_BY_NAME = FIND_ALL_DEFINITIONS + TASK_DEFINITION_NAME_WHERE_CLAUSE;

	private static final String DELETE_FROM_TASK_DEFINITION = "DELETE FROM " + FROM_CLAUSE;

	private static final String DELETE_FROM_TASK_DEFINITION_BY_NAME = DELETE_FROM_TASK_DEFINITION
			+ TASK_DEFINITION_NAME_WHERE_CLAUSE;

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	private String findAllDefinitionsQuery;

	private String fromClauseQuerySegment;

	private String saveTaskDefinitionStatement;

	private String taskDefinitionCountQuery;

	private String taskDefinitionCountByNameQuery;

	private String findTaskDefinitionByNameQuery;

	private String deleteFromTaskDefinitionStatement;

	private String deleteFromTaskDefinitionByNameStatement;

	private String findDefinitionsWhereInClause;

	private JdbcOperations jdbcTemplate;

	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private DataSource dataSource;

	private Map<String, Order> orderMap;

	public RdbmsTaskDefinitionRepository(DataSource dataSource) {
		Assert.notNull(dataSource);
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		this.dataSource = dataSource;
		this.orderMap = new TreeMap<>();
		this.orderMap.put("DEFINITION_NAME", Order.ASCENDING);
		updateQueryPrefixes();
	}

	@Override
	public Iterable<TaskDefinition> findAll(Sort sort) {
		Assert.notNull(sort, "sort must not be null");
		Iterator<Sort.Order> iter = sort.iterator();
		String query = findAllDefinitionsQuery  + "ORDER BY " ;

		while (iter.hasNext()) {
			Sort.Order order = iter.next();
			query = query + order.getProperty() + " " + order.getDirection();
			if (iter.hasNext()) {
				query = query + ", ";
			}
		}
		return jdbcTemplate.query(query, new TaskDefinitionRowMapper());
	}

	@Override
	public Page<TaskDefinition> findAll(Pageable pageable) {
		Assert.notNull(pageable, "pageable must not be null");
		return queryForPageableResults(pageable, SELECT_CLAUSE, fromClauseQuerySegment, null,
				new Object[]{}, count());
	}

	@Override
	public <S extends TaskDefinition> Iterable<S> save(Iterable<S> iterableDefinitions) {
		Assert.notNull(iterableDefinitions, "iterableDefinitions must not be null");
		for (S definition : iterableDefinitions) {
			save(definition);
		}
		return iterableDefinitions;
	}

	@Override
	public <S extends TaskDefinition> S save(S definition) {
		Assert.notNull(definition, "definition must not be null");
		if (exists(definition.getName())) {
			throw new DuplicateTaskException(
					String.format("Cannot register task %s because another one has already " +
									"been registered with the same name",
							definition.getName()));
		}

		Object[] insertParameters = new Object[]{definition.getName(), definition.getDslText()};
		jdbcTemplate.update(saveTaskDefinitionStatement, insertParameters,
				new int[]{Types.VARCHAR, Types.CLOB});

		return definition;
	}

	@Override
	public TaskDefinition findOne(String taskName) {
		Assert.hasText(taskName, "taskName must not be empty nor null");
		try {
			return jdbcTemplate.queryForObject(findTaskDefinitionByNameQuery,
					new TaskDefinitionRowMapper(),
					taskName);
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public boolean exists(String taskName) {
		Assert.hasText(taskName, "taskName must not be empty nor null");
		boolean result;
		try {
			result = (jdbcTemplate.queryForObject( taskDefinitionCountByNameQuery,
					new Object[]{taskName}, Long.class) > 0) ? true : false;
		}
		catch (EmptyResultDataAccessException e) {
			result = false;
		}
		return result;
	}

	@Override
	public Iterable<TaskDefinition> findAll() {
		return jdbcTemplate.query(findAllDefinitionsQuery, new TaskDefinitionRowMapper());
	}

	@Override
	public Iterable<TaskDefinition> findAll(Iterable<String> taskNames) {
		Assert.notNull(taskNames, "taskNames must not be null");
		List<String> listOfNames = new ArrayList<String>();
		for (String name : taskNames) {
			listOfNames.add(name);
		}

		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue(LIST_OF_NAMES, listOfNames);

		return namedParameterJdbcTemplate.query(findDefinitionsWhereInClause,
				namedParameters, new TaskDefinitionRowMapper());
	}

	@Override
	public long count() {
		try {
			return jdbcTemplate.queryForObject(taskDefinitionCountQuery, new Object[]{}, Long.class);
		}
		catch (EmptyResultDataAccessException e) {
			return 0;
		}
	}

	@Override
	public void delete(String taskName) {
		Assert.hasText(taskName, "taskName must not be empty nor null");
		jdbcTemplate.update(deleteFromTaskDefinitionByNameStatement, taskName);
	}

	@Override
	public void delete(TaskDefinition definition) {
		Assert.notNull(definition, "definition must not null");
		delete(definition.getName());
	}

	@Override
	public void delete(Iterable<? extends TaskDefinition> definitions) {
		Assert.notNull(definitions, "definitions must not null");
		for (TaskDefinition definition : definitions) {
			delete(definition);
		}
	}

	@Override
	public void deleteAll() {
		jdbcTemplate.update(deleteFromTaskDefinitionStatement);
	}

	public void setTablePrefix(String tablePrefix) {
		Assert.notNull(tablePrefix, "tablePrefix must not be null");
		this.tablePrefix = tablePrefix;
		updateQueryPrefixes();
	}

	private void updateQueryPrefixes() {
		findAllDefinitionsQuery = prefixSql(FIND_ALL_DEFINITIONS);
		fromClauseQuerySegment = prefixSql(FROM_CLAUSE);
		saveTaskDefinitionStatement = prefixSql(SAVE_TASK_DEFINITION);
		taskDefinitionCountQuery = prefixSql(TASK_DEFINITION_COUNT);
		taskDefinitionCountByNameQuery = prefixSql(TASK_DEFINITION_COUNT_BY_NAME);
		findTaskDefinitionByNameQuery = prefixSql(FIND_TASK_DEFINITION_BY_NAME);
		deleteFromTaskDefinitionStatement = prefixSql(DELETE_FROM_TASK_DEFINITION);
		deleteFromTaskDefinitionByNameStatement = prefixSql(DELETE_FROM_TASK_DEFINITION_BY_NAME);
		findDefinitionsWhereInClause = prefixSql(FIND_DEFINITIONS_WHERE_IN_CLAUSE);

	}

	private String prefixSql(String base) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}

	private Page<TaskDefinition> queryForPageableResults(Pageable pageable,
			String selectClause,
			String fromClause,
			String whereClause,
			Object[] queryParam,
			long totalCount) {
		//Possible performance improvement refactoring so factory isn't called everytime.
		SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
		factoryBean.setSelectClause(selectClause);
		factoryBean.setFromClause(fromClause);
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
		List<TaskDefinition> resultList = jdbcTemplate.query(
				query,
				queryParam,
				new TaskDefinitionRowMapper());
		return new PageImpl<>(resultList, pageable, totalCount);
	}

	/**
	 * Re-usable mapper for {@link TaskExecution} instances.
	 */
	private final class TaskDefinitionRowMapper implements RowMapper<TaskDefinition> {

		public TaskDefinitionRowMapper() {
		}

		@Override
		public TaskDefinition mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new TaskDefinition(
					rs.getString(DEFINITION_NAME_COLUMN), rs.getString(DEFINITION_COLUMN));
		}
	}
}
