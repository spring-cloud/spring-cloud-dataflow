/*
 * Copyright 2019-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskPlatformFactory;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.controller.VisibleProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Create a {@link AppDeploymentRequest} from a
 * {@link org.springframework.cloud.dataflow.core.TaskDefinition} and deployment
 * properties map.
 *
 * @author Glenn Renfro
 */
public class TaskAppDeploymentRequestCreator {

	private static final Logger logger = LoggerFactory.getLogger(TaskAppDeploymentRequestCreator.class);

	private static final String TASK_EXECUTION_KEY = "--spring.cloud.task.executionid=";

	private static final String PLATFORM_NAME_KEY = "--spring.cloud.data.flow.platformname=";

	private static final String TASK_EXECUTION_APP_NAME = "--spring.cloud.data.flow.taskappname=";

	private final CommonApplicationProperties commonApplicationProperties;

	private final VisibleProperties visibleProperties;

	private final String dataflowServerUri;

	/**
	 * Initializes the {@link TaskAppDeploymentRequestCreator}.
	 *
	 * @param commonApplicationProperties the common application properties for all tasks
	 * @param metaDataResolver the metadata resolver
	 * @param dataflowServerUri the URI of the data flow server
	 */
	public TaskAppDeploymentRequestCreator(CommonApplicationProperties commonApplicationProperties,
			ApplicationConfigurationMetadataResolver metaDataResolver,
			String dataflowServerUri) {
		Assert.notNull(commonApplicationProperties, "commonApplicationProperties must not be null");
		Assert.notNull(metaDataResolver, "metaDataResolver must not be null");

		this.commonApplicationProperties = commonApplicationProperties;
		this.visibleProperties = new VisibleProperties(metaDataResolver);
		this.dataflowServerUri = dataflowServerUri;
	}


	/**
	 * Create a {@link AppDeploymentRequest} from the provided
	 * {@link TaskExecutionInformation}, {@link TaskExecution}.
	 *
	 * @param taskExecution the task execution
	 * @param taskExecutionInformation the task execution info
	 * @param commandLineArgs the command line args
	 * @param platformName the platform name
	 * @param platformType the platform type
	 *
	 * @return an instance of {@link AppDeploymentRequest}
	 */
	public AppDeploymentRequest createRequest(
			TaskExecution taskExecution,
			TaskExecutionInformation taskExecutionInformation,
			List<String> commandLineArgs,
			String platformName,
			String platformType) {
		TaskDefinition taskDefinition = taskExecutionInformation.getTaskDefinition();
		String registeredAppName = taskDefinition.getRegisteredAppName();
		Map<String, String> appDeploymentProperties = new HashMap<>(commonApplicationProperties.getTask());
		String label = TaskServiceUtils.labelForSimpleTask(taskDefinition.getName(), taskDefinition.getDslText());
		appDeploymentProperties.putAll(
				TaskServiceUtils.extractAppProperties(
						taskExecutionInformation.isComposed() ? "composed-task-runner" : registeredAppName,
						label,
						taskExecutionInformation.getTaskDeploymentProperties()));

		// Merge the common properties defined via the spring.cloud.dataflow.common-properties.task-resource file.
		// Doesn't override existing properties!
		// The placeholders defined in the task-resource file are not resolved by SCDF but passed to the apps as they are.
		TaskServiceUtils.contributeCommonProperties(this.commonApplicationProperties.getTaskResourceProperties(),
				appDeploymentProperties, "common");
		TaskServiceUtils.contributeCommonProperties(this.commonApplicationProperties.getTaskResourceProperties(),
				appDeploymentProperties, platformType);

		Map<String, String> props = taskExecutionInformation.getTaskDeploymentProperties();
		if (!taskExecutionInformation.isComposed()) {
			props = props.entrySet().stream()
					.collect(Collectors.toMap(e -> {
						return e.getKey().replaceFirst("^deployer\\." + label, "deployer." + registeredAppName);
					}, e -> e.getValue()));
		}

		// Need to keep all properties around, not just 'deployer.*'
		// as those are a source to restore app specific props
		Map<String, String> deployerDeploymentProperties = DeploymentPropertiesUtils.qualifyDeployerProperties(props,
				taskExecutionInformation.isComposed() ? "composed-task-runner" : registeredAppName);

		if (StringUtils.hasText(this.dataflowServerUri) && taskExecutionInformation.isComposed()) {
			TaskServiceUtils.updateDataFlowUriIfNeeded(this.dataflowServerUri, appDeploymentProperties,
					commandLineArgs);
		}
		if (taskExecutionInformation.isComposed()) {
			appDeploymentProperties.put("platform-name", platformName);
		}
		AppDefinition revisedDefinition = TaskServiceUtils.mergeAndExpandAppProperties(taskDefinition,
				taskExecutionInformation.getMetadataResource(),
				appDeploymentProperties, this.visibleProperties);

		List<String> updatedCmdLineArgs = (taskExecutionInformation.isComposed()) ? this.updateCommandLineArgs(commandLineArgs,
				taskExecution, platformName, registeredAppName) : this.updateCommandLineArgs(commandLineArgs,
				taskExecution, platformName);
		if(TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE.equals(platformType)) {
			//Extracting boot version from app deployer property ex: 'app.timestamp3.spring.cloud.deployer.bootVersion=3'
			String bootVersion = deployerDeploymentProperties.entrySet().stream()
				.filter((e) -> e.getKey().endsWith(".bootVersion"))
				.findFirst().map(Map.Entry::getValue)
				.orElse(null);
			logger.debug("Resolved default bootVersion = " + bootVersion);
		}
		AppDeploymentRequest request = new AppDeploymentRequest(revisedDefinition,
				taskExecutionInformation.getAppResource(),
				deployerDeploymentProperties, updatedCmdLineArgs);

		logger.debug("Created AppDeploymentRequest = " + request.toString() + " AppDefinition = "
				+ request.getDefinition().toString());
		return request;
	}

	private List<String> updateCommandLineArgs(List<String> commandLineArgs, TaskExecution taskExecution, String platformName) {
		List<String> results = new ArrayList();
		commandLineArgs.stream()
				.filter(arg -> !arg.startsWith(TASK_EXECUTION_KEY))
				.forEach(results::add);
		results.add(TASK_EXECUTION_KEY + taskExecution.getExecutionId());
		return results;
	}

	private List<String> updateCommandLineArgs(List<String> commandLineArgs, TaskExecution taskExecution, String platformName, String appName) {
		List<String> results = new ArrayList();
		commandLineArgs.stream()
				.filter(arg -> !arg.startsWith(TASK_EXECUTION_KEY)
						&& !arg.startsWith(TASK_EXECUTION_APP_NAME))
				.forEach(results::add);

		results.add(TASK_EXECUTION_KEY + taskExecution.getExecutionId());
		results.add(TASK_EXECUTION_APP_NAME + appName);
		return results;
	}
}
