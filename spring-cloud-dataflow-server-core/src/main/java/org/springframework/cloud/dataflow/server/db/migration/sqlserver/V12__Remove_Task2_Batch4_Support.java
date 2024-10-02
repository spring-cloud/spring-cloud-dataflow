/*
 * Copyright 2024 the original author or authors.
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

import org.springframework.cloud.dataflow.common.flyway.SqlCommand;
import org.springframework.cloud.dataflow.server.db.migration.AbstractRemoveBatch4Task2Tables;

/**
 * Prefixes Task V2 tables and V4 Batch tables with a V2_ prefix as well as remove the BOOT3_ prefix for V3 task and v5 batch tables.
 *
 * @author Glenn Renfro
 */
public class V12__Remove_Task2_Batch4_Support extends AbstractRemoveBatch4Task2Tables {

	/*
	 * Scripts to remove views used for Task V2/Batch V4 Task V3/Batch V5 queries.
	 */
	private final static String DROP_VIEW_AGGREGATE_TASK_EXECUTION =
		"DROP VIEW AGGREGATE_TASK_EXECUTION";

	private final static String DROP_VIEW_AGGREGATE_TASK_EXECUTION_PARAMS =
		"DROP VIEW AGGREGATE_TASK_EXECUTION_PARAMS";

	private final static String DROP_VIEW_AGGREGATE_JOB_EXECUTION =
		"DROP VIEW AGGREGATE_JOB_EXECUTION";

	private final static String DROP_VIEW_AGGREGATE_JOB_INSTANCE =
		"DROP VIEW AGGREGATE_JOB_INSTANCE";

	private final static String DROP_VIEW_AGGREGATE_TASK_BATCH =
		"DROP VIEW AGGREGATE_TASK_BATCH";

	private final static String DROP_VIEW_AGGREGATE_STEP_EXECUTION =
		"DROP VIEW AGGREGATE_STEP_EXECUTION";

	/*
	 * Scripts to rename Task V2 tables removing BOOT_ prefix.
	 */
	private final static String RENAME_TASK_EXECUTION_V2_TABLE =
		"exec sp_rename 'TASK_EXECUTION', 'V2_TASK_EXECUTION'";

	private final static String RENAME_TASK_EXECUTION_PARAMS_V2_TABLE =
		"exec sp_rename 'TASK_EXECUTION_PARAMS', 'V2_TASK_EXECUTION_PARAMS'";

	private final static String RENAME_TASK_TASK_BATCH_V2_TABLE =
		"exec sp_rename 'TASK_TASK_BATCH', 'V2_TASK_TASK_BATCH'";

	private final static String RENAME_TASK_LOCK_V2_TABLE =
		"exec sp_rename 'TASK_LOCK', 'V2_TASK_LOCK'";

	private final static String RENAME_TASK_V2_SEQ =
		"exec sp_rename 'TASK_SEQ', 'V2_TASK_SEQ'";

	private final static String RENAME_TASK_EXECUTION_METADATA_V2_TABLE =
		"exec sp_rename 'TASK_EXECUTION_METADATA', 'V2_TASK_EXECUTION_METADATA'";

	private final static String RENAME_TASK_EXECUTION_METADATA_V2_SEQ =
		"exec sp_rename 'TASK_EXECUTION_METADATA_SEQ', 'V2_TASK_EXECUTION_METADATA_SEQ'";

	/*
	 * Scripts to rename Batch V5 tables removing BOOT_ prefix.
	 */

	private final static String RENAME_BATCH_JOB_INSTANCE_V4_TABLE =
		"exec sp_rename 'BATCH_JOB_INSTANCE', 'V2_BATCH_JOB_INSTANCE'";

	private final static String RENAME_BATCH_JOB_EXECUTION_V4_TABLE =
		"exec sp_rename 'BATCH_JOB_EXECUTION', 'V2_BATCH_JOB_EXECUTION'";

	private final static String RENAME_BATCH_JOB_EXECUTION_PARAMS_V4_TABLE =
		"exec sp_rename 'BATCH_JOB_EXECUTION_PARAMS', 'V2_BATCH_JOB_EXECUTION_PARAMS'";

	private final static String RENAME_BATCH_STEP_EXECUTION_V4_TABLE =
		"exec sp_rename 'BATCH_STEP_EXECUTION', 'V2_BATCH_STEP_EXECUTION'";

	private final static String RENAME_BATCH_STEP_EXECUTION_CONTEXT_V4_TABLE =
		"exec sp_rename 'BATCH_STEP_EXECUTION_CONTEXT', 'V2_BATCH_STEP_EXECUTION_CONTEXT'";

	private final static String RENAME_BATCH_JOB_EXECUTION_CONTEXT_V4_TABLE =
		"exec sp_rename 'BATCH_JOB_EXECUTION_CONTEXT', 'V2_BATCH_JOB_EXECUTION_CONTEXT'";

	private final static String RENAME_BATCH_STEP_EXECUTION_V4_SEQ =
		"exec sp_rename 'BATCH_STEP_EXECUTION_SEQ', 'V2_BATCH_STEP_EXECUTION_SEQ'";

	private final static String RENAME_BATCH_JOB_EXECUTION_V4_SEQ =
		"exec sp_rename 'BATCH_JOB_EXECUTION_SEQ', 'V2_BATCH_JOB_EXECUTION_SEQ'";

	private final static String RENAME_BATCH_JOB_V4_SEQ =
		"exec sp_rename 'BATCH_JOB_SEQ', 'V2_BATCH_JOB_SEQ'";

	/*
	 * Scripts to rename Task V3 tables removing BOOT_ prefix.
	 */
	private final static String RENAME_TASK_EXECUTION_V3_TABLE =
			"exec sp_rename 'BOOT3_TASK_EXECUTION', 'TASK_EXECUTION'";

	private final static String RENAME_TASK_EXECUTION_PARAMS_V3_TABLE =
			"exec sp_rename 'BOOT3_TASK_EXECUTION_PARAMS', 'TASK_EXECUTION_PARAMS'";

	private final static String RENAME_TASK_TASK_BATCH_V3_TABLE =
			"exec sp_rename 'BOOT3_TASK_TASK_BATCH', 'TASK_TASK_BATCH'";

	private final static String RENAME_TASK_LOCK_V3_TABLE =
			"exec sp_rename 'BOOT3_TASK_LOCK', 'TASK_LOCK'";

	private final static String RENAME_TASK_V3_SEQ =
			"exec sp_rename 'BOOT3_TASK_SEQ', 'TASK_SEQ'";

	private final static String RENAME_TASK_EXECUTION_METADATA_V3_TABLE =
			"exec sp_rename 'BOOT3_TASK_EXECUTION_METADATA', 'TASK_EXECUTION_METADATA'";

	private final static String RENAME_TASK_EXECUTION_METADATA_V3_SEQ =
			"exec sp_rename 'BOOT3_TASK_EXECUTION_METADATA_SEQ', 'TASK_EXECUTION_METADATA_SEQ'";

	/*
	 * Scripts to rename Batch V5 tables removing BOOT_ prefix.
	 */

	private final static String RENAME_BATCH_JOB_INSTANCE_V5_TABLE =
			"exec sp_rename 'BOOT3_BATCH_JOB_INSTANCE', 'BATCH_JOB_INSTANCE'";

	private final static String RENAME_BATCH_JOB_EXECUTION_V5_TABLE =
			"exec sp_rename 'BOOT3_BATCH_JOB_EXECUTION', 'BATCH_JOB_EXECUTION'";

	private final static String RENAME_BATCH_JOB_EXECUTION_PARAMS_V5_TABLE =
			"exec sp_rename 'BOOT3_BATCH_JOB_EXECUTION_PARAMS', 'BATCH_JOB_EXECUTION_PARAMS'";

	private final static String RENAME_BATCH_STEP_EXECUTION_V5_TABLE =
			"exec sp_rename 'BOOT3_BATCH_STEP_EXECUTION', 'BATCH_STEP_EXECUTION'";

	private final static String RENAME_BATCH_STEP_EXECUTION_CONTEXT_V5_TABLE =
			"exec sp_rename 'BOOT3_BATCH_STEP_EXECUTION_CONTEXT', 'BATCH_STEP_EXECUTION_CONTEXT'";

	private final static String RENAME_BATCH_JOB_EXECUTION_CONTEXT_V5_TABLE =
			"exec sp_rename 'BOOT3_BATCH_JOB_EXECUTION_CONTEXT', 'BATCH_JOB_EXECUTION_CONTEXT'";

	private final static String RENAME_BATCH_STEP_EXECUTION_V5_SEQ =
			"exec sp_rename 'BOOT3_BATCH_STEP_EXECUTION_SEQ', 'BATCH_STEP_EXECUTION_SEQ'";

	private final static String RENAME_BATCH_JOB_EXECUTION_V5_SEQ =
			"exec sp_rename 'BOOT3_BATCH_JOB_EXECUTION_SEQ', 'BATCH_JOB_EXECUTION_SEQ'";

	private final static String RENAME_BATCH_JOB_V5_SEQ =
			"exec sp_rename 'BOOT3_BATCH_JOB_SEQ', 'BATCH_JOB_SEQ'";

	@Override
	public List<SqlCommand> dropBoot3Boot2Views() {
		return Arrays.asList(
			SqlCommand.from(DROP_VIEW_AGGREGATE_TASK_EXECUTION),
			SqlCommand.from(DROP_VIEW_AGGREGATE_TASK_EXECUTION_PARAMS),
			SqlCommand.from(DROP_VIEW_AGGREGATE_JOB_EXECUTION),
			SqlCommand.from(DROP_VIEW_AGGREGATE_JOB_INSTANCE),
			SqlCommand.from(DROP_VIEW_AGGREGATE_TASK_BATCH),
			SqlCommand.from(DROP_VIEW_AGGREGATE_STEP_EXECUTION)
		);
	}

	@Override
	public List<SqlCommand> renameTask3Tables() {
		return Arrays.asList(
			SqlCommand.from(RENAME_TASK_EXECUTION_V3_TABLE),
			SqlCommand.from(RENAME_TASK_EXECUTION_PARAMS_V3_TABLE),
			SqlCommand.from(RENAME_TASK_TASK_BATCH_V3_TABLE),
			SqlCommand.from(RENAME_TASK_V3_SEQ),
			SqlCommand.from(RENAME_TASK_LOCK_V3_TABLE),
			SqlCommand.from(RENAME_TASK_EXECUTION_METADATA_V3_TABLE),
			SqlCommand.from(RENAME_TASK_EXECUTION_METADATA_V3_SEQ)
		);
	}

	@Override
	public List<SqlCommand> renameBatch5Tables() {
		return Arrays.asList(
			SqlCommand.from(RENAME_BATCH_JOB_INSTANCE_V5_TABLE),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_V5_TABLE),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_PARAMS_V5_TABLE),
			SqlCommand.from(RENAME_BATCH_STEP_EXECUTION_V5_TABLE),
			SqlCommand.from(RENAME_BATCH_STEP_EXECUTION_CONTEXT_V5_TABLE),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_CONTEXT_V5_TABLE),
			SqlCommand.from(RENAME_BATCH_STEP_EXECUTION_V5_SEQ),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_V5_SEQ),
			SqlCommand.from(RENAME_BATCH_JOB_V5_SEQ)
		);
	}

	@Override
	public List<SqlCommand> renameTask2Tables() {
		return Arrays.asList(
			SqlCommand.from(RENAME_TASK_EXECUTION_V2_TABLE),
			SqlCommand.from(RENAME_TASK_EXECUTION_PARAMS_V2_TABLE),
			SqlCommand.from(RENAME_TASK_TASK_BATCH_V2_TABLE),
			SqlCommand.from(RENAME_TASK_V2_SEQ),
			SqlCommand.from(RENAME_TASK_LOCK_V2_TABLE),
			SqlCommand.from(RENAME_TASK_EXECUTION_METADATA_V2_TABLE),
			SqlCommand.from(RENAME_TASK_EXECUTION_METADATA_V2_SEQ)
		);
	}

	@Override
	public List<SqlCommand> renameBatch4Tables() {
		return Arrays.asList(
			SqlCommand.from(RENAME_BATCH_JOB_INSTANCE_V4_TABLE),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_V4_TABLE),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_PARAMS_V4_TABLE),
			SqlCommand.from(RENAME_BATCH_STEP_EXECUTION_V4_TABLE),
			SqlCommand.from(RENAME_BATCH_STEP_EXECUTION_CONTEXT_V4_TABLE),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_CONTEXT_V4_TABLE),
			SqlCommand.from(RENAME_BATCH_STEP_EXECUTION_V4_SEQ),
			SqlCommand.from(RENAME_BATCH_JOB_EXECUTION_V4_SEQ),
			SqlCommand.from(RENAME_BATCH_JOB_V4_SEQ)
		);
	}

}
