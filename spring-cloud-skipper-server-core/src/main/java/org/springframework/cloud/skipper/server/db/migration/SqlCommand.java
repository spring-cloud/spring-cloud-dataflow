/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.skipper.server.db.migration;

import java.util.Collections;
import java.util.List;

/**
 * Class keeping a sql command and its possible suppressing sql codes together.
 *
 * @author Janne Valkealahti
 *
 */
public class SqlCommand {

	private final String command;
	private final List<Integer> suppressedErrorCodes;

	/**
	 * Convenience method returning new instance.
	 *
	 * @param command the command
	 * @return the sql command
	 */
	public static SqlCommand from(String command) {
		return new SqlCommand(command, null);
	}

	/**
	 * Convenience method returning new instance.
	 *
	 * @param command the command
	 * @param suppressedErrorCode the suppressed error code
	 * @return the sql command
	 */
	public static SqlCommand from(String command, int suppressedErrorCode) {
		return new SqlCommand(command, suppressedErrorCode);
	}

	/**
	 * Instantiates a new sql command.
	 *
	 * @param command the command
	 * @param suppressedErrorCode the suppressed error code
	 */
	public SqlCommand(String command, int suppressedErrorCode) {
		this(command, Collections.singletonList(suppressedErrorCode));
	}

	/**
	 * Instantiates a new sql command.
	 *
	 * @param command the command
	 * @param suppressedErrorCodes the suppressed error codes
	 */
	public SqlCommand(String command, List<Integer> suppressedErrorCodes) {
		this.command = command;
		this.suppressedErrorCodes = suppressedErrorCodes;
	}

	/**
	 * Gets the command.
	 *
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Gets the suppressed error codes.
	 *
	 * @return the suppressed error codes
	 */
	public List<Integer> getSuppressedErrorCodes() {
		return suppressedErrorCodes;
	}
}
