/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.AppBootSchemaVersion;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;
import org.springframework.cloud.dataflow.shell.ShellCommandRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AppRegistryCommands}.
 *
 * @author Chris Bono
 * @author Corneil du Plessis
 */
public class AppRegistryCommandsTests extends AbstractShellIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(AppRegistryCommandsTests.class);

	private AppRegistryService registry;
	private ShellCommandRunner commandRunner;
	private List<AppRegistration> registeredApps;

	@Before
	public void prepareForTest() {
		registeredApps = new ArrayList<>();
		registry = applicationContext.getBean(AppRegistryService.class);
		commandRunner = commandRunner().withValidateCommandSuccess();
	}

	@After
	public void unregisterApps() {
		registeredApps.forEach(this::safeDeleteAppRegistration);
	}

	private void safeDeleteAppRegistration(AppRegistration registration) {
		try {
			registry.delete(registration.getName(), registration.getType(), registration.getVersion());
		} catch (Exception ex) {
			logger.error("Failed to delete app registration: " + registration, ex);
		}
	}

	@Test
	public void testRegisterTaskAppNoBootVersion() {
		AppRegistration registration = registerTimestampTask("timestamp", "3.2.0", "", false);
		assertThat(registration.getVersion()).isEqualTo("3.2.0");
		assertThat(registration.getBootVersion()).isEqualTo(AppBootSchemaVersion.defaultVersion());
	}

	@Test
	public void testRegisterTaskAppBootVersion2() {
		AppRegistration registration = registerTimestampTask("timestamp2", "3.2.0", "--bootVersion 2", false);
		assertThat(registration.getVersion()).isEqualTo("3.2.0");
		assertThat(registration.getBootVersion()).isEqualTo(AppBootSchemaVersion.BOOT2);
	}

	@Test
	public void testRegisterTaskAppBootVersion3() {
		AppRegistration registration = registerTimestampTask("timestamp3", "3.2.1", "-b 3", false);
		assertThat(registration.getVersion()).isEqualTo("3.2.1");
		assertThat(registration.getBootVersion()).isEqualTo(AppBootSchemaVersion.BOOT3);
	}

	@Test
	public void testRegisterTaskUpdateBootVersion3() {
		AppRegistration registration = registerTimestampTask("timestamp2to3", "3.2.0", "--bootVersion 2", false);
		assertThat(registration.getVersion()).isEqualTo("3.2.0");
		assertThat(registration.getBootVersion()).isEqualTo(AppBootSchemaVersion.BOOT2);
		// The 'force=true' signals to udpate the existing 'timestamp2to3' app
		registration = registerTimestampTask("timestamp2to3", "3.2.1", "-b 3", true);
		assertThat(registration.getVersion()).isEqualTo("3.2.1");
		assertThat(registration.getBootVersion()).isEqualTo(AppBootSchemaVersion.BOOT3);
	}

	private AppRegistration registerTimestampTask(String name, String timestampArtifactVersion, String bootVersionOption, boolean force) {
		String commandTemplate = "app register --type task --name %s %s %s --uri maven://org.springframework.cloud.task.app:task-timestamp:%s";
		String command = String.format(commandTemplate, name, bootVersionOption, (force ? "--force" : ""), timestampArtifactVersion);
		logger.info("COMMAND -> {}", command);
		Object result = this.commandRunner.executeCommand(command);
		logger.info("RESULT <- {}", result);
		assertThat(registry.appExist(name, ApplicationType.task, timestampArtifactVersion)).isTrue();
		AppRegistration registration = registry.find(name, ApplicationType.task, timestampArtifactVersion);
		registeredApps.add(registration);
		return registration;
	}
}
