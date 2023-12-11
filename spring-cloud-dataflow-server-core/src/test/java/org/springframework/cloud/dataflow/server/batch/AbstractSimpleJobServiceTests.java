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

package org.springframework.cloud.dataflow.server.batch;

import javax.sql.DataSource;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.database.support.DatabaseType;
import org.springframework.cloud.dataflow.core.database.support.MultiSchemaIncrementerFactory;
import org.springframework.cloud.dataflow.core.database.support.MultiSchemaTaskExecutionDaoFactoryBean;
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersion;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.schema.service.impl.DefaultSchemaService;
import org.springframework.cloud.dataflow.server.repository.JobRepositoryContainer;
import org.springframework.cloud.dataflow.server.service.JobExplorerContainer;
import org.springframework.cloud.dataflow.server.service.JobServiceContainer;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;


public abstract class AbstractSimpleJobServiceTests extends AbstractDaoTests {

	private static final String SAVE_JOB_EXECUTION = "INSERT INTO %PREFIX%JOB_EXECUTION(JOB_EXECUTION_ID, " +
		"JOB_INSTANCE_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, VERSION, CREATE_TIME, LAST_UPDATED) " +
		"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String BASE_JOB_INST_NAME = "JOB_INST_";


	private JdbcSearchableJobExecutionDao jdbcSearchableJobExecutionDao;

	private JdbcSearchableJobInstanceDao jdbcSearchableJobInstanceDao;

	private DataFieldMaxValueIncrementerFactory incrementerFactory;

	@Autowired
	private JobServiceContainer jobServiceContainer;

	private DatabaseType databaseType;

	private TaskRepository taskRepositoryBoot2;

	private TaskRepository taskRepositoryBoot3;

	protected void prepareForTest(JdbcDatabaseContainer dbContainer, String schemaName, DatabaseType databaseType) throws Exception {
		this.databaseType = databaseType;
		super.prepareForTest(dbContainer, schemaName);
		this.jdbcSearchableJobExecutionDao = new JdbcSearchableJobExecutionDao();
		this.jdbcSearchableJobExecutionDao.setDataSource(getDataSource());
		this.jdbcSearchableJobExecutionDao.afterPropertiesSet();
		this.jdbcSearchableJobInstanceDao = new JdbcSearchableJobInstanceDao();
		this.jdbcSearchableJobInstanceDao.setJdbcTemplate(getJdbcTemplate());
		incrementerFactory = new MultiSchemaIncrementerFactory(getDataSource());

		this.jdbcSearchableJobInstanceDao.setJobIncrementer(incrementerFactory.getIncrementer(databaseType.name(),
			AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX + "JOB_SEQ"));
		TaskExecutionDaoFactoryBean teFactory = new MultiSchemaTaskExecutionDaoFactoryBean(getDataSource(), "TASK_") ;
		taskRepositoryBoot2 =  new SimpleTaskRepository(teFactory);
		teFactory = new MultiSchemaTaskExecutionDaoFactoryBean(getDataSource(), "BOOT3_TASK_") ;
		taskRepositoryBoot3 =  new SimpleTaskRepository(teFactory);
	}

	@Test
	void getJobInstancesThatExist() throws Exception {
		createJobInstance("BOOT2", AppBootSchemaVersion.BOOT2);
		createJobInstance("BOOT3", AppBootSchemaVersion.BOOT3);
		verifyJobInstance(1, "boot2", "BOOT2");
		verifyJobInstance(1, "boot3", "BOOT3");
	}

	@Test
	void getJobExecutionsThatExist() throws Exception {
		createJobExecution("BOOT2", AppBootSchemaVersion.BOOT2);
		verifyJobExecution(1, "boot2", "BOOT2");
		createJobExecution("BOOT3", AppBootSchemaVersion.BOOT3);
		createJobExecution("BOOT3A", AppBootSchemaVersion.BOOT3);
		verifyJobExecution(2, "boot3", "BOOT3A");
	}

	@Test
	void exceptionsShouldBeThrownIfRequestForNonExistingJobInstance() {
		assertThatThrownBy(() -> {
			this.jobServiceContainer.get("boot2").getJobInstance(1);
		}).isInstanceOf(NoSuchJobInstanceException.class)
			.hasMessageContaining("JobInstance with id=1 does not exist");
		assertThatThrownBy(() -> {
			this.jobServiceContainer.get("boot3").getJobInstance(1);
		}).isInstanceOf(NoSuchJobInstanceException.class)
			.hasMessageContaining("JobInstance with id=1 does not exist");
	}

	@Test
	void stoppingJobExecutionShouldLeaveJobExecutionwithStatusOfStopping() throws Exception{
		JobExecution jobExecution = createJobExecution("BOOT3", AppBootSchemaVersion.BOOT3, true);
		jobExecution = this.jobServiceContainer.get("boot3").getJobExecution(jobExecution.getId());
		assertThat(jobExecution.isRunning()).isTrue();
		assertThat(jobExecution.getStatus()).isNotEqualTo(BatchStatus.STOPPING);
		this.jobServiceContainer.get("boot3").stop(jobExecution.getId());
		jobExecution = this.jobServiceContainer.get("boot3").getJobExecution(jobExecution.getId());
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.STOPPING);

		jobExecution = createJobExecution("BOOT2", AppBootSchemaVersion.BOOT2, true);
		jobExecution = this.jobServiceContainer.get("boot2").getJobExecution(jobExecution.getId());
		assertThat(jobExecution.isRunning()).isTrue();
		assertThat(jobExecution.getStatus()).isNotEqualTo(BatchStatus.STOPPING);
		this.jobServiceContainer.get("boot2").stop(jobExecution.getId());
		jobExecution = this.jobServiceContainer.get("boot2").getJobExecution(jobExecution.getId());
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.STOPPING);
	}

	private void verifyJobInstance(long id, String schemaTarget, String suffix) throws Exception {
		JobInstance jobInstance = this.jobServiceContainer.get(schemaTarget).getJobInstance(id);
		assertThat(jobInstance).isNotNull();
		assertThat(jobInstance.getJobName()).isEqualTo(BASE_JOB_INST_NAME + suffix);
	}

	private void verifyJobExecution(long id, String schemaTarget, String suffix) throws Exception {
		JobExecution jobExecution = this.jobServiceContainer.get(schemaTarget).getJobExecution(id);
		assertThat(jobExecution).isNotNull();
		assertThat(jobExecution.getId()).isEqualTo(id);
		assertThat(jobExecution.getJobInstance().getJobName()).isEqualTo(BASE_JOB_INST_NAME + suffix);
	}

	private JobInstance createJobInstance(String suffix, AppBootSchemaVersion appBootSchemaVersion) throws Exception {
		String prefix = AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;

		if (appBootSchemaVersion.equals(AppBootSchemaVersion.BOOT3)) {
			prefix = "BOOT3_BATCH_";
		}
		this.jdbcSearchableJobInstanceDao.setJobIncrementer(incrementerFactory.getIncrementer(this.databaseType.name(),
			prefix + "JOB_SEQ"));
		this.jdbcSearchableJobInstanceDao.setTablePrefix(prefix);
		return jdbcSearchableJobInstanceDao.createJobInstance(BASE_JOB_INST_NAME + suffix,
			new JobParameters());
	}

	private JobExecution createJobExecution(String suffix, AppBootSchemaVersion appBootSchemaVersion) throws Exception{
		return createJobExecution(suffix, appBootSchemaVersion, false);
	}
	private JobExecution createJobExecution(String suffix, AppBootSchemaVersion appBootSchemaVersion, boolean isRunning)
		throws Exception {
		String prefix = AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;

		if (appBootSchemaVersion.equals(AppBootSchemaVersion.BOOT3)) {
			prefix = "BOOT3_BATCH_";
		}
		JobInstance jobInstance = createJobInstance(suffix, appBootSchemaVersion);
		JobExecution jobExecution = new JobExecution(jobInstance, null, "foo");
		DataFieldMaxValueIncrementer jobExecutionIncrementer = incrementerFactory.getIncrementer(databaseType.name(),
			prefix+ "JOB_EXECUTION_SEQ");
		jobExecution.setId(jobExecutionIncrementer.nextLongValue());
		jobExecution.setStartTime(new Date());
		if(!isRunning) {
			jobExecution.setEndTime(new Date());
		}
		jobExecution.setVersion(3);
		Timestamp startTime = jobExecution.getStartTime() == null ? null
			: Timestamp.valueOf(jobExecution.getStartTime().toInstant()
			.atZone(ZoneId.systemDefault())
			.toLocalDateTime());
		Timestamp endTime = jobExecution.getEndTime() == null ? null : Timestamp.valueOf(jobExecution.getEndTime().toInstant()
			.atZone(ZoneId.systemDefault())
			.toLocalDateTime());
		Timestamp createTime = jobExecution.getCreateTime() == null ? null
			: Timestamp.valueOf(jobExecution.getCreateTime().toInstant()
			.atZone(ZoneId.systemDefault())
			.toLocalDateTime());
		Timestamp lastUpdated = jobExecution.getLastUpdated() == null ? null
			: Timestamp.valueOf(jobExecution.getLastUpdated().toInstant()
			.atZone(ZoneId.systemDefault())
			.toLocalDateTime());
		Object[] parameters = new Object[] { jobExecution.getId(), jobExecution.getJobId(), startTime, endTime,
			jobExecution.getStatus().toString(), jobExecution.getExitStatus().getExitCode(),
			jobExecution.getExitStatus().getExitDescription(), jobExecution.getVersion(), createTime, lastUpdated };
		getJdbcTemplate().update(getQuery(SAVE_JOB_EXECUTION, appBootSchemaVersion), parameters,
			new int[] { Types.BIGINT, Types.BIGINT, Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR,
				Types.VARCHAR, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP });
		createTaskExecution(appBootSchemaVersion, jobExecution);
		return jobExecution;
	}

	private TaskExecution createTaskExecution(AppBootSchemaVersion appBootSchemaVersion, JobExecution jobExecution) {
		TaskRepository taskRepository = this.taskRepositoryBoot2;
		String taskPrefix = "TASK_";
		if (appBootSchemaVersion.equals(AppBootSchemaVersion.BOOT3)) {
			taskPrefix = "BOOT3_TASK_";
			taskRepository = this.taskRepositoryBoot3;
		}
		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(new Date());
		taskExecution = taskRepository.createTaskExecution(taskExecution);
		getJdbcTemplate().execute("INSERT INTO " + taskPrefix + "TASK_BATCH (TASK_EXECUTION_ID, JOB_EXECUTION_ID) VALUES (" +
			taskExecution.getExecutionId() + ", " + jobExecution.getJobId() + ")");
		return taskExecution;
	}

	private String getQuery(String base, AppBootSchemaVersion appBootSchemaVersion) {
		String tablePrefix = "BATCH_";
		if(appBootSchemaVersion.equals(AppBootSchemaVersion.BOOT3)) {
			tablePrefix = "BOOT3_BATCH_";
		}
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}

	protected static class SimpleJobTestConfiguration {

		@Bean
		public JdbcTemplate jdbcTemplate(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		@Bean
		public PlatformTransactionManager platformTransactionManager() {
			return new ResourcelessTransactionManager();
		}

		@Bean
		public SchemaService schemaService() {
			return new DefaultSchemaService();
		}

		@Bean
		public JobRepositoryContainer jobRepositoryContainer(DataSource dataSource, PlatformTransactionManager transactionManager,
										 SchemaService schemaService) {
			return new JobRepositoryContainer(dataSource, transactionManager, schemaService);
		}

		@Bean
		public JobExplorerContainer jobExplorerContainer(DataSource dataSource, SchemaService schemaService) {
			return new JobExplorerContainer(dataSource, schemaService);
		}

		@Bean
		public JobServiceContainer jobServiceContainer(DataSource dataSource,
				PlatformTransactionManager platformTransactionManager,
				SchemaService schemaService,
				JobRepositoryContainer jobRepositoryContainer,
				JobExplorerContainer jobExplorerContainer,
				Environment environment) {
			return new JobServiceContainer(dataSource, platformTransactionManager, schemaService, jobRepositoryContainer,
					jobExplorerContainer, environment);
		}
	}
}
