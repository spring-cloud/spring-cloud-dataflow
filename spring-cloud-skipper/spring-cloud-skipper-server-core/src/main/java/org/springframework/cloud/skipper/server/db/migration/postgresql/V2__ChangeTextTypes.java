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
package org.springframework.cloud.skipper.server.db.migration.postgresql;

import org.flywaydb.core.api.migration.Context;

import org.springframework.cloud.dataflow.common.flyway.AbstractMigration;
import org.springframework.cloud.skipper.server.db.migration.PostgreSQLTextToOID;

public class V2__ChangeTextTypes extends AbstractMigration {
	public V2__ChangeTextTypes() {
		super(null);
	}

	@Override
	public void migrate(Context context) {
		PostgreSQLTextToOID.convertColumn("skipper_app_deployer_data", "id", "deployment_data", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumn("skipper_manifest", "id", "data", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumn("skipper_package_metadata", "id", "description", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumn("skipper_package_metadata", "id", "icon_url", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumn("skipper_package_metadata", "id", "package_home_url", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumn("skipper_package_metadata", "id", "package_source_url", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumn("skipper_package_metadata", "id", "tags", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumn("skipper_release", "id", "config_values_string", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumn("skipper_release", "id", "pkg_json_string", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumn("skipper_repository", "id", "source_url", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumn("skipper_repository", "id", "url", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumn("skipper_status", "id", "platform_status", context.getConfiguration().getDataSource());
	}
}
