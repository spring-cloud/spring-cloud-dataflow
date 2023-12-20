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
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import org.springframework.cloud.dataflow.server.db.arm64.OracleArm64ContainerSupport;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Provides support for running an {@link OracleContainer Oracle XE 18 Testcontainer}.
 *
 * @author Chris Bono
 */
public interface Oracle_XE_18_ContainerSupport extends OracleArm64ContainerSupport {

	AtomicReference<OracleContainer> containerReference = new AtomicReference<>(null);

	@BeforeAll
	static void startContainer() {
		OracleContainer oracleContainer = OracleArm64ContainerSupport.startContainer(() ->
				new OracleContainer(DockerImageName.parse("gvenzl/oracle-xe").withTag("18-slim-faststart")));
		containerReference.set(oracleContainer);
	}

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		OracleContainer oracleContainer = containerReference.get();
		registry.add("spring.datasource.url", oracleContainer::getJdbcUrl);
		registry.add("spring.datasource.username", oracleContainer::getUsername);
		registry.add("spring.datasource.password", oracleContainer::getPassword);
		registry.add("spring.datasource.driver-class-name", oracleContainer::getDriverClassName);
	}

}
