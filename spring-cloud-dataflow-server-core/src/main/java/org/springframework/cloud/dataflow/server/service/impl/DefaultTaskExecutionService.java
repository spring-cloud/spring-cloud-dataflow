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
import org.springframework.cloud.dataflow.server.service.AuditRecordService;
import org.springframework.cloud.dataflow.server.service.TaskDefinitionRetriever;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
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

	private final CommonApplicationProperties commonApplicationProperties;

	protected final AuditRecordService auditRecordService;

	private final TaskDefinitionRetriever taskDefinitionRetriever;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	public static final String TASK_DEFINITION_DSL_TEXT = "taskDefinitionDslText";

	public static final String TASK_DEPLOYMENT_PROPERTIES = "taskDeploymentProperties";

	public static final String COMMAND_LINE_ARGS = "commandLineArgs";

	/**
	 * Initializes the {@link DefaultTaskExecutionService}.
	 *
	 * @param launcherRepository the repository of task launcher used to launch task apps.
	 * @param metaDataResolver the metadata resolver
	 * @param auditRecordService the audit record service
	 * @param dataflowServerUri the data flow server URI
	 * @param commonApplicationProperties the common application properties
	 */
	public DefaultTaskExecutionService(LauncherRepository launcherRepository,
			ApplicationConfigurationMetadataResolver metaDataResolver,
			AuditRecordService auditRecordService,
			String dataflowServerUri, CommonApplicationProperties commonApplicationProperties,
			TaskDefinitionRetriever taskDefinitionRetriever) {
		Assert.notNull(launcherRepository, "LauncherRepository must not be null");
		Assert.notNull(metaDataResolver, "metaDataResolver must not be null");
		Assert.notNull(commonApplicationProperties, "commonApplicationProperties must not be null");
		Assert.notNull(auditRecordService, "auditRecordService must not be null");
		Assert.notNull(taskDefinitionRetriever, "TaskDefinitionRetriever must not be null");

		this.launcherRepository = launcherRepository;
		this.whitelistProperties = new WhitelistProperties(metaDataResolver);
		this.dataflowServerUri = dataflowServerUri;
		this.commonApplicationProperties = commonApplicationProperties;
		this.auditRecordService = auditRecordService;
		this.taskDefinitionRetriever = taskDefinitionRetriever;
	}

	@Override
	public long executeTask(String taskName, Map<String, String> taskDeploymentProperties,
			List<String> commandLineArgs, String platformName) {

		TaskExecutionInformation taskExecutionInformation = taskDefinitionRetriever
				.retrieveTaskDefinitionInformation(taskName, taskDeploymentProperties);
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
		String id = taskLauncher.launch(request);
		if (!StringUtils.hasText(id)) {
			throw new IllegalStateException("Deployment ID is null for the task:" + taskName);
		}
		taskDefinitionRetriever.updateExternalExecutionId(taskExecution.getExecutionId(), id);

		this.auditRecordService.populateAndSaveAuditRecordUsingMapData(
				AuditOperationType.TASK, AuditActionType.DEPLOY,
				taskDefinition.getName(),
				getAudited(taskDefinition, taskExecutionInformation.getTaskDeploymentProperties(), updatedCmdLineArgs));

		return taskExecution.getExecutionId();
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
