/*
 * Copyright 2019-2023 the original author or authors.
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

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.AbstractSqlPagingQueryProvider;
import org.springframework.batch.item.database.support.Db2PagingQueryProvider;
import org.springframework.batch.item.database.support.OraclePagingQueryProvider;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.database.support.SqlPagingQueryUtils;
import org.springframework.batch.item.database.support.SqlServerPagingQueryProvider;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.cloud.dataflow.core.database.support.DatabaseType;
import org.springframework.cloud.dataflow.rest.job.JobInstanceExecutions;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersion;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.springframework.cloud.dataflow.server.converter.DateToStringConverter;
import org.springframework.cloud.dataflow.server.converter.StringToDateConverter;
import org.springframework.cloud.dataflow.server.service.JobServiceContainer;
import org.springframework.cloud.dataflow.server.service.impl.OffsetOutOfBoundsException;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Stores job execution information to a JDBC DataSource. Mirrors the {@link JdbcJobExecutionDao}
 * but contains Spring Cloud Data Flow specific operations. This functionality might
 * be migrated to Spring Batch itself eventually.
 *
 * @author Corneil du Plessis
 * @since 2.11.0
 */
public class JdbcAggregateJobQueryDao implements AggregateJobQueryDao {

	private final static Logger LOG = LoggerFactory.getLogger(JdbcAggregateJobQueryDao.class);

	private static final String GET_COUNT = "SELECT COUNT(1) from AGGREGATE_JOB_EXECUTION";

	private static final String GET_COUNT_BY_DATE = "SELECT COUNT(1) from AGGREGATE_JOB_EXECUTION WHERE START_TIME BETWEEN ? AND ?";

	private static final String GET_COUNT_BY_JOB_NAME = "SELECT COUNT(E.JOB_EXECUTION_ID) from AGGREGATE_JOB_INSTANCE I" +
			" JOIN AGGREGATE_JOB_EXECUTION E ON I.JOB_INSTANCE_ID=E.JOB_INSTANCE_ID AND I.SCHEMA_TARGET=E.SCHEMA_TARGET" +
			" JOIN AGGREGATE_TASK_BATCH B ON E.JOB_EXECUTION_ID = B.JOB_EXECUTION_ID AND E.SCHEMA_TARGET = B.SCHEMA_TARGET" +
			" JOIN AGGREGATE_TASK_EXECUTION T ON B.TASK_EXECUTION_ID = T.TASK_EXECUTION_ID AND B.SCHEMA_TARGET = T.SCHEMA_TARGET" +
			" WHERE I.JOB_NAME LIKE ?";

	private static final String GET_COUNT_BY_STATUS = "SELECT COUNT(E.JOB_EXECUTION_ID) from AGGREGATE_JOB_INSTANCE I" +
			" JOIN AGGREGATE_JOB_EXECUTION E ON I.JOB_INSTANCE_ID=E.JOB_INSTANCE_ID AND I.SCHEMA_TARGET=E.SCHEMA_TARGET" +
			" JOIN AGGREGATE_TASK_BATCH B ON E.JOB_EXECUTION_ID = B.JOB_EXECUTION_ID AND E.SCHEMA_TARGET = B.SCHEMA_TARGET" +
			" JOIN AGGREGATE_TASK_EXECUTION T ON B.TASK_EXECUTION_ID = T.TASK_EXECUTION_ID AND B.SCHEMA_TARGET = T.SCHEMA_TARGET" +
			" WHERE E.STATUS = ?";

	private static final String GET_COUNT_BY_JOB_INSTANCE_ID = "SELECT COUNT(E.JOB_EXECUTION_ID) from AGGREGATE_JOB_INSTANCE I" +
			" JOIN AGGREGATE_JOB_EXECUTION E ON I.JOB_INSTANCE_ID=E.JOB_INSTANCE_ID AND I.SCHEMA_TARGET=E.SCHEMA_TARGET" +
			" WHERE I.JOB_INSTANCE_ID = ? AND I.SCHEMA_TARGET = ?";

	private static final String GET_COUNT_BY_TASK_EXECUTION_ID = "SELECT COUNT(T.TASK_EXECUTION_ID) FROM AGGREGATE_JOB_EXECUTION E" +
			" JOIN AGGREGATE_TASK_BATCH B ON E.JOB_EXECUTION_ID = B.JOB_EXECUTION_ID AND E.SCHEMA_TARGET = B.SCHEMA_TARGET" +
			" JOIN AGGREGATE_TASK_EXECUTION T ON B.TASK_EXECUTION_ID = T.TASK_EXECUTION_ID AND B.SCHEMA_TARGET = T.SCHEMA_TARGET" +
			" WHERE T.TASK_EXECUTION_ID = ? AND T.SCHEMA_TARGET = ?";

	private static final String GET_COUNT_BY_JOB_NAME_AND_STATUS = "SELECT COUNT(E.JOB_EXECUTION_ID) FROM AGGREGATE_JOB_INSTANCE I" +
			" JOIN AGGREGATE_JOB_EXECUTION E ON I.JOB_INSTANCE_ID = E.JOB_INSTANCE_ID AND I.SCHEMA_TARGET = E.SCHEMA_TARGET" +
			" JOIN AGGREGATE_TASK_BATCH B ON E.JOB_EXECUTION_ID = B.JOB_EXECUTION_ID AND E.SCHEMA_TARGET = B.SCHEMA_TARGET" +
			" JOIN AGGREGATE_TASK_EXECUTION T ON B.TASK_EXECUTION_ID = T.TASK_EXECUTION_ID AND B.SCHEMA_TARGET = T.SCHEMA_TARGET" +
			" WHERE I.JOB_NAME LIKE ? AND E.STATUS = ?";

	private static final String FIELDS = "E.JOB_EXECUTION_ID as JOB_EXECUTION_ID, E.START_TIME as START_TIME," +
			" E.END_TIME as END_TIME, E.STATUS as STATUS, E.EXIT_CODE as EXIT_CODE, E.EXIT_MESSAGE as EXIT_MESSAGE," +
			" E.CREATE_TIME as CREATE_TIME, E.LAST_UPDATED as LAST_UPDATED, E.VERSION as VERSION," +
			" I.JOB_INSTANCE_ID as JOB_INSTANCE_ID, I.JOB_NAME as JOB_NAME, T.TASK_EXECUTION_ID as TASK_EXECUTION_ID," +
			" E.SCHEMA_TARGET as SCHEMA_TARGET";

	private static final String FIELDS_WITH_STEP_COUNT = FIELDS +
			", (SELECT COUNT(*) FROM AGGREGATE_STEP_EXECUTION S WHERE S.JOB_EXECUTION_ID = E.JOB_EXECUTION_ID AND S.SCHEMA_TARGET = E.SCHEMA_TARGET) as STEP_COUNT";

	private static final String GET_JOB_INSTANCE_BY_ID = "SELECT I.JOB_INSTANCE_ID, I.VERSION, I.JOB_NAME, I.JOB_KEY" +
		" FROM AGGREGATE_JOB_INSTANCE I" +
		" WHERE I.JOB_INSTANCE_ID = ? AND I.SCHEMA_TARGET = ?";

	private static final String NAME_FILTER = "I.JOB_NAME LIKE ?";

	private static final String DATE_RANGE_FILTER = "E.START_TIME BETWEEN ? AND ?";

	private static final String JOB_INSTANCE_ID_FILTER = "I.JOB_INSTANCE_ID = ? AND I.SCHEMA_TARGET = ?";

	private static final String STATUS_FILTER = "E.STATUS = ?";

	private static final String NAME_AND_STATUS_FILTER = "I.JOB_NAME LIKE ? AND E.STATUS = ?";

	private static final String TASK_EXECUTION_ID_FILTER =
			"B.JOB_EXECUTION_ID = E.JOB_EXECUTION_ID AND B.SCHEMA_TARGET = E.SCHEMA_TARGET AND B.TASK_EXECUTION_ID = ? AND E.SCHEMA_TARGET = ?";

	private static final String FROM_CLAUSE_TASK_EXEC_BATCH = "JOIN AGGREGATE_TASK_BATCH B ON E.JOB_EXECUTION_ID = B.JOB_EXECUTION_ID AND E.SCHEMA_TARGET = B.SCHEMA_TARGET" +
			" JOIN AGGREGATE_TASK_EXECUTION T ON B.TASK_EXECUTION_ID = T.TASK_EXECUTION_ID AND B.SCHEMA_TARGET = T.SCHEMA_TARGET";

	private static final String FIND_PARAMS_FROM_ID2 = "SELECT JOB_EXECUTION_ID, KEY_NAME, TYPE_CD, "
			+ "STRING_VAL, DATE_VAL, LONG_VAL, DOUBLE_VAL, IDENTIFYING, 'boot2' as SCHEMA_TARGET from %PREFIX%JOB_EXECUTION_PARAMS where JOB_EXECUTION_ID = ?";

	private static final String FIND_PARAMS_FROM_ID3 = "SELECT JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING, 'boot3' as SCHEMA_TARGET" +
			" from %PREFIX%JOB_EXECUTION_PARAMS where JOB_EXECUTION_ID = ?";

	private static final String FIND_JOB_BY = "SELECT I.JOB_INSTANCE_ID as JOB_INSTANCE_ID, I.JOB_NAME as JOB_NAME, I.SCHEMA_TARGET as SCHEMA_TARGET," +
			" E.JOB_EXECUTION_ID as JOB_EXECUTION_ID, E.START_TIME as START_TIME, E.END_TIME as END_TIME, E.STATUS as STATUS, E.EXIT_CODE as EXIT_CODE, E.EXIT_MESSAGE as EXIT_MESSAGE, E.CREATE_TIME as CREATE_TIME," +
			" E.LAST_UPDATED as LAST_UPDATED, E.VERSION as VERSION, T.TASK_EXECUTION_ID as TASK_EXECUTION_ID," +
			" (SELECT COUNT(*) FROM AGGREGATE_STEP_EXECUTION S WHERE S.JOB_EXECUTION_ID = E.JOB_EXECUTION_ID AND S.SCHEMA_TARGET = E.SCHEMA_TARGET) as STEP_COUNT" +
			" from AGGREGATE_JOB_INSTANCE I" +
			" JOIN AGGREGATE_JOB_EXECUTION E ON I.JOB_INSTANCE_ID = E.JOB_INSTANCE_ID AND I.SCHEMA_TARGET = E.SCHEMA_TARGET" +
			" LEFT OUTER JOIN AGGREGATE_TASK_BATCH TT ON E.JOB_EXECUTION_ID = TT.JOB_EXECUTION_ID AND E.SCHEMA_TARGET = TT.SCHEMA_TARGET" +
			" LEFT OUTER JOIN AGGREGATE_TASK_EXECUTION T ON TT.TASK_EXECUTION_ID = T.TASK_EXECUTION_ID AND TT.SCHEMA_TARGET = T.SCHEMA_TARGET";

	private static final String FIND_CTR_STATUS = "SELECT T.TASK_EXECUTION_ID as TASK_EXECUTION_ID, J.EXIT_MESSAGE as CTR_STATUS" +
		" from AGGREGATE_TASK_EXECUTION T" +
		" JOIN AGGREGATE_TASK_BATCH TB ON TB.TASK_EXECUTION_ID=T.TASK_EXECUTION_ID AND TB.SCHEMA_TARGET=T.SCHEMA_TARGET" +
		" JOIN AGGREGATE_JOB_EXECUTION J ON J.JOB_EXECUTION_ID=TB.JOB_EXECUTION_ID AND J.SCHEMA_TARGET=TB.SCHEMA_TARGET" +
		" WHERE T.TASK_EXECUTION_ID in (:taskExecutionIds) " +
		"  AND T.SCHEMA_TARGET = ':schemaTarget'" +
		"  AND (select count(*) from AGGREGATE_TASK_EXECUTION CT" +
		"        where (select count(*) from AGGREGATE_TASK_EXECUTION_PARAMS where" +
		"                    CT.TASK_EXECUTION_ID = TASK_EXECUTION_ID and" +
		"                    CT.SCHEMA_TARGET = SCHEMA_TARGET and" +
		"                    TASK_PARAM = '--spring.cloud.task.parent-schema-target=:schemaTarget') > 0" +
		"    AND CT.PARENT_EXECUTION_ID = T.TASK_EXECUTION_ID) > 0";

	private static final String FIND_JOB_BY_NAME_INSTANCE_ID = FIND_JOB_BY +
			" where I.JOB_NAME = ? AND I.JOB_INSTANCE_ID = ?";

	private static final String FIND_JOB_BY_INSTANCE_ID_SCHEMA = FIND_JOB_BY +
			" where I.JOB_INSTANCE_ID = ? AND I.SCHEMA_TARGET = ?";

	private static final String FIND_JOBS_FIELDS = "I.JOB_INSTANCE_ID as JOB_INSTANCE_ID, I.JOB_NAME as JOB_NAME, I.SCHEMA_TARGET as SCHEMA_TARGET," +
			" E.JOB_EXECUTION_ID as JOB_EXECUTION_ID, E.START_TIME as START_TIME, E.END_TIME as END_TIME, E.STATUS as STATUS, E.EXIT_CODE as EXIT_CODE, E.EXIT_MESSAGE as EXIT_MESSAGE, E.CREATE_TIME as CREATE_TIME," +
			" E.LAST_UPDATED as LAST_UPDATED, E.VERSION as VERSION, T.TASK_EXECUTION_ID as TASK_EXECUTION_ID";

	private static final String FIND_JOBS_FROM = "LEFT OUTER JOIN AGGREGATE_TASK_BATCH TT ON E.JOB_EXECUTION_ID = TT.JOB_EXECUTION_ID AND E.SCHEMA_TARGET = TT.SCHEMA_TARGET" +
			" LEFT OUTER JOIN AGGREGATE_TASK_EXECUTION T ON TT.TASK_EXECUTION_ID = T.TASK_EXECUTION_ID AND TT.SCHEMA_TARGET = T.SCHEMA_TARGET";

	private static final String FIND_JOBS_WHERE = "I.JOB_NAME LIKE ?";

	private static final String FIND_BY_ID_SCHEMA = "E.JOB_EXECUTION_ID = ? AND E.SCHEMA_TARGET = ?";

	private static final String ROW_NUMBER_OPTIMIZATION_ENABLED_PROPERTY = DataFlowPropertyKeys.PREFIX + "task.jdbc.row-number-optimization.enabled";

	private final PagingQueryProvider allExecutionsPagingQueryProvider;

	private final PagingQueryProvider byJobNameAndStatusPagingQueryProvider;

	private final PagingQueryProvider byStatusPagingQueryProvider;

	private final PagingQueryProvider byJobNameWithStepCountPagingQueryProvider;

	private final PagingQueryProvider executionsByDateRangeWithStepCountPagingQueryProvider;

	private final PagingQueryProvider byJobInstanceIdWithStepCountPagingQueryProvider;

	private final PagingQueryProvider byTaskExecutionIdWithStepCountPagingQueryProvider;

	private final PagingQueryProvider jobExecutionsPagingQueryProviderByName;

	private final PagingQueryProvider allExecutionsPagingQueryProviderNoStepCount;

	private final PagingQueryProvider byJobNamePagingQueryProvider;

	private final PagingQueryProvider byJobExecutionIdAndSchemaPagingQueryProvider;

	private final DataSource dataSource;

	private final JdbcTemplate jdbcTemplate;

	private final SchemaService schemaService;

	private final JobServiceContainer jobServiceContainer;

	private final ConfigurableConversionService conversionService = new DefaultConversionService();

	private final boolean useRowNumberOptimization;

	public JdbcAggregateJobQueryDao(
			DataSource dataSource,
			SchemaService schemaService,
			JobServiceContainer jobServiceContainer,
			Environment environment) throws Exception {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.schemaService = schemaService;
		this.jobServiceContainer = jobServiceContainer;
		this.useRowNumberOptimization = determineUseRowNumberOptimization(environment);

		conversionService.addConverter(new DateToStringConverter());
		conversionService.addConverter(new StringToDateConverter());
		Jsr310Converters.getConvertersToRegister().forEach(conversionService::addConverter);

		allExecutionsPagingQueryProvider = getPagingQueryProvider(FIELDS_WITH_STEP_COUNT, FROM_CLAUSE_TASK_EXEC_BATCH, null);
		executionsByDateRangeWithStepCountPagingQueryProvider = getPagingQueryProvider(FIELDS_WITH_STEP_COUNT, FROM_CLAUSE_TASK_EXEC_BATCH, DATE_RANGE_FILTER);
		allExecutionsPagingQueryProviderNoStepCount = getPagingQueryProvider(FROM_CLAUSE_TASK_EXEC_BATCH, null);
		byStatusPagingQueryProvider = getPagingQueryProvider(FROM_CLAUSE_TASK_EXEC_BATCH, STATUS_FILTER);
		byJobNameAndStatusPagingQueryProvider = getPagingQueryProvider(FROM_CLAUSE_TASK_EXEC_BATCH, NAME_AND_STATUS_FILTER);
		byJobNamePagingQueryProvider = getPagingQueryProvider(FROM_CLAUSE_TASK_EXEC_BATCH, NAME_FILTER);
		byJobNameWithStepCountPagingQueryProvider = getPagingQueryProvider(FIELDS_WITH_STEP_COUNT, FROM_CLAUSE_TASK_EXEC_BATCH, NAME_FILTER);
		byJobInstanceIdWithStepCountPagingQueryProvider = getPagingQueryProvider(FIELDS_WITH_STEP_COUNT, FROM_CLAUSE_TASK_EXEC_BATCH, JOB_INSTANCE_ID_FILTER);
		byTaskExecutionIdWithStepCountPagingQueryProvider = getPagingQueryProvider(FIELDS_WITH_STEP_COUNT, FROM_CLAUSE_TASK_EXEC_BATCH, TASK_EXECUTION_ID_FILTER);
		jobExecutionsPagingQueryProviderByName = getPagingQueryProvider(FIND_JOBS_FIELDS, FIND_JOBS_FROM, FIND_JOBS_WHERE, Collections.singletonMap("E.JOB_EXECUTION_ID", Order.DESCENDING));
		byJobExecutionIdAndSchemaPagingQueryProvider = getPagingQueryProvider(FIELDS_WITH_STEP_COUNT, FROM_CLAUSE_TASK_EXEC_BATCH, FIND_BY_ID_SCHEMA);
	}

	private boolean determineUseRowNumberOptimization(Environment environment) {
		boolean supportsRowNumberFunction = determineSupportsRowNumberFunction(this.dataSource);
		boolean rowNumberOptimizationEnabled = environment.getProperty(ROW_NUMBER_OPTIMIZATION_ENABLED_PROPERTY , Boolean.class, Boolean.TRUE);
		return supportsRowNumberFunction && rowNumberOptimizationEnabled;
	}

	@Override
	public Page<JobInstanceExecutions> listJobInstances(String jobName, Pageable pageable) throws NoSuchJobException {
		int total = countJobExecutions(jobName);
		if (total == 0) {
			throw new NoSuchJobException("No Job with that name either current or historic: [" + jobName + "]");
		}
		List<JobInstanceExecutions> taskJobInstancesForJobName = getTaskJobInstancesForJobName(jobName, pageable);
		return new PageImpl<>(taskJobInstancesForJobName, pageable, total);

	}

	@Override
	public void populateCtrStatus(Collection<AggregateTaskExecution> aggregateTaskExecutions) {
		Map<String, List<AggregateTaskExecution>> targets = aggregateTaskExecutions.stream().collect(Collectors.groupingBy(aggregateTaskExecution -> aggregateTaskExecution.getSchemaTarget()));
		final AtomicInteger updated = new AtomicInteger(0);
		for(Map.Entry<String, List<AggregateTaskExecution>> entry : targets.entrySet()) {
			String target = entry.getKey();
			Map<Long, AggregateTaskExecution> aggregateTaskExecutionMap = entry.getValue().stream()
				.collect(Collectors.toMap(AggregateTaskExecution::getExecutionId, Function.identity()));
			String ids = aggregateTaskExecutionMap.keySet()
				.stream()
				.map(Object::toString)
				.collect(Collectors.joining(","));
			String sql = FIND_CTR_STATUS.replace(":taskExecutionIds", ids).replace(":schemaTarget", target);
			LOG.debug("populateCtrStatus:{}", sql);
			jdbcTemplate.query(sql, rs -> {
				Long id = rs.getLong("TASK_EXECUTION_ID");
				String ctrStatus = rs.getString("CTR_STATUS");
				LOG.debug("populateCtrStatus:{}={}", id, ctrStatus);
				AggregateTaskExecution execution = aggregateTaskExecutionMap.get(id);
				Assert.notNull(execution, "Expected AggregateTaskExecution for " + id + " from " + ids);
				updated.incrementAndGet();
				execution.setCtrTaskStatus(ctrStatus);
			});
		}
		LOG.debug("updated {} ctr statuses", updated);
	}

	@Override
	public JobInstanceExecutions getJobInstanceExecution(String jobName, long instanceId) {
		LOG.debug("getJobInstanceExecution:{}:{}:{}", jobName, instanceId, FIND_JOB_BY_NAME_INSTANCE_ID);
		List<JobInstanceExecutions> executions = jdbcTemplate.query(FIND_JOB_BY_NAME_INSTANCE_ID, new JobInstanceExecutionsExtractor(true), jobName, instanceId);
		if (executions == null || executions.isEmpty()) {
			return null;
		} else if (executions.size() > 1) {
			throw new RuntimeException("Expected a single JobInstanceExecutions not " + executions.size());
		}
		return executions.get(0);
	}

	@Override
	public JobInstanceExecutions getJobInstanceExecutions(long jobInstanceId, String schemaTarget) {
		List<JobInstanceExecutions> executions = jdbcTemplate.query(FIND_JOB_BY_INSTANCE_ID_SCHEMA, new JobInstanceExecutionsExtractor(true), jobInstanceId, schemaTarget);
		if (executions == null || executions.isEmpty()) {
			return null;
		} else if (executions.size() > 1) {
			throw new RuntimeException("Expected a single JobInstanceExecutions not " + executions.size());
		}
		JobInstanceExecutions jobInstanceExecution = executions.get(0);
		if (!ObjectUtils.isEmpty(jobInstanceExecution.getTaskJobExecutions())) {
			jobInstanceExecution.getTaskJobExecutions().forEach((execution) ->
				jobServiceContainer.get(execution.getSchemaTarget()).addStepExecutions(execution.getJobExecution())
			);
		}
		return jobInstanceExecution;
	}

	@Override
	public JobInstance getJobInstance(long id, String schemaTarget) throws NoSuchJobInstanceException {
		List<JobInstance> instances = jdbcTemplate.query(GET_JOB_INSTANCE_BY_ID, new JobInstanceExtractor(), id, schemaTarget);
		if (ObjectUtils.isEmpty(instances)) {
			throw new NoSuchJobInstanceException(String.format("JobInstance with id=%d does not exist", id));
		} else if (instances.size() > 1) {
			throw new NoSuchJobInstanceException(String.format("More than one Job Instance exists for ID %d ", id));
		}
		return instances.get(0);
	}

	@Override
	public Page<TaskJobExecution> listJobExecutions(String jobName, BatchStatus status, Pageable pageable) throws NoSuchJobExecutionException {
		int total = countJobExecutions(jobName, status);
		List<TaskJobExecution> executions = getJobExecutions(jobName, status, getPageOffset(pageable), pageable.getPageSize());
		Assert.isTrue(total >= executions.size(), () -> "Expected total at least " + executions.size() + " not " + total);
		return new PageImpl<>(executions, pageable, total);
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsBetween(Date fromDate, Date toDate, Pageable pageable) {
		int total = countJobExecutionsByDate(fromDate, toDate);
		List<TaskJobExecution> executions = total > 0
				? getTaskJobExecutionsByDate(fromDate, toDate, getPageOffset(pageable), pageable.getPageSize())
				: Collections.emptyList();
		return new PageImpl<>(executions, pageable, total);
	}


	@Override
	public Page<TaskJobExecution> listJobExecutionsWithSteps(Pageable pageable) {
		int total = countJobExecutions();
		List<TaskJobExecution> jobExecutions = total > 0
				? getJobExecutionsWithStepCount(getPageOffset(pageable), pageable.getPageSize())
				: Collections.emptyList();
		return new PageImpl<>(jobExecutions, pageable, total);
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsWithStepCount(Pageable pageable) {
		int total = countJobExecutions();
		List<TaskJobExecution> jobExecutions = total > 0
				? getJobExecutionsWithStepCount(getPageOffset(pageable), pageable.getPageSize())
				: Collections.emptyList();
		return new PageImpl<>(jobExecutions, pageable, total);
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByJobInstanceId(int jobInstanceId, String schemaTarget, Pageable pageable) {
		int total = countJobExecutionsByInstanceId(jobInstanceId, schemaTarget);
		List<TaskJobExecution> jobExecutions = total > 0
				? getJobExecutionsWithStepCountFilteredByJobInstanceId(jobInstanceId, schemaTarget, getPageOffset(pageable), pageable.getPageSize())
				: Collections.emptyList();
		return new PageImpl<>(jobExecutions, pageable, total);
	}


	@Override
	public Page<TaskJobExecution> listJobExecutionsForJobWithStepCountFilteredByTaskExecutionId(int taskExecutionId, String schemaTarget, Pageable pageable) {
		int total = countJobExecutionsByTaskExecutionId(taskExecutionId, schemaTarget);
		List<TaskJobExecution> jobExecutions = total > 0
				? getJobExecutionsWithStepCountFilteredByTaskExecutionId(taskExecutionId, schemaTarget, getPageOffset(pageable), pageable.getPageSize())
				: Collections.emptyList();
		return new PageImpl<>(jobExecutions, pageable, total);
	}

	@Override
	public Page<TaskJobExecution> listJobExecutionsForJobWithStepCount(String jobName, Pageable pageable) throws NoSuchJobException {
		int total = countJobExecutions(jobName);
		if(total == 0) {
			throw new NoSuchJobException("No Job with that name either current or historic: [" + jobName + "]");
		}
		List<TaskJobExecution> jobExecutions = total > 0
				? getJobExecutionsWithStepCount(jobName, getPageOffset(pageable), pageable.getPageSize())
				: Collections.emptyList();
		return new PageImpl<>(jobExecutions, pageable, total);
	}

	@Override
	public TaskJobExecution getJobExecution(long jobExecutionId, String schemaTarget) throws NoSuchJobExecutionException {
		List<TaskJobExecution> jobExecutions = getJobExecutionPage(jobExecutionId, schemaTarget);
		if (jobExecutions.isEmpty()) {
			throw new NoSuchJobExecutionException(String.format("Job id %s for schema target %s not found", jobExecutionId, schemaTarget));
		}
		if (jobExecutions.size() > 1) {
			LOG.debug("Too many job executions:{}", jobExecutions);
			LOG.warn("Expected only 1 job for {}: not {}", jobExecutionId, jobExecutions.size());
		}

		TaskJobExecution taskJobExecution = jobExecutions.get(0);
		JobService jobService = jobServiceContainer.get(taskJobExecution.getSchemaTarget());
		jobService.addStepExecutions(taskJobExecution.getJobExecution());
		return taskJobExecution;
	}

	private List<TaskJobExecution> getJobExecutionPage(long jobExecutionId, String schemaTarget) {
		return queryForProvider(
				byJobExecutionIdAndSchemaPagingQueryProvider,
				new JobExecutionRowMapper(true),
				0,
				2,
				jobExecutionId,
				schemaTarget
		);
	}

	private int countJobExecutions() {
		LOG.debug("countJobExecutions:{}", GET_COUNT);
		Integer count = jdbcTemplate.queryForObject(GET_COUNT, Integer.class);
		return count != null ? count : 0;
	}

	private int countJobExecutionsByDate(Date fromDate, Date toDate) {
		Assert.notNull(fromDate, "fromDate must not be null");
		Assert.notNull(toDate, "toDate must not be null");
		LOG.debug("countJobExecutionsByDate:{}:{}:{}", fromDate, toDate, GET_COUNT_BY_DATE);
		Integer count = jdbcTemplate.queryForObject(GET_COUNT_BY_DATE, Integer.class, fromDate, toDate);
		return count != null ? count : 0;
	}

	private int countJobExecutions(String jobName) {
		LOG.debug("countJobExecutions:{}:{}", jobName, GET_COUNT_BY_JOB_NAME);
		Integer count = jdbcTemplate.queryForObject(GET_COUNT_BY_JOB_NAME, Integer.class, jobName);
		return count != null ? count : 0;
	}

	private int countJobExecutions(BatchStatus status) {
		LOG.debug("countJobExecutions:{}:{}", status, GET_COUNT_BY_STATUS);
		Integer count = jdbcTemplate.queryForObject(GET_COUNT_BY_STATUS, Integer.class, status.name());
		return count != null ? count : 0;
	}

	private int countJobExecutions(String jobName, BatchStatus status) {
		LOG.debug("countJobExecutions:{}:{}", jobName, status);
		Integer count;
		if (StringUtils.hasText(jobName) && status != null) {
			LOG.debug("countJobExecutions:{}:{}:{}", jobName, status, GET_COUNT_BY_JOB_NAME_AND_STATUS);
			count = jdbcTemplate.queryForObject(GET_COUNT_BY_JOB_NAME_AND_STATUS, Integer.class, jobName, status.name());
		} else if (status != null) {
			LOG.debug("countJobExecutions:{}:{}", status, GET_COUNT_BY_STATUS);
			count = jdbcTemplate.queryForObject(GET_COUNT_BY_STATUS, Integer.class, status.name());
		} else if (StringUtils.hasText(jobName)) {
			LOG.debug("countJobExecutions:{}:{}", jobName, GET_COUNT_BY_JOB_NAME);
			count = jdbcTemplate.queryForObject(GET_COUNT_BY_JOB_NAME, Integer.class, jobName);
		} else {
			count = jdbcTemplate.queryForObject(GET_COUNT, Integer.class);
		}
		return count != null ? count : 0;
	}

	private int countJobExecutionsByInstanceId(int jobInstanceId, String schemaTarget) {
		if (!StringUtils.hasText(schemaTarget)) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		LOG.debug("countJobExecutionsByInstanceId:{}:{}:{}", jobInstanceId, schemaTarget, GET_COUNT_BY_JOB_INSTANCE_ID);
		Integer count = jdbcTemplate.queryForObject(GET_COUNT_BY_JOB_INSTANCE_ID, Integer.class, jobInstanceId, schemaTarget);
		return count != null ? count : 0;
	}

	private int countJobExecutionsByTaskExecutionId(int taskExecutionId, String schemaTarget) {
		if (!StringUtils.hasText(schemaTarget)) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		LOG.debug("countJobExecutionsByTaskExecutionId:{}:{}:{}", taskExecutionId, schemaTarget, GET_COUNT_BY_TASK_EXECUTION_ID);
		Integer count = jdbcTemplate.queryForObject(GET_COUNT_BY_TASK_EXECUTION_ID, Integer.class, taskExecutionId, schemaTarget);
		return count != null ? count : 0;
	}

	private List<TaskJobExecution> getJobExecutionsWithStepCountFilteredByJobInstanceId(
			int jobInstanceId,
			String schemaTarget,
			int start,
			int count
	) {
		if (!StringUtils.hasText(schemaTarget)) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		return queryForProvider(
				byJobInstanceIdWithStepCountPagingQueryProvider,
				new JobExecutionRowMapper(true),
				start,
				count,
				jobInstanceId,
				schemaTarget
		);
	}

	private List<TaskJobExecution> getJobExecutionsWithStepCountFilteredByTaskExecutionId(
			int taskExecutionId,
			String schemaTarget,
			int start,
			int count
	) {
		if (!StringUtils.hasText(schemaTarget)) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		return queryForProvider(
				byTaskExecutionIdWithStepCountPagingQueryProvider,
				new JobExecutionRowMapper(true),
				start,
				count,
				taskExecutionId,
				schemaTarget
		);
	}

	private List<TaskJobExecution> getJobExecutions(String jobName, BatchStatus status, int start, int count) throws NoSuchJobExecutionException {
		if (StringUtils.hasText(jobName) && status != null) {
			return queryForProvider(byJobNameAndStatusPagingQueryProvider, new JobExecutionRowMapper(false), start, count, jobName, status.name());
		} else if (status != null) {
			return queryForProvider(byStatusPagingQueryProvider, new JobExecutionRowMapper(false), start, count, status.name());
		} else if (StringUtils.hasText(jobName)) {
			return queryForProvider(byJobNamePagingQueryProvider, new JobExecutionRowMapper(false), start, count, jobName);
		}
		return queryForProvider(allExecutionsPagingQueryProviderNoStepCount, new JobExecutionRowMapper(false), start, count);
	}

	private List<TaskJobExecution> getJobExecutionsWithStepCount(String jobName, int start, int count) {
		return queryForProvider(byJobNameWithStepCountPagingQueryProvider, new JobExecutionRowMapper(true), start, count, jobName);
	}

	public List<TaskJobExecution> getJobExecutionsWithStepCount(int start, int count) {
		return queryForProvider(allExecutionsPagingQueryProvider, new JobExecutionRowMapper(true), start, count);
	}

	protected JobParameters getJobParameters(Long executionId, String schemaTarget) {
		final Map<String, JobParameter> map = new HashMap<>();
		final SchemaVersionTarget schemaVersionTarget = schemaService.getTarget(schemaTarget);
		boolean boot2 = AppBootSchemaVersion.BOOT2 == schemaVersionTarget.getSchemaVersion();
		RowCallbackHandler handler;
		if (boot2) {
			handler = rs -> {
				String keyName = rs.getString("KEY_NAME");
				JobParameter.ParameterType type = JobParameter.ParameterType.valueOf(rs.getString("TYPE_CD"));
				boolean identifying = rs.getString("IDENTIFYING").equalsIgnoreCase("Y");
				JobParameter value;
				switch (type) {
					case STRING:
						value = new JobParameter(rs.getString("STRING_VAL"), identifying);
						break;
					case LONG:
						long longValue = rs.getLong("LONG_VAL");
						value = new JobParameter(rs.wasNull() ? null : longValue, identifying);
						break;
					case DOUBLE:
						double doubleValue = rs.getDouble("DOUBLE_VAL");
						value = new JobParameter(rs.wasNull() ? null : doubleValue, identifying);
						break;
					case DATE:
						value = new JobParameter(rs.getTimestamp("DATE_VAL"), identifying);
						break;
					default:
						LOG.error("Unknown type:{} for {}", type, keyName);
						return;
				}
				map.put(keyName, value);
			};
		} else {
			handler = rs -> {
				String parameterName = rs.getString("PARAMETER_NAME");
				Class<?> parameterType = null;
				try {
					parameterType = Class.forName(rs.getString("PARAMETER_TYPE"));
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
				String stringValue = rs.getString("PARAMETER_VALUE");
				boolean identifying = rs.getString("IDENTIFYING").equalsIgnoreCase("Y");
				Object typedValue = conversionService.convert(stringValue, parameterType);
				JobParameter value;
				if (typedValue instanceof String) {
					value = new JobParameter((String) typedValue, identifying);
				} else if (typedValue instanceof Date) {
					value = new JobParameter((Date) typedValue, identifying);
				} else if (typedValue instanceof Double) {
					value = new JobParameter((Double) typedValue, identifying);
				} else if (typedValue instanceof Long) {
					value = new JobParameter((Long) typedValue, identifying);
				} else if (typedValue instanceof Number) {
					value = new JobParameter(((Number) typedValue).doubleValue(), identifying);
				} else if (typedValue instanceof Instant) {
					value = new JobParameter(new Date(((Instant) typedValue).toEpochMilli()), identifying);
				} else {

					value = new JobParameter(typedValue != null ? typedValue.toString() : null, identifying);
				}
				map.put(parameterName, value);
			};
		}

		jdbcTemplate.query(
				getQuery(
						boot2 ? FIND_PARAMS_FROM_ID2 : FIND_PARAMS_FROM_ID3,
						schemaVersionTarget.getBatchPrefix()
				),
				handler,
				executionId
		);
		return new JobParameters(map);
	}

	private <T, P extends PagingQueryProvider, M extends RowMapper<T>> List<T> queryForProvider(P pagingQueryProvider, M mapper, int start, int count, Object... arguments) {
		if (start <= 0) {
			String sql = pagingQueryProvider.generateFirstPageQuery(count);
			if (LOG.isDebugEnabled()) {
				LOG.debug("queryFirstPage:{}:{}:{}:{}", sql, start, count, Arrays.asList(arguments));
			}
			return jdbcTemplate.query(sql, mapper, arguments);
		} else {
			try {
				String sqlJump = pagingQueryProvider.generateJumpToItemQuery(start, count);
				if (LOG.isDebugEnabled()) {
					LOG.debug("queryJumpToItem:{}:{}:{}:{}", sqlJump, start, count, Arrays.asList(arguments));
				}
				Long startValue;
				startValue = jdbcTemplate.queryForObject(sqlJump, Long.class, arguments);
				List<Object> args = new ArrayList<>(Arrays.asList(arguments));
				args.add(startValue);
				String sql = pagingQueryProvider.generateRemainingPagesQuery(count);
				if (LOG.isDebugEnabled()) {
					LOG.debug("queryRemaining:{}:{}:{}:{}", sql, start, count, args);
				}
				return jdbcTemplate.query(sql, mapper, args.toArray());
			} catch (IncorrectResultSizeDataAccessException x) {
				return Collections.emptyList();
			}
		}
	}

	private <T, P extends PagingQueryProvider, R extends ResultSetExtractor<List<T>>> List<T> queryForProvider(P pagingQueryProvider, R extractor, int start, int count, Object... arguments) {
		if (start <= 0) {
			String sql = pagingQueryProvider.generateFirstPageQuery(count);
			if (LOG.isDebugEnabled()) {
				LOG.debug("queryFirstPage:{}:{}:{}:{}", sql, start, count, Arrays.asList(arguments));
			}
			return jdbcTemplate.query(sql, extractor, arguments);
		} else {
			String sqlJump = pagingQueryProvider.generateJumpToItemQuery(start, count);
			if (LOG.isDebugEnabled()) {
				LOG.debug("queryJumpToItem:{}:{}:{}:{}", sqlJump, start, count, Arrays.asList(arguments));
			}
			Long startValue = jdbcTemplate.queryForObject(sqlJump, Long.class, arguments);
			List<Object> args = new ArrayList<>(Arrays.asList(arguments));
			args.add(startValue);
			String sql = pagingQueryProvider.generateRemainingPagesQuery(count);
			if (LOG.isDebugEnabled()) {
				LOG.debug("queryRemaining:{}:{}:{}:{}", sql, start, count, args);
			}
			return jdbcTemplate.query(sql, extractor, args.toArray());
		}
	}

	private List<JobInstanceExecutions> getTaskJobInstancesForJobName(String jobName, Pageable pageable) {
		Assert.notNull(pageable, "pageable must not be null");
		Assert.notNull(jobName, "jobName must not be null");
		int start = getPageOffset(pageable);
		int count = pageable.getPageSize();
		return queryForProvider(jobExecutionsPagingQueryProviderByName, new JobInstanceExecutionsExtractor(false), start, count, jobName);
	}

	private TaskJobExecution createJobExecutionFromResultSet(ResultSet rs, int row, boolean readStepCount) throws SQLException {
		long taskExecutionId = rs.getLong("TASK_EXECUTION_ID");
		Long jobExecutionId = rs.getLong("JOB_EXECUTION_ID");
		JobExecution jobExecution;
		String schemaTarget = rs.getString("SCHEMA_TARGET");
		JobParameters jobParameters = getJobParameters(jobExecutionId, schemaTarget);

		JobInstance jobInstance = new JobInstance(rs.getLong("JOB_INSTANCE_ID"), rs.getString("JOB_NAME"));
		jobExecution = new JobExecution(jobInstance, jobParameters);
		jobExecution.setId(jobExecutionId);

		jobExecution.setStartTime(rs.getTimestamp("START_TIME"));
		jobExecution.setEndTime(rs.getTimestamp("END_TIME"));
		jobExecution.setStatus(BatchStatus.valueOf(rs.getString("STATUS")));
		jobExecution.setExitStatus(new ExitStatus(rs.getString("EXIT_CODE"), rs.getString("EXIT_MESSAGE")));
		jobExecution.setCreateTime(rs.getTimestamp("CREATE_TIME"));
		jobExecution.setLastUpdated(rs.getTimestamp("LAST_UPDATED"));
		jobExecution.setVersion(rs.getInt("VERSION"));

		return readStepCount ?
				new TaskJobExecution(taskExecutionId, jobExecution, true, rs.getInt("STEP_COUNT"), schemaTarget) :
				new TaskJobExecution(taskExecutionId, jobExecution, true, schemaTarget);
	}

	private List<TaskJobExecution> getTaskJobExecutionsByDate(Date startDate, Date endDate, int start, int count) {
		return queryForProvider(
				executionsByDateRangeWithStepCountPagingQueryProvider,
				new JobExecutionRowMapper(true),
				start,
				count,
				startDate,
				endDate
		);
	}
	private class JobInstanceExtractor implements ResultSetExtractor<List<JobInstance>> {

		@Override
		public List<JobInstance> extractData(ResultSet rs) throws SQLException,
			DataAccessException {
			List<JobInstance> jobInstances = new ArrayList();
			while (rs.next()) {
				jobInstances.add( new JobInstance(rs.getLong("JOB_INSTANCE_ID"), rs.getString("JOB_NAME")));
			}
			return jobInstances;
		}
	}

	private class JobInstanceExecutionsExtractor implements ResultSetExtractor<List<JobInstanceExecutions>> {
		final boolean readStepCount;

		public JobInstanceExecutionsExtractor(boolean readStepCount) {
			this.readStepCount = readStepCount;
		}

		@Override
		public List<JobInstanceExecutions> extractData(ResultSet rs) throws SQLException,
				DataAccessException {
			final Map<Long, List<TaskJobExecution>> taskJobExecutions = new HashMap<>();
			final Map<Long, JobInstance> jobInstances = new TreeMap<>();

			while (rs.next()) {
				Long id = rs.getLong("JOB_INSTANCE_ID");
				JobInstance jobInstance;
				if (!jobInstances.containsKey(id)) {
					jobInstance = new JobInstance(id, rs.getString("JOB_NAME"));
					jobInstances.put(id, jobInstance);
				} else {
					jobInstance = jobInstances.get(id);
				}
				long taskId = rs.getLong("TASK_EXECUTION_ID");
				if (!rs.wasNull()) {
					String schemaTarget = rs.getString("SCHEMA_TARGET");
					List<TaskJobExecution> executions = taskJobExecutions.computeIfAbsent(id, k -> new ArrayList<>());
					long jobExecutionId = rs.getLong("JOB_EXECUTION_ID");
					JobParameters jobParameters = getJobParameters(jobExecutionId, schemaTarget);
					JobExecution jobExecution = new JobExecution(jobInstance, jobExecutionId, jobParameters, null);

					int stepCount = readStepCount ? rs.getInt("STEP_COUNT") : 0;
					TaskJobExecution execution = new TaskJobExecution(taskId, jobExecution, true, stepCount, schemaTarget);
					executions.add(execution);
				}
			}
			return jobInstances.values()
					.stream()
					.map(jobInstance -> new JobInstanceExecutions(jobInstance, taskJobExecutions.get(jobInstance.getInstanceId())))
					.collect(Collectors.toList());
		}

	}

	class JobExecutionRowMapper implements RowMapper<TaskJobExecution> {
		boolean readStepCount;

		JobExecutionRowMapper(boolean readStepCount) {
			this.readStepCount = readStepCount;
		}

		@Override
		public TaskJobExecution mapRow(ResultSet rs, int rowNum) throws SQLException {
			return createJobExecutionFromResultSet(rs, rowNum, readStepCount);
		}

	}

	protected String getQuery(String base, String tablePrefix) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}

	private int getPageOffset(Pageable pageable) {
		if (pageable.getOffset() > (long) Integer.MAX_VALUE) {
			throw new OffsetOutOfBoundsException("The pageable offset requested for this query is greater than MAX_INT.");
		}
		return (int) pageable.getOffset();
	}

	/**
	 * @return a {@link PagingQueryProvider} for all job executions
	 * @throws Exception if page provider is not created.
	 */
	private PagingQueryProvider getPagingQueryProvider() throws Exception {
		return getPagingQueryProvider(null, null, null, Collections.emptyMap());
	}

	/**
	 * @return a {@link PagingQueryProvider} for all job executions with the
	 * provided where clause
	 * @throws Exception if page provider is not created.
	 */
	private PagingQueryProvider getPagingQueryProvider(String whereClause) throws Exception {
		return getPagingQueryProvider(null, null, whereClause, Collections.emptyMap());
	}

	/**
	 * @return a {@link PagingQueryProvider} with a where clause to narrow the
	 * query
	 * @throws Exception if page provider is not created.
	 */
	private PagingQueryProvider getPagingQueryProvider(String fromClause, String whereClause) throws Exception {
		return getPagingQueryProvider(null, fromClause, whereClause, Collections.emptyMap());
	}

	private PagingQueryProvider getPagingQueryProvider(String fields, String fromClause, String whereClause) throws Exception {
		return getPagingQueryProvider(fields, fromClause, whereClause, Collections.emptyMap());
	}

	/**
	 * @return a {@link PagingQueryProvider} with a where clause to narrow the
	 * query
	 * @throws Exception if page provider is not created.
	 */
	private PagingQueryProvider getPagingQueryProvider(String fields, String fromClause, String whereClause, Map<String, Order> sortKeys) throws Exception {
		SqlPagingQueryProviderFactoryBean factory = new SafeSqlPagingQueryProviderFactoryBean();
		factory.setDataSource(dataSource);
		fromClause = "AGGREGATE_JOB_INSTANCE I JOIN AGGREGATE_JOB_EXECUTION E ON I.JOB_INSTANCE_ID=E.JOB_INSTANCE_ID AND I.SCHEMA_TARGET=E.SCHEMA_TARGET" + (fromClause == null ? "" : " " + fromClause);
		factory.setFromClause(fromClause);
		if (fields == null) {
			fields = FIELDS;
		}
		if (fields.contains("E.JOB_EXECUTION_ID") && this.useRowNumberOptimization) {
			Order order = sortKeys.get("E.JOB_EXECUTION_ID");
			String orderString = (order == null || order == Order.DESCENDING) ? "DESC" : "ASC";
			fields += ", ROW_NUMBER() OVER (PARTITION BY E.JOB_EXECUTION_ID ORDER BY E.JOB_EXECUTION_ID " + orderString + ") as RN";
		}
		factory.setSelectClause(fields);
		if (sortKeys.isEmpty()) {
			sortKeys = Collections.singletonMap("E.JOB_EXECUTION_ID", Order.DESCENDING);
		}
		factory.setSortKeys(sortKeys);
		factory.setWhereClause(whereClause);
		return factory.getObject();
	}

	private boolean determineSupportsRowNumberFunction(DataSource dataSource) {
		try {
			return DatabaseType.supportsRowNumberFunction(dataSource);
		}
		catch (Exception e) {
			LOG.warn("Unable to determine if DB supports ROW_NUMBER() function (reason: " + e.getMessage() + ")", e);
		}
		return false;
	}

	/**
	 * A {@link SqlPagingQueryProviderFactoryBean} specialization that overrides the {@code Oracle, MSSQL, and DB2}
	 * paging {@link SafeOraclePagingQueryProvider provider} with an implementation that properly handles sort aliases.
	 * <p><b>NOTE:</b> nested within the aggregate DAO as this is the only place that needs this specialization.
	 */
	static class SafeSqlPagingQueryProviderFactoryBean extends SqlPagingQueryProviderFactoryBean {

		private DataSource dataSource;

		@Override
		public void setDataSource(DataSource dataSource) {
			super.setDataSource(dataSource);
			this.dataSource = dataSource;
		}

		@Override
		public PagingQueryProvider getObject() throws Exception {
			PagingQueryProvider provider = super.getObject();
			if (provider instanceof OraclePagingQueryProvider) {
				provider = new SafeOraclePagingQueryProvider((AbstractSqlPagingQueryProvider) provider, this.dataSource);
			}
			else if (provider instanceof SqlServerPagingQueryProvider) {
				provider = new SafeSqlServerPagingQueryProvider((SqlServerPagingQueryProvider) provider, this.dataSource);
			}
			else if (provider instanceof Db2PagingQueryProvider) {
				provider = new SafeDb2PagingQueryProvider((Db2PagingQueryProvider) provider, this.dataSource);
			}
			return provider;
		}

	}

	/**
	 * A {@link AbstractSqlPagingQueryProvider paging provider} for {@code Oracle} that works around the fact that the
	 * Oracle provider in Spring Batch 4.x does not properly handle sort aliases when using nested {@code ROW_NUMBER}
	 * clauses.
	 */
	static class SafeOraclePagingQueryProvider extends AbstractSqlPagingQueryProvider {

		SafeOraclePagingQueryProvider(AbstractSqlPagingQueryProvider delegate, DataSource dataSource) {
			// Have to use reflection to retrieve the provider fields
			this.setFromClause(extractField(delegate, "fromClause", String.class));
			this.setWhereClause(extractField(delegate, "whereClause", String.class));
			this.setSortKeys(extractField(delegate, "sortKeys", Map.class));
			this.setSelectClause(extractField(delegate, "selectClause", String.class));
			this.setGroupClause(extractField(delegate, "groupClause", String.class));
			try {
				this.init(dataSource);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private <T> T extractField(AbstractSqlPagingQueryProvider target, String fieldName, Class<T> fieldType) {
			Field field = ReflectionUtils.findField(AbstractSqlPagingQueryProvider.class, fieldName, fieldType);
			ReflectionUtils.makeAccessible(field);
			return (T) ReflectionUtils.getField(field, target);
		}

		@Override
		public String generateFirstPageQuery(int pageSize) {
			return generateRowNumSqlQuery(false, pageSize);
		}

		@Override
		public String generateRemainingPagesQuery(int pageSize) {
			return generateRowNumSqlQuery(true, pageSize);
		}

		@Override
		public String generateJumpToItemQuery(int itemIndex, int pageSize) {
			int page = itemIndex / pageSize;
			int offset = (page * pageSize);
			offset = (offset == 0) ? 1 : offset;
			String sortKeyInnerSelect = this.getSortKeySelect(true);
			String sortKeyOuterSelect = this.getSortKeySelect(false);
			return SqlPagingQueryUtils.generateRowNumSqlQueryWithNesting(this, sortKeyInnerSelect, sortKeyOuterSelect,
					false, "TMP_ROW_NUM = " + offset);
		}

		private String getSortKeySelect(boolean withAliases) {
			StringBuilder sql = new StringBuilder();
			Map<String, Order> sortKeys = (withAliases) ? this.getSortKeys() : this.getSortKeysWithoutAliases();
			sql.append(sortKeys.keySet().stream().collect(Collectors.joining(",")));
			return sql.toString();
		}

		// Taken from SqlPagingQueryUtils.generateRowNumSqlQuery but use sortKeysWithoutAlias
		// for outer sort condition.
		private String generateRowNumSqlQuery(boolean remainingPageQuery, int pageSize) {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM (SELECT ").append(getSelectClause());
			sql.append(" FROM ").append(this.getFromClause());
			if (StringUtils.hasText(this.getWhereClause())) {
				sql.append(" WHERE ").append(this.getWhereClause());
			}
			if (StringUtils.hasText(this.getGroupClause())) {
				sql.append(" GROUP BY ").append(this.getGroupClause());
			}
			// inner sort by
			sql.append(" ORDER BY ").append(SqlPagingQueryUtils.buildSortClause(this));
			sql.append(") WHERE ").append("ROWNUM <= " + pageSize);
			if (remainingPageQuery) {
				sql.append(" AND ");
				// For the outer sort we want to use sort keys w/o aliases. However,
				// SqlPagingQueryUtils.buildSortConditions does not allow sort keys to be passed in.
				// Therefore, we temporarily set the 'sortKeys' for the call to 'buildSortConditions'.
				// The alternative is to clone the 'buildSortConditions' method here and allow the sort keys to be
				// passed in BUT method is gigantic and this approach is the lesser of the two evils.
				Map<String, Order> originalSortKeys = this.getSortKeys();
				this.setSortKeys(this.getSortKeysWithoutAliases());
				try {
					SqlPagingQueryUtils.buildSortConditions(this, sql);
				}
				finally {
					this.setSortKeys(originalSortKeys);
				}
			}
			return sql.toString();
		}
	}

	/**
	 * A {@link SqlServerPagingQueryProvider paging provider} for {@code MSSQL} that works around the fact that the
	 * MSSQL provider in Spring Batch 4.x does not properly handle sort aliases when generating jump to page queries.
	 */
	static class SafeSqlServerPagingQueryProvider extends SqlServerPagingQueryProvider {

		SafeSqlServerPagingQueryProvider(SqlServerPagingQueryProvider delegate, DataSource dataSource) {
			// Have to use reflection to retrieve the provider fields
			this.setFromClause(extractField(delegate, "fromClause", String.class));
			this.setWhereClause(extractField(delegate, "whereClause", String.class));
			this.setSortKeys(extractField(delegate, "sortKeys", Map.class));
			this.setSelectClause(extractField(delegate, "selectClause", String.class));
			this.setGroupClause(extractField(delegate, "groupClause", String.class));
			try {
				this.init(dataSource);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private <T> T extractField(AbstractSqlPagingQueryProvider target, String fieldName, Class<T> fieldType) {
			Field field = ReflectionUtils.findField(AbstractSqlPagingQueryProvider.class, fieldName, fieldType);
			ReflectionUtils.makeAccessible(field);
			return (T) ReflectionUtils.getField(field, target);
		}

		@Override
		protected String getOverClause() {
			// Overrides the parent impl to use 'getSortKeys' instead of 'getSortKeysWithoutAliases'
			StringBuilder sql = new StringBuilder();
			sql.append(" ORDER BY ").append(SqlPagingQueryUtils.buildSortClause(this.getSortKeys()));
			return sql.toString();
		}

	}

	/**
	 * A {@link Db2PagingQueryProvider paging provider} for {@code DB2} that works around the fact that the
	 * DB2 provider in Spring Batch 4.x does not properly handle sort aliases when generating jump to page queries.
	 */
	static class SafeDb2PagingQueryProvider extends Db2PagingQueryProvider {

		SafeDb2PagingQueryProvider(Db2PagingQueryProvider delegate, DataSource dataSource) {
			// Have to use reflection to retrieve the provider fields
			this.setFromClause(extractField(delegate, "fromClause", String.class));
			this.setWhereClause(extractField(delegate, "whereClause", String.class));
			this.setSortKeys(extractField(delegate, "sortKeys", Map.class));
			this.setSelectClause(extractField(delegate, "selectClause", String.class));
			this.setGroupClause(extractField(delegate, "groupClause", String.class));
			try {
				this.init(dataSource);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private <T> T extractField(AbstractSqlPagingQueryProvider target, String fieldName, Class<T> fieldType) {
			Field field = ReflectionUtils.findField(AbstractSqlPagingQueryProvider.class, fieldName, fieldType);
			ReflectionUtils.makeAccessible(field);
			return (T) ReflectionUtils.getField(field, target);
		}

		@Override
		protected String getOverClause() {
			// Overrides the parent impl to use 'getSortKeys' instead of 'getSortKeysWithoutAliases'
			StringBuilder sql = new StringBuilder();
			sql.append(" ORDER BY ").append(SqlPagingQueryUtils.buildSortClause(this.getSortKeys()));
			return sql.toString();
		}

	}
}
