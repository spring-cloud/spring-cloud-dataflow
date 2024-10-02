package org.springframework.cloud.dataflow.integration.test.tasks;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.server.db.Oracle_XE_18_ContainerSupport;

@EnabledIfEnvironmentVariable(named = "ENABLE_ORACLE", matches = "true", disabledReason = "Container is too big")
@Tag("ORACLE")
class Oracle_TaskExecutionQueryIT extends AbstractLargeTaskExecutionDatabaseBase
		implements Oracle_XE_18_ContainerSupport {
	static {
		logger = LoggerFactory.getLogger(Oracle_TaskExecutionQueryIT.class);
	}
}
