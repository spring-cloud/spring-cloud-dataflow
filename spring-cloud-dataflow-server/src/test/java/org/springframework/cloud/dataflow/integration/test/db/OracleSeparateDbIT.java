/*
 * Copyright 2022 the original author or authors.
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

import org.springframework.cloud.dataflow.integration.test.tags.DatabaseSeparate;
import org.springframework.cloud.dataflow.integration.test.tags.Oracle;
import org.springframework.cloud.dataflow.integration.test.tags.TagNames;
import org.springframework.test.context.ActiveProfiles;

/**
 * Database tests for {@code mssql} using separate db's.
 */
@Oracle
@DatabaseSeparate
@ActiveProfiles({TagNames.PROFILE_DB_SEPARATE})
public class OracleSeparateDbIT extends AbstractDatabaseTests {

	@Override
	protected String getDatabaseTag() {
		return TagNames.ORACLE_XE_18;
	}
}
