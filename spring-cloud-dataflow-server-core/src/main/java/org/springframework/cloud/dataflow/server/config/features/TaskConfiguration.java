/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config.features;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.AggregateTaskConfiguration;
import org.springframework.cloud.dataflow.aggregate.task.AggregateTaskExplorer;
import org.springframework.cloud.dataflow.aggregate.task.DataflowTaskExecutionQueryDao;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.aggregate.task.TaskDeploymentReader;
import org.springframework.cloud.dataflow.aggregate.task.TaskRepositoryContainer;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.schema.service.SchemaServiceConfiguration;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.dataflow.server.config.AggregateDataFlowTaskConfiguration;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.AggregateJobQueryDao;
import org.springframework.cloud.dataflow.server.repository.DataflowJobExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDaoContainer;
import org.springframework.cloud.dataflow.server.repository.DefaultTaskDefinitionReader;
import org.springframework.cloud.dataflow.server.repository.DefaultTaskDeploymentReader;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.DeployerConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.server.service.JobServiceContainer;
import org.springframework.cloud.dataflow.server.service.LauncherInitializationService;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionCreationService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.service.TaskSaveService;
import org.springframework.cloud.dataflow.server.service.impl.ComposedTaskRunnerConfigurationProperties;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskDeleteService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionRepositoryService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskJobService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskSaveService;
import org.springframework.cloud.dataflow.server.service.impl.TaskAppDeploymentRequestCreator;
import org.springframework.cloud.dataflow.server.service.impl.TaskConfigurationProperties;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.PropertyResolver;
import org.springframework.data.map.repository.config.EnableMapRepositories;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Thomas Risberg
 * @author Janne Valkealahti
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * @author Christian Tzolov
 * @author David Turanski
 * @author Corneil du Plessis
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnTasksEnabled
@EnableConfigurationProperties({
		TaskConfigurationProperties.class,
		CommonApplicationProperties.class,
		DockerValidatorProperties.class,
		LocalPlatformProperties.class,
		ComposedTaskRunnerConfigurationProperties.class
})
@EnableMapRepositories(basePackages = "org.springframework.cloud.dataflow.server.job")
@EnableTransactionManagement
@Import({
		TaskConfiguration.TaskDeleteServiceConfig.class,
		SchemaServiceConfiguration.class,
		AggregateTaskConfiguration.class,
		AggregateDataFlowTaskConfiguration.class
})
public class TaskConfiguration {

	@Autowired
	DataSourceProperties dataSourceProperties;

	@Value("${spring.cloud.dataflow.server.uri:}")
	private String dataflowServerUri;

	@Autowired
	private TaskConfigurationProperties taskConfigurationProperties;

	@Autowired
	private ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties;

	@Bean
	@ConditionalOnMissingBean
	public TaskDefinitionReader taskDefinitionReader(TaskDefinitionRepository taskDefinitionRepository) {
		return new DefaultTaskDefinitionReader(taskDefinitionRepository);
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskDeploymentReader taskDeploymentReader(TaskDeploymentRepository repository) {
		return new DefaultTaskDeploymentReader(repository);
	}

	@Bean
	public DeployerConfigurationMetadataResolver deployerConfigurationMetadataResolver(
			TaskConfigurationProperties taskConfigurationProperties
	) {
		return new DeployerConfigurationMetadataResolver(taskConfigurationProperties.getDeployerProperties());
	}

	@Bean
	public LauncherInitializationService launcherInitializationService(
			LauncherRepository launcherRepository,
			List<TaskPlatform> platforms,
			DeployerConfigurationMetadataResolver resolver
	) {
		return new LauncherInitializationService(launcherRepository, platforms, resolver);
	}

	/**
	 * The default profile is active when no other profiles are active. This is configured so
	 * that several tests will pass without having to explicitly enable the local profile.
	 *
	 * @param localPlatformProperties the local platform properties
	 * @param localScheduler          the local scheduler
	 * @return the task platform
	 */
	@Profile({"local", "default"})
	@Bean
	public TaskPlatform localTaskPlatform(
			LocalPlatformProperties localPlatformProperties,
			@Nullable Scheduler localScheduler
	) {
		TaskPlatform taskPlatform = new LocalTaskPlatformFactory(localPlatformProperties, localScheduler)
				.createTaskPlatform();
		taskPlatform.setPrimary(true);
		return taskPlatform;
	}

	@Bean
	public TaskExecutionInfoService taskDefinitionRetriever(
			AppRegistryService registry,
			AggregateTaskExplorer taskExplorer,
			TaskDefinitionRepository taskDefinitionRepository,
			TaskConfigurationProperties taskConfigurationProperties,
			LauncherRepository launcherRepository,
			List<TaskPlatform> taskPlatforms,
			ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties
	) {
		return new DefaultTaskExecutionInfoService(dataSourceProperties, registry,
				taskExplorer, taskDefinitionRepository, taskConfigurationProperties, launcherRepository, taskPlatforms,
				composedTaskRunnerConfigurationProperties);
	}

	@Bean
	public TaskSaveService saveTaskService(
			TaskDefinitionRepository taskDefinitionRepository,
			AuditRecordService auditRecordService, AppRegistryService registry
	) {
		return new DefaultTaskSaveService(taskDefinitionRepository, auditRecordService, registry);
	}

	@Bean
	public TaskExecutionCreationService taskExecutionRepositoryService(
			TaskRepositoryContainer taskRepositoryContainer,
			AggregateExecutionSupport aggregateExecutionSupport,
			TaskDefinitionReader taskDefinitionReader
	) {
		return new DefaultTaskExecutionRepositoryService(taskRepositoryContainer, aggregateExecutionSupport, taskDefinitionReader);
	}

	@Bean
	public TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator(
			CommonApplicationProperties commonApplicationProperties,
			ApplicationConfigurationMetadataResolver metadataResolver
	) {
		return new TaskAppDeploymentRequestCreator(commonApplicationProperties,
				metadataResolver, dataflowServerUri);
	}

	@Configuration
	public static class TaskExecutionServiceConfig {
		@Bean
		public TaskExecutionService taskService(
				PropertyResolver propertyResolver,
				TaskConfigurationProperties taskConfigurationProperties,
				ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties,
				LauncherRepository launcherRepository,
				AuditRecordService auditRecordService,
				TaskRepositoryContainer taskRepositoryContainer,
				TaskExecutionInfoService taskExecutionInfoService,
				TaskDeploymentRepository taskDeploymentRepository,
				TaskExecutionCreationService taskExecutionRepositoryService,
				TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator,
				AggregateTaskExplorer taskExplorer,
				DataflowTaskExecutionDaoContainer dataflowTaskExecutionDaoContainer,
				DataflowTaskExecutionMetadataDaoContainer dataflowTaskExecutionMetadataDaoContainer,
				DataflowTaskExecutionQueryDao dataflowTaskExecutionQueryDao,
				@Nullable OAuth2TokenUtilsService oauth2TokenUtilsService,
				TaskSaveService taskSaveService,
				AggregateExecutionSupport aggregateExecutionSupport,
				TaskDefinitionRepository taskDefinitionRepository,
				TaskDefinitionReader taskDefinitionReader
		) {
			DefaultTaskExecutionService defaultTaskExecutionService = new DefaultTaskExecutionService(
					propertyResolver,
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
					composedTaskRunnerConfigurationProperties);
			defaultTaskExecutionService.setAutoCreateTaskDefinitions(taskConfigurationProperties.isAutoCreateTaskDefinitions());
			return defaultTaskExecutionService;
		}
	}

	@Configuration(proxyBeanMethods = false)
	public static class TaskJobServiceConfig {
		@Bean
		public TaskJobService taskJobExecutionRepository(
				JobServiceContainer serviceContainer,
				AggregateTaskExplorer taskExplorer,
				TaskDefinitionRepository taskDefinitionRepository,
				TaskExecutionService taskExecutionService,
				LauncherRepository launcherRepository,
				AggregateExecutionSupport aggregateExecutionSupport,
				AggregateJobQueryDao aggregateJobQueryDao,
				TaskDefinitionReader taskDefinitionReader
		) {
			return new DefaultTaskJobService(
					serviceContainer,
					taskExplorer,
					taskDefinitionRepository,
					taskExecutionService,
					launcherRepository,
					aggregateExecutionSupport,
					aggregateJobQueryDao,
					taskDefinitionReader
			);
		}
	}

	@Configuration(proxyBeanMethods = false)
	public static class TaskDeleteServiceConfig {
		@Bean
		public TaskDeleteService deleteTaskService(
				AggregateTaskExplorer taskExplorer,
				LauncherRepository launcherRepository,
				TaskDefinitionRepository taskDefinitionRepository,
				TaskDeploymentRepository taskDeploymentRepository,
				AuditRecordService auditRecordService,
				DataflowTaskExecutionDaoContainer dataflowTaskExecutionDaoContainer,
				DataflowJobExecutionDaoContainer dataflowJobExecutionDaoContainer,
				DataflowTaskExecutionMetadataDaoContainer dataflowTaskExecutionMetadataDaoContainer,
				TaskConfigurationProperties taskConfigurationProperties,
				DataSource dataSource,
				SchemaService schemaService,
				@Autowired(required = false) SchedulerService schedulerService
		) {
			return new DefaultTaskDeleteService(
					taskExplorer,
					launcherRepository,
					taskDefinitionRepository,
					taskDeploymentRepository,
					auditRecordService,
					dataflowTaskExecutionDaoContainer,
					dataflowJobExecutionDaoContainer,
					dataflowTaskExecutionMetadataDaoContainer,
					schedulerService,
					schemaService,
					taskConfigurationProperties,
					dataSource
			);
		}
	}
}
