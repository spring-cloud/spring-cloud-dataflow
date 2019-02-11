/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Base implementation for baselining schema setup.
 *
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractBaselineCallback extends AbstractCallback {

	private static final Logger logger = LoggerFactory.getLogger(AbstractBaselineCallback.class);
	private final AbstractInitialSetupMigration initialSetupMigration;

	/**
	 * Instantiates a new abstract baseline callback.
	 *
	 * @param initialSetupMigration the initial setup migration
	 */
	public AbstractBaselineCallback(AbstractInitialSetupMigration initialSetupMigration) {
		super(Event.BEFORE_BASELINE);
		this.initialSetupMigration = initialSetupMigration;
	}

	@Override
	public List<SqlCommand> getCommands(Event event, Context context) {
		List<SqlCommand> commands = new ArrayList<>();
		List<SqlCommand> defaultCommands = super.getCommands(event, context);
		if (defaultCommands != null) {
			commands.addAll(defaultCommands);
		}

		boolean migrateToInitial = true;
		try {
			JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
			jdbcTemplate.execute("select 1 from APP_REGISTRATION");
			migrateToInitial = false;
		} catch (Exception e) {
		}

		if (migrateToInitial) {
			logger.info("Did not detect prior Data Flow schema, doing baseline.");
			commands.addAll(initialSetupMigration.getCommands());
		}
		else {
			logger.info("Detected old Data Flow schema, doing baseline.");
			commands.addAll(dropIndexes());
			commands.addAll(changeAppRegistrationTable());
			commands.addAll(changeUriRegistryTable());
			commands.addAll(changeStreamDefinitionsTable());
			commands.addAll(changeTaskDefinitionsTable());
			commands.addAll(changeAuditRecordsTable());
			commands.addAll(createTaskLockTable());
			commands.addAll(createTaskDeploymentTable());
			commands.addAll(createIndexes());
		}

		return commands;
	}

	/**
	 * Drop indexes.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> dropIndexes();

	/**
	 * Change app registration table.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> changeAppRegistrationTable();

	/**
	 * Change uri registry table.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> changeUriRegistryTable();

	/**
	 * Change stream definitions table.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> changeStreamDefinitionsTable();

	/**
	 * Change task definitions table.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> changeTaskDefinitionsTable();

	/**
	 * Change audit records table.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> changeAuditRecordsTable();

	/**
	 * Creates the indexes.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createIndexes();

	/**
	 * Creates the task lock table.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createTaskLockTable();

	/**
	 * Create the task deployment table.
	 *
	 * @return the list of sql commands
	 */
	public abstract List<SqlCommand> createTaskDeploymentTable();
}
