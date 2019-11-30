package org.springframework.cloud.dataflow.server.db.migration.mysql;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.cloud.dataflow.server.db.migration.SqlCommand;
import org.springframework.cloud.dataflow.server.db.migration.SqlCommandsRunner;

import java.util.Arrays;

public class V3__Add_Platform_To_AuditRecords extends BaseJavaMigration {

    public final static String ALTER_AUDIT_RECORDS_TABLE_PLATFORM = "alter table audit_records add platform_name varchar(255)";

    private final SqlCommandsRunner runner = new SqlCommandsRunner();

    @Override
    public void migrate(Context context) throws Exception {
        runner.execute(context.getConnection(), Arrays.asList(
                SqlCommand.from(ALTER_AUDIT_RECORDS_TABLE_PLATFORM)));
    }
}
