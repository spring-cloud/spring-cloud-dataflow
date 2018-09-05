/*
 * Copyright 2006-2014 the original author or authors.
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
package org.springframework.batch.admin.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * @author Michael Minella
 * 
 */
public class JdbcSearchableJobExecutionDao extends JdbcJobExecutionDao implements SearchableJobExecutionDao {

	private static final String GET_COUNT = "SELECT COUNT(1) from %PREFIX%JOB_EXECUTION";

	private static final String GET_COUNT_BY_JOB_NAME = "SELECT COUNT(1) from %PREFIX%JOB_EXECUTION E, %PREFIX%JOB_INSTANCE I "
			+ "where E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID and I.JOB_NAME=?";

	private static final String FIELDS = "E.JOB_EXECUTION_ID, E.START_TIME, E.END_TIME, E.STATUS, E.EXIT_CODE, E.EXIT_MESSAGE, "
			+ "E.CREATE_TIME, E.LAST_UPDATED, E.VERSION, I.JOB_INSTANCE_ID, I.JOB_NAME";

	private static final String GET_RUNNING_EXECUTIONS = "SELECT " + FIELDS
			+ " from %PREFIX%JOB_EXECUTION E, %PREFIX%JOB_INSTANCE I "
			+ "where E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID and E.END_TIME is NULL";

	private PagingQueryProvider allExecutionsPagingQueryProvider;

	private PagingQueryProvider byJobNamePagingQueryProvider;

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
		setJobExecutionIncrementer(new AbstractDataFieldMaxValueIncrementer() {
			@Override
			protected long getNextKey() {
				return 0;
			}
		});

		allExecutionsPagingQueryProvider = getPagingQueryProvider();
		byJobNamePagingQueryProvider = getPagingQueryProvider("I.JOB_NAME=?");

		super.afterPropertiesSet();

	}

	/**
	 * @return a {@link PagingQueryProvider} for all job executions
	 * @throws Exception
	 */
	private PagingQueryProvider getPagingQueryProvider() throws Exception {
		return getPagingQueryProvider(null);
	}

	/**
	 * @return a {@link PagingQueryProvider} for all job executions with the
	 * provided where clause
	 * @throws Exception
	 */
	private PagingQueryProvider getPagingQueryProvider(String whereClause) throws Exception {
		return getPagingQueryProvider(null, whereClause);
	}

	/**
	 * @return a {@link PagingQueryProvider} with a where clause to narrow the
	 * query
	 * @throws Exception
	 */
	private PagingQueryProvider getPagingQueryProvider(String fromClause, String whereClause) throws Exception {
		SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
		factory.setDataSource(dataSource);
		fromClause = "%PREFIX%JOB_EXECUTION E, %PREFIX%JOB_INSTANCE I" + (fromClause == null ? "" : ", " + fromClause);
		factory.setFromClause(getQuery(fromClause));
		factory.setSelectClause(FIELDS);
		Map<String, Order> sortKeys = new HashMap<String, Order>();
		sortKeys.put("JOB_EXECUTION_ID", Order.DESCENDING);
		factory.setSortKeys(sortKeys);
		whereClause = "E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID" + (whereClause == null ? "" : " and " + whereClause);
		factory.setWhereClause(whereClause);

		return (PagingQueryProvider) factory.getObject();
	}

	/**
	 * @see SearchableJobExecutionDao#countJobExecutions()
	 */
	@Override
	public int countJobExecutions() {
		return getJdbcTemplate().queryForObject(getQuery(GET_COUNT), Integer.class);
	}

	/**
	 * @see SearchableJobExecutionDao#countJobExecutions(String)
	 */
	@Override
	public int countJobExecutions(String jobName) {
		return getJdbcTemplate().queryForObject(getQuery(GET_COUNT_BY_JOB_NAME), Integer.class, jobName);
	}

	/**
	 * @see SearchableJobExecutionDao#getRunningJobExecutions()
	 */
	@Override
	public Collection<JobExecution> getRunningJobExecutions() {
		return getJdbcTemplate().query(getQuery(GET_RUNNING_EXECUTIONS), new JobExecutionRowMapper());
	}

	/**
	 * @see SearchableJobExecutionDao#getJobExecutions(String, int, int)
	 */
	@Override
	public List<JobExecution> getJobExecutions(String jobName, int start, int count) {
		if (start <= 0) {
			return getJdbcTemplate().query(byJobNamePagingQueryProvider.generateFirstPageQuery(count),
					new JobExecutionRowMapper(), jobName);
		}
		try {
			Long startAfterValue = getJdbcTemplate().queryForObject(
					byJobNamePagingQueryProvider.generateJumpToItemQuery(start, count), Long.class, jobName);
			return getJdbcTemplate().query(byJobNamePagingQueryProvider.generateRemainingPagesQuery(count),
					new JobExecutionRowMapper(), jobName, startAfterValue);
		}
		catch (IncorrectResultSizeDataAccessException e) {
			return Collections.emptyList();
		}
	}

	/**
	 * @see SearchableJobExecutionDao#getJobExecutions(int, int)
	 */
	@Override
	public List<JobExecution> getJobExecutions(int start, int count) {
		if (start <= 0) {
			return getJdbcTemplate().query(allExecutionsPagingQueryProvider.generateFirstPageQuery(count),
					new JobExecutionRowMapper());
		}
		try {
			Long startAfterValue = getJdbcTemplate().queryForObject(
					allExecutionsPagingQueryProvider.generateJumpToItemQuery(start, count), Long.class);
			return getJdbcTemplate().query(allExecutionsPagingQueryProvider.generateRemainingPagesQuery(count),
					new JobExecutionRowMapper(), startAfterValue);
		}
		catch (IncorrectResultSizeDataAccessException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public void saveJobExecution(JobExecution jobExecution) {
		throw new UnsupportedOperationException("SearchableJobExecutionDao is read only");
	}

	@Override
	public void synchronizeStatus(JobExecution jobExecution) {
		throw new UnsupportedOperationException("SearchableJobExecutionDao is read only");
	}

	@Override
	public void updateJobExecution(JobExecution jobExecution) {
		throw new UnsupportedOperationException("SearchableJobExecutionDao is read only");
	}

	/**
	 * Re-usable mapper for {@link JobExecution} instances.
	 * 
	 * @author Dave Syer
	 * 
	 */
	protected class JobExecutionRowMapper implements RowMapper<JobExecution> {

		public JobExecutionRowMapper() {
		}

		@Override
		public JobExecution mapRow(ResultSet rs, int rowNum) throws SQLException {
			Long id = rs.getLong(1);
			JobExecution jobExecution;
			
			JobParameters jobParameters = getJobParameters(id);

			JobInstance jobInstance = new JobInstance(rs.getLong(10), rs.getString(11));
			jobExecution = new JobExecution(jobInstance, jobParameters);
			jobExecution.setId(id);

			jobExecution.setStartTime(rs.getTimestamp(2));
			jobExecution.setEndTime(rs.getTimestamp(3));
			jobExecution.setStatus(BatchStatus.valueOf(rs.getString(4)));
			jobExecution.setExitStatus(new ExitStatus(rs.getString(5), rs.getString(6)));
			jobExecution.setCreateTime(rs.getTimestamp(7));
			jobExecution.setLastUpdated(rs.getTimestamp(8));
			jobExecution.setVersion(rs.getInt(9));
			return jobExecution;
		}

	}
}
