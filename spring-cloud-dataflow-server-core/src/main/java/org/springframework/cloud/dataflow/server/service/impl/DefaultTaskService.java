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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.DefinitionUtils;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.rest.support.ArgumentSanitizer;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.audit.domain.AuditActionType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditOperationType;
import org.springframework.cloud.dataflow.server.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.controller.WhitelistProperties;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentKey;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskExecutionCreationService;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.TaskValidationService;
import org.springframework.cloud.dataflow.server.service.ValidationStatus;
import org.springframework.cloud.dataflow.server.support.ApplicationDoesNotExistException;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.core.io.Resource;
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
 * @author Michael Wirth
 * @author David Turanski
 */
@Transactional
public class DefaultTaskService implements TaskService {

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

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final WhitelistProperties whitelistProperties;

	private final TaskConfigurationProperties taskConfigurationProperties;

	private final DeploymentIdRepository deploymentIdRepository;

	private final String dataflowServerUri;

	private final CommonApplicationProperties commonApplicationProperties;

	private final TaskValidationService taskValidationService;

	private final TaskExecutionCreationService taskExecutionCreationService;

	protected final AuditRecordService auditRecordService;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	public static final String TASK_DEFINITION_DSL_TEXT = "taskDefinitionDslText";
	public static final String TASK_DEPLOYMENT_PROPERTIES = "taskDeploymentProperties";
	public static final String COMMAND_LINE_ARGS = "commandLineArgs";

	/**
	 * Initializes the {@link DefaultTaskService}.
	 *
	 * @param dataSourceProperties the data source properties.
	 * @param taskDefinitionRepository the {@link TaskDefinitionRepository} this service will
	 * use for task CRUD operations.
	 * @param taskExecutionRepository the repository this service will use for deployment IDs.
	 * @param taskExplorer the explorer this service will use to lookup task executions
	 * @param registry URI registry this service will use to look up app URIs.
	 * @param taskLauncher the launcher this service will use to launch task apps.
	 * @param metaDataResolver the metadata resolver
	 * @param taskConfigurationProperties the properties used to define the behavior of tasks
	 * @param deploymentIdRepository the repository that maps deployment keys to IDs
	 * @param auditRecordService the audit record service
	 * @param dataflowServerUri the data flow server URI
	 * @param commonApplicationProperties the common application properties
	 * @param taskValidationService the task validation service
	 */
	public DefaultTaskService(DataSourceProperties dataSourceProperties,
			TaskDefinitionRepository taskDefinitionRepository, TaskExplorer taskExplorer,
			TaskRepository taskExecutionRepository, AppRegistryCommon registry,
			TaskLauncher taskLauncher, ApplicationConfigurationMetadataResolver metaDataResolver,
			TaskConfigurationProperties taskConfigurationProperties, DeploymentIdRepository deploymentIdRepository,
			AuditRecordService auditRecordService,
			String dataflowServerUri, CommonApplicationProperties commonApplicationProperties,
			TaskValidationService taskValidationService,
			TaskExecutionCreationService taskExecutionCreationService) {
		Assert.notNull(dataSourceProperties, "dataSourceProperties must not be null");
		Assert.notNull(taskDefinitionRepository, "taskDefinitionRepository must not be null");
		Assert.notNull(taskExecutionRepository, "taskExecutionRepository must not be null");
		Assert.notNull(taskExplorer, "taskExplorer must not be null");
		Assert.notNull(registry, "uriRegistry must not be null");
		Assert.notNull(taskLauncher, "taskLauncher must not be null");
		Assert.notNull(metaDataResolver, "metaDataResolver must not be null");
		Assert.notNull(taskConfigurationProperties, "taskConfigurationProperties must not be null");
		Assert.notNull(deploymentIdRepository, "deploymentIdRepository must not be null");
		Assert.notNull(commonApplicationProperties, "commonApplicationProperties must not be null");
		Assert.notNull(auditRecordService, "auditRecordService must not be null");
		Assert.notNull(taskValidationService, "taskValidationService must not be null");
		Assert.notNull(taskExecutionCreationService, "taskExecutionRepositoryService must not be null");
		this.dataSourceProperties = dataSourceProperties;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskExecutionRepository = taskExecutionRepository;
		this.taskExplorer = taskExplorer;
		this.registry = registry;
		this.taskLauncher = taskLauncher;
		this.whitelistProperties = new WhitelistProperties(metaDataResolver);
		this.taskConfigurationProperties = taskConfigurationProperties;
		this.deploymentIdRepository = deploymentIdRepository;
		this.dataflowServerUri = dataflowServerUri;
		this.commonApplicationProperties = commonApplicationProperties;
		this.auditRecordService = auditRecordService;
		this.taskValidationService = taskValidationService;
		this.taskExecutionCreationService = taskExecutionCreationService;
	}

	@Override
	public long executeTask(String taskName, Map<String, String> taskDeploymentProperties,
			List<String> commandLineArgs) {
		Assert.hasText(taskName, "The provided taskName must not be null or empty.");
		Assert.notNull(taskDeploymentProperties, "The provided runtimeProperties must not be null.");

		if (maxConcurrentExecutionsReached()) {
			throw new IllegalStateException(String.format(
					"The maximum concurrent task executions [%d] is at its limit.",
					taskConfigurationProperties.getMaximumConcurrentTasks()));
		}

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
					TaskServiceUtils.createComposedTaskDefinition(
							taskNode.toExecutableDSL(), taskConfigurationProperties));
			taskDeploymentProperties = TaskServiceUtils.establishComposedTaskProperties(taskDeploymentProperties,
					taskNode);
		}

		AppRegistration appRegistration = this.registry.find(taskDefinition.getRegisteredAppName(),
				ApplicationType.task);
		Assert.notNull(appRegistration, "Unknown task app: " + taskDefinition.getRegisteredAppName());
		Resource appResource = this.registry.getAppResource(appRegistration);
		Resource metadataResource = this.registry.getAppMetadataResource(appRegistration);

		TaskExecution taskExecution = this.taskExecutionCreationService.createTaskExecution(taskName);
		taskDefinition = TaskServiceUtils.updateTaskProperties(taskDefinition,
				dataSourceProperties);

		Map<String, String> appDeploymentProperties = new HashMap<>(commonApplicationProperties.getTask());
		appDeploymentProperties.putAll(
				TaskServiceUtils.extractAppProperties(taskDefinition.getRegisteredAppName(), taskDeploymentProperties));

		Map<String, String> deployerDeploymentProperties = DeploymentPropertiesUtils
				.extractAndQualifyDeployerProperties(taskDeploymentProperties, taskDefinition.getRegisteredAppName());
		if (StringUtils.hasText(this.dataflowServerUri) && taskNode.isComposed()) {
			TaskServiceUtils.updateDataFlowUriIfNeeded(this.dataflowServerUri, appDeploymentProperties,
					commandLineArgs);
		}
		AppDefinition revisedDefinition = TaskServiceUtils.mergeAndExpandAppProperties(taskDefinition, metadataResource,
				appDeploymentProperties, this.whitelistProperties);
		List<String> updatedCmdLineArgs = this.updateCommandLineArgs(commandLineArgs, taskExecution);
		AppDeploymentRequest request = new AppDeploymentRequest(revisedDefinition, appResource,
				deployerDeploymentProperties, updatedCmdLineArgs);
		String id = this.taskLauncher.launch(request);
		if (!StringUtils.hasText(id)) {
			throw new IllegalStateException("Deployment ID is null for the task:" + taskName);
		}
		this.taskExecutionRepository.updateExternalExecutionId(taskExecution.getExecutionId(), id);

		this.auditRecordService.populateAndSaveAuditRecordUsingMapData(
				AuditOperationType.TASK, AuditActionType.DEPLOY,
				taskDefinition.getName(), getAuditata(taskDefinition, taskDeploymentProperties, updatedCmdLineArgs));

		return taskExecution.getExecutionId();
	}

	private Map<String, Object> getAuditata(TaskDefinition taskDefinition, Map<String, String> taskDeploymentProperties,
			List<String> commandLineArgs) {
		final Map<String, Object> auditedData = new HashMap<>(3);
		auditedData.put(TASK_DEFINITION_DSL_TEXT, this.argumentSanitizer.sanitizeTaskDsl(taskDefinition));
		auditedData.put(TASK_DEPLOYMENT_PROPERTIES, this.argumentSanitizer.sanitizeProperties(taskDeploymentProperties));
		auditedData.put(COMMAND_LINE_ARGS, this.argumentSanitizer.sanitizeArguments(commandLineArgs));
		return auditedData;
	}

	private synchronized boolean maxConcurrentExecutionsReached() {
		return this.taskExplorer.getRunningTaskExecutionCount() >= this.taskConfigurationProperties
				.getMaximumConcurrentTasks();
	}

	private List<String> updateCommandLineArgs(List<String> commandLineArgs, TaskExecution taskExecution) {
		return Stream
				.concat(commandLineArgs.stream().filter(a -> !a.startsWith("--spring.cloud.task.executionid=")),
						Stream.of("--spring.cloud.task.executionid=" + taskExecution.getExecutionId()))
				.collect(Collectors.toList());
	}

	@Override
	public boolean isComposedDefinition(String dsl) {
		return TaskServiceUtils.isComposedTaskDefinition(dsl);
	}

	@Override
	public long getMaximumConcurrentTasks() {
		return taskConfigurationProperties.getMaximumConcurrentTasks();
	}

	@Override
	public ValidationStatus validateTask(String name) {
		return this.taskValidationService.validateTask(name);
	}

	@Override
	public void cleanupExecution(long id) {
		TaskExecution taskExecution = taskExplorer.getTaskExecution(id);
		Assert.notNull(taskExecution, "There was no task execution with id " + id);
		String launchId = taskExecution.getExternalExecutionId();
		Assert.hasLength(launchId, "The TaskExecution for id " + id + " did not have an externalExecutionId");
		taskLauncher.cleanup(launchId);
	}

	@Override
	@Transactional
	public void saveTaskDefinition(String name, String dsl) {
		TaskParser taskParser = new TaskParser(name, dsl, true, true);
		TaskNode taskNode = taskParser.parse();
		TaskDefinition taskDefinition = new TaskDefinition(name, dsl);
		if (taskNode.isComposed()) {
			// Create the child task definitions needed for the composed task
			taskNode.getTaskApps().stream().forEach(task -> {
				// Add arguments to child task definitions
				String generatedTaskDSL = task.getName() + task.getArguments().entrySet().stream()
						.map(argument -> String.format(" --%s=%s", argument.getKey(),
								DefinitionUtils.autoQuotes(argument.getValue())))
						.collect(Collectors.joining());
				TaskDefinition composedTaskDefinition = new TaskDefinition(task.getExecutableDSLName(),
						generatedTaskDSL);
				saveStandardTaskDefinition(composedTaskDefinition);
			});
			this.taskDefinitionRepository.save(taskDefinition);
		}
		else {
			saveStandardTaskDefinition(taskDefinition);
		}
		this.auditRecordService.populateAndSaveAuditRecord(
				AuditOperationType.TASK, AuditActionType.CREATE,
				name, this.argumentSanitizer.sanitizeTaskDsl(taskDefinition));
	}

	private void saveStandardTaskDefinition(TaskDefinition taskDefinition) {
		String appName = taskDefinition.getRegisteredAppName();
		if (registry.find(appName, ApplicationType.task) == null) {
			throw new ApplicationDoesNotExistException(
					String.format("Application name '%s' with type '%s' does not exist in the app registry.", appName,
							ApplicationType.task));
		}
		this.taskDefinitionRepository.save(taskDefinition);
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
				destroyChildTask(childTaskPrefix + childName);
			});
		}
		// destroy normal task or composed parent task
		destroyPrimaryTask(name);

		auditRecordService.populateAndSaveAuditRecord(
				AuditOperationType.TASK, AuditActionType.DELETE,
				name, this.argumentSanitizer.sanitizeTaskDsl(taskDefinition));
	}

	private void destroyPrimaryTask(String name) {
		TaskDefinition taskDefinition = taskDefinitionRepository.findOne(name);
		if (taskDefinition == null) {
			throw new NoSuchTaskDefinitionException(name);
		}
		destroyTask(taskDefinition);
	}

	private void destroyChildTask(String name) {
		TaskDefinition taskDefinition = taskDefinitionRepository.findOne(name);
		if (taskDefinition != null) {
			destroyTask(taskDefinition);
		}
	}

	private void destroyTask(TaskDefinition taskDefinition) {
		taskLauncher.destroy(taskDefinition.getName());
		deploymentIdRepository.delete(DeploymentKey.forTaskDefinition(taskDefinition));
		taskDefinitionRepository.delete(taskDefinition.getName());
	}

}
