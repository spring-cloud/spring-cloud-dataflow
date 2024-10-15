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

package org.springframework.cloud.dataflow.rest.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.cloud.dataflow.core.DefinitionUtils;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.core.dsl.graph.Graph;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Sanitizes potentially sensitive keys for a specific command line arg.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
public class ArgumentSanitizer {
	private final static Logger logger = LoggerFactory.getLogger(ArgumentSanitizer.class);

	private static final String[] REGEX_PARTS = {"*", "$", "^", "+"};

	private static final String REDACTION_STRING = "******";

	private static final String[] KEYS_TO_SANITIZE = {"username", "password", "secret", "key", "token", ".*credentials.*",
			"vcap_services", "url"};

	private final static TypeReference<Map<String, Object>> mapTypeReference = new TypeReference<>() {
	};

	private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

	private final ObjectMapper jsonMapper = new ObjectMapper();

	private final Pattern[] keysToSanitize;

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
		// Oracle handles an empty string as a null.
		if (argument == null) {
			return "";
		}
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
	 * @param key   to check for sensitive words.
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
	 * Replaces the sensitive String values in the JobParameter value.
	 *
	 * @param jobParameters the original job parameters
	 * @return the sanitized job parameters
	 */
	public JobParameters sanitizeJobParameters(JobParameters jobParameters) {
		Map<String, JobParameter<?>> newJobParameters = new HashMap<>();
		jobParameters.getParameters().forEach((key, jobParameter) -> {
			String updatedKey = !jobParameter.isIdentifying() ? "-" + key : key;
			if (jobParameter.getType().isAssignableFrom(String.class)) {
				newJobParameters.put(updatedKey, new JobParameter<>(this.sanitize(key, jobParameter.toString()), String.class));
			} else {
				newJobParameters.put(updatedKey, jobParameter);
			}
		});
		return new JobParameters(newJobParameters);
	}

	/**
	 * Redacts sensitive property values in a task.
	 *
	 * @param taskDefinition the task definition to sanitize
	 * @return Task definition text that has sensitive data redacted.
	 */
	public String sanitizeTaskDsl(TaskDefinition taskDefinition) {
		if (!StringUtils.hasText(taskDefinition.getDslText())) {
			return taskDefinition.getDslText();
		}
		TaskParser taskParser = new TaskParser(taskDefinition.getTaskName(), taskDefinition.getDslText(), true, true);
		Graph graph = taskParser.parse().toGraph();
		graph.getNodes().forEach(node -> {
			if (node.properties != null) {
				node.properties.keySet().forEach(key -> node.properties.put(key,
						DefinitionUtils.autoQuotes(sanitize(key, node.properties.get(key)))));
			}
		});
		return graph.toDSLText();
	}

	/**
	 * For all sensitive properties (e.g. key names containing words like password, secret,
	 * key, token) replace the value with '*****' string
	 *
	 * @param properties to be sanitized
	 * @return sanitized properties
	 */
	public Map<String, String> sanitizeProperties(Map<String, String> properties) {
		if (!CollectionUtils.isEmpty(properties)) {
			final Map<String, String> sanitizedProperties = new LinkedHashMap<>(properties.size());
			for (Map.Entry<String, String> property : properties.entrySet()) {
				sanitizedProperties.put(property.getKey(), this.sanitize(property.getKey(), property.getValue()));
			}
			return sanitizedProperties;
		}
		return properties;
	}

	/**
	 * For all sensitive arguments (e.g. key names containing words like password, secret,
	 * key, token) replace the value with '*****' string
	 *
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

	public HttpHeaders sanitizeHeaders(HttpHeaders headers) {
		HttpHeaders result = new HttpHeaders();
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			List<String> values = entry.getValue();
			for (String value : values) {
				result.add(entry.getKey(), sanitize(entry.getKey(), value));
			}
		}
		return result;
	}

	/**
	 * Will replace sensitive string value in the Map with '*****'
	 *
	 * @param input to be sanitized
	 * @return the sanitized map.
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> sanitizeMap(Map<String, Object> input) {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<String, Object> entry : input.entrySet()) {
			if (entry.getValue() instanceof String) {
				result.put(entry.getKey(), sanitize(entry.getKey(), (String) entry.getValue()));
			} else if (entry.getValue() instanceof Map) {
				Map<String, Object> map = (Map<String, Object>) entry.getValue();
				result.put(entry.getKey(), sanitizeMap(map));
			} else {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Will replace the sensitive string fields with '*****'
	 *
	 * @param input to be sanitized
	 * @return The sanitized JSON string
	 * @throws JsonProcessingException from mapper.
	 */
	public String sanitizeJsonString(String input) throws JsonProcessingException {
		if (input == null) {
			return null;
		}
		Map<String, Object> data = jsonMapper.readValue(input, mapTypeReference);
		return jsonMapper.writeValueAsString(sanitizeMap(data));
	}

	/**
	 * Will replace the sensitive string fields with '*****'
	 *
	 * @param input to be sanitized
	 * @return The sanitized YAML string
	 * @throws JsonProcessingException from mapper
	 */
	public String sanitizeYamlString(String input) throws JsonProcessingException {
		if (input == null) {
			return null;
		}
		Map<String, Object> data = yamlMapper.readValue(input, mapTypeReference);
		return yamlMapper.writeValueAsString(sanitizeMap(data));
	}

	/**
	 * Will determine the type of data and treat as JSON or YAML to sanitize sensitive values.
	 *
	 * @param input to be sanitized
	 * @return the sanitized string
	 */
	@SuppressWarnings("StringConcatenationArgumentToLogCall")
	public String sanitizeJsonOrYamlString(String input) {
		if (input == null) {
			return null;
		}
		try { // Try parsing as JSON
			return sanitizeJsonString(input);
		} catch (Throwable x) {
			logger.trace("Cannot parse as JSON:" + x);
		}
		try {
			return sanitizeYamlString(input);
		} catch (Throwable x) {
			logger.trace("Cannot parse as YAML:" + x);
		}
		if (input.contains("\n")) {
			//noinspection DataFlowIssue
			return StringUtils.collectionToDelimitedString(sanitizeArguments(Arrays.asList(StringUtils.split(input, "\n"))), "\n");
		}
		if (input.contains("--")) {
			//noinspection DataFlowIssue
			return StringUtils.collectionToDelimitedString(sanitizeArguments(Arrays.asList(StringUtils.split(input, "--"))), "--");
		}
		return sanitize(input);
	}
}
