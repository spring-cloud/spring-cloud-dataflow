/*
 * Copyright 2021-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.integration.test.db;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;

import org.springframework.cloud.dataflow.integration.test.tags.Database;
import org.springframework.cloud.dataflow.integration.test.tags.DatabaseFailure;
import org.springframework.cloud.dataflow.integration.test.tags.DatabaseSlow;
import org.springframework.cloud.dataflow.integration.test.tags.DataflowAll;
import org.springframework.cloud.dataflow.integration.test.tags.DataflowMain;
import org.springframework.cloud.dataflow.integration.test.tags.TagNames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Database
public abstract class AbstractPostgresDatabaseTests extends AbstractDatabaseTests {

	private final Logger log = LoggerFactory.getLogger(AbstractPostgresDatabaseTests.class);

	@Test
	@DataflowMain
	@DatabaseFailure
	public void testMigrationError() {
		log.info("Running testMigrationError()");
		this.dataflowCluster.startSkipperDatabase(getDatabaseTag());
		this.dataflowCluster.startDataflowDatabase(getDatabaseTag());

		// break db
		runExecute(getTestMigrationErrorBreakClause());

		this.dataflowCluster.startSkipper(TagNames.SKIPPER_main);
		assertSkipperServerRunning(this.dataflowCluster);

		assertThatThrownBy(() -> {
			this.dataflowCluster.startDataflow(TagNames.DATAFLOW_main);
		}).isInstanceOf(ContainerLaunchException.class);

		Integer count = runCountQuery("select count(*) from flyway_schema_history_dataflow");
		assertThat(count).isEqualTo(-1);

		// fix db
		runExecute(getTestMigrationErrorFixClause());

		this.dataflowCluster.startDataflow(TagNames.DATAFLOW_main);
		assertDataflowServerRunning(this.dataflowCluster);
		count = runCountQuery("select count(*) from flyway_schema_history_dataflow");
		assertThat(count).isGreaterThan(1);
	}

	protected abstract String getTestMigrationErrorBreakClause();
	protected abstract String getTestMigrationErrorFixClause();

	@Test
	@DataflowAll
	@DatabaseSlow
	public void testFullMigrationFlow() {
		super.testFullMigrationFlow();
	}
}
