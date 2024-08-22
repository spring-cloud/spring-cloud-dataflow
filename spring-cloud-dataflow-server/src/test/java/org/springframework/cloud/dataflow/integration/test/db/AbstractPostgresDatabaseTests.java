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

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.integration.test.tags.Database;
import org.springframework.cloud.dataflow.integration.test.tags.DatabaseFailure;
import org.springframework.cloud.dataflow.integration.test.tags.DatabaseSlow;
import org.springframework.cloud.dataflow.integration.test.tags.DataflowAll;
import org.springframework.cloud.dataflow.integration.test.tags.DataflowMain;
import org.springframework.cloud.dataflow.integration.test.tags.TagNames;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.support.jackson.Jackson2DataflowModule;

import static org.assertj.core.api.Assertions.assertThat;

@Database
public abstract class AbstractPostgresDatabaseTests extends AbstractDatabaseTests {

	private final Logger log = LoggerFactory.getLogger(AbstractPostgresDatabaseTests.class);

	@Test
	@DataflowMain
	@DatabaseFailure
	public void migrationError() {
		log.info("Running testMigrationError()");
		this.dataflowCluster.startSkipperDatabase(getDatabaseTag());
		this.dataflowCluster.startDataflowDatabase(getDatabaseTag());

		// break db
		runExecute(getTestMigrationErrorBreakClause());

		this.dataflowCluster.startSkipper(TagNames.SKIPPER_main);
		assertSkipperServerRunning(this.dataflowCluster);

		this.dataflowCluster.startDataflow(TagNames.DATAFLOW_main);

		assertDataflowServerNotRunning(this.dataflowCluster);
		try {
			this.dataflowCluster.stopDataflow();
		} catch (Throwable x) {
			// Ignore
		}
		Integer count = runCountQuery("select count(*) from flyway_schema_history_dataflow");
		assertThat(count).isEqualTo(-1);

		// fix db
		runExecute(getTestMigrationErrorFixClause());

		this.dataflowCluster.startDataflow(TagNames.DATAFLOW_main);
		assertDataflowServerRunning(this.dataflowCluster);
		count = runCountQuery("select count(*) from flyway_schema_history_dataflow");
		assertThat(count).isGreaterThan(1);
	}

	@SuppressWarnings("deprecation")
	@Test
	@DataflowMain
	public void migration210211() throws URISyntaxException {
		log.info("Running testMigrationError()");
		this.dataflowCluster.startSkipperDatabase(getDatabaseTag());
		this.dataflowCluster.startDataflowDatabase(getDatabaseTag());

		this.dataflowCluster.startSkipper(TagNames.SKIPPER_2_9);
		assertSkipperServerRunning(this.dataflowCluster);

		this.dataflowCluster.startDataflow(TagNames.DATAFLOW_2_10);
		assertDataflowServerRunning(this.dataflowCluster);

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new Jackson2DataflowModule());
		final DataFlowTemplate dataFlowTemplate = new DataFlowTemplate(new URI(dataflowCluster.getDataflowUrl()), objectMapper);
		dataFlowTemplate.appRegistryOperations().register("time", ApplicationType.source, "docker:springcloudstream/time-source-rabbit:5.0.0", null, false);
		dataFlowTemplate.appRegistryOperations().register("log", ApplicationType.sink, "docker:springcloudstream/log-sink-rabbit:5.0.0", null, false);
		dataFlowTemplate.streamOperations().createStream("timelogger", "time | log", "timelogger", false);

		StreamDefinitionResource timelogger = dataFlowTemplate.streamOperations().getStreamDefinition("timelogger");
		assertThat(timelogger.getDslText()).isEqualTo("time | log");
		assertThat(timelogger.getDescription()).isEqualTo("timelogger");

		this.dataflowCluster.replaceSkipperAndDataflow(TagNames.SKIPPER_main, TagNames.DATAFLOW_main);
//		this.dataflowCluster.replaceSkipper(TagNames.SKIPPER_main);
		assertSkipperServerRunning(this.dataflowCluster);
//		this.dataflowCluster.replaceDataflow(TagNames.DATAFLOW_main);
		assertDataflowServerRunning(this.dataflowCluster);

		Integer count = runCountQuery("select count(*) from flyway_schema_history_dataflow");
		assertThat(count).isGreaterThan(1);
		DataFlowTemplate dataFlowTemplate2 = new DataFlowTemplate(new URI(dataflowCluster.getDataflowUrl()), objectMapper);
		timelogger = dataFlowTemplate2.streamOperations().getStreamDefinition("timelogger");
		assertThat(timelogger.getDslText()).isEqualTo("time | log");
		assertThat(timelogger.getDescription()).isEqualTo("timelogger");
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
