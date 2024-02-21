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

package org.springframework.cloud.dataflow.server.repository;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the row number optimization feature of {@link JdbcAggregateJobQueryDao}.
 *
 * @author Chris Bono
 */
@Testcontainers(disabledWithoutDocker = true)
class JdbcAggregateJobQueryDaoRowNumberOptimizationTests {

	@Container
	private static final JdbcDatabaseContainer container = new MariaDBContainer("mariadb:10.9.3");

	private static DataSource dataSource;

	@BeforeAll
	static void startContainer() {
		dataSource = DataSourceBuilder.create()
				.url(container.getJdbcUrl())
				.username(container.getUsername())
				.password(container.getPassword())
				.driverClassName(container.getDriverClassName())
				.build();
	}

	@Test
	void shouldUseOptimizationWhenPropertyNotSpecified() throws Exception {
		MockEnvironment mockEnv = new MockEnvironment();
		JdbcAggregateJobQueryDao dao = new JdbcAggregateJobQueryDao(dataSource, mock(SchemaService.class), mock(JobService.class), mockEnv);
		assertThat(dao).hasFieldOrPropertyWithValue("useRowNumberOptimization", true);
	}

	@Test
	void shouldUseOptimizationWhenPropertyEnabled() throws Exception {
		MockEnvironment mockEnv = new MockEnvironment();
		mockEnv.setProperty("spring.cloud.dataflow.task.jdbc.row-number-optimization.enabled", "true");
		JdbcAggregateJobQueryDao dao = new JdbcAggregateJobQueryDao(dataSource, mock(SchemaService.class), mock(JobService.class), mockEnv);
		assertThat(dao).hasFieldOrPropertyWithValue("useRowNumberOptimization", true);
	}

	@Test
	void shouldNotUseOptimizationWhenPropertyDisabled() throws Exception {
		MockEnvironment mockEnv = new MockEnvironment();
		mockEnv.setProperty("spring.cloud.dataflow.task.jdbc.row-number-optimization.enabled", "false");
		JdbcAggregateJobQueryDao dao = new JdbcAggregateJobQueryDao(dataSource, mock(SchemaService.class), mock(JobService.class), mockEnv);
		assertThat(dao).hasFieldOrPropertyWithValue("useRowNumberOptimization", false);
	}
}
