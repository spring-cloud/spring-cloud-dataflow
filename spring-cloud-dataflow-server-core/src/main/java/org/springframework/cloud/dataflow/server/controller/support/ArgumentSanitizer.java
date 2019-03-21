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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionToDslConverter;

/**
 * Sanitizes potentially sensitive keys for a specific command line arg.
 *
 * @author Glenn Renfro
 */
public class ArgumentSanitizer {
	private static final String[] REGEX_PARTS = { "*", "$", "^", "+" };

	private static final String REDACTION_STRING = "******";

	private static final String[] KEYS_TO_SANITIZE = { "password", "secret", "key", "token", ".*credentials.*",
			"vcap_services" };

	private Pattern[] keysToSanitize;

	private StreamDefinitionToDslConverter dslConverter;

	public ArgumentSanitizer() {
		this.keysToSanitize = new Pattern[KEYS_TO_SANITIZE.length];
		for (int i = 0; i < keysToSanitize.length; i++) {
			this.keysToSanitize[i] = getPattern(KEYS_TO_SANITIZE[i]);
		}
		this.dslConverter = new StreamDefinitionToDslConverter();
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
		for (Pattern pattern : this.keysToSanitize) {
			if (pattern.matcher(key).matches()) {
				value = REDACTION_STRING;
				break;
			}
		}
		return value;
	}

	/**
	 * Redacts sensitive property values in a stream.
	 *
	 * @param streamDefinition the stream definition to sanitize
	 * @return Stream definition text that has sensitive data redacted.
	 */
	public String sanitizeStream(StreamDefinition streamDefinition) {
		List<StreamAppDefinition> sanitizedAppDefinitions = streamDefinition.getAppDefinitions().stream()
				.map(app -> StreamAppDefinition.Builder
						.from(app)
						.setProperties(this.sanitizeProperties(app.getProperties()))
						.build(streamDefinition.getName())
				).collect(Collectors.toList());

		return this.dslConverter.toDsl(sanitizedAppDefinitions);
	}

	private Map<String, String> sanitizeProperties(Map<String, String> properties) {
		return properties.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> this.sanitize(e.getKey(), e.getValue())));
	}
}
