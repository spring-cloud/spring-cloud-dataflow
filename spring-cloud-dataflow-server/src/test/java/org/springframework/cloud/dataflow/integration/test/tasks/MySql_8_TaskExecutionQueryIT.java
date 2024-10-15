package org.springframework.cloud.dataflow.integration.test.tasks;

import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.server.db.MySQL_8_ContainerSupport;

class MySql_8_TaskExecutionQueryIT extends AbstractLargeTaskExecutionDatabaseBase
		implements MySQL_8_ContainerSupport {
	static {
		logger = LoggerFactory.getLogger(MySql_8_TaskExecutionQueryIT.class);
	}
}
