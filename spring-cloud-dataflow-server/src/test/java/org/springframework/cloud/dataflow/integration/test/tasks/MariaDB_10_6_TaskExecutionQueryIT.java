package org.springframework.cloud.dataflow.integration.test.tasks;

import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.server.db.MariaDB_10_6_ContainerSupport;

class MariaDB_10_6_TaskExecutionQueryIT extends AbstractLargeTaskExecutionDatabaseBase
		implements MariaDB_10_6_ContainerSupport {
	static {
		logger = LoggerFactory.getLogger(MariaDB_10_6_TaskExecutionQueryIT.class);
	}
}
