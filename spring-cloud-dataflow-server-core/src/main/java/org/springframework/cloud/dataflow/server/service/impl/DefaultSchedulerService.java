/*
 * Copyright 2018-2019 the original author or authors.
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

import java.net.URI;
import java.net.URISyntaxException;
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

	private final static String APP_PREFIX = "app.";
	private final static String DEPLOYER_PREFIX = "deployer.";
	private final static String COMMAND_ARGUMENT_PREFIX = "cmdarg.";
	private final static String DATA_FLOW_URI_KEY = "spring.cloud.dataflow.client.serverUri";

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

	/**
	 * Constructor for DefaultSchedulerService
	 * @param commonApplicationProperties common properties for applications deployed via Spring Cloud Data Flow.
	 * @param taskPlatform the {@link TaskPlatform} for this service.
	 * @param taskDefinitionRepository the {@link TaskDefinitionRepository} for this service.
	 * @param registry the {@link AppRegistryService} for this service.
	 * @param resourceLoader the {@link ResourceLoader} for this service.
	 * @param taskConfigurationProperties the {@link TaskConfigurationProperties} for this service.
	 * @param dataSourceProperties the {@link DataSourceProperties} for this service.
	 * @param dataflowServerUri the Spring Cloud Data Flow uri for this service.
	 * @param metaDataResolver the {@link ApplicationConfigurationMetadataResolver} for this service.
	 * @param schedulerServiceProperties the {@link SchedulerServiceProperties} for this service.
	 * @param auditRecordService the {@link AuditRecordService} for this service.
	 *
	 * @deprecated dataSourceProperties is no longer used.  Use constructor that does not have that parameter.
	 */
	@Deprecated
	public DefaultSchedulerService(CommonApplicationProperties commonApplicationProperties,
			TaskPlatform taskPlatform, TaskDefinitionRepository taskDefinitionRepository,
			AppRegistryService registry, ResourceLoader resourceLoader,
			TaskConfigurationProperties taskConfigurationProperties,
			DataSourceProperties dataSourceProperties, String dataflowServerUri,
			ApplicationConfigurationMetadataResolver metaDataResolver,
			SchedulerServiceProperties schedulerServiceProperties,
			AuditRecordService auditRecordService) {
		this(commonApplicationProperties, taskPlatform, taskDefinitionRepository,
				registry, resourceLoader, taskConfigurationProperties,
				dataflowServerUri, metaDataResolver,
				schedulerServiceProperties, auditRecordService);
	}

	/**
	 * Constructor for DefaultSchedulerService
	 * @param commonApplicationProperties common properties for applications deployed via Spring Cloud Data Flow.
	 * @param taskPlatform the {@link TaskPlatform} for this service.
	 * @param taskDefinitionRepository the {@link TaskDefinitionRepository} for this service.
	 * @param registry the {@link AppRegistryService} for this service.
	 * @param resourceLoader the {@link ResourceLoader} for this service.
	 * @param taskConfigurationProperties the {@link TaskConfigurationProperties} for this service.
	 * @param dataflowServerUri the Spring Cloud Data Flow uri for this service.
	 * @param metaDataResolver the {@link ApplicationConfigurationMetadataResolver} for this service.
	 * @param schedulerServiceProperties the {@link SchedulerServiceProperties} for this service.
	 * @param auditRecordService the {@link AuditRecordService} for this service
	 */
	public DefaultSchedulerService(CommonApplicationProperties commonApplicationProperties,
			TaskPlatform taskPlatform, TaskDefinitionRepository taskDefinitionRepository,
			AppRegistryService registry, ResourceLoader resourceLoader,
			TaskConfigurationProperties taskConfigurationProperties, String dataflowServerUri,
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
	}
	@Override
	public void schedule(String scheduleName, String taskDefinitionName, Map<String, String> taskDeploymentProperties,
			List<String> commandLineArgs) {
		Assert.hasText(taskDefinitionName, "The provided taskName must not be null or empty.");
		Assert.notNull(taskDeploymentProperties, "The provided taskDeploymentProperties must not be null.");
		scheduleName =  scheduleName + "-" + getSchedulePrefix(taskDefinitionName);
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
		}

		AppRegistration appRegistration = this.registry.find(taskDefinition.getRegisteredAppName(),
				ApplicationType.task);
		Assert.notNull(appRegistration, "Unknown task app: " + taskDefinition.getRegisteredAppName());
		Resource metadataResource = this.registry.getAppMetadataResource(appRegistration);

		Map<String, String> appProperties = new HashMap<>(commonApplicationProperties.getTask());
		appProperties.putAll(
				extractPropertiesByPrefix(taskDeploymentProperties, APP_PREFIX));

		Map<String, String> deployerProperties = new HashMap<>(commonApplicationProperties.getTask());
		deployerProperties.putAll(
				extractPropertiesByPrefix(taskDeploymentProperties, DEPLOYER_PREFIX));

		Map<String, String> deployerDeploymentProperties = DeploymentPropertiesUtils
				.extractAndQualifyDeployerProperties(taskDeploymentProperties, taskDefinition.getRegisteredAppName());
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_URI_KEY, this.dataflowServerUri, appProperties, commandLineArgs);

		appProperties = tagProperties(null, appProperties, APP_PREFIX);
		deployerProperties = tagProperties(null, deployerProperties, DEPLOYER_PREFIX);
		appProperties.putAll(deployerProperties);
		AppDefinition revisedDefinition =
				TaskServiceUtils.mergeAndExpandAppProperties(taskDefinition, metadataResource,
						appProperties, whitelistProperties);
		revisedDefinition = new AppDefinition(scheduleName,
				cleanseTaskProperties(revisedDefinition.getProperties()));

		DeploymentPropertiesUtils.validateDeploymentProperties(taskDeploymentProperties);
		taskDeploymentProperties = extractAndQualifySchedulerProperties(taskDeploymentProperties);
		List<String> revisedCommandLineArgs = tagCommandLineArgs(new ArrayList<>(commandLineArgs));
		revisedCommandLineArgs.add("--spring.cloud.scheduler.task.launcher.taskName=" + taskDefinitionName);
		ScheduleRequest scheduleRequest = new ScheduleRequest(revisedDefinition, taskDeploymentProperties,
				deployerDeploymentProperties, revisedCommandLineArgs, scheduleName, getTaskLauncherResource());
		Launcher launcher = getDefaultLauncher();
		launcher.getScheduler().schedule(scheduleRequest);
		this.auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE,
				scheduleRequest.getScheduleName(), this.auditServiceUtils.convertScheduleRequestToAuditData(scheduleRequest),
				taskPlatform.getName());
	}
	private static Map<String, String> extractPropertiesByPrefix(Map<String, String> taskDeploymentProperties, String prefix) {
		return taskDeploymentProperties.entrySet().stream()
				.filter(kv -> kv.getKey().startsWith(prefix))
				.collect(Collectors.toMap(kv -> kv.getKey().substring(prefix.length()), Map.Entry::getValue));
	}

	private List<String> tagCommandLineArgs(List<String> args) {
		List<String> taggedArgs = new ArrayList<>();

		for(String arg : args) {
			if(arg.contains("spring.cloud.task.name")) {
				continue;
			}
			String updatedArg = arg;
			if (!arg.startsWith(DATA_FLOW_URI_KEY) && !"--".concat(arg).startsWith(DATA_FLOW_URI_KEY)) {
					updatedArg = COMMAND_ARGUMENT_PREFIX +
							taskConfigurationProperties.getTaskLauncherPrefix() + "." + arg;
			}
			taggedArgs.add(updatedArg);
		}
		return taggedArgs;
	}

	private Map<String, String> tagProperties(String appName, Map<String, String> appProperties, String prefix) {
		Map<String, String> taggedAppProperties = new HashMap<>(appProperties.size());

		for(String key : appProperties.keySet()) {
			if(key.contains("spring.cloud.task.name")) {
				continue;
			}
			String updatedKey = key;
			if (!key.startsWith(DATA_FLOW_URI_KEY)) {
				if (StringUtils.hasText(appName)) {
					updatedKey = taskConfigurationProperties.getTaskLauncherPrefix() + "." +
							prefix + appName + "." + key;
				}
				else {
					updatedKey = taskConfigurationProperties.getTaskLauncherPrefix() + "." +
							prefix + key;
				}
			}
			taggedAppProperties.put(updatedKey, appProperties.get(key));
		}
		return taggedAppProperties;
	}

	private Map<String, String> cleanseTaskProperties(Map<String, String> taskProperties) {
		Map<String, String> cleansedProperties = new HashMap<>(taskProperties);
		cleansedProperties.remove(TaskDefinition.SPRING_CLOUD_TASK_NAME);
		cleansedProperties.remove("graph");
		return cleansedProperties;
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
			launcher.getTaskLauncher().destroy(scheduleName);
			this.auditRecordService.populateAndSaveAuditRecord(
					AuditOperationType.SCHEDULE,
					AuditActionType.DELETE, scheduleInfo.getScheduleName(),
					scheduleInfo.getTaskDefinitionName(),
					taskPlatform.getName());
		}
	}

	@Override
	public void unscheduleForTaskDefinition(String taskDefinitionName) {
		String schedulePrefix = getSchedulePrefix(taskDefinitionName);
		for(ScheduleInfo scheduleInfo : list()) {
			if(scheduleInfo.getScheduleName().endsWith(schedulePrefix)) {
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
		List<ScheduleInfo> list = updateTaskDefinitionNames(launcher.getScheduler().list());
		List<ScheduleInfo> result = new ArrayList<>();
		for(ScheduleInfo scheduleInfo: list) {
			if(scheduleInfo.getScheduleName().endsWith(getSchedulePrefix(taskDefinitionName))) {
				result.add(scheduleInfo);
			}
		}
		return limitScheduleInfoResultSize(result,
				this.schedulerServiceProperties.getMaxSchedulesReturned());
	}

	private String getSchedulePrefix(String taskDefinitionName) {
		return taskConfigurationProperties.getScheduleNamePrefix() + taskDefinitionName;
	}

	private List<ScheduleInfo> updateTaskDefinitionNames(List<ScheduleInfo> scheduleInfos) {
		int schedulerTagNameLength = taskConfigurationProperties.getScheduleNamePrefix().length();
		for(ScheduleInfo scheduleInfo : scheduleInfos) {
			int taskDefinitionNameOffset = scheduleInfo.getScheduleName().indexOf(
					taskConfigurationProperties.getScheduleNamePrefix());
			if(taskDefinitionNameOffset > -1) {
				String taskDefinitionName = scheduleInfo.getScheduleName().substring(
						taskDefinitionNameOffset + schedulerTagNameLength);
					scheduleInfo.setTaskDefinitionName(taskDefinitionName);
				}
			}
		return scheduleInfos;
	}

	@Override
	public List<ScheduleInfo> list() {
		Launcher launcher = getDefaultLauncher();
		return updateTaskDefinitionNames(limitScheduleInfoResultSize(launcher.getScheduler().list(),
				this.schedulerServiceProperties.getMaxSchedulesReturned()));
	}

	@Override
	public ScheduleInfo getSchedule(String scheduleName) {
		List<ScheduleInfo> result = updateTaskDefinitionNames(list().stream()
				.filter(scheduleInfo -> scheduleInfo.getScheduleName().equals(scheduleName))
				.collect(Collectors.toList()));
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

		return new TreeMap<>(input).entrySet().stream()
				.filter(kv -> kv.getKey().startsWith(prefix))
				.collect(Collectors.toMap(kv -> "spring.cloud.scheduler." + kv.getKey().substring(prefixLength), Map.Entry::getValue,
						(fromWildcard, fromApp) -> fromApp));
	}

	protected Resource getTaskLauncherResource() {
		final URI url;
		try {
			url = new URI(this.taskConfigurationProperties.getSchedulerTaskLauncherUrl());
		} catch (URISyntaxException urise) {
			throw new IllegalStateException(urise);
		}

		AppRegistration appRegistration = new AppRegistration(this.taskConfigurationProperties.getSchedulerTaskLauncherName(), ApplicationType.app, url);
		return this.registry.getAppResource(appRegistration);
	}

}
