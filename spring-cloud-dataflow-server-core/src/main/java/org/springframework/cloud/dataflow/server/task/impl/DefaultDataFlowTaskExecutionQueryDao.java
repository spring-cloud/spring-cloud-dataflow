/*
 * Copyright 2023-2023 the original author or authors.
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
package org.springframework.cloud.dataflow.server.task.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.database.Order;
import org.springframework.cloud.dataflow.core.ThinTaskExecution;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExecutionQueryDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.database.PagingQueryProvider;
import org.springframework.cloud.task.repository.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Implementation of the {@link DataflowTaskExecutionQueryDao}.
 *
 * @author Corneil du Plessis
 */

public class DefaultDataFlowTaskExecutionQueryDao implements DataflowTaskExecutionQueryDao {
	private final static Logger logger = LoggerFactory.getLogger(DefaultDataFlowTaskExecutionQueryDao.class);

	/**
	 * SELECT clause for task execution.
	 */
	public static final String SELECT_CLAUSE = "TASK_EXECUTION_ID, "
			+ "START_TIME, END_TIME, TASK_NAME, EXIT_CODE, "
			+ "EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, "
			+ "EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID";

	/**
	 * FROM clause for task execution.
	 */
	public static final String FROM_CLAUSE = "TASK_EXECUTION";

	private static final String FIND_TASK_ARGUMENTS = "SELECT TASK_EXECUTION_ID, "
			+ "TASK_PARAM from TASK_EXECUTION_PARAMS where TASK_EXECUTION_ID = :taskExecutionId";

	private static final String FIND_CTR_STATUS = "SELECT T.TASK_EXECUTION_ID as TASK_EXECUTION_ID, J.EXIT_CODE as CTR_STATUS" +
			" from TASK_EXECUTION T" +
			" JOIN TASK_TASK_BATCH TB ON TB.TASK_EXECUTION_ID = T.TASK_EXECUTION_ID" +
			" JOIN BATCH_JOB_EXECUTION J ON J.JOB_EXECUTION_ID = TB.JOB_EXECUTION_ID" +
			" WHERE T.TASK_EXECUTION_ID in (:taskExecutionIds) " +
			"    AND (select count(*) from TASK_EXECUTION CT" + // it is the parent of one or more tasks
			"        where CT.PARENT_EXECUTION_ID = T.TASK_EXECUTION_ID) > 0";

	private static final String GET_EXECUTIONS = "SELECT " + SELECT_CLAUSE +
			" from TASK_EXECUTION";

	private static final String GET_EXECUTION_BY_ID = GET_EXECUTIONS +
			" where TASK_EXECUTION_ID = :taskExecutionId and SCHEMA_TARGET = :schemaTarget";

	private final static String GET_CHILD_EXECUTION_BY_ID = GET_EXECUTIONS +
			" where PARENT_EXECUTION_ID = :taskExecutionId" +
			" and (SELECT COUNT(*) FROM TASK_EXECUTION_PARAMS P " +
			"           WHERE P.TASK_EXECUTION_ID=TASK_EXECUTION_ID " +
			"             AND P.SCHEMA_TARGET=SCHEMA_TARGET" +
			"             AND P.TASK_PARAM = :schemaTarget) > 0";

	private final static String GET_CHILD_EXECUTION_BY_IDS = GET_EXECUTIONS +
			" where PARENT_EXECUTION_ID IN (:taskExecutionIds)" +
			" and (SELECT COUNT(*) FROM TASK_EXECUTION_PARAMS P " +
			"           WHERE P.TASK_EXECUTION_ID=TASK_EXECUTION_ID " +
			"            ) > 0";

	private static final String GET_EXECUTION_BY_EXTERNAL_EXECUTION_ID = GET_EXECUTIONS +
			" where EXTERNAL_EXECUTION_ID = :externalExecutionId and TASK_NAME = :taskName";

	private static final String GET_EXECUTIONS_BY_NAME_COMPLETED = GET_EXECUTIONS +
			" where TASK_NAME = :taskName AND END_TIME IS NOT NULL";

	private static final String GET_EXECUTIONS_BY_NAME = GET_EXECUTIONS +
			" where TASK_NAME = :taskName";

	private static final String GET_EXECUTIONS_COMPLETED = GET_EXECUTIONS +
			" where END_TIME IS NOT NULL";

	private static final String GET_EXECUTION_BY_NAME_COMPLETED_BEFORE_END_TIME = GET_EXECUTIONS +
			" where TASK_NAME = :taskName AND END_TIME IS NOT NULL AND END_TIME < :endTime";

	private static final String GET_EXECUTIONS_COMPLETED_BEFORE_END_TIME = GET_EXECUTIONS +
			" where END_TIME IS NOT NULL AND END_TIME < :endTime";

	private static final String TASK_EXECUTION_COUNT = "SELECT COUNT(*) FROM "
			+ "TASK_EXECUTION ";

	private static final String TASK_EXECUTION_COUNT_BY_NAME = "SELECT COUNT(*) FROM "
			+ "TASK_EXECUTION where TASK_NAME = :taskName";

	private static final String COMPLETED_TASK_EXECUTION_COUNT = "SELECT COUNT(*) FROM "
			+ "TASK_EXECUTION WHERE END_TIME IS NOT NULL";

	private static final String COMPLETED_TASK_EXECUTION_COUNT_AND_BEFORE_END_TIME = "SELECT COUNT(*) FROM "
			+ "TASK_EXECUTION WHERE END_TIME IS NOT NULL AND END_TIME < :endTime";

	private static final String COMPLETED_TASK_EXECUTION_COUNT_BY_NAME = "SELECT COUNT(*) FROM "
			+ "TASK_EXECUTION where TASK_NAME = :taskName AND END_TIME IS NOT NULL ";

	private static final String COMPLETED_TASK_EXECUTION_COUNT_BY_NAME_AND_BEFORE_END_TIME = "SELECT COUNT(*) FROM "
			+ "TASK_EXECUTION where TASK_NAME = :taskName AND END_TIME IS NOT NULL AND END_TIME < :endTime ";


	private static final String RUNNING_TASK_EXECUTION_COUNT_BY_NAME = "SELECT COUNT(*) FROM "
			+ "TASK_EXECUTION where TASK_NAME = :taskName AND END_TIME IS NULL ";

	private static final String RUNNING_TASK_EXECUTION_COUNT = "SELECT COUNT(*) FROM "
			+ "TASK_EXECUTION where END_TIME IS NULL ";

	private static final String LAST_TASK_EXECUTIONS_BY_TASK_NAMES = "select TE2.* from ("
			+ "select MAX(TE.TASK_EXECUTION_ID) as TASK_EXECUTION_ID, TE.TASK_NAME, TE.START_TIME from ("
			+ "select TASK_NAME, MAX(START_TIME) as START_TIME"
			+ "      FROM TASK_EXECUTION where TASK_NAME in (:taskNames)"
			+ "      GROUP BY TASK_NAME) TE_MAX"
			+ " inner join TASK_EXECUTION TE ON TE.TASK_NAME = TE_MAX.TASK_NAME AND TE.START_TIME = TE_MAX.START_TIME"
			+ " group by TE.TASK_NAME, TE.START_TIME" + ") TE1"
			+ " inner join TASK_EXECUTION TE2 ON TE1.TASK_EXECUTION_ID = TE2.TASK_EXECUTION_ID AND TE1.SCHEMA_TARGET = TE2.SCHEMA_TARGET"
			+ " order by TE2.START_TIME DESC, TE2.TASK_EXECUTION_ID DESC";

	private static final Set<String> validSortColumns = new HashSet<>(10);

	static {
		validSortColumns.add("TASK_EXECUTION_ID");
		validSortColumns.add("START_TIME");
		validSortColumns.add("END_TIME");
		validSortColumns.add("TASK_NAME");
		validSortColumns.add("EXIT_CODE");
		validSortColumns.add("EXIT_MESSAGE");
		validSortColumns.add("ERROR_MESSAGE");
		validSortColumns.add("LAST_UPDATED");
		validSortColumns.add("EXTERNAL_EXECUTION_ID");
		validSortColumns.add("PARENT_EXECUTION_ID");
		validSortColumns.add("SCHEMA_TARGET");
	}

	private final NamedParameterJdbcTemplate jdbcTemplate;

	private final DataSource dataSource;

	private final LinkedHashMap<String, Order> orderMap;

	/**
	 * Initializes the DefaultDataFlowJobExecutionDao.
	 *
	 * @param dataSource used by the dao to execute queries and update the tables.
	 */
	public DefaultDataFlowTaskExecutionQueryDao(DataSource dataSource) {
		Assert.notNull(dataSource, "The dataSource must not be null.");
		this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		this.dataSource = dataSource;
		this.orderMap = new LinkedHashMap<>();
		this.orderMap.put("START_TIME", Order.DESCENDING);
		this.orderMap.put("TASK_EXECUTION_ID", Order.DESCENDING);
	}

	@Override
	public TaskExecution geTaskExecutionByExecutionId(String externalExecutionId, String taskName) {
		final SqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("externalExecutionId", externalExecutionId)
				.addValue("taskName", taskName);

		try {
			return this.jdbcTemplate.queryForObject(
					GET_EXECUTION_BY_EXTERNAL_EXECUTION_ID,
					queryParameters,
					new CompositeTaskExecutionRowMapper()
			);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public TaskExecution getTaskExecution(long executionId) {
		final SqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("taskExecutionId", executionId, Types.BIGINT);

		try {
			return this.jdbcTemplate.queryForObject(
					GET_EXECUTION_BY_ID,
					queryParameters,
					new CompositeTaskExecutionRowMapper()
			);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public List<TaskExecution> findChildTaskExecutions(long executionId) {
		final SqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("taskExecutionId", executionId, Types.BIGINT);
		try {
			return this.jdbcTemplate.query(
					GET_CHILD_EXECUTION_BY_ID,
					queryParameters,
					new CompositeTaskExecutionRowMapper()
			);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public List<TaskExecution> findChildTaskExecutions(Collection<Long> parentIds) {
		final SqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("taskExecutionIds", parentIds);
		try {
			return this.jdbcTemplate.query(
				GET_CHILD_EXECUTION_BY_IDS,
				queryParameters,
					new CompositeTaskExecutionRowMapper()
			);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public List<TaskExecution> findTaskExecutions(String taskName, boolean completed) {
		if (StringUtils.hasLength(taskName)) {
			final SqlParameterSource queryParameters = new MapSqlParameterSource()
					.addValue("taskName", taskName);
			String query = completed ? GET_EXECUTIONS_BY_NAME_COMPLETED : GET_EXECUTIONS_BY_NAME;
			return this.jdbcTemplate.query(query, queryParameters, new CompositeTaskExecutionRowMapper());
		} else {
			return this.jdbcTemplate.query(completed ? GET_EXECUTIONS_COMPLETED : GET_EXECUTIONS, Collections.emptyMap(), new CompositeTaskExecutionRowMapper());
		}
	}

	@Override
	public List<TaskExecution> findTaskExecutionsBeforeEndTime(String taskName, @NonNull Date endTime) {
		final SqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("taskName", taskName)
				.addValue("endTime", endTime);
		String query;
		query = taskName.isEmpty() ? GET_EXECUTIONS_COMPLETED_BEFORE_END_TIME : GET_EXECUTION_BY_NAME_COMPLETED_BEFORE_END_TIME;
		return this.jdbcTemplate.query(query, queryParameters, new CompositeTaskExecutionRowMapper());
	}

	@Override
	public long getTaskExecutionCountByTaskName(String taskName) {
		Long count;
		if (StringUtils.hasText(taskName)) {
			final SqlParameterSource queryParameters = new MapSqlParameterSource()
					.addValue("taskName", taskName, Types.VARCHAR);

			try {
				count = this.jdbcTemplate.queryForObject(TASK_EXECUTION_COUNT_BY_NAME, queryParameters, Long.class);
			} catch (EmptyResultDataAccessException e) {
				count = 0L;
			}
		} else {
			count = this.jdbcTemplate.queryForObject(TASK_EXECUTION_COUNT, Collections.emptyMap(), Long.class);
		}
		return count != null ? count : 0L;
	}

	@Override
	public long getCompletedTaskExecutionCountByTaskName(String taskName) {
		Long count;
		if (StringUtils.hasText(taskName)) {
			final SqlParameterSource queryParameters = new MapSqlParameterSource()
					.addValue("taskName", taskName, Types.VARCHAR);

			try {
				count = this.jdbcTemplate.queryForObject(COMPLETED_TASK_EXECUTION_COUNT_BY_NAME, queryParameters, Long.class);
			} catch (EmptyResultDataAccessException e) {
				count = 0L;
			}
		} else {
			count = this.jdbcTemplate.queryForObject(COMPLETED_TASK_EXECUTION_COUNT, Collections.emptyMap(), Long.class);
		}
		return count != null ? count : 0L;
	}

	@Override
	public long getCompletedTaskExecutionCountByTaskNameAndBeforeDate(String taskName, @NonNull Date endTime) {
		Long count;
		if (StringUtils.hasText(taskName)) {
			final SqlParameterSource queryParameters = new MapSqlParameterSource()
					.addValue("taskName", taskName, Types.VARCHAR)
					.addValue("endTime", endTime, Types.DATE);

			try {
				count = this.jdbcTemplate.queryForObject(COMPLETED_TASK_EXECUTION_COUNT_BY_NAME_AND_BEFORE_END_TIME, queryParameters, Long.class);
			} catch (EmptyResultDataAccessException e) {
				count = 0L;
			}
		} else {
			final SqlParameterSource queryParameters = new MapSqlParameterSource()
					.addValue("endTime", endTime, Types.DATE);
			count = this.jdbcTemplate.queryForObject(COMPLETED_TASK_EXECUTION_COUNT_AND_BEFORE_END_TIME, queryParameters, Long.class);
		}
		return count != null ? count : 0L;
	}

	@Override
	public long getRunningTaskExecutionCountByTaskName(String taskName) {
		Long count;
		if (StringUtils.hasText(taskName)) {
			final SqlParameterSource queryParameters = new MapSqlParameterSource()
					.addValue("taskName", taskName, Types.VARCHAR);

			try {
				logger.debug("getRunningTaskExecutionCountByTaskName:{}:sql={}", taskName, RUNNING_TASK_EXECUTION_COUNT_BY_NAME);
				count = this.jdbcTemplate.queryForObject(RUNNING_TASK_EXECUTION_COUNT_BY_NAME, queryParameters, Long.class);
			} catch (EmptyResultDataAccessException e) {
				count = 0L;
			}
		} else {
			logger.debug("getRunningTaskExecutionCountByTaskName:{}:sql={}", taskName, RUNNING_TASK_EXECUTION_COUNT);
			count = this.jdbcTemplate.queryForObject(RUNNING_TASK_EXECUTION_COUNT, Collections.emptyMap(), Long.class);

		}
		return count != null ? count : 0L;
	}

	@Override
	public long getRunningTaskExecutionCount() {
		try {
			final SqlParameterSource queryParameters = new MapSqlParameterSource();
			Long result = this.jdbcTemplate.queryForObject(RUNNING_TASK_EXECUTION_COUNT, queryParameters, Long.class);
			return result != null ? result : 0L;
		} catch (EmptyResultDataAccessException e) {
			return 0;
		}
	}

	@Override
	public List<TaskExecution> getLatestTaskExecutionsByTaskNames(String... taskNames) {
		Assert.notEmpty(taskNames, "At least 1 task name must be provided.");
		final List<String> taskNamesAsList = new ArrayList<>();

		for (String taskName : taskNames) {
			if (StringUtils.hasText(taskName)) {
				taskNamesAsList.add(taskName);
			}
		}

		Assert.isTrue(taskNamesAsList.size() == taskNames.length, String.format(
				"Task names must not contain any empty elements but %s of %s were empty or null.",
				taskNames.length - taskNamesAsList.size(), taskNames.length));

		try {
			final Map<String, List<String>> paramMap = Collections
					.singletonMap("taskNames", taskNamesAsList);
			return this.jdbcTemplate.query(LAST_TASK_EXECUTIONS_BY_TASK_NAMES, paramMap, new CompositeTaskExecutionRowMapper());
		} catch (EmptyResultDataAccessException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public TaskExecution getLatestTaskExecutionForTaskName(String taskName) {
		Assert.hasText(taskName, "The task name must not be empty.");
		final List<TaskExecution> taskExecutions = this
				.getLatestTaskExecutionsByTaskNames(taskName);
		if (taskExecutions.isEmpty()) {
			return null;
		} else if (taskExecutions.size() == 1) {
			return taskExecutions.get(0);
		} else {
			throw new IllegalStateException(
					"Only expected a single TaskExecution but received "
							+ taskExecutions.size());
		}
	}

	@Override
	public long getTaskExecutionCount() {
		try {
			Long count = this.jdbcTemplate.queryForObject(TASK_EXECUTION_COUNT, new MapSqlParameterSource(), Long.class);
			return count != null ? count : 0;
		} catch (EmptyResultDataAccessException e) {
			return 0;
		}
	}

	@Override
	public Page<TaskExecution> findAll(Pageable pageable) {
		return queryForPageableResults(pageable, SELECT_CLAUSE, FROM_CLAUSE, null,
				new MapSqlParameterSource(), getTaskExecutionCount());
	}

	private Page<TaskExecution> queryForPageableResults(
			Pageable pageable,
			String selectClause,
			String fromClause,
			String whereClause,
			MapSqlParameterSource queryParameters,
			long totalCount
	) {
		SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
		factoryBean.setSelectClause(selectClause);
		factoryBean.setFromClause(fromClause);
		if (StringUtils.hasText(whereClause)) {
			factoryBean.setWhereClause(whereClause);
		}
		final Sort sort = pageable.getSort();
		final LinkedHashMap<String, Order> sortOrderMap = new LinkedHashMap<>();

		if (sort != null) {
			for (Sort.Order sortOrder : sort) {
				if (validSortColumns.contains(sortOrder.getProperty().toUpperCase())) {
					sortOrderMap.put(sortOrder.getProperty(),
							sortOrder.isAscending() ? Order.ASCENDING : Order.DESCENDING);
				} else {
					throw new IllegalArgumentException(
							String.format("Invalid sort option selected: %s", sortOrder.getProperty()));
				}
			}
		}

		if (!CollectionUtils.isEmpty(sortOrderMap)) {
			factoryBean.setSortKeys(sortOrderMap);
		} else {
			factoryBean.setSortKeys(this.orderMap);
		}

		factoryBean.setDataSource(this.dataSource);
		PagingQueryProvider pagingQueryProvider;
		try {
			pagingQueryProvider = factoryBean.getObject();
			pagingQueryProvider.init(this.dataSource);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		String query = pagingQueryProvider.getPageQuery(pageable);
		List<TaskExecution> resultList = this.jdbcTemplate.query(query,
				queryParameters, new CompositeTaskExecutionRowMapper());
		return new PageImpl<>(resultList, pageable, totalCount);
	}


	private class CompositeTaskExecutionRowMapper implements RowMapper<TaskExecution> {

		private CompositeTaskExecutionRowMapper() {
		}

		@Override
		public TaskExecution mapRow(ResultSet rs, int rowNum) throws SQLException {
			long id = rs.getLong("TASK_EXECUTION_ID");
			Long parentExecutionId = rs.getLong("PARENT_EXECUTION_ID");
			if (rs.wasNull()) {
				parentExecutionId = null;
			}
			Timestamp endTimestamp = rs.getTimestamp("END_TIME");
			Timestamp startTime = rs.getTimestamp("START_TIME");
			return new TaskExecution(id,
					getNullableExitCode(rs),
					rs.getString("TASK_NAME"),
					startTime != null ? startTime.toLocalDateTime() : null,
					(endTimestamp != null) ? endTimestamp.toLocalDateTime() : null,
					rs.getString("EXIT_MESSAGE"),
					getTaskArguments(id),
					rs.getString("ERROR_MESSAGE"),
					rs.getString("EXTERNAL_EXECUTION_ID"),
					parentExecutionId);
		}

		private Integer getNullableExitCode(ResultSet rs) throws SQLException {
			int exitCode = rs.getInt("EXIT_CODE");
			return !rs.wasNull() ? exitCode : null;
		}
	}

	private List<String> getTaskArguments(long taskExecutionId) {
		final List<String> params = new ArrayList<>();
		RowCallbackHandler handler = rs -> params.add(rs.getString(2));
		this.jdbcTemplate.query(
				FIND_TASK_ARGUMENTS,
				new MapSqlParameterSource("taskExecutionId", taskExecutionId),
				handler);
		return params;
	}

	@Override
	public void populateCtrStatus(Collection<ThinTaskExecution> thinTaskExecutions) {
		if(!thinTaskExecutions.isEmpty()) {
			Map<Long, ThinTaskExecution> taskExecutionMap = thinTaskExecutions.stream()
				.collect(Collectors.toMap(ThinTaskExecution::getExecutionId, Function.identity()));
			String ids = taskExecutionMap.keySet().stream().map(Object::toString).collect(Collectors.joining(","));
			String sql = FIND_CTR_STATUS.replace(":taskExecutionIds", ids);
			jdbcTemplate.query(sql, rs -> {
				Long id = rs.getLong("TASK_EXECUTION_ID");
				String ctrStatus = rs.getString("CTR_STATUS");
				logger.debug("populateCtrStatus:{}={}", id, ctrStatus);
				ThinTaskExecution execution = taskExecutionMap.get(id);
				Assert.notNull(execution, "Expected TaskExecution for " + id + " from " + ids);
				execution.setCtrTaskStatus(ctrStatus);
			});
		}
	}
}
