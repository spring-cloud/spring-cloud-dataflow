/*
 * Copyright 2022-2022 the original author or authors.
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
package org.flywaydb.database.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.extensibility.Tier;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.database.base.Table;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.database.mysql.mariadb.MariaDBDatabaseType;

public class MySQL57Database extends Database<MySQLConnection> {

    private final MySQLDatabase delegateDatabase;

    public MySQL57Database(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory, StatementInterceptor statementInterceptor) {
        this(configuration, jdbcConnectionFactory, statementInterceptor, new MySQLDatabase(configuration, jdbcConnectionFactory, statementInterceptor));
    }

    protected MySQL57Database(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory, StatementInterceptor statementInterceptor, MySQLDatabase delegateDatabase) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
        this.delegateDatabase = delegateDatabase;
    }

    @Override
    public String getRawCreateScript(Table table, boolean baseline) {
        return delegateDatabase.getRawCreateScript(table, baseline);
    }

    @Override
    protected MySQLConnection doGetConnection(Connection connection) {
        return delegateDatabase.doGetConnection(connection);
    }

    @Override
    protected MigrationVersion determineVersion() {
        return delegateDatabase.determineVersion();
    }

    @Override
    public void ensureSupported(Configuration configuration) {
        ensureDatabaseIsRecentEnough("5.1");
        if (databaseType instanceof MariaDBDatabaseType) {
            ensureDatabaseNotOlderThanOtherwiseRecommendUpgradeToFlywayEdition("10.4", List.of(Tier.ENTERPRISE), configuration);
            recommendFlywayUpgradeIfNecessary("10.6");
        } else {
            ensureDatabaseNotOlderThanOtherwiseRecommendUpgradeToFlywayEdition("5.7", List.of(Tier.ENTERPRISE), configuration);
            recommendFlywayUpgradeIfNecessary("8.0");
        }
    }

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            delegateDatabase.close();
        }
    }

    @Override
    protected String doGetCurrentUser() throws SQLException {
        return delegateDatabase.doGetCurrentUser();
    }

    @Override
    public boolean supportsDdlTransactions() {
        return delegateDatabase.supportsDdlTransactions();
    }

    @Override
    public String getBooleanTrue() {
        return delegateDatabase.getBooleanTrue();
    }

    @Override
    public String getBooleanFalse() {
        return delegateDatabase.getBooleanFalse();
    }

    @Override
    public String getOpenQuote() {
        return delegateDatabase.getOpenQuote();
    }

    @Override
    public String getCloseQuote() {
        return delegateDatabase.getCloseQuote();
    }

    @Override
    public boolean catalogIsSchema() {
        return delegateDatabase.catalogIsSchema();
    }

    @Override
    public boolean useSingleConnection() {
        return delegateDatabase.useSingleConnection();
    }
}
