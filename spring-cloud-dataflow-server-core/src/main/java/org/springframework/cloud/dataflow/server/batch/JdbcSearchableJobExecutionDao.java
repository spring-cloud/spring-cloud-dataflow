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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.item.database.Order;
import org.springframework.cloud.dataflow.server.batch.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.cloud.dataflow.server.converter.StringToDateConverter;
import org.springframework.cloud.dataflow.server.repository.support.SchemaUtilities;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Corneil du Plessis
 *
 */
public class JdbcSearchableJobExecutionDao extends JdbcJobExecutionDao implements SearchableJobExecutionDao {

	private static final String FIND_PARAMS_FROM_ID = "SELECT JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING FROM %PREFIX%JOB_EXECUTION_PARAMS WHERE JOB_EXECUTION_ID = ?";

	private static final String GET_COUNT = "SELECT COUNT(1) from %PREFIX%JOB_EXECUTION";

	private static final String GET_COUNT_BY_JOB_NAME = "SELECT COUNT(1) from %PREFIX%JOB_EXECUTION E "
			+ "JOIN %PREFIX%JOB_INSTANCE I ON E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID where I.JOB_NAME=?";

	private static final String GET_COUNT_BY_STATUS = "SELECT COUNT(1) from %PREFIX%JOB_EXECUTION E "
			+ "JOIN %PREFIX%JOB_INSTANCE I ON E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID where E.STATUS = ?";

	private static final String GET_COUNT_BY_JOB_NAME_AND_STATUS = "SELECT COUNT(1) from %PREFIX%JOB_EXECUTION E "
			+ "JOIN %PREFIX%JOB_INSTANCE I ON E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID where I.JOB_NAME=? AND E.STATUS = ?";

	private static final String FIELDS = "E.JOB_EXECUTION_ID, E.START_TIME, E.END_TIME, E.STATUS, E.EXIT_CODE, E.EXIT_MESSAGE, "
			+ "E.CREATE_TIME, E.LAST_UPDATED, E.VERSION, I.JOB_INSTANCE_ID, I.JOB_NAME";

	private static final String FIELDS_WITH_STEP_COUNT = FIELDS
			+ ", (SELECT COUNT(*) FROM %PREFIX%STEP_EXECUTION S WHERE S.JOB_EXECUTION_ID = E.JOB_EXECUTION_ID) as STEP_COUNT";

	private static final String GET_RUNNING_EXECUTIONS = "SELECT " + FIELDS
			+ " from %PREFIX%JOB_EXECUTION E JOIN %PREFIX%JOB_INSTANCE I ON E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID where E.END_TIME is NULL";

	private static final String NAME_FILTER = "I.JOB_NAME LIKE ?";

	private static final String DATE_RANGE_FILTER = "E.START_TIME BETWEEN ? AND ?";

	private static final String JOB_INSTANCE_ID_FILTER = "I.JOB_INSTANCE_ID = ?";

	private static final String STATUS_FILTER = "E.STATUS = ?";

	private static final String NAME_AND_STATUS_FILTER = "I.JOB_NAME LIKE ? AND E.STATUS = ?";

	private static final String TASK_EXECUTION_ID_FILTER = "B.JOB_EXECUTION_ID = E.JOB_EXECUTION_ID AND B.TASK_EXECUTION_ID = ?";

	private static final String FIND_JOB_EXECUTIONS = "SELECT JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, CREATE_TIME, LAST_UPDATED, VERSION"
			+ " from %PREFIX%JOB_EXECUTION where JOB_INSTANCE_ID = ? order by JOB_EXECUTION_ID desc";

	private static final String GET_LAST_EXECUTION = "SELECT JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, CREATE_TIME, LAST_UPDATED, VERSION"
			+ " from %PREFIX%JOB_EXECUTION E where JOB_INSTANCE_ID = ? and JOB_EXECUTION_ID in (SELECT max(JOB_EXECUTION_ID) from %PREFIX%JOB_EXECUTION E2 where E2.JOB_INSTANCE_ID = ?)";

	private static final String GET_RUNNING_EXECUTIONS_BY_JOB_NAME = "SELECT E.JOB_EXECUTION_ID, E.START_TIME, E.END_TIME, E.STATUS, E.EXIT_CODE, E.EXIT_MESSAGE, E.CREATE_TIME, E.LAST_UPDATED, E.VERSION, "
			+ "E.JOB_INSTANCE_ID from %PREFIX%JOB_EXECUTION E, %PREFIX%JOB_INSTANCE I where E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID and I.JOB_NAME=? and E.START_TIME is not NULL and E.END_TIME is NULL order by E.JOB_EXECUTION_ID desc";

	private static final String GET_EXECUTION_BY_ID = "SELECT JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, CREATE_TIME, LAST_UPDATED, VERSION"
			+ " from %PREFIX%JOB_EXECUTION where JOB_EXECUTION_ID = ?";

	private static final String FROM_CLAUSE_TASK_TASK_BATCH = "%TASK_PREFIX%TASK_BATCH B";

	private static final String GET_JOB_EXECUTIONS_BY_TASK_IDS = "SELECT JOB_EXECUTION_ID, TASK_EXECUTION_ID from %TASK_PREFIX%TASK_BATCH WHERE TASK_EXECUTION_ID in (?)";

	private DataflowSqlPagingQueryProvider allExecutionsPagingQueryProvider;

	private DataflowSqlPagingQueryProvider byJobNamePagingQueryProvider;

	private DataflowSqlPagingQueryProvider byStatusPagingQueryProvider;

	private DataflowSqlPagingQueryProvider byJobNameAndStatusPagingQueryProvider;


	private DataflowSqlPagingQueryProvider byJobNameWithStepCountPagingQueryProvider;

	private DataflowSqlPagingQueryProvider executionsWithStepCountPagingQueryProvider;

	private DataflowSqlPagingQueryProvider byDateRangeWithStepCountPagingQueryProvider;

	private DataflowSqlPagingQueryProvider byJobInstanceIdWithStepCountPagingQueryProvider;

	private DataflowSqlPagingQueryProvider byTaskExecutionIdWithStepCountPagingQueryProvider;

	private final ConfigurableConversionService conversionService;

	private DataSource dataSource;

	private String taskTablePrefix;

	public JdbcSearchableJobExecutionDao() {
		conversionService = new DefaultConversionService();
		conversionService.addConverter(new StringToDateConverter());
	}

	/**
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setTaskTablePrefix(String taskTablePrefix) {
		this.taskTablePrefix = taskTablePrefix;
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
		executionsWithStepCountPagingQueryProvider = getPagingQueryProvider(FIELDS_WITH_STEP_COUNT, null, null);
		byJobNamePagingQueryProvider = getPagingQueryProvider(NAME_FILTER);
		byStatusPagingQueryProvider = getPagingQueryProvider(STATUS_FILTER);
		byJobNameAndStatusPagingQueryProvider = getPagingQueryProvider(NAME_AND_STATUS_FILTER);
		byJobNameWithStepCountPagingQueryProvider = getPagingQueryProvider(FIELDS_WITH_STEP_COUNT, null, NAME_FILTER);
		byDateRangeWithStepCountPagingQueryProvider = getPagingQueryProvider(FIELDS_WITH_STEP_COUNT, null, DATE_RANGE_FILTER);
		byJobInstanceIdWithStepCountPagingQueryProvider = getPagingQueryProvider(FIELDS_WITH_STEP_COUNT, null, JOB_INSTANCE_ID_FILTER);
		byTaskExecutionIdWithStepCountPagingQueryProvider = getPagingQueryProvider(FIELDS_WITH_STEP_COUNT,
				getTaskQuery(FROM_CLAUSE_TASK_TASK_BATCH), TASK_EXECUTION_ID_FILTER);

		super.afterPropertiesSet();
	}

	protected String getTaskQuery(String base) {
		return StringUtils.replace(base, "%TASK_PREFIX%", taskTablePrefix);
	}
	@Override
	public List<JobExecution> findJobExecutions(JobInstance job) {
		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(job.getId(), "Job Id cannot be null.");

		String sqlQuery = FIND_JOB_EXECUTIONS;
		return getJdbcTemplate().query(getQuery(sqlQuery), new JobExecutionRowMapper(job), job.getId());

	}

	@Override
	public JobExecution getLastJobExecution(JobInstance jobInstance) {
		Long id = jobInstance.getId();
		String sqlQuery = GET_LAST_EXECUTION;
		List<JobExecution> executions = getJdbcTemplate().query(getQuery(sqlQuery),
				new JobExecutionRowMapper(jobInstance), id, id);

		Assert.state(executions.size() <= 1, "There must be at most one latest job execution");

		if (executions.isEmpty()) {
			return null;
		}
		else {
			return executions.get(0);
		}
	}

	@Override
	public Set<JobExecution> findRunningJobExecutions(String jobName) {
		Set<JobExecution> result = new HashSet<>();
		String sqlQuery = GET_RUNNING_EXECUTIONS_BY_JOB_NAME;
		getJdbcTemplate().query(getQuery(sqlQuery), new JobExecutionRowMapper(), jobName);

		return result;
	}

	@Override
	public JobExecution getJobExecution(Long executionId) {
		try {
			String sqlQuery = GET_EXECUTION_BY_ID;
			return getJdbcTemplate().queryForObject(getQuery(sqlQuery), new JobExecutionRowMapper(),
					executionId);
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	/**
	 * @return a {@link DataflowSqlPagingQueryProvider} for all job executions
	 * @throws Exception if page provider is not created.
	 */
	private DataflowSqlPagingQueryProvider getPagingQueryProvider() throws Exception {
		return getPagingQueryProvider(null);
	}


	/**
	 * @return a {@link DataflowSqlPagingQueryProvider} for all job executions with the provided
	 * where clause
	 * @throws Exception if page provider is not created.
	 */
	private DataflowSqlPagingQueryProvider getPagingQueryProvider(String whereClause) throws Exception {
		return getPagingQueryProvider(null, whereClause);
	}


	/**
	 * @return a {@link DataflowSqlPagingQueryProvider} with a where clause to narrow the query
	 * @throws Exception if page provider is not created.
	 */
	private DataflowSqlPagingQueryProvider getPagingQueryProvider(String fromClause, String whereClause) throws Exception {
		return getPagingQueryProvider(null, fromClause, whereClause);
	}

	/**
	 * @return a {@link DataflowSqlPagingQueryProvider} with a where clause to narrow the query
	 * @throws Exception if page provider is not created.
	 */
	private DataflowSqlPagingQueryProvider getPagingQueryProvider(String fields, String fromClause, String whereClause)
			throws Exception {
		SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
		factory.setDataSource(dataSource);
		fromClause = "%PREFIX%JOB_EXECUTION E, %PREFIX%JOB_INSTANCE I" + (fromClause == null ? "" : ", " + fromClause);
		factory.setFromClause(getQuery(fromClause));
		if (fields == null) {
			fields = FIELDS;
		}
		factory.setSelectClause(getQuery(fields));
		Map<String, Order> sortKeys = new HashMap<>();
		sortKeys.put("JOB_EXECUTION_ID", Order.DESCENDING);
		factory.setSortKeys(sortKeys);
		whereClause = "E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID" + (whereClause == null ? "" : " and " + whereClause);
		factory.setWhereClause(whereClause);

		return factory.getObject();
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
	 * @see SearchableJobExecutionDao#countJobExecutions(BatchStatus)
	 */
	@Override
	public int countJobExecutions(BatchStatus status) {
		return getJdbcTemplate().queryForObject(getQuery(GET_COUNT_BY_STATUS), Integer.class, status.name());
	}

	/**
	 * @see SearchableJobExecutionDao#countJobExecutions(String, BatchStatus)
	 */
	@Override
	public int countJobExecutions(String jobName, BatchStatus status) {
		return getJdbcTemplate().queryForObject(getQuery(GET_COUNT_BY_JOB_NAME_AND_STATUS), Integer.class, jobName,
				status.name());
	}

	/**
	 * @see SearchableJobExecutionDao#getJobExecutionsWithStepCount(Date, Date, int, int)
	 */
	@Override
	public List<JobExecutionWithStepCount> getJobExecutionsWithStepCount(Date fromDate, Date toDate, int start,
			int count) {

		if (start <= 0) {
			return getJdbcTemplate().query(byDateRangeWithStepCountPagingQueryProvider.generateFirstPageQuery(count),
					new JobExecutionStepCountRowMapper(), fromDate, toDate);
		}
		try {
			Long startAfterValue = getJdbcTemplate().queryForObject(
					byDateRangeWithStepCountPagingQueryProvider.generateJumpToItemQuery(start, count), Long.class,
					fromDate, toDate);
			return getJdbcTemplate().query(
					byDateRangeWithStepCountPagingQueryProvider.generateRemainingPagesQuery(count),
					new JobExecutionStepCountRowMapper(), fromDate, toDate, startAfterValue);
		}
		catch (IncorrectResultSizeDataAccessException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public List<JobExecutionWithStepCount> getJobExecutionsWithStepCountFilteredByJobInstanceId(int jobInstanceId,
			int start, int count) {
		if (start <= 0) {
			return getJdbcTemplate().query(
					byJobInstanceIdWithStepCountPagingQueryProvider.generateFirstPageQuery(count),
					new JobExecutionStepCountRowMapper(), jobInstanceId);
		}
		try {
			Long startAfterValue = getJdbcTemplate().queryForObject(
					byJobInstanceIdWithStepCountPagingQueryProvider.generateJumpToItemQuery(start, count), Long.class,
					jobInstanceId);
			return getJdbcTemplate().query(
					byJobInstanceIdWithStepCountPagingQueryProvider.generateRemainingPagesQuery(count),
					new JobExecutionStepCountRowMapper(), jobInstanceId, startAfterValue);
		}
		catch (IncorrectResultSizeDataAccessException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public List<JobExecutionWithStepCount> getJobExecutionsWithStepCountFilteredByTaskExecutionId(int taskExecutionId,
			int start, int count) {
		if (start <= 0) {
			return getJdbcTemplate().query(SchemaUtilities.getQuery(
					byTaskExecutionIdWithStepCountPagingQueryProvider.generateFirstPageQuery(count),
					this.getTablePrefix()), new JobExecutionStepCountRowMapper(), taskExecutionId);
		}
		try {
			Long startAfterValue = getJdbcTemplate().queryForObject(
					byTaskExecutionIdWithStepCountPagingQueryProvider.generateJumpToItemQuery(start, count), Long.class,
					taskExecutionId);
			return getJdbcTemplate().query(
					byTaskExecutionIdWithStepCountPagingQueryProvider.generateRemainingPagesQuery(count),
					new JobExecutionStepCountRowMapper(), taskExecutionId, startAfterValue);
		}
		catch (IncorrectResultSizeDataAccessException e) {
			return Collections.emptyList();
		}
	}

	/**
	 * @see SearchableJobExecutionDao#getRunningJobExecutions()
	 */
	@Override
	public Collection<JobExecution> getRunningJobExecutions() {
		return getJdbcTemplate().query(getQuery(GET_RUNNING_EXECUTIONS), new SearchableJobExecutionRowMapper());
	}

	/**
	 * @see SearchableJobExecutionDao#getJobExecutions(String, BatchStatus, int, int)
	 */
	@Override
	public List<JobExecution> getJobExecutions(String jobName, BatchStatus status, int start, int count) {
		if (start <= 0) {
			return getJdbcTemplate().query(byJobNameAndStatusPagingQueryProvider.generateFirstPageQuery(count),
					new SearchableJobExecutionRowMapper(), jobName, status.name());
		}
		try {
			Long startAfterValue = getJdbcTemplate().queryForObject(
					byJobNameAndStatusPagingQueryProvider.generateJumpToItemQuery(start, count), Long.class, jobName,
					status.name());
			return getJdbcTemplate().query(byJobNameAndStatusPagingQueryProvider.generateRemainingPagesQuery(count),
					new SearchableJobExecutionRowMapper(), jobName, status.name(), startAfterValue);
		}
		catch (IncorrectResultSizeDataAccessException e) {
			return Collections.emptyList();
		}
	}

	/**
	 * @see SearchableJobExecutionDao#getJobExecutions(String, int, int)
	 */
	@Override
	public List<JobExecution> getJobExecutions(String jobName, int start, int count) {
		if (start <= 0) {
			return getJdbcTemplate().query(byJobNamePagingQueryProvider.generateFirstPageQuery(count),
					new SearchableJobExecutionRowMapper(), jobName);
		}
		try {
			Long startAfterValue = getJdbcTemplate().queryForObject(
					byJobNamePagingQueryProvider.generateJumpToItemQuery(start, count), Long.class, jobName);
			return getJdbcTemplate().query(byJobNamePagingQueryProvider.generateRemainingPagesQuery(count),
					new SearchableJobExecutionRowMapper(), jobName, startAfterValue);
		}
		catch (IncorrectResultSizeDataAccessException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public List<JobExecution> getJobExecutions(BatchStatus status, int start, int count) {
		if (start <= 0) {
			return getJdbcTemplate().query(byStatusPagingQueryProvider.generateFirstPageQuery(count),
					new SearchableJobExecutionRowMapper(), status.name());
		}
		try {
			Long startAfterValue = getJdbcTemplate().queryForObject(
					byStatusPagingQueryProvider.generateJumpToItemQuery(start, count), Long.class, status.name());
			return getJdbcTemplate().query(byStatusPagingQueryProvider.generateRemainingPagesQuery(count),
					new SearchableJobExecutionRowMapper(), status.name(), startAfterValue);
		}
		catch (IncorrectResultSizeDataAccessException e) {
			return Collections.emptyList();
		}
	}

	/**
	 * @see SearchableJobExecutionDao#getJobExecutionsWithStepCount(String, int, int)
	 */
	@Override
	public List<JobExecutionWithStepCount> getJobExecutionsWithStepCount(String jobName, int start, int count) {
		if (start <= 0) {
			return getJdbcTemplate().query(byJobNameWithStepCountPagingQueryProvider.generateFirstPageQuery(count),
					new JobExecutionStepCountRowMapper(), jobName);
		}
		try {
			Long startAfterValue = getJdbcTemplate().queryForObject(
					byJobNameWithStepCountPagingQueryProvider.generateJumpToItemQuery(start, count), Long.class,
					jobName);
			return getJdbcTemplate().query(byJobNameWithStepCountPagingQueryProvider.generateRemainingPagesQuery(count),
					new JobExecutionStepCountRowMapper(), jobName, startAfterValue);
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
					new SearchableJobExecutionRowMapper());
		}
		try {
			Long startAfterValue = getJdbcTemplate()
				.queryForObject(allExecutionsPagingQueryProvider.generateJumpToItemQuery(start, count), Long.class);
			return getJdbcTemplate().query(allExecutionsPagingQueryProvider.generateRemainingPagesQuery(count),
					new SearchableJobExecutionRowMapper(), startAfterValue);
		}
		catch (IncorrectResultSizeDataAccessException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public List<JobExecutionWithStepCount> getJobExecutionsWithStepCount(int start, int count) {
		if (start <= 0) {
			return getJdbcTemplate().query(executionsWithStepCountPagingQueryProvider.generateFirstPageQuery(count),
					new JobExecutionStepCountRowMapper());
		}
		try {
			Long startAfterValue = getJdbcTemplate().queryForObject(
					executionsWithStepCountPagingQueryProvider.generateJumpToItemQuery(start, count), Long.class);
			return getJdbcTemplate().query(
					executionsWithStepCountPagingQueryProvider.generateRemainingPagesQuery(count),
					new JobExecutionStepCountRowMapper(), startAfterValue);
		}
		catch (IncorrectResultSizeDataAccessException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public Map<Long, Set<Long>> getJobExecutionsByTaskIds(Collection<Long> ids) {
		JdbcOperations jdbcTemplate = getJdbcTemplate();
		String strIds = StringUtils.collectionToCommaDelimitedString(ids);

		String sql = getTaskQuery(GET_JOB_EXECUTIONS_BY_TASK_IDS).replace("?", strIds);
		return jdbcTemplate.query(sql,
                rs -> {
            	    final Map<Long, Set<Long>> results = new HashMap<>();
                	while (rs.next()) {
                    	Long taskExecutionId = rs.getLong("TASK_EXECUTION_ID");
                    	Long jobExecutionId = rs.getLong("JOB_EXECUTION_ID");
                    	Set<Long> jobs = results.computeIfAbsent(taskExecutionId, aLong -> new HashSet<>());
                    	jobs.add(jobExecutionId);
                	}
                	return results;
				});
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
	 * @author Glenn Renfro
	 * 
	 */
	protected class SearchableJobExecutionRowMapper implements RowMapper<JobExecution> {

		SearchableJobExecutionRowMapper() {
		}

		@Override
		public JobExecution mapRow(ResultSet rs, int rowNum) throws SQLException {
			return createJobExecutionFromResultSet(rs, rowNum);
		}

	}

	/**
	 * Re-usable mapper for {@link JobExecutionWithStepCount} instances.
	 *
	 * @author Glenn Renfro
	 *
	 */
	protected class JobExecutionStepCountRowMapper implements RowMapper<JobExecutionWithStepCount> {

		JobExecutionStepCountRowMapper() {
		}

		@Override
		public JobExecutionWithStepCount mapRow(ResultSet rs, int rowNum) throws SQLException {

			return new JobExecutionWithStepCount(createJobExecutionFromResultSet(rs, rowNum), rs.getInt(12));
		}

	}

	//TODO: Boot3x followup - need to handle LocalDateTime and possibly Integer
	protected JobParameters getJobParameters(Long executionId) {
		Map<String, JobParameter<?>> map = new HashMap<>();
		RowCallbackHandler handler = rs -> {
			String parameterName = rs.getString("PARAMETER_NAME");

			Class<?> parameterType = null;
			try {
				parameterType = Class.forName(rs.getString("PARAMETER_TYPE"));
			}
			catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			String stringValue = rs.getString("PARAMETER_VALUE");
			Object typedValue = conversionService.convert(stringValue, parameterType);

			boolean identifying = rs.getString("IDENTIFYING").equalsIgnoreCase("Y");

			if (typedValue instanceof String) {
				map.put(parameterName, new JobParameter(typedValue, String.class, identifying));
			}
			else if (typedValue instanceof Integer integer) {
				map.put(parameterName, new JobParameter(integer.longValue(), Integer.class, identifying));
			}
			else if (typedValue instanceof Long) {
				map.put(parameterName, new JobParameter(typedValue, Long.class, identifying));
			}
			else if (typedValue instanceof Float float1) {
				map.put(parameterName, new JobParameter(float1.doubleValue(), Float.class, identifying));
			}
			else if (typedValue instanceof Double) {
				map.put(parameterName, new JobParameter(typedValue, Double.class, identifying));
			}
			else if (typedValue instanceof Timestamp timestamp) {
				map.put(parameterName, new JobParameter(new Date(timestamp.getTime()), Timestamp.class, identifying));
			}
			else if (typedValue instanceof Date) {
				map.put(parameterName, new JobParameter(typedValue, Date.class, identifying));
			}
			else {
				map.put(parameterName,
						new JobParameter(typedValue != null ? typedValue.toString() : "null", String.class, identifying));
			}
		};

		getJdbcTemplate().query(getQuery(FIND_PARAMS_FROM_ID), handler, executionId);

		return new JobParameters(map);
	}

	JobExecution createJobExecutionFromResultSet(ResultSet rs, int rowNum) throws SQLException {
		Long id = rs.getLong(1);
		JobExecution jobExecution;

		JobParameters jobParameters = getJobParameters(id);

		JobInstance jobInstance = new JobInstance(rs.getLong(10), rs.getString(11));
		jobExecution = new JobExecution(jobInstance, jobParameters);
		jobExecution.setId(id);

		jobExecution.setStartTime(rs.getObject(2, LocalDateTime.class));
		jobExecution.setEndTime(rs.getObject(3, LocalDateTime.class));
		jobExecution.setStatus(BatchStatus.valueOf(rs.getString(4)));
		jobExecution.setExitStatus(new ExitStatus(rs.getString(5), rs.getString(6)));
		jobExecution.setCreateTime(rs.getObject(7, LocalDateTime.class));
		jobExecution.setLastUpdated(rs.getObject(8, LocalDateTime.class));
		jobExecution.setVersion(rs.getInt(9));
		return jobExecution;
	}

	private final class JobExecutionRowMapper implements RowMapper<JobExecution> {

		private JobInstance jobInstance;

		public JobExecutionRowMapper() {

		}

		public JobExecutionRowMapper(JobInstance jobInstance) {
			this.jobInstance = jobInstance;
		}

		@Override
		public JobExecution mapRow(ResultSet rs, int rowNum) throws SQLException {
			Long id = rs.getLong(1);
			JobParameters jobParameters = getJobParameters(id);
			JobExecution jobExecution;
			if (jobInstance == null) {
				jobExecution = new JobExecution(id, jobParameters);
			}
			else {
				jobExecution = new JobExecution(jobInstance, id, jobParameters);
			}
			Timestamp startTime = rs.getTimestamp(2);
			Timestamp endTime = rs.getTimestamp(3);
			Timestamp lastUpdatedTime = rs.getTimestamp(8);
			jobExecution.setStartTime((startTime != null) ? startTime.toLocalDateTime() : null);
			jobExecution.setEndTime((endTime != null) ? endTime.toLocalDateTime() : null);
			jobExecution.setStatus(BatchStatus.valueOf(rs.getString(4)));
			jobExecution.setExitStatus(new ExitStatus(rs.getString(5), rs.getString(6)));
			jobExecution.setCreateTime(rs.getTimestamp(7).toLocalDateTime());
			jobExecution.setLastUpdated((lastUpdatedTime != null) ? lastUpdatedTime.toLocalDateTime() : null);
			jobExecution.setVersion(rs.getInt(9));
			return jobExecution;
		}

	}

}
