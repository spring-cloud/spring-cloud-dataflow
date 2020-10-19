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
package org.springframework.cloud.dataflow.server.db.migration.mysql;

import java.util.Arrays;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import org.springframework.cloud.dataflow.common.flyway.SqlCommand;
import org.springframework.cloud.dataflow.common.flyway.SqlCommandsRunner;

/**
 * This migration class adds description column to stream_definitions and task_definitions
 * tables and original_definition column to stream_definitions.
 *
 * @author Daniel Serleg
 * @author Ilayaperumal Gopinathan
 * @author Michael Minella
 *
 * @since 2.3
 */
public class V2__Add_Descriptions_And_OriginalDefinition extends BaseJavaMigration {

	public final static String ALTER_STREAM_DEFINITION_TABLE_DESC = "alter table stream_definitions add description varchar(255)";

	public final static String ALTER_STREAM_DEFINITION_TABLE_ORIG_DEF = "alter table stream_definitions add original_definition longtext";

	public final static String ALTER_TASK_DEFINITION_TABLE = "" +
			"alter table task_definitions add description varchar(255)";

	public final static String UPDATE_STREAM_DEFINITION_TABLE_ORIG_DEF = "update stream_definitions set original_definition=definition";

	public final static String CREATE_TASK_METADATA_TABLE =
			"CREATE TABLE task_execution_metadata (\n" +
					"    id BIGINT NOT NULL,\n" +
					"    task_execution_id BIGINT NOT NULL,\n" +
					"    task_execution_manifest LONGTEXT,\n" +
					"    primary key (id),\n" +
					"    CONSTRAINT TASK_METADATA_FK FOREIGN KEY (task_execution_id)\n" +
					"    REFERENCES TASK_EXECUTION(TASK_EXECUTION_ID)\n" +
					")";

	public final static String CREATE_TASK_METADATA_SEQUENCE =
			"CREATE TABLE task_execution_metadata_seq (\n" +
					"  ID BIGINT NOT NULL,\n" +
					"  UNIQUE_KEY CHAR(1) NOT NULL,\n" +
					"  constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)\n" +
					")";

	public final static String INSERT_TASK_METADATA_SEQUENCE =
			"INSERT INTO task_execution_metadata_seq (ID, UNIQUE_KEY) select * from (select 0 as ID, '0' as UNIQUE_KEY) as tmp where not exists(select * from task_execution_metadata_seq)";

	private final SqlCommandsRunner runner = new SqlCommandsRunner();

	@Override
	public void migrate(Context context) throws Exception {
		runner.execute(context.getConnection(), Arrays.asList(
				SqlCommand.from(ALTER_STREAM_DEFINITION_TABLE_DESC),
				SqlCommand.from(ALTER_STREAM_DEFINITION_TABLE_ORIG_DEF),
				SqlCommand.from(ALTER_TASK_DEFINITION_TABLE),
				SqlCommand.from(UPDATE_STREAM_DEFINITION_TABLE_ORIG_DEF),
				SqlCommand.from(CREATE_TASK_METADATA_TABLE),
				SqlCommand.from(CREATE_TASK_METADATA_SEQUENCE),
				SqlCommand.from(INSERT_TASK_METADATA_SEQUENCE)));

	}
}
