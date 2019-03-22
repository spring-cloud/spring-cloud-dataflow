/*
 * Copyright 2018 the original author or authors.
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * {@link UserInput} that uses Standard in and out.
 *
 * @author Eric Bottard
 * @author Gary Russell
 */
@Component
public class ConsoleUserInput implements UserInput {

	/**
	 * Loops until one of the {@code options} is provided. Pressing return is equivalent
	 * to returning {@code defaultValue}.
	 */
	@Override
	public String promptWithOptions(String prompt, String defaultValue, String... options) {
		List<String> optionsAsList = Arrays.asList(options);
		InputStreamReader console = new InputStreamReader(System.in);
		String answer;
		do {
			System.out.format("%s %s: ", prompt, optionsAsList);
			answer = read(console, true);
		}
		while (!optionsAsList.contains(answer) && !"".equals(answer));
		return "".equals(answer) && !optionsAsList.contains("") ? defaultValue : answer;
	}

	@Override
	public String prompt(String prompt, String defaultValue, boolean echo) {
		InputStreamReader console = new InputStreamReader(System.in);
		System.out.format("%s: ", prompt);
		String answer = read(console, echo);
		return "".equals(answer) ? defaultValue : answer;
	}

	/**
	 * Reads a single line of input from the console.
	 *
	 * @param console input
	 * @param echo whether the input should be echoed (e.g. false for passwords, other
	 * sensitive data)
	 */
	private String read(InputStreamReader console, boolean echo) {
		StringBuilder builder = new StringBuilder();
		try {
			for (char c = (char) console.read(); !(c == '\n' || c == '\r'); c = (char) console.read()) {
				if (echo) {
					System.out.print(c);
				}
				builder.append(c);
			}
			System.out.println();
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return builder.toString();
	}
}
