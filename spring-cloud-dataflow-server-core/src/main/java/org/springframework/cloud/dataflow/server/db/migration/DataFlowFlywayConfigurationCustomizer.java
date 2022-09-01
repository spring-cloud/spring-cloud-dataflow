/*
 * Copyright 2019-2022 the original author or authors.
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

import javax.sql.DataSource;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.cloud.dataflow.common.flyway.DatabaseDriverUtils;
import org.springframework.cloud.dataflow.server.db.migration.db2.Db2BeforeBaseline;
import org.springframework.cloud.dataflow.server.db.migration.mariadb.MariadbBeforeBaseline;
import org.springframework.cloud.dataflow.server.db.migration.mysql.MysqlBeforeBaseline;
import org.springframework.cloud.dataflow.server.db.migration.oracle.OracleBeforeBaseline;
import org.springframework.cloud.dataflow.server.db.migration.postgresql.PostgresBeforeBaseline;
import org.springframework.cloud.dataflow.server.db.migration.sqlserver.MsSqlBeforeBaseline;

/**
 * Flyway {@link FlywayConfigurationCustomizer} bean customizing callbacks per
 * active db vendor.
 *
 * @author Janne Valkealahti
 * @author Chris Bono
 */
public class DataFlowFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer {

	private static final Logger LOG = LoggerFactory.getLogger(DataFlowFlywayConfigurationCustomizer.class);

	@Override
	public void customize(FluentConfiguration configuration) {
		// Boot's flyway auto-config doesn't allow to define callbacks per
		// vendor id, so essentially customizing those here.
		DataSource dataSource = configuration.getDataSource();
		DatabaseDriver databaseDriver = DatabaseDriverUtils.getDatabaseDriver(dataSource);
		LOG.info("Adding vendor specific Flyway callback for {}", databaseDriver.name());
		if (databaseDriver == DatabaseDriver.POSTGRESQL) {
			configuration.callbacks(new PostgresBeforeBaseline());
		}
		else if (databaseDriver == DatabaseDriver.MARIADB) {
			configuration.callbacks(new MariadbBeforeBaseline());
		}
		else if (databaseDriver == DatabaseDriver.MYSQL) {
			configuration.callbacks(new MysqlBeforeBaseline());
		}
		else if (databaseDriver == DatabaseDriver.SQLSERVER) {
			configuration.callbacks(new MsSqlBeforeBaseline());
		}
		else if (databaseDriver == DatabaseDriver.ORACLE) {
			configuration.callbacks(new OracleBeforeBaseline());
		}
		else if (databaseDriver == DatabaseDriver.DB2) {
			configuration.callbacks(new Db2BeforeBaseline());
		}
	}
}
