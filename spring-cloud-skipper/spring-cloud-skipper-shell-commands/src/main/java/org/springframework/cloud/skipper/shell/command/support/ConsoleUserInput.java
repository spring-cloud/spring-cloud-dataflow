/*
 * Copyright 2017 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import org.jline.reader.LineReader;

/**
 * Interact with the user via the JLine terminal.
 *
 * @author Eric Bottard
 */
public class ConsoleUserInput {

	private final LineReader lineReader;

	public ConsoleUserInput(LineReader lineReader) {
		this.lineReader = lineReader;
	}

	/**
	 * Loops until one of the {@code options} is provided. Pressing return is equivalent to
	 * returning {@code defaultValue}.
	 */
	public String promptWithOptions(String prompt, String defaultValue, String... options) {
		List<String> optionsAsList = Arrays.asList(options);
		String answer;
		do {
			answer = lineReader.readLine(String.format("%s %s: ", prompt, optionsAsList));
		}
		while (!optionsAsList.contains(answer) && !"".equals(answer));
		return "".equals(answer) && !optionsAsList.contains("") ? defaultValue : answer;
	}

	public String prompt(String prompt, String defaultValue, boolean echo) {
		String answer = lineReader.readLine(prompt + ": ", '*');
		return "".equals(answer) ? defaultValue : answer;
	}

}
