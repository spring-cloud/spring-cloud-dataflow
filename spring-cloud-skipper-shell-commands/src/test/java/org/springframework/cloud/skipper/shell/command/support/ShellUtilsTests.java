/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.cloud.skipper.shell.command.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ShellUtils}.
 */
class ShellUtilsTests {

	@ParameterizedTest
	@ValueSource(strings = { "-h", "--h", "-help", "--help", "help", "h" })
	void hasHelpOptionReturnsTrueWhenHelpOptionSpecified(String helpArg) {
		ApplicationArguments appArgs = new DefaultApplicationArguments("foo", helpArg);
		assertThat(ShellUtils.hasHelpOption(appArgs)).isTrue();
	}

	@Test
	void hasHelpOptionReturnsFalseWhenNoHelpOptionSpecified() {
		ApplicationArguments appArgs = new DefaultApplicationArguments("foo");
		assertThat(ShellUtils.hasHelpOption(appArgs)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "-h", "--h", "-help", "--help", "help", "h" })
	void filteredArgsToShellCommandsReturnsSingleHelpCommandWhenHelpOptionSpecified(String helpArg) {
		ApplicationArguments appArgs = new DefaultApplicationArguments("foo", helpArg);
		assertThat(ShellUtils.filteredArgsToShellCommands(appArgs)).containsExactly("help");
	}

	@Test
	void filteredArgsToShellCommandsReturnsAllArgsWhenNoSkipperClientArgsSpecified() {
		ApplicationArguments appArgs = new DefaultApplicationArguments("foo", "bar");
		assertThat(ShellUtils.filteredArgsToShellCommands(appArgs)).containsExactly("foo", "bar");
	}

	@Test
	void filteredArgsToShellCommandsFiltersOutSkipperClientArgs() {
		ApplicationArguments appArgs = new DefaultApplicationArguments("foo",
				"--spring.cloud.skipper.client.url=http://localhost", "--spring.cloud.skipper.client.foo=bar");
		assertThat(ShellUtils.filteredArgsToShellCommands(appArgs)).containsExactly("foo");
	}
}
