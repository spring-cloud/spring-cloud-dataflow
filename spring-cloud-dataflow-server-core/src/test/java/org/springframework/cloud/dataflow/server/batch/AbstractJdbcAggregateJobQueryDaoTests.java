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
import org.mockito.Mock;
import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.cloud.dataflow.core.database.support.DatabaseType;
import org.springframework.cloud.dataflow.core.database.support.MultiSchemaIncrementerFactory;
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersion;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.impl.DefaultSchemaService;
import org.springframework.cloud.dataflow.server.repository.JdbcAggregateJobQueryDao;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

abstract class AbstractJdbcAggregateJobQueryDaoTests extends AbstractDaoTests {

	private static final String BASE_JOB_INST_NAME = "JOB_INST_";

	public JdbcSearchableJobInstanceDao jdbcSearchableJobInstanceDao;

	@Mock
	private JobService jobService;

	private JdbcAggregateJobQueryDao jdbcAggregateJobQueryDao;

	private DataFieldMaxValueIncrementerFactory incrementerFactory;

	private DatabaseType databaseType;

	protected void prepareForTest(JdbcDatabaseContainer dbContainer,  String schemaName, DatabaseType databaseType) throws Exception {
		super.prepareForTest(dbContainer, schemaName);
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.cloud.dataflow.task.jdbc.row-number-optimization.enabled", "true");
		this.jdbcAggregateJobQueryDao = new JdbcAggregateJobQueryDao(super.getDataSource(), new DefaultSchemaService(),
			this.jobService, environment);
		jdbcSearchableJobInstanceDao = new JdbcSearchableJobInstanceDao();
		jdbcSearchableJobInstanceDao.setJdbcTemplate(super.getJdbcTemplate());
		incrementerFactory = new MultiSchemaIncrementerFactory(super.getDataSource());
		this.databaseType = databaseType;
	}

	@Test
	void getJobInstancesForBoot3AndBoot2Instances() throws Exception {
		assertThatThrownBy( () -> this.jdbcAggregateJobQueryDao.getJobInstance(1, "boot2"))
			.isInstanceOf(NoSuchJobInstanceException.class)
			.hasMessageContaining("JobInstance with id=1 does not exist");
		assertThatThrownBy( () -> this.jdbcAggregateJobQueryDao.getJobInstance(1, "boot3"))
			.isInstanceOf(NoSuchJobInstanceException.class)
			.hasMessageContaining("JobInstance with id=1 does not exist");
		createJobInstance("BOOT2", SchemaVersionTarget.defaultTarget());
		createJobInstance("BOOT3", SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT3));
		verifyJobInstance(1, "boot2", "BOOT2");
		verifyJobInstance(1, "boot3", "BOOT3");
	}

	private void verifyJobInstance(long id, String schemaTarget, String suffix) throws Exception{
		JobInstance  jobInstance = this.jdbcAggregateJobQueryDao.getJobInstance(id, schemaTarget);
		assertThat(jobInstance).isNotNull();
		assertThat(jobInstance.getJobName()).isEqualTo(BASE_JOB_INST_NAME + suffix );
	}

	private JobInstance createJobInstance(String suffix, SchemaVersionTarget schemaVersionTarget) {
		this.jdbcSearchableJobInstanceDao.setJobIncrementer(incrementerFactory.getIncrementer(this.databaseType.name(),
			schemaVersionTarget.getBatchPrefix()+ "JOB_SEQ"));
		this.jdbcSearchableJobInstanceDao.setTablePrefix(schemaVersionTarget.getBatchPrefix());
		return  jdbcSearchableJobInstanceDao.createJobInstance(BASE_JOB_INST_NAME + suffix,
			new JobParameters());
	}
}
