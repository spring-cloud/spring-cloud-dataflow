/*
 * Copyright 2016-2021 the original author or authors.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.dataflow.core.AllPlatformsTaskExecutionInformation;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.core.dsl.TaskApp;
import org.springframework.cloud.dataflow.core.dsl.TaskAppNode;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorer;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
 * @author Corneil du Plessis
 */
public class DefaultTaskExecutionInfoService implements TaskExecutionInfoService {
	private final static Logger logger = LoggerFactory.getLogger(DefaultTaskExecutionInfoService.class);

	private final DataSourceProperties dataSourceProperties;

	/**
	 * The {@link AppRegistryService} this service will use to look up task app URIs.
	 */
	private final AppRegistryService appRegistryService;

	/**
	 * Used to read TaskExecutions.
	 */
	private final DataflowTaskExplorer taskExplorer;

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final TaskConfigurationProperties taskConfigurationProperties;

	private final LauncherRepository launcherRepository;

	private final List<TaskPlatform> taskPlatforms;

	private final ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties;

	/**
	 * Initializes the {@link DefaultTaskExecutionInfoService}.
	 *
	 * @param dataSourceProperties        the data source properties.
	 * @param appRegistryService          URI registry this service will use to look up app URIs.
	 * @param taskExplorer                the explorer this service will use to lookup task executions
	 * @param taskDefinitionRepository    the {@link TaskDefinitionRepository} this service will
	 *                                    use for task CRUD operations.
	 * @param taskConfigurationProperties the properties used to define the behavior of tasks
	 * @param launcherRepository          the launcher repository
	 * @param taskPlatforms               the task platforms
	 */
	@Deprecated
	public DefaultTaskExecutionInfoService(
		DataSourceProperties dataSourceProperties,
		AppRegistryService appRegistryService,
		DataflowTaskExplorer taskExplorer,
		TaskDefinitionRepository taskDefinitionRepository,
		TaskConfigurationProperties taskConfigurationProperties,
		LauncherRepository launcherRepository,
		List<TaskPlatform> taskPlatforms
	) {
		this(dataSourceProperties,
			appRegistryService,
			taskExplorer,
			taskDefinitionRepository,
			taskConfigurationProperties,
			launcherRepository,
			taskPlatforms,
			null);
	}

	/**
	 * Initializes the {@link DefaultTaskExecutionInfoService}.
	 *
	 * @param dataSourceProperties                      the data source properties.
	 * @param appRegistryService                        URI registry this service will use to look up app URIs.
	 * @param taskExplorer                              the explorer this service will use to lookup task executions
	 * @param taskDefinitionRepository                  the {@link TaskDefinitionRepository} this service will
	 *                                                  use for task CRUD operations.
	 * @param taskConfigurationProperties               the properties used to define the behavior of tasks
	 * @param launcherRepository                        the launcher repository
	 * @param taskPlatforms                             the task platforms
	 * @param composedTaskRunnerConfigurationProperties the properties used to define the behavior of CTR
	 */
	public DefaultTaskExecutionInfoService(
		DataSourceProperties dataSourceProperties,
		AppRegistryService appRegistryService,
		DataflowTaskExplorer taskExplorer,
		TaskDefinitionRepository taskDefinitionRepository,
		TaskConfigurationProperties taskConfigurationProperties,
		LauncherRepository launcherRepository,
		List<TaskPlatform> taskPlatforms,
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties
	) {
		Assert.notNull(dataSourceProperties, "DataSourceProperties must not be null");
		Assert.notNull(appRegistryService, "AppRegistryService must not be null");
		Assert.notNull(taskDefinitionRepository, "TaskDefinitionRepository must not be null");
		Assert.notNull(taskExplorer, "TaskExplorer must not be null");
		Assert.notNull(taskConfigurationProperties, "taskConfigurationProperties must not be null");
		Assert.notNull(launcherRepository, "launcherRepository must not be null");
		Assert.notEmpty(taskPlatforms, "taskPlatform must not be empty or null");

		this.dataSourceProperties = dataSourceProperties;
		this.appRegistryService = appRegistryService;
		this.taskExplorer = taskExplorer;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskConfigurationProperties = taskConfigurationProperties;
		this.launcherRepository = launcherRepository;
		this.taskPlatforms = taskPlatforms;
		this.composedTaskRunnerConfigurationProperties = composedTaskRunnerConfigurationProperties;
	}

	@Override
	public TaskExecutionInformation findTaskExecutionInformation(
		String taskName,
		Map<String, String> taskDeploymentProperties, boolean addDatabaseCredentials, Map<String, String> previousTaskDeploymentProperties
	) {
		Assert.hasText(taskName, "The provided taskName must not be null or empty.");
		Assert.notNull(taskDeploymentProperties, "The provided runtimeProperties must not be null.");

		TaskExecutionInformation taskExecutionInformation = new TaskExecutionInformation();
		taskExecutionInformation.setTaskDeploymentProperties(taskDeploymentProperties);

		TaskDefinition originalTaskDefinition = taskDefinitionRepository.findById(taskName)
			.orElseThrow(() -> new NoSuchTaskDefinitionException(taskName));
		//TODO: This normally called by JPA automatically but `AutoCreateTaskDefinitionTests` fails without this.
		originalTaskDefinition.initialize();
		TaskParser taskParser = new TaskParser(originalTaskDefinition.getName(), originalTaskDefinition.getDslText(),
			true, true);
		TaskNode taskNode = taskParser.parse();
		// if composed task definition replace definition with one composed task
		// runner and executable graph.
		TaskDefinition taskDefinitionToUse;
		AppRegistration appRegistration;
		if (taskNode.isComposed()) {
			taskDefinitionToUse = new TaskDefinition(originalTaskDefinition.getName(),
				TaskServiceUtils.createComposedTaskDefinition(taskNode.toExecutableDSL()));
			taskExecutionInformation.setTaskDeploymentProperties(
				TaskServiceUtils.establishComposedTaskProperties(taskDeploymentProperties,
					taskNode));
			taskDefinitionToUse = TaskServiceUtils.updateTaskProperties(taskDefinitionToUse,
				dataSourceProperties, addDatabaseCredentials);
			try {
				appRegistration = new AppRegistration(ComposedTaskRunnerConfigurationProperties.COMPOSED_TASK_RUNNER_NAME,
					ApplicationType.task,
					new URI(TaskServiceUtils.getComposedTaskLauncherUri(this.taskConfigurationProperties,
						this.composedTaskRunnerConfigurationProperties)));
			} catch (URISyntaxException e) {
				throw new IllegalStateException("Invalid Compose Task Runner Resource", e);
			}

		} else {
			taskDefinitionToUse = TaskServiceUtils.updateTaskProperties(originalTaskDefinition,
				dataSourceProperties, addDatabaseCredentials);

			String label = null;
			if (taskNode.getTaskApp() != null) {
				TaskAppNode taskAppNode = taskNode.getTaskApp();
				if (taskAppNode.getLabel() != null) {
					label = taskAppNode.getLabel().stringValue();
				} else {
					label = taskAppNode.getName();
				}
			}
			String version = taskDeploymentProperties.get("version." + label);
			if (version == null) {
				// restore from previous "manifest"
				version = previousTaskDeploymentProperties.get("version." + label);
			}
			// if we have version, use that or rely on default version set
			if (version == null) {
				appRegistration = appRegistryService.find(taskDefinitionToUse.getRegisteredAppName(),
					ApplicationType.task);
			} else {
				appRegistration = appRegistryService.find(taskDefinitionToUse.getRegisteredAppName(),
					ApplicationType.task, version);
			}
		}

		Assert.notNull(appRegistration, "Unknown task app: " + taskDefinitionToUse.getRegisteredAppName());

		taskExecutionInformation.setTaskDefinition(taskDefinitionToUse);
		taskExecutionInformation.setOriginalTaskDefinition(originalTaskDefinition);
		taskExecutionInformation.setComposed(taskNode.isComposed());
		taskExecutionInformation.setAppResource(appRegistryService.getAppResource(appRegistration));
		taskExecutionInformation.setMetadataResource(appRegistryService.getAppMetadataResource(appRegistration));
		return taskExecutionInformation;
	}

	@Override
	public Set<String> composedTaskChildNames(String taskName) {
		TaskDefinition taskDefinition = taskDefinitionRepository.findByTaskName(taskName);
		TaskParser taskParser = new TaskParser(taskDefinition.getTaskName(), taskDefinition.getDslText(), true, true);
		Set<String> result = new HashSet<>();
		TaskNode taskNode = taskParser.parse();
		if (taskNode.isComposed()) {
			extractNames(taskNode, result);
		}
		return result;
	}

	@Override
	public Set<String> taskNames(String taskName) {
		TaskDefinition taskDefinition = taskDefinitionRepository.findByTaskName(taskName);
		TaskParser taskParser = new TaskParser(taskDefinition.getTaskName(), taskDefinition.getDslText(), true, true);
		Set<String> result = new HashSet<>();
		TaskNode taskNode = taskParser.parse();
		extractNames(taskNode, result);
		return result;
	}

	private void extractNames(TaskNode taskNode, Set<String> result) {
		for (TaskApp subTask : taskNode.getTaskApps()) {
			logger.debug("subTask:{}:{}:{}:{}", subTask.getName(), subTask.getTaskName(), subTask.getLabel(), subTask);
			TaskDefinition subTaskDefinition = taskDefinitionRepository.findByTaskName(subTask.getName());
			if (subTaskDefinition != null) {
				if(StringUtils.hasText(subTask.getLabel())) {
					result.add(subTaskDefinition.getRegisteredAppName() + "," + subTask.getLabel());
				} else {
					result.add(subTaskDefinition.getRegisteredAppName());
				}
				TaskParser subTaskParser = new TaskParser(subTaskDefinition.getTaskName(), subTaskDefinition.getDslText(), true, true);
				TaskNode subTaskNode = subTaskParser.parse();
				if (subTaskNode != null && subTaskNode.getTaskApp() != null) {
					for (TaskApp subSubTask : subTaskNode.getTaskApps()) {
						logger.debug("subSubTask:{}:{}:{}:{}", subSubTask.getName(), subSubTask.getTaskName(), subSubTask.getLabel(), subSubTask);
						TaskDefinition subSubTaskDefinition = taskDefinitionRepository.findByTaskName(subSubTask.getName());
						if (subSubTaskDefinition != null) {
							if (subSubTask.getLabel() != null && !subTask.getLabel().contains("$")) {
								result.add(subSubTaskDefinition.getRegisteredAppName() + "," + subSubTask.getLabel());
							} else {
								result.add(subSubTaskDefinition.getRegisteredAppName());
							}
						}
					}
				}
			} else {
				if ((subTask.getLabel() == null || subTask.getLabel().equals(subTask.getName())) && !subTask.getName().contains("$")) {
					result.add(subTask.getName());
				} else {
					if (!subTask.getName().contains("$") && !subTask.getLabel().contains("$")) {
						result.add(subTask.getName() + "," + subTask.getLabel());
					} else if (!subTask.getName().contains("$")) {
						result.add(subTask.getName());
					} else if (!subTask.getTaskName().contains("$")) {
						result.add(subTask.getTaskName());
					}
				}
			}
		}
	}

	@Override
	public List<AppDeploymentRequest> createTaskDeploymentRequests(String taskName, String dslText) {
		List<AppDeploymentRequest> appDeploymentRequests = new ArrayList<>();
		TaskParser taskParser = new TaskParser(taskName, dslText, true, true);
		TaskNode taskNode = taskParser.parse();
		if (taskNode.isComposed()) {
			for (TaskApp subTask : taskNode.getTaskApps()) {
				// composed tasks have taskDefinitions in the graph
				TaskDefinition subTaskDefinition = taskDefinitionRepository.findByTaskName(subTask.getName());
				String subTaskDsl = subTaskDefinition.getDslText();
				TaskParser subTaskParser = new TaskParser(subTaskDefinition.getTaskName(), subTaskDsl, true, true);
				TaskNode subTaskNode = subTaskParser.parse();
				String subTaskName = subTaskNode.getTaskApp().getName();
				AppRegistration appRegistration = appRegistryService.find(subTaskName,
					ApplicationType.task);
				Assert.notNull(appRegistration, "Unknown task app: " + subTask.getName());
				Resource appResource = appRegistryService.getAppResource(appRegistration);

				// TODO filter args
				// TODO incorporate the label somehow, ea. 1:timestamp --format=YYYY
				AppDefinition appDefinition = new AppDefinition(subTask.getName(), subTaskNode.getTaskApp().getArgumentsAsMap());

				AppDeploymentRequest appDeploymentRequest = new AppDeploymentRequest(appDefinition,
					appResource, null, null);
				appDeploymentRequests.add(appDeploymentRequest);
			}
		}
		return appDeploymentRequests;
	}

	@Override
	public AllPlatformsTaskExecutionInformation findAllPlatformTaskExecutionInformation() {
		return new AllPlatformsTaskExecutionInformation(this.taskPlatforms);
	}

}
