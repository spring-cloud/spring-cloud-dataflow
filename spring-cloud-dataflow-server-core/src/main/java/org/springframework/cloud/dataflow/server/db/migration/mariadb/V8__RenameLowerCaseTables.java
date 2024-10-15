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
package org.springframework.cloud.dataflow.server.db.migration.mariadb;

import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.dataflow.common.flyway.SqlCommand;
import org.springframework.cloud.dataflow.server.db.migration.AbstractCaseSensitiveMigration;

/**
 * Since MariaDB operates in a case-sensitive mode for table and column names we need TASK_ tables referenced with a prefix to be uppercase.
 *
 * @author Corneil du Plessis
 */
public class V8__RenameLowerCaseTables extends AbstractCaseSensitiveMigration {


	@Override
	public List<SqlCommand> getCommands() {
		return Arrays.asList(
				SqlCommand.from(RENAME_TASK_EXECUTION_METADATA_LC),
				SqlCommand.from(RENAME_TASK_EXECUTION_METADATA),
				SqlCommand.from(RENAME_TASK_EXECUTION_METADATA_SEQ_LC_TBL),
				SqlCommand.from(RENAME_TASK_EXECUTION_METADATA_SEQ_TBL)
		);
	}
}
