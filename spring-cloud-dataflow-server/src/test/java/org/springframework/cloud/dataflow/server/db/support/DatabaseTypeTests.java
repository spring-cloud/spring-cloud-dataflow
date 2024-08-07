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

package org.springframework.cloud.dataflow.server.db.support;

import javax.sql.DataSource;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.cloud.dataflow.core.database.support.DatabaseType;
import org.springframework.cloud.dataflow.server.db.DB2_11_5_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.MariaDB_10_6_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.MariaDB_11_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.MySQL_5_7_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.MySQL_8_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.Oracle_XE_18_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.SqlServer_2017_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.SqlServer_2019_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.SqlServer_2022_ContainerSupport;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseTypeTests {

	@JdbcTest(properties = "spring.jpa.hibernate.ddl-auto=none")
	@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
	@Testcontainers
	@ContextConfiguration(classes = FakeApp.class)
	static abstract class AbstractSingleDbDatabaseTypeTests {

		@Test
		void shouldSupportRowNumberFunction(@Autowired DataSource dataSource) throws MetaDataAccessException {
			assertThat(DatabaseType.supportsRowNumberFunction(dataSource)).isEqualTo(supportsRowNumberFunction());
		}

		protected boolean supportsRowNumberFunction() {
			return true;
		}

	}
	@SpringBootConfiguration
	static class FakeApp {
	}

	@Nested
	class MariaDB_10_6_DatabaseTypeSingleDbDatabaseTypeTests extends AbstractSingleDbDatabaseTypeTests implements MariaDB_10_6_ContainerSupport {
	}

	@Nested
	class MariaDB_11_DatabaseTypeSingleDbDatabaseTypeTests extends AbstractSingleDbDatabaseTypeTests implements MariaDB_11_ContainerSupport {
	}

	@Nested
	class MySql_5_7_DatabaseTypeSingleDbDatabaseTypeTests extends AbstractSingleDbDatabaseTypeTests implements MySQL_5_7_ContainerSupport {
		@Override
		protected boolean supportsRowNumberFunction() {
			return false;
		}
	}

	@Nested
	class MySql_8_DatabaseTypeSingleDbDatabaseTypeTests extends AbstractSingleDbDatabaseTypeTests implements MySQL_8_ContainerSupport {
	}

	@Nested
	@EnabledIfEnvironmentVariable(named = "ENABLE_DB2", matches = "true", disabledReason = "Container is too big")
	@Tag("DB2")
	class DB2DatabaseTypeSingleDbDatabaseTypeTests extends AbstractSingleDbDatabaseTypeTests implements DB2_11_5_ContainerSupport {
	}

	@Nested
	@EnabledIfEnvironmentVariable(named = "ENABLE_ORACLE", matches = "true", disabledReason = "Container is too big")
	@Tag("ORACLE")
	class OracleDatabaseTypeSingleDbDatabaseTypeTests extends AbstractSingleDbDatabaseTypeTests implements Oracle_XE_18_ContainerSupport {
	}

	@Nested
	class SqlServer_2017_DatabaseTypeSingleDbDatabaseTypeTests extends AbstractSingleDbDatabaseTypeTests implements SqlServer_2017_ContainerSupport {
	}

	@Nested
	class SqlServer_2019_DatabaseTypeSingleDbDatabaseTypeTests extends AbstractSingleDbDatabaseTypeTests implements SqlServer_2019_ContainerSupport {
	}

	@Nested
	class SqlServer_2022_DatabaseTypeSingleDbDatabaseTypeTests extends AbstractSingleDbDatabaseTypeTests implements SqlServer_2022_ContainerSupport {
	}

}
