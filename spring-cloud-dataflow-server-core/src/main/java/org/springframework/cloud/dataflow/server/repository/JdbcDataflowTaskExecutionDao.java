/*
 * Copyright 2017-2021 the original author or authors.
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

import java.sql.Types;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.server.repository.support.SchemaUtilities;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Manages Task Execution information using a JDBC DataSource. Mirrors the {@link JdbcTaskExecutionDao}
 * but contains Spring Cloud Data Flow specific operations. This functionality might
 * be migrated to Spring Cloud Task itself eventually.
 *
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
public class JdbcDataflowTaskExecutionDao implements DataflowTaskExecutionDao {
	private final static Logger logger = LoggerFactory.getLogger(JdbcDataflowTaskExecutionDao.class);
	private final NamedParameterJdbcTemplate jdbcTemplate;

	private static final String DELETE_TASK_EXECUTIONS = "DELETE FROM %PREFIX%EXECUTION "
			+ "WHERE TASK_EXECUTION_ID in (:taskExecutionIds)";

	private static final String DELETE_TASK_EXECUTION_PARAMS = "DELETE FROM %PREFIX%EXECUTION_PARAMS "
			+ "WHERE TASK_EXECUTION_ID in (:taskExecutionIds)";

	private static final String DELETE_TASK_TASK_BATCH = "DELETE FROM %PREFIX%TASK_BATCH "
			+ "WHERE TASK_EXECUTION_ID in (:taskExecutionIds)";

	private static final String SELECT_CHILD_TASK_EXECUTION_IDS = "SELECT TASK_EXECUTION_ID FROM %PREFIX%EXECUTION "
			+ "WHERE PARENT_EXECUTION_ID in (:parentTaskExecutionIds)";

	private static final String FIND_TASK_EXECUTION_IDS_BY_TASK_NAME = "SELECT TASK_EXECUTION_ID "
			+ "from %PREFIX%EXECUTION where TASK_NAME = :taskName";

	private static final String GET_COMPLETED_TASK_EXECUTIONS_COUNT = "SELECT COUNT(TASK_EXECUTION_ID) AS count "
			+ "from %PREFIX%EXECUTION where END_TIME IS NOT NULL";

	private static final String GET_ALL_TASK_EXECUTIONS_COUNT = "SELECT COUNT(TASK_EXECUTION_ID) AS count "
			+ "from %PREFIX%EXECUTION";

	private static final String GET_COMPLETED_TASK_EXECUTIONS_COUNT_BY_TASK_NAME = "SELECT COUNT(TASK_EXECUTION_ID) AS count "
			+ "from %PREFIX%EXECUTION where END_TIME IS NOT NULL AND TASK_NAME = :taskName";

	private static final String GET_ALL_TASK_EXECUTIONS_COUNT_BY_TASK_NAME = "SELECT COUNT(TASK_EXECUTION_ID) AS count "
			+ "from %PREFIX%EXECUTION where TASK_NAME = :taskName";

	private static final String FIND_ALL_COMPLETED_TASK_EXECUTION_IDS = "SELECT TASK_EXECUTION_ID "
			+ "from %PREFIX%EXECUTION where END_TIME IS NOT NULL";

	private static final String FIND_ALL_TASK_EXECUTION_IDS = "SELECT TASK_EXECUTION_ID "
			+ "from %PREFIX%EXECUTION";

	private static final String FIND_ALL_COMPLETED_TASK_EXECUTION_IDS_BY_TASK_NAME = "SELECT TASK_EXECUTION_ID "
			+ "from %PREFIX%EXECUTION where END_TIME IS NOT NULL AND TASK_NAME = :taskName";

	private static final String FIND_ALL_TASK_EXECUTION_IDS_BY_TASK_NAME = "SELECT TASK_EXECUTION_ID "
			+ "from %PREFIX%EXECUTION where TASK_NAME = :taskName";


	private final TaskProperties taskProperties;

	/**
	 * @param dataSource  used by the dao to execute queries and updates the tables.
	 * @param taskProperties the {@link TaskProperties} to use for this dao.
	 */
	public JdbcDataflowTaskExecutionDao(DataSource dataSource, TaskProperties taskProperties) {
		Assert.notNull(dataSource, "The dataSource must not be null.");
		Assert.notNull(taskProperties, "taskProperties must not be null");
		this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		this.taskProperties = taskProperties;
	}

	@Override
	public int deleteTaskExecutionsByTaskExecutionIds(Set<Long> taskExecutionIds) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("taskExecutionIds", taskExecutionIds);
		final String query = SchemaUtilities.getQuery(DELETE_TASK_EXECUTIONS, this.taskProperties.getTablePrefix());
		return this.jdbcTemplate.update(query, queryParameters);
	}

	@Override
	public int deleteTaskExecutionParamsByTaskExecutionIds(Set<Long> taskExecutionIds) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("taskExecutionIds", taskExecutionIds);
		final String query = SchemaUtilities.getQuery(DELETE_TASK_EXECUTION_PARAMS, this.taskProperties.getTablePrefix());
		return this.jdbcTemplate.update(query, queryParameters);
	}

	@Override
	public int deleteTaskTaskBatchRelationshipsByTaskExecutionIds(Set<Long> taskExecutionIds) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("taskExecutionIds", taskExecutionIds);
		final String query = SchemaUtilities.getQuery(DELETE_TASK_TASK_BATCH, this.taskProperties.getTablePrefix());
		return this.jdbcTemplate.update(query, queryParameters);
	}



	@Override
	public Set<Long> findChildTaskExecutionIds(Set<Long> taskExecutionIds) {
		logger.debug("findChildTaskExecutionIds:{}", taskExecutionIds);
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("parentTaskExecutionIds", taskExecutionIds);

		Set<Long> childTaskExecutionIds;
		try {
			childTaskExecutionIds = this.jdbcTemplate.query(
					SchemaUtilities.getQuery(SELECT_CHILD_TASK_EXECUTION_IDS, this.taskProperties.getTablePrefix()),
					queryParameters,
					resultSet -> {

							Set<Long> jobExecutionIds = new TreeSet<>();

							while (resultSet.next()) {
								jobExecutionIds
										.add(resultSet.getLong("TASK_EXECUTION_ID"));
							}

							return jobExecutionIds;
					});
			Assert.notNull(childTaskExecutionIds, "Expected childTaskExecutionIds");
		}
		catch (DataAccessException e) {
			childTaskExecutionIds = Collections.emptySet();
		}
		if (!childTaskExecutionIds.isEmpty()) {
			Set<Long> newChildren = new HashSet<>(childTaskExecutionIds);
			newChildren.removeAll(taskExecutionIds);
			if(!newChildren.isEmpty()) {
				childTaskExecutionIds.addAll(this.findChildTaskExecutionIds(newChildren));
			}
		}
		logger.debug("findChildTaskExecutionIds:childTaskExecutionIds={}", childTaskExecutionIds);
		return childTaskExecutionIds;
	}

	@Override
	public Set<Long> getTaskExecutionIdsByTaskName(String taskName) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("taskName", taskName, Types.VARCHAR);

		try {
			return this.jdbcTemplate.query(
					SchemaUtilities.getQuery(FIND_TASK_EXECUTION_IDS_BY_TASK_NAME, this.taskProperties.getTablePrefix()),
					queryParameters,
					resultSet -> {
							Set<Long> taskExecutionIds = new TreeSet<>();

							while (resultSet.next()) {
								taskExecutionIds
										.add(resultSet.getLong("TASK_EXECUTION_ID"));
							}
							return taskExecutionIds;
					});
		}
		catch (DataAccessException e) {
			return Collections.emptySet();
		}
	}

	@Override
	public Integer getAllTaskExecutionsCount(boolean onlyCompleted, String taskName) {
		String QUERY;
		MapSqlParameterSource queryParameters = new MapSqlParameterSource();
		if (StringUtils.hasText(taskName)) {
			queryParameters.addValue("taskName", taskName, Types.VARCHAR);
			QUERY = (onlyCompleted) ? GET_COMPLETED_TASK_EXECUTIONS_COUNT_BY_TASK_NAME : GET_ALL_TASK_EXECUTIONS_COUNT_BY_TASK_NAME;
		}
		else {
			QUERY = (onlyCompleted) ? GET_COMPLETED_TASK_EXECUTIONS_COUNT: GET_ALL_TASK_EXECUTIONS_COUNT;
		}
		try {
			return this.jdbcTemplate.query(
					SchemaUtilities.getQuery(QUERY, this.taskProperties.getTablePrefix()),
					queryParameters,
					resultSet -> {
							if (resultSet.next()) {
								return resultSet.getInt("count");
							}
						return 0;
					});
		}
		catch (DataAccessException e) {
			return 0;
		}
	}

	@Override
	public Set<Long> getAllTaskExecutionIds(boolean onlyCompleted, String taskName) {

		String QUERY;
		MapSqlParameterSource queryParameters = new MapSqlParameterSource();
		if (StringUtils.hasText(taskName)) {
			queryParameters.addValue("taskName", taskName, Types.VARCHAR);
			QUERY = (onlyCompleted) ? FIND_ALL_COMPLETED_TASK_EXECUTION_IDS_BY_TASK_NAME : FIND_ALL_TASK_EXECUTION_IDS_BY_TASK_NAME;
		}
		else {
			QUERY = (onlyCompleted) ? FIND_ALL_COMPLETED_TASK_EXECUTION_IDS : FIND_ALL_TASK_EXECUTION_IDS;
		}
		try {
			return this.jdbcTemplate.query(
					SchemaUtilities.getQuery(QUERY, this.taskProperties.getTablePrefix()),
					queryParameters,
					resultSet -> {
					Set<Long> taskExecutionIds = new TreeSet<>();

					while (resultSet.next()) {
							taskExecutionIds.add(resultSet.getLong("TASK_EXECUTION_ID"));
					}
					return taskExecutionIds;
			});
		}
		catch (DataAccessException e) {
			return Collections.emptySet();
		}
	}
}
