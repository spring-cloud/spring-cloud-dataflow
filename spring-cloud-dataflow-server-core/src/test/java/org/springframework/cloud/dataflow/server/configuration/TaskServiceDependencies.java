/*
 * Copyright 2015-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.server.configuration;


import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.audit.repository.AuditRecordRepository;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.audit.service.DefaultAuditRecordService;
import org.springframework.cloud.dataflow.completion.CompletionConfiguration;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryService;
import org.springframework.cloud.dataflow.core.DefaultStreamDefinitionService;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.dataflow.server.config.DataFlowTaskConfiguration;
import org.springframework.cloud.dataflow.server.config.VersionInfoProperties;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.config.features.SchedulerConfiguration;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.DataflowJobExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.SchedulerServiceProperties;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionCreationService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskSaveService;
import org.springframework.cloud.dataflow.server.service.TaskValidationService;
import org.springframework.cloud.dataflow.server.service.impl.ComposedTaskRunnerConfigurationProperties;
import org.springframework.cloud.dataflow.server.service.impl.DefaultSchedulerService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskDeleteService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionRepositoryService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskSaveService;
import org.springframework.cloud.dataflow.server.service.impl.TaskAppDeploymentRequestCreator;
import org.springframework.cloud.dataflow.server.service.impl.TaskConfigurationProperties;
import org.springframework.cloud.dataflow.server.service.impl.validation.DefaultTaskValidationService;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorerConfiguration;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExecutionQueryDao;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorer;
import org.springframework.cloud.dataflow.server.task.impl.DefaultDataFlowTaskExecutionQueryDao;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.map.repository.config.EnableMapRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@EnableSpringDataWebSupport
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@Import({
		CompletionConfiguration.class,
		DataflowTaskExplorerConfiguration.class,
		DataFlowTaskConfiguration.class
})
@ImportAutoConfiguration({
		TransactionManagerCustomizationAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
		JacksonAutoConfiguration.class,
		FlywayAutoConfiguration.class,
		RestTemplateAutoConfiguration.class
})
@EnableWebMvc
@EnableConfigurationProperties({
		CommonApplicationProperties.class,
		VersionInfoProperties.class,
		DockerValidatorProperties.class,
		TaskConfigurationProperties.class,
		TaskProperties.class,
		DockerValidatorProperties.class,
		ComposedTaskRunnerConfigurationProperties.class
})
@EntityScan({
		"org.springframework.cloud.dataflow.registry.domain",
		"org.springframework.cloud.dataflow.core"
})
@EnableMapRepositories("org.springframework.cloud.dataflow.server.job")
@EnableJpaRepositories(basePackages = {
		"org.springframework.cloud.dataflow.registry.repository",
		"org.springframework.cloud.dataflow.server.repository",
		"org.springframework.cloud.dataflow.audit.repository"
})
@EnableJpaAuditing
@EnableTransactionManagement
public class TaskServiceDependencies extends WebMvcConfigurationSupport {

	@Autowired
	DataSourceProperties dataSourceProperties;

	@Autowired
	TaskConfigurationProperties taskConfigurationProperties;

	@Autowired
	ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties;

	@Autowired
	DockerValidatorProperties dockerValidatorProperties;

	@Bean
	@ConditionalOnMissingBean
	public StreamDefinitionService streamDefinitionService() {
		return new DefaultStreamDefinitionService();
	}

	@Bean
	public TaskValidationService taskValidationService(AppRegistryService appRegistry,
													   DockerValidatorProperties dockerValidatorProperties, TaskDefinitionRepository taskDefinitionRepository) {
		return new DefaultTaskValidationService(appRegistry,
				dockerValidatorProperties,
				taskDefinitionRepository);
	}

	@Bean
	PlatformTransactionManager transactionManager(TransactionManagerCustomizers transactionManagerCustomizers) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManagerCustomizers.customize(transactionManager);
		return transactionManager;
	}

	@Bean
	public JobRepository jobRepository(DataSource dataSource,
									   PlatformTransactionManager platformTransactionManager) throws Exception{
		JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setTransactionManager(platformTransactionManager);

		try {
			factoryBean.afterPropertiesSet();
		} catch (Throwable x) {
			throw new RuntimeException("Exception creating JobRepository", x);
		}
		return factoryBean.getObject();
	}

	@Bean
	public AuditRecordService auditRecordService(AuditRecordRepository repository) {
		return new DefaultAuditRecordService(repository);
	}


	@Bean
	public AppRegistryService appRegistry() {
		return mock(AppRegistryService.class);
	}

	@Bean
	public ResourceLoader resourceLoader() {
		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		when(resourceLoader.getResource(anyString())).thenReturn(mock(Resource.class));
		return resourceLoader;
	}

	@Bean
	TaskLauncher taskLauncher() {
		TaskLauncher taskLauncher = mock(TaskLauncher.class);
		when(taskLauncher.getLog(any())).thenReturn("");
		return taskLauncher;
	}

	@Bean
	ApplicationConfigurationMetadataResolver metadataResolver() {
		return mock(ApplicationConfigurationMetadataResolver.class);
	}

	@Bean
	public ContainerRegistryService containerRegistryService() {
		return mock(ContainerRegistryService.class);
	}

	@Bean
	public FeaturesProperties featuresProperties() {
		return new FeaturesProperties();
	}

	@Bean
	public SchedulerServiceProperties schedulerServiceProperties() {
		return new SchedulerServiceProperties();
	}


	@Bean
	public TaskDeleteService deleteTaskService(
			DataflowTaskExplorer taskExplorer,
			LauncherRepository launcherRepository,
			TaskDefinitionRepository taskDefinitionRepository,
			TaskDeploymentRepository taskDeploymentRepository,
			AuditRecordService auditRecordService,
			DataflowTaskExecutionDao dataflowTaskExecutionDao,
			DataflowJobExecutionDao dataflowJobExecutionDao,
			DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao,
			@Autowired(required = false) SchedulerService schedulerService,
			TaskConfigurationProperties taskConfigurationProperties,
			DataSource dataSource
	) {

		return new DefaultTaskDeleteService(taskExplorer, launcherRepository, taskDefinitionRepository,
				taskDeploymentRepository,
				auditRecordService,
				dataflowTaskExecutionDao,
				dataflowJobExecutionDao,
				dataflowTaskExecutionMetadataDao,
				schedulerService,
				taskConfigurationProperties,
				dataSource);
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskSaveService saveTaskService(TaskDefinitionRepository taskDefinitionRepository,
										   AuditRecordService auditRecordService, AppRegistryService registry) {
		return new DefaultTaskSaveService(taskDefinitionRepository, auditRecordService, registry);
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskExecutionCreationService taskExecutionRepositoryService(
			TaskRepository taskRepository
	) {
		return new DefaultTaskExecutionRepositoryService(taskRepository);
	}

	@Bean
	public TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator(
			CommonApplicationProperties commonApplicationProperties,
			ApplicationConfigurationMetadataResolver metadataResolver) {
		return new TaskAppDeploymentRequestCreator(commonApplicationProperties,
				metadataResolver, null);
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskExecutionService defaultTaskService(
			ApplicationContext applicationContext,
			LauncherRepository launcherRepository,
			AuditRecordService auditRecordService,
			TaskRepository taskRepository,
			TaskExecutionInfoService taskExecutionInfoService,
			TaskDeploymentRepository taskDeploymentRepository,
			TaskExecutionCreationService taskExecutionRepositoryService,
			TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator,
			DataflowTaskExplorer taskExplorer,
			DataflowTaskExecutionDao dataflowTaskExecutionDao,
			DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao,
			DataflowTaskExecutionQueryDao dataflowTaskExecutionQueryDao,
			OAuth2TokenUtilsService oauth2TokenUtilsService,
			TaskSaveService taskSaveService,
			TaskDefinitionRepository taskDefinitionRepository
	) {
		DefaultTaskExecutionService taskExecutionService = new DefaultTaskExecutionService(
				applicationContext.getEnvironment(),
				launcherRepository,
				auditRecordService,
				taskRepository,
				taskExecutionInfoService,
				taskDeploymentRepository,
				taskDefinitionRepository,
				taskExecutionRepositoryService,
				taskAppDeploymentRequestCreator,
				taskExplorer,
				dataflowTaskExecutionDao,
				dataflowTaskExecutionMetadataDao,
				dataflowTaskExecutionQueryDao,
				oauth2TokenUtilsService,
				taskSaveService,
				this.taskConfigurationProperties,
				this.composedTaskRunnerConfigurationProperties);
		taskExecutionService.setAutoCreateTaskDefinitions(this.taskConfigurationProperties.isAutoCreateTaskDefinitions());
		return taskExecutionService;
	}

	@Bean
	public TaskRepository taskRepository(DataSource dataSource) {
		TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean = new TaskExecutionDaoFactoryBean(dataSource);
		return new SimpleTaskRepository(taskExecutionDaoFactoryBean);
	}

	@Bean
	public DataflowTaskExecutionQueryDao dataflowTaskExecutionQueryDao(
		DataSource dataSource) {
		return new DefaultDataFlowTaskExecutionQueryDao(dataSource);
	}
	@Bean
	@ConditionalOnMissingBean
	public TaskExecutionInfoService taskDefinitionRetriever(
			AppRegistryService registry,
			DataflowTaskExplorer taskExplorer,
			TaskDefinitionRepository taskDefinitionRepository,
			TaskConfigurationProperties taskConfigurationProperties,
			LauncherRepository launcherRepository,
			List<TaskPlatform> taskPlatforms,
			ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties
	) {
		return new DefaultTaskExecutionInfoService(this.dataSourceProperties,
				registry,
				taskExplorer,
				taskDefinitionRepository,
				taskConfigurationProperties,
				launcherRepository,
				taskPlatforms,
				composedTaskRunnerConfigurationProperties
		);
	}

	@Bean
	@Conditional({SchedulerConfiguration.SchedulerConfigurationPropertyChecker.class})
	public SchedulerService schedulerService(
			CommonApplicationProperties commonApplicationProperties,
			List<TaskPlatform> taskPlatforms, TaskDefinitionRepository taskDefinitionRepository,
			AppRegistryService registry, ResourceLoader resourceLoader,
			ApplicationConfigurationMetadataResolver metaDataResolver,
			SchedulerServiceProperties schedulerServiceProperties,
			AuditRecordService auditRecordService,
			TaskConfigurationProperties taskConfigurationProperties,
			DataSourceProperties dataSourceProperties,
			TaskExecutionInfoService taskExecutionInfoService,
			PropertyResolver propertyResolver,
			ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties
	) {
		return new DefaultSchedulerService(commonApplicationProperties,
				taskPlatforms,
				taskDefinitionRepository,
				registry,
				resourceLoader,
				taskConfigurationProperties,
				dataSourceProperties,
				null,
				metaDataResolver,
				schedulerServiceProperties,
				auditRecordService,
				taskExecutionInfoService,
				propertyResolver,
				composedTaskRunnerConfigurationProperties);
	}

	@Bean
	public TaskPlatform taskPlatform(Scheduler scheduler) {
		Launcher launcher = new Launcher("testTaskPlatform", "defaultType", mock(TaskLauncher.class), scheduler);
		List<Launcher> launchers = new ArrayList<>();
		launchers.add(launcher);
		return new TaskPlatform("testTaskPlatform", launchers);
	}

	@Bean
	public Scheduler scheduler() {
		return new SimpleTestScheduler();
	}

	@Bean
	public OAuth2TokenUtilsService oauth2TokenUtilsService() {
		final OAuth2TokenUtilsService oauth2TokenUtilsService = mock(OAuth2TokenUtilsService.class);
		when(oauth2TokenUtilsService.getAccessTokenOfAuthenticatedUser()).thenReturn("foo-bar-123-token");
		return oauth2TokenUtilsService;
	}
	@Bean
	public JobExplorer jobExplorer(DataSource dataSource, PlatformTransactionManager platformTransactionManager)
		throws Exception {
		JobExplorerFactoryBean factoryBean = new JobExplorerFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setTransactionManager(platformTransactionManager);
		try {
			factoryBean.afterPropertiesSet();
		} catch (Throwable x) {
			throw new RuntimeException("Exception creating JobExplorer", x);
		}
		return factoryBean.getObject();
	}

}
