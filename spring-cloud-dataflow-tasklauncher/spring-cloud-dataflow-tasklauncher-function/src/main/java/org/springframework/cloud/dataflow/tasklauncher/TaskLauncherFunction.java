/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.cloud.dataflow.tasklauncher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.CurrentTaskExecutionsResource;
import org.springframework.cloud.dataflow.rest.resource.LaunchResponseResource;
import org.springframework.cloud.dataflow.rest.resource.LauncherResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.hateoas.PagedModel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 *
 * A {@link Function} that submits a task {@link LaunchRequest} to a Data Flow server.
 * This will check if the Data Flow task platform is at capacity. If not, will submit the
 * task launch request, otherwise it will return and log a warning message.
 *
 * @author David Turanski
 **/
public class TaskLauncherFunction implements Function<LaunchRequest, Optional<LaunchResponse>>, InitializingBean {
	private static final Log log = LogFactory.getLog(TaskLauncherFunction.class);

	static final String TASK_PLATFORM_NAME = "spring.cloud.dataflow.task.platformName";

	private final TaskOperations taskOperations;

	private String platformName = "default";

	public TaskLauncherFunction(TaskOperations taskOperations) {
		Assert.notNull(taskOperations, "`taskOperations` cannot be null.");
		this.taskOperations = taskOperations;
	}

	/**
	 *
	 * @param launchRequest the task launch request for the Data Flow server.
	 * @return an {@code Optional<Long>} containing the task Id if the request is accepted or
	 * empty otherwise.
	 */
	@Override
	public Optional<LaunchResponse> apply(LaunchRequest launchRequest) {
		if (platformIsAcceptingNewTasks()) {
			return Optional.of(launchTask(launchRequest));
		}
		log.warn(String.format("Platform is at capacity. Did not submit task launch request for task %s.",
				launchRequest.getTaskName()));
		return Optional.empty();
	}

	public boolean platformIsAcceptingNewTasks() {

		boolean availableForNewTasks;
		int maximumTaskExecutions = 0;
		int runningExecutionCount = 0;

		List<String> currentPlatforms = new ArrayList<>();

		boolean validPlatform = false;
		for (CurrentTaskExecutionsResource currentTaskExecutionsResource : taskOperations.currentTaskExecutions()) {
			if (currentTaskExecutionsResource.getName().equals(platformName)) {
				maximumTaskExecutions = currentTaskExecutionsResource.getMaximumTaskExecutions();
				runningExecutionCount = currentTaskExecutionsResource.getRunningExecutionCount();
				validPlatform = true;
			}
			currentPlatforms.add(currentTaskExecutionsResource.getName());
		}

		// Verify for each request as configuration may have changed on server.
		assertValidPlatform(validPlatform, currentPlatforms);

		availableForNewTasks = runningExecutionCount < maximumTaskExecutions;
		if (!availableForNewTasks) {
			log.warn(String.format(
					"The data Flow task platform %s has reached its concurrent task execution limit: (%d)",
					platformName,
					maximumTaskExecutions));
		}

		return availableForNewTasks;

	}

	private LaunchResponse launchTask(LaunchRequest request) {
		String requestPlatformName = request.getDeploymentProperties().get(TASK_PLATFORM_NAME);
		if (StringUtils.hasText(requestPlatformName) && !platformName.equals(requestPlatformName)) {
			throw new IllegalStateException(
					String.format(
							"Task Launch request for Task %s contains deployment property '%s=%s' which does not " +
									"match the platform configured for the Task Launcher: '%s'",
							request.getTaskName(),
							TASK_PLATFORM_NAME,
							request.getDeploymentProperties().get(TASK_PLATFORM_NAME),
							platformName));
		}
		log.info(String.format("Launching Task %s on platform %s", request.getTaskName(), platformName));
		LaunchResponseResource response = taskOperations.launch(request.getTaskName(),
				enrichDeploymentProperties(request.getDeploymentProperties()),
				request.getCommandlineArguments());
		log.info(String.format("Launched Task %s - task ID is %d", request.getTaskName(), response.getTaskId()));
		return new LaunchResponse(response.getTaskId(), response.getSchemaTarget());
	}

	private Map<String, String> enrichDeploymentProperties(Map<String, String> deploymentProperties) {
		if (!deploymentProperties.containsKey(TASK_PLATFORM_NAME)) {
			Map<String, String> enrichedProperties = new HashMap<>();
			enrichedProperties.putAll(deploymentProperties);
			enrichedProperties.put(TASK_PLATFORM_NAME, platformName);
			return enrichedProperties;
		}
		return deploymentProperties;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	@Override
	public void afterPropertiesSet() {
		PagedModel<LauncherResource> launchers = taskOperations.listPlatforms();

		boolean validPlatform = false;
		List<String> currentPlatforms = new ArrayList<>();

		for (LauncherResource launcherResource : launchers) {
			currentPlatforms.add(launcherResource.getName());
			if (launcherResource.getName().equals(platformName)) {
				validPlatform = true;
			}
		}

		assertValidPlatform(validPlatform, currentPlatforms);
	}

	private void assertValidPlatform(boolean validPlatform, List<String> currentPlatforms) {
		Assert.notEmpty(currentPlatforms, "The Data Flow Server has no task platforms configured");

		Assert.isTrue(validPlatform, String.format(
				"The task launcher's platform name '%s' does not match one of the Data Flow server's configured task "
						+ "platforms: [%s].",
				platformName, StringUtils.collectionToCommaDelimitedString(currentPlatforms)));
	}
}
