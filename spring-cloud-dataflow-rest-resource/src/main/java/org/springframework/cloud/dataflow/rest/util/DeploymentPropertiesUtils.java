/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.rest.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

/**
 * Provides utility methods for formatting and parsing deployment properties.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public final class DeploymentPropertiesUtils {

	private DeploymentPropertiesUtils() {
		// prevent instantiation
	}

	/**
	 * Pattern used for parsing a String of comma-delimited key=value pairs.
	 */
	private static final Pattern DEPLOYMENT_PROPERTIES_PATTERN = Pattern.compile(",\\s*app\\.[^\\.]+\\.[^=]+=");

	/**
	 * Pattern used for parsing a String of command-line arguments.
	 */
	private static final Pattern DEPLOYMENT_PARAMS_PATTERN = Pattern.compile("(\\s(?=([^\\\"']*[\\\"'][^\\\"']*[\\\"'])*[^\\\"']*$))");

	/**
	 * Parses a String comprised of 0 or more comma-delimited key=value pairs where each key has the format:
	 * {@code app.[appname].[key]}.
	 * Values may themselves contain commas, since the split points will be based upon the key pattern.
	 *
	 * @param s the string to parse
	 * @return the Map of parsed key value pairs
	 */
	public static Map<String, String> parse(String s) {
		Map<String, String> deploymentProperties = new HashMap<String, String>();
		if (!StringUtils.isEmpty(s)) {
			Matcher matcher = DEPLOYMENT_PROPERTIES_PATTERN.matcher(s);
			int start = 0;
			while (matcher.find()) {
				addKeyValuePairAsProperty(s.substring(start, matcher.start()), deploymentProperties);
				start = matcher.start() + 1;
			}
			addKeyValuePairAsProperty(s.substring(start), deploymentProperties);
		}
		return deploymentProperties;
	}

	/**
	 * Returns a String representation of deployment properties as a comma separated list of key=value pairs.
	 *
	 * @param properties the properties to format
	 * @return the properties formatted as a String
	 */
	public static String format(Map<String, String> properties) {
		StringBuilder sb = new StringBuilder(15 * properties.size());
		for (Map.Entry<String, String> pair : properties.entrySet()) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(pair.getKey()).append("=").append(pair.getValue());
		}
		return sb.toString();
	}

	/**
	 * Convert Properties to a Map with String keys and values. Entries whose key or value is not a String are omitted.
	 */
	public static Map<String, String> convert(Properties properties) {
		Map<String, String> result  = new HashMap<>(properties.size());
		for (String key : properties.stringPropertyNames()) {
			result.put(key, properties.getProperty(key));
		}
		return result;
	}

	/**
	 * Adds a String of format key=value to the provided Map as a key/value pair.
	 *
	 * @param pair the String representation
	 * @param properties the Map to which the key/value pair should be added
	 */
	private static void addKeyValuePairAsProperty(String pair, Map<String, String> properties) {
		int firstEquals = pair.indexOf('=');
		if (firstEquals != -1) {
			// todo: should key only be a "flag" as in: put(key, true)?
			properties.put(pair.substring(0, firstEquals).trim(), pair.substring(firstEquals + 1).trim());
		}
	}

	/**
	 * Parses a list of command line parameters and returns a list of parameters
	 * which doesn't contain any special quoting either for values or whole parameter.
	 *
	 * @param params the params
	 * @return the list
	 */
	public static List<String> parseParams(List<String> params) {
		List<String> paramsToUse = new ArrayList<>();
		if (params != null) {
			for (String param : params) {
				Matcher regexMatcher = DEPLOYMENT_PARAMS_PATTERN.matcher(param);
				int start = 0;
				while (regexMatcher.find()) {
					String p = removeQuoting(param.substring(start, regexMatcher.start()).trim());
					if (StringUtils.hasText(p)) {
						paramsToUse.add(p);
					}
					start = regexMatcher.start();
				}
				if (param != null && param.length() > 0) {
					String p = removeQuoting(param.substring(start, param.length()).trim());
					if (StringUtils.hasText(p)) {
						paramsToUse.add(p);
					}
				}
			}
		}
		return paramsToUse;
	}

	private static String removeQuoting(String param) {
		param = removeQuote(param, '\'');
		param = removeQuote(param, '"');
		if (StringUtils.hasText(param)) {
			String[] split = param.split("=", 2);
			if (split.length == 2) {
				String value = removeQuote(split[1], '\'');
				value = removeQuote(value, '"');
				param = split[0] + "=" + value;
			}
		}
		return param;
	}

	private static String removeQuote(String param, char c) {
		if (param != null && param.length() > 1) {
			if (param.charAt(0) == c && param.charAt(param.length()-1) == c) {
				param = param.substring(1, param.length()-1);
			}
		}
		return param;
	}
}
