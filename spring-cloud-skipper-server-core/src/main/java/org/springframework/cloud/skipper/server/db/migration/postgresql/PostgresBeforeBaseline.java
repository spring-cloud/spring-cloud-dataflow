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
package org.springframework.cloud.skipper.server.db.migration.postgresql;

import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

import org.springframework.cloud.skipper.server.db.migration.AbstractBaselineCallback;

/**
 * Baselining schema setup for {@code postgres}.
 *
 * @author Janne Valkealahti
 *
 */
public class PostgresBeforeBaseline extends AbstractBaselineCallback {

	/**
	 * Instantiates a new postgres before baseline.
	 */
	public PostgresBeforeBaseline() {
		super(new V1__Initial_Setup());
	}

	@Override
	public boolean canHandleInTransaction(Event event, Context context) {
		// postgresql is one database where a single any error, even query error,
		// results transaction to get aborted in a connection, so tell
		// flyway not to use it. i.e. we check if some tables exists and this
		// will result errors if table to check is not there.
		return false;
	}
}
