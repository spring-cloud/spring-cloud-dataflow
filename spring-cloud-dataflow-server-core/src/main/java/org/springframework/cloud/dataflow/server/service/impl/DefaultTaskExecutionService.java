/*
 * Copyright 2015-2020 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDeployment;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.core.TaskPlatformFactory;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.core.dsl.visitor.ComposedTaskRunnerVisitor;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionMissingExternalIdException;
import org.springframework.cloud.dataflow.server.service.TaskExecutionCreationService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskSaveService;
import org.springframework.cloud.dataflow.server.service.impl.diff.TaskAnalysisReport;
import org.springframework.cloud.dataflow.server.service.impl.diff.TaskAnalyzer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

	private final DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao;

	private OAuth2TokenUtilsService oauth2TokenUtilsService;

	private final Map<String, List<String>> tasksBeingUpgraded = new ConcurrentHashMap<>();

	private final TaskAnalyzer taskAnalyzer = new TaskAnalyzer();

	private final TaskSaveService taskSaveService;

	private boolean autoCreateTaskDefinitions;

	private TaskConfigurationProperties taskConfigurationProperties;

	private ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties;

	/**
	 * Initializes the {@link DefaultTaskExecutionService}.
	 *
	 * @param launcherRepository the repository of task launcher used to launch task apps.
	 * @param auditRecordService the audit record service
	 * @param taskRepository the repository to use for accessing and updating task executions
	 * @param taskExecutionInfoService the task execution info service
	 * @param taskDeploymentRepository the repository to track task deployment
	 * @param taskExecutionInfoService the service used to setup a task execution
	 * @param taskExecutionRepositoryService the service used to create the task execution
	 * @param taskAppDeploymentRequestCreator the task app deployment request creator
	 * @param taskExplorer the task explorer
	 * @param dataflowTaskExecutionDao the dataflow task execution dao
	 * @param dataflowTaskExecutionMetadataDao repository used to manipulate task manifests
	 * @param oauth2TokenUtilsService the oauth2 token server
	 * @param taskSaveService the task save service
	 */
	@Deprecated
	public DefaultTaskExecutionService(LauncherRepository launcherRepository,
									   AuditRecordService auditRecordService,
									   TaskRepository taskRepository,
									   TaskExecutionInfoService taskExecutionInfoService,
									   TaskDeploymentRepository taskDeploymentRepository,
									   TaskExecutionCreationService taskExecutionRepositoryService,
									   TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator,
									   TaskExplorer taskExplorer,
									   DataflowTaskExecutionDao dataflowTaskExecutionDao,
									   DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao,
									   OAuth2TokenUtilsService oauth2TokenUtilsService,
									   TaskSaveService taskSaveService,
									   TaskConfigurationProperties taskConfigurationProperties) {
		this(launcherRepository, auditRecordService, taskRepository, taskExecutionInfoService, taskDeploymentRepository,
				taskExecutionRepositoryService, taskAppDeploymentRequestCreator, taskExplorer, dataflowTaskExecutionDao,
				dataflowTaskExecutionMetadataDao, oauth2TokenUtilsService, taskSaveService, taskConfigurationProperties,
				null);
	}

	/**
	 * Initializes the {@link DefaultTaskExecutionService}.
	 *
	 * @param launcherRepository the repository of task launcher used to launch task apps.
	 * @param auditRecordService the audit record service
	 * @param taskRepository the repository to use for accessing and updating task executions
	 * @param taskExecutionInfoService the task execution info service
	 * @param taskDeploymentRepository the repository to track task deployment
	 * @param taskExecutionInfoService the service used to setup a task execution
	 * @param taskExecutionRepositoryService the service used to create the task execution
	 * @param taskAppDeploymentRequestCreator the task app deployment request creator
	 * @param taskExplorer the task explorer
	 * @param dataflowTaskExecutionDao the dataflow task execution dao
	 * @param dataflowTaskExecutionMetadataDao repository used to manipulate task manifests
	 * @param oauth2TokenUtilsService the oauth2 token server
	 * @param taskSaveService the task save service
	 * @param composedTaskRunnerConfigurationProperties properties used to configure the composed task runner
	 */
	public DefaultTaskExecutionService(LauncherRepository launcherRepository,
									   AuditRecordService auditRecordService,
									   TaskRepository taskRepository,
									   TaskExecutionInfoService taskExecutionInfoService,
									   TaskDeploymentRepository taskDeploymentRepository,
									   TaskExecutionCreationService taskExecutionRepositoryService,
									   TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator,
									   TaskExplorer taskExplorer,
									   DataflowTaskExecutionDao dataflowTaskExecutionDao,
									   DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao,
									   OAuth2TokenUtilsService oauth2TokenUtilsService,
									   TaskSaveService taskSaveService,
									   TaskConfigurationProperties taskConfigurationProperties,
									   ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties) {
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
		Assert.notNull(dataflowTaskExecutionMetadataDao, "dataflowTaskExecutionMetadataDao must not be null");
		Assert.notNull(taskSaveService, "taskSaveService must not be null");
		Assert.notNull(taskConfigurationProperties, "taskConfigurationProperties must not be null");

		this.oauth2TokenUtilsService = oauth2TokenUtilsService;
		this.launcherRepository = launcherRepository;
		this.auditRecordService = auditRecordService;
		this.taskRepository = taskRepository;
		this.taskExecutionInfoService = taskExecutionInfoService;
		this.taskDeploymentRepository = taskDeploymentRepository;
		this.taskExecutionRepositoryService = taskExecutionRepositoryService;
		this.taskAppDeploymentRequestCreator = taskAppDeploymentRequestCreator;
		this.taskExplorer = taskExplorer;
		this.dataflowTaskExecutionDao = dataflowTaskExecutionDao;
		this.dataflowTaskExecutionMetadataDao = dataflowTaskExecutionMetadataDao;
		this.taskSaveService = taskSaveService;
		this.taskConfigurationProperties = taskConfigurationProperties;
		this.composedTaskRunnerConfigurationProperties = composedTaskRunnerConfigurationProperties;
	}

	/**
	 * Launch a task.
	 * @param taskName Name of the task definition or registered task application.
	 *                 If a task definition does not exist, one will be created if `autoCreateTask-Definitions` is true.  Must not be null or empty.
	 * @param taskDeploymentProperties Optional deployment properties. Must not be null.
	 * @param commandLineArgs Optional runtime commandline argument
	 * @return the task execution ID.
	 */
	@Override
	public long executeTask(String taskName, Map<String, String> taskDeploymentProperties, List<String> commandLineArgs) {
		// Get platform name and fallback to 'default'
		String platformName = getPlatform(taskDeploymentProperties);

		// Naive local state to prevent parallel launches to break things up
		if(this.tasksBeingUpgraded.containsKey(taskName)) {
			List<String> platforms = this.tasksBeingUpgraded.get(taskName);
			if(platforms.contains(platformName)) {
				throw new IllegalStateException(String.format(
						"Unable to launch %s on platform %s because it is being upgraded", taskName, platformName));
			}
		}
		Launcher launcher = this.launcherRepository.findByName(platformName);
		validateTaskName(taskName, launcher);
		// Remove since the key for task platform name will not pass validation for app,
		// deployer, or scheduler prefix.
		// Then validate
		if (taskDeploymentProperties.containsKey(TASK_PLATFORM_NAME)) {
			taskDeploymentProperties.remove(TASK_PLATFORM_NAME);
		}
		DeploymentPropertiesUtils.validateDeploymentProperties(taskDeploymentProperties);

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


		TaskExecutionInformation taskExecutionInformation =
				findOrCreateTaskExecutionInformation(taskName, taskDeploymentProperties, launcher.getType());

		TaskLauncher taskLauncher = findTaskLauncher(platformName);

		if (taskExecutionInformation.isComposed()) {
			handleAccessToken(commandLineArgs, taskExecutionInformation);
			TaskServiceUtils.addImagePullSecretProperty(taskDeploymentProperties,
					this.composedTaskRunnerConfigurationProperties);
			isCTRSplitValidForCurrentCTR(taskLauncher, taskExecutionInformation.getTaskDefinition());
		}

		// Create task execution for the task
		TaskExecution taskExecution = taskExecutionRepositoryService.createTaskExecution(taskName);

		// Get the previous manifest
		TaskManifest previousManifest = this.dataflowTaskExecutionMetadataDao.getLatestManifest(taskName);
		
		// Analysing task to know what to bring forward from existing
		TaskAnalysisReport report = taskAnalyzer
				.analyze(
						previousManifest != null
								? previousManifest.getTaskDeploymentRequest() != null
										? previousManifest.getTaskDeploymentRequest().getDeploymentProperties() : null
								: null,
						DeploymentPropertiesUtils.qualifyDeployerProperties(
								taskExecutionInformation.getTaskDeploymentProperties(),
								taskExecutionInformation.isComposed() ? "composed-task-runner"
										: taskExecutionInformation.getTaskDefinition().getRegisteredAppName()));
		logger.debug("Task analysis report {}", report);

		// We now have a new props and args what should really get used.
		Map<String, String> mergedTaskDeploymentProperties = report.getMergedDeploymentProperties();

		// Get the merged deployment properties and update the task exec. info
		taskExecutionInformation.setTaskDeploymentProperties(mergedTaskDeploymentProperties);

		// Finally create App deployment request
		AppDeploymentRequest request = this.taskAppDeploymentRequestCreator.createRequest(taskExecution,
				taskExecutionInformation, commandLineArgs, platformName, launcher.getType());

		TaskManifest taskManifest = createTaskManifest(platformName, request);
		String taskDeploymentId = null;
		
		try {
			if(launcher.getType().equals(TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE) && !isAppDeploymentSame(previousManifest, taskManifest)) {
				verifyTaskIsNotRunning(taskName, taskExecution, taskLauncher);
				validateAndLockUpgrade(taskName, platformName);
				logger.debug("Deleting %s and all related resources from the platform", taskName);
				taskLauncher.destroy(taskName);
			}
			this.dataflowTaskExecutionMetadataDao.save(taskExecution, taskManifest);
			taskDeploymentId = taskLauncher.launch(request);
			saveExternalExecutionId(taskExecution, taskDeploymentId);
		}
		finally {
			if(this.tasksBeingUpgraded.containsKey(taskName)) {
				List<String> platforms = this.tasksBeingUpgraded.get(taskName);
				platforms.remove(platformName);

				if(platforms.isEmpty()) {
					this.tasksBeingUpgraded.remove(taskName);
				}
			}
		}

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
						request.getCommandlineArguments()
						), platformName);

		return taskExecution.getExecutionId();
	}

	private void validateTaskName(String taskName, Launcher launcher) {
		if (launcher.getType().equals(TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE)
				|| launcher.getType().equals(TaskPlatformFactory.KUBERNETES_PLATFORM_TYPE)) {
			if (taskName.length() > 63)
				throw new IllegalStateException(String.format(
						"Task name [%s] length must be less than 64 characters to be launched on platform %s",
						taskName, launcher.getType()));
		}
	}

	private TaskExecutionInformation findOrCreateTaskExecutionInformation(String taskName, Map<String, String> taskDeploymentProperties, String platform) {

		TaskExecutionInformation taskExecutionInformation;
		try {
			 taskExecutionInformation = taskExecutionInfoService
					.findTaskExecutionInformation(taskName, taskDeploymentProperties,
							TaskServiceUtils.addDatabaseCredentials(this.taskConfigurationProperties.isUseKubernetesSecretsForDbCredentials(), platform));

		} catch (NoSuchTaskDefinitionException e) {
			if (autoCreateTaskDefinitions) {
				logger.info("Creating a Task Definition {} for registered app name {}", taskName, taskName);
				TaskDefinition taskDefinition = new TaskDefinition(taskName, taskName);
				taskSaveService.saveTaskDefinition(taskDefinition);
				taskExecutionInformation = taskExecutionInfoService
						.findTaskExecutionInformation(taskName, taskDeploymentProperties,
								TaskServiceUtils.addDatabaseCredentials(this.taskConfigurationProperties.isUseKubernetesSecretsForDbCredentials(), platform));
			}
			else {
				throw e;
			}
		}
		return taskExecutionInformation;
	}

	/**
	 * Determines if an OAuth token is available and if so, sets it as a deployment property.
	 *
	 * @param commandLineArgs args for the task execution
	 * @param taskExecutionInformation source of deployment properties
	 */
	private void handleAccessToken(List<String> commandLineArgs, TaskExecutionInformation taskExecutionInformation) {
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
		if(TaskServiceUtils.isUseUserAccessToken(this.taskConfigurationProperties, this.composedTaskRunnerConfigurationProperties)) {
			useUserAccessToken = true;
		}
		if (!containsAccessToken && useUserAccessToken && oauth2TokenUtilsService != null) {
			final String token = oauth2TokenUtilsService.getAccessTokenOfAuthenticatedUser();

			if (token != null) {
				taskExecutionInformation.getTaskDeploymentProperties().put(dataflowAccessTokenPropertyKey, token);
			}
		}
	}

	/**
	 * Stores the platform specific execution id for a given task execution
	 *
	 * @param taskExecution task execution id to associate the external execution id with
	 * @param taskDeploymentId platform specific execution id
	 */
	private void saveExternalExecutionId(TaskExecution taskExecution, String taskDeploymentId) {
		if (!StringUtils.hasText(taskDeploymentId)) {
			throw new IllegalStateException("Deployment ID is null for the task:" + taskExecution.getTaskName());
		}
		this.updateExternalExecutionId(taskExecution.getExecutionId(), taskDeploymentId);
	}

	/**
	 * Updates the deployment properties on the provided {@code AppDeploymentRequest}
	 *
	 * @param commandLineArgs command line args for the task execution
	 * @param platformName name of the platform configuration to use
	 * @param taskExecutionInformation details about the task execution request
	 * @param taskExecution task execution data
	 * @param deploymentProperties properties of the deployment
	 * @return an updated {@code AppDeploymentRequest}
	 */
	private AppDeploymentRequest updateDeploymentProperties(List<String> commandLineArgs, String platformName,
			String platformType,
			TaskExecutionInformation taskExecutionInformation, TaskExecution taskExecution,
			Map<String, String> deploymentProperties) {
		AppDeploymentRequest appDeploymentRequest;
		TaskExecutionInformation info = new TaskExecutionInformation();
		info.setTaskDefinition(taskExecutionInformation.getTaskDefinition());
		info.setAppResource(taskExecutionInformation.getAppResource());
		info.setComposed(taskExecutionInformation.isComposed());
		info.setMetadataResource(taskExecutionInformation.getMetadataResource());
		info.setOriginalTaskDefinition(taskExecutionInformation.getOriginalTaskDefinition());
		info.setTaskDeploymentProperties(deploymentProperties);

		appDeploymentRequest = this.taskAppDeploymentRequestCreator.
				createRequest(taskExecution, info, commandLineArgs, platformName, platformType);
		return appDeploymentRequest;
	}

	/**
	 * A task should not be allowed to be launched when one is running (allowing the upgrade
	 * to proceed may kill running task instances of that definition on certain platforms).
	 *
	 * @param taskName task name to check
	 * @param taskExecution the candidate TaskExecution
	 * @param taskLauncher the TaskLauncher used to verify the status of a recorded task execution.
	 */
	private void verifyTaskIsNotRunning(String taskName, TaskExecution taskExecution, TaskLauncher taskLauncher) {
		Page<TaskExecution> runningTaskExecutions =
				this.taskExplorer.findRunningTaskExecutions(taskName, PageRequest.of(0, 1));

		//Found only the candidate TaskExecution
		if(runningTaskExecutions.getTotalElements() == 1 && runningTaskExecutions.toList().get(0).getExecutionId() == taskExecution.getExecutionId()) {
			return;
		}

		/*
		 * The task repository recorded a different task execution for the task which is started but not completed.
		 * It is possible that the task failed and terminated before updating the task repository.
		 * Use the TaskLauncher to verify the actual state.
		 */
		if (runningTaskExecutions.getTotalElements() > 0) {

			LaunchState launchState = taskLauncher.status(runningTaskExecutions.toList().get(0).getExternalExecutionId()).getState();
			if (launchState.equals(LaunchState.running) || launchState.equals(LaunchState.launching)) {
				throw new IllegalStateException("Unable to update application due to currently running applications");
			}
			else {
				logger.warn("Task repository shows a running task execution for task {} but the actual state is {}."
						+ launchState.toString(), taskName, launchState);
			}
		}
	}

	/**
	 * A task should not be allowed to be launched when an upgrade is required
	 * @param taskName task name to check or lock
	 * @param platformName the platform configuration to confirm if the task is being run on
	 */
	private void validateAndLockUpgrade(String taskName, String platformName) {
		if(this.tasksBeingUpgraded.containsKey(taskName)) {
			List<String> platforms = this.tasksBeingUpgraded.get(taskName);

			if(platforms.contains(platformName)) {
				throw new IllegalStateException(String.format("Currently upgrading %s on platform %s", taskName, platformName));
			}

			platforms.add(platformName);
		}
		else {
			List<String> platformList = new ArrayList<>();
			platformList.add(platformName);
			this.tasksBeingUpgraded.put(taskName, platformList);
		}
	}

	/**
	 * Create a {@code TaskManifest}
	 *
	 * @param platformName name of the platform configuration to run the task on	 * 
	 * @param appDeploymentRequest the details about the deployment to be executed
	 * @return {@code TaskManifest}
	 */
	private TaskManifest createTaskManifest(String platformName, AppDeploymentRequest appDeploymentRequest) {
		TaskManifest taskManifest = new TaskManifest();
		taskManifest.setPlatformName(platformName);
		taskManifest.setTaskDeploymentRequest(appDeploymentRequest);
		return taskManifest;
	}

	/**
	 * Return the platform specified.  If none have been specified, then "default" will be returned.
	 *
	 * @param taskDeploymentProperties properties to interrogate if a platform has been specified
	 * @return name of the platform configuration to execute the task on
	 */
	private String getPlatform(Map<String, String> taskDeploymentProperties) {
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

		return platformName;
	}

	/**
	 * Determines if the requested deployment is the same (thereby not needing an upgrade) vs different and needing to
	 * be upgraded via the data in the manifests.  Specifically, the URI of the {@code Resource}, the app properties
	 * and the deployment properties are all evaluated in this comparison.
	 *
	 * @param previousManifest the manifest for the last task execution
	 * @param newManifest the manifest for the task execution currently being requested
	 * @return {@code true} if no upgrade is required, {@code false} if an upgrade is required.
	 */
	private boolean isAppDeploymentSame(TaskManifest previousManifest, TaskManifest newManifest) {
		boolean same;

		if(previousManifest == null) {
			return true;
		}

		Resource previousResource = previousManifest.getTaskDeploymentRequest().getResource();
		Resource newResource = newManifest.getTaskDeploymentRequest().getResource();

		try {
			logger.debug("Previous resource was %s and new resource is %s", previousResource.getURI().toString(), newResource.getURI().toString());
		}
		catch (IOException e) {
			logger.debug("Unable to obtain URIs from resources to be compared in debug log statement", e);
		}

		same = previousResource.equals(newResource);

		Map<String, String> previousAppProperties = previousManifest.getTaskDeploymentRequest().getDefinition().getProperties();
		Map<String, String> newAppProperties = newManifest.getTaskDeploymentRequest().getDefinition().getProperties();

		Map<String, String> previousDeploymentProperties = previousManifest.getTaskDeploymentRequest().getDeploymentProperties();
		Map<String, String> newDeploymentProperties = newManifest.getTaskDeploymentRequest().getDeploymentProperties();

		same = same && previousDeploymentProperties.equals(newDeploymentProperties) && previousAppProperties.equals(newAppProperties);

		return same;
	}

	@Override
	/**
	 * Return the log content of the task execution identified by the given task deployment ID (external execution ID).
	 * In case of concurrent task executions on Cloud Foundry, the logs of all the concurrent executions are displayed.
	 * Also, on Cloud Foundry, the task execution log is retrieved only for the latest execution that matches the
	 * given deployment ID (external execution ID).
	 */
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
				String taskName = taskDeployment.getTaskDefinitionName();
				TaskExecution taskExecution = this.taskExplorer.getLatestTaskExecutionForTaskName(taskName);
				if (taskExecution != null && !taskExecution.getExternalExecutionId().equals(taskId)) {
					return "";
				}
				// Override the task ID to be task name as task execution log is identified by the task name on CF.
				taskId = taskName;
			}
			catch (Exception e) {
				return "Log could not be retrieved as the task instance is not running by the ID: "+ taskId;
			}
		}
		String result;
		try {
			result = findTaskLauncher(platformName).getLog(taskId);
		}
		catch (Exception iae) {
			logger.warn("Failed to retrieve the log, returning verification message. ", iae);
			result = "Log could not be retrieved.  Verify that deployments are still available.";
		}
		return result;
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

	@Override
	public TaskManifest findTaskManifestById(Long id) {
		TaskExecution taskExecution = this.taskExplorer.getTaskExecution(id);
		return this.dataflowTaskExecutionMetadataDao.findManifestById(taskExecution.getExecutionId());
	}

	public void setAutoCreateTaskDefinitions(boolean autoCreateTaskDefinitions) {
		this.autoCreateTaskDefinitions = autoCreateTaskDefinitions;
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
				numberOfExecutionsStopped + " Task Execution Stopped", auditData, null);
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

	private void isCTRSplitValidForCurrentCTR(TaskLauncher taskLauncher, TaskDefinition taskDefinition) {
		TaskParser taskParser = new TaskParser("composed-task-runner", taskDefinition.getProperties().get("graph"), true, true);
		TaskNode taskNode = taskParser.parse();
		ComposedTaskRunnerVisitor composedRunnerVisitor = new ComposedTaskRunnerVisitor();
		taskNode.accept(composedRunnerVisitor);
		if(composedRunnerVisitor.getHighCount() > taskLauncher.getMaximumConcurrentTasks()) {
			throw new IllegalArgumentException(String.format("One or more of the " +
					"splits in the composed task contains a task count that exceeds " +
					"the maximumConcurrentTasks count of %s",
					taskLauncher.getMaximumConcurrentTasks()));
		}
	}
}
