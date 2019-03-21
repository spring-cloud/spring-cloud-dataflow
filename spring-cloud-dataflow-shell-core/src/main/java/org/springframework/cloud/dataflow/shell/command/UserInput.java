/*
 * Copyright 2013-2015 the original author or authors.
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

/**
 * Abstraction for a mechanism used to get user interactive user input.
 *
 * @author Eric Bottard
 * @author Marius Bogoevici
 */
public interface UserInput {

	/**
	 * Display a prompt text to the user and expect one of {@code options} in return.
	 *
	 * @param prompt the a message to prompt the user with
	 * @param defaultValue the default value to be returned if the user simply presses
	 * Enter
	 * @param options valid input option set
	 * @return the prompt text to display to the user
	 */
	public String promptWithOptions(String prompt, String defaultValue, String... options);

	/**
	 * Display a prompt text to the user and expect them to enter a free-form value.
	 * Optionally, the input is echoed.
	 *
	 * @param prompt the a message to prompt the user with
	 * @param defaultValue the default value to be returned if the user simply presses
	 * Enter
	 * @param echo echo the input to output (set to false for sensitive input, e.g.
	 * passwords)
	 * @return the prompt text to display to the user
	 */
	public String prompt(String prompt, String defaultValue, boolean echo);

}
