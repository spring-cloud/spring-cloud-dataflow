package org.springframework.cloud.dataflow.integration.test.tasks;

import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.server.db.SqlServer_2022_ContainerSupport;

class SqlServer_2022_TaskExecutionQueryIT extends AbstractLargeTaskExecutionDatabaseBase
		implements SqlServer_2022_ContainerSupport {
	static {
		logger = LoggerFactory.getLogger(SqlServer_2022_TaskExecutionQueryIT.class);
	}
}
