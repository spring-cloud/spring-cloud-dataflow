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

package org.springframework.cloud.dataflow.server.db;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import org.springframework.cloud.dataflow.server.db.arm64.SqlServerArm64ContainerSupport;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Provides support for running a {@link MSSQLServerContainer MSSQL 2022 Testcontainer}.
 *
 * @author Chris Bono
 */
public interface SqlServer_2022_ContainerSupport extends SqlServerArm64ContainerSupport {

	AtomicReference<MSSQLServerContainer> containerReference = new AtomicReference<>(null);

	@BeforeAll
	static void startContainer() {
		MSSQLServerContainer container = SqlServerArm64ContainerSupport.startContainer(() ->
				new MSSQLServerContainer(DockerImageName.parse(MSSQLServerContainer.IMAGE).withTag("2022-latest")).acceptLicense());
		containerReference.set(container);
	}

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		MSSQLServerContainer container = containerReference.get();
		registry.add("spring.datasource.url", container::getJdbcUrl);
		registry.add("spring.datasource.username", container::getUsername);
		registry.add("spring.datasource.password", container::getPassword);
		registry.add("spring.datasource.driver-class-name", container::getDriverClassName);
	}

}
