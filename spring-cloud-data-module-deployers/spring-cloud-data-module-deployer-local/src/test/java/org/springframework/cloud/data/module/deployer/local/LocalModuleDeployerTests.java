/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.module.deployer.local;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.cloud.data.core.ModuleCoordinates;
import org.springframework.cloud.data.core.ModuleDefinition;
import org.springframework.cloud.data.core.ModuleDeploymentRequest;

/**
 * Tests deployment of the time-source and log-sink modules.
 *
 * @author Mark Fisher
 */
public class LocalModuleDeployerTests {

	private static final String GROUP_ID = "org.springframework.cloud.stream.module";

	private static final String VERSION = "1.0.0.BUILD-SNAPSHOT";

	@Test @Ignore // see TODO below
	public void timeToLogStream() {
		LocalModuleDeployer deployer = new LocalModuleDeployer();
		ModuleDefinition timeDefinition = new ModuleDefinition.Builder()
				.setGroup("ticktock")
				.setName("time")
				.addBinding("output", "ticktock.0")
				.build();
		ModuleDefinition logDefinition = new ModuleDefinition.Builder()
				.setGroup("ticktock")
				.setName("log")
				.addBinding("input", "ticktock.0")
				.build();
		ModuleCoordinates timeCoordinates = new ModuleCoordinates.Builder()
				.setGroupId(GROUP_ID)
				.setArtifactId("time-source")
				.setVersion(VERSION)
				.setClassifier("exec")
				.build();
		ModuleCoordinates logCoordinates = new ModuleCoordinates.Builder()
				.setGroupId(GROUP_ID)
				.setArtifactId("log-sink")
				.setVersion(VERSION)
				.setClassifier("exec")
				.build();
		ModuleDeploymentRequest time = new ModuleDeploymentRequest(timeDefinition, timeCoordinates);
		ModuleDeploymentRequest log = new ModuleDeploymentRequest(logDefinition, logCoordinates);
		deployer.deploy(time);
		deployer.deploy(log);
		// TODO: check status, then undeploy
	}
}
