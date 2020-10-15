/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.cloud.dataflow.server.db.migration.postgresql;

import java.util.Arrays;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import org.springframework.cloud.dataflow.common.flyway.SqlCommand;
import org.springframework.cloud.dataflow.common.flyway.SqlCommandsRunner;

/**
 * This migration class adds index for STEP_NAME on BATCH_STEP_EXECUTION.
 *
 * @author Glenn Renfro
 *
 * @since 2.7
 */
public class V4__Add_Step_Name_Indexes extends BaseJavaMigration {

	public final static String ADD_INDEX_TO_BATCH_STEP_EXECUTION = "create index STEP_NAME_IDX on BATCH_STEP_EXECUTION (STEP_NAME)";

	private final SqlCommandsRunner runner = new SqlCommandsRunner();

	@Override
	public void migrate(Context context) throws Exception {
		runner.execute(context.getConnection(), Arrays.asList(
				SqlCommand.from(ADD_INDEX_TO_BATCH_STEP_EXECUTION)));
	}
}
