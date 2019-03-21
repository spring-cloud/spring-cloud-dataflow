/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.features;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AbstractDatabaseInitializer;
import org.springframework.core.io.ResourceLoader;

/**
 * Add indexes to task and batch tables
 * @author Christian Tzolov
 */
public class BatchTaskIndexesDatabaseInitializer extends AbstractDatabaseInitializer {

	public BatchTaskIndexesDatabaseInitializer(DataSource dataSource, ResourceLoader resourceLoader) {
		super(dataSource, resourceLoader);
	}

	@Override
	protected boolean isEnabled() {
		String databaseName = this.getDatabaseName();
		return "mysql".equalsIgnoreCase(databaseName) || "postgresql".equalsIgnoreCase(databaseName);
	}

	@Override
	protected String getSchemaLocation() {
		return String.format("classpath:schemas/%s/batch_task_indexes.sql", this.getDatabaseName());
	}
}
