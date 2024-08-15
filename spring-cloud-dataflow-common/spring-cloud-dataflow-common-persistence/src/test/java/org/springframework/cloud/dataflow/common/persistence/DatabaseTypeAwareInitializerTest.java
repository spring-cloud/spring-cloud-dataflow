package org.springframework.cloud.dataflow.common.persistence;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.dataflow.common.persistence.type.DatabaseTypeAwareInitializer;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
public class DatabaseTypeAwareInitializerTest {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseTypeAwareInitializerTest.class);
	@Test
	public void testInitPostgres() {
		initDriverType("org.postgresql.Driver");
		assertThat(DatabaseTypeAwareInitializer.getPostgresDatabase()).isNotNull();
		assertThat(DatabaseTypeAwareInitializer.getPostgresDatabase()).isTrue();
	}
	@Test
	public void testInitMariaDB() {
		initDriverType("org.mariadb.jdbc.Driver");
		assertThat(DatabaseTypeAwareInitializer.getPostgresDatabase()).isNotNull();
		assertThat(DatabaseTypeAwareInitializer.getPostgresDatabase()).isFalse();
	}
	private void initDriverType(String driverClassName) {
			// Prime an actual env by running it through the AppContextRunner with the configured properties
			new ApplicationContextRunner().withPropertyValues("spring.datasource.driver-class-name=" + driverClassName).run((context) -> {
				ConfigurableEnvironment env = context.getEnvironment();
				logger.info("spring.datasource.driver-class-name={}", env.getProperty("spring.datasource.driver-class-name"));
				DatabaseTypeAwareInitializer initializer = new DatabaseTypeAwareInitializer();
				initializer.initialize(context);
			});
	}
}
