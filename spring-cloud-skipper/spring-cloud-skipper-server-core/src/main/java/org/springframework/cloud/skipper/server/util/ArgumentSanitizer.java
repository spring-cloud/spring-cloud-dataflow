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
package org.springframework.cloud.skipper.server.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Sanitizes potentially sensitive keys from manifest data.
 *
 * @author Glenn Renfro
 */
public class ArgumentSanitizer {
	private static final Logger logger = LoggerFactory.getLogger(ArgumentSanitizer.class);

	private static final String REDACTION_STRING = "******";

	private static final String[] REGEX_PARTS = { "*", "$", "^", "+" };

	private static final String[] KEYS_TO_SANITIZE = { "password", "secret", "key", "token", ".*credentials.*",
			"vcap_services" };

	private static Pattern[] keysToSanitize;

	static {
		keysToSanitize = new Pattern[KEYS_TO_SANITIZE.length];
		for (int i = 0; i < keysToSanitize.length; i++) {
			keysToSanitize[i] = getPattern(KEYS_TO_SANITIZE[i]);
		}
	}

	/**
	 * Redacts the values stored for keys that contain the following:  password,
	 * secret, key, token, *credentials.*.
	 * @param yml String containing a yaml.
	 * @return redacted yaml String.
	 */
	public static String sanitizeYml(String yml) {
		String result = "";
		try {
			DumperOptions options = new DumperOptions();
			options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			options.setPrettyFlow(true);
			Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(options), options);
			Iterator<Object> iter = yaml.loadAll(yml).iterator();
			while (iter.hasNext()) {
				Object o = iter.next();
				if (o instanceof LinkedHashMap) {
					iterateLinkedHashMap((LinkedHashMap<String, Object>) o);
				}
				result += yaml.dump(o);
			}
		}
		catch (Throwable throwable) {
			logger.error("Unable to redact data from Manifest debug entry", throwable);
		}
		if (result == null || result.length() == 0) {
			result = yml;
		}
		return result;
	}

	private static void iterateLinkedHashMap(LinkedHashMap<String, Object> map) {
		for (String key : map.keySet()) {
			Object value = map.get(key);
			if (value instanceof LinkedHashMap) {
				iterateLinkedHashMap((LinkedHashMap<String, Object>) value);
			}
			else {
				if (toBeSanitized(key)) {
					map.put(key, REDACTION_STRING);
				}
			}
		}
	}

	/**
	 * Replaces a potential secure value with "******".
	 *
	 * @param key to evaluate if its value should be redacted.
	 * @return true if result needs to be sanitized.
	 */
	private static boolean toBeSanitized(String key) {
		boolean result = false;
		for (Pattern pattern : keysToSanitize) {
			if (pattern.matcher(key).matches()) {
				result = true;
				break;
			}
		}
		return result;
	}

	private static Pattern getPattern(String value) {
		if (isRegex(value)) {
			return Pattern.compile(value, Pattern.CASE_INSENSITIVE);
		}
		return Pattern.compile(".*" + value + "$", Pattern.CASE_INSENSITIVE);
	}

	private static boolean isRegex(String value) {
		for (String part : REGEX_PARTS) {
			if (value.contains(part)) {
				return true;
			}
		}
		return false;
	}
}
