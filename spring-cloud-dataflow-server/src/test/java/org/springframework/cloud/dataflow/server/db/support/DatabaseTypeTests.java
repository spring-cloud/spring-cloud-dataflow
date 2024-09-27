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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.cloud.dataflow.server.db.DB2_11_5_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.MariaDB_10_6_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.MariaDB_11_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.MySQL_5_7_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.MySQL_8_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.Oracle_XE_18_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.SqlServer_2017_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.SqlServer_2019_ContainerSupport;
import org.springframework.cloud.dataflow.server.db.SqlServer_2022_ContainerSupport;

class DatabaseTypeTests {

	@Nested
	class MariaDB_10_6_DatabaseTypeTests extends SingleDbDatabaseTypeTests implements MariaDB_10_6_ContainerSupport {
	}

	@Nested
	class MariaDB_11_DatabaseTypeTests extends SingleDbDatabaseTypeTests implements MariaDB_11_ContainerSupport {
	}

	@Nested
	class MySql_5_7_DatabaseTypeTests extends SingleDbDatabaseTypeTests implements MySQL_5_7_ContainerSupport {
		@Override
		protected boolean supportsRowNumberFunction() {
			return false;
		}
	}

	@Nested
	class MySql_8_DatabaseTypeTests extends SingleDbDatabaseTypeTests implements MySQL_8_ContainerSupport {
	}

	@Nested
	@EnabledIfEnvironmentVariable(named = "ENABLE_DB2", matches = "true", disabledReason = "Container is too big")
	@Tag("DB2")
	class DB2DatabaseTypeTests extends SingleDbDatabaseTypeTests implements DB2_11_5_ContainerSupport {
	}

	@Nested
	@EnabledIfEnvironmentVariable(named = "ENABLE_ORACLE", matches = "true", disabledReason = "Container is too big")
	@Tag("ORACLE")
	class OracleDatabaseTypeTests extends SingleDbDatabaseTypeTests implements Oracle_XE_18_ContainerSupport {
	}

	@Disabled("See https://github.com/spring-cloud/spring-cloud-dataflow/issues/5952")
	@Nested
	class SqlServer_2017_DatabaseTypeSingleDbDatabaseTypeTests extends SingleDbDatabaseTypeTests implements SqlServer_2017_ContainerSupport {
	}

	@Nested
	class SqlServer_2019_DatabaseTypeSingleDbDatabaseTypeTests extends SingleDbDatabaseTypeTests implements SqlServer_2019_ContainerSupport {
	}

	@Nested
	class SqlServer_2022_DatabaseTypeSingleDbDatabaseTypeTests extends SingleDbDatabaseTypeTests implements SqlServer_2022_ContainerSupport {
	}
}
