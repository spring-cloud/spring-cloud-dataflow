/*
 * Copyright 2015-2018 the original author or authors.
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDeployment;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.controller.WhitelistProperties;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.TaskExecutionCreationService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link TaskExecutionService} interface. Provide service
 * methods for Tasks.
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
@Transactional
public class DefaultTaskExecutionService implements TaskExecutionService {

	/**
	 * Used to launch apps as tasks.
	 */
	private final LauncherRepository launcherRepository;

	private final WhitelistProperties whitelistProperties;

	private final String dataflowServerUri;

	private final TaskExecutionCreationService taskExecutionRepositoryService;

	private final CommonApplicationProperties commonApplicationProperties;

	protected final AuditRecordService auditRecordService;

	/**
	 * Used to create TaskExecutions.
	 */
	private final TaskRepository taskRepository;

	private final TaskExecutionInfoService taskExecutionInfoService;

	private final TaskDeploymentRepository taskDeploymentRepository;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	public static final String TASK_DEFINITION_DSL_TEXT = "taskDefinitionDslText";

	public static final String TASK_DEPLOYMENT_PROPERTIES = "taskDeploymentProperties";

	public static final String COMMAND_LINE_ARGS = "commandLineArgs";

	public static final String TASK_PLATFORM_NAME = "spring.cloud.dataflow.task.platformName";

	/**
	 * Initializes the {@link DefaultTaskExecutionService}.
	 *
	 * @param launcherRepository the repository of task launcher used to launch task apps.
	 * @param metaDataResolver the metadata resolver
	 * @param auditRecordService the audit record service
	 * @param dataflowServerUri the URI of the data flow server
	 * @param commonApplicationProperties the common application properties for all tasks
	 * @param taskRepository the repository to use for accessing and updating task executions
	 * @param taskDeploymentRepository the repository to track task deployment
	 * @param taskExecutionInfoService the service used to setup a task execution
	 * @param taskExecutionRepositoryService the service used to create the task execution
	 */
	public DefaultTaskExecutionService(LauncherRepository launcherRepository,
			ApplicationConfigurationMetadataResolver metaDataResolver,
			AuditRecordService auditRecordService,
			String dataflowServerUri, CommonApplicationProperties commonApplicationProperties,
			TaskRepository taskRepository,
			TaskExecutionInfoService taskExecutionInfoService,
			TaskDeploymentRepository taskDeploymentRepository,
			TaskExecutionCreationService taskExecutionRepositoryService) {
		Assert.notNull(launcherRepository, "launcherRepository must not be null");
		Assert.notNull(metaDataResolver, "metaDataResolver must not be null");
		Assert.notNull(auditRecordService, "auditRecordService must not be null");
		Assert.notNull(commonApplicationProperties, "commonApplicationProperties must not be null");
		Assert.notNull(taskExecutionInfoService, "taskDefinitionRetriever must not be null");
		Assert.notNull(taskRepository, "taskRepository must not be null");
		Assert.notNull(taskExecutionInfoService, "taskExecutionInfoService must not be null");
		Assert.notNull(taskDeploymentRepository, "taskDeploymentRepository must not be null");
		Assert.notNull(taskExecutionRepositoryService, "taskExecutionRepositoryService must not be null");


		this.launcherRepository = launcherRepository;
		this.whitelistProperties = new WhitelistProperties(metaDataResolver);
		this.auditRecordService = auditRecordService;
		this.dataflowServerUri = dataflowServerUri;
		this.commonApplicationProperties = commonApplicationProperties;
		this.taskRepository = taskRepository;
		this.taskExecutionInfoService = taskExecutionInfoService;
		this.taskDeploymentRepository = taskDeploymentRepository;
		this.taskExecutionRepositoryService = taskExecutionRepositoryService;
	}

	@Override
	public long executeTask(String taskName, Map<String, String> taskDeploymentProperties, List<String> commandLineArgs) {

		if (taskExecutionInfoService.maxConcurrentExecutionsReached()) {
			throw new IllegalStateException(String.format(
					"The maximum concurrent task executions [%d] is at its limit.",
					taskExecutionInfoService.getMaximumConcurrentTasks()));
		}

		String platformName = taskDeploymentProperties.get(TASK_PLATFORM_NAME);
		if (!StringUtils.hasText(platformName)) {
			platformName = "default";
		}
		// Remove since the key for task platform name will not pass validation for app, deployer, or scheduler prefix
		if (taskDeploymentProperties.containsKey(TASK_PLATFORM_NAME)) {
			taskDeploymentProperties.remove(TASK_PLATFORM_NAME);
		}

		DeploymentPropertiesUtils.validateDeploymentProperties(taskDeploymentProperties);

		TaskLauncher taskLauncher = findTaskLaucher(platformName);

		TaskDeployment existingTaskDeployment =
				taskDeploymentRepository.findTopByTaskDefinitionNameOrderByCreatedOnAsc(taskName);
		if (existingTaskDeployment != null) {
			if (!existingTaskDeployment.getPlatformName().equals(platformName)) {
				throw new IllegalStateException(String.format(
						"Task definition [%s] has already been deployed on platfrom [%s].  " +
						"Requested to deploy on platform [%s].",
						taskName, existingTaskDeployment.getPlatformName(), platformName));
			}
		}
		TaskExecutionInformation taskExecutionInformation = taskExecutionInfoService
				.findTaskExecutionInformation(taskName, taskDeploymentProperties);
		TaskDefinition taskDefinition = taskExecutionInformation.getTaskDefinition();
		String registeredAppName = taskDefinition.getRegisteredAppName();
		TaskExecution taskExecution = taskExecutionRepositoryService.createTaskExecution(taskName);

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

		String id = taskLauncher.launch(request);
		if (!StringUtils.hasText(id)) {
			throw new IllegalStateException("Deployment ID is null for the task:" + taskName);
		}
		this.updateExternalExecutionId(taskExecution.getExecutionId(), id);

		TaskDeployment taskDeployment = new TaskDeployment();
		taskDeployment.setTaskDeploymentId(id);
		taskDeployment.setPlatformName(platformName);
		taskDeployment.setTaskDefinitionName(taskName);
		this.taskDeploymentRepository.save(taskDeployment);

		this.auditRecordService.populateAndSaveAuditRecordUsingMapData(
				AuditOperationType.TASK, AuditActionType.DEPLOY,
				taskDefinition.getName(),
				getAudited(taskDefinition, taskExecutionInformation.getTaskDeploymentProperties(), updatedCmdLineArgs));

		return taskExecution.getExecutionId();
	}

	private TaskLauncher findTaskLaucher(String platformName) {
		Launcher launcher = this.launcherRepository.findByName(platformName);
		if (launcher == null) {
			List<String> launcherNames =
					StreamSupport.stream(launcherRepository.findAll().spliterator(), false)
							.map(Launcher::getName)
							.collect(Collectors.toList());
			throw new IllegalStateException(String.format("No Launcher found for the platform named '%s'.  " +
							"Available platform names are %s",
					platformName, launcherNames));
		}
		TaskLauncher taskLauncher = launcher.getTaskLauncher();
		if (taskLauncher == null) {
			throw new IllegalStateException(String.format("No TaskLauncher found for the platform named '%s'",
					platformName));
		}
		return taskLauncher;
	}

	protected void updateExternalExecutionId(long executionId, String taskLaunchId) {
		this.taskRepository.updateExternalExecutionId(executionId, taskLaunchId);
	}

	private Map<String, Object> getAudited(TaskDefinition taskDefinition, Map<String, String> taskDeploymentProperties,
			List<String> commandLineArgs) {
		final Map<String, Object> auditedData = new HashMap<>(3);
		auditedData.put(TASK_DEFINITION_DSL_TEXT, this.argumentSanitizer.sanitizeTaskDsl(taskDefinition));
		auditedData.put(TASK_DEPLOYMENT_PROPERTIES,
				this.argumentSanitizer.sanitizeProperties(taskDeploymentProperties));
		auditedData.put(COMMAND_LINE_ARGS, this.argumentSanitizer.sanitizeArguments(commandLineArgs));
		return auditedData;
	}

	private List<String> updateCommandLineArgs(List<String> commandLineArgs, TaskExecution taskExecution) {
		return Stream
				.concat(commandLineArgs.stream().filter(a -> !a.startsWith("--spring.cloud.task.executionid=")),
						Stream.of("--spring.cloud.task.executionid=" + taskExecution.getExecutionId()))
				.collect(Collectors.toList());
	}

}
