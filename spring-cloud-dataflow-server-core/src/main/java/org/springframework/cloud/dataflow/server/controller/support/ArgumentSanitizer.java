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

package org.springframework.cloud.dataflow.server.controller.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Sanitizes potentially sensitive keys for a specific command line arg.
 *
 * @author Glenn Renfro
 */
public class ArgumentSanitizer {
	private static final String[] REGEX_PARTS = {"*", "$", "^", "+"};

	private static final String REDACTION_STRING = "******";

	private static final String[] KEYS_TO_SANITIZE = {"password", "secret", "key", "token", ".*credentials.*",
			"vcap_services"};
	//used to find the passwords embedded in a stream definition
	private static Pattern passwordParameterPatternForStreams = Pattern.compile(
			//Search for the -- characters then look for unicode letters
			"(?i)(--[\\p{Z}]*[\\p{L}]*("
			//that match the following strings from the KEYS_TO_SANITIZE array
					+ StringUtils.arrayToDelimitedString(KEYS_TO_SANITIZE, "|")
			//Following the equal sign (group1) accept any number of unicode characters(letters, open punctuation, close punctuation etc) for the value to be sanitized for group 3.
					+ ")[\\p{L}]*[\\p{Z}]*=[\\p{Z}]*)((\"[\\p{L}|\\p{Pd}|\\p{Ps}|\\p{Pe}|\\p{Pc}|\\p{S}|\\p{N}|\\p{Z}]*\")|([\\p{N}|\\p{L}|\\p{Po}|\\p{Pc}|\\p{S}]*))",
			Pattern.UNICODE_CASE);


	private Pattern[] keysToSanitize;

	public ArgumentSanitizer() {
		this.keysToSanitize = new Pattern[KEYS_TO_SANITIZE.length];
		for (int i = 0; i < keysToSanitize.length; i++) {
			this.keysToSanitize[i] = getPattern(KEYS_TO_SANITIZE[i]);
		}
	}

	private Pattern getPattern(String value) {
		if (isRegex(value)) {
			return Pattern.compile(value, Pattern.CASE_INSENSITIVE);
		}
		return Pattern.compile(".*" + value + "$", Pattern.CASE_INSENSITIVE);
	}

	private boolean isRegex(String value) {
		for (String part : REGEX_PARTS) {
			if (value.contains(part)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Replaces a potential secure value with "******".
	 *
	 * @param argument the argument to cleanse.
	 * @return the argument with a potentially sanitized value
	 */
	public String sanitize(String argument) {
		int indexOfFirstEqual = argument.indexOf("=");
		if (indexOfFirstEqual == -1) {
			return argument;
		}
		String key = argument.substring(0, indexOfFirstEqual);
		String value = argument.substring(indexOfFirstEqual + 1);
		for (Pattern pattern : this.keysToSanitize) {
			if (pattern.matcher(key).matches()) {
				value = REDACTION_STRING;
				break;
			}
		}
		return String.format("%s=%s", key, value);
	}

	/**
	 * Redacts sensitive values in a stream.
	 *
	 * @param definition the definition to sanitize
	 * @return Stream definition that has sensitive data redacted.
	 */
	public static String sanitizeStream(String definition) {
		Assert.hasText(definition, "definition must not be null nor empty");
		final StringBuffer output = new StringBuffer();
		final Matcher matcher = passwordParameterPatternForStreams.matcher(definition);
		while (matcher.find()) {
			String passwordValue = matcher.group(3);

			String maskedPasswordValue;
			boolean isPipeAppended = false;
			boolean isNameChannelAppended = false;
			if (passwordValue.endsWith("|")) {
				isPipeAppended = true;
			}
			if (passwordValue.endsWith(">")) {
				isNameChannelAppended = true;
			}
			maskedPasswordValue = REDACTION_STRING;
			if (isPipeAppended) {
				maskedPasswordValue = maskedPasswordValue + " |";
			}
			if (isNameChannelAppended) {
				maskedPasswordValue = maskedPasswordValue + " >";
			}
			matcher.appendReplacement(output, matcher.group(1) + maskedPasswordValue);
		}
		matcher.appendTail(output);
		return output.toString();
	}

}
