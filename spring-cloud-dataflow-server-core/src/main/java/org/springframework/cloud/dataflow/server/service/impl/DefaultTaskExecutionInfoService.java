/*
 * Copyright 2016-2018 the original author or authors.
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

import java.util.Map;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link DefaultTaskExecutionInfoService} interface.
 * Provide service methods for {@link DefaultTaskExecutionService} about task definitions
 * and execution related information.
 *
 * @author Michael Minella
 * @author Marius Bogoevici
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Janne Valkealahti
 * @author Gunnar Hillert
 * @author Thomas Risberg
 * @author Ilayaperumal Gopinathan
 * @author Michael Wirth
 * @author David Turanski
 * @author Daniel Serleg
 */
public class DefaultTaskExecutionInfoService implements TaskExecutionInfoService {

	private final DataSourceProperties dataSourceProperties;

	/**
	 * The {@link AppRegistryService} this service will use to look up task app URIs.
	 */
	private final AppRegistryService appRegistryService;

	/**
	 * Used to read TaskExecutions.
	 */
	private final TaskExplorer taskExplorer;

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final TaskConfigurationProperties taskConfigurationProperties;

	/**
	 * Initializes the {@link DefaultTaskExecutionInfoService}.
	 *
	 * @param dataSourceProperties the data source properties.
	 * @param appRegistryService URI registry this service will use to look up app URIs.
	 * @param taskExplorer the explorer this service will use to lookup task executions
	 * @param taskDefinitionRepository the {@link TaskDefinitionRepository} this service will
	 *     use for task CRUD operations.
	 * @param taskConfigurationProperties the properties used to define the behavior of tasks
	 */
	public DefaultTaskExecutionInfoService(DataSourceProperties dataSourceProperties,
			AppRegistryService appRegistryService,
			TaskExplorer taskExplorer,
			TaskDefinitionRepository taskDefinitionRepository,
			TaskConfigurationProperties taskConfigurationProperties) {
		Assert.notNull(dataSourceProperties, "DataSourceProperties must not be null");
		Assert.notNull(appRegistryService, "AppRegistryService must not be null");
		Assert.notNull(taskDefinitionRepository, "TaskDefinitionRepository must not be null");
		Assert.notNull(taskExplorer, "TaskExplorer must not be null");
		Assert.notNull(taskConfigurationProperties, "taskConfigurationProperties must not be null");

		this.dataSourceProperties = dataSourceProperties;
		this.appRegistryService = appRegistryService;
		this.taskExplorer = taskExplorer;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskConfigurationProperties = taskConfigurationProperties;
	}

	@Override
	public long getMaximumConcurrentTasks() {
		return taskConfigurationProperties.getMaximumConcurrentTasks();
	}

	@Override
	public synchronized boolean maxConcurrentExecutionsReached() {
		return this.taskExplorer.getRunningTaskExecutionCount() >= taskConfigurationProperties
				.getMaximumConcurrentTasks();
	}

	@Override
	public TaskExecutionInformation findTaskExecutionInformation(String taskName,
			Map<String, String> taskDeploymentProperties) {
		Assert.hasText(taskName, "The provided taskName must not be null or empty.");
		Assert.notNull(taskDeploymentProperties, "The provided runtimeProperties must not be null.");

		TaskExecutionInformation retData = new TaskExecutionInformation();
		retData.setTaskDeploymentProperties(taskDeploymentProperties);

		TaskDefinition taskDefinition = taskDefinitionRepository.findById(taskName)
				.orElseThrow(() -> new NoSuchTaskDefinitionException(taskName));
		TaskParser taskParser = new TaskParser(taskDefinition.getName(), taskDefinition.getDslText(), true, true);
		TaskNode taskNode = taskParser.parse();
		// if composed task definition replace definition with one composed task
		// runner and executable graph.
		if (taskNode.isComposed()) {
			taskDefinition = new TaskDefinition(taskDefinition.getName(),
					TaskServiceUtils.createComposedTaskDefinition(
							taskNode.toExecutableDSL(), taskConfigurationProperties));
			retData.setTaskDeploymentProperties(
					TaskServiceUtils.establishComposedTaskProperties(taskDeploymentProperties,
							taskNode));
		}
		taskDefinition = TaskServiceUtils.updateTaskProperties(taskDefinition,
				dataSourceProperties);

		AppRegistration appRegistration = appRegistryService.find(taskDefinition.getRegisteredAppName(),
				ApplicationType.task);
		Assert.notNull(appRegistration, "Unknown task app: " + taskDefinition.getRegisteredAppName());

		retData.setTaskDefinition(taskDefinition);
		retData.setComposed(taskNode.isComposed());
		retData.setAppResource(appRegistryService.getAppResource(appRegistration));
		retData.setMetadataResource(appRegistryService.getAppMetadataResource(appRegistration));
		return retData;
	}

}
