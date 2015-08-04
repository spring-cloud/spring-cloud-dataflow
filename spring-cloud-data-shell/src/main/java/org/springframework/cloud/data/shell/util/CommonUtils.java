/*
 * Copyright 2009-2015 the original author or authors.
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

package org.springframework.cloud.data.shell.util;

/**
 * Contains common non-ui related helper methods for rendering text to the console.
 *
 * @author Gunnar Hillert
 * @author Stephan Oudmaijer
 *
 * @since 1.0
 */
public final class CommonUtils {

	private static final String EMAIL_VALIDATION_REGEX = "^[_A-Za-z0-9-+]+(\\.[_A-Za-z0-9-+]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";

	public static final String NOT_AVAILABLE = "N/A";

	/**
	 * Prevent instantiation.
	 */
	private CommonUtils() {
		throw new AssertionError();
	}

	/**
	 * Right-pad a String with a configurable padding character.
	 *
	 * @param inputString The String to pad. A {@code null} String will be treated like an empty String.
	 * @param size Pad String by the number of characters.
	 * @param paddingChar The character to pad the String with.
	 * @return The padded String. If the provided String is null, an empty String is returned.
	 */
	public static String padRight(String inputString, int size, char paddingChar) {

		final String stringToPad;

		if (inputString == null) {
			stringToPad = "";
		}
		else {
			stringToPad = inputString;
		}

		StringBuilder padded = new StringBuilder(stringToPad);
		while (padded.length() < size) {
			padded.append(paddingChar);
		}
		return padded.toString();
	}

	/**
	 * Right-pad the provided String with empty spaces.
	 *
	 * @param string The String to pad
	 * @param size Pad String by the number of characters.
	 * @return The padded String. If the provided String is null, an empty String is returned.
	 */
	public static String padRight(String string, int size) {
		return padRight(string, size, ' ');
	}
}
