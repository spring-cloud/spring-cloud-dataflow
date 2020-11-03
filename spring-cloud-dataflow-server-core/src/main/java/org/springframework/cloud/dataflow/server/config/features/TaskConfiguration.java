/*
 * Copyright 2016-2020 the original author or authors.
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

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.springframework.cloud.dataflow.server.batch.SimpleJobServiceFactoryBean;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.DataflowJobExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.DeployerConfigurationMetadataResolver;
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
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.map.repository.config.EnableMapRepositories;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
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
 */
@Configuration
@ConditionalOnTasksEnabled
@EnableConfigurationProperties({ TaskConfigurationProperties.class, CommonApplicationProperties.class,
		DockerValidatorProperties.class, LocalPlatformProperties.class, ComposedTaskRunnerConfigurationProperties.class
})
@EnableMapRepositories(basePackages = "org.springframework.cloud.dataflow.server.job")
@EnableTransactionManagement
public class TaskConfiguration {

	@Autowired
	DataSourceProperties dataSourceProperties;

	@Autowired(required = false)
	SchedulerService schedulerService;

	@Value("${spring.cloud.dataflow.server.uri:}")
	private String dataflowServerUri;

	@Autowired
	private TaskConfigurationProperties taskConfigurationProperties;

	@Autowired
	private ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties;

	@Bean
	public DeployerConfigurationMetadataResolver deployerConfigurationMetadataResolver(
			TaskConfigurationProperties taskConfigurationProperties) {
		return new DeployerConfigurationMetadataResolver(taskConfigurationProperties.getDeployerProperties());
	}

	@Bean
	public LauncherInitializationService launcherInitializationService(
			LauncherRepository launcherRepository,
			List<TaskPlatform> platforms,
			DeployerConfigurationMetadataResolver resolver) {
		return new LauncherInitializationService(launcherRepository, platforms, resolver);
	}

	/**
	 * The default profile is active when no other profiles are active. This is configured so
	 * that several tests will pass without having to explicitly enable the local profile.
	 * @param localPlatformProperties the local platform properties
	 * @param localScheduler the local scheduler
	 *
	 * @return the task platform
	 */
	@Profile({ "local", "default" })
	@Bean
	public TaskPlatform localTaskPlatform(LocalPlatformProperties localPlatformProperties,
			@Nullable Scheduler localScheduler) {
		TaskPlatform taskPlatform = new LocalTaskPlatformFactory(localPlatformProperties, localScheduler)
				.createTaskPlatform();
		taskPlatform.setPrimary(true);
		return taskPlatform;
	}

	@Bean
	public TaskExecutionInfoService taskDefinitionRetriever(AppRegistryService registry,
			TaskExplorer taskExplorer, TaskDefinitionRepository taskDefinitionRepository,
			TaskConfigurationProperties taskConfigurationProperties,
			LauncherRepository launcherRepository, List<TaskPlatform> taskPlatforms,
			ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties) {
		return new DefaultTaskExecutionInfoService(dataSourceProperties, registry,
				taskExplorer, taskDefinitionRepository, taskConfigurationProperties, launcherRepository, taskPlatforms,
				composedTaskRunnerConfigurationProperties);
	}

	@Bean
	public TaskDeleteService deleteTaskService(TaskExplorer taskExplorer, LauncherRepository launcherRepository,
			TaskDefinitionRepository taskDefinitionRepository, TaskDeploymentRepository taskDeploymentRepository,
			AuditRecordService auditRecordService,
			DataflowTaskExecutionDao dataflowTaskExecutionDao,
			DataflowJobExecutionDao dataflowJobExecutionDao,
			DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao) {
		return new DefaultTaskDeleteService(taskExplorer, launcherRepository, taskDefinitionRepository,
				taskDeploymentRepository,
				auditRecordService,
				dataflowTaskExecutionDao,
				dataflowJobExecutionDao,
				dataflowTaskExecutionMetadataDao,
				this.schedulerService);
	}

	@Bean
	public TaskSaveService saveTaskService(TaskDefinitionRepository taskDefinitionRepository,
			AuditRecordService auditRecordService, AppRegistryService registry) {
		return new DefaultTaskSaveService(taskDefinitionRepository, auditRecordService, registry);
	}

	@Bean
	public TaskExecutionCreationService taskExecutionRepositoryService(TaskRepository taskRepository) {
		return new DefaultTaskExecutionRepositoryService(taskRepository);
	}

	@Bean
	public TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator(
			CommonApplicationProperties commonApplicationProperties,
			ApplicationConfigurationMetadataResolver metadataResolver) {
		return new TaskAppDeploymentRequestCreator(commonApplicationProperties,
				metadataResolver, dataflowServerUri);
	}

	@Bean
	public TaskExecutionService taskService(LauncherRepository launcherRepository,
			AuditRecordService auditRecordService,
			TaskRepository taskRepository,
			TaskExecutionInfoService taskExecutionInfoService,
			TaskDeploymentRepository taskDeploymentRepository,
			TaskExecutionCreationService taskExecutionRepositoryService,
			TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator,
			TaskExplorer taskExplorer,
			DataflowTaskExecutionDao dataflowTaskExecutionDao,
			DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao,
			@Nullable OAuth2TokenUtilsService oauth2TokenUtilsService,
			TaskSaveService taskSaveService) {
		DefaultTaskExecutionService defaultTaskExecutionService =  new DefaultTaskExecutionService(
				launcherRepository, auditRecordService, taskRepository,
				taskExecutionInfoService, taskDeploymentRepository, taskExecutionRepositoryService,
				taskAppDeploymentRequestCreator, taskExplorer, dataflowTaskExecutionDao,
				dataflowTaskExecutionMetadataDao, oauth2TokenUtilsService, taskSaveService,
				this.taskConfigurationProperties, this.composedTaskRunnerConfigurationProperties);
		defaultTaskExecutionService.setAutoCreateTaskDefinitions(this.taskConfigurationProperties.isAutoCreateTaskDefinitions());
		return defaultTaskExecutionService;
	}

	@Bean
	public TaskJobService taskJobExecutionRepository(JobService service, TaskExplorer taskExplorer,
			TaskDefinitionRepository taskDefinitionRepository, TaskExecutionService taskExecutionService) {
		return new DefaultTaskJobService(service, taskExplorer, taskDefinitionRepository, taskExecutionService);
	}

	@Bean
	public SimpleJobServiceFactoryBean simpleJobServiceFactoryBean(DataSource dataSource,
			JobRepositoryFactoryBean repositoryFactoryBean, JobExplorer jobExplorer,
			PlatformTransactionManager dataSourceTransactionManager) throws Exception {
		SimpleJobServiceFactoryBean factoryBean = new SimpleJobServiceFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setJobRepository(repositoryFactoryBean.getObject());
		factoryBean.setJobLauncher(new SimpleJobLauncher());
		factoryBean.setDataSource(dataSource);
		factoryBean.setJobExplorer(jobExplorer);
		factoryBean.setTransactionManager(dataSourceTransactionManager);
		return factoryBean;
	}

	@Bean
	public JobExplorerFactoryBean jobExplorerFactoryBean(DataSource dataSource) {
		JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
		jobExplorerFactoryBean.setDataSource(dataSource);
		return jobExplorerFactoryBean;
	}

	@Bean
	public JobRepositoryFactoryBean jobRepositoryFactoryBean(DataSource dataSource,
			PlatformTransactionManager platformTransactionManager) {
		JobRepositoryFactoryBean repositoryFactoryBean = new JobRepositoryFactoryBean();
		repositoryFactoryBean.setDataSource(dataSource);
		repositoryFactoryBean.setTransactionManager(platformTransactionManager);
		return repositoryFactoryBean;
	}
}
