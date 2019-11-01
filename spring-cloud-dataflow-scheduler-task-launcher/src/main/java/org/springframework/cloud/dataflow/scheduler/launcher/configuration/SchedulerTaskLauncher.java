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

package org.springframework.cloud.dataflow.scheduler.launcher.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.rest.client.DataFlowClientException;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.LauncherResource;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.hateoas.PagedModel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Sends a request to the Spring Cloud Data Flow server to launch a task.
 *
 * @author Glenn Renfro
 **/
public class SchedulerTaskLauncher {

	static final String TASK_PLATFORM_NAME = "spring.cloud.dataflow.task.platformName";

	public final static String COMMAND_ARGUMENT_PREFIX = "cmdarg";

	private static final Log log = LogFactory.getLog(SchedulerTaskLauncher.class);

	private final String taskName;

	private final String platformName;

	private final SchedulerTaskLauncherProperties schedulerTaskLauncherProperties;

	private final TaskOperations taskOperations;

	private Environment environment;

	public SchedulerTaskLauncher(TaskOperations taskOperations,
			SchedulerTaskLauncherProperties schedulerTaskLauncherProperties,
			Environment environment) {
		Assert.notNull(taskOperations, "`taskOperations` must not be null");
		Assert.notNull(schedulerTaskLauncherProperties, "`schedulerTaskLauncherProperties` must not be null");
		Assert.notNull(environment, "`environment` must not be null");
		Assert.hasText(schedulerTaskLauncherProperties.getTaskName(), "`taskName` must not be empty or null");
		Assert.hasText(schedulerTaskLauncherProperties.getPlatformName(), "`platformName` must not be empty or null");

		this.taskOperations = taskOperations;
		this.taskName = schedulerTaskLauncherProperties.getTaskName();
		this.platformName = schedulerTaskLauncherProperties.getPlatformName();
		this.schedulerTaskLauncherProperties = schedulerTaskLauncherProperties;
		this.environment = environment;
	}

	public void launchTask(String... args) {
		verifyTaskPlatform(this.taskOperations);
		List<String> argList = extractLaunchArgs(args);
		try {
			log.info(String.format("Launching Task %s on the %s platform.", this.taskName, this.platformName));
			this.taskOperations.launch(this.taskName, enrichDeploymentProperties(getDeploymentProperties()), argList, null);
		}
		catch (DataFlowClientException e) {
			throw new SchedulerTaskLauncherException(e);
		}
	}

	private List<String> extractLaunchArgs(String... args) {
		List<String> result = new ArrayList<>();
		for(String arg : args) {
			String prefix = COMMAND_ARGUMENT_PREFIX + "." +
					this.schedulerTaskLauncherProperties.getTaskLauncherPropertyPrefix() + ".";

			if (arg.startsWith(prefix)) {
				result.add(arg.substring(prefix.length()));
			}
		}
		return result;
	}

	private Map<String, String> enrichDeploymentProperties(Map<String, String> deploymentProperties) {
		Map<String, String> enrichedProperties = new HashMap<>(deploymentProperties);
		if (!deploymentProperties.containsKey(TASK_PLATFORM_NAME)) {
			enrichedProperties.put(TASK_PLATFORM_NAME, this.platformName);
		}
		return enrichedProperties;
	}


	private Map<String, String> getDeploymentProperties() {
		Map<String, String> props = new HashMap<>();
		final String taskLauncherPrefix = this.schedulerTaskLauncherProperties.getTaskLauncherPropertyPrefix() + ".";
		MutablePropertySources propSrcs = ((AbstractEnvironment) this.environment).getPropertySources();
		StreamSupport.stream(propSrcs.spliterator(), false)
				.filter(ps -> ps instanceof EnumerablePropertySource)
				.map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
				.flatMap(Arrays::<String>stream)
				.filter(propName -> propName.startsWith(taskLauncherPrefix))
				.forEach(propName -> {
					String trimmedPropName = propName.substring(taskLauncherPrefix.length());
					props.put(trimmedPropName, this.environment.getProperty(propName));
				});
		return props;
	}

	public void verifyTaskPlatform(TaskOperations taskOperations) {
		PagedModel<LauncherResource> launchers = taskOperations.listPlatforms();

		boolean validPlatform = false;
		List<String> currentPlatforms = new ArrayList<>();

		for (LauncherResource launcherResource : launchers) {
			currentPlatforms.add(launcherResource.getName());
			if (launcherResource.getName().equals(this.platformName)) {
				validPlatform = true;
				break;
			}
		}

		assertValidPlatform(validPlatform, currentPlatforms);
	}

	private void assertValidPlatform(boolean validPlatform, List<String> currentPlatforms) {
		Assert.notEmpty(currentPlatforms, "The Data Flow Server has no task platforms configured");

		Assert.isTrue(validPlatform, String.format(
				"The task launcher's platform name '%s' does not match one of the Data Flow server's configured task "
						+ "platforms: [%s].",
				this.platformName, StringUtils.collectionToCommaDelimitedString(currentPlatforms)));
	}

}
