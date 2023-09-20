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
package org.springframework.cloud.dataflow.server.db.migration.postgresql;

import org.flywaydb.core.api.migration.Context;

import org.springframework.cloud.dataflow.common.flyway.AbstractMigration;
import org.springframework.cloud.dataflow.server.db.migration.PostgreSQLTextToOID;

public class V10__ChangeTextTypes extends AbstractMigration {

	public V10__ChangeTextTypes() {
		super(null);
	}


	@Override
	public void migrate(Context context) throws Exception {
		PostgreSQLTextToOID.convertColumnFromOID("app_registration", "id", "uri", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumnFromOID("app_registration", "id", "metadata_uri", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumnFromOID("stream_definitions", "definition_name", "definition", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumnFromOID("stream_definitions", "definition_name", "original_definition", context.getConfiguration().getDataSource());
		PostgreSQLTextToOID.convertColumnFromOID("task_definitions", "definition_name", "definition", context.getConfiguration().getDataSource());
	}
}
