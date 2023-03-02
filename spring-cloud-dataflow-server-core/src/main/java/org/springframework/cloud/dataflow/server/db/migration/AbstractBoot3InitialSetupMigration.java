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
package org.springframework.cloud.dataflow.server.db.migration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.dataflow.common.flyway.AbstractMigration;
import org.springframework.cloud.dataflow.common.flyway.SqlCommand;

/**
 * Base implementation for initial Boot 3 schema
 *
 * @author Chris Bono
 */
public abstract class AbstractBoot3InitialSetupMigration extends AbstractMigration {

	public AbstractBoot3InitialSetupMigration() {
		super(null);
	}

	@Override
	public List<SqlCommand> getCommands() {
		List<SqlCommand> commands = new ArrayList<>();
		commands.addAll(createTask3Tables());
		commands.addAll(createBatch5Tables());
		return commands;
	}

	/**
	 * Creates the spring-cloud-task V3 tables.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createTask3Tables();

	/**
	 * Creates the spring-batch V5 tables.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createBatch5Tables();
}
