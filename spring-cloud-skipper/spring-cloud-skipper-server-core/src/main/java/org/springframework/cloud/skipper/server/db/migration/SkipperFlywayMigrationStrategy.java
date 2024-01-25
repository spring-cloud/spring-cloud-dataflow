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

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.CoreMigrationType;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

/**
 * Flyway {@link FlywayMigrationStrategy} bean customizing migration process.
 *
 * @author Janne Valkealahti
 *
 */
public class SkipperFlywayMigrationStrategy implements FlywayMigrationStrategy {

	private static final Logger logger = LoggerFactory.getLogger(SkipperFlywayMigrationStrategy.class);
	private final static MigrationVersion INITIAL = MigrationVersion.fromVersion("1");

	@Override
	public void migrate(Flyway flyway) {
		MigrationInfo current = flyway.info().current();
		if (current != null && current.getVersion().equals(INITIAL) && current.getType() == CoreMigrationType.SQL) {
			logger.info("Detected initial version based on SQL scripts, doing repair to switch to Java based migrations.");
			flyway.repair();
		}
		flyway.migrate();
	}
}
