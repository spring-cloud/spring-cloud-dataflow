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
package org.springframework.cloud.dataflow.server.db.migration.oracle;

import java.util.Arrays;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import org.springframework.cloud.dataflow.common.flyway.SqlCommand;
import org.springframework.cloud.dataflow.common.flyway.SqlCommandsRunner;

/**
 * Fixes the names of the {@code BOOT3_BATCH_JOB_EXECUTION_PARAMS} parameter columns.
 *
 * @author Chris Bono
 */
public class V9__Boot3_Batch5_Job_Execution_Params_Column_Fix extends BaseJavaMigration {

	public final static String DROP_COLUMNS_BATCH_JOB_EXECUTION_PARAMS_TABLE =
			"ALTER TABLE BOOT3_BATCH_JOB_EXECUTION_PARAMS DROP (TYPE_CD, KEY_NAME, STRING_VAL, DATE_VAL, " +
					"LONG_VAL, DOUBLE_VAL, IDENTIFYING)";

	public final static String ADD_COLUMNS_BATCH_JOB_EXECUTION_PARAMS_TABLE =
			"ALTER TABLE BOOT3_BATCH_JOB_EXECUTION_PARAMS ADD (\n" +
					"    PARAMETER_NAME VARCHAR(100 char) NOT NULL,\n" +
					"    PARAMETER_TYPE VARCHAR(100 char) NOT NULL,\n" +
					"    PARAMETER_VALUE VARCHAR(2500 char),\n" +
					"	 IDENTIFYING CHAR(1) NOT NULL\n" +
					")";

	private final SqlCommandsRunner runner = new SqlCommandsRunner();

	@Override
	public void migrate(Context context) throws Exception {
		runner.execute(context.getConnection(), Arrays.asList(
				SqlCommand.from(DROP_COLUMNS_BATCH_JOB_EXECUTION_PARAMS_TABLE),
				SqlCommand.from(ADD_COLUMNS_BATCH_JOB_EXECUTION_PARAMS_TABLE)));
	}
}
