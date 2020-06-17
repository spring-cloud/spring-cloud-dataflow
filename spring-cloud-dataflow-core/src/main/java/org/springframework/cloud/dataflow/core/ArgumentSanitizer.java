/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Sanitizes potentially sensitive keys for a specific command line arg.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
public class ArgumentSanitizer {

	private static final String[] REGEX_PARTS = { "*", "$", "^", "+" };

	private static final String REDACTION_STRING = "******";

	private static final String[] KEYS_TO_SANITIZE = { "username", "password", "secret", "key", "token", ".*credentials.*",
			"vcap_services", "url" };

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

		value = sanitize(key, value);

		return String.format("%s=%s", key, value);
	}

	/**
	 * Replaces a potential secure value with "******".
	 *
	 * @param key to check for sensitive words.
	 * @param value the argument to cleanse.
	 * @return the argument with a potentially sanitized value
	 */
	public String sanitize(String key, String value) {
		if (StringUtils.hasText(value)) {
			for (Pattern pattern : this.keysToSanitize) {
				if (pattern.matcher(key).matches()) {
					value = REDACTION_STRING;
					break;
				}
			}
		}
		return value;
	}

	/**
	 * For all sensitive properties (e.g. key names containing words like password, secret,
	 * key, token) replace the value with '*****' string
	 * @param properties to be sanitized
	 * @return sanitized properties
	 */
	public Map<String, String> sanitizeProperties(Map<String, String> properties) {
		if (!CollectionUtils.isEmpty(properties)) {
			final Map<String, String> sanitizedProperties = new LinkedHashMap<>(properties.size());
			for (Map.Entry<String, String > property : properties.entrySet()) {
				sanitizedProperties.put(property.getKey(), this.sanitize(property.getKey(), property.getValue()));
			}
			return sanitizedProperties;
		}
		return properties;
	}

	/**
	 * For all sensitive arguments (e.g. key names containing words like password, secret,
	 * key, token) replace the value with '*****' string
	 * @param arguments to be sanitized
	 * @return sanitized arguments
	 */
	public List<String> sanitizeArguments(List<String> arguments) {
		if (!CollectionUtils.isEmpty(arguments)) {
			final List<String> sanitizedArguments = new ArrayList<>(arguments.size());
			for (String argument : arguments) {
				sanitizedArguments.add(this.sanitize(argument));
			}
			return sanitizedArguments;
		}
		return arguments;
	}

}
