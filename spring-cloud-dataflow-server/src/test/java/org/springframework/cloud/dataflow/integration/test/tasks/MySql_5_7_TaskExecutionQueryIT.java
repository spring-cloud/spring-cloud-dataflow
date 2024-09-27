package org.springframework.cloud.dataflow.integration.test.tasks;

import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.server.db.MySQL_5_7_ContainerSupport;

class MySql_5_7_TaskExecutionQueryIT extends AbstractLargeTaskExecutionDatabaseBase
		implements MySQL_5_7_ContainerSupport {
	static {
		logger = LoggerFactory.getLogger(MySql_5_7_TaskExecutionQueryIT.class);
	}
}
