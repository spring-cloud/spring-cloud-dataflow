package org.springframework.cloud.dataflow.integration.test.tasks;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.server.db.DB2_11_5_ContainerSupport;

@EnabledIfEnvironmentVariable(named = "ENABLE_DB2", matches = "true", disabledReason = "Container is too big")
@Tag("DB2")
class DB2_TaskExecutionQueryIT extends AbstractLargeTaskExecutionDatabaseBase implements DB2_11_5_ContainerSupport {
	static {
		logger = LoggerFactory.getLogger(DB2_TaskExecutionQueryIT.class);
	}
}
