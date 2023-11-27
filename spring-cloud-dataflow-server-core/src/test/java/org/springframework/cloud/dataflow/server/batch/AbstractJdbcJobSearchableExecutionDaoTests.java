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

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.JdbcStepExecutionDao;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.cloud.dataflow.core.database.support.DatabaseType;
import org.springframework.cloud.dataflow.core.database.support.MultiSchemaIncrementerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public abstract class AbstractJdbcJobSearchableExecutionDaoTests extends AbstractDaoTests {

	private static final String BASE_JOB_INSTANCE_NAME = "JOB_INST_";

	JdbcSearchableJobExecutionDao jdbcSearchableJobExecutionDao;

	JdbcSearchableJobInstanceDao jdbcSearchableJobInstanceDao;

	DataFieldMaxValueIncrementerFactory incrementerFactory;

	JdbcJobExecutionDao jobExecutionDao;

	JdbcStepExecutionDao stepExecutionDao;

	void setupSearchableExecutionDaoTest(JdbcDatabaseContainer dbContainer, String schemaName,
												DatabaseType databaseType) throws Exception {
		prepareForTest(dbContainer, schemaName);

		this.jdbcSearchableJobExecutionDao = new JdbcSearchableJobExecutionDao();
		this.jdbcSearchableJobExecutionDao.setDataSource(this.dataSource);
		this.jdbcSearchableJobExecutionDao.afterPropertiesSet();
		this.jdbcSearchableJobInstanceDao = new JdbcSearchableJobInstanceDao();
		this.jdbcSearchableJobInstanceDao.setJdbcTemplate(this.jdbcTemplate);
		incrementerFactory = new MultiSchemaIncrementerFactory(dataSource);

		this.jdbcSearchableJobInstanceDao.setJobIncrementer(incrementerFactory.getIncrementer(databaseType.name(),
			AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX + "JOB_SEQ"));
		jobExecutionDao = new JdbcJobExecutionDao();
		jobExecutionDao.setJobExecutionIncrementer(incrementerFactory.getIncrementer(databaseType.name(),
			AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX + "JOB_EXECUTION_SEQ"));
		this.jobExecutionDao.setJdbcTemplate(new JdbcTemplate(this.dataSource));
		jobExecutionDao.afterPropertiesSet();
		this.stepExecutionDao = new JdbcStepExecutionDao();
		this.stepExecutionDao.setJdbcTemplate(this.jdbcTemplate);
		this.stepExecutionDao.setStepExecutionIncrementer(incrementerFactory.getIncrementer(databaseType.name(),
			AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX + "STEP_EXECUTION_SEQ"));
		this.stepExecutionDao.afterPropertiesSet();
	}

	@Test
	void retrieveJobExecutionCountBeforeAndAfterJobExecution() {
		assertThat(this.jdbcSearchableJobExecutionDao.countJobExecutions()).isEqualTo(0);
		JobInstance jobInstance = jdbcSearchableJobInstanceDao.createJobInstance(BASE_JOB_INSTANCE_NAME,
			new JobParameters());
		jobExecutionDao.saveJobExecution(new JobExecution(jobInstance, new JobParameters()));
		assertThat(this.jdbcSearchableJobExecutionDao.countJobExecutions()).isEqualTo(1);
	}

	@Test
	void retrieveJobExecutionsByTypeAfterJobExeuction() {
		String suffix = "_BY_NAME";
		assertThat(this.jdbcSearchableJobExecutionDao.getJobExecutions(BASE_JOB_INSTANCE_NAME + suffix,
			BatchStatus.COMPLETED, 0, 5).size()).isEqualTo(0);
		createJobExecutions(BASE_JOB_INSTANCE_NAME + suffix, 7);
		createJobExecutions(BASE_JOB_INSTANCE_NAME + suffix + "_FAILED", BatchStatus.FAILED, 5);

		assertThat(this.jdbcSearchableJobExecutionDao.getJobExecutions(BASE_JOB_INSTANCE_NAME + suffix,
			BatchStatus.COMPLETED, 0, 7).size()).isEqualTo(7);
		assertThat(this.jdbcSearchableJobExecutionDao.getJobExecutions(BASE_JOB_INSTANCE_NAME + suffix,
			BatchStatus.COMPLETED, 0, 5).size()).isEqualTo(5);
	}

	@Test
	void retrieveJobExecutionCountWithoutFilter() {
		String suffix = "_BY_NAME";
		String suffixFailed = suffix + "_FAILED";
		assertThat(this.jdbcSearchableJobExecutionDao.getJobExecutions(BASE_JOB_INSTANCE_NAME + suffix,
			BatchStatus.COMPLETED, 0, 5).size()).isEqualTo(0);
		createJobExecutions(BASE_JOB_INSTANCE_NAME + suffix, 5);
		createJobExecutions(BASE_JOB_INSTANCE_NAME + suffixFailed, BatchStatus.FAILED, 7);

		assertThat(this.jdbcSearchableJobExecutionDao.getJobExecutions(BASE_JOB_INSTANCE_NAME + suffix,
			0, 20).size()).isEqualTo(5);
		assertThat(this.jdbcSearchableJobExecutionDao.getJobExecutions(BASE_JOB_INSTANCE_NAME + suffixFailed,
			0, 20).size()).isEqualTo(7);

	}

	@Test
	void retrieveJobExecutionCountFilteredByName() {
		String suffix = "COUNT_BY_NAME";
		assertThat(this.jdbcSearchableJobExecutionDao.countJobExecutions(BASE_JOB_INSTANCE_NAME + suffix))
			.isEqualTo(0);
		createJobExecutions(BASE_JOB_INSTANCE_NAME + suffix, 5);
		assertThat(this.jdbcSearchableJobExecutionDao.countJobExecutions(BASE_JOB_INSTANCE_NAME + suffix))
			.isEqualTo(5);
	}

	@Test
	void retrieveJobExecutionCountFilteredByStatus() {
		String suffix = "_COUNT_BY_NAME";
		assertThat(this.jdbcSearchableJobExecutionDao.countJobExecutions(BatchStatus.COMPLETED)).isEqualTo(0);
		createJobExecutions(BASE_JOB_INSTANCE_NAME + suffix, 5);
		assertThat(this.jdbcSearchableJobExecutionDao.countJobExecutions(BatchStatus.COMPLETED)).isEqualTo(5);
	}

	@Test
	void retrieveJobExecutionCountFilteredNameAndStatus() {
		String suffix = "_COUNT_BY_NAME_STATUS";
		assertThat(this.jdbcSearchableJobExecutionDao.countJobExecutions(BASE_JOB_INSTANCE_NAME + suffix,
			BatchStatus.COMPLETED)).isEqualTo(0);
		createJobExecutions(BASE_JOB_INSTANCE_NAME + suffix, 5);
		assertThat(this.jdbcSearchableJobExecutionDao.countJobExecutions(BASE_JOB_INSTANCE_NAME + suffix,
			BatchStatus.COMPLETED)).isEqualTo(5);
	}

	@Test
	void retrieveJobExecutionWithStepCount() {
		String suffix = "_JOB_EXECUTIONS_WITH_STEP_COUNT";

		createJobExecutions(BASE_JOB_INSTANCE_NAME + suffix, 5);

		List<JobExecutionWithStepCount> jobExecutionsWithStepCount =
			this.jdbcSearchableJobExecutionDao.getJobExecutionsWithStepCount(0, 20);
		assertThat(jobExecutionsWithStepCount.size()).isEqualTo(5);
		assertThat(jobExecutionsWithStepCount.get(0).getStepCount()).isEqualTo(3);
	}

	@Test
	void retrieveJobExecutionWithStepCountFilteredJobInstance() {
		String suffix = "_JOB_EXECUTIONS_WITH_STEP_COUNT_BY_JOB_INSTANCE";

		createJobExecutions(BASE_JOB_INSTANCE_NAME + suffix, 5);
		JobInstance jobInstance = this.jdbcSearchableJobInstanceDao.getJobInstances(
			BASE_JOB_INSTANCE_NAME + suffix, 0, 5).get(0);

		List<JobExecutionWithStepCount> jobExecutionsWithStepCount =
			this.jdbcSearchableJobExecutionDao.getJobExecutionsWithStepCountFilteredByJobInstanceId((int) jobInstance
				.getInstanceId(), 0, 10);
		assertThat(jobExecutionsWithStepCount.size()).isEqualTo(5);
		assertThat(jobExecutionsWithStepCount.get(0).getStepCount()).isEqualTo(3);
	}

	private List<JobExecution> createJobExecutions(String name, int numberOfJobs) {
		return createJobExecutions(name, BatchStatus.COMPLETED, numberOfJobs);
	}

	private List<JobExecution> createJobExecutions(String name, BatchStatus batchStatus, int numberOfJobs) {
		List<JobExecution> jobExecutions = new ArrayList<>();
		JobInstance jobInstance = jdbcSearchableJobInstanceDao.createJobInstance(name, new JobParameters());

		for (int i = 0; i < numberOfJobs; i++) {
			JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());
			jobExecution.setStatus(batchStatus);
			jobExecution.createStepExecution("StepOne");
			jobExecution.createStepExecution("StepTwo");
			jobExecution.createStepExecution("StepThree");
			jobExecutionDao.saveJobExecution(jobExecution);
			StepExecution stepExecution = new StepExecution("StepOne", jobExecution);
			this.stepExecutionDao.saveStepExecution(stepExecution);
			stepExecution = new StepExecution("StepTwo", jobExecution);
			this.stepExecutionDao.saveStepExecution(stepExecution);
			stepExecution = new StepExecution("StepThree", jobExecution);
			this.stepExecutionDao.saveStepExecution(stepExecution);
			jobExecutions.add(jobExecution);
		}
		return jobExecutions;
	}
}
