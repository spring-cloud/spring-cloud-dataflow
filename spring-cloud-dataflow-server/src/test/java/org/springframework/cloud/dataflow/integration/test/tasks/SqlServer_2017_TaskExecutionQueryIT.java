package org.springframework.cloud.dataflow.integration.test.tasks;

import org.junit.jupiter.api.Disabled;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.server.db.SqlServer_2017_ContainerSupport;

@Disabled("See https://github.com/spring-cloud/spring-cloud-dataflow/issues/5952")
class SqlServer_2017_TaskExecutionQueryIT extends AbstractLargeTaskExecutionDatabaseBase implements SqlServer_2017_ContainerSupport {
	static {
		logger = LoggerFactory.getLogger(SqlServer_2017_TaskExecutionQueryIT.class);
	}
}
