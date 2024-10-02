package org.springframework.cloud.dataflow.integration.test.tasks;

import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.server.db.PostgreSQL_14_ContainerSupport;

class PostgreSQL_TaskExecutionQueryIT extends AbstractLargeTaskExecutionDatabaseBase
		implements PostgreSQL_14_ContainerSupport {
	static {
		logger = LoggerFactory.getLogger(PostgreSQL_TaskExecutionQueryIT.class);
	}
}
