package org.springframework.cloud.dataflow.server.db.migration.postgresql;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.common.flyway.AbstractMigration;
import org.springframework.cloud.dataflow.common.flyway.SqlCommand;
import org.springframework.jdbc.core.JdbcTemplate;

public class V9__ChangeTextTypes extends AbstractMigration {
	private final static Logger logger = LoggerFactory.getLogger(V9__ChangeTextTypes.class);

	private final static String ADD_TMP_OID_COL = "alter table %s add column %s oid";

	private final static String UPDATE_TMP_OID_COL = "update %s set %s = lo_from_bytea(0, %s::bytea), %s = null where %s = (select %s from %s where %s is null and %s is not null limit 100)";

	private final static String DROP_ORIGINAL_COL = "alter table %s drop column %s";

	private final static String RENAME_OID_COL = "alter table %s rename column %s to %s";

	public V9__ChangeTextTypes() {
		super(null);
	}

	private void convertColumn(String table, String id, String column, DataSource dataSource) {
		JdbcTemplate template = new JdbcTemplate(dataSource);
		final String tmp_col = column + "_tmp";
		String sqlTmp = String.format(ADD_TMP_OID_COL, table, tmp_col);
		logger.debug("Executing:{}", sqlTmp);
		template.update(sqlTmp);
		int total = 0;
		do {
			String sql = String.format(UPDATE_TMP_OID_COL, table, tmp_col, column, column, id, id, table, tmp_col, column);
			logger.debug("Executing:{}", sql);
			int count = template.update(sql);
			total += count;
			if (count <= 0) {
				logger.info("Updated {} rows of {} in {}", total, column, table);
				break;
			}
		} while (true);
		String sqlDrop = String.format(DROP_ORIGINAL_COL, table, column);
		logger.debug("Executing:{}", sqlDrop);
		template.update(sqlDrop);
		String sqlRename = String.format(RENAME_OID_COL, table, tmp_col, column);
		logger.debug("Executing:{}", sqlRename);
		template.update(sqlRename);
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
