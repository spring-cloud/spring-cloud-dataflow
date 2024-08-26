/*
 * Copyright 2021-2022 the original author or authors.
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
import org.springframework.cloud.dataflow.integration.test.tags.Mariadb;
import org.springframework.cloud.dataflow.integration.test.tags.TagNames;
import org.springframework.test.context.ActiveProfiles;

/**
 * Database tests for {@code mariadb 10.3} using shared db.
 */
@Mariadb
@DatabaseShared
@ActiveProfiles({TagNames.PROFILE_DB_SHARED})
public class MariadbSharedDbIT extends AbstractDatabaseTests {

	@Override
	protected String getDatabaseTag() {
		return TagNames.MARIADB_10_5;
	}
}
