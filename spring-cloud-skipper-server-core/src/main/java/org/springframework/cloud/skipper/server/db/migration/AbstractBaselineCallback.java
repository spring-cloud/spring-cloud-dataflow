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
package org.springframework.cloud.skipper.server.db.migration;

import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		logger.debug("Baselining Skipper and creating initial schema.");
		commands.addAll(createHibernateSequence());
		commands.addAll(createSkipperTables());
		commands.addAll(createStateMachineTables());

		return commands;
	}

	public List<SqlCommand> createHibernateSequence() {
		return initialSetupMigration.createHibernateSequence();
	}

	public List<SqlCommand> createSkipperTables() {
		return initialSetupMigration.createSkipperTables();
	}

	public List<SqlCommand> createStateMachineTables() {
		return initialSetupMigration.createStateMachineTables();
	}

}
