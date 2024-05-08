/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.cloud.dataflow.server.db.migration;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.dao.JdbcJobInstanceDao;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.dataflow.core.database.support.DatabaseType;
import org.springframework.cloud.dataflow.core.database.support.MultiSchemaIncrementerFactory;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.schema.service.impl.DefaultSchemaService;
import org.springframework.cloud.dataflow.server.repository.TaskBatchDaoContainer;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionDaoContainer;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

/**
 * Test utility related to job execution test data setup.
 */
class JobExecutionTestUtils
{
	private final TaskExecutionDaoContainer taskExecutionDaoContainer;

	private final TaskBatchDaoContainer taskBatchDaoContainer;

	JobExecutionTestUtils(
			TaskExecutionDaoContainer taskExecutionDaoContainer,
			TaskBatchDaoContainer taskBatchDaoContainer
		) {
		this.taskExecutionDaoContainer = taskExecutionDaoContainer;
		this.taskBatchDaoContainer = taskBatchDaoContainer;
	}

	TaskExecution createSampleJob(String jobName, int jobExecutionCount, BatchStatus batchStatus, JobParameters jobParameters, SchemaVersionTarget schemaVersionTarget) {
		String schemaVersion = schemaVersionTarget.getName();

		TaskExecutionDao taskExecutionDao = this.taskExecutionDaoContainer.get(schemaVersion);
		DataSource dataSource = (DataSource) ReflectionTestUtils.getField(taskExecutionDao, JdbcTaskExecutionDao.class, "dataSource");
		NamedParameterJdbcTemplate namedParamJdbcTemplate = (NamedParameterJdbcTemplate) ReflectionTestUtils.getField(taskExecutionDao, JdbcTaskExecutionDao.class, "jdbcTemplate");
		JdbcTemplate jdbcTemplate = namedParamJdbcTemplate.getJdbcTemplate();
		DataFieldMaxValueIncrementerFactory incrementerFactory = new MultiSchemaIncrementerFactory(dataSource);
		DatabaseType incrementerFallbackType = determineIncrementerFallbackType(dataSource);

		JdbcJobInstanceDao jobInstanceDao = new JdbcJobInstanceDao();
		jobInstanceDao.setJdbcTemplate(jdbcTemplate);
		jobInstanceDao.setTablePrefix(schemaVersionTarget.getBatchPrefix());
		jobInstanceDao.setJobIncrementer(incrementerFactory.getIncrementer(incrementerFallbackType.name(), schemaVersionTarget.getBatchPrefix() + "JOB_SEQ"));

		// BATCH_JOB_EXECUTION differs and the DAO can not be used for BATCH4/5 inserting
		DataFieldMaxValueIncrementer jobExecutionIncrementer = incrementerFactory.getIncrementer(incrementerFallbackType.name(), schemaVersionTarget.getBatchPrefix() + "JOB_EXECUTION_SEQ");
		TaskBatchDao taskBatchDao = this.taskBatchDaoContainer.get(schemaVersion);
		TaskExecution taskExecution = taskExecutionDao.createTaskExecution(jobName, new Date(), new ArrayList<>(), null);
		JobInstance jobInstance = jobInstanceDao.createJobInstance(jobName, jobParameters);
		for (int i = 0; i < jobExecutionCount; i++) {
			JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
			jobExecution.setStatus(batchStatus);
			jobExecution.setId(jobExecutionIncrementer.nextLongValue());
			jobExecution.setStartTime(new Date());
			saveJobExecution(jobExecution, jdbcTemplate, schemaVersionTarget);
			taskBatchDao.saveRelationship(taskExecution, jobExecution);
		}
		return taskExecution;
	}

	private DatabaseType determineIncrementerFallbackType(DataSource dataSource) {
		DatabaseType databaseType;
		try {
			databaseType = DatabaseType.fromMetaData(dataSource);
		} catch (MetaDataAccessException e) {
			throw new IllegalStateException(e);
		}
		if (databaseType == DatabaseType.MARIADB) {
			databaseType = DatabaseType.MYSQL;
		}
		return databaseType;
	}

	private JobExecution saveJobExecution(JobExecution jobExecution, JdbcTemplate jdbcTemplate, SchemaVersionTarget schemaVersionTarget) {
		jobExecution.setStartTime(new Date());
		jobExecution.setVersion(1);
		Timestamp startTime = timestampFromDate(jobExecution.getStartTime());
		Timestamp endTime = timestampFromDate(jobExecution.getEndTime());
		Timestamp createTime = timestampFromDate(jobExecution.getCreateTime());
		Timestamp lastUpdated = timestampFromDate(jobExecution.getLastUpdated());
		Object[] parameters = new Object[] { jobExecution.getId(), jobExecution.getJobId(), startTime, endTime,
				jobExecution.getStatus().toString(), jobExecution.getExitStatus().getExitCode(),
				jobExecution.getExitStatus().getExitDescription(), jobExecution.getVersion(), createTime, lastUpdated };
		String sql = "INSERT INTO %PREFIX%JOB_EXECUTION(JOB_EXECUTION_ID, " +
				"JOB_INSTANCE_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, VERSION, CREATE_TIME, LAST_UPDATED) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		sql = StringUtils.replace(sql, "%PREFIX%", schemaVersionTarget.getBatchPrefix());
		jdbcTemplate.update(sql, parameters,
				new int[] { Types.BIGINT, Types.BIGINT, Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR,
						Types.VARCHAR, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP });
		return jobExecution;
	}

	private Timestamp timestampFromDate(Date date) {
		return (date != null) ? Timestamp.valueOf(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()) : null;
	}


	/**
	 * Test utility that generates hundreds of job executions which can be useful when debugging paging issues.
	 * <p>To run, adjust the datasource properties accordingly and then execute the test manually in your editor.
	 */
	@Disabled
	@Nested class JobExecutionTestDataGenerator {

		@Test
		void generateJobExecutions() {
			// Adjust these properties as necessary to point to your env
			DataSourceProperties dataSourceProperties = new DataSourceProperties();
			dataSourceProperties.setUrl("jdbc:oracle:thin:@localhost:1521/dataflow");
			dataSourceProperties.setUsername("spring");
			dataSourceProperties.setPassword("spring");
			dataSourceProperties.setDriverClassName("oracle.jdbc.OracleDriver");

			DataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
			SchemaService schemaService = new DefaultSchemaService();
			TaskExecutionDaoContainer taskExecutionDaoContainer = new TaskExecutionDaoContainer(dataSource, schemaService);
			TaskBatchDaoContainer taskBatchDaoContainer = new TaskBatchDaoContainer(dataSource, schemaService);
			JobExecutionTestUtils generator = new JobExecutionTestUtils(taskExecutionDaoContainer, taskBatchDaoContainer);
			generator.createSampleJob(jobName("boot2"), 200, BatchStatus.COMPLETED, new JobParameters(),
					schemaService.getTarget("boot2"));
			generator.createSampleJob(jobName("boot3"), 200, BatchStatus.COMPLETED, new JobParameters(),
					schemaService.getTarget("boot3"));
		}

		private String jobName(String schemaTarget) {
			return schemaTarget + "-job-" + System.currentTimeMillis();
		}
	}
}
