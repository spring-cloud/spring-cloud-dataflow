package org.springframework.cloud.dataflow.server.db.migration.oracle;

import java.util.Arrays;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.cloud.dataflow.common.flyway.SqlCommand;
import org.springframework.cloud.dataflow.common.flyway.SqlCommandsRunner;

/**
 * This migration class adds index for TASK_EXECUTION_ID on TASK_EXECUTION_PARAMS.
 *
 * @author Claudio Tasso
 *
 * @since 2.10
 */
public class V5__Add_Task_Execution_Params_Indexes extends BaseJavaMigration {

	public final static String ADD_INDEX_TO_STEP_EXECUTION_PARAMS = "create index TASK_EXECUTION_ID_IDX on TASK_EXECUTION_PARAMS (TASK_EXECUTION_ID)";

	private final SqlCommandsRunner runner = new SqlCommandsRunner();

	@Override
	public void migrate(Context context) throws Exception {
		runner.execute(context.getConnection(), Arrays.asList(
				SqlCommand.from(ADD_INDEX_TO_STEP_EXECUTION_PARAMS)));
	

	}
}
