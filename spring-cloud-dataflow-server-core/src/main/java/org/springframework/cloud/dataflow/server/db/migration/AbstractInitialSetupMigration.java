/*
 * Copyright 2019 the original author or authors.
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

/**
 * Base implementation for initial schema setup.
 *
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractInitialSetupMigration extends AbstractMigration {

	/**
	 * Instantiates a new abstract initial setup migration.
	 *
	 * @param commands the default commands
	 */
	public AbstractInitialSetupMigration(List<SqlCommand> commands) {
		super(commands);
	}

	@Override
	public List<SqlCommand> getCommands() {
		List<SqlCommand> commands = new ArrayList<>();
		List<SqlCommand> defaultCommands = super.getCommands();
		if (defaultCommands != null) {
			commands.addAll(defaultCommands);
		}
		commands.addAll(createHibernateSequence());
		commands.addAll(createAppRegistrationTable());
		commands.addAll(createTaskDeploymentTable());
		commands.addAll(createAuditRecordsTable());
		commands.addAll(createStreamDefinitionsTable());
		commands.addAll(createTaskDefinitionsTable());
		commands.addAll(createTaskTables());
		commands.addAll(createBatchTables());
		return commands;
	}

	/**
	 * Creates the hibernate sequence.
	 *
	 * @return the sql command
	 */
	public abstract List<SqlCommand> createHibernateSequence();

	/**
	 * Creates the app registration table.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createAppRegistrationTable();

	/**
	 * Creates the task deployment table.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createTaskDeploymentTable();

	/**
	 * Creates the audit records table.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createAuditRecordsTable();

	/**
	 * Creates the stream definitions table.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createStreamDefinitionsTable();

	/**
	 * Creates the task definitions table.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createTaskDefinitionsTable();

	/**
	 * Creates the task tables.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createTaskTables();

	/**
	 * Creates the batch tables.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createBatchTables();
}
