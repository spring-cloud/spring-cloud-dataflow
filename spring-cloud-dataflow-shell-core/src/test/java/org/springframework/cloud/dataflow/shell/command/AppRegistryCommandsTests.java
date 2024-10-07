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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
class AppRegistryCommandsTests extends AbstractShellIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(AppRegistryCommandsTests.class);

	private AppRegistryService registry;
	private ShellCommandRunner commandRunner;
	private List<AppRegistration> registeredApps;

	@BeforeEach
	void prepareForTest() {
		registeredApps = new ArrayList<>();
		registry = applicationContext.getBean(AppRegistryService.class);
		commandRunner = commandRunner().withValidateCommandSuccess();
	}

	@AfterEach
	void unregisterApps() {
		registeredApps.forEach(this::safeDeleteAppRegistration);
	}

	private void safeDeleteAppRegistration(AppRegistration registration) {
		try {
			registry.delete(registration.getName(), registration.getType(), registration.getVersion());
		} catch (Exception ex) {
			logger.error("Failed to delete app registration: " + registration, ex);
		}
	}

	private AppRegistration registerTimestampTask(String name, String timestampArtifactVersion, boolean force) {
		String commandTemplate = "app register --type task --name %s %s --uri maven://org.springframework.cloud.task.app:task-timestamp:%s";
		String command = String.format(commandTemplate, name, (force ? "--force" : ""), timestampArtifactVersion);
		logger.info("COMMAND -> {}", command);
		Object result = this.commandRunner.executeCommand(command);
		logger.info("RESULT <- {}", result);
		assertThat(registry.appExist(name, ApplicationType.task, timestampArtifactVersion)).isTrue();
		AppRegistration registration = registry.find(name, ApplicationType.task, timestampArtifactVersion);
		registeredApps.add(registration);
		return registration;
	}

	@Nested
	class AppRegisterTests {
		@Test
		void taskAppNoBootVersion() {
			AppRegistration registration = registerTimestampTask("timestamp1", "3.2.0",  false);
			assertThat(registration.getVersion()).isEqualTo("3.2.0");
		}

	}
}
