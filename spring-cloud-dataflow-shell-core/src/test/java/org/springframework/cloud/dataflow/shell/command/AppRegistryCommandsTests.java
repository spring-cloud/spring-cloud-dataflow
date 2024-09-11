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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersion;
import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;
import org.springframework.cloud.dataflow.shell.ShellCommandRunner;
import org.springframework.cloud.dataflow.shell.command.support.TablesInfo;

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

	@BeforeEach
	public void prepareForTest() {
		registeredApps = new ArrayList<>();
		registry = applicationContext.getBean(AppRegistryService.class);
		commandRunner = commandRunner().withValidateCommandSuccess();
	}

	@AfterEach
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

	private AppRegistration registerTimeSource(String name, String timeSourceArtifactVersion, String bootVersionOption, boolean force) {
		String commandTemplate = "app register --type source --name %s %s %s --uri maven://org.springframework.cloud.stream.app:time-source-kafka:%s";
		String command = String.format(commandTemplate, name, bootVersionOption, (force ? "--force" : ""), timeSourceArtifactVersion);
		logger.info("COMMAND -> {}", command);
		Object result = this.commandRunner.executeCommand(command);
		logger.info("RESULT <- {}", result);
		assertThat(registry.appExist(name, ApplicationType.source, timeSourceArtifactVersion)).isTrue();
		AppRegistration registration = registry.find(name, ApplicationType.source, timeSourceArtifactVersion);
		registeredApps.add(registration);
		return registration;
	}

	@Nested
	class AppRegisterTests {
		@Test
		void taskAppNoBootVersion() {
			AppRegistration registration = registerTimestampTask("timestamp1", "3.2.0", "", false);
			assertThat(registration.getVersion()).isEqualTo("3.2.0");
			assertThat(registration.getBootVersion()).isEqualTo(AppBootSchemaVersion.defaultVersion());
		}

		@Test
		void taskAppBootVersion2() {
			AppRegistration registration = registerTimestampTask("timestamp2", "3.2.0", "--bootVersion 2", false);
			assertThat(registration.getVersion()).isEqualTo("3.2.0");
			assertThat(registration.getBootVersion()).isEqualTo(AppBootSchemaVersion.BOOT2);
		}

		@Test
		void taskAppBootVersion3() {
			AppRegistration registration = registerTimestampTask("timestamp3", "3.2.1", "--bootVersion 3", false);
			assertThat(registration.getVersion()).isEqualTo("3.2.1");
			assertThat(registration.getBootVersion()).isEqualTo(AppBootSchemaVersion.BOOT3);
		}

		@Test
		void taskAppBootVersion2updateTo3() {
			AppRegistration registration = registerTimestampTask("timestamp2to3", "3.2.0", "-b 2", false);
			assertThat(registration.getVersion()).isEqualTo("3.2.0");
			assertThat(registration.getBootVersion()).isEqualTo(AppBootSchemaVersion.BOOT2);
			// The 'force=true' signals to udpate the existing 'timestamp2to3' app
			registration = registerTimestampTask("timestamp2to3", "3.2.1", "-b 3", true);
			assertThat(registration.getVersion()).isEqualTo("3.2.1");
			assertThat(registration.getBootVersion()).isEqualTo(AppBootSchemaVersion.BOOT3);
		}
	}

	@Nested
	class AppInfoTests {

		@Test
		void noBootVersion() {
			registerAppAndVerifyInfoCommand("time1", "", AppBootSchemaVersion.defaultVersion());
		}

		@ParameterizedTest
		@ValueSource(booleans = { true, false })
		void bootVersion2(boolean useLongBootVersionCommandArg) {
			String bootVersionCommandArg = useLongBootVersionCommandArg ? "--bootVersion 2" : "-b 2";
			registerAppAndVerifyInfoCommand("time2", bootVersionCommandArg, AppBootSchemaVersion.BOOT2);
		}

		@ParameterizedTest
		@ValueSource(booleans = { true, false })
		void bootVersion3(boolean useLongBootVersionCommandArg) {
			String bootVersionCommandArg = useLongBootVersionCommandArg ? "--bootVersion 3" : "-b 3";
			registerAppAndVerifyInfoCommand("time2", bootVersionCommandArg, AppBootSchemaVersion.BOOT3);
		}

		private void registerAppAndVerifyInfoCommand(String appName, String bootVersionCommandArg, AppBootSchemaVersion expectedBootVersion) {
			AppRegistration registration = registerTimeSource(appName, "3.2.1", bootVersionCommandArg, false);
			assertThat(registration.getBootVersion()).isEqualTo(expectedBootVersion);
			TablesInfo info = invokeAppInfoCommand(appName, ApplicationType.source);
			assertResultHasBootVersion(info, expectedBootVersion);
		}

		private TablesInfo invokeAppInfoCommand(String name, ApplicationType type) {
			String command = String.format( "app info --name %s --type %s ", name, type.name());
			logger.info("COMMAND -> {}", command);
			Object result = AppRegistryCommandsTests.this.commandRunner.executeCommand(command);
			logger.info("RESULT <- {}", result);
			assertThat(result).isInstanceOf(TablesInfo.class);
			return (TablesInfo) result;
		}

		private void assertResultHasBootVersion(TablesInfo result, AppBootSchemaVersion expectedBootVersion) {
			assertThat(result)
					.extracting(TablesInfo::getHeaders).asList()
					.contains(String.format("Boot version: %s:", expectedBootVersion.getBootVersion()));
		}
	}
}
