/*
 * Copyright 2023-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.server.batch;

import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.cloud.dataflow.core.database.support.DatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@JdbcTest(properties = {"spring.jpa.hibernate.ddl-auto=none",
		"spring.test.context.cache.maxSize=2",
		"spring.datasource.hikari.maximum-pool-size=4",
		"spring.jpa.database-platform=org.hibernate.dialect.MariaDB106Dialect"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = SimpleJobServiceMariadbTests.SimpleJobTestMariaDBConfiguration.class)
@Testcontainers
class SimpleJobServiceMariadbTests extends AbstractSimpleJobServiceTests {

	@Container
	private static final MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>("mariadb:10.6")
		.withCommand("--max-connections=500");

	@BeforeEach
	void setup() throws Exception {
		super.prepareForTest(mariaDBContainer, "mariadb", determineDatabaseType(DatabaseType.MARIADB));
	}

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mariaDBContainer::getJdbcUrl);
		registry.add("spring.datasource.username", mariaDBContainer::getUsername);
		registry.add("spring.datasource.password", mariaDBContainer::getPassword);
		registry.add("spring.datasource.driver-class-name", mariaDBContainer::getDriverClassName);
	}

	@SpringBootConfiguration
	public static class SimpleJobTestMariaDBConfiguration extends SimpleJobTestConfiguration {

	}

}
