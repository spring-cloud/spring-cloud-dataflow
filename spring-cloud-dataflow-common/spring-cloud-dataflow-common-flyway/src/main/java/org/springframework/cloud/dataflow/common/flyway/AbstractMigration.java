/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.cloud.dataflow.common.flyway;

import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Base implementation providing some shared features for java based migrations.
 *
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractMigration extends BaseJavaMigration {

	private final List<SqlCommand> commands;
	private final SqlCommandsRunner runner = new SqlCommandsRunner();

	/**
	 * Instantiates a new abstract migration.
	 *
	 * @param commands the commands
	 */
	public AbstractMigration(List<SqlCommand> commands) {
		super();
		this.commands = commands;
	}

	@Override
	public void migrate(Context context) throws Exception {
		runner.execute(context.getConnection(), getCommands());
	}

	/**
	 * Gets the commands.
	 *
	 * @return the commands
	 */
	public List<SqlCommand> getCommands() {
		return commands;
	}
}
