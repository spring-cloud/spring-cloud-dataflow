package org.springframework.cloud.dataflow.server.db.migration.postgresql;

import org.flywaydb.core.api.migration.Context;

import org.springframework.cloud.dataflow.common.flyway.AbstractMigration;

import static org.springframework.cloud.dataflow.server.db.migration.PostgreSQLTextToOID.convertColumn;
public class V9__ChangeTextTypes extends AbstractMigration {

	public V9__ChangeTextTypes() {
		super(null);
	}


	@Override
	public void migrate(Context context) throws Exception {
		convertColumn("app_registration", "id", "uri", context.getConfiguration().getDataSource());
		convertColumn("app_registration", "id", "metadata_uri", context.getConfiguration().getDataSource());
		//convertColumn("audit_records", "id", "audit_data", context.getConfiguration().getDataSource());
		convertColumn("stream_definitions", "definition_name", "definition", context.getConfiguration().getDataSource());
		convertColumn("stream_definitions", "definition_name", "original_definition", context.getConfiguration().getDataSource());
		convertColumn("task_definitions", "definition_name", "definition", context.getConfiguration().getDataSource());
	}
}
