/*
 * Copyright 2015-2023 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.AggregateTaskExplorer;
import org.springframework.cloud.dataflow.aggregate.task.DataflowTaskExecutionQueryDao;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.aggregate.task.TaskRepositoryContainer;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.LaunchResponse;
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
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDaoContainer;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
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
import org.springframework.cloud.task.listener.TaskException;
import org.springframework.cloud.task.listener.TaskExecutionException;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.core.env.PropertyResolver;
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
 * @author Corneil du Plessis
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
	private final TaskRepositoryContainer taskRepositoryContainer;

	private final TaskExecutionInfoService taskExecutionInfoService;

	private final TaskDeploymentRepository taskDeploymentRepository;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	private final TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator;

	private final AggregateTaskExplorer taskExplorer;

	private final DataflowTaskExecutionDaoContainer dataflowTaskExecutionDaoContainer;

	private final DataflowTaskExecutionMetadataDaoContainer dataflowTaskExecutionMetadataDaoContainer;

	private final OAuth2TokenUtilsService oauth2TokenUtilsService;

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final TaskDefinitionReader taskDefinitionReader;

	private final Map<String, List<String>> tasksBeingUpgraded = new ConcurrentHashMap<>();

	private final TaskAnalyzer taskAnalyzer = new TaskAnalyzer();

	private final TaskSaveService taskSaveService;

	private boolean autoCreateTaskDefinitions;

	private final TaskConfigurationProperties taskConfigurationProperties;

	private final ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties;

	private final AggregateExecutionSupport aggregateExecutionSupport;

	private final DataflowTaskExecutionQueryDao dataflowTaskExecutionQueryDao;

	private final PropertyResolver propertyResolver;

	private static final Pattern TASK_NAME_PATTERN = Pattern.compile("[a-zA-Z]([-a-zA-Z0-9]*[a-zA-Z0-9])?");

	private static final String TASK_NAME_VALIDATION_MSG = "Task name must consist of alphanumeric characters " +
		"or '-', start with an alphabetic character, and end with an alphanumeric character (e.g. 'my-name', " +
		"or 'abc-123')";

	/**
	 * Initializes the {@link DefaultTaskExecutionService}.
	 *
	 * @param propertyResolver                          the spring application context
	 * @param launcherRepository                        the repository of task launcher used to launch task apps.
	 * @param auditRecordService                        the audit record service
	 * @param taskRepositoryContainer                   the container of repositories to use for accessing and updating task executions
	 * @param taskExecutionInfoService                  the service used to setup a task execution
	 * @param taskDeploymentRepository                  the repository to track task deployment
	 * @param taskDefinitionRepository                  the repository to query the task definition
	 * @param taskDefinitionReader                      use task definition repository to retrieve definition
	 * @param taskExecutionRepositoryService            the service used to create the task execution
	 * @param taskAppDeploymentRequestCreator           the task app deployment request creator
	 * @param taskExplorer                              the task explorer
	 * @param dataflowTaskExecutionDaoContainer         the dataflow task execution dao
	 * @param dataflowTaskExecutionMetadataDaoContainer repository used to manipulate task manifests
	 * @param dataflowTaskExecutionQueryDao             repository to query aggregate TaskExecution data
	 * @param oauth2TokenUtilsService                   the oauth2 token server
	 * @param taskSaveService                           the task save service
	 * @param taskConfigurationProperties               task configuration properties.
	 * @param aggregateExecutionSupport                 support for selecting SchemaVersionTarget
	 */
	@Deprecated
	public DefaultTaskExecutionService(
		PropertyResolver propertyResolver,
		LauncherRepository launcherRepository,
		AuditRecordService auditRecordService,
		TaskRepositoryContainer taskRepositoryContainer,
		TaskExecutionInfoService taskExecutionInfoService,
		TaskDeploymentRepository taskDeploymentRepository,
		TaskDefinitionRepository taskDefinitionRepository,
		TaskDefinitionReader taskDefinitionReader,
		TaskExecutionCreationService taskExecutionRepositoryService,
		TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator,
		AggregateTaskExplorer taskExplorer,
		DataflowTaskExecutionDaoContainer dataflowTaskExecutionDaoContainer,
		DataflowTaskExecutionMetadataDaoContainer dataflowTaskExecutionMetadataDaoContainer,
		DataflowTaskExecutionQueryDao dataflowTaskExecutionQueryDao,
		OAuth2TokenUtilsService oauth2TokenUtilsService,
		TaskSaveService taskSaveService,
		TaskConfigurationProperties taskConfigurationProperties,
		AggregateExecutionSupport aggregateExecutionSupport
	) {
		this(propertyResolver,
			launcherRepository,
			auditRecordService,
			taskRepositoryContainer,
			taskExecutionInfoService,
			taskDeploymentRepository,
			taskDefinitionRepository,
			taskDefinitionReader,
			taskExecutionRepositoryService,
			taskAppDeploymentRequestCreator,
			taskExplorer,
			dataflowTaskExecutionDaoContainer,
			dataflowTaskExecutionMetadataDaoContainer,
			dataflowTaskExecutionQueryDao,
			oauth2TokenUtilsService,
			taskSaveService,
			taskConfigurationProperties,
			aggregateExecutionSupport,
			null);
	}

	/**
	 * Initializes the {@link DefaultTaskExecutionService}.
	 *
	 * @param propertyResolver                          the spring application context
	 * @param launcherRepository                        the repository of task launcher used to launch task apps.
	 * @param auditRecordService                        the audit record service
	 * @param taskRepositoryContainer                   the container of repositories to use for accessing and updating task executions
	 * @param taskExecutionInfoService                  the task execution info service
	 * @param taskDeploymentRepository                  the repository to track task deployment
	 * @param taskDefinitionRepository                  the repository to query the task definition
	 * @param taskDefinitionReader                      uses task definition repository to retrieve definition
	 * @param taskExecutionRepositoryService            the service used to create the task execution
	 * @param taskAppDeploymentRequestCreator           the task app deployment request creator
	 * @param taskExplorer                              the task explorer
	 * @param dataflowTaskExecutionDaoContainer         the dataflow task execution dao
	 * @param dataflowTaskExecutionMetadataDaoContainer repository used to manipulate task manifests
	 * @param dataflowTaskExecutionQueryDao             repository to query aggregate task execution data.
	 * @param oauth2TokenUtilsService                   the oauth2 token server
	 * @param taskSaveService                           the task save service
	 * @param taskConfigurationProperties               task configuration properties
	 * @param aggregateExecutionSupport                 support for selecting SchemaVersionTarget.
	 * @param composedTaskRunnerConfigurationProperties properties used to configure the composed task runner
	 */
	public DefaultTaskExecutionService(
		PropertyResolver propertyResolver,
		LauncherRepository launcherRepository,
		AuditRecordService auditRecordService,
		TaskRepositoryContainer taskRepositoryContainer,
		TaskExecutionInfoService taskExecutionInfoService,
		TaskDeploymentRepository taskDeploymentRepository,
		TaskDefinitionRepository taskDefinitionRepository,
		TaskDefinitionReader taskDefinitionReader,
		TaskExecutionCreationService taskExecutionRepositoryService,
		TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator,
		AggregateTaskExplorer taskExplorer,
		DataflowTaskExecutionDaoContainer dataflowTaskExecutionDaoContainer,
		DataflowTaskExecutionMetadataDaoContainer dataflowTaskExecutionMetadataDaoContainer,
		DataflowTaskExecutionQueryDao dataflowTaskExecutionQueryDao,
		OAuth2TokenUtilsService oauth2TokenUtilsService,
		TaskSaveService taskSaveService,
		TaskConfigurationProperties taskConfigurationProperties,
		AggregateExecutionSupport aggregateExecutionSupport,
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties
	) {
		Assert.notNull(propertyResolver, "propertyResolver must not be null");
		Assert.notNull(launcherRepository, "launcherRepository must not be null");
		Assert.notNull(auditRecordService, "auditRecordService must not be null");
		Assert.notNull(taskExecutionInfoService, "taskExecutionInfoService must not be null");
		Assert.notNull(taskRepositoryContainer, "taskRepositoryContainer must not be null");
		Assert.notNull(taskExecutionInfoService, "taskExecutionInfoService must not be null");
		Assert.notNull(taskDeploymentRepository, "taskDeploymentRepository must not be null");
		Assert.notNull(taskExecutionRepositoryService, "taskExecutionRepositoryService must not be null");
		Assert.notNull(taskAppDeploymentRequestCreator, "taskAppDeploymentRequestCreator must not be null");
		Assert.notNull(taskExplorer, "taskExplorer must not be null");
		Assert.notNull(dataflowTaskExecutionDaoContainer, "dataflowTaskExecutionDaoContainer must not be null");
		Assert.notNull(dataflowTaskExecutionMetadataDaoContainer, "dataflowTaskExecutionMetadataDaoContainer must not be null");
		Assert.notNull(taskSaveService, "taskSaveService must not be null");
		Assert.notNull(taskConfigurationProperties, "taskConfigurationProperties must not be null");
		Assert.notNull(aggregateExecutionSupport, "compositeExecutionSupport must not be null");
		Assert.notNull(taskDefinitionRepository, "taskDefinitionRepository must not be null");
		Assert.notNull(taskDefinitionReader, "taskDefinitionReader must not be null");

		this.propertyResolver = propertyResolver;
		this.oauth2TokenUtilsService = oauth2TokenUtilsService;
		this.launcherRepository = launcherRepository;
		this.auditRecordService = auditRecordService;
		this.taskRepositoryContainer = taskRepositoryContainer;
		this.taskExecutionInfoService = taskExecutionInfoService;
		this.taskDeploymentRepository = taskDeploymentRepository;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskDefinitionReader = taskDefinitionReader;
		this.taskExecutionRepositoryService = taskExecutionRepositoryService;
		this.taskAppDeploymentRequestCreator = taskAppDeploymentRequestCreator;
		this.taskExplorer = taskExplorer;
		this.dataflowTaskExecutionDaoContainer = dataflowTaskExecutionDaoContainer;
		this.dataflowTaskExecutionMetadataDaoContainer = dataflowTaskExecutionMetadataDaoContainer;
		this.taskSaveService = taskSaveService;
		this.taskConfigurationProperties = taskConfigurationProperties;
		this.aggregateExecutionSupport = aggregateExecutionSupport;
		this.composedTaskRunnerConfigurationProperties = composedTaskRunnerConfigurationProperties;
		this.dataflowTaskExecutionQueryDao = dataflowTaskExecutionQueryDao;

	}

	/**
	 * Launch a task.
	 *
	 * @param taskName                 Name of the task definition or registered task application.
	 *                                 If a task definition does not exist, one will be created if `autoCreateTask-Definitions` is true.  Must not be null or empty.
	 * @param taskDeploymentProperties Optional deployment properties. Must not be null.
	 * @param commandLineArgs          Optional runtime commandline argument
	 * @return the task execution ID.
	 */
	@Override
	public LaunchResponse executeTask(String taskName, Map<String, String> taskDeploymentProperties, List<String> commandLineArgs) {
		// Get platform name and fallback to 'default'
		String platformName = getPlatform(taskDeploymentProperties);
		String platformType = StreamSupport.stream(launcherRepository.findAll().spliterator(), true)
			.filter(deployer -> deployer.getName().equalsIgnoreCase(platformName))
			.map(Launcher::getType)
			.findFirst()
			.orElse("unknown");
		if (platformType.equals(TaskPlatformFactory.KUBERNETES_PLATFORM_TYPE) && !TASK_NAME_PATTERN.matcher(taskName).matches()) {
			throw new TaskException(String.format("Task name %s is invalid. %s", taskName, TASK_NAME_VALIDATION_MSG));
		}
		// Naive local state to prevent parallel launches to break things up
		if (this.tasksBeingUpgraded.containsKey(taskName)) {
			List<String> platforms = this.tasksBeingUpgraded.get(taskName);
			if (platforms.contains(platformName)) {
				throw new IllegalStateException(String.format(
					"Unable to launch %s on platform %s because it is being upgraded", taskName, platformName));
			}
		}
		Launcher launcher = this.launcherRepository.findByName(platformName);
		if (launcher == null) {
			throw new IllegalStateException(String.format("No launcher was available for platform %s", platformName));
		}
		validateTaskName(taskName, launcher);
		// Remove since the key for task platform name will not pass validation for app,
		// deployer, or scheduler prefix.
		// Then validate
		Map<String, String> deploymentProperties = new HashMap<>(taskDeploymentProperties);
		deploymentProperties.remove(TASK_PLATFORM_NAME);
		DeploymentPropertiesUtils.validateDeploymentProperties(deploymentProperties);
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
		List<String> commandLineArguments = new ArrayList<>(commandLineArgs);
		TaskDefinition taskDefinition = taskDefinitionRepository.findByTaskName(taskName);

		String taskAppName = taskDefinition != null ? taskDefinition.getRegisteredAppName() : taskName;

		SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget(taskAppName, taskDefinition);
		Assert.notNull(schemaVersionTarget, "schemaVersionTarget not found for " + taskAppName);

		DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao = dataflowTaskExecutionMetadataDaoContainer.get(schemaVersionTarget.getName());
		// Get the previous manifest
		TaskManifest previousManifest = dataflowTaskExecutionMetadataDao.getLatestManifest(taskName);
		Map<String, String> previousTaskDeploymentProperties = previousManifest != null
			&& previousManifest.getTaskDeploymentRequest() != null
			&& previousManifest.getTaskDeploymentRequest().getDeploymentProperties() != null
			? previousManifest.getTaskDeploymentRequest().getDeploymentProperties()
			: Collections.emptyMap();

		TaskExecutionInformation taskExecutionInformation = findOrCreateTaskExecutionInformation(taskName,
			deploymentProperties, launcher.getType(), previousTaskDeploymentProperties);

		String version = null;
		if (taskExecutionInformation.isComposed()) {
			commandLineArguments = TaskServiceUtils.convertCommandLineArgsToCTRFormat(commandLineArguments);
		} else {
			Set<String> appNames = taskExecutionInfoService.taskNames(taskName);
			Assert.isTrue(appNames.size() == 1, () -> "Expected one entry in " + appNames);
			String appName = appNames.iterator().next();
			List<String> names = new ArrayList<>(Arrays.asList(StringUtils.delimitedListToStringArray(appName, ",")));
			String registeredName = names.get(0);
			String appId = registeredName;
			if (names.size() > 1) {
				appId = names.get(1);
			}
			String appVersion = deploymentProperties.get("version." + appId);
			if (StringUtils.hasText(appVersion)) {
				version = appVersion;
			}
			schemaVersionTarget = this.aggregateExecutionSupport.findSchemaVersionTarget(registeredName, appVersion, taskDefinitionReader);
			dataflowTaskExecutionMetadataDao = dataflowTaskExecutionMetadataDaoContainer.get(schemaVersionTarget.getName());
			addPrefixCommandLineArgs(schemaVersionTarget, "app." + appId + ".", commandLineArguments);
			addPrefixProperties(schemaVersionTarget, "app." + appId + ".", deploymentProperties);
			String regex = String.format("app\\.%s\\.\\d+=", appId);
			commandLineArguments = commandLineArguments.stream()
				.map(arg -> arg.replaceFirst(regex, ""))
				.collect(Collectors.toList());

		}

		TaskLauncher taskLauncher = findTaskLauncher(platformName);
		addDefaultDeployerProperties(platformType, schemaVersionTarget, deploymentProperties);
		if (taskExecutionInformation.isComposed()) {
			Set<String> appNames = taskExecutionInfoService.composedTaskChildNames(taskName);
			if (taskDefinition != null) {
				logger.info("composedTask:dsl={}:appNames:{}", taskDefinition.getDslText(), appNames);
			} else {
				logger.info("composedTask:appNames:{}", appNames);
			}
			addPrefixProperties(schemaVersionTarget, "app.composed-task-runner.", deploymentProperties);
			addPrefixProperties(schemaVersionTarget, "app." + taskName + ".", deploymentProperties);
			for (String appName : appNames) {
				List<String> names = new ArrayList<>(Arrays.asList(StringUtils.delimitedListToStringArray(appName, ",")));
				String registeredName = names.get(0);
				String appId = registeredName;
				if (names.size() > 1) {
					appId = names.get(1);
				}

				String appVersion = deploymentProperties.get("version." + taskName + "-" + appId + "." + appId);
				if (!StringUtils.hasText(appVersion)) {
					appVersion = deploymentProperties.get("version." + taskName + "-" + appId);
				}
				if (!StringUtils.hasText(appVersion)) {
					appVersion = deploymentProperties.get("version." + appId);
				}
				SchemaVersionTarget appSchemaTarget = this.aggregateExecutionSupport.findSchemaVersionTarget(registeredName, appVersion, taskDefinitionReader);
				logger.debug("ctr:{}:registeredName={}, schemaTarget={}", names, registeredName, appSchemaTarget.getName());
				deploymentProperties.put("app.composed-task-runner.composed-task-app-properties.app." + taskName + "-" + appId + ".spring.cloud.task.tablePrefix",
					appSchemaTarget.getTaskPrefix());
				deploymentProperties.put("app.composed-task-runner.composed-task-app-properties.app." + appId + ".spring.cloud.task.tablePrefix",
					appSchemaTarget.getTaskPrefix());
				deploymentProperties.put("app." + taskName + "-" + appId + ".spring.batch.jdbc.table-prefix", appSchemaTarget.getBatchPrefix());
				deploymentProperties.put("app." + registeredName + ".spring.batch.jdbc.table-prefix", appSchemaTarget.getBatchPrefix());
			}
			logger.debug("ctr:added:{}:{}", taskName, deploymentProperties);
			handleAccessToken(commandLineArguments, taskExecutionInformation);
			TaskServiceUtils.addImagePullSecretProperty(deploymentProperties,
				this.composedTaskRunnerConfigurationProperties);
			isCTRSplitValidForCurrentCTR(taskLauncher, taskExecutionInformation.getTaskDefinition());
		}

		// Create task execution for the task
		TaskExecution taskExecution = taskExecutionRepositoryService.createTaskExecution(taskName, version);
		Assert.isTrue(taskExecution.getExecutionId() > 0, () -> "Expected executionId > 0 for " + taskName);
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
		//capture merged deployment properties that are not qualified so they can be saved in the manifest for relaunching tasks.
		Map<String, String> mergedTaskUnqualifiedDeploymentProperties = taskAnalyzer
			.analyze(
				previousManifest != null
					? previousManifest.getTaskDeploymentRequest() != null
					? previousManifest.getTaskDeploymentRequest().getDeploymentProperties() : null
					: null,
				taskExecutionInformation.getTaskDeploymentProperties()).getMergedDeploymentProperties();
		// Get the merged deployment properties and update the task exec. info
		taskExecutionInformation.setTaskDeploymentProperties(mergedTaskDeploymentProperties);

		// Finally create App deployment request
		AppDeploymentRequest request = this.taskAppDeploymentRequestCreator.createRequest(taskExecution,
			taskExecutionInformation, commandLineArguments, platformName, launcher.getType());

		TaskManifest taskManifest = createTaskManifest(platformName, request, mergedTaskUnqualifiedDeploymentProperties);
		String taskDeploymentId;

		try {
			if (launcher.getType().equals(TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE) && !isAppDeploymentSame(previousManifest, taskManifest)) {
				verifyTaskIsNotRunning(taskName, taskExecution, taskLauncher);
				validateAndLockUpgrade(taskName, platformName);
				logger.debug("Deleting {} and all related resources from the platform", taskName);
				taskLauncher.destroy(taskName);
			}

			dataflowTaskExecutionMetadataDao.save(taskExecution, taskManifest);
			taskDeploymentId = taskLauncher.launch(request);
			saveExternalExecutionId(taskExecution, version, taskDeploymentId);
		} finally {
			if (this.tasksBeingUpgraded.containsKey(taskName)) {
				List<String> platforms = this.tasksBeingUpgraded.get(taskName);
				platforms.remove(platformName);

				if (platforms.isEmpty()) {
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

		return new LaunchResponse(taskExecution.getExecutionId(), schemaVersionTarget.getName());
	}

	private void addDefaultDeployerProperties(
		String platformType,
		SchemaVersionTarget schemaVersionTarget,
		Map<String, String> deploymentProperties
	) {
		String bootVersion = schemaVersionTarget.getSchemaVersion().getBootVersion();
		switch (platformType) {
			case TaskPlatformFactory.LOCAL_PLATFORM_TYPE: {
				String javaHome = propertyResolver.getProperty("spring.cloud.dataflow.defaults.boot" + bootVersion + ".local.javaHomePath");
				if (StringUtils.hasText(javaHome)) {
					String property = "spring.cloud.deployer.local.javaHomePath." + bootVersion;
					addProperty(property, javaHome, deploymentProperties);
				}
				break;
			}
			case TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE: {
				String buildpack = propertyResolver.getProperty("spring.cloud.dataflow.defaults.boot" + bootVersion + ".cloudfoundry.buildpack");
				if (StringUtils.hasText(buildpack)) {
					String property = "spring.cloud.deployer.cloudfoundry.buildpack";
					addProperty(property, buildpack, deploymentProperties);
				}
				String buildpacks = propertyResolver.getProperty("spring.cloud.dataflow.defaults.boot" + bootVersion + ".cloudfoundry.buildpacks");
				if (StringUtils.hasText(buildpacks)) {
					String property = "spring.cloud.deployer.cloudfoundry.buildpacks";
					addProperty(property, buildpacks, deploymentProperties);
				}
				break;
			}
		}
	}

	private static void addProperty(String property, String value, Map<String, String> properties) {
		if (properties.containsKey(property)) {
			logger.info("overriding:{}={}", property, properties.get(property));
		} else {
			logger.info("adding:{}={}", property, value);
		}
		properties.put(property, value);
	}

	private static void addPrefixProperties(SchemaVersionTarget schemaVersionTarget, String prefix, Map<String, String> deploymentProperties) {
		addProperty(prefix + "spring.cloud.task.initialize-enabled", "false", deploymentProperties);
		addProperty(prefix + "spring.batch.jdbc.table-prefix", schemaVersionTarget.getBatchPrefix(), deploymentProperties);
		addProperty(prefix + "spring.cloud.task.tablePrefix", schemaVersionTarget.getTaskPrefix(), deploymentProperties);
		addProperty(prefix + "spring.cloud.task.schemaTarget", schemaVersionTarget.getName(), deploymentProperties);
		addProperty(prefix + "spring.cloud.deployer.bootVersion", schemaVersionTarget.getSchemaVersion().getBootVersion(), deploymentProperties);
	}

	private static void addPrefixCommandLineArgs(SchemaVersionTarget schemaVersionTarget, String prefix, List<String> commandLineArgs) {
		addCommandLine(prefix + "spring.cloud.task.initialize-enabled", "false", commandLineArgs);
		addCommandLine(prefix + "spring.batch.jdbc.table-prefix", schemaVersionTarget.getBatchPrefix(), commandLineArgs);
		addCommandLine(prefix + "spring.cloud.task.tablePrefix", schemaVersionTarget.getTaskPrefix(), commandLineArgs);
		addCommandLine(prefix + "spring.cloud.task.schemaTarget", schemaVersionTarget.getName(), commandLineArgs);
		addCommandLine(prefix + "spring.cloud.deployer.bootVersion", schemaVersionTarget.getSchemaVersion().getBootVersion(), commandLineArgs);
	}

	private static void addCommandLine(String property, String value, List<String> commandLineArgs) {
		String argPrefix = "--" + property + "=";
		commandLineArgs.removeIf(item -> item.startsWith(argPrefix));
		commandLineArgs.add(argPrefix + value);
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

	private TaskExecutionInformation findOrCreateTaskExecutionInformation(
		String taskName,
		Map<String, String> taskDeploymentProperties, String platform,
		Map<String, String> previousTaskDeploymentProperties
	) {

		TaskExecutionInformation taskExecutionInformation;
		try {
			taskExecutionInformation = taskExecutionInfoService.findTaskExecutionInformation(taskName,
				taskDeploymentProperties,
				TaskServiceUtils.addDatabaseCredentials(
					this.taskConfigurationProperties.isUseKubernetesSecretsForDbCredentials(), platform),
				previousTaskDeploymentProperties);

		} catch (NoSuchTaskDefinitionException e) {
			if (autoCreateTaskDefinitions) {
				logger.info("Creating a Task Definition {} for registered app name {}", taskName, taskName);
				TaskDefinition taskDefinition = new TaskDefinition(taskName, taskName);
				taskSaveService.saveTaskDefinition(taskDefinition);
				taskExecutionInformation = taskExecutionInfoService.findTaskExecutionInformation(taskName,
					taskDeploymentProperties,
					TaskServiceUtils.addDatabaseCredentials(
						this.taskConfigurationProperties.isUseKubernetesSecretsForDbCredentials(), platform),
					previousTaskDeploymentProperties);
			} else {
				throw e;
			}
		}
		return taskExecutionInformation;
	}

	/**
	 * Determines if an OAuth token is available and if so, sets it as a deployment property.
	 *
	 * @param commandLineArgs          args for the task execution
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

		final String dataflowAccessTokenPropertyKey = "app." + taskExecutionInformation.getTaskDefinition()
			.getRegisteredAppName() + "." + dataflowServerAccessTokenKey;
		for (Map.Entry<String, String> taskDeploymentProperty : taskExecutionInformation.getTaskDeploymentProperties().entrySet()) {
			if (taskDeploymentProperty.getKey().equals(dataflowAccessTokenPropertyKey)) {
				containsAccessToken = true;
				break;
			}
		}
		if (TaskServiceUtils.isUseUserAccessToken(this.taskConfigurationProperties, this.composedTaskRunnerConfigurationProperties)) {
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
	 * @param taskExecution    task execution id to associate the external execution id with
	 * @param taskDeploymentId platform specific execution id
	 */
	private void saveExternalExecutionId(TaskExecution taskExecution, String version, String taskDeploymentId) {
		if (!StringUtils.hasText(taskDeploymentId)) {
			throw new IllegalStateException("Deployment ID is null for the task:" + taskExecution.getTaskName());
		}
		SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget(taskExecution.getTaskName(), version, taskDefinitionReader);
		this.updateExternalExecutionId(taskExecution.getExecutionId(), taskDeploymentId, schemaVersionTarget.getName());
		taskExecution.setExternalExecutionId(taskDeploymentId);
	}

	/**
	 * A task should not be allowed to be launched when one is running (allowing the upgrade
	 * to proceed may kill running task instances of that definition on certain platforms).
	 *
	 * @param taskName      task name to check
	 * @param taskExecution the candidate TaskExecution
	 * @param taskLauncher  the TaskLauncher used to verify the status of a recorded task execution.
	 */
	private void verifyTaskIsNotRunning(String taskName, TaskExecution taskExecution, TaskLauncher taskLauncher) {
		Page<AggregateTaskExecution> runningTaskExecutions =
			this.taskExplorer.findRunningTaskExecutions(taskName, PageRequest.of(0, 1));

		//Found only the candidate TaskExecution
		if (runningTaskExecutions.getTotalElements() == 1 && runningTaskExecutions.toList().get(0).getExecutionId() == taskExecution.getExecutionId()) {
			return;
		}

		/*
		 * The task repository recorded a different task execution for the task which is started but not completed.
		 * It is possible that the task failed and terminated before updating the task repository.
		 * Use the TaskLauncher to verify the actual state.
		 */
		if (runningTaskExecutions.getTotalElements() > 0) {
			AggregateTaskExecution latestRunningExecution = runningTaskExecutions.toList().get(0);
			if (latestRunningExecution.getExternalExecutionId() == null) {
				logger.warn("Task repository shows a running task execution for task {} with no externalExecutionId.",
					taskName);
				return;
			}
			LaunchState launchState = taskLauncher.status(latestRunningExecution.getExternalExecutionId()).getState();

			if (launchState.equals(LaunchState.running) || launchState.equals(LaunchState.launching)) {
				throw new IllegalStateException("Unable to update application due to currently running applications");
			} else {
				logger.warn("Task repository shows a running task execution for task {} but the actual state is {}.", taskName, launchState);
			}
		}
	}

	/**
	 * A task should not be allowed to be launched when an upgrade is required
	 *
	 * @param taskName     task name to check or lock
	 * @param platformName the platform configuration to confirm if the task is being run on
	 */
	private void validateAndLockUpgrade(String taskName, String platformName) {
		if (this.tasksBeingUpgraded.containsKey(taskName)) {
			List<String> platforms = this.tasksBeingUpgraded.get(taskName);

			if (platforms.contains(platformName)) {
				throw new IllegalStateException(String.format("Currently upgrading %s on platform %s", taskName, platformName));
			}

			platforms.add(platformName);
		} else {
			List<String> platformList = new ArrayList<>();
			platformList.add(platformName);
			this.tasksBeingUpgraded.put(taskName, platformList);
		}
	}

	/**
	 * Create a {@code TaskManifest}
	 *
	 * @param platformName         name of the platform configuration to run the task on
	 * @param appDeploymentRequest the details about the deployment to be executed
	 * @return {@code TaskManifest}
	 */
	private TaskManifest createTaskManifest(String platformName, AppDeploymentRequest appDeploymentRequest, Map<String, String> taskDeploymentProperties) {
		TaskManifest taskManifest = new TaskManifest();
		taskManifest.setPlatformName(platformName);
		AppDeploymentRequest appRequestWithTaggedProps = new AppDeploymentRequest(
			appDeploymentRequest.getDefinition(), appDeploymentRequest.getResource(),
			taskDeploymentProperties,
			appDeploymentRequest.getCommandlineArguments());
		taskManifest.setTaskDeploymentRequest(appRequestWithTaggedProps);
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
	 * @param newManifest      the manifest for the task execution currently being requested
	 * @return {@code true} if no upgrade is required, {@code false} if an upgrade is required.
	 */
	private boolean isAppDeploymentSame(TaskManifest previousManifest, TaskManifest newManifest) {

		if (previousManifest == null) {
			return true;
		}

		Resource previousResource = previousManifest.getTaskDeploymentRequest().getResource();
		Resource newResource = newManifest.getTaskDeploymentRequest().getResource();

		try {
			logger.debug("Previous resource was {} and new resource is {}", previousResource.getURI(), newResource.getURI());
		} catch (IOException e) {
			logger.debug("Unable to obtain URIs from resources to be compared in debug log statement", e);
		}

		if (previousResource.equals(newResource)) {
			Map<String, String> previousAppProperties = previousManifest.getTaskDeploymentRequest().getDefinition().getProperties();
			Map<String, String> newAppProperties = newManifest.getTaskDeploymentRequest().getDefinition().getProperties();
			if (previousAppProperties.equals(newAppProperties)) {
				Map<String, String> previousDeploymentProperties = previousManifest.getTaskDeploymentRequest().getDeploymentProperties();
				Map<String, String> newDeploymentProperties = newManifest.getTaskDeploymentRequest().getDeploymentProperties();
				return previousDeploymentProperties.equals(newDeploymentProperties);
			}
		}
		return false;
	}


	/**
	 * Return the log content of the task execution identified by the given task deployment ID (external execution ID).
	 * In case of concurrent task executions on Cloud Foundry, the logs of all the concurrent executions are displayed.
	 * Also, on Cloud Foundry, the task execution log is retrieved only for the latest execution that matches the
	 * given deployment ID (external execution ID).
	 *
	 * @param platformName the platform
	 * @param taskId       the deploymentID (externalExecutionId) associated with the task execution.
	 * @return the log of the specified task.
	 */
	@Override
	public String getLog(String platformName, String taskId, String schemaTarget) {
		String result;
		try {
			result = findTaskLauncher(platformName).getLog(taskId);
		} catch (Exception iae) {
			logger.warn("Failed to retrieve the log, returning verification message. ", iae);
			result = "Log could not be retrieved.  Verify that deployments are still available.";
		}
		return result;
	}

	@Override
	public void stopTaskExecution(Set<Long> ids, String schemaTarget) {
		stopTaskExecution(ids, schemaTarget, null);
	}

	@Override
	public void stopTaskExecution(Set<Long> ids, String schemaTarget, String platform) {
		logger.info("Stopping {} task executions.", ids.size());

		Set<AggregateTaskExecution> taskExecutions = getValidStopExecutions(ids, schemaTarget);
		Set<AggregateTaskExecution> childTaskExecutions = getValidStopChildExecutions(ids, schemaTarget);
		for (AggregateTaskExecution taskExecution : taskExecutions) {
			cancelTaskExecution(taskExecution, platform);
		}
		childTaskExecutions.forEach(childTaskExecution -> cancelTaskExecution(childTaskExecution, platform));
		updateAuditInfoForTaskStops(taskExecutions.size() + childTaskExecutions.size());
	}

	@Override
	public TaskManifest findTaskManifestById(Long id, String schemaTarget) {
		DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao = dataflowTaskExecutionMetadataDaoContainer.get(schemaTarget);
		Assert.notNull(dataflowTaskExecutionMetadataDao, "Expected dataflowTaskExecutionMetadataDao using " + schemaTarget);
		return dataflowTaskExecutionMetadataDao.findManifestById(id);
	}

	@Override
	public Map<Long, TaskManifest> findTaskManifestByIds(Set<Long> ids, String schemaTarget) {
		DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao = dataflowTaskExecutionMetadataDaoContainer.get(schemaTarget);
		Assert.notNull(dataflowTaskExecutionMetadataDao, "Expected dataflowTaskExecutionMetadataDao using " + schemaTarget);
		return dataflowTaskExecutionMetadataDao.findManifestByIds(ids);
	}

	public void setAutoCreateTaskDefinitions(boolean autoCreateTaskDefinitions) {
		this.autoCreateTaskDefinitions = autoCreateTaskDefinitions;
	}

	private Set<AggregateTaskExecution> getValidStopExecutions(Set<Long> ids, String schemaTarget) {
		Set<AggregateTaskExecution> taskExecutions = getTaskExecutions(ids, schemaTarget);
		validateExternalExecutionIds(taskExecutions);
		return taskExecutions;
	}

	private Set<AggregateTaskExecution> getValidStopChildExecutions(Set<Long> ids, String schemaTarget) {
		DataflowTaskExecutionDao dataflowTaskExecutionDao = this.dataflowTaskExecutionDaoContainer.get(schemaTarget);
		Set<Long> childTaskExecutionIds = dataflowTaskExecutionDao.findChildTaskExecutionIds(ids);
		Set<AggregateTaskExecution> childTaskExecutions = getTaskExecutions(childTaskExecutionIds, schemaTarget);
		validateExternalExecutionIds(childTaskExecutions);
		return childTaskExecutions;
	}

	private void updateAuditInfoForTaskStops(long numberOfExecutionsStopped) {
		final Map<String, Object> auditData = Collections.singletonMap("Stopped Task Executions", numberOfExecutionsStopped);
		this.auditRecordService.populateAndSaveAuditRecordUsingMapData(
			AuditOperationType.TASK, AuditActionType.UNDEPLOY,
			numberOfExecutionsStopped + " Task Execution Stopped", auditData, null);
	}

	private void validateExternalExecutionIds(Set<AggregateTaskExecution> taskExecutions) {
		Set<Long> invalidIds = new HashSet<>();
		for (AggregateTaskExecution taskExecution : taskExecutions) {
			if (taskExecution.getExternalExecutionId() == null) {
				invalidIds.add(taskExecution.getExecutionId());
			}
		}
		if (!invalidIds.isEmpty()) {
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

	protected void updateExternalExecutionId(long executionId, String taskLaunchId, String schemaTarget) {
		TaskRepository taskRepository = this.taskRepositoryContainer.get(schemaTarget);
		taskRepository.updateExternalExecutionId(executionId, taskLaunchId);
	}

	private Map<String, Object> getAudited(
		TaskDefinition taskDefinition, Map<String, String> taskDeploymentProperties,
		List<String> commandLineArgs
	) {
		final Map<String, Object> auditedData = new HashMap<>(3);
		auditedData.put(TASK_DEFINITION_DSL_TEXT, this.argumentSanitizer.sanitizeTaskDsl(taskDefinition));
		auditedData.put(TASK_DEPLOYMENT_PROPERTIES,
			this.argumentSanitizer.sanitizeProperties(taskDeploymentProperties));
		auditedData.put(COMMAND_LINE_ARGS, this.argumentSanitizer.sanitizeArguments(commandLineArgs));
		return auditedData;
	}

	private void cancelTaskExecution(AggregateTaskExecution taskExecution, String platformName) {
		String platformNameToUse;
		if (StringUtils.hasText(platformName)) {
			platformNameToUse = platformName;
		} else {
			AggregateTaskExecution platformTaskExecution = taskExecution;
			TaskDeployment taskDeployment = this.taskDeploymentRepository.findByTaskDeploymentId(platformTaskExecution.getExternalExecutionId());
			// If TaskExecution does not have an associated platform see if parent task has the platform information.
			if (taskDeployment == null) {
				if (platformTaskExecution.getParentExecutionId() != null) {
					platformTaskExecution = this.taskExplorer.getTaskExecution(platformTaskExecution.getParentExecutionId(),
						platformTaskExecution.getSchemaTarget());
					taskDeployment = this.taskDeploymentRepository.findByTaskDeploymentId(platformTaskExecution.getExternalExecutionId());
				}
				if (taskDeployment == null) {
					throw new TaskExecutionException(String.format("No platform could be found for task execution id %s", taskExecution.getExecutionId()));
				}
			}
			platformNameToUse = taskDeployment.getPlatformName();
		}
		TaskLauncher taskLauncher = findTaskLauncher(platformNameToUse);
		taskLauncher.cancel(taskExecution.getExternalExecutionId());
		logger.info("Task execution stop request for id {} for platform {} has been submitted", taskExecution.getExecutionId(), platformNameToUse);

	}

	private Set<AggregateTaskExecution> getTaskExecutions(Set<Long> ids, String schemaTarget) {
		Set<AggregateTaskExecution> taskExecutions = new HashSet<>();
		final SortedSet<Long> nonExistingTaskExecutions = new TreeSet<>();
		for (Long id : ids) {
			final AggregateTaskExecution taskExecution = this.taskExplorer.getTaskExecution(id, schemaTarget);
			if (taskExecution == null) {
				nonExistingTaskExecutions.add(id);
			} else {
				taskExecutions.add(taskExecution);
			}
		}
		if (!nonExistingTaskExecutions.isEmpty()) {
			if (nonExistingTaskExecutions.size() == 1) {
				throw new NoSuchTaskExecutionException(nonExistingTaskExecutions.first(), schemaTarget);
			} else {
				throw new NoSuchTaskExecutionException(nonExistingTaskExecutions, schemaTarget);
			}
		}
		return taskExecutions;
	}

	private void isCTRSplitValidForCurrentCTR(TaskLauncher taskLauncher, TaskDefinition taskDefinition) {
		TaskParser taskParser = new TaskParser("composed-task-runner", taskDefinition.getProperties().get("graph"), true, true);
		TaskNode taskNode = taskParser.parse();
		ComposedTaskRunnerVisitor composedRunnerVisitor = new ComposedTaskRunnerVisitor();
		taskNode.accept(composedRunnerVisitor);
		if (composedRunnerVisitor.getHighCount() > taskLauncher.getMaximumConcurrentTasks()) {
			throw new IllegalArgumentException(String.format("One or more of the " +
					"splits in the composed task contains a task count that exceeds " +
					"the maximumConcurrentTasks count of %s",
				taskLauncher.getMaximumConcurrentTasks()));
		}
	}

	@Override
	public Set<Long> getAllTaskExecutionIds(boolean onlyCompleted, String taskName) {
		SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget(taskName, taskDefinitionReader);
		DataflowTaskExecutionDao dataflowTaskExecutionDao = dataflowTaskExecutionDaoContainer.get(schemaVersionTarget.getName());
		return dataflowTaskExecutionDao.getAllTaskExecutionIds(onlyCompleted, taskName);
	}

	@Override
	public Integer getAllTaskExecutionsCount(boolean onlyCompleted, String taskName) {
		return getAllTaskExecutionsCount(onlyCompleted, taskName, null);
	}

	@Override
	public Integer getAllTaskExecutionsCount(boolean onlyCompleted, String taskName, Integer days) {
		if (days != null) {
			Date dateBeforeDays = TaskServicesDateUtils.numDaysAgoFromLocalMidnightToday(days);
			return (int) dataflowTaskExecutionQueryDao.getCompletedTaskExecutionCountByTaskNameAndBeforeDate(taskName, dateBeforeDays);
		} else {
			return (int) (onlyCompleted ? dataflowTaskExecutionQueryDao.getCompletedTaskExecutionCountByTaskName(taskName)
				: dataflowTaskExecutionQueryDao.getTaskExecutionCountByTaskName(taskName));
		}
	}
}
