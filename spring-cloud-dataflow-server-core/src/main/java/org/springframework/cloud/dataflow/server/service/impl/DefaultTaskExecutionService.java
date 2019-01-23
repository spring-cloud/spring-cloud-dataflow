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

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.rest.support.ArgumentSanitizer;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.controller.WhitelistProperties;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
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
public class DefaultTaskExecutionService implements TaskExecutionService {

	private final Logger logger = LoggerFactory.getLogger(DefaultTaskExecutionService.class);

	/**
	 * Used to launch apps as tasks.
	 */
	private final LauncherRepository launcherRepository;

	private final WhitelistProperties whitelistProperties;

	private final String dataflowServerUri;

	private final CommonApplicationProperties commonApplicationProperties;

	protected final AuditRecordService auditRecordService;

	/**
	 * Used to create TaskExecutions.
	 */
	private final TaskRepository taskRepository;

	private final TaskExecutionInfoService taskExecutionInfoService;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	private DataSource dataSource;

	private PlatformTransactionManager transactionManager;


	private static final String TASK_DEFINITION_DSL_TEXT = "taskDefinitionDslText";

	private static final String TASK_DEPLOYMENT_PROPERTIES = "taskDeploymentProperties";

	private static final String COMMAND_LINE_ARGS = "commandLineArgs";


	/**
	 * Initializes the {@link DefaultTaskExecutionService}.
	 *
	 * @param launcherRepository the repository of task launcher used to launch task apps.
	 * @param metaDataResolver the metadata resolver
	 * @param auditRecordService the audit record service
	 *
	 */
	public DefaultTaskExecutionService(LauncherRepository launcherRepository,
			ApplicationConfigurationMetadataResolver metaDataResolver,
			AuditRecordService auditRecordService,
			String dataflowServerUri, CommonApplicationProperties commonApplicationProperties,
			TaskRepository taskRepository,
			TaskExecutionInfoService taskExecutionInfoService,
			DataSource dataSource, PlatformTransactionManager transactionManager) {
		Assert.notNull(launcherRepository, "LauncherRepository must not be null");
		Assert.notNull(metaDataResolver, "metaDataResolver must not be null");
		Assert.notNull(auditRecordService, "auditRecordService must not be null");
		Assert.notNull(commonApplicationProperties, "commonApplicationProperties must not be null");
		Assert.notNull(taskExecutionInfoService, "TaskDefinitionRetriever must not be null");
		Assert.notNull(taskRepository, "TaskRepository must not be null");
		Assert.notNull(taskExecutionInfoService, "TaskExecutionInfoService must not be null");
		Assert.notNull(dataSource, "dataSource must not be null");
		Assert.notNull(transactionManager, "transactionManager must not be null");


		this.launcherRepository = launcherRepository;
		this.whitelistProperties = new WhitelistProperties(metaDataResolver);
		this.auditRecordService = auditRecordService;
		this.dataflowServerUri = dataflowServerUri;
		this.commonApplicationProperties = commonApplicationProperties;
		this.taskRepository = taskRepository;
		this.taskExecutionInfoService = taskExecutionInfoService;
		this.transactionManager = transactionManager;
		this.dataSource = dataSource;
	}

	@Override
	public long executeTask(String taskName, Map<String, String> taskDeploymentProperties,
			List<String> commandLineArgs, String platformName) {
		RequestCriteria requestCriteria = createLaunchRequest(taskName, taskDeploymentProperties, commandLineArgs);
		Launcher launcher = this.launcherRepository.findByName(platformName);
		if (launcher == null) {
			throw new IllegalStateException(String.format("No Launcher found for the platform named '%s'",
					platformName));
		}
		TaskLauncher taskLauncher = launcher.getTaskLauncher();
		if (taskLauncher == null) {
			throw new IllegalStateException(String.format("No TaskLauncher found for the platform named '%s'",
					platformName));
		}
		String id;

		try {
			id = taskLauncher.launch(requestCriteria.getAppDeploymentRequest());
		} catch (Throwable throwable) {
			recordFailedTaskLaunch(requestCriteria.taskExecution.getExecutionId(), "An error occurred while attempting to launch task: " + taskName);
			throw throwable;
		}

		if (!StringUtils.hasText(id)) {
			recordFailedTaskLaunch(requestCriteria.taskExecution.getExecutionId(), "Deployment ID is null for the task:" + taskName);
			throw new IllegalStateException("Deployment ID is null for the task:" + taskName);
		}

		try {
			this.updateExternalExecutionId(requestCriteria.getTaskExecution().getExecutionId(), id);
		} catch (Throwable throwable) {
			logger.warn("Unable to update taskExecution's externalExecutionId", throwable);
		}

		postLaunchProcessing(requestCriteria);

		return requestCriteria.getTaskExecution().getExecutionId();
	}

	private RequestCriteria createLaunchRequest (final String taskName, final Map<String, String> taskDeploymentProperties,
			final List<String> commandLineArgs) {
		TransactionTemplate template = new TransactionTemplate(transactionManager);

		return template.execute(status -> {
			if (taskExecutionInfoService.maxConcurrentExecutionsReached()) {
				throw new IllegalStateException(String.format(
						"The maximum concurrent task executions [%d] is at its limit.",
						taskExecutionInfoService.getMaximumConcurrentTasks()));
			}

			TaskExecutionInformation taskExecutionInformation = taskExecutionInfoService
					.findTaskExecutionInformation(taskName, taskDeploymentProperties);
			TaskDefinition taskDefinition = taskExecutionInformation.getTaskDefinition();
			String registeredAppName = taskDefinition.getRegisteredAppName();
			TaskExecution taskExecution = taskExecutionInformation.getTaskExecution();

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

			return new RequestCriteria(request, taskDefinition, taskExecution);
		});
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

	private void postLaunchProcessing(RequestCriteria requestCriteria) {
		TransactionTemplate template = new TransactionTemplate(transactionManager);

		template.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				auditRecordService.populateAndSaveAuditRecordUsingMapData(
						AuditOperationType.TASK, AuditActionType.DEPLOY,
						requestCriteria.getTaskDefinition().getName(),
						getAudited(requestCriteria.getTaskDefinition(),
								requestCriteria.getAppDeploymentRequest().getDeploymentProperties(),
								requestCriteria.getAppDeploymentRequest().getCommandlineArguments()));
			}
		});
	}

	protected void recordFailedTaskLaunch(long executionId, String message) {
		//TODO: Remove this code when https://github.com/spring-cloud/spring-cloud-task/issues/507
		//is merged and TaskRepository.updateErrorMessage is available.
		TransactionTemplate template = new TransactionTemplate(transactionManager);

		template.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

				JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
				int[] types = {Types.VARCHAR, Types.BIGINT};
				Object[] params = {message, executionId};
				jdbcTemplate.update("UPDATE TASK_EXECUTION SET ERROR_MESSAGE = ? WHERE TASK_EXECUTION_ID = ?", params, types);
			}
		});
	}

	private static class RequestCriteria {

		private AppDeploymentRequest appDeploymentRequest;

		private TaskDefinition taskDefinition;

		private TaskExecution taskExecution;

		public RequestCriteria(AppDeploymentRequest appDeploymentRequest,
				TaskDefinition taskDefinition, TaskExecution taskExecution) {
			this.appDeploymentRequest = appDeploymentRequest;
			this.taskDefinition = taskDefinition;
			this.taskExecution = taskExecution;
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

	}
}
