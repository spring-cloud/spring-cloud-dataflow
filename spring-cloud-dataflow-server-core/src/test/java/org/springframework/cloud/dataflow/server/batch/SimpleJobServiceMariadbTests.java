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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.cloud.dataflow.core.database.support.DatabaseType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SimpleJobServiceMariadbTests.SimpleJobTestPostgresConfiguration.class)
@Testcontainers
public class SimpleJobServiceMariadbTests extends AbstractSimpleJobServiceTests{

	@Container
	private static final JdbcDatabaseContainer mariaDBContainer = new MariaDBContainer("mariadb:10.9.3");

	@BeforeEach
	void setup() throws Exception {
		super.prepareForTest(mariaDBContainer, "mariadb", determineDatabaseType(DatabaseType.MARIADB));
	}

	@Configuration
	public static class SimpleJobTestPostgresConfiguration extends SimpleJobTestConfiguration {
		@Bean
		public DataSource dataSource() {
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setDriverClassName(mariaDBContainer.getDriverClassName());
			dataSource.setUrl(mariaDBContainer.getJdbcUrl());
			dataSource.setUsername(mariaDBContainer.getUsername());
			dataSource.setPassword(mariaDBContainer.getPassword());
			return dataSource;
		}
	}
}
