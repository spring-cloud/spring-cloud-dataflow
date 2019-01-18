/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.cloud.dataflow.server.db.migration.AbstractMigration;
import org.springframework.cloud.dataflow.server.db.migration.SqlCommand;

/**
 * Repeatable migration ensuring that {@code hibernate_sequence} table exists.
 * Done for {@code mssql} via java and suppressing error as it doesn't support
 * "create sequence if not exists".
 *
 * @author Janne Valkealahti
 *
 */
public class R__Hibernate_Sequence extends AbstractMigration {

	// Caused by: org.springframework.jdbc.UncategorizedSQLException:
	// StatementCallback; uncategorized SQLException for SQL [create sequence hibernate_sequence start with 1 increment by 1];
	// SQL state [S0001]; error code [2714]; There is already an object named 'hibernate_sequence' in the database.;
	// nested exception is com.microsoft.sqlserver.jdbc.SQLServerException:
	// There is already an object named 'hibernate_sequence' in the database.
	private final static List<SqlCommand> commands = Arrays.asList(
			SqlCommand.from("create sequence hibernate_sequence start with 1 increment by 1", 2714));

	public R__Hibernate_Sequence() {
		super(commands);
	}
}
