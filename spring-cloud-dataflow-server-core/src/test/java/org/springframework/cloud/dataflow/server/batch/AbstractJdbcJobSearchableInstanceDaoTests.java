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
import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.batch.core.JobParameters;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractJdbcJobSearchableInstanceDaoTests extends AbstractDaoTests {

	private static final String BASE_JOB_INST_NAME = "JOB_INST_";

	protected JdbcSearchableJobInstanceDao jdbcSearchableJobInstanceDao;

	@Override
	protected void prepareForTest(JdbcDatabaseContainer dbContainer,  String schemaName) throws Exception {
		super.prepareForTest(dbContainer, schemaName);
		jdbcSearchableJobInstanceDao = new JdbcSearchableJobInstanceDao();
		jdbcSearchableJobInstanceDao.setJdbcTemplate(getJdbcTemplate());
		jdbcSearchableJobInstanceDao.afterPropertiesSet();
	}

	@Test
	void retrieveJobInstanceCountWithoutFilter() {
		assertThat(jdbcSearchableJobInstanceDao.countJobInstances(BASE_JOB_INST_NAME)).isEqualTo(0);
		jdbcSearchableJobInstanceDao.createJobInstance(BASE_JOB_INST_NAME, new JobParameters());
		assertThat(jdbcSearchableJobInstanceDao.countJobInstances(BASE_JOB_INST_NAME)).isEqualTo(1);
	}
}
