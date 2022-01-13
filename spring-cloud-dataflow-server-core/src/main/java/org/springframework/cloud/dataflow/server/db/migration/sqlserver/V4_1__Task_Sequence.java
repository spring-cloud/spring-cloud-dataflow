/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.cloud.dataflow.server.db.migration.sqlserver;

import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.common.flyway.AbstractMigration;
import org.springframework.cloud.dataflow.common.flyway.SqlCommand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Migration ensuring that {@code TASK_SEQ} sequence exists.
 * Done for {@code mssql} via java and suppressing error as it doesn't support
 * "create sequence if not exists". Also we migrate from {@code TASK_SEQ} table
 * into sequence by taking out a correct seq value.
 *
 * @author Janne Valkealahti
 *
 */
public class V4_1__Task_Sequence extends AbstractMigration {

	private static final Logger logger = LoggerFactory.getLogger(V4_1__Task_Sequence.class);

	private final static List<SqlCommand> commands = Arrays.asList(
			SqlCommand.from("create sequence TASK_SEQ start with 1 increment by 1", 2714));

	// sequence of tsql commands to change table to sequence
	// need to +1 as initial sequence is the one set
	public final static List<SqlCommand> fixcommands = Arrays.asList(
			SqlCommand.from("exec sp_rename 'TASK_SEQ', 'TASK_SEQ_OLD';  \n" +
					"declare @max int;\n" +
					"select @max = isnull(max(ID),0)+1 from TASK_SEQ_OLD;\n" +
					"exec('create sequence TASK_SEQ start with ' + @max + ' increment by 1;');\n" +
					"drop table TASK_SEQ_OLD;"));

	private boolean fixSequence;

	public V4_1__Task_Sequence() {
		super(commands);
	}

	@Override
	public void migrate(Context context) throws Exception {
		logger.info("About to check if mssql TASK_SEQ needs fix from table to a sequence");
		try {
			JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
			// in case we have old wrongly created table, this command should succeed
			jdbcTemplate.execute("select 1 from TASK_SEQ");
			fixSequence = true;
			logger.info("Looks like we have TASK_SEQ table, initiate fix");
		}
		catch (Exception e) {
			logger.debug("Unable to query TASK_SEQ table, a TASK_SEQ sequence may already exist", e);
		}
		// will result call to get commands from this class and then we choose which ones to run
		super.migrate(context);
	}

	@Override
	public List<SqlCommand> getCommands() {
		return fixSequence ? fixcommands : super.getCommands();
	}
}
