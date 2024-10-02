/*
 * Copyright 2023-2024 the original author or authors.
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

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
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
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.batch.listener.support.JdbcTaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test utility related to job execution test data setup.
 */
class JobExecutionTestUtils
{
	private final TaskExecutionDao taskExecutionDao;

	private final TaskBatchDao taskBatchDao;

	JobExecutionTestUtils(
			TaskExecutionDao taskExecutionDao,
			TaskBatchDao taskBatchDao
		) {
		this.taskExecutionDao = taskExecutionDao;
		this.taskBatchDao = taskBatchDao;
	}

	TaskExecution createSampleJob(String jobName, int jobExecutionCount, BatchStatus batchStatus, JobParameters jobParameters) {

		DataSource dataSource = (DataSource) ReflectionTestUtils.getField(taskExecutionDao, JdbcTaskExecutionDao.class, "dataSource");
		NamedParameterJdbcTemplate namedParamJdbcTemplate = (NamedParameterJdbcTemplate) ReflectionTestUtils.getField(taskExecutionDao, JdbcTaskExecutionDao.class, "jdbcTemplate");
		JdbcTemplate jdbcTemplate = namedParamJdbcTemplate.getJdbcTemplate();
		DataFieldMaxValueIncrementerFactory incrementerFactory = new MultiSchemaIncrementerFactory(dataSource);
		DatabaseType incrementerFallbackType = determineIncrementerFallbackType(dataSource);

		JdbcJobInstanceDao jobInstanceDao = new JdbcJobInstanceDao();
		jobInstanceDao.setJdbcTemplate(jdbcTemplate);
		jobInstanceDao.setJobIncrementer(incrementerFactory.getIncrementer(incrementerFallbackType.name(), "BATCH_JOB_SEQ"));

		// BATCH_JOB_EXECUTION differs and the DAO can not be used for BATCH4/5 inserting
		DataFieldMaxValueIncrementer jobExecutionIncrementer = incrementerFactory.getIncrementer(incrementerFallbackType.name(), "BATCH_JOB_EXECUTION_SEQ");
		TaskExecution taskExecution = taskExecutionDao.createTaskExecution(jobName, LocalDateTime.now(), new ArrayList<>(), null);
		JobInstance jobInstance = jobInstanceDao.createJobInstance(jobName, jobParameters);
		for (int i = 0; i < jobExecutionCount; i++) {
			JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
			jobExecution.setStatus(batchStatus);
			jobExecution.setId(jobExecutionIncrementer.nextLongValue());
			jobExecution.setStartTime(LocalDateTime.now());
			saveJobExecution(jobExecution, jdbcTemplate);
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

	private JobExecution saveJobExecution(JobExecution jobExecution, JdbcTemplate jdbcTemplate) {
		jobExecution.setStartTime(LocalDateTime.now());
		jobExecution.setVersion(1);
		Timestamp startTime = timestampFromDate(jobExecution.getStartTime());
		Timestamp endTime = timestampFromDate(jobExecution.getEndTime());
		Timestamp createTime = timestampFromDate(jobExecution.getCreateTime());
		Timestamp lastUpdated = timestampFromDate(jobExecution.getLastUpdated());
		Object[] parameters = new Object[] { jobExecution.getId(), jobExecution.getJobId(), startTime, endTime,
				jobExecution.getStatus().toString(), jobExecution.getExitStatus().getExitCode(),
				jobExecution.getExitStatus().getExitDescription(), jobExecution.getVersion(), createTime, lastUpdated };
		String sql = "INSERT INTO BATCH_JOB_EXECUTION(JOB_EXECUTION_ID, " +
				"JOB_INSTANCE_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, VERSION, CREATE_TIME, LAST_UPDATED) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		jdbcTemplate.update(sql, parameters,
				new int[] { Types.BIGINT, Types.BIGINT, Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR,
						Types.VARCHAR, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP });
		return jobExecution;
	}

	private Timestamp timestampFromDate(LocalDateTime date) {
		return (date != null) ? Timestamp.valueOf(date) : null;
	}


	/**
	 * Test utility that generates hundreds of job executions which can be useful when debugging paging issues.
	 * <p>To run, adjust the datasource properties accordingly and then execute the test manually in your editor.
	 */
	// @Disabled
	@Nested class JobExecutionTestDataGenerator {

		@Test
		void generateJobExecutions() throws SQLException {
			// Adjust these properties as necessary to point to your env
			DataSourceProperties dataSourceProperties = new DataSourceProperties();
			dataSourceProperties.setUrl("jdbc:oracle:thin:@localhost:1521/dataflow");
			dataSourceProperties.setUsername("spring");
			dataSourceProperties.setPassword("spring");
			dataSourceProperties.setDriverClassName("oracle.jdbc.OracleDriver");

			DataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
			DataFieldMaxValueIncrementerFactory incrementerFactory = new MultiSchemaIncrementerFactory(dataSource);
			JdbcTaskExecutionDao taskExecutionDao = new JdbcTaskExecutionDao(dataSource);
			String databaseType;
			try {
				databaseType = org.springframework.cloud.task.repository.support.DatabaseType.fromMetaData(dataSource).name();
			}
			catch (MetaDataAccessException e) {
				throw new IllegalStateException(e);
			}
			taskExecutionDao.setTaskIncrementer(incrementerFactory.getIncrementer(databaseType, "TASK_SEQ"));
			JdbcTaskBatchDao taskBatchDao = new JdbcTaskBatchDao(dataSource);
			JobExecutionTestUtils generator = new JobExecutionTestUtils(taskExecutionDao, taskBatchDao);
			generator.createSampleJob(jobName("boot2"), 200, BatchStatus.COMPLETED, new JobParameters());
			generator.createSampleJob(jobName("boot3"), 200, BatchStatus.COMPLETED, new JobParameters());
		}

		private String jobName(String schemaTarget) {
			return schemaTarget + "-job-" + System.currentTimeMillis();
		}
	}
}
