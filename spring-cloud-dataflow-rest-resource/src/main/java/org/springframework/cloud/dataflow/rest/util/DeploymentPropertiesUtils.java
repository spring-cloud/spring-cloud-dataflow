/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.StringUtils;


/**
 * Provides utility methods for formatting and parsing deployment properties.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Janne Valkealahti
 * @author Christian Tzolov
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 */
public final class DeploymentPropertiesUtils {
	private static final Logger logger = LoggerFactory.getLogger(DeploymentPropertiesUtils.class);
	/**
	 * Pattern used for parsing a String of command-line arguments.
	 */
	private static final Pattern DEPLOYMENT_PARAMS_PATTERN = Pattern
			.compile("(\\s(?=" + "([^\\\"']*[\\\"'][^\\\"']*[\\\"'])*[^\\\"']*$))");


	private static final String[] DEPLOYMENT_PROPERTIES_PREFIX ={"deployer", "app", "version", "spring.cloud.dataflow.task"};

	private DeploymentPropertiesUtils() {
		// prevent instantiation
	}

	/**
	 * Parses a String comprised of 0 or more comma-delimited key=value pairs where each key
	 * has the format: {@code app.[appname].[key]} or {@code deployer.[appname].[key]}. Values
	 * may themselves contain commas, since the split points will be based upon the key
	 * pattern.
	 * <p>
	 * Logic of parsing key/value pairs from a string is based on few rules and assumptions 1.
	 * keys will not have commas or equals. 2. First raw split is done by commas which will
	 * need to be fixed later if value is a comma-delimited list.
	 *
	 * @param s the string to parse
	 * @return the Map of parsed key value pairs
	 */
	public static Map<String, String> parse(String s) {
		Map<String, String> deploymentProperties = new LinkedHashMap<>();
		List<String> pairs = parseParamList(s, ",");

		// add what we got, addKeyValuePairAsProperty
		// handles rest as trimming, etc
		for (String pair : pairs) {
			addKeyValuePairAsProperty(pair, deploymentProperties);
		}
		logger.debug("parse:{}={}", s, deploymentProperties);
		return deploymentProperties;
	}

	/**
	 * Parses a String comprised of 0 or more delimited key=value pairs where each key
	 * has the format: {@code app.[appname].[key]} or {@code deployer.[appname].[key]}. Values
	 * may themselves contain commas, since the split points will be based upon the key
	 * pattern.
	 * <p>
	 * Logic of parsing key/value pairs from a string is based on few rules and assumptions 1.
	 * keys will not have commas or equals. 2. First raw split is done by commas which will
	 * need to be fixed later if value is a comma-delimited list.
	 *
	 * @param s the string to parse
	 * @param delimiter delimiter used to split the string into pairs
	 * @return the List key=value pairs
	 */
	public static List<String> parseParamList(String s, String delimiter) {
		ArrayList<String> pairs = new ArrayList<>();

		// get raw candidates as simple comma split
		String[] candidates = StringUtils.delimitedListToStringArray(s, delimiter);
		for (int i = 0; i < candidates.length; i++) {
			String candidate = candidates[i];
			if(StringUtils.hasText(candidate)) {
				if (i > 0 && !candidate.contains("=") || (i > 0 && candidate.contains("=") && !startsWithDeploymentPropertyPrefix(candidate))) {
					// we don't have '=' so this has to be latter parts of
					// a comma delimited value, append it to previously added
					// key/value pair.
					// we skip first as we would not have anything to append to. this
					// would happen if dep prop string is malformed and first given
					// key/value pair is not actually a key/value.
					pairs.set(pairs.size() - 1, pairs.get(pairs.size() - 1) + delimiter + candidate);
				} else {
					// we have a key/value pair having '=', or malformed first pair
					if (!startsWithDeploymentPropertyPrefix(candidate)) {
						throw new IllegalArgumentException(
								"Only deployment property keys starting with 'app.' or 'deployer.'  or 'version.'" +
										" allowed. Not " + candidate);
					}
					pairs.add(candidate);
				}
			}
		}

		return pairs;
	}


	/**
	 * Parses a String comprised of 0 or more delimited key=value pairs where each key
	 * has the format: {@code app.[appname].[key]} or {@code deployer.[appname].[key]}. Values
	 * may themselves contain commas, since the split points will be based upon the key
	 * pattern.
	 * <p>
	 * Logic of parsing key/value pairs from a string is based on few rules and assumptions 1.
	 * keys will not have commas or equals. 2. First raw split is done by commas which will
	 * need to be fixed later if value is a comma-delimited list.
	 *
	 * @param s the string to parse
	 * @param delimiter delimiter used to split the string into pairs
	 * @return the List key=value pairs
	 */
	public static List<String> parseArgumentList(String s, String delimiter) {
		ArrayList<String> pairs = new ArrayList<>();
		if (s != null && s.contains("=")) {
			// get raw candidates as simple comma split
			String[] candidates = StringUtils.delimitedListToStringArray(s, delimiter);
			for (int i = 0; i < candidates.length; i++) {
				int elementsInQuotesIndex = findEndToken(candidates, i) +1;
				if (elementsInQuotesIndex > -1) {
					if(!candidates[i].equals("")) {
						pairs.add(candidates[i]);
					}
					i++;
					for (; i < elementsInQuotesIndex; i++) {
						pairs.set(pairs.size() - 1, pairs.get(pairs.size() - 1) + delimiter + candidates[i]);
					}
					if(!(i < candidates.length)) {
						break;
					}
				}
				if (i > 0 && !candidates[i].contains("=")) {
					// we don't have '=' so this has to be latter parts of
					// a comma delimited value, append it to previously added
					// key/value pair.
					// we skip first as we would not have anything to append to. this
					// would happen if dep prop string is malformed and first given
					// key/value pair is not actually a key/value.
					pairs.set(pairs.size() - 1, pairs.get(pairs.size() - 1) + delimiter + candidates[i]);
				}
				else {
					// we have a key/value pair having '=', or malformed first pair
					if(!candidates[i].equals("")) {
						int endToken = findEndToken(candidates, i);
						if(endToken > -1) {
							pairs.add(candidates[i] + " " + candidates[endToken]);
							i = endToken;
						}
						else {
							pairs.add(candidates[i]);
						}
					}
				}
			}
			for(int i = 0; i < pairs.size(); i++) {
				pairs.set(i, StringUtils.trimTrailingWhitespace(pairs.get(i)));
			}
 		}
		return pairs;
	}

	private  static int findEndToken(String[] candidates, int currentPos) {
		int result = -1;
		if(!candidates[currentPos].contains("=\"")) {
			return -1;
		}
		for(int i = currentPos; i < candidates.length; i++) {
			if(candidates[i].endsWith("\"" )) {
				result = i;
				break;
			}
		}
		return result;
	}


	private static boolean startsWithDeploymentPropertyPrefix(String candidate) {
		for (String deploymentPropertyPrefix: DEPLOYMENT_PROPERTIES_PREFIX) {
			if (StringUtils.hasText(candidate)) {
				String prefix = candidate.trim().startsWith("--") ? candidate.trim().substring(2) : candidate.trim();
				if (prefix.startsWith(deploymentPropertyPrefix)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Parses a deployment properties conditionally either from properties
	 * string or file which can be legacy properties file or yaml.
	 *
	 * @param deploymentProperties the deployment properties string
	 * @param propertiesFile the deployment properties file
	 * @param which the flag to choose between properties or file
	 * @return the map of parsed properties
	 * @throws IOException if file loading errors
	 */
	public static Map<String, String> parseDeploymentProperties(String deploymentProperties, File propertiesFile,
			int which) throws IOException {
		Map<String, String> propertiesToUse;
		switch (which) {
		case 0:
			propertiesToUse = parse(deploymentProperties);
			break;
		case 1:
			String extension = FilenameUtils.getExtension(propertiesFile.getName());
			Properties props = null;
			if (extension.equals("yaml") || extension.equals("yml")) {
				YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
				yamlPropertiesFactoryBean.setResources(new FileSystemResource(propertiesFile));
				yamlPropertiesFactoryBean.afterPropertiesSet();
				props = yamlPropertiesFactoryBean.getObject();
			}
			else {
				props = new Properties();
				try (FileInputStream fis = new FileInputStream(propertiesFile)) {
					props.load(fis);
				}
			}
			propertiesToUse = convert(props);
			break;
		case -1: // Neither option specified
			propertiesToUse = new HashMap<>(1);
			break;
		default:
			throw new AssertionError();
		}
		return propertiesToUse;
	}

	/**
	 * Ensure that deployment properties only have keys starting with {@code app.},
	 * {@code deployer.} or, {@code spring.cloud.scheduler.}. In case non supported key is found {@link IllegalArgumentException}
	 * is thrown.
	 *
	 * @param properties the properties to check
	 */
	public static void validateDeploymentProperties(Map<String, String> properties) {
		if (properties == null) {
			return;
		}
		for (Entry<String, String> property : properties.entrySet()) {
			String key = property.getKey();
			if (!key.startsWith("app.") && !key.startsWith("deployer.")
					&& !key.startsWith("version.")) {
				throw new IllegalArgumentException(
						"Only deployment property keys starting with 'app.' or 'deployer.' allowed, got '" + key + "'");
			}
		}
	}

	/**
	 * Ensure that deployment properties only have keys starting with {@code app.},
	 * {@code deployer.} or {@code version.}. In case non supported key is found
	 * {@link IllegalArgumentException} is thrown.
	 *
	 * @param properties the properties to check
	 */
	public static void validateSkipperDeploymentProperties(Map<String, String> properties) {
		if (properties == null) {
			return;
		}
		for (Entry<String, String> property : properties.entrySet()) {
			String key = property.getKey();
			if (!key.startsWith("app.") && !key.startsWith("deployer.") && !key.startsWith("version.")) {
				throw new IllegalArgumentException(
						"Only deployment property keys starting with 'app.' or 'deployer.'  or 'version.'" +
								" allowed, got '" + key + "'");
			}
		}
	}

	/**
	 * Retain only properties that are meant for the <em>deployer</em> of a given app (those
	 * that start with {@code deployer.[appname]} or {@code deployer.*}) and qualify all
	 * property values with the {@code spring.cloud.deployer.} prefix.
	 *
	 * @param input the deplopyment properties
	 * @param appName the app name
	 * @return deployment properties for the spepcific app name
	 */
	public static Map<String, String> extractAndQualifyDeployerProperties(Map<String, String> input, String appName) {
		final String wildcardPrefix = "deployer.*.";
		final int wildcardLength = wildcardPrefix.length();
		final String appPrefix = String.format("deployer.%s.", appName);
		final int appLength = appPrefix.length();

		// Using a TreeMap makes sure wildcard entries appear before app specific ones
		Map<String, String> result = new TreeMap<>(input).entrySet().stream()
				.filter(kv -> kv.getKey().startsWith(wildcardPrefix) || kv.getKey().startsWith(appPrefix))
				.collect(Collectors.toMap(kv -> kv.getKey().startsWith(wildcardPrefix)
								? "spring.cloud.deployer." + kv.getKey().substring(wildcardLength)
								: "spring.cloud.deployer." + kv.getKey().substring(appLength), Entry::getValue,
						(fromWildcard, fromApp) -> fromApp));
		logger.debug("extractAndQualifyDeployerProperties:{}", result);
		return result;
	}

	/**
	 * Retain all properties that are meant for the <em>deployer</em> of a given app
	 * (those that start with {@code deployer.[appname]} or {@code deployer.*}
	 * and qualify all property values with the
	 * {@code spring.cloud.deployer.} prefix.
	 *
	 * @param input   the deplopyment properties
	 * @param appName the app name
	 * @return deployment properties for the spepcific app name
	 */
	public static Map<String, String> qualifyDeployerProperties(Map<String, String> input, String appName) {
		final String wildcardPrefix = "deployer.*.";
		final int wildcardLength = wildcardPrefix.length();
		final String appPrefix = String.format("deployer.%s.", appName);
		final int appLength = appPrefix.length();

		// Using a TreeMap makes sure wildcard entries appear before app specific ones
		Map<String, String> resultDeployer = new TreeMap<>(input).entrySet().stream()
				.filter(kv -> kv.getKey().startsWith(wildcardPrefix) || kv.getKey().startsWith(appPrefix))
				.collect(Collectors.toMap(kv -> kv.getKey().startsWith(wildcardPrefix)
								? "spring.cloud.deployer." + kv.getKey().substring(wildcardLength)
								: "spring.cloud.deployer." + kv.getKey().substring(appLength), Entry::getValue,
						(fromWildcard, fromApp) -> fromApp));

		Map<String, String> resultApp = new TreeMap<>(input).entrySet().stream()
				.filter(kv -> !kv.getKey().startsWith(wildcardPrefix) && !kv.getKey().startsWith(appPrefix))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue,
						(fromWildcard, fromApp) -> fromApp));

		resultDeployer.putAll(resultApp);
		logger.debug("qualifyDeployerProperties:{}", resultDeployer);
		return resultDeployer;
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
		for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); ) {
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
			// todo: should key only be a "flag" as in: put(key, true)?
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
	public static List<String> removeQuoting(List<String> params) {
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
				if (param != null && !param.isEmpty()) {
					String p = removeQuoting(param.substring(start).trim());
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
