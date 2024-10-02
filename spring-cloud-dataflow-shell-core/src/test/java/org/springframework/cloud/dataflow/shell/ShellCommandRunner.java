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
package org.springframework.cloud.dataflow.shell;

import java.util.List;

import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;

import org.springframework.shell.Input;
import org.springframework.shell.Shell;
import org.springframework.shell.Utils;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test utility that knows how to execute a simple string command in non-interactive mode.
 *
 * <p>Invokes commands directly using {@link Shell#evaluate} which allows inspection of the command result.
 *
 * @author Chris Bono
 */
public class ShellCommandRunner {

	private final Shell shell;

	private boolean validateCommandSuccess;

	public ShellCommandRunner(Shell shell) {
		this.shell = shell;
	}

	/**
	 * @return a copy of this runner that validates that all executed commands succeed.
	 */
	public ShellCommandRunner withValidateCommandSuccess() {
		ShellCommandRunner copy = new ShellCommandRunner(this.shell);
		copy.validateCommandSuccess = true;
		return copy;
	}

	/**
	 * Executes the specified command.
	 *
	 * @param command command line to execute
	 * @return result of executing command
	 */
	public Object executeCommand(String command) {
		Parser parser = new DefaultParser();
		ParsedLine parsedLine = parser.parse(command, command.length() + 1);
		// TODO: evaluate is not private method in spring-shell so calling it via
		//       reflection until we refactor to use new shell testing system
		Object rawResult = ReflectionTestUtils.invokeMethod(this.shell, "evaluate", new ParsedLineInput(parsedLine));
		if (!this.validateCommandSuccess) {
			assertThat(rawResult)
					.isNotNull()
					.isNotInstanceOf(Exception.class);
		}
		if (rawResult instanceof Exception) {
			throw new RuntimeException((Exception) rawResult);
		}
		return rawResult;
	}

	static class ParsedLineInput implements Input {

		private final ParsedLine parsedLine;

		ParsedLineInput(ParsedLine parsedLine) {
			this.parsedLine = parsedLine;
		}

		@Override
		public String rawText() {
			return parsedLine.line();
		}

		@Override
		public List<String> words() {
			return Utils.sanitizeInput(parsedLine.words());
		}
	}
}
