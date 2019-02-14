/*
 * Copyright 2015-2019 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.common.security.support.TokenUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDeployment;
import org.springframework.cloud.dataflow.core.TaskPlatformFactory;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionMissingExternalIdException;
import org.springframework.cloud.dataflow.server.service.TaskExecutionCreationService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
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

	private static final Logger logger = LoggerFactory.getLogger(DefaultTaskExecutionService.class);

	public static final String TASK_DEFINITION_DSL_TEXT = "taskDefinitionDslText";

	public static final String TASK_DEPLOYMENT_PROPERTIES = "taskDeploymentProperties";

	public static final String COMMAND_LINE_ARGS = "commandLineArgs";

	public static final String TASK_PLATFORM_NAME = "spring.cloud.dataflow.task.platformName";

	protected final AuditRecordService auditRecordService;

	/**
	 * Used to launch apps as tasks.
	 */
	private final LauncherRepository launcherRepository;

	private final TaskExecutionCreationService taskExecutionRepositoryService;

	/**
	 * Used to create TaskExecutions.
	 */
	private final TaskRepository taskRepository;

	private final TaskExecutionInfoService taskExecutionInfoService;

	private final TaskDeploymentRepository taskDeploymentRepository;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	private final TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator;

	private final TaskExplorer taskExplorer;

	private final DataflowTaskExecutionDao dataflowTaskExecutionDao;

	public static final String TASK_DEFINITION_DSL_TEXT = "taskDefinitionDslText";

	public static final String TASK_DEPLOYMENT_PROPERTIES = "taskDeploymentProperties";

	public static final String COMMAND_LINE_ARGS = "commandLineArgs";

	public static final String TASK_PLATFORM_NAME = "spring.cloud.dataflow.task.platformName";

	/**
	 * Initializes the {@link DefaultTaskExecutionService}.
	 *
	 * @param launcherRepository the repository of task launcher used to launch task apps.
	 * @param auditRecordService the audit record service
	 * @param taskRepository the repository to use for accessing and updating task executions
	 * @param taskDeploymentRepository the repository to track task deployment
	 * @param taskExecutionInfoService the service used to setup a task execution
	 * @param taskExecutionRepositoryService the service used to create the task execution
	 */
	public DefaultTaskExecutionService(LauncherRepository launcherRepository,
			AuditRecordService auditRecordService,
			TaskRepository taskRepository,
			TaskExecutionInfoService taskExecutionInfoService,
			TaskDeploymentRepository taskDeploymentRepository,
			TaskExecutionCreationService taskExecutionRepositoryService,
			TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator,
			TaskExplorer taskExplorer,
			DataflowTaskExecutionDao dataflowTaskExecutionDao) {
		Assert.notNull(launcherRepository, "launcherRepository must not be null");
		Assert.notNull(auditRecordService, "auditRecordService must not be null");
		Assert.notNull(taskExecutionInfoService, "taskExecutionInfoService must not be null");
		Assert.notNull(taskRepository, "taskRepository must not be null");
		Assert.notNull(taskExecutionInfoService, "taskExecutionInfoService must not be null");
		Assert.notNull(taskDeploymentRepository, "taskDeploymentRepository must not be null");
		Assert.notNull(taskExecutionRepositoryService, "taskExecutionRepositoryService must not be null");
		Assert.notNull(taskAppDeploymentRequestCreator, "taskAppDeploymentRequestCreator must not be null");
		Assert.notNull(taskExplorer, "taskExplorer must not be null");
		Assert.notNull(dataflowTaskExecutionDao, "dataflowTaskExecutionDao must not be null");

		this.launcherRepository = launcherRepository;
		this.auditRecordService = auditRecordService;
		this.taskRepository = taskRepository;
		this.taskExecutionInfoService = taskExecutionInfoService;
		this.taskDeploymentRepository = taskDeploymentRepository;
		this.taskExecutionRepositoryService = taskExecutionRepositoryService;
		this.taskAppDeploymentRequestCreator = taskAppDeploymentRequestCreator;
		this.taskExplorer = taskExplorer;
		this.dataflowTaskExecutionDao = dataflowTaskExecutionDao;
	}

	@Override
	public long executeTask(String taskName, Map<String, String> taskDeploymentProperties, List<String> commandLineArgs,
			String composedTaskRunnerName) {

		String platformName = taskDeploymentProperties.get(TASK_PLATFORM_NAME);

		// If not given, use 'default'
		if (!StringUtils.hasText(platformName)) {
			platformName = "default";
		}

		// In case we have exactly one launcher, we override to that
		List<String> launcherNames = StreamSupport.stream(launcherRepository.findAll().spliterator(), false)
				.map(Launcher::getName).collect(Collectors.toList());
		if (launcherNames.size() == 1) {
			platformName = launcherNames.get(0);
		}

		// Remove since the key for task platform name will not pass validation for app, deployer, or scheduler prefix
		if (taskDeploymentProperties.containsKey(TASK_PLATFORM_NAME)) {
			taskDeploymentProperties.remove(TASK_PLATFORM_NAME);
		}

		DeploymentPropertiesUtils.validateDeploymentProperties(taskDeploymentProperties);

		TaskLauncher taskLauncher = findTaskLauncher(platformName);

		TaskDeployment existingTaskDeployment = taskDeploymentRepository
				.findTopByTaskDefinitionNameOrderByCreatedOnAsc(taskName);
		if (existingTaskDeployment != null) {
			if (!existingTaskDeployment.getPlatformName().equals(platformName)) {
				throw new IllegalStateException(String.format(
						"Task definition [%s] has already been deployed on platform [%s].  " +
						"Requested to deploy on platform [%s].",
						taskName, existingTaskDeployment.getPlatformName(), platformName));
			}
		}
		TaskExecutionInformation taskExecutionInformation = taskExecutionInfoService
				.findTaskExecutionInformation(taskName, taskDeploymentProperties, composedTaskRunnerName);

		TaskExecution taskExecution = taskExecutionRepositoryService.createTaskExecution(taskName);

		if (taskExecutionInformation.isComposed()) {
			boolean containsAccessToken = false;
			boolean useUserAccessToken = false;

			final String dataflowServerAccessTokenKey = "dataflow-server-access-token";
			final String dataflowServerUseUserAccessToken = "dataflow-server-use-user-access-token";

			for (String commandLineArg : commandLineArgs) {
				if (commandLineArg.startsWith("--" + dataflowServerAccessTokenKey)) {
					containsAccessToken = true;
				}
				if (StringUtils.trimAllWhitespace(commandLineArg).equalsIgnoreCase("--" + dataflowServerUseUserAccessToken + "=true")) {
					useUserAccessToken = true;
				}
			}

			final String dataflowAccessTokenPropertyKey = "app." + taskExecutionInformation.getTaskDefinition().getRegisteredAppName() + "." + dataflowServerAccessTokenKey;
			for (Map.Entry<String, String> taskDeploymentProperty : taskExecutionInformation.getTaskDeploymentProperties().entrySet()) {
				if (taskDeploymentProperty.getKey().equals(dataflowAccessTokenPropertyKey)) {
					containsAccessToken = true;
				}
			}

			if (!containsAccessToken && useUserAccessToken) {
				final String token = TokenUtils.getAccessToken();

				if (token != null) {
					taskExecutionInformation.getTaskDeploymentProperties().put(dataflowAccessTokenPropertyKey, token);
				}
			}
		}

		AppDeploymentRequest request = this.taskAppDeploymentRequestCreator.
				createRequest(taskExecution, taskExecutionInformation, commandLineArgs, platformName);

		// Task Manifest
		TaskManifest taskManifest = new TaskManifest();
		String composedTaskDsl = taskExecutionInformation.getTaskDefinition().getProperties().get("graph");
		if (StringUtils.hasText(composedTaskDsl)) {
			taskManifest.setDslText(composedTaskDsl); // this is now empty.
			taskManifest.setTaskDeploymentRequest(appDeploymentRequest);
			List<AppDeploymentRequest> subTaskAppDeploymentRequests = this.taskExecutionInfoService.createRequests(
					taskExecutionInformation.getTaskDefinition().getTaskName(),
					taskManifest.getDslText());
			taskManifest.setSubTaskDeploymentRequests(subTaskAppDeploymentRequests);

			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.addMixIn(Resource.class, ResourceMixin.class);
			objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			try {
				String manifestAsString = objectMapper.writeValueAsString(taskManifest);
				System.out.println(manifestAsString);
				// taskDeployment.setTaskManifestString(manifestAsString);
			}
			catch (JsonProcessingException e) {
				throw new IllegalArgumentException("Could not serialize Task Manifest", e);
			}
		}

		String taskDeploymentId = taskLauncher.launch(appDeploymentRequest);
		if (!StringUtils.hasText(taskDeploymentId)) {
			throw new IllegalStateException("Deployment ID is null for the task:" + taskName);
		}
		this.updateExternalExecutionId(taskExecution.getExecutionId(), taskDeploymentId);

		TaskDeployment taskDeployment = new TaskDeployment();
		taskDeployment.setTaskDeploymentId(taskDeploymentId);
		taskDeployment.setPlatformName(platformName);
		taskDeployment.setTaskDefinitionName(taskName);
		this.taskDeploymentRepository.save(taskDeployment);

		this.auditRecordService.populateAndSaveAuditRecordUsingMapData(
				AuditOperationType.TASK, AuditActionType.DEPLOY,
				taskExecutionInformation.getTaskDefinition().getName(),
				getAudited(taskExecutionInformation.getTaskDefinition(),
						taskExecutionInformation.getTaskDeploymentProperties(),
						appDeploymentRequest.getCommandlineArguments()));

		return taskExecution.getExecutionId();
	}

	@Override
	public long executeTask(String taskName, Map<String, String> taskDeploymentProperties, List<String> commandLineArgs) {
		return executeTask(taskName, taskDeploymentProperties, commandLineArgs, null);
	}

	@Override
	public String getLog(String platformName, String taskId) {
		Launcher launcher = this.launcherRepository.findByName(platformName);
		// In case of Cloud Foundry, fetching logs by external execution Id isn't valid as the execution instance is destroyed.
		// We need to use the task name instead.
		if (launcher != null && launcher.getType().equals(TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE)) {
			try {
				TaskDeployment taskDeployment = this.taskDeploymentRepository.findByTaskDeploymentId(taskId);
				if (taskDeployment == null) {
					throw new IllegalArgumentException();
				}
				taskId = taskDeployment.getTaskDefinitionName();
			}
			catch (Exception e) {
				return "Log could not be retrieved as the task instance is not running by the ID: "+ taskId;
			}
		}
		return findTaskLauncher(platformName).getLog(taskId);
	}

	@Override
	public void stopTaskExecution(Set<Long> ids) {
		stopTaskExecution(ids, null);
	}

	@Override
	public void stopTaskExecution(Set<Long> ids, String platform) {
		logger.info("Stopping {} task executions.", ids.size());

		Set<TaskExecution> taskExecutions = getValidStopExecutions(ids);
		Set<TaskExecution> childTaskExecutions = getValidStopChildExecutions(ids);
		for (TaskExecution taskExecution : taskExecutions) {
			cancelTaskExecution(taskExecution, platform);
		}
		childTaskExecutions.forEach(childTaskExecution -> {
			cancelTaskExecution(childTaskExecution, platform);
		});

		updateAuditInfoForTaskStops(taskExecutions.size() + childTaskExecutions.size());
	}

	private Set<TaskExecution> getValidStopExecutions(Set<Long> ids) {
		Set<TaskExecution> taskExecutions = getTaskExecutions(ids);
		validateExternalExecutionIds(taskExecutions);
		return taskExecutions;
	}

	private Set<TaskExecution> getValidStopChildExecutions(Set<Long> ids) {
		Set<Long> childTaskExecutionIds = this.dataflowTaskExecutionDao.findChildTaskExecutionIds(ids);
		Set<TaskExecution> childTaskExecutions = getTaskExecutions(childTaskExecutionIds);
		validateExternalExecutionIds(childTaskExecutions);
		return childTaskExecutions;
	}

	private void updateAuditInfoForTaskStops(long numberOfExecutionsStopped) {
		final Map<String, Object> auditData = Collections.singletonMap("Stopped Task Executions", numberOfExecutionsStopped);
		this.auditRecordService.populateAndSaveAuditRecordUsingMapData(
				AuditOperationType.TASK, AuditActionType.UNDEPLOY,
				numberOfExecutionsStopped + " Task Execution Stopped", auditData);
	}

	private void validateExternalExecutionIds(Set<TaskExecution> taskExecutions) {
		Set<Long> invalidIds = new HashSet<>();
		for(TaskExecution taskExecution: taskExecutions) {
			if(taskExecution.getExternalExecutionId() == null) {
				invalidIds.add(taskExecution.getExecutionId());
			}
		}
		if(!invalidIds.isEmpty()) {
			throw new TaskExecutionMissingExternalIdException(invalidIds);
		}
	}

	private TaskLauncher findTaskLauncher(String platformName) {
		Launcher launcher = this.launcherRepository.findByName(platformName);
		if (launcher == null) {
			List<String> launcherNames = StreamSupport.stream(launcherRepository.findAll().spliterator(), false)
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

	private void cancelTaskExecution(TaskExecution taskExecution, String platformName) {
		String platformNameToUse;
		if (StringUtils.hasText(platformName)) {
			platformNameToUse = platformName;
		} else {
			TaskDeployment taskDeployment = this.taskDeploymentRepository.findByTaskDeploymentId(taskExecution.getExternalExecutionId());
			platformNameToUse = taskDeployment.getPlatformName();
		}
		TaskLauncher taskLauncher = findTaskLauncher(platformNameToUse);
		taskLauncher.cancel(taskExecution.getExternalExecutionId());
		this.logger.info(String.format("Task execution stop request for id %s for platform %s has been submitted", taskExecution.getExecutionId(), platformNameToUse));

	}

	private Set<TaskExecution> getTaskExecutions(Set<Long> ids) {
		Set<TaskExecution> taskExecutions = new HashSet<>();
		final SortedSet<Long> nonExistingTaskExecutions = new TreeSet<>();
		for (Long id : ids) {
			final TaskExecution taskExecution = this.taskExplorer.getTaskExecution(id);
			if (taskExecution == null) {
				nonExistingTaskExecutions.add(id);
			}
			else {
				taskExecutions.add(taskExecution);
			}
		}
		if (!nonExistingTaskExecutions.isEmpty()) {
			if (nonExistingTaskExecutions.size() == 1) {
				throw new NoSuchTaskExecutionException(nonExistingTaskExecutions.first());
			}
			else {
				throw new NoSuchTaskExecutionException(nonExistingTaskExecutions);
			}
		}
		return taskExecutions;
	}

}
