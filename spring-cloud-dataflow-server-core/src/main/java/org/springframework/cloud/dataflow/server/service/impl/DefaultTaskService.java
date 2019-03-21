/*
 * Copyright 2015-2018 the original author or authors.
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
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.bind.RelaxedNames;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDefinition.TaskDefinitionBuilder;
import org.springframework.cloud.dataflow.core.dsl.TaskApp;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.controller.WhitelistProperties;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentKey;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.support.ApplicationDoesNotExistException;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link TaskService} interface. Provide service methods
 * for Tasks.
 *
 * @author Michael Minella
 * @author Marius Bogoevici
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Janne Valkealahti
 * @author Gunnar Hillert
 * @author Thomas Risberg
 * @author Ilayaperumal Gopinathan
 */
@Transactional
public class DefaultTaskService implements TaskService {

	private static final String DATAFLOW_SERVER_URI_KEY = "dataflowServerUri";

	private final DataSourceProperties dataSourceProperties;

	/**
	 * Used to create TaskExecutions.
	 */
	private final TaskRepository taskExecutionRepository;

	/**
	 * Used to read TaskExecutions.
	 */
	private final TaskExplorer taskExplorer;

	/**
	 * Used to launch apps as tasks.
	 */
	private final TaskLauncher taskLauncher;

	/**
	 * The {@link AppRegistry} this service will use to look up task app URIs.
	 */
	private final AppRegistryCommon registry;

	/**
	 * The {@link ResourceLoader} that will resolve URIs to {@link Resource}s.
	 */
	private final ResourceLoader resourceLoader;

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final WhitelistProperties whitelistProperties;

	private final TaskConfigurationProperties taskConfigurationProperties;

	private final DeploymentIdRepository deploymentIdRepository;

	private final String dataflowServerUri;

	private final CommonApplicationProperties commonApplicationProperties;

	/**
	 * Initializes the {@link DefaultTaskService}.
	 *
	 * @param dataSourceProperties the data source properties.
	 * @param taskDefinitionRepository the {@link TaskDefinitionRepository} this service will
	 * use for task CRUD operations.
	 * @param taskExecutionRepository the repository this service will use for deployment IDs.
	 * @param taskExplorer the explorer this service will use to lookup task executions
	 * @param registry URI registry this service will use to look up app URIs.
	 * @param resourceLoader the {@link ResourceLoader} that will resolve URIs to
	 * {@link Resource}s.
	 * @param taskLauncher the launcher this service will use to launch task apps.
	 * @param metaDataResolver the metadata resolver
	 * @param taskConfigurationProperties the properties used to define the behavior of tasks
	 * @param deploymentIdRepository the repository that maps deployment keys to IDs
	 * @param dataflowServerUri the data flow server URI
	 * @param commonApplicationProperties the common application properties
	 */
	public DefaultTaskService(DataSourceProperties dataSourceProperties,
			TaskDefinitionRepository taskDefinitionRepository, TaskExplorer taskExplorer,
			TaskRepository taskExecutionRepository, AppRegistryCommon registry, ResourceLoader resourceLoader,
			TaskLauncher taskLauncher, ApplicationConfigurationMetadataResolver metaDataResolver,
			TaskConfigurationProperties taskConfigurationProperties, DeploymentIdRepository deploymentIdRepository,
			String dataflowServerUri, CommonApplicationProperties commonApplicationProperties) {
		Assert.notNull(dataSourceProperties, "DataSourceProperties must not be null");
		Assert.notNull(taskDefinitionRepository, "TaskDefinitionRepository must not be null");
		Assert.notNull(taskExecutionRepository, "TaskExecutionRepository must not be null");
		Assert.notNull(taskExplorer, "TaskExplorer must not be null");
		Assert.notNull(registry, "UriRegistry must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Assert.notNull(taskLauncher, "TaskLauncher must not be null");
		Assert.notNull(metaDataResolver, "metaDataResolver must not be null");
		Assert.notNull(taskConfigurationProperties, "taskConfigurationProperties must not be null");
		Assert.notNull(deploymentIdRepository, "deploymentIdRepository must not be null");
		Assert.notNull(commonApplicationProperties, "commonApplicationProperties must not be null");
		this.dataSourceProperties = dataSourceProperties;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskExecutionRepository = taskExecutionRepository;
		this.taskExplorer = taskExplorer;
		this.registry = registry;
		this.taskLauncher = taskLauncher;
		this.resourceLoader = resourceLoader;
		this.whitelistProperties = new WhitelistProperties(metaDataResolver);
		this.taskConfigurationProperties = taskConfigurationProperties;
		this.deploymentIdRepository = deploymentIdRepository;
		this.dataflowServerUri = dataflowServerUri;
		this.commonApplicationProperties = commonApplicationProperties;
	}

	@Override
	public long executeTask(String taskName, Map<String, String> taskDeploymentProperties,
			List<String> commandLineArgs) {
		Assert.hasText(taskName, "The provided taskName must not be null or empty.");
		Assert.notNull(taskDeploymentProperties, "The provided runtimeProperties must not be null.");
		TaskDefinition taskDefinition = this.taskDefinitionRepository.findOne(taskName);
		if (taskDefinition == null) {
			throw new NoSuchTaskDefinitionException(taskName);
		}
		TaskParser taskParser = new TaskParser(taskDefinition.getName(), taskDefinition.getDslText(), true, true);
		TaskNode taskNode = taskParser.parse();
		// if composed task definition replace definition with one composed task
		// runner and executable graph.
		if (taskNode.isComposed()) {
			taskDefinition = new TaskDefinition(taskDefinition.getName(),
					createComposedTaskDefinition(taskNode.toExecutableDSL()));
			taskDeploymentProperties = establishComposedTaskProperties(taskDeploymentProperties, taskNode);
		}

		AppRegistration appRegistration = this.registry.find(taskDefinition.getRegisteredAppName(),
				ApplicationType.task);
		Assert.notNull(appRegistration, "Unknown task app: " + taskDefinition.getRegisteredAppName());
		Resource appResource = this.registry.getAppResource(appRegistration);
		Resource metadataResource = this.registry.getAppMetadataResource(appRegistration);

		TaskExecution taskExecution = taskExecutionRepository.createTaskExecution(taskName);
		taskDefinition = this.updateTaskProperties(taskDefinition);

		Map<String, String> appDeploymentProperties = extractAppProperties(taskDefinition.getRegisteredAppName(),
				taskDeploymentProperties);
		Map<String, String> deployerDeploymentProperties = DeploymentPropertiesUtils
				.extractAndQualifyDeployerProperties(taskDeploymentProperties, taskDefinition.getRegisteredAppName());
		if (StringUtils.hasText(this.dataflowServerUri) && taskNode.isComposed()) {
			updateDataFlowUriIfNeeded(appDeploymentProperties, commandLineArgs);
		}
		AppDefinition revisedDefinition = mergeAndExpandAppProperties(taskDefinition, metadataResource,
				appDeploymentProperties);
		List<String> updatedCmdLineArgs = this.updateCommandLineArgs(commandLineArgs, taskExecution);
		AppDeploymentRequest request = new AppDeploymentRequest(revisedDefinition, appResource,
				deployerDeploymentProperties, updatedCmdLineArgs);
		String id = this.taskLauncher.launch(request);
		if (!StringUtils.hasText(id)) {
			throw new IllegalStateException("Deployment ID is null for the task:" + taskName);
		}
		taskExecutionRepository.updateExternalExecutionId(taskExecution.getExecutionId(), id);
		return taskExecution.getExecutionId();
	}

	private Map<String, String> establishComposedTaskProperties(
			Map<String, String> taskDeploymentProperties,
			TaskNode taskNode) {
		String result = "";
		taskDeploymentProperties = new HashMap<>(taskDeploymentProperties);
		for (TaskApp subTask : taskNode.getTaskApps()) {
			String subTaskName = String.format("app.%s-%s.", taskNode.getName(),
					(subTask.getLabel() == null) ? subTask.getName() : subTask.getLabel());
			String scdfTaskName = String.format("app.%s.%s.", taskNode.getName(),
					(subTask.getLabel() == null) ? subTask.getName() : subTask.getLabel());
			Set<String> propertyKeys = taskDeploymentProperties.keySet().
					stream().filter(taskProperty -> taskProperty.startsWith(scdfTaskName))
					.collect(Collectors.toSet());
			for (String taskProperty : propertyKeys) {
				if (result.length() != 0) {
					result += ", ";
				}
				result += String.format("%sapp.%s.%s=%s", subTaskName,
						subTask.getName(),
						taskProperty.substring(subTaskName.length()),
						taskDeploymentProperties.get(taskProperty));
				taskDeploymentProperties.remove(taskProperty);
			}
		}
		if (result.length() != 0) {
			taskDeploymentProperties.put("app.composed-task-runner.composed-task-properties", result);
		}
		return taskDeploymentProperties;
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

	private List<String> updateCommandLineArgs(List<String> commandLineArgs, TaskExecution taskExecution) {
		return Stream
				.concat(commandLineArgs.stream().filter(a -> !a.startsWith("--spring.cloud.task.executionid=")),
						Stream.of("--spring.cloud.task.executionid=" + taskExecution.getExecutionId()))
				.collect(Collectors.toList());
	}

	@Override
	public boolean isComposedDefinition(String dsl) {
		Assert.hasText(dsl, "dsl must not be empty nor null");
		TaskParser taskParser = new TaskParser("__dummy", dsl, true, true);
		return taskParser.parse().isComposed();
	}

	@Override
	public void cleanupExecution(long id) {
		TaskExecution taskExecution = taskExplorer.getTaskExecution(id);
		Assert.notNull(taskExecution, "There was no task execution with id " + id);
		String launchId = taskExecution.getExternalExecutionId();
		Assert.hasLength(launchId, "The TaskExecution for id " + id + " did not have an externalExecutionId");
		taskLauncher.cleanup(launchId);
	}

	private Map<String, String> extractAppProperties(String name, Map<String, String> taskDeploymentProperties) {
		final String prefix = "app." + name + ".";
		return Stream
				.concat(commonApplicationProperties.getTask().entrySet().stream(),
						taskDeploymentProperties.entrySet().stream())
				.filter(kv -> kv.getKey().startsWith(prefix))
				.collect(Collectors.toMap(kv -> kv.getKey().substring(prefix.length()), kv -> kv.getValue()));
	}

	/**
	 * Return a copy of a given task definition where short form parameters have been expanded
	 * to their long form (amongst the whitelisted supported properties of the app) if
	 * applicable.
	 */
	private AppDefinition mergeAndExpandAppProperties(TaskDefinition original, Resource resource,
			Map<String, String> appDeploymentProperties) {
		Map<String, String> merged = new HashMap<>(original.getProperties());
		merged.putAll(appDeploymentProperties);
		merged = whitelistProperties.qualifyProperties(merged, resource);
		return new AppDefinition(original.getName(), merged);
	}

	private TaskDefinition updateTaskProperties(TaskDefinition taskDefinition) {
		TaskDefinitionBuilder builder = TaskDefinitionBuilder.from(taskDefinition);
		builder.setProperty("spring.datasource.url", dataSourceProperties.getUrl());
		builder.setProperty("spring.datasource.username", dataSourceProperties.getUsername());
		// password may be empty
		if (StringUtils.hasText(dataSourceProperties.getPassword())) {
			builder.setProperty("spring.datasource.password", dataSourceProperties.getPassword());
		}
		builder.setProperty("spring.datasource.driverClassName", dataSourceProperties.getDriverClassName());

		return builder.build();
	}

	@Override
	@Transactional
	public void saveTaskDefinition(String name, String dsl) {
		TaskParser taskParser = new TaskParser(name, dsl, true, true);
		TaskNode taskNode = taskParser.parse();
		if (taskNode.isComposed()) {
			// Create the child task definitions needed for the composed task
			taskNode.getTaskApps().stream().forEach(task -> {
				// Add arguments to child task definitions
				String generatedTaskDSL = task.getName() + task.getArguments().entrySet().stream()
						.map(argument -> String.format(" --%s=%s", argument.getKey(), argument.getValue()))
						.collect(Collectors.joining());
				TaskDefinition composedTaskDefinition = new TaskDefinition(task.getExecutableDSLName(),
						generatedTaskDSL);
				saveStandardTaskDefinition(composedTaskDefinition);
			});
			taskDefinitionRepository.save(new TaskDefinition(name, dsl));
		}
		else {
			saveStandardTaskDefinition(new TaskDefinition(name, dsl));
		}

	}

	private void saveStandardTaskDefinition(TaskDefinition taskDefinition) {
		String appName = taskDefinition.getRegisteredAppName();
		if (registry.find(appName, ApplicationType.task) == null) {
			throw new ApplicationDoesNotExistException(
					String.format("Application name '%s' with type '%s' does not exist in the app registry.", appName,
							ApplicationType.task));
		}
		taskDefinitionRepository.save(taskDefinition);
	}

	private String createComposedTaskDefinition(String graph) {
		return String.format("%s --graph=\"%s\"", taskConfigurationProperties.getComposedTaskRunnerName(), graph);
	}

	@Override
	public void deleteTaskDefinition(String name) {
		TaskDefinition taskDefinition = taskDefinitionRepository.findOne(name);
		if (taskDefinition == null) {
			throw new NoSuchTaskDefinitionException(name);
		}
		TaskParser taskParser = new TaskParser(taskDefinition.getName(), taskDefinition.getDslText(), true, true);
		TaskNode taskNode = taskParser.parse();
		// if composed-task-runner definition then destroy all child tasks associated with
		// it.
		if (taskNode.isComposed()) {
			String childTaskPrefix = TaskNode.getTaskPrefix(name);
			// destroy composed child tasks
			taskNode.getTaskApps().stream().forEach(task -> {
				String childName = task.getName();
				if (task.getLabel() != null) {
					childName = task.getLabel();
				}
				destroyTask(childTaskPrefix + childName);
			});
		}
		// destroy normal task or composed parent task
		destroyTask(name);
	}

	private void destroyTask(String name) {
		TaskDefinition taskDefinition = taskDefinitionRepository.findOne(name);
		if (taskDefinition == null) {
			throw new NoSuchTaskDefinitionException(name);
		}
		taskLauncher.destroy(name);
		deploymentIdRepository.delete(DeploymentKey.forTaskDefinition(taskDefinition));
		taskDefinitionRepository.delete(name);
	}

}
