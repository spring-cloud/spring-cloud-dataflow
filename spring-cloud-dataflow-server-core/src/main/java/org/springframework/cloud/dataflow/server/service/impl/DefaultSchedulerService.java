/*
 * Copyright 2018-2024 the original author or authors.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import org.springframework.cloud.dataflow.server.controller.VisibleProperties;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.SchedulerServiceProperties;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.cloud.task.listener.TaskException;
import org.springframework.core.env.PropertyResolver;
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
 * @author Ilayaperumal Gopinathan
 */
public class DefaultSchedulerService implements SchedulerService {

	private static final Logger logger = LoggerFactory.getLogger(DefaultSchedulerService.class);

	private final static int MAX_SCHEDULE_NAME_LEN = 52;

	private CommonApplicationProperties commonApplicationProperties;

	private List<TaskPlatform> taskPlatforms;

	private TaskDefinitionRepository taskDefinitionRepository;

	private AppRegistryService registry;

	private final TaskConfigurationProperties taskConfigurationProperties;

	private final String dataflowServerUri;

	private final VisibleProperties visibleProperties;

	private final SchedulerServiceProperties schedulerServiceProperties;

	private final AuditRecordService auditRecordService;

	private final AuditServiceUtils auditServiceUtils;

	private final DataSourceProperties dataSourceProperties;

	private final ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties;

	private final TaskExecutionInfoService taskExecutionInfoService;

	private final PropertyResolver propertyResolver;

	private static final Pattern TASK_NAME_PATTERN = Pattern.compile("[a-zA-Z]([-a-zA-Z0-9]*[a-zA-Z0-9])?");

	private static final String TASK_NAME_VALIDATION_MSG = "Task name must consist of alphanumeric characters " +
			"or '-', start with an alphabetic character, and end with an alphanumeric character (e.g. 'my-name', " +
			"or 'abc-123')";

	/**
	 * Constructor for DefaultSchedulerService
	 *
	 * @param commonApplicationProperties               common properties for applications deployed via Spring Cloud Data Flow.
	 * @param taskPlatforms                             the {@link TaskPlatform}s for this service.
	 * @param taskDefinitionRepository                  the {@link TaskDefinitionRepository} for this service.
	 * @param registry                                  the {@link AppRegistryService} for this service.
	 * @param resourceLoader                            the {@link ResourceLoader} for this service.
	 * @param taskConfigurationProperties               the {@link TaskConfigurationProperties} for this service.
	 * @param dataSourceProperties                      the {@link DataSourceProperties} for this service.
	 * @param dataflowServerUri                         the Spring Cloud Data Flow uri for this service.
	 * @param metaDataResolver                          the {@link ApplicationConfigurationMetadataResolver} for this service.
	 * @param schedulerServiceProperties                the {@link SchedulerServiceProperties} for this service.
	 * @param auditRecordService                        the {@link AuditRecordService} for this service.
	 * @param taskExecutionInfoService                  the {@link TaskExecutionInfoService} for this service
	 * @param propertyResolver                          the {@link PropertyResolver} for this service
	 * @param composedTaskRunnerConfigurationProperties the {@link ComposedTaskRunnerConfigurationProperties} for this service
	 */
	public DefaultSchedulerService(
			CommonApplicationProperties commonApplicationProperties,
			List<TaskPlatform> taskPlatforms,
			TaskDefinitionRepository taskDefinitionRepository,
			AppRegistryService registry,
			ResourceLoader resourceLoader,
			TaskConfigurationProperties taskConfigurationProperties,
			DataSourceProperties dataSourceProperties,
			String dataflowServerUri,
			ApplicationConfigurationMetadataResolver metaDataResolver,
			SchedulerServiceProperties schedulerServiceProperties,
			AuditRecordService auditRecordService,
			TaskExecutionInfoService taskExecutionInfoService,
			PropertyResolver propertyResolver,
			ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties

	) {
		Assert.notNull(commonApplicationProperties, "commonApplicationProperties must not be null");
		Assert.notNull(taskPlatforms, "taskPlatforms must not be null");
		Assert.notNull(registry, "AppRegistryService must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Assert.notNull(taskDefinitionRepository, "TaskDefinitionRepository must not be null");
		Assert.notNull(taskConfigurationProperties, "taskConfigurationProperties must not be null");
		Assert.notNull(metaDataResolver, "metaDataResolver must not be null");
		Assert.notNull(schedulerServiceProperties, "schedulerServiceProperties must not be null");
		Assert.notNull(auditRecordService, "AuditRecordService must not be null");
		Assert.notNull(dataSourceProperties, "dataSourceProperties must not be null");
		Assert.notNull(taskExecutionInfoService, "taskExecutionInfoService must not be null");
		Assert.notNull(propertyResolver, "propertyResolver must not be null");
		this.commonApplicationProperties = commonApplicationProperties;
		this.taskPlatforms = taskPlatforms;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.registry = registry;
		this.taskConfigurationProperties = taskConfigurationProperties;
		this.dataflowServerUri = dataflowServerUri;
		this.visibleProperties = new VisibleProperties(metaDataResolver);
		this.schedulerServiceProperties = schedulerServiceProperties;
		this.auditRecordService = auditRecordService;
		this.auditServiceUtils = new AuditServiceUtils();
		this.dataSourceProperties = dataSourceProperties;
		this.taskExecutionInfoService = taskExecutionInfoService;
		this.propertyResolver = propertyResolver;
		this.composedTaskRunnerConfigurationProperties = composedTaskRunnerConfigurationProperties;
	}

	@Override
	public void schedule(
			String scheduleName, String taskDefinitionName, Map<String, String> taskDeploymentProperties,
			List<String> commandLineArgs
	) {
		schedule(scheduleName, taskDefinitionName, taskDeploymentProperties, commandLineArgs, null);
	}

	@SuppressWarnings("DuplicatedCode")
	@Override
	public void schedule(
			String scheduleName, String taskDefinitionName, Map<String, String> taskDeploymentProperties,
			List<String> commandLineArgs, String platformName
	) {
		String platformType = StreamSupport.stream(getLaunchers().spliterator(), true)
				.filter(deployer -> deployer.getName().equalsIgnoreCase(platformName))
				.map(Launcher::getType)
				.findFirst()
				.orElse("unknown");
		if (platformType.equals(TaskPlatformFactory.KUBERNETES_PLATFORM_TYPE) && !TASK_NAME_PATTERN.matcher(taskDefinitionName).matches()) {
			throw new TaskException(String.format("Task name %s is invalid. %s", taskDefinitionName, TASK_NAME_VALIDATION_MSG));
		}
		Assert.hasText(taskDefinitionName, "The provided taskName must not be null or empty.");
		Assert.notNull(taskDeploymentProperties, "The provided taskDeploymentProperties must not be null.");
		TaskDefinition taskDefinition = this.taskDefinitionRepository.findById(taskDefinitionName).orElse(null);
		if (taskDefinition == null) {
			throw new NoSuchTaskDefinitionException(taskDefinitionName);
		}

		String taskAppName = taskDefinition.getRegisteredAppName();
		String taskLabel = taskDefinition.getAppDefinition().getName();
		String version = taskDeploymentProperties.get("version." + taskLabel);
		TaskParser taskParser = new TaskParser(taskDefinition.getName(), taskDefinition.getDslText(), true, true);
		TaskNode taskNode = taskParser.parse();
		AppRegistration appRegistration;
		// if composed task definition replace definition with one composed task
		// runner and executable graph.
		if (taskNode.isComposed()) {
			taskDefinition = new TaskDefinition(taskDefinition.getName(), TaskServiceUtils.createComposedTaskDefinition(taskNode.toExecutableDSL()));
			Map<String, String> establishedComposedTaskProperties = TaskServiceUtils.establishComposedTaskProperties(taskDeploymentProperties, taskNode);
			taskDeploymentProperties.putAll(establishedComposedTaskProperties);
			TaskServiceUtils.addImagePullSecretProperty(taskDeploymentProperties, this.composedTaskRunnerConfigurationProperties);
			try {
				appRegistration = new AppRegistration(
						ComposedTaskRunnerConfigurationProperties.COMPOSED_TASK_RUNNER_NAME,
						ApplicationType.task,
						new URI(this.composedTaskRunnerConfigurationProperties.getUri()));
			} catch (URISyntaxException e) {
				throw new IllegalStateException("Invalid Compose Task Runner Resource", e);
			}
			Set<String> appNames = taskExecutionInfoService.composedTaskChildNames(taskDefinition.getName());

			logger.info("composedTask:dsl={}:appNames:{}", taskDefinition.getDslText(), appNames);
			addPrefixProperties("app.composed-task-runner.", taskDeploymentProperties);
			addPrefixProperties("app." + scheduleName + ".", taskDeploymentProperties);
			for (String appName : appNames) {
				List<String> names = new ArrayList<>(Arrays.asList(StringUtils.delimitedListToStringArray(appName, ",")));
				String registeredName = names.get(0);
				String appId = registeredName;
				if (names.size() > 1) {
					appId = names.get(1);
				}
				String appVersion = taskDeploymentProperties.get("version." + taskAppName + "-" + appId + "." + appId);
				if(!StringUtils.hasText(appVersion)) {
					appVersion = taskDeploymentProperties.get("version." + taskAppName + "-" + appId);
				}
				if(!StringUtils.hasText(appVersion)) {
					appVersion = taskDeploymentProperties.get("version." + appId);
				}
				logger.debug("ctr:{}:registeredName={}, version={}", names, registeredName, appVersion);
			}
			logger.debug("ctr:added:{}:{}", scheduleName, taskDeploymentProperties);
			commandLineArgs = TaskServiceUtils.convertCommandLineArgsToCTRFormat(commandLineArgs);
		} else {
			appRegistration = this.registry.find(taskDefinition.getRegisteredAppName(),
					ApplicationType.task);
			addPrefixCommandLineArgs("app." + taskDefinition.getRegisteredAppName() + ".", commandLineArgs);
			addPrefixProperties("app." + taskDefinition.getRegisteredAppName() + ".", taskDeploymentProperties);
		}
		Assert.notNull(appRegistration, "Unknown task app: " + taskDefinition.getRegisteredAppName());
		Resource metadataResource = this.registry.getAppMetadataResource(appRegistration);
		Launcher launcher = getTaskLauncher(platformName);
		taskDefinition = TaskServiceUtils.updateTaskProperties(taskDefinition, this.dataSourceProperties,
				TaskServiceUtils.addDatabaseCredentials(this.taskConfigurationProperties.isUseKubernetesSecretsForDbCredentials(), launcher.getType()));

		Map<String, String> appDeploymentProperties = new HashMap<>(commonApplicationProperties.getTask());
		appDeploymentProperties.putAll(TaskServiceUtils.extractAppProperties(taskDefinition.getRegisteredAppName(), taskDeploymentProperties));

		// Merge the common properties defined via the spring.cloud.dataflow.common-properties.task-resource file.
		// Doesn't override existing properties!
		// The placeholders defined in the task-resource file are not resolved by SCDF but passed to the apps as they are.
		TaskServiceUtils.contributeCommonProperties(this.commonApplicationProperties.getTaskResourceProperties(),
				appDeploymentProperties, "common");
		TaskServiceUtils.contributeCommonProperties(this.commonApplicationProperties.getTaskResourceProperties(),
				appDeploymentProperties, launcher.getType().toLowerCase(Locale.ROOT));

		Map<String, String> deployerDeploymentProperties = DeploymentPropertiesUtils
				.extractAndQualifyDeployerProperties(taskDeploymentProperties, taskDefinition.getRegisteredAppName());
		if (StringUtils.hasText(this.dataflowServerUri) && taskNode.isComposed()) {
			TaskServiceUtils.updateDataFlowUriIfNeeded(this.dataflowServerUri, appDeploymentProperties, commandLineArgs);
		}
		AppDefinition revisedDefinition = TaskServiceUtils.mergeAndExpandAppProperties(taskDefinition, metadataResource,
				appDeploymentProperties, visibleProperties);
		DeploymentPropertiesUtils.validateDeploymentProperties(taskDeploymentProperties);
		taskDeploymentProperties = extractAndQualifySchedulerProperties(taskDeploymentProperties);
		deployerDeploymentProperties.putAll(taskDeploymentProperties);
		scheduleName = validateScheduleNameForPlatform(launcher.getType(), scheduleName);
		ScheduleRequest scheduleRequest = new ScheduleRequest(revisedDefinition,
				deployerDeploymentProperties,
				commandLineArgs,
				scheduleName,
				getTaskResource(taskDefinitionName, version));

		launcher.getScheduler().schedule(scheduleRequest);

		this.auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE,
				scheduleRequest.getScheduleName(), this.auditServiceUtils.convertScheduleRequestToAuditData(scheduleRequest),
				launcher.getName());
	}


	private static void addProperty(String property, String value, Map<String, String> properties) {
		if (properties.containsKey(property)) {
			logger.debug("exists:{}={}", property, properties.get(property));
		} else {
			logger.debug("adding:{}={}", property, value);
			properties.put(property, value);
		}
	}

	private static void addPrefixProperties(String prefix, Map<String, String> deploymentProperties) {
		addProperty(prefix + "spring.cloud.task.initialize-enabled", "false", deploymentProperties);
	}

	private static void addPrefixCommandLineArgs(String prefix, List<String> commandLineArgs) {
		addCommandLine(prefix + "spring.cloud.task.initialize-enabled", "false", commandLineArgs);
	}

	private static void addCommandLine(String property, String value, List<String> commandLineArgs) {
		String argPrefix = "--" + property + "=";
		if(commandLineArgs.stream().noneMatch(item -> item.startsWith(argPrefix))) {
			String arg = argPrefix + value;
			commandLineArgs.add(arg);
			logger.debug("adding:{}", arg);
		} else {
			logger.debug("exists:{}", argPrefix);
		}
	}

	private String validateScheduleNameForPlatform(String type, String scheduleName) {
		if (type.equals(TaskPlatformFactory.KUBERNETES_PLATFORM_TYPE)) {
			if (scheduleName.length() > MAX_SCHEDULE_NAME_LEN) {
				throw new IllegalArgumentException(String.format("the name specified " +
						"exceeds the maximum schedule name length of %s.", MAX_SCHEDULE_NAME_LEN));
			}
			scheduleName = scheduleName.toLowerCase(Locale.ROOT);
		}
		return scheduleName;
	}

	private Launcher getTaskLauncher(String platformName) {
		Launcher launcherToUse = null;
		List<Launcher> launchers = getLaunchers();
		for (Launcher launcher : launchers) {
			if (launcher.getName().equalsIgnoreCase(platformName)) {
				launcherToUse = launcher;
				break;
			}
		}
		if (launcherToUse == null && StringUtils.hasText(platformName)) {
			throw new IllegalArgumentException(String.format("The platform %s does not exist", platformName));
		}
		// Get Default Launcher if only one launcher has a scheduler.
		// if more than one launcher has a scheduler and a user didn't specify a platform then that is an error.
		int launcherCount = 0;
		if (launcherToUse == null) {
			if (getPrimaryLauncher() != null) {
				return getPrimaryLauncher();
			}
			for (Launcher launcher : launchers) {
				if (launcher.getScheduler() != null) {
					launcherToUse = launcher;
					launcherCount++;
				}
			}
		}
		if (launcherCount > 1) {
			throw new IllegalArgumentException("Must select a platform.  " +
					"A default could not be determined because more than one platform" +
					" had an associated scheduler");
		}
		if (platformName != null && launcherToUse == null) {
			throw new IllegalArgumentException(String.format("The platform %s does not support a scheduler service.", platformName));
		} else if (platformName == null && launcherToUse == null) {
			throw new IllegalStateException("Could not find a default scheduler.");
		}
		return launcherToUse;
	}

	private List<Launcher> getLaunchers() {
		List<Launcher> launchers = new ArrayList<>();
		for (TaskPlatform taskPlatform : this.taskPlatforms) {
			launchers.addAll(taskPlatform.getLaunchers());
		}
		return launchers;
	}

	private Launcher getPrimaryLauncher() {
		Launcher result = null;
		for (TaskPlatform taskPlatform : this.taskPlatforms) {
			if (taskPlatform.isPrimary()) {
				for (Launcher launcher : taskPlatform.getLaunchers()) {
					result = launcher;
				}
			}
		}
		return result;
	}

	@Override
	public void unschedule(String scheduleName, String platformName) {
		final ScheduleInfo scheduleInfo = getSchedule(scheduleName, platformName);
		if (scheduleInfo != null) {
			Launcher launcher = getTaskLauncher(platformName);
			launcher.getScheduler().unschedule(scheduleInfo.getScheduleName());
			this.auditRecordService.populateAndSaveAuditRecord(
					AuditOperationType.SCHEDULE,
					AuditActionType.DELETE, scheduleInfo.getScheduleName(),
					scheduleInfo.getTaskDefinitionName(),
					platformName);
		}
	}

	@Override
	public void unschedule(String scheduleName) {
		unschedule(scheduleName, null);
	}

	@Override
	public void unscheduleForTaskDefinition(String taskDefinitionName) {
		for (Launcher launcher : getLaunchers()) {
			for (ScheduleInfo scheduleInfo : listForPlatform(launcher.getName())) {
				if (scheduleInfo.getTaskDefinitionName().equals(taskDefinitionName)) {
					unschedule(scheduleInfo.getScheduleName(), launcher.getName());
				}
			}
		}
	}

	@Override
	public List<ScheduleInfo> list(Pageable pageable, String taskDefinitionName, String platformName) {
		throw new UnsupportedOperationException("method not supported");
	}

	@Override
	public Page<ScheduleInfo> list(Pageable pageable, String platformName) {
		throw new UnsupportedOperationException("method not supported");
	}

	@Override
	public Page<ScheduleInfo> list(Pageable pageable) {
		throw new UnsupportedOperationException("method not supported");
	}

	@Override
	public List<ScheduleInfo> list(String taskDefinitionName, String platformName) {
		Launcher launcher = getTaskLauncher(platformName);
		List<ScheduleInfo> list = launcher.getScheduler().list();
		List<ScheduleInfo> result = new ArrayList<>();
		for (ScheduleInfo scheduleInfo : list) {
			if (scheduleInfo.getTaskDefinitionName().equals(taskDefinitionName)) {
				result.add(scheduleInfo);
			}
		}
		return limitScheduleInfoResultSize(result,
				this.schedulerServiceProperties.getMaxSchedulesReturned());
	}

	@Override
	public List<ScheduleInfo> list(String taskDefinitionName) {
		return list(taskDefinitionName, null);
	}

	@Override
	public List<ScheduleInfo> listForPlatform(String platformName) {
		Launcher launcher = getTaskLauncher(platformName);
		return limitScheduleInfoResultSize(launcher.getScheduler().list(),
				this.schedulerServiceProperties.getMaxSchedulesReturned());
	}

	@Override
	public List<ScheduleInfo> list() {
		Launcher launcher = getTaskLauncher(null);
		return limitScheduleInfoResultSize(launcher.getScheduler().list(),
				this.schedulerServiceProperties.getMaxSchedulesReturned());
	}

	@Override
	public ScheduleInfo getSchedule(String scheduleName, String platformName) {
		List<ScheduleInfo> result = listForPlatform(platformName).stream()
				.filter(scheduleInfo -> scheduleInfo.getScheduleName().equals(scheduleName))
				.collect(Collectors.toList());
		Assert.isTrue(!(result.size() > 1), "more than one schedule was returned for scheduleName, should only be one");
		return !result.isEmpty() ? result.get(0) : null;
	}

	@Override
	public ScheduleInfo getSchedule(String scheduleName) {
		return getSchedule(scheduleName, null);
	}

	private List<ScheduleInfo> limitScheduleInfoResultSize(
			List<ScheduleInfo> resultSet,
			int schedulerLimitResultSize
	) {
		if (resultSet.size() > schedulerLimitResultSize) {
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
	@Deprecated
	private static Map<String, String> extractAndQualifySchedulerProperties(Map<String, String> input) {
		final String prefix = "scheduler.";
		final int prefixLength = prefix.length();

		return new TreeMap<>(input).entrySet().stream()
				.filter(kv -> kv.getKey().startsWith(prefix))
				.collect(Collectors.toMap(kv -> "spring.cloud.deployer." + kv.getKey().substring(prefixLength), Map.Entry::getValue,
						(fromWildcard, fromApp) -> fromApp));
	}

	protected Resource getTaskResource(String taskDefinitionName, String version) {
		TaskDefinition taskDefinition = this.taskDefinitionRepository.findById(taskDefinitionName)
				.orElseThrow(() -> new NoSuchTaskDefinitionException(taskDefinitionName));
		AppRegistration appRegistration = null;
		if (TaskServiceUtils.isComposedTaskDefinition(taskDefinition.getDslText())) {
			URI composedTaskUri = null;
			String composedTaskLauncherUri = this.composedTaskRunnerConfigurationProperties.getUri();
			try {
				composedTaskUri = new URI(composedTaskLauncherUri);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Invalid Composed Task Url: " + composedTaskLauncherUri);
			}
			appRegistration = new AppRegistration(ComposedTaskRunnerConfigurationProperties.COMPOSED_TASK_RUNNER_NAME, ApplicationType.task, composedTaskUri);
		} else {
			if(version != null) {
				appRegistration = this.registry.find(taskDefinition.getRegisteredAppName(),
					ApplicationType.task, version);
			}
			else {
				appRegistration = this.registry.find(taskDefinition.getRegisteredAppName(),
					ApplicationType.task);
			}
		}
		Assert.notNull(appRegistration, "Unknown task app: " + taskDefinition.getRegisteredAppName());
		return this.registry.getAppResource(appRegistration);
	}

}
