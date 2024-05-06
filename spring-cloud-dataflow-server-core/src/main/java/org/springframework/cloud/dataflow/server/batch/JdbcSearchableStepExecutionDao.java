/*
 * Copyright 2006-2024 the original author or authors.
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
package org.springframework.cloud.dataflow.server.batch;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.JdbcStepExecutionDao;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.support.PatternMatcher;
import org.springframework.cloud.dataflow.server.batch.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Glenn Renfro
 */
public class JdbcSearchableStepExecutionDao extends JdbcStepExecutionDao implements SearchableStepExecutionDao {

	private static final String STEP_EXECUTIONS_FOR_JOB = "SELECT distinct STEP_NAME from %PREFIX%STEP_EXECUTION S, %PREFIX%JOB_EXECUTION E, %PREFIX%JOB_INSTANCE I "
			+ "where S.JOB_EXECUTION_ID = E.JOB_EXECUTION_ID AND E.JOB_INSTANCE_ID = I.JOB_INSTANCE_ID AND I.JOB_NAME = ?";

	private static final String COUNT_STEP_EXECUTIONS_FOR_STEP = "SELECT COUNT(STEP_EXECUTION_ID) from %PREFIX%STEP_EXECUTION S, %PREFIX%JOB_EXECUTION E, %PREFIX%JOB_INSTANCE I "
			+ "where S.JOB_EXECUTION_ID = E.JOB_EXECUTION_ID AND E.JOB_INSTANCE_ID = I.JOB_INSTANCE_ID AND I.JOB_NAME = ? AND S.STEP_NAME = ?";

	private static final String COUNT_STEP_EXECUTIONS_FOR_JOB_EXECUTION = "SELECT COUNT(STEP_EXECUTION_ID) from %PREFIX%STEP_EXECUTION S, %PREFIX%JOB_EXECUTION E "
			+ "where S.JOB_EXECUTION_ID = E.JOB_EXECUTION_ID and E.JOB_EXECUTION_ID = ?";

	private static final String COUNT_STEP_EXECUTIONS_FOR_STEP_PATTERN = "SELECT COUNT(STEP_EXECUTION_ID) from %PREFIX%STEP_EXECUTION S, %PREFIX%JOB_EXECUTION E, %PREFIX%JOB_INSTANCE I"
			+ " where S.JOB_EXECUTION_ID = E.JOB_EXECUTION_ID AND E.JOB_INSTANCE_ID = I.JOB_INSTANCE_ID AND I.JOB_NAME = ? AND S.STEP_NAME like ?";

	private static final String FIELDS = "S.STEP_EXECUTION_ID, S.STEP_NAME, S.START_TIME, S.END_TIME, S.STATUS, S.COMMIT_COUNT,"
			+ " S.READ_COUNT, S.FILTER_COUNT, S.WRITE_COUNT, S.EXIT_CODE, S.EXIT_MESSAGE, S.READ_SKIP_COUNT, S.WRITE_SKIP_COUNT,"
			+ " S.PROCESS_SKIP_COUNT, S.ROLLBACK_COUNT, S.LAST_UPDATED, S.VERSION";

	private DataSource dataSource;

	/**
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * @see JdbcJobExecutionDao#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.state(dataSource != null, "DataSource must be provided");

		if (getJdbcTemplate() == null) {
			setJdbcTemplate(new JdbcTemplate(dataSource));
		}
		setStepExecutionIncrementer(new AbstractDataFieldMaxValueIncrementer() {
			@Override
			protected long getNextKey() {
				return 0;
			}
		});

		super.afterPropertiesSet();

	}

	public Collection<String> findStepNamesForJobExecution(String jobName, String excludesPattern) {

		List<String> list = getJdbcTemplate().query(getQuery(STEP_EXECUTIONS_FOR_JOB), new RowMapper<String>() {
			public String mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
				return rs.getString(1);
			}
		}, jobName);

		Set<String> stepNames = new LinkedHashSet<String>(list);
		for (Iterator<String> iterator = stepNames.iterator(); iterator.hasNext();) {
			String name = iterator.next();
			if (PatternMatcher.match(excludesPattern, name)) {
				iterator.remove();
			}
		}

		return stepNames;

	}

	public Collection<StepExecution> findStepExecutions(String jobName, String stepName, int start, int count) {

		String whereClause;

		if (jobName.contains("*")) {
			whereClause = "JOB_NAME like ?";
			jobName = jobName.replace("*", "%");
		}
		else {
			whereClause = "JOB_NAME = ?";
		}

		if (stepName.contains("*")) {
			whereClause = whereClause + " AND STEP_NAME like ?";
			stepName = stepName.replace("*", "%");
		}
		else {
			whereClause = whereClause + " AND STEP_NAME = ?";
		}

		DataflowSqlPagingQueryProvider queryProvider = getPagingQueryProvider(whereClause);


		List<StepExecution> stepExecutions;
		if (start <= 0) {
			stepExecutions = getJdbcTemplate().query(queryProvider.generateFirstPageQuery(count),
					new StepExecutionRowMapper(), jobName, stepName);
		}
		else {
			try {
				Long startAfterValue = getJdbcTemplate().queryForObject(
						queryProvider.generateJumpToItemQuery(start, count), Long.class, jobName, stepName);
				stepExecutions = getJdbcTemplate().query(queryProvider.generateRemainingPagesQuery(count),
						new StepExecutionRowMapper(), jobName, stepName, startAfterValue);
			}
			catch (IncorrectResultSizeDataAccessException e) {
				return Collections.emptyList();
			}
		}

		return stepExecutions;

	}

	public int countStepExecutions(String jobName, String stepName) {
		if (stepName.contains("*")) {
			return getJdbcTemplate().queryForObject(getQuery(COUNT_STEP_EXECUTIONS_FOR_STEP_PATTERN), Integer.class, jobName,
					stepName.replace("*", "%"));
		}
		return getJdbcTemplate().queryForObject(getQuery(COUNT_STEP_EXECUTIONS_FOR_STEP), Integer.class, jobName, stepName);
	}

	@Override
	public int countStepExecutionsForJobExecution(long jobExecutionId) {
		return getJdbcTemplate().queryForObject(getQuery(COUNT_STEP_EXECUTIONS_FOR_JOB_EXECUTION), Integer.class, jobExecutionId);
	}

	/**
	 * @return a {@link DataflowSqlPagingQueryProvider} with a where clause to narrow the query
	 */
	private DataflowSqlPagingQueryProvider getPagingQueryProvider(String whereClause) {
		SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
		factory.setDataSource(dataSource);
		factory.setFromClause(getQuery("%PREFIX%STEP_EXECUTION S, %PREFIX%JOB_EXECUTION J, %PREFIX%JOB_INSTANCE I"));
		factory.setSelectClause(FIELDS);
		Map<String, Order> sortKeys = new HashMap<String, Order>();
		sortKeys.put("STEP_EXECUTION_ID", Order.DESCENDING);
		factory.setSortKeys(sortKeys);
		if (whereClause != null) {
			factory.setWhereClause(whereClause
					+ " AND S.JOB_EXECUTION_ID = J.JOB_EXECUTION_ID AND J.JOB_INSTANCE_ID = I.JOB_INSTANCE_ID");
		}
		try {
			return factory.getObject();
		}
		catch (Exception e) {
			throw new IllegalStateException("Unexpected exception creating paging query provide", e);
		}
	}

	private static class StepExecutionRowMapper implements RowMapper<StepExecution> {

		public StepExecution mapRow(ResultSet rs, int rowNum) throws SQLException {
			StepExecution stepExecution = new StepExecution(rs.getString(2), null);
			stepExecution.setId(rs.getLong(1));
			stepExecution.setStartTime(rs.getTimestamp(3).toLocalDateTime());
			stepExecution.setEndTime(rs.getTimestamp(4).toLocalDateTime());
			stepExecution.setStatus(BatchStatus.valueOf(rs.getString(5)));
			stepExecution.setCommitCount(rs.getInt(6));
			stepExecution.setReadCount(rs.getInt(7));
			stepExecution.setFilterCount(rs.getInt(8));
			stepExecution.setWriteCount(rs.getInt(9));
			stepExecution.setExitStatus(new ExitStatus(rs.getString(10), rs.getString(11)));
			stepExecution.setReadSkipCount(rs.getInt(12));
			stepExecution.setWriteSkipCount(rs.getInt(13));
			stepExecution.setProcessSkipCount(rs.getInt(14));
			stepExecution.setRollbackCount(rs.getInt(15));
			stepExecution.setLastUpdated(rs.getTimestamp(16).toLocalDateTime());
			stepExecution.setVersion(rs.getInt(17));
			return stepExecution;
		}

	}

}
