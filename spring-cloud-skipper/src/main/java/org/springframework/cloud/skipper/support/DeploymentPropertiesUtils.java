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

package org.springframework.cloud.skipper.support;

import java.util.ArrayList;
import java.util.Enumeration;
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
 * @author Janne Valkealahti
 */
public final class DeploymentPropertiesUtils {

	/**
	 * Pattern used for parsing a String of command-line arguments.
	 */
	private static final Pattern DEPLOYMENT_PARAMS_PATTERN = Pattern
			.compile("(\\s(?=" + "([^\\\"']*[\\\"'][^\\\"']*[\\\"'])*[^\\\"']*$))");

	private DeploymentPropertiesUtils() {
		// prevent instantiation
	}

	/**
	 * Parses a String comprised of 0 or more comma-delimited key=value pairs where each key
	 * has the properties format(strings and dots). Values may themselves contain commas, since
	 * the split points will be based upon the key pattern.
	 * <p>
	 * Logic of parsing key/value pairs from a string is based on few rules and assumptions 1.
	 * keys will not have commas or equals. 2. First raw split is done by commas which will
	 * need to be fixed later if value is a comma-delimited list.
	 *
	 * @param s the string to parse
	 * @return the Map of parsed key value pairs
	 */
	public static Map<String, String> parse(String s) {
		Map<String, String> deploymentProperties = new HashMap<String, String>();
		ArrayList<String> pairs = new ArrayList<>();

		// get raw candidates as simple comma split
		String[] candidates = StringUtils.commaDelimitedListToStringArray(s);
		for (int i = 0; i < candidates.length; i++) {
			if (i > 0 && !candidates[i].contains("=")) {
				// we don't have '=' so this has to be latter parts of
				// a comma delimited value, append it to previously added
				// key/value pair.
				// we skip first as we would not have anything to append to. this
				// would happen if dep prop string is malformed and first given
				// key/value pair is not actually a key/value.
				pairs.set(pairs.size() - 1, pairs.get(pairs.size() - 1) + "," + candidates[i]);
			}
			else {
				// we have a key/value pair having '=', or malformed first pair
				pairs.add(candidates[i]);
			}
		}

		// add what we got, addKeyValuePairAsProperty
		// handles rest as trimming, etc
		for (String pair : pairs) {
			addKeyValuePairAsProperty(pair, deploymentProperties);
		}
		return deploymentProperties;
	}

	/**
	 * Returns a String representation of deployment properties as a comma separated list of
	 * key=value pairs.
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
	 * Convert Properties to a Map with String keys and String values.
	 *
	 * @param properties the properties object
	 * @return the equivalent {@code Map<String,String>}
	 */
	public static Map<String, String> convert(Properties properties) {
		Map<String, String> result = new HashMap<>(properties.size());
		for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
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
			properties.put(pair.substring(0, firstEquals).trim(), pair.substring(firstEquals + 1).trim());
		}
	}

	/**
	 * Parses a list of command line parameters and returns a list of parameters which doesn't
	 * contain any special quoting either for values or whole parameter.
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
			if (param.charAt(0) == c && param.charAt(param.length() - 1) == c) {
				param = param.substring(1, param.length() - 1);
			}
		}
		return param;
	}
}
