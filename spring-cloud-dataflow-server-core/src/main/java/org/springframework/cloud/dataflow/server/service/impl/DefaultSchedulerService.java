/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.bind.RelaxedNames;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.controller.WhitelistProperties;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.scheduler.spi.core.ScheduleInfo;
import org.springframework.cloud.scheduler.spi.core.ScheduleRequest;
import org.springframework.cloud.scheduler.spi.core.Scheduler;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link SchedulerService} interface. Provide service methods
 * for Scheduling tasks.
 *
 * @author Glenn Renfro
 */
public class DefaultSchedulerService implements SchedulerService {

	private static final String DATAFLOW_SERVER_URI_KEY = "dataflowServerUri";

	private CommonApplicationProperties commonApplicationProperties;
	private Scheduler scheduler;
	private TaskDefinitionRepository taskDefinitionRepository;
	private AppRegistryCommon registry;
	private final TaskConfigurationProperties taskConfigurationProperties;
	private final DataSourceProperties dataSourceProperties;
	private final String dataflowServerUri;
	private final WhitelistProperties whitelistProperties;

	public DefaultSchedulerService(CommonApplicationProperties commonApplicationProperties,
			Scheduler scheduler, TaskDefinitionRepository taskDefinitionRepository,
			AppRegistryCommon registry, ResourceLoader resourceLoader,
			TaskConfigurationProperties taskConfigurationProperties,
			DataSourceProperties dataSourceProperties, String dataflowServerUri,
			ApplicationConfigurationMetadataResolver metaDataResolver) {
		Assert.notNull(commonApplicationProperties, "commonApplicationProperties must not be null");
		Assert.notNull(scheduler, "scheduler must not be null");
		Assert.notNull(registry, "UriRegistry must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Assert.notNull(taskDefinitionRepository, "TaskDefinitionRepository must not be null");
		Assert.notNull(taskConfigurationProperties, "taskConfigurationProperties must not be null");
		Assert.notNull(dataSourceProperties, "DataSourceProperties must not be null");
		Assert.notNull(metaDataResolver, "metaDataResolver must not be null");

		this.dataSourceProperties = dataSourceProperties;
		this.commonApplicationProperties = commonApplicationProperties;
		this.scheduler = scheduler;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.registry = registry;
		this.taskConfigurationProperties = taskConfigurationProperties;
		this.dataflowServerUri = dataflowServerUri;
		this.whitelistProperties = new WhitelistProperties(metaDataResolver);
	}

	@Override
	public void schedule(String scheduleName, String taskDefinitionName, Map<String, String> taskDeploymentProperties,
			List<String> commandLineArgs) {
		Assert.hasText(taskDefinitionName, "The provided taskName must not be null or empty.");
		Assert.notNull(taskDeploymentProperties, "The provided runtimeProperties must not be null.");
		TaskDefinition taskDefinition = this.taskDefinitionRepository.findOne(taskDefinitionName);
		if (taskDefinition == null) {
			throw new NoSuchTaskDefinitionException(taskDefinitionName);
		}
		TaskParser taskParser = new TaskParser(taskDefinition.getName(), taskDefinition.getDslText(), true, true);
		TaskNode taskNode = taskParser.parse();
		// if composed task definition replace definition with one composed task
		// runner and executable graph.
		if (taskNode.isComposed()) {
			taskDefinition = new TaskDefinition(taskDefinition.getName(),
					TaskServiceUtils.createComposedTaskDefinition(
							taskNode.toExecutableDSL(), this.taskConfigurationProperties));
			taskDeploymentProperties = TaskServiceUtils.establishComposedTaskProperties(taskDeploymentProperties, taskNode);
		}

		AppRegistration appRegistration = this.registry.find(taskDefinition.getRegisteredAppName(),
				ApplicationType.task);
		Assert.notNull(appRegistration, "Unknown task app: " + taskDefinition.getRegisteredAppName());
		Resource metadataResource = this.registry.getAppMetadataResource(appRegistration);

		taskDefinition = TaskServiceUtils.updateTaskProperties(taskDefinition, this.dataSourceProperties);

		Map<String, String> appDeploymentProperties = new HashMap<>(commonApplicationProperties.getTask());
		appDeploymentProperties.putAll(
				TaskServiceUtils.extractAppProperties(taskDefinition.getRegisteredAppName(), taskDeploymentProperties));

		Map<String, String> deployerDeploymentProperties = DeploymentPropertiesUtils
				.extractAndQualifyDeployerProperties(taskDeploymentProperties, taskDefinition.getRegisteredAppName());
		if (StringUtils.hasText(this.dataflowServerUri) && taskNode.isComposed()) {
			updateDataFlowUriIfNeeded(appDeploymentProperties, commandLineArgs);
		}
		AppDefinition revisedDefinition = TaskServiceUtils.mergeAndExpandAppProperties(taskDefinition, metadataResource,
				appDeploymentProperties, whitelistProperties);
		ScheduleRequest scheduleRequest = new ScheduleRequest(revisedDefinition,
				TaskServiceUtils.extractSchedulerProperties(taskDefinition.getRegisteredAppName(), taskDeploymentProperties),
				deployerDeploymentProperties, scheduleName, getTaskResource(taskDefinitionName));
		this.scheduler.schedule(scheduleRequest);
	}

	@Override
	public void unschedule(String scheduleName) {
		this.scheduler.unschedule(scheduleName);
	}

	@Override
	public List<ScheduleInfo> list(Pageable pageable, String taskDefinitionName) {
		// Need to add support for pagination
		return scheduler.list(taskDefinitionName);
	}

	@Override
	public List<ScheduleInfo> list(Pageable pageable) {
		// Need to add support for pagination
		return scheduler.list();
	}

	private Resource getTaskResource(String taskDefinitionName ) {
		TaskDefinition taskDefinition = this.taskDefinitionRepository.findOne(taskDefinitionName);
		if (taskDefinition == null) {
			throw new NoSuchTaskDefinitionException(taskDefinitionName);
		}

		AppRegistration appRegistration = this.registry.find(taskDefinition.getRegisteredAppName(),
				ApplicationType.task);
		Assert.notNull(appRegistration, "Unknown task app: " + taskDefinition.getRegisteredAppName());
		return this.registry.getAppResource(appRegistration);
	}

	private void updateDataFlowUriIfNeeded(Map<String, String> appDeploymentProperties, List<String> commandLineArgs) {
		if (StringUtils.isEmpty(this.dataflowServerUri)) {
			return;
		}
		RelaxedNames relaxedNames = new RelaxedNames(DATAFLOW_SERVER_URI_KEY);
		for (String dataFlowUriKey : relaxedNames) {
			if (appDeploymentProperties.containsKey(dataFlowUriKey)) {
				return;
			}
			for (String cmdLineArg : commandLineArgs) {
				if (cmdLineArg.contains(dataFlowUriKey + "=")) {
					return;
				}
			}
		}
		appDeploymentProperties.put(DATAFLOW_SERVER_URI_KEY, this.dataflowServerUri);
	}

}
