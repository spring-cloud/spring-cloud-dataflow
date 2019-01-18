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

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;

/**
 * Base implementation providing some shared features for java based migrations.
 *
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractMigration extends BaseJavaMigration {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AbstractMigration.class);

	@Override
	public void migrate(Context context) throws Exception {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
		jdbcTemplate.setExceptionTranslator(getExceptionTranslator());
		try {
			executeInternal(jdbcTemplate);
		} catch (SuppressDataAccessException e) {
			logger.debug("Suppressing error {}", e);
		}
	}

	protected void executeInternal(JdbcTemplate jdbcTemplate) {
	}

	protected SQLErrorCodeSQLExceptionTranslator getExceptionTranslator() {
		return null;
	}
}
