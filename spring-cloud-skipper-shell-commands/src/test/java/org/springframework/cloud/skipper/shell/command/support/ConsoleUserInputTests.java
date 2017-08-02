/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.skipper.shell.command.support;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
public class ConsoleUserInputTests {
	@Test
	public void promptWithOptions() throws Exception {

		String testAnswer = "y\n";
		InputStream stream = getInputStream(testAnswer);
		assertThat(getConsoleUserInput(stream).promptWithOptions("Really do this?", "n", "y", "n")).isEqualTo("y");
		testAnswer = "n\n";
		stream = getInputStream(testAnswer);
		assertThat(getConsoleUserInput(stream).promptWithOptions("Really do this?", "n", "y", "n")).isEqualTo("n");
	}

	@Test
	public void prompt() throws Exception {
		String testString = "mypassword\n";
		InputStream stream = getInputStream(testString);
		assertThat(getConsoleUserInput(stream).prompt("myshell", "badvalue", false)).isEqualTo("mypassword");
	}

	private InputStream getInputStream(String testString) {
		return new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
	}

	private ConsoleUserInput getConsoleUserInput(InputStream stream) {
		return new ConsoleUserInput(new InputStreamReader(stream));
	}

}
