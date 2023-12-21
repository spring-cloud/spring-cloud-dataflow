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

import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.dao.JdbcStepExecutionDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.database.support.DatabaseType;
import org.springframework.cloud.dataflow.core.database.support.MultiSchemaIncrementerFactory;
import org.springframework.cloud.dataflow.core.database.support.MultiSchemaTaskExecutionDaoFactoryBean;
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersion;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
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

@SuppressWarnings("ALL")
public abstract class AbstractSimpleJobServiceTests extends AbstractDaoTests {

	private static final String SAVE_JOB_EXECUTION = "INSERT INTO %PREFIX%JOB_EXECUTION(JOB_EXECUTION_ID, JOB_INSTANCE_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, VERSION, CREATE_TIME, LAST_UPDATED) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String SAVE_STEP_EXECUTION_4 = "INSERT into %PREFIX%STEP_EXECUTION(STEP_EXECUTION_ID, STEP_NAME, JOB_EXECUTION_ID, START_TIME, END_TIME, VERSION, STATUS, LAST_UPDATED) values(?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String SAVE_STEP_EXECUTION_5 = "INSERT into %PREFIX%STEP_EXECUTION(STEP_EXECUTION_ID, STEP_NAME, JOB_EXECUTION_ID, START_TIME, END_TIME, VERSION, STATUS, LAST_UPDATED, CREATE_TIME) values(?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String INSERT_TASK_BATCH = "INSERT INTO %sTASK_BATCH (TASK_EXECUTION_ID, JOB_EXECUTION_ID) VALUES (%d, %d)";

	private static final String BASE_JOB_INST_NAME = "JOB_INST_";

	private final Map<AppBootSchemaVersion, JdbcSearchableJobInstanceDao> jdbcSearchableJobInstanceDaoContainer = new HashMap<>();

	private final Map<AppBootSchemaVersion, JdbcStepExecutionDao> stepExecutionDaoContainer = new HashMap<>();

	private DataFieldMaxValueIncrementerFactory incrementerFactory;

	@Autowired
	private JobServiceContainer jobServiceContainer;

	private DatabaseType databaseType;

	private final Map<AppBootSchemaVersion, TaskRepository> taskRepositoryContainer = new HashMap<>();

	@Autowired
	private SchemaService schemaService;

	protected void prepareForTest(JdbcDatabaseContainer<?> dbContainer, String schemaName, DatabaseType databaseType)
			throws Exception {
		this.databaseType = databaseType;
		super.prepareForTest(dbContainer, schemaName);
		for (SchemaVersionTarget schemaVersionTarget : schemaService.getTargets().getSchemas()) {
			JdbcSearchableJobInstanceDao jdbcSearchableJobInstanceDao = new JdbcSearchableJobInstanceDao();
			jdbcSearchableJobInstanceDao.setJdbcTemplate(getJdbcTemplate());
			jdbcSearchableJobInstanceDao.setTablePrefix(schemaVersionTarget.getBatchPrefix());
			incrementerFactory = new MultiSchemaIncrementerFactory(getDataSource());
			jdbcSearchableJobInstanceDao.setJobIncrementer(incrementerFactory.getIncrementer(databaseType.name(),
					schemaVersionTarget.getBatchPrefix() + "JOB_SEQ"));
			this.jdbcSearchableJobInstanceDaoContainer.put(schemaVersionTarget.getSchemaVersion(),
					jdbcSearchableJobInstanceDao);
			JdbcStepExecutionDao stepExecutionDao = new JdbcStepExecutionDao();
			stepExecutionDao.setJdbcTemplate(getJdbcTemplate());
			stepExecutionDao.setTablePrefix(schemaVersionTarget.getBatchPrefix());
			stepExecutionDao.setStepExecutionIncrementer(incrementerFactory.getIncrementer(databaseType.name(),
					schemaVersionTarget.getBatchPrefix() + "STEP_EXECUTION_SEQ"));
			stepExecutionDaoContainer.put(schemaVersionTarget.getSchemaVersion(), stepExecutionDao);
			TaskExecutionDaoFactoryBean teFactory = new MultiSchemaTaskExecutionDaoFactoryBean(getDataSource(),
					schemaVersionTarget.getTaskPrefix());
			TaskRepository taskRepository = new SimpleTaskRepository(teFactory);
			taskRepositoryContainer.put(schemaVersionTarget.getSchemaVersion(), taskRepository);
		}
	}

	@Test
	void retrieveJobExecutionCountBeforeAndAfterJobExecutionBoot2() throws Exception {
		doRetrieveJobExecutionCountBeforeAndAfter(SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT2));
	}

	@Test
	void retrieveJobExecutionCountBeforeAndAfterJobExecutionBoot3() throws Exception {
		doRetrieveJobExecutionCountBeforeAndAfter(SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT3));
	}

	private void doRetrieveJobExecutionCountBeforeAndAfter(SchemaVersionTarget schemaVersionTarget) throws Exception {
		JobService jobService = jobServiceContainer.get(schemaVersionTarget.getName());
		assertThat(jobService.countJobExecutions()).isEqualTo(0);
		createJobExecution(BASE_JOB_INST_NAME, schemaVersionTarget.getSchemaVersion());
		assertThat(jobService.countJobExecutions()).isEqualTo(1);
	}

	@Test
	void retrieveJobExecutionsByTypeAfterJobExeuctionBoot2() throws Exception {
		doRetrieveJobExecutionsByTypeAfter(SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT2));
	}

	@Test
	void retrieveJobExecutionsByTypeAfterJobExeuctionBoot3() throws Exception {
		doRetrieveJobExecutionsByTypeAfter(SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT3));
	}

	private void doRetrieveJobExecutionsByTypeAfter(SchemaVersionTarget schemaVersionTarget) throws Exception {
		String suffix = "_BY_NAME";
		JobService jobService = jobServiceContainer.get(schemaVersionTarget.getName());
		assertThat(jobService.listJobExecutionsForJob(BASE_JOB_INST_NAME + suffix, BatchStatus.COMPLETED, 0, 5).size())
			.isEqualTo(0);
		createJobExecutions(BASE_JOB_INST_NAME + suffix, BatchStatus.COMPLETED, schemaVersionTarget.getSchemaVersion(),
				false, 7);
		createJobExecutions(BASE_JOB_INST_NAME + suffix + "_FAILED", BatchStatus.FAILED,
				schemaVersionTarget.getSchemaVersion(), false, 5);

		assertThat(jobService.listJobExecutionsForJob(BASE_JOB_INST_NAME + suffix, BatchStatus.COMPLETED, 0, 20).size())
			.isEqualTo(7);
		assertThat(
				jobService.listJobExecutionsForJob(BASE_JOB_INST_NAME + suffix + "_FAILED", BatchStatus.FAILED, 0, 20)
					.size())
			.isEqualTo(5);
	}

	@Test
	void retrieveJobExecutionCountWithoutFilterBoot2() throws Exception {
		doRetrieveJobExecutionCountWithoutFilter(SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT2));
	}

	@Test
	void retrieveJobExecutionCountWithoutFilterBoot3() throws Exception {
		doRetrieveJobExecutionCountWithoutFilter(SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT3));
	}

	private void doRetrieveJobExecutionCountWithoutFilter(SchemaVersionTarget schemaVersionTarget) throws Exception {
		String suffix = "_BY_NAME";
		String suffixFailed = suffix + "_FAILED";
		JobService jobService = jobServiceContainer.get(schemaVersionTarget.getName());
		assertThat(jobService.listJobExecutionsForJob(BASE_JOB_INST_NAME + suffix, BatchStatus.COMPLETED, 0, 20).size())
			.isEqualTo(0);
		createJobExecutions(BASE_JOB_INST_NAME + suffix, BatchStatus.COMPLETED, schemaVersionTarget.getSchemaVersion(),
				false, 5);
		createJobExecutions(BASE_JOB_INST_NAME + suffixFailed, BatchStatus.FAILED,
				schemaVersionTarget.getSchemaVersion(), false, 7);

		assertThat(jobService.listJobExecutionsForJob(BASE_JOB_INST_NAME + suffix, null, 0, 20).size()).isEqualTo(5);
		assertThat(jobService.listJobExecutionsForJob(BASE_JOB_INST_NAME + suffixFailed, null, 0, 20).size())
			.isEqualTo(7);
	}

	@Test
	void retrieveJobExecutionCountFilteredByNameBoot2() throws Exception {
		doRetrieveJobExecutionCountFilteredByName(SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT2));
	}

	@Test
	void retrieveJobExecutionCountFilteredByNameBoot3() throws Exception {
		doRetrieveJobExecutionCountFilteredByName(SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT3));
	}

	private void doRetrieveJobExecutionCountFilteredByName(SchemaVersionTarget schemaVersionTarget) throws Exception {
		String suffix = "COUNT_BY_NAME";
		JobService jobService = jobServiceContainer.get(schemaVersionTarget.getName());
		assertThat(jobService.listJobExecutionsForJob(BASE_JOB_INST_NAME + suffix, null, 0, 20).size()).isEqualTo(0);
		createJobExecutions(BASE_JOB_INST_NAME + suffix, BatchStatus.COMPLETED, schemaVersionTarget.getSchemaVersion(),
				false, 5);
		assertThat(jobService.listJobExecutionsForJob(BASE_JOB_INST_NAME + suffix, null, 0, 20).size()).isEqualTo(5);
	}

	@Test
	void retrieveJobExecutionCountFilteredByStatusBoot2() throws Exception {
		SchemaVersionTarget schemaVersionTarget = SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT2);
		doRetrieveJobExecutionCountFilteredByStatus(schemaVersionTarget);
	}

	@Test
	void retrieveJobExecutionCountFilteredByStatusBoot3() throws Exception {
		SchemaVersionTarget schemaVersionTarget = SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT3);
		doRetrieveJobExecutionCountFilteredByStatus(schemaVersionTarget);
	}

	private void doRetrieveJobExecutionCountFilteredByStatus(SchemaVersionTarget schemaVersionTarget) throws Exception {
		String suffix = "_COUNT_BY_NAME";
		JobService jobService = jobServiceContainer.get(schemaVersionTarget.getName());
		assertThat(jobService.countJobExecutionsForJob(null, BatchStatus.COMPLETED)).isEqualTo(0);
		createJobExecutions(BASE_JOB_INST_NAME + suffix, BatchStatus.COMPLETED, schemaVersionTarget.getSchemaVersion(),
				false, 5);
		assertThat(jobService.countJobExecutionsForJob(null, BatchStatus.COMPLETED)).isEqualTo(5);
	}

	@Test
	void retrieveJobExecutionCountFilteredNameAndStatusBoot2() throws Exception {
		SchemaVersionTarget schemaVersionTarget = SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT2);
		doRetrieveJobExecutionCountFilteredNameAndStatus(schemaVersionTarget);
	}

	@Test
	void retrieveJobExecutionCountFilteredNameAndStatusBoot3() throws Exception {
		SchemaVersionTarget schemaVersionTarget = SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT3);
		doRetrieveJobExecutionCountFilteredNameAndStatus(schemaVersionTarget);
	}

	private void doRetrieveJobExecutionCountFilteredNameAndStatus(SchemaVersionTarget schemaVersionTarget)
			throws Exception {
		JobService jobService = jobServiceContainer.get(schemaVersionTarget.getName());
		String suffix = "_COUNT_BY_NAME_STATUS";
		assertThat(jobService.listJobExecutionsForJob(BASE_JOB_INST_NAME + suffix, BatchStatus.COMPLETED, 0, 20).size())
			.isEqualTo(0);
		createJobExecutions(BASE_JOB_INST_NAME + suffix, BatchStatus.COMPLETED, schemaVersionTarget.getSchemaVersion(),
				false, 5);
		createJobExecutions(BASE_JOB_INST_NAME + suffix + "_FAILED", BatchStatus.FAILED,
				schemaVersionTarget.getSchemaVersion(), false, 5);
		assertThat(jobService.listJobExecutionsForJob(BASE_JOB_INST_NAME + suffix, BatchStatus.COMPLETED, 0, 20).size())
			.isEqualTo(5);
	}

	@Test
	void retrieveJobExecutionWithStepCountBoot2() throws Exception {
		SchemaVersionTarget schemaVersionTarget = SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT2);
		doRetrieveJobExecutionWithStepCount(schemaVersionTarget);
	}

	@Test
	void retrieveJobExecutionWithStepCountBoot3() throws Exception {
		SchemaVersionTarget schemaVersionTarget = SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT3);
		doRetrieveJobExecutionWithStepCount(schemaVersionTarget);
	}

	private void doRetrieveJobExecutionWithStepCount(SchemaVersionTarget schemaVersionTarget) throws Exception {
		String suffix = "_JOB_EXECUTIONS_WITH_STEP_COUNT";
		JobService jobService = jobServiceContainer.get(schemaVersionTarget.getName());
		createJobExecutions(BASE_JOB_INST_NAME + suffix, BatchStatus.COMPLETED, schemaVersionTarget.getSchemaVersion(),
				false, 5);
		Collection<JobExecutionWithStepCount> jobExecutionsWithStepCount = jobService.listJobExecutionsWithStepCount(0,
				20);
		assertThat(jobExecutionsWithStepCount.size()).isEqualTo(5);
		JobExecutionWithStepCount jobExecutionWithStepCount = jobExecutionsWithStepCount.stream()
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Expected entry"));
		assertThat(jobExecutionWithStepCount.getStepCount()).isEqualTo(3);
	}

	@Test
	void getJobInstancesThatExist() throws Exception {
		createJobInstance(BASE_JOB_INST_NAME + "BOOT2", AppBootSchemaVersion.BOOT2);
		createJobInstance(BASE_JOB_INST_NAME + "BOOT3", AppBootSchemaVersion.BOOT3);
		verifyJobInstance(1, "boot2", BASE_JOB_INST_NAME + "BOOT2");
		verifyJobInstance(1, "boot3", BASE_JOB_INST_NAME + "BOOT3");
	}

	@Test
	void getJobExecutionsThatExist() throws Exception {
		createJobExecution(BASE_JOB_INST_NAME + "BOOT2", AppBootSchemaVersion.BOOT2);
		verifyJobExecution(1, "boot2", BASE_JOB_INST_NAME + "BOOT2");
		createJobExecution(BASE_JOB_INST_NAME + "BOOT3", AppBootSchemaVersion.BOOT3);
		createJobExecution(BASE_JOB_INST_NAME + "BOOT3A", AppBootSchemaVersion.BOOT3);
		verifyJobExecution(2, "boot3", BASE_JOB_INST_NAME + "BOOT3A");
	}

	@Test
	void exceptionsShouldBeThrownIfRequestForNonExistingJobInstance() {
		assertThatThrownBy(() -> {
			this.jobServiceContainer.get("boot2").getJobInstance(1);
		}).isInstanceOf(NoSuchJobInstanceException.class).hasMessageContaining("JobInstance with id=1 does not exist");
		assertThatThrownBy(() -> {
			this.jobServiceContainer.get("boot3").getJobInstance(1);
		}).isInstanceOf(NoSuchJobInstanceException.class).hasMessageContaining("JobInstance with id=1 does not exist");
	}

	@Test
	void stoppingJobExecutionShouldLeaveJobExecutionWithStatusOfStopping() throws Exception {
		JobExecution jobExecution = createJobExecution(BASE_JOB_INST_NAME + "BOOT3", AppBootSchemaVersion.BOOT3, true);
		jobExecution = this.jobServiceContainer.get("boot3").getJobExecution(jobExecution.getId());
		assertThat(jobExecution.isRunning()).isTrue();
		assertThat(jobExecution.getStatus()).isNotEqualTo(BatchStatus.STOPPING);
		this.jobServiceContainer.get("boot3").stop(jobExecution.getId());
		jobExecution = this.jobServiceContainer.get("boot3").getJobExecution(jobExecution.getId());
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.STOPPING);

		jobExecution = createJobExecution(BASE_JOB_INST_NAME + "BOOT2", AppBootSchemaVersion.BOOT2, true);
		jobExecution = this.jobServiceContainer.get("boot2").getJobExecution(jobExecution.getId());
		assertThat(jobExecution.isRunning()).isTrue();
		assertThat(jobExecution.getStatus()).isNotEqualTo(BatchStatus.STOPPING);
		this.jobServiceContainer.get("boot2").stop(jobExecution.getId());
		jobExecution = this.jobServiceContainer.get("boot2").getJobExecution(jobExecution.getId());
		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.STOPPING);
	}

	private void verifyJobInstance(long id, String schemaTarget, String name) throws Exception {
		JobInstance jobInstance = this.jobServiceContainer.get(schemaTarget).getJobInstance(id);
		assertThat(jobInstance).isNotNull();
		assertThat(jobInstance.getJobName()).isEqualTo(name);
	}

	private void verifyJobExecution(long id, String schemaTarget, String name) throws Exception {
		JobExecution jobExecution = this.jobServiceContainer.get(schemaTarget).getJobExecution(id);
		assertThat(jobExecution).isNotNull();
		assertThat(jobExecution.getId()).isEqualTo(id);
		assertThat(jobExecution.getJobInstance().getJobName()).isEqualTo(name);
	}

	private JobInstance createJobInstance(String name, AppBootSchemaVersion appBootSchemaVersion) throws Exception {
		JdbcSearchableJobInstanceDao jdbcSearchableJobInstanceDao = this.jdbcSearchableJobInstanceDaoContainer
			.get(appBootSchemaVersion);
		assertThat(jdbcSearchableJobInstanceDao).isNotNull();

		return jdbcSearchableJobInstanceDao.createJobInstance(name, new JobParameters());
	}

	private JobExecution createJobExecution(String name, AppBootSchemaVersion appBootSchemaVersion) throws Exception {
		return createJobExecution(name, BatchStatus.STARTING, appBootSchemaVersion, false);
	}

	private JobExecution createJobExecution(String name, AppBootSchemaVersion appBootSchemaVersion, boolean isRunning)
			throws Exception {
		return createJobExecution(name, BatchStatus.STARTING, appBootSchemaVersion, isRunning);
	}

	private JobExecution createJobExecution(String name, BatchStatus batchStatus,
			AppBootSchemaVersion appBootSchemaVersion, boolean isRunning) throws Exception {
		return createJobExecutions(name, batchStatus, appBootSchemaVersion, isRunning, 1).stream()
			.findFirst()
			.orElse(null);
	}

	private List<JobExecution> createJobExecutions(String name, AppBootSchemaVersion appBootSchemaVersion,
			int numberOfJobs) throws Exception {
		return createJobExecutions(name, BatchStatus.STARTING, appBootSchemaVersion, false, numberOfJobs);
	}

	private List<JobExecution> createJobExecutions(String name, BatchStatus batchStatus,
			AppBootSchemaVersion appBootSchemaVersion, boolean isRunning, int numberOfJobs) throws Exception {
		SchemaVersionTarget schemaVersionTarget = schemaService.getTargets()
			.getSchemas()
			.stream()
			.filter(svt -> svt.getSchemaVersion().equals(appBootSchemaVersion))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Cannot find SchemaTarget for " + appBootSchemaVersion));
		String prefix = schemaVersionTarget.getBatchPrefix();
		StepExecutionDao stepExecutionDao = this.stepExecutionDaoContainer.get(appBootSchemaVersion);
		assertThat(stepExecutionDao).isNotNull();
		List<JobExecution> result = new ArrayList<>();
		JobInstance jobInstance = createJobInstance(name, appBootSchemaVersion);
		DataFieldMaxValueIncrementer jobExecutionIncrementer = incrementerFactory.getIncrementer(databaseType.name(),
				prefix + "JOB_EXECUTION_SEQ");
		for (int i = 0; i < numberOfJobs; i++) {
			JobExecution jobExecution = new JobExecution(jobInstance, null, name);
			result.add(jobExecution);
			jobExecution.setId(jobExecutionIncrementer.nextLongValue());
			jobExecution.setStartTime(new Date());
			if (!isRunning) {
				jobExecution.setEndTime(new Date());
			}
			jobExecution.setVersion(3);
			Timestamp startTime = jobExecution.getStartTime() == null ? null : Timestamp
				.valueOf(jobExecution.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
			Timestamp endTime = jobExecution.getEndTime() == null ? null : Timestamp
				.valueOf(jobExecution.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
			Timestamp createTime = jobExecution.getCreateTime() == null ? null : Timestamp
				.valueOf(jobExecution.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
			Timestamp lastUpdated = jobExecution.getLastUpdated() == null ? null : Timestamp
				.valueOf(jobExecution.getLastUpdated().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
			Object[] parameters = new Object[] { jobExecution.getId(), jobExecution.getJobId(), startTime, endTime,
					batchStatus, jobExecution.getExitStatus().getExitCode(),
					jobExecution.getExitStatus().getExitDescription(), jobExecution.getVersion(), createTime,
					lastUpdated };
			int[] argTypes = { Types.BIGINT, Types.BIGINT, Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR,
					Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP };
			getJdbcTemplate().update(getQuery(SAVE_JOB_EXECUTION, schemaVersionTarget), parameters, argTypes);

			StepExecution stepExecution = new StepExecution("StepOne", jobExecution);
			saveStepExecution(schemaVersionTarget, stepExecution);
			stepExecution = new StepExecution("StepTwo", jobExecution);
			saveStepExecution(schemaVersionTarget, stepExecution);
			stepExecution = new StepExecution("StepThree", jobExecution);
			saveStepExecution(schemaVersionTarget, stepExecution);
			createTaskExecution(appBootSchemaVersion, jobExecution);
		}
		return result;
	}

	private void saveStepExecution(SchemaVersionTarget schemaVersionTarget, StepExecution stepExecution) {
		JdbcStepExecutionDao stepExecutionDao = stepExecutionDaoContainer.get(schemaVersionTarget.getSchemaVersion());
		stepExecution.incrementVersion();
		if (stepExecution.getId() == null) {
			DataFieldMaxValueIncrementer stepExecutionIncrementer = incrementerFactory
				.getIncrementer(databaseType.name(), schemaVersionTarget.getBatchPrefix() + "STEP_EXECUTION_SEQ");
			stepExecution.setId(stepExecutionIncrementer.nextLongValue());
		}
		if (stepExecution.getStartTime() == null) {
			stepExecution.setStartTime(new Date());
		}
		boolean isBatch4 = schemaVersionTarget.getSchemaVersion().equals(AppBootSchemaVersion.BOOT2);
		Object[] parameters = isBatch4
				? new Object[] { stepExecution.getId(), stepExecution.getStepName(), stepExecution.getJobExecutionId(),
						stepExecution.getStartTime(), stepExecution.getEndTime(), stepExecution.getVersion(),
						stepExecution.getStatus().toString(), stepExecution.getLastUpdated() }
				: new Object[] { stepExecution.getId(), stepExecution.getStepName(), stepExecution.getJobExecutionId(),
						stepExecution.getStartTime(), stepExecution.getEndTime(), stepExecution.getVersion(),
						stepExecution.getStatus().toString(), stepExecution.getLastUpdated(), new Date() };
		String sql = getQuery(isBatch4 ? SAVE_STEP_EXECUTION_4 : SAVE_STEP_EXECUTION_5, schemaVersionTarget);
		int[] argTypes4 = { Types.BIGINT, Types.VARCHAR, Types.BIGINT, Types.TIMESTAMP, Types.TIMESTAMP, Types.INTEGER,
				Types.VARCHAR, Types.TIMESTAMP };
		int[] argTypes5 = { Types.BIGINT, Types.VARCHAR, Types.BIGINT, Types.TIMESTAMP, Types.TIMESTAMP, Types.INTEGER,
				Types.VARCHAR, Types.TIMESTAMP, Types.TIMESTAMP };
		getJdbcTemplate().update(sql, parameters, isBatch4 ? argTypes4 : argTypes5);
	}

	private TaskExecution createTaskExecution(AppBootSchemaVersion appBootSchemaVersion, JobExecution jobExecution) {
		SchemaVersionTarget schemaVersionTarget = schemaService.getTargets()
			.getSchemas()
			.stream()
			.filter(svt -> svt.getSchemaVersion().equals(appBootSchemaVersion))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Cannot find SchemaTarget for " + appBootSchemaVersion));

		String taskPrefix = schemaVersionTarget.getTaskPrefix();
		TaskRepository taskRepository = taskRepositoryContainer.get(appBootSchemaVersion);

		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(new Date());
		taskExecution = taskRepository.createTaskExecution(taskExecution);
		getJdbcTemplate().execute(
				String.format(INSERT_TASK_BATCH, taskPrefix, taskExecution.getExecutionId(), jobExecution.getJobId()));
		return taskExecution;
	}

	private String getQuery(String inputSql, AppBootSchemaVersion appBootSchemaVersion) {
		SchemaVersionTarget schemaVersionTarget = schemaService.getTargets()
			.getSchemas()
			.stream()
			.filter(svt -> svt.getSchemaVersion().equals(appBootSchemaVersion))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Cannot find SchemaTarget for " + appBootSchemaVersion));
		return getQuery(inputSql, schemaVersionTarget);
	}

	private static String getQuery(String inputSql, SchemaVersionTarget schemaVersionTarget) {
		String tablePrefix = schemaVersionTarget.getBatchPrefix();
		return StringUtils.replace(inputSql, "%PREFIX%", tablePrefix);
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
		public JobRepositoryContainer jobRepositoryContainer(DataSource dataSource,
				PlatformTransactionManager transactionManager, SchemaService schemaService) {
			return new JobRepositoryContainer(dataSource, transactionManager, schemaService);
		}

		@Bean
		public JobExplorerContainer jobExplorerContainer(DataSource dataSource, SchemaService schemaService) {
			return new JobExplorerContainer(dataSource, schemaService);
		}

		@Bean
		public JobServiceContainer jobServiceContainer(DataSource dataSource,
				PlatformTransactionManager platformTransactionManager, SchemaService schemaService,
				JobRepositoryContainer jobRepositoryContainer, JobExplorerContainer jobExplorerContainer,
				Environment environment) {
			return new JobServiceContainer(dataSource, platformTransactionManager, schemaService,
					jobRepositoryContainer, jobExplorerContainer, environment);
		}

	}

}
