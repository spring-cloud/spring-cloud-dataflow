package org.springframework.cloud.dataflow.integration.test.tasks;

import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.server.db.MariaDB_11_ContainerSupport;

class MariaDB_11_TaskExecutionQueryIT extends AbstractLargeTaskExecutionDatabaseBase
		implements MariaDB_11_ContainerSupport {
	static {
		logger = LoggerFactory.getLogger(MariaDB_11_TaskExecutionQueryIT.class);
	}
}
