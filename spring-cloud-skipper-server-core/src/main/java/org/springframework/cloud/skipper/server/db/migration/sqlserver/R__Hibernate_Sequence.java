/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.skipper.server.db.migration.sqlserver;

import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.server.db.migration.AbstractMigration;
import org.springframework.cloud.skipper.server.db.migration.SqlCommand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Repeatable migration ensuring that {@code hibernate_sequence} table exists.
 * Done for {@code mssql} via java and suppressing error as it doesn't support
 * "create sequence if not exists". Also we fix wrongly create hibernate_sequence
 * if it was created as table by replacing it with a proper sequence.
 *
 * @author Janne Valkealahti
 *
 */
public class R__Hibernate_Sequence extends AbstractMigration {

	private static final Logger logger = LoggerFactory.getLogger(R__Hibernate_Sequence.class);

	// Caused by: org.springframework.jdbc.UncategorizedSQLException:
	// StatementCallback; uncategorized SQLException for SQL [create sequence hibernate_sequence start with 1 increment by 1];
	// SQL state [S0001]; error code [2714]; There is already an object named 'hibernate_sequence' in the database.;
	// nested exception is com.microsoft.sqlserver.jdbc.SQLServerException:
	// There is already an object named 'hibernate_sequence' in the database.
	private final static List<SqlCommand> commands = Arrays.asList(
			SqlCommand.from("create sequence hibernate_sequence start with 1 increment by 1", 2714));

	// sequence of tsql commands to change table to sequence
	private final static List<SqlCommand> fixcommands = Arrays.asList(
			SqlCommand.from("exec sp_rename 'hibernate_sequence', 'hibernate_sequence_old';  \n" +
					"declare @max int;\n" +
					"select @max = max(next_val) from hibernate_sequence_old;\n" +
					"exec('create sequence hibernate_sequence start with ' + @max + ' increment by 1;');\n" +
					"drop table hibernate_sequence_old;"));

	private boolean fixHibernateSequence;

	public R__Hibernate_Sequence() {
		super(commands);
	}

	@Override
	public void migrate(Context context) throws Exception {
		logger.info("About to check if mssql hibernate_sequence needs fix from table to a sequence");
		try {
			JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
			// in case we have old wrongly created table, this command should succeed
			jdbcTemplate.execute("select 1 from hibernate_sequence");
			fixHibernateSequence = true;
			logger.info("Looks like we have hibernate_sequence table, initiate fix");
		}
		catch (Exception e) {
			logger.debug("Unable to query hibernate_sequence table, looks like we have a proper sequence", e);
		}
		// will result call to get commands from this class and then we choose which ones to run
		super.migrate(context);
	}

	@Override
	public List<SqlCommand> getCommands() {
		return fixHibernateSequence ? fixcommands : super.getCommands();
	}
}
