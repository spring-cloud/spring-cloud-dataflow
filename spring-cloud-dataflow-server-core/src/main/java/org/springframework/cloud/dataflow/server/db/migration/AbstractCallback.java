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

import java.util.List;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.util.ObjectUtils;

/**
 * Base implementation providing some shared features for java based callbacks.
 *
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractCallback implements Callback {

	private final Event event;
	private final List<SqlCommand> commands;
	private final SqlCommandsRunner runner = new SqlCommandsRunner();

	/**
	 * Instantiates a new abstract callback.
	 *
	 * @param event the event to hook into
	 */
	public AbstractCallback(Event event) {
		this(event, null);
	}

	/**
	 * Instantiates a new abstract callback.
	 *
	 * @param event the event to hook into
	 * @param commands the sql commands to run
	 */
	public AbstractCallback(Event event, List<SqlCommand> commands) {
		this.event = event;
		this.commands = commands;
	}

	@Override
	public boolean supports(Event event, Context context) {
		return ObjectUtils.nullSafeEquals(this.event, event);
	}

	@Override
	public boolean canHandleInTransaction(Event event, Context context) {
		return true;
	}

	@Override
	public void handle(Event event, Context context) {
		try {
			runner.execute(context.getConnection(), getCommands(event, context));
		}
		catch(Exception sqe) {
			if (sqe instanceof BadSqlGrammarException) {
				throw new DataFlowSchemaMigrationException(
						"An exception occured during migration.  This may indicate " +
								"that you have run Spring Batch Jobs or Spring Cloud " +
								"Tasks prior to running Spring Cloud Data Flow first. " +
								"Data Flow must create these tables.", sqe);

			}
			throw sqe;
		}
	}

	/**
	 * Gets the commands.
	 *
	 * @param event the event
	 * @param context the context
	 * @return the commands
	 */
	public List<SqlCommand> getCommands(Event event, Context context) {
		return commands;
	}
}
