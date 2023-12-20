/*
 * Copyright 2023-2023 the original author or authors.
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
package org.springframework.cloud.dataflow.server.db.arm64;

import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.MSSQLServerContainer;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import org.springframework.cloud.dataflow.server.db.ContainerSupport;
import org.springframework.core.log.LogAccessor;

/**
 * Provides support for testing against an {@link MSSQLServerContainer MSSQL testcontainer} on Mac ARM64.
 *
 * @author Chris Bono
 */
@ExtendWith(SystemStubsExtension.class)
public interface SqlServerArm64ContainerSupport {

	LogAccessor LOG = new LogAccessor(SqlServerArm64ContainerSupport.class);

	@SystemStub
	EnvironmentVariables ENV_VARS = new EnvironmentVariables();

	static MSSQLServerContainer startContainer(Supplier<MSSQLServerContainer> mssqlContainerSupplier) {
		if (ContainerSupport.runningOnMacArm64()) {
			String wiki = "https://github.com/spring-cloud/spring-cloud-dataflow/wiki/MSSQL-on-Mac-ARM64";
			LOG.warn(() -> "You are running on Mac ARM64. If this test fails, make sure Colima is running prior " +
					"to test invocation. See " + wiki + " for details");
			ENV_VARS.set("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock");
			ENV_VARS.set("DOCKER_HOST", String.format("unix://%s/.colima/docker.sock", System.getProperty("user.home")));
		}
		MSSQLServerContainer mssqlContainer = mssqlContainerSupplier.get();
		mssqlContainer.start();
		return mssqlContainer;
	}
}
