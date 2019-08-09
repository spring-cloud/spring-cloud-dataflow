package org.springframework.cloud.dataflow.server.db.migration.sqlserver;

import java.util.Arrays;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import org.springframework.cloud.dataflow.server.db.migration.SqlCommand;
import org.springframework.cloud.dataflow.server.db.migration.SqlCommandsRunner;

public class V2__Add_Descriptions extends BaseJavaMigration {

    public final static String ALTER_STREAM_DEFINITION_TABLE =
            "alter table stream_definitions add description varchar(255)";

    public final static String ALTER_TASK_DEFINITION_TABLE = "" +
            "alter table task_definitions add description varchar(255)";

    private final SqlCommandsRunner runner = new SqlCommandsRunner();

    @Override
    public void migrate(Context context) throws Exception {
        runner.execute(context.getConnection(), Arrays.asList(
                SqlCommand.from(ALTER_STREAM_DEFINITION_TABLE),
                SqlCommand.from(ALTER_TASK_DEFINITION_TABLE)));
    }
}
