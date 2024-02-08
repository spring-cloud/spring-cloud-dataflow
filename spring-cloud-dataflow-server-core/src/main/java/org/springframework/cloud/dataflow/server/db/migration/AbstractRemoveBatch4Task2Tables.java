/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.cloud.dataflow.server.db.migration;

import org.springframework.cloud.dataflow.common.flyway.AbstractMigration;
import org.springframework.cloud.dataflow.common.flyway.SqlCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation for removing Task v2 and Batch v4 schema.
 * Also removing the BOOT3_ prefix from Batch v5 and Task v3 tables.
 *
 * @author Glenn Renfro
 */
public abstract class AbstractRemoveBatch4Task2Tables extends AbstractMigration {
	public AbstractRemoveBatch4Task2Tables() {
		super(null);
	}

	@Override
	public List<SqlCommand> getCommands() {
		List<SqlCommand> commands = new ArrayList<>();
		commands.addAll(dropBoot3Boot2Views());
		commands.addAll(renameTask2Tables());
		commands.addAll(renameBatch4Tables());
		commands.addAll(renameTask3Tables());
		commands.addAll(renameBatch5Tables());
		return commands;
	}

	/**
	 * Renames the spring-cloud-task V3 tables removing the BOOT3_ prefix.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> renameTask3Tables();

	/**
	 * Renames the spring batch V5 tables removing the BOOT3_ prefix.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> renameBatch5Tables();

	/**
	 * Renames the spring-cloud-task V2 tables adding a V2_ prefix.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> renameTask2Tables();

	/**
	 * Renames the spring batch V4 tables adding a V2_ prefix.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> renameBatch4Tables();

	/**
	 * Removes views for TaskV2/BatchV4 TaskV3/BatchV5 views.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> dropBoot3Boot2Views();

}
