/*
 * Copyright 2019 the original author or authors.
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Stores job execution information to a JDBC DataSource. Mirrors the {@link JdbcJobExecutionDao}
 * but contains Spring Cloud Data Flow specific operations. This functionality might
 * be migrated to Spring Batch itself eventually.
 *
 * @author Gunnar Hillert
 */
public class JdbcDataflowJobExecutionDao implements DataflowJobExecutionDao {

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final String tablePrefix;

	/**
	 * SQL statements for retrieving Step Execution Ids.
	 */
	private static final String SELECT_STEP_EXECUTION_IDS =
			"SELECT SEC.STEP_EXECUTION_ID AS STEP_EXECUTION_ID " +
			"FROM %PREFIX%STEP_EXECUTION_CONTEXT SEC " +
			"JOIN %PREFIX%STEP_EXECUTION SE ON SE.STEP_EXECUTION_ID = SEC.STEP_EXECUTION_ID " +
			"WHERE SE.JOB_EXECUTION_ID in (:jobExecutionIds)";

	/**
	 * SQL statements for removing the Step Execution Context.
	 */
	private static final String  SQL_DELETE_BATCH_STEP_EXECUTION_CONTEXT =
			"DELETE FROM %PREFIX%STEP_EXECUTION_CONTEXT " +
			"WHERE STEP_EXECUTION_ID " +
			"IN (:stepExecutionIds)";

	/**
	 * SQL statements for removing the Step Executions.
	 */
	private static final String  SQL_DELETE_BATCH_STEP_EXECUTION =
			"DELETE FROM %PREFIX%STEP_EXECUTION " +
			"WHERE JOB_EXECUTION_ID " +
			"IN (:jobExecutionIds)";

	/**
	 * SQL statements for removing the Job Execution Context.
	 */
	private static final String  SQL_DELETE_BATCH_JOB_EXECUTION_CONTEXT =
			"DELETE FROM %PREFIX%JOB_EXECUTION_CONTEXT " +
			"WHERE JOB_EXECUTION_ID IN (:jobExecutionIds)";

	/**
	 * SQL statements for removing the Job Execution Parameters.
	 */
	private static final String  SQL_DELETE_BATCH_JOB_EXECUTION_PARAMS =
			"DELETE FROM %PREFIX%JOB_EXECUTION_PARAMS " +
			"WHERE JOB_EXECUTION_ID IN (:jobExecutionIds)";

	/**
	 * SQL statements for removing the Job Executions.
	 */
	private static final String  SQL_DELETE_BATCH_JOB_EXECUTION =
			"DELETE FROM %PREFIX%JOB_EXECUTION " +
			"WHERE JOB_EXECUTION_ID IN (:jobExecutionIds)";

	/**
	 * SQL statements for removing Job Instances.
	 */
	private static final String  SQL_DELETE_BATCH_JOB_INSTANCE =
			"DELETE FROM %PREFIX%JOB_INSTANCE " +
			"WHERE NOT EXISTS ( " +
			"SELECT JOB_INSTANCE_ID FROM %PREFIX%JOB_EXECUTION WHERE %PREFIX%JOB_INSTANCE.JOB_INSTANCE_ID = JOB_INSTANCE_ID)";

	/**
	 * Initializes the JdbcDataflowJobExecutionDao.
	 *
	 * @param dataSource used by the dao to execute queries and update the tables. Must not be null.
	 * @param tablePrefix Must not be null or empty.
	 */
	public JdbcDataflowJobExecutionDao(DataSource dataSource, String tablePrefix) {
		Assert.hasText(tablePrefix, "tablePrefix must not be null nor empty.");
		Assert.notNull(dataSource, "The dataSource must not be null.");
		this.tablePrefix = tablePrefix;
		this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
	}

	@Override
	public int deleteBatchStepExecutionContextByStepExecutionIds(Set<Long> stepExecutionIds) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("stepExecutionIds", stepExecutionIds);
		final String query = getQuery(SQL_DELETE_BATCH_STEP_EXECUTION_CONTEXT);
		return this.jdbcTemplate.update(query, queryParameters);
	}

	@Override
	public int deleteBatchStepExecutionsByJobExecutionIds(Set<Long> jobExecutionIds) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("jobExecutionIds", jobExecutionIds);
		final String query = getQuery(SQL_DELETE_BATCH_STEP_EXECUTION);
		return this.jdbcTemplate.update(query, queryParameters);
	}

	@Override
	public int deleteBatchJobExecutionContextByJobExecutionIds(Set<Long> jobExecutionIds) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("jobExecutionIds", jobExecutionIds);
		final String query = getQuery(SQL_DELETE_BATCH_JOB_EXECUTION_CONTEXT);
		return this.jdbcTemplate.update(query, queryParameters);
	}

	@Override
	public int deleteBatchJobExecutionParamsByJobExecutionIds(Set<Long> jobExecutionIds) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("jobExecutionIds", jobExecutionIds);
		final String query = getQuery(SQL_DELETE_BATCH_JOB_EXECUTION_PARAMS);
		return this.jdbcTemplate.update(query, queryParameters);
	}

	@Override
	public int deleteBatchJobExecutionByJobExecutionIds(Set<Long> jobExecutionIds) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("jobExecutionIds", jobExecutionIds);
		final String query = getQuery(SQL_DELETE_BATCH_JOB_EXECUTION);
		return this.jdbcTemplate.update(query, queryParameters);
	}

	@Override
	public int deleteUnusedBatchJobInstances() {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource();
		final String query = getQuery(SQL_DELETE_BATCH_JOB_INSTANCE);
		return this.jdbcTemplate.update(query, queryParameters);
	}

	@Override
	public Set<Long> findStepExecutionIds(Set<Long> jobExecutionIds) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("jobExecutionIds", jobExecutionIds);

		Set<Long> stepExecutionIds;
		try {
			stepExecutionIds = this.jdbcTemplate.query(
					getQuery(SELECT_STEP_EXECUTION_IDS), queryParameters,
					new ResultSetExtractor<Set<Long>>() {
						@Override
						public Set<Long> extractData(ResultSet resultSet)
								throws SQLException, DataAccessException {

							Set<Long> stepExecutionIds = new TreeSet<>();

							while (resultSet.next()) {
								stepExecutionIds
										.add(resultSet.getLong("STEP_EXECUTION_ID"));
							}

							return stepExecutionIds;
						}
					});
		}
		catch (DataAccessException e) {
			stepExecutionIds = Collections.emptySet();
		}

		return stepExecutionIds;

	}

	private String getQuery(String base) {
		return StringUtils.replace(base, "%PREFIX%", this.tablePrefix);
	}
}
