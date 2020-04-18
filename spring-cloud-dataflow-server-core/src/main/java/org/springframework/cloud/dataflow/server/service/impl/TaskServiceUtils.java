/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.context.encrypt.EncryptorFactory;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.TaskApp;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.server.controller.WhitelistProperties;
import org.springframework.cloud.dataflow.server.support.RelaxedNames;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Provides utility methods for setting up tasks for execution.
 */
public class TaskServiceUtils {
	private static final String DATAFLOW_SERVER_URI_KEY = "dataflowServerUri";

	private static final String ENCRYPT_KEY = "ENCRYPT_KEY";

	private static final String CIPHER = "{cipher}";

	/**
	 * Parses the task DSL to see if it is a composed task definition
	 * @param dsl task DSL
	 * @return true if composed, false otherwise.
	 */
	public static boolean isComposedTaskDefinition(String dsl) {
		Assert.hasText(dsl, "dsl must not be empty nor null");
		TaskParser taskParser = new TaskParser("__dummy", dsl, true, true);
		return taskParser.parse().isComposed();
	}

	/**
	 * Creates a properly formatted CTR definition based on the graph provided.
	 * @param graph the graph for the CTR to execute.
	 * @param taskConfigurationProperties the properties that contain the name
	 * of the CTR app to be launched.
	 * @return String containing the CTR task definition.
	 */
	public static String createComposedTaskDefinition(String graph,
			TaskConfigurationProperties taskConfigurationProperties) {
			return createComposedTaskDefinition(null, graph, taskConfigurationProperties);
	}

	/**
	 * Creates a properly formatted CTR definition based on the graph provided.
	 * @param alternateComposedTaskRunnerName a ctr name to be used instead of the default.
	 * @param graph the graph for the CTR to execute.
	 * @param taskConfigurationProperties the properties that contain the name
	 * of the CTR app to be launched.
	 * @return String containing the CTR task definition.
	 */
	public static String createComposedTaskDefinition(String alternateComposedTaskRunnerName, String graph,
			TaskConfigurationProperties taskConfigurationProperties) {
		Assert.hasText(graph, "graph must not be empty or null");
		Assert.notNull(taskConfigurationProperties,
				"taskConfigurationProperties must not be null");
		Assert.hasText(taskConfigurationProperties.getComposedTaskRunnerName(),
				"taskConfigurationProperties.composedTaskRunnerName must not be null");
		String composedTaskRunnerName = taskConfigurationProperties.getComposedTaskRunnerName();
		if(StringUtils.hasText(alternateComposedTaskRunnerName)) {
			composedTaskRunnerName = alternateComposedTaskRunnerName;
		}
		return String.format("%s --graph=\"%s\"", composedTaskRunnerName, graph);
	}

	/**
	 * Establish the app and deployment properties to be used for a CTR.
	 * @param taskDeploymentProperties properties to be parsed for the CTR.
	 * @param taskNode containing the tasks apps to be executed by the CTR
	 * @return properties that can be consumed by the CTR.
	 */
	public static Map<String, String> establishComposedTaskProperties(
			Map<String, String> taskDeploymentProperties,
			TaskNode taskNode) {
		Assert.notNull(taskDeploymentProperties, "taskDeploymentProperties must not be null");
		Assert.notNull(taskNode, "taskNode must not be null");
		String result = "";
		for (TaskApp subTask : taskNode.getTaskApps()) {
			result = updateProperties(taskNode, subTask, taskDeploymentProperties, result, "app");
			result = updateProperties(taskNode, subTask, taskDeploymentProperties, result, "deployer");
		}
		if (result.length() != 0) {
			taskDeploymentProperties.put("app.composed-task-runner.composed-task-properties", result);
		}
		return taskDeploymentProperties;
	}

	/**
	 * Updates the task definition with the datasource properties.
	 * @param taskDefinition the {@link TaskDefinition} to be updated.
	 * @param dataSourceProperties the dataSource properties used by SCDF.
	 * @return the updated {@link TaskDefinition}
	 */
	public static TaskDefinition updateTaskProperties(TaskDefinition taskDefinition,
			DataSourceProperties dataSourceProperties) {
		Assert.notNull(taskDefinition, "taskDefinition must not be null");
		Assert.notNull(dataSourceProperties, "dataSourceProperties must not be null");
		TaskDefinition.TaskDefinitionBuilder builder = TaskDefinition.TaskDefinitionBuilder.from(taskDefinition);
		builder.setProperty("spring.datasource.url", dataSourceProperties.getUrl());
		builder.setProperty("spring.datasource.username", dataSourceProperties.getUsername());
		// password may be empty
		if (StringUtils.hasText(dataSourceProperties.getPassword())) {
			builder.setProperty("spring.datasource.password", StringUtils.isEmpty(System.getenv(ENCRYPT_KEY))
					? dataSourceProperties.getPassword()
					: CIPHER + new EncryptorFactory().create(System.getenv(ENCRYPT_KEY))
					.encrypt(dataSourceProperties.getPassword()));
		}
		builder.setProperty("spring.datasource.driverClassName", dataSourceProperties.getDriverClassName());
		builder.setTaskName(taskDefinition.getTaskName());
		builder.setDslText(taskDefinition.getDslText());
		return builder.build();
	}

	/**
	 * Extract app properties from the deployment properties by task name.
	 * @param name the task app name to search for in the deployment properties.
	 * @param taskDeploymentProperties the properties for the task deployment.
	 * @return a map containing the app properties for a task.
	 */
	public static Map<String, String> extractAppProperties(String name, Map<String, String> taskDeploymentProperties) {
		Assert.hasText(name, "name must not be empty or null");
		Assert.notNull(taskDeploymentProperties, "taskDeploymentProperties must not be null");
		return extractPropertiesByPrefix("app", name, taskDeploymentProperties);
	}

	/**
	 * Return a copy of a given task definition where short form parameters have been expanded
	 * to their long form (amongst the whitelisted supported properties of the app) if
	 * applicable.
	 * @param original the task definition with the original set of properties.
	 * @param resource the resource to be used for identifying white listed properties.
	 * @param appDeploymentProperties the app deployment properties to be added to the {@link AppDefinition}.
	 * @param whitelistProperties util for formatting white listed properties properly.
	 * @return fully qualified {@link AppDefinition}.
	 */
	public static AppDefinition mergeAndExpandAppProperties(TaskDefinition original,
			Resource resource,
			Map<String, String> appDeploymentProperties,
			WhitelistProperties whitelistProperties) {
		Assert.notNull(original, "original must not be null");
		Assert.notNull(appDeploymentProperties, "appDeploymentProperties must not be null");
		Assert.notNull(whitelistProperties, "whitelistProperties must not be null");
		Map<String, String> merged = new HashMap<>(original.getProperties());
		merged.putAll(appDeploymentProperties);
		merged = whitelistProperties.qualifyProperties(merged, resource);
		return new AppDefinition(original.getName(), merged);
	}

	public static void updateDataFlowUriIfNeeded(String dataflowServerUri,
			Map<String, String> appDeploymentProperties, List<String> commandLineArgs) {
		updateDataFlowUriIfNeeded(DATAFLOW_SERVER_URI_KEY, dataflowServerUri, appDeploymentProperties, commandLineArgs);
	}

	public static void updateDataFlowUriIfNeeded(String dataFlowServerUriKey, String dataflowServerUri,
			Map<String, String> appDeploymentProperties, List<String> commandLineArgs) {
		Assert.notNull(appDeploymentProperties, "appDeploymentProperties must not be null");
		Assert.notNull(commandLineArgs, "commandLineArgs must not be null");
		if (!StringUtils.isEmpty(dataflowServerUri)) {
			RelaxedNames relaxedNames = new RelaxedNames(dataFlowServerUriKey);
			boolean isPutDataFlowServerUriKey = true;
			for (String dataFlowUriKey : relaxedNames) {
				if (appDeploymentProperties.containsKey(dataFlowUriKey)) {
					isPutDataFlowServerUriKey = false;
					break;
				}
				for (String cmdLineArg : commandLineArgs) {
					if (cmdLineArg.contains(dataFlowUriKey + "=")) {
						return;
					}
				}
			}
			if(isPutDataFlowServerUriKey) {
				appDeploymentProperties.put(dataFlowServerUriKey, dataflowServerUri);
			}
		}
	}

	private static Map<String, String> extractPropertiesByPrefix(String type,
			String name, Map<String, String> taskDeploymentProperties) {
		final String prefix = type + "." + name + ".";
		return taskDeploymentProperties.entrySet().stream()
				.filter(kv -> kv.getKey().startsWith(prefix))
				.collect(Collectors.toMap(kv -> kv.getKey().substring(prefix.length()), kv -> kv.getValue()));
	}

	private static String updateProperties(TaskNode taskNode, TaskApp subTask, Map<String, String> taskDeploymentProperties,
			String result, String prefix) {
		String subTaskName = String.format("%s.%s-%s.", prefix, taskNode.getName(),
				(subTask.getLabel() == null) ? subTask.getName() : subTask.getLabel());
		String scdfTaskName = String.format("%s.%s.%s.", prefix, taskNode.getName(),
				(subTask.getLabel() == null) ? subTask.getName() : subTask.getLabel());
		Set<String> propertyKeys = taskDeploymentProperties.keySet().
				stream().filter(taskProperty -> taskProperty.startsWith(scdfTaskName))
				.collect(Collectors.toSet());
		for (String taskProperty : propertyKeys) {
			if (result.length() != 0) {
				result += ", ";
			}
			result += String.format("%s%s.%s.%s=%s", subTaskName, prefix,
					subTask.getName(),
					taskProperty.substring(subTaskName.length()),
					taskDeploymentProperties.get(taskProperty));
			taskDeploymentProperties.remove(taskProperty);
		}
		return result;
	}
}
