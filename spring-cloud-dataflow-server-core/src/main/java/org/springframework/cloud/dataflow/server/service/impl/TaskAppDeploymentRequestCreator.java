/*
 * Copyright 2019 the original author or authors.
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.controller.WhitelistProperties;
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

	private final CommonApplicationProperties commonApplicationProperties;

	private final WhitelistProperties whitelistProperties;

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
		this.whitelistProperties = new WhitelistProperties(metaDataResolver);
		this.dataflowServerUri = dataflowServerUri;
	}



	/**
	 * Create a {@link AppDeploymentRequest} from the provided
	 * {@link TaskExecutionInformation}, {@link TaskExecution}.
	 * @return an instance of {@link AppDeploymentRequest}
	 */
	public AppDeploymentRequest createRequest(
			TaskExecution taskExecution,
			TaskExecutionInformation taskExecutionInformation,
			List<String> commandLineArgs) {
		TaskDefinition taskDefinition = taskExecutionInformation.getTaskDefinition();
		String registeredAppName = taskDefinition.getRegisteredAppName();
		Map<String, String> appDeploymentProperties = new HashMap<>(commonApplicationProperties.getTask());
		appDeploymentProperties.putAll(
				TaskServiceUtils.extractAppProperties(registeredAppName,
						taskExecutionInformation.getTaskDeploymentProperties()));

		Map<String, String> deployerDeploymentProperties = DeploymentPropertiesUtils
				.extractAndQualifyDeployerProperties(taskExecutionInformation.getTaskDeploymentProperties(),
						registeredAppName);
		if (StringUtils.hasText(this.dataflowServerUri) && taskExecutionInformation.isComposed()) {
			TaskServiceUtils.updateDataFlowUriIfNeeded(this.dataflowServerUri, appDeploymentProperties,
					commandLineArgs);
		}
		AppDefinition revisedDefinition = TaskServiceUtils.mergeAndExpandAppProperties(taskDefinition,
				taskExecutionInformation.getMetadataResource(),
				appDeploymentProperties, this.whitelistProperties);

		List<String> updatedCmdLineArgs = this.updateCommandLineArgs(commandLineArgs, taskExecution);
		AppDeploymentRequest request = new AppDeploymentRequest(revisedDefinition,
				taskExecutionInformation.getAppResource(),
				deployerDeploymentProperties, updatedCmdLineArgs);

		logger.debug("Created AppDeploymentRequest = " + request.toString() + " AppDefinition = "
				+ request.getDefinition().toString());
		return request;
	}

	private List<String> updateCommandLineArgs(List<String> commandLineArgs, TaskExecution taskExecution) {
		return Stream
				.concat(commandLineArgs.stream().filter(a -> !a.startsWith("--spring.cloud.task.executionid=")),
						Stream.of("--spring.cloud.task.executionid=" + taskExecution.getExecutionId()))
				.collect(Collectors.toList());
	}
}
