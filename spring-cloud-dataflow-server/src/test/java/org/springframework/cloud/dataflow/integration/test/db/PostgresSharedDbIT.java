/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.cloud.dataflow.integration.test.db;

import org.springframework.cloud.dataflow.integration.test.tags.DatabaseShared;
import org.springframework.cloud.dataflow.integration.test.tags.Postgres;
import org.springframework.cloud.dataflow.integration.test.tags.TagNames;
import org.springframework.test.context.ActiveProfiles;

/**
 * Database tests for {@code postgres 10} using shared db.
 */
@Postgres
@DatabaseShared
@ActiveProfiles({TagNames.PROFILE_DB_SHARED})
public class PostgresSharedDbIT extends AbstractPostgresDatabaseTests {

	@Override
	protected String getDatabaseTag() {
		return TagNames.POSTGRES_14;
	}

	@Override
	protected String getTestMigrationErrorBreakClause() {
		return "CREATE SEQUENCE TASK_SEQ MAXVALUE 9223372036854775807 NO CYCLE";
	}

	@Override
	protected String getTestMigrationErrorFixClause() {
		return "DROP SEQUENCE TASK_SEQ";
	}
}
