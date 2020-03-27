/*
 * Copyright 2018-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.audit.service.AuditServiceUtils;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.core.TaskPlatformFactory;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.controller.WhitelistProperties;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.SchedulerServiceProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link SchedulerService} interface. Provide service methods
 * for Scheduling tasks.
 *
 * @author Glenn Renfro
 * @author Chris Schaefer
 */
public class DefaultSchedulerService implements SchedulerService {

	private final static int MAX_SCHEDULE_NAME_LEN = 52;

	private CommonApplicationProperties commonApplicationProperties;
	private TaskPlatform taskPlatform;
	private TaskDefinitionRepository taskDefinitionRepository;
	private AppRegistryService registry;
	private final TaskConfigurationProperties taskConfigurationProperties;
	private final String dataflowServerUri;
	private final WhitelistProperties whitelistProperties;
	private final SchedulerServiceProperties schedulerServiceProperties;
	private final AuditRecordService auditRecordService;
	private final AuditServiceUtils auditServiceUtils;
	private final DataSourceProperties dataSourceProperties;

	/**
	 * Constructor for DefaultSchedulerService
	 * @param commonApplicationProperties common properties for applications deployed via Spring Cloud Data Flow.
	 * @param taskPlatform the {@link TaskPlatform} for this service.
	 * @param taskDefinitionRepository the {@link TaskDefinitionRepository} for this service.
	 * @param registry the {@link AppRegistryService} for this service.
	 * @param resourceLoader the {@link ResourceLoader} for this service.
	 * @param taskConfigurationProperties the {@link taskConfigurationProperties} for this service.
	 * @param dataSourceProperties the {@link DataSourceProperties} for this service.
	 * @param dataflowServerUri the Spring Cloud Data Flow uri for this service.
	 * @param metaDataResolver the {@link ApplicationConfigurationMetadataResolver} for this service.
	 * @param schedulerServiceProperties the {@link SchedulerServiceProperties} for this service.
	 * @param auditRecordService the {@link AuditRecordService} for this service.
	 */
	public DefaultSchedulerService(CommonApplicationProperties commonApplicationProperties,
			TaskPlatform taskPlatform, TaskDefinitionRepository taskDefinitionRepository,
			AppRegistryService registry, ResourceLoader resourceLoader,
			TaskConfigurationProperties taskConfigurationProperties,
			DataSourceProperties dataSourceProperties, String dataflowServerUri,
			ApplicationConfigurationMetadataResolver metaDataResolver,
			SchedulerServiceProperties schedulerServiceProperties,
			AuditRecordService auditRecordService) {

		Assert.notNull(commonApplicationProperties, "commonApplicationProperties must not be null");
		Assert.notNull(taskPlatform, "taskPlatform must not be null");
		Assert.notNull(registry, "AppRegistryService must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Assert.notNull(taskDefinitionRepository, "TaskDefinitionRepository must not be null");
		Assert.notNull(taskConfigurationProperties, "taskConfigurationProperties must not be null");
		Assert.notNull(metaDataResolver, "metaDataResolver must not be null");
		Assert.notNull(schedulerServiceProperties, "schedulerServiceProperties must not be null");
		Assert.notNull(auditRecordService, "AuditRecordService must not be null");
		Assert.notNull(dataSourceProperties, "dataSourceProperties must not be null");
		this.commonApplicationProperties = commonApplicationProperties;
		this.taskPlatform = taskPlatform;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.registry = registry;
		this.taskConfigurationProperties = taskConfigurationProperties;
		this.dataflowServerUri = dataflowServerUri;
		this.whitelistProperties = new WhitelistProperties(metaDataResolver);
		this.schedulerServiceProperties = schedulerServiceProperties;
		this.auditRecordService = auditRecordService;
		this.auditServiceUtils = new AuditServiceUtils();
		this.dataSourceProperties = dataSourceProperties;
	}

	@Override
	public void schedule(String scheduleName, String taskDefinitionName, Map<String, String> taskDeploymentProperties,
			List<String> commandLineArgs) {
		Assert.hasText(taskDefinitionName, "The provided taskName must not be null or empty.");
		Assert.notNull(taskDeploymentProperties, "The provided taskDeploymentProperties must not be null.");
		TaskDefinition taskDefinition = this.taskDefinitionRepository.findById(taskDefinitionName)
				.orElseThrow(() -> new NoSuchTaskDefinitionException(taskDefinitionName));
		TaskParser taskParser = new TaskParser(taskDefinition.getName(), taskDefinition.getDslText(), true, true);
		TaskNode taskNode = taskParser.parse();
		// if composed task definition replace definition with one composed task
		// runner and executable graph.
		if (taskNode.isComposed()) {
			taskDefinition = new TaskDefinition(taskDefinition.getName(),
					TaskServiceUtils.createComposedTaskDefinition(
							taskNode.toExecutableDSL(), this.taskConfigurationProperties));
			taskDeploymentProperties = TaskServiceUtils.establishComposedTaskProperties(taskDeploymentProperties, taskNode);
		}

		AppRegistration appRegistration = this.registry.find(taskDefinition.getRegisteredAppName(),
				ApplicationType.task);
		Assert.notNull(appRegistration, "Unknown task app: " + taskDefinition.getRegisteredAppName());
		Resource metadataResource = this.registry.getAppMetadataResource(appRegistration);

		taskDefinition = TaskServiceUtils.updateTaskProperties(taskDefinition, this.dataSourceProperties);

		Map<String, String> appDeploymentProperties = new HashMap<>(commonApplicationProperties.getTask());
		appDeploymentProperties.putAll(
				TaskServiceUtils.extractAppProperties(taskDefinition.getRegisteredAppName(), taskDeploymentProperties));

		Map<String, String> deployerDeploymentProperties = DeploymentPropertiesUtils
				.extractAndQualifyDeployerProperties(taskDeploymentProperties, taskDefinition.getRegisteredAppName());
		if (StringUtils.hasText(this.dataflowServerUri) && taskNode.isComposed()) {
			TaskServiceUtils.updateDataFlowUriIfNeeded(this.dataflowServerUri, appDeploymentProperties, commandLineArgs);
		}
		AppDefinition revisedDefinition = TaskServiceUtils.mergeAndExpandAppProperties(taskDefinition, metadataResource,
				appDeploymentProperties, whitelistProperties);
		DeploymentPropertiesUtils.validateDeploymentProperties(taskDeploymentProperties);
		taskDeploymentProperties = extractAndQualifySchedulerProperties(taskDeploymentProperties);

		Launcher launcher = getDefaultLauncher();
		scheduleName = validateScheduleNameForPlatform(launcher.getType(), scheduleName);

		ScheduleRequest scheduleRequest = new ScheduleRequest(revisedDefinition, taskDeploymentProperties,
				deployerDeploymentProperties, commandLineArgs, scheduleName, getTaskResource(taskDefinitionName));
		launcher.getScheduler().schedule(scheduleRequest);

		this.auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE,
				scheduleRequest.getScheduleName(), this.auditServiceUtils.convertScheduleRequestToAuditData(scheduleRequest));
	}

	private String validateScheduleNameForPlatform(String type, String scheduleName) {
		if(type.equals(TaskPlatformFactory.KUBERNETES_PLATFORM_TYPE)) {
			if(scheduleName.length() > MAX_SCHEDULE_NAME_LEN) {
				throw new IllegalArgumentException(String.format("the name specified " +
						"exceeds the maximum schedule name length of %s.", MAX_SCHEDULE_NAME_LEN));
			}
			scheduleName = scheduleName.toLowerCase();
		}
		return scheduleName;
	}
	private static Map<String, String> extractPropertiesByPrefix(Map<String, String> taskDeploymentProperties, String prefix) {
		return taskDeploymentProperties.entrySet().stream()
				.filter(kv -> kv.getKey().startsWith(prefix))

				.collect(Collectors.toMap(kv -> kv.getKey(), Map.Entry::getValue));
	}

	private Launcher getDefaultLauncher() {
		Launcher launcherToUse = null;
		for (Launcher launcher : this.taskPlatform.getLaunchers()) {
			if (launcher.getName().equalsIgnoreCase("default")) {
				launcherToUse = launcher;
				break;
			}
		}
		if (launcherToUse == null) {
			launcherToUse = this.taskPlatform.getLaunchers().get(0);
		}
		if (launcherToUse == null) {
			throw new IllegalStateException("Could not find a default launcher.");
		}
		Scheduler scheduler = launcherToUse.getScheduler();
		if (scheduler == null) {
			throw new IllegalStateException("Could not find a default scheduler.");
		}
		return launcherToUse;
	}

	@Override
	public void unschedule(String scheduleName) {
		final ScheduleInfo scheduleInfo = getSchedule(scheduleName);
		if (scheduleInfo != null) {
			Launcher launcher = getDefaultLauncher();
			launcher.getScheduler().unschedule(scheduleInfo.getScheduleName());
			this.auditRecordService.populateAndSaveAuditRecord(
					AuditOperationType.SCHEDULE,
					AuditActionType.DELETE, scheduleInfo.getScheduleName(),
					scheduleInfo.getTaskDefinitionName());
		}
	}

	@Override
	public void unscheduleForTaskDefinition(String taskDefinitionName) {
		for(ScheduleInfo scheduleInfo : list()) {
			if(scheduleInfo.getTaskDefinitionName().equals(taskDefinitionName)) {
				unschedule(scheduleInfo.getScheduleName());
			}
		}
	}

	@Override
	public List<ScheduleInfo> list(Pageable pageable, String taskDefinitionName) {
		throw new UnsupportedOperationException("method not supported");
	}

	@Override
	public Page<ScheduleInfo> list(Pageable pageable) {
		throw new UnsupportedOperationException("method not supported");
	}

	@Override
	public List<ScheduleInfo> list(String taskDefinitionName) {
		Launcher launcher = getDefaultLauncher();
		List<ScheduleInfo> list = launcher.getScheduler().list();
		List<ScheduleInfo> result = new ArrayList<>();
		for(ScheduleInfo scheduleInfo: list) {
			if(scheduleInfo.getTaskDefinitionName().equals(taskDefinitionName)) {
				result.add(scheduleInfo);
			}
		}
		return limitScheduleInfoResultSize(result,
				this.schedulerServiceProperties.getMaxSchedulesReturned());
	}

	@Override
	public List<ScheduleInfo> list() {
		Launcher launcher = getDefaultLauncher();
		return limitScheduleInfoResultSize(launcher.getScheduler().list(),
				this.schedulerServiceProperties.getMaxSchedulesReturned());
	}

	@Override
	public ScheduleInfo getSchedule(String scheduleName) {
		List<ScheduleInfo> result = list().stream()
				.filter(scheduleInfo -> scheduleInfo.getScheduleName().equals(scheduleName))
				.collect(Collectors.toList());
		Assert.isTrue(!(result.size() > 1), "more than one schedule was returned for scheduleName, should only be one");
		return result.size() > 0 ? result.get(0) : null;
	}

	private List<ScheduleInfo> limitScheduleInfoResultSize(List<ScheduleInfo> resultSet,
			int schedulerLimitResultSize) {
		if(resultSet.size() > schedulerLimitResultSize) {
			resultSet = resultSet.subList(0, schedulerLimitResultSize);
		}
		return resultSet;
	}

	/**
	 * Retain only properties that are meant for the <em>scheduler</em> of a given task(those
	 * that start with {@code scheduler.}and qualify all
	 * property values with the {@code spring.cloud.scheduler.} prefix.
	 *
	 * @param input the scheduler properties
	 * @return scheduler properties for the task
	 */
	private static Map<String, String> extractAndQualifySchedulerProperties(Map<String, String> input) {
		final String prefix = "scheduler.";
		final int prefixLength = prefix.length();

		Map<String, String> result = new TreeMap<>(input).entrySet().stream()
				.filter(kv -> kv.getKey().startsWith(prefix))
				.collect(Collectors.toMap(kv -> "spring.cloud.scheduler." + kv.getKey().substring(prefixLength), kv -> kv.getValue(),
						(fromWildcard, fromApp) -> fromApp));

		return result;
	}

	protected Resource getTaskResource(String taskDefinitionName) {
		TaskDefinition taskDefinition = this.taskDefinitionRepository.findById(taskDefinitionName)
				.orElseThrow(() -> new NoSuchTaskDefinitionException(taskDefinitionName));
		AppRegistration appRegistration = null;
		if (TaskServiceUtils.isComposedTaskDefinition(taskDefinition.getDslText())) {
			appRegistration = this.registry.find(taskConfigurationProperties.getComposedTaskRunnerName(),
					ApplicationType.task);
		}
		else {
			appRegistration = this.registry.find(taskDefinition.getRegisteredAppName(),
					ApplicationType.task);
		}
		Assert.notNull(appRegistration, "Unknown task app: " + taskDefinition.getRegisteredAppName());
		return this.registry.getAppResource(appRegistration);
	}
}
