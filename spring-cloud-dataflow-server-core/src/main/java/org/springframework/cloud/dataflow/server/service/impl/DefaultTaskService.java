/*
 * Copyright 2015-2019 the original author or authors.
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.DefinitionUtils;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.cloud.dataflow.rest.support.ArgumentSanitizer;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.controller.WhitelistProperties;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.DuplicateTaskException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.TaskValidationService;
import org.springframework.cloud.dataflow.server.service.ValidationStatus;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
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
 * @author Daniel Serleg
 */
public class DefaultTaskService implements TaskService {

	private PlatformTransactionManager transactionManager;

	private static final Logger logger = LoggerFactory.getLogger(DefaultTaskService.class);

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
	private final LauncherRepository launcherRepository;

	/**
	 * The {@link AppRegistryService} this service will use to look up task app URIs.
	 */
	private final AppRegistryService registry;

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final WhitelistProperties whitelistProperties;

	private final TaskConfigurationProperties taskConfigurationProperties;

	private final String dataflowServerUri;

	private final CommonApplicationProperties commonApplicationProperties;

	private final TaskValidationService taskValidationService;

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
	 * @param launcherRepository the repository of task launcher used to launch task apps.
	 * @param metaDataResolver the metadata resolver
	 * @param taskConfigurationProperties the properties used to define the behavior of tasks
	 * @param auditRecordService the audit record service
	 * @param dataflowServerUri the data flow server URI
	 * @param commonApplicationProperties the common application properties
	 * @param taskValidationService the task validation service
	 * @param transactionManager the {@link PlatformTransactionManager} to be used by the DefaultTaskManager
	 */
	public DefaultTaskService(DataSourceProperties dataSourceProperties,
							TaskDefinitionRepository taskDefinitionRepository, TaskExplorer taskExplorer,
							TaskRepository taskExecutionRepository, AppRegistryService registry,
							LauncherRepository launcherRepository, ApplicationConfigurationMetadataResolver metaDataResolver,
							TaskConfigurationProperties taskConfigurationProperties,
							AuditRecordService auditRecordService,
							String dataflowServerUri, CommonApplicationProperties commonApplicationProperties,
							TaskValidationService taskValidationService,
							PlatformTransactionManager transactionManager) {
		Assert.notNull(dataSourceProperties, "DataSourceProperties must not be null");
		Assert.notNull(taskDefinitionRepository, "TaskDefinitionRepository must not be null");
		Assert.notNull(taskExecutionRepository, "TaskExecutionRepository must not be null");
		Assert.notNull(taskExplorer, "TaskExplorer must not be null");
		Assert.notNull(registry, "AppRegistryService must not be null");
		Assert.notNull(launcherRepository, "LauncherRepository must not be null");
		Assert.notNull(metaDataResolver, "metaDataResolver must not be null");
		Assert.notNull(taskConfigurationProperties, "taskConfigurationProperties must not be null");
		Assert.notNull(commonApplicationProperties, "commonApplicationProperties must not be null");
		Assert.notNull(auditRecordService, "auditRecordService must not be null");
		Assert.notNull(taskValidationService, "TaskValidationService must not be null");
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		this.dataSourceProperties = dataSourceProperties;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskExecutionRepository = taskExecutionRepository;
		this.taskExplorer = taskExplorer;
		this.registry = registry;
		this.launcherRepository = launcherRepository;
		this.whitelistProperties = new WhitelistProperties(metaDataResolver);
		this.taskConfigurationProperties = taskConfigurationProperties;
		this.dataflowServerUri = dataflowServerUri;
		this.commonApplicationProperties = commonApplicationProperties;
		this.auditRecordService = auditRecordService;
		this.taskValidationService = taskValidationService;
		this.transactionManager = transactionManager;
	}

	@Override
	public long executeTask(String taskName, Map<String, String> taskDeploymentProperties,
			List<String> commandLineArgs, String platformName) {
		RequestCriteria requestCriteria = createLaunchRequest(taskName, taskDeploymentProperties, commandLineArgs, platformName);
		TaskExecution taskExecution = requestCriteria.getTaskExecution();
		AppDeploymentRequest request = requestCriteria.getAppDeploymentRequest();
		TaskLauncher taskLauncher = requestCriteria.getLauncher().getTaskLauncher();
		if (taskLauncher == null) {
			throw new IllegalStateException(String.format("No TaskLauncher found for the platform named '%s'",
					platformName));
		}
		String id = taskLauncher.launch(request);
		if (!StringUtils.hasText(id)) {
			throw new IllegalStateException("Deployment ID is null for the task:" + taskName);
		}

		postLaunchProcessing(requestCriteria, taskDeploymentProperties, id);
		return taskExecution.getExecutionId();
	}

	private RequestCriteria createLaunchRequest (final String taskName, final Map<String, String> taskDeploymentProperties,
			final List<String> commandLineArgs, final String platformName) {
		TransactionTemplate template = new TransactionTemplate(transactionManager);

		return template.execute(status -> {
			Map<String, String> taskExecutionDeployementProperties = taskDeploymentProperties;
			Assert.hasText(taskName, "The provided taskName must not be null or empty.");
			Assert.notNull(taskExecutionDeployementProperties, "The provided taskDeploymentProperties must not be null.");
			if (maxConcurrentExecutionsReached()) {
				throw new IllegalStateException(String.format(
						"The maximum concurrent task executions [%d] is at its limit.",
						taskConfigurationProperties.getMaximumConcurrentTasks()));
			}

			TaskExecution taskExecution = taskExecutionRepository.createTaskExecution(taskName);
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
				taskExecutionDeployementProperties = TaskServiceUtils.establishComposedTaskProperties(taskExecutionDeployementProperties,
						taskNode);
			}

			AppRegistration appRegistration = registry.find(taskDefinition.getRegisteredAppName(),
					ApplicationType.task);
			Assert.notNull(appRegistration, "Unknown task app: " + taskDefinition.getRegisteredAppName());
			Resource appResource = registry.getAppResource(appRegistration);
			Resource metadataResource = registry.getAppMetadataResource(appRegistration);

			taskDefinition = TaskServiceUtils.updateTaskProperties(taskDefinition,
					dataSourceProperties);

			Map<String, String> appDeploymentProperties = new HashMap<>(commonApplicationProperties.getTask());
			appDeploymentProperties.putAll(
					TaskServiceUtils.extractAppProperties(taskDefinition.getRegisteredAppName(), taskExecutionDeployementProperties));

			Map<String, String> deployerDeploymentProperties = DeploymentPropertiesUtils
					.extractAndQualifyDeployerProperties(taskExecutionDeployementProperties, taskDefinition.getRegisteredAppName());
			if (StringUtils.hasText(dataflowServerUri) && taskNode.isComposed()) {
				TaskServiceUtils.updateDataFlowUriIfNeeded(dataflowServerUri, appDeploymentProperties,
						commandLineArgs);
			}
			AppDefinition revisedDefinition = TaskServiceUtils.mergeAndExpandAppProperties(taskDefinition, metadataResource,
					appDeploymentProperties, whitelistProperties);
			List<String> updatedCmdLineArgs = updateCommandLineArgs(commandLineArgs, taskExecution);

			Launcher launcher = this.launcherRepository.findByName(platformName);
			if (launcher == null) {
				throw new IllegalStateException(String.format("No Launcher found for the platform named '%s'",
						platformName));
			}

			RequestCriteria requestCriteria = new RequestCriteria(
					new AppDeploymentRequest(revisedDefinition, appResource,
					deployerDeploymentProperties, updatedCmdLineArgs),
					taskDefinition, taskExecution, taskExecutionDeployementProperties,
					launcher);
			return requestCriteria;
		});
	}

	private void postLaunchProcessing(RequestCriteria requestCriteria,
			final Map<String, String> taskDeploymentProperties,
			String externalExecutionId) {
		TransactionTemplate template = new TransactionTemplate(transactionManager);

		template.execute(new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				taskExecutionRepository.updateExternalExecutionId(requestCriteria.getTaskExecution().getExecutionId(), externalExecutionId);
				AppDeploymentRequest request = requestCriteria.getAppDeploymentRequest();
				auditRecordService.populateAndSaveAuditRecordUsingMapData(
						AuditOperationType.TASK, AuditActionType.DEPLOY,
						request.getDefinition().getName(), getAuditata(
								requestCriteria.getTaskDefinition(),
								requestCriteria.getLaunchTaskDeploymentProperties(),
								request.getCommandlineArguments()));
			}
		});
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
	@Transactional
	public boolean isComposedDefinition(String dsl) {
		return TaskServiceUtils.isComposedTaskDefinition(dsl);
	}

	@Override
	@Transactional
	public long getMaximumConcurrentTasks() {
		return taskConfigurationProperties.getMaximumConcurrentTasks();
	}

	@Override
	@Transactional
	public ValidationStatus validateTask(String name) {
		return this.taskValidationService.validateTask(name);
	}

	@Override
	@Transactional
	public void cleanupExecution(long id) {
		TaskExecution taskExecution = taskExplorer.getTaskExecution(id);
		Assert.notNull(taskExecution, "There was no task execution with id " + id);
		String launchId = taskExecution.getExternalExecutionId();
		Assert.hasLength(launchId, "The TaskExecution for id " + id + " did not have an externalExecutionId");
		// TODO GH-2674
		Launcher launcher = this.launcherRepository.findByName("default");
		if (launcher != null) {
			TaskLauncher taskLauncher = launcher.getTaskLauncher();
			taskLauncher.cleanup(launchId);
		} else {
			logger.info("Could clean up execution for task id " + id + ". Did not find a task launcher named 'default'");
		}
	}

	@Override
	@Transactional
	public void saveTaskDefinition(String name, String dsl) {
		TaskParser taskParser = new TaskParser(name, dsl, true, true);
		TaskNode taskNode = taskParser.parse();
		TaskDefinition taskDefinition = new TaskDefinition(name, dsl);
		if (this.taskDefinitionRepository.existsById(name)) {
			throw new DuplicateTaskException(String.format(
					"Cannot register task %s because another one has already " + "been registered with the same name",
					name));
		}
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
			throw new NoSuchAppRegistrationException(appName, ApplicationType.task);
		}
		if (this.taskDefinitionRepository.existsById(taskDefinition.getTaskName())) {
			throw new DuplicateTaskException(String.format(
					"Cannot register task %s because another one has already " + "been registered with the same name",
					taskDefinition.getTaskName()));
		}
		this.taskDefinitionRepository.save(taskDefinition);
	}


	@Override
	@Transactional
	public void deleteAll() {
		Iterable<TaskDefinition> allTaskDefinition = this.taskDefinitionRepository.findAll();

		for(TaskDefinition taskDefinition : allTaskDefinition) {
			deleteTaskDefinition(taskDefinition);

			auditRecordService.populateAndSaveAuditRecord(
					AuditOperationType.TASK, AuditActionType.DELETE,
					taskDefinition.getTaskName(), this.argumentSanitizer.sanitizeTaskDsl(taskDefinition));
		}
	}

	@Override
	@Transactional
	public void deleteTaskDefinition(String name) {
		TaskDefinition taskDefinition = this.taskDefinitionRepository.findById(name)
				.orElseThrow(() -> new NoSuchTaskDefinitionException(name));

		deleteTaskDefinition(taskDefinition);

		auditRecordService.populateAndSaveAuditRecord(
				AuditOperationType.TASK, AuditActionType.DELETE,
				taskDefinition.getTaskName(), this.argumentSanitizer.sanitizeTaskDsl(taskDefinition));
	}

	private void deleteTaskDefinition(TaskDefinition taskDefinition) {
		TaskParser taskParser = new TaskParser(taskDefinition.getName(), taskDefinition.getDslText(), true, true);
		TaskNode taskNode = taskParser.parse();
		// if composed-task-runner definition then destroy all child tasks associated with
		// it.
		if (taskNode.isComposed()) {
			String childTaskPrefix = TaskNode.getTaskPrefix(taskDefinition.getTaskName());
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
		destroyPrimaryTask(taskDefinition.getTaskName());
	}

	private void destroyPrimaryTask(String name) {
		TaskDefinition taskDefinition = this.taskDefinitionRepository.findById(name)
				.orElseThrow(() -> new NoSuchTaskDefinitionException(name));
		destroyTask(taskDefinition);
	}

	private void destroyChildTask(String name) {
		Optional<TaskDefinition> taskDefinition = this.taskDefinitionRepository.findById(name);
		if (taskDefinition.isPresent()) {
			destroyTask(taskDefinition.get());
		}
	}

	private void destroyTask(TaskDefinition taskDefinition) {
		// TODO GH-2678
		Launcher launcher = this.launcherRepository.findByName("default");
		if (launcher != null) {
			TaskLauncher taskLauncher = launcher.getTaskLauncher();
			taskLauncher.destroy(taskDefinition.getName());
			taskDefinitionRepository.deleteById(taskDefinition.getName());
		} else {
			logger.info("Could destory task definition " +
					taskDefinition.getTaskName() + ". Did not find a task launcher named 'default'");
		}
	}


	private static class RequestCriteria {

		private AppDeploymentRequest appDeploymentRequest;

		private TaskDefinition taskDefinition;

		private TaskExecution taskExecution;

		private Map<String, String> launchTaskDeploymentProperties;

		private Launcher launcher;


		public RequestCriteria(AppDeploymentRequest appDeploymentRequest,
				TaskDefinition taskDefinition, TaskExecution taskExecution,
				Map<String, String> launchTaskDeploymentProperties,
				Launcher launcher) {
			this.appDeploymentRequest = appDeploymentRequest;
			this.taskDefinition = taskDefinition;
			this.taskExecution = taskExecution;
			this.launchTaskDeploymentProperties = launchTaskDeploymentProperties;
			this.launcher = launcher;
		}

		public AppDeploymentRequest getAppDeploymentRequest() {
			return appDeploymentRequest;
		}

		public TaskDefinition getTaskDefinition() {
			return taskDefinition;
		}

		public TaskExecution getTaskExecution() {
			return taskExecution;
		}

		public Map<String, String> getLaunchTaskDeploymentProperties() {
			return launchTaskDeploymentProperties;
		}

		public Launcher getLauncher() {
			return launcher;
		}
	}

}
