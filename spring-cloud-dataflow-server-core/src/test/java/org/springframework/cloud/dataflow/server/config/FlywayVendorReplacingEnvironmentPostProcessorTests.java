/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.config;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.dataflow.common.flyway.FlywayVendorReplacingEnvironmentPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lightweight integration test that uses {@link Testcontainers} to start a MariaDB server
 * in order to verify the {@link FlywayVendorReplacingEnvironmentPostProcessor} properly
 * handles the '{vendor} token.
 *
 * @author Chris Bono
 */
@Testcontainers(disabledWithoutDocker = true)
public class FlywayVendorReplacingEnvironmentPostProcessorTests {

	@Container
	private static final MariaDBContainer<?> MARIADB_CONTAINER = new MariaDBContainer<>("mariadb:10.4")
			.withDatabaseName("dataflow")
			.withUsername("spring")
			.withPassword("spring");

	@Test
	void vendorIsReplacedInFlywayLocationsWhenMysqlInUrl() {
		String jdbcUrl = String.format("jdbc:mysql://%s:%d/dataflow?permitMysqlScheme", MARIADB_CONTAINER.getHost(), MARIADB_CONTAINER.getMappedPort(3306));
		try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultTestApplication.class,
				"--server.port=0",
				"--spring.datasource.url=" + jdbcUrl,
				"--spring.datasource.username=spring",
				"--spring.datasource.password=spring",
				"--spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
				"--spring.main.allow-bean-definition-overriding=true",
				"--spring.autoconfigure.exclude=org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration,org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration")) {
			assertThat(ctx.getEnvironment().getProperty("spring.flyway.locations[0]"))
					.isEqualTo("classpath:org/springframework/cloud/dataflow/server/db/migration/mysql");
		}
	}

	@Test
	void vendorIsNotReplacedInFlywayLocationsWhenMysqlNotInUrl() {
		try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultTestApplication.class,
				"--server.port=0",
				"--spring.datasource.url=" + MARIADB_CONTAINER.getJdbcUrl(),
				"--spring.datasource.username=spring",
				"--spring.datasource.password=spring",
				"--spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
				"--spring.main.allow-bean-definition-overriding=true",
				"--spring.autoconfigure.exclude=org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration,org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration")) {
			assertThat(ctx.getEnvironment().getProperty("spring.flyway.locations[0]"))
					.isEqualTo("classpath:org/springframework/cloud/dataflow/server/db/migration/{vendor}");
		}
	}
}
