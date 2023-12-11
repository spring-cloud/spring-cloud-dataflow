/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.cloud.dataflow.server.db.migration;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import org.springframework.cloud.dataflow.server.db.ContainerSupport;


/**
 * Basic database schema and JPA tests for MS SQL Server.
 *
 * @author Corneil du Plessis
 */
public class SqlServer2019SmokeTest extends AbstractSmokeTest {

	@BeforeAll
	static void startContainer() {
		if (ContainerSupport.runningOnMacArm64()) {
			throw new RuntimeException("Unable to run SQLServer tests on Mac OS");
		}
		container = new MSSQLServerContainer<>(
			DockerImageName.parse(MSSQLServerContainer.IMAGE).withTag("2019-latest")
		).acceptLicense();
		container.start();
	}
}
