package org.springframework.cloud.dataflow.server.db.migration;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.OracleContainer;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


public class OracleSmokeTest extends AbstractSmokeTest {
	private static final OracleContainer container = new OracleContainer();

	@BeforeAll
	static void startContainer() {
		container.start();
	}

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", container::getJdbcUrl);
		registry.add("spring.datasource.username", container::getUsername);
		registry.add("spring.datasource.password", container::getPassword);
		registry.add("spring.datasource.driver-class-name", container::getDriverClassName);
	}

}
