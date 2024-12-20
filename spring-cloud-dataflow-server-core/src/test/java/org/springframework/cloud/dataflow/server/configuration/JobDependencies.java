/*
 * Copyright 2016-2024 the original author or authors.
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
import java.util.Map;

import javax.sql.DataSource;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.audit.repository.AuditRecordRepository;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.audit.service.DefaultAuditRecordService;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.BootApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageMetadataResolver;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepository;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.registry.service.DefaultAppRegistryService;
import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.dataflow.rest.support.jackson.ISO8601DateFormatWithMilliSeconds;
import org.springframework.cloud.dataflow.rest.support.jackson.Jackson2DataflowModule;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.springframework.cloud.dataflow.server.config.DataFlowTaskConfiguration;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.controller.JobExecutionController;
import org.springframework.cloud.dataflow.server.controller.JobExecutionThinController;
import org.springframework.cloud.dataflow.server.controller.JobInstanceController;
import org.springframework.cloud.dataflow.server.controller.JobStepExecutionController;
import org.springframework.cloud.dataflow.server.controller.JobStepExecutionProgressController;
import org.springframework.cloud.dataflow.server.controller.RestControllerAdvice;
import org.springframework.cloud.dataflow.server.controller.TaskDefinitionController;
import org.springframework.cloud.dataflow.server.controller.TaskExecutionController;
import org.springframework.cloud.dataflow.server.controller.TaskExecutionThinController;
import org.springframework.cloud.dataflow.server.controller.TaskLogsController;
import org.springframework.cloud.dataflow.server.controller.TaskPlatformController;
import org.springframework.cloud.dataflow.server.controller.TasksInfoController;
import org.springframework.cloud.dataflow.server.controller.assembler.DefaultTaskDefinitionAssemblerProvider;
import org.springframework.cloud.dataflow.server.controller.assembler.TaskDefinitionAssemblerProvider;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.DataflowJobExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.LauncherService;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionCreationService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.service.TaskSaveService;
import org.springframework.cloud.dataflow.server.service.TaskValidationService;
import org.springframework.cloud.dataflow.server.service.impl.ComposedTaskRunnerConfigurationProperties;
import org.springframework.cloud.dataflow.server.service.impl.DefaultLauncherService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskDeleteService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionRepositoryService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskJobService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskSaveService;
import org.springframework.cloud.dataflow.server.service.impl.TaskAppDeploymentRequestCreator;
import org.springframework.cloud.dataflow.server.service.impl.TaskConfigurationProperties;
import org.springframework.cloud.dataflow.server.service.impl.validation.DefaultTaskValidationService;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorerConfiguration;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExecutionQueryDao;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorer;
import org.springframework.cloud.dataflow.server.task.impl.DefaultDataFlowTaskExecutionQueryDao;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.map.repository.config.EnableMapRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author David Turanski
 * @author Corneil du Plessis
 */
@Configuration
@EnableSpringDataWebSupport
@Import({
		DataflowTaskExplorerConfiguration.class,
		DataFlowTaskConfiguration.class
})
@ImportAutoConfiguration({
		HibernateJpaAutoConfiguration.class,
		JacksonAutoConfiguration.class,
		FlywayAutoConfiguration.class,
		HypermediaAutoConfiguration.class
})
@EnableWebMvc
@EnableTransactionManagement
@EntityScan({
		"org.springframework.cloud.dataflow.server.audit.domain",
		"org.springframework.cloud.dataflow.core"
})
@EnableJpaRepositories(basePackages = {
		"org.springframework.cloud.dataflow.registry.repository",
		"org.springframework.cloud.dataflow.server.repository",
		"org.springframework.cloud.dataflow.audit.repository"
})
@EnableJpaAuditing
@EnableConfigurationProperties({DockerValidatorProperties.class, TaskConfigurationProperties.class,
		TaskProperties.class, ComposedTaskRunnerConfigurationProperties.class})
@EnableMapRepositories(basePackages = "org.springframework.cloud.dataflow.server.job")
public class JobDependencies {

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
	public DataflowTaskExecutionQueryDao dataflowTaskExecutionQueryDao(
		DataSource dataSource) {
		return new DefaultDataFlowTaskExecutionQueryDao(dataSource);
	}

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer dataflowObjectMapperBuilderCustomizer() {
		return (builder) -> {
			builder.dateFormat(new ISO8601DateFormatWithMilliSeconds());
			builder.modules(new JavaTimeModule(), new Jackson2DataflowModule());
		};
	}

	@Bean
	public AuditRecordService auditRecordService(AuditRecordRepository auditRecordRepository) {
		return new DefaultAuditRecordService(auditRecordRepository);
	}

	@Bean
	public TaskValidationService taskValidationService(AppRegistryService appRegistry,
													   DockerValidatorProperties dockerValidatorProperties, TaskDefinitionRepository taskDefinitionRepository,
													   TaskConfigurationProperties taskConfigurationProperties) {
		return new DefaultTaskValidationService(appRegistry, dockerValidatorProperties, taskDefinitionRepository);
	}

	@Bean
	public ApplicationConfigurationMetadataResolver metadataResolver() {
		return new BootApplicationConfigurationMetadataResolver(mock(ContainerImageMetadataResolver.class));
	}

	@Bean
	public JobExecutionController jobExecutionController(TaskJobService repository) {
		return new JobExecutionController(repository);
	}

	@Bean
	public JobExecutionThinController jobExecutionThinController(TaskJobService repository) {
		return new JobExecutionThinController(repository);
	}

	@Bean
	public JobStepExecutionController jobStepExecutionController(JobService jobService) {
		return new JobStepExecutionController(jobService);
	}

	@Bean
	public JobStepExecutionProgressController jobStepExecutionProgressController(JobService jobService, TaskJobService taskJobService) {
		return new JobStepExecutionProgressController(jobService, taskJobService);
	}

	@Bean
	public JobInstanceController jobInstanceController(TaskJobService repository) {
		return new JobInstanceController(repository);
	}

	@Bean
	public TaskExecutionController taskExecutionController(
			DataflowTaskExplorer explorer,
			TaskExecutionService taskExecutionService,
			TaskDefinitionRepository taskDefinitionRepository,
			TaskExecutionInfoService taskExecutionInfoService,
			TaskDeleteService taskDeleteService,
			TaskJobService taskJobService
	) {
		return new TaskExecutionController(
				explorer,
				taskExecutionService,
				taskDefinitionRepository,
				taskExecutionInfoService,
				taskDeleteService,
				taskJobService
		);
	}

	@Bean
	public TaskExecutionThinController taskExecutionThinController(DataflowTaskExplorer dataflowTaskExplorer, TaskDefinitionRepository taskDefinitionRepository) {
		return new TaskExecutionThinController(dataflowTaskExplorer, taskDefinitionRepository);
	}

	@Bean
	public TasksInfoController taskExecutionsInfoController(TaskExecutionService taskExecutionService) {
		return new TasksInfoController(taskExecutionService);
	}

	@Bean
	public TaskPlatformController taskPlatformController(LauncherService launcherService) {
		return new TaskPlatformController(launcherService);
	}

	@Bean
	LauncherService launcherService(LauncherRepository launcherRepository) {
		return new DefaultLauncherService(launcherRepository);
	}

	@Bean
	public TaskDefinitionAssemblerProvider taskDefinitionAssemblerProvider(
		TaskExecutionService taskExecutionService,
		TaskJobService taskJobService,
		DataflowTaskExplorer taskExplorer) {
		return new DefaultTaskDefinitionAssemblerProvider(taskExecutionService, taskJobService, taskExplorer);
	}

	@Bean
	public TaskDefinitionController taskDefinitionController(
		DataflowTaskExplorer explorer, TaskDefinitionRepository repository,
		TaskSaveService taskSaveService, TaskDeleteService taskDeleteService,
		TaskDefinitionAssemblerProvider taskDefinitionAssemblerProvider
	) {
		return new TaskDefinitionController(explorer,
			repository,
			taskSaveService,
			taskDeleteService,
			taskDefinitionAssemblerProvider
		);
	}

	@Bean
	public TaskLogsController taskLogsController(TaskExecutionService taskExecutionService) {
		return new TaskLogsController(taskExecutionService);
	}

	@Bean
	public TaskJobService taskJobExecutionRepository(
			JobService jobService,
			DataflowTaskExplorer taskExplorer,
			TaskDefinitionRepository taskDefinitionRepository,
			TaskExecutionService taskExecutionService,
			LauncherRepository launcherRepository,
			TaskConfigurationProperties taskConfigurationProperties
	) {
		return new DefaultTaskJobService(
				jobService,
				taskExplorer,
				taskDefinitionRepository,
				taskExecutionService,
				launcherRepository,
				taskConfigurationProperties);
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
			SchedulerService schedulerService,
			TaskConfigurationProperties taskConfigurationProperties,
			DataSource dataSource
	) {

		return new DefaultTaskDeleteService(
				taskExplorer,
				launcherRepository,
				taskDefinitionRepository,
				taskDeploymentRepository,
				auditRecordService,
				dataflowTaskExecutionDao,
				dataflowJobExecutionDao,
				dataflowTaskExecutionMetadataDao,
				schedulerService,
				taskConfigurationProperties,
				dataSource
		);
	}

	@Bean
	public TaskSaveService saveTaskService(TaskDefinitionRepository taskDefinitionRepository,
										   AuditRecordService auditRecordService, AppRegistryService registry) {
		return new DefaultTaskSaveService(taskDefinitionRepository, auditRecordService, registry);
	}

	@Bean
	public TaskExecutionCreationService taskExecutionRepositoryService(
			TaskRepository taskRepository) {
		return new DefaultTaskExecutionRepositoryService(taskRepository);
	}

	@Bean
	TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator(
			CommonApplicationProperties commonApplicationProperties,
			ApplicationConfigurationMetadataResolver metadataResolver) {
		return new TaskAppDeploymentRequestCreator(commonApplicationProperties,
				metadataResolver, null);
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskExecutionService taskService(
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
			TaskConfigurationProperties taskConfigurationProperties,
			ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties,
			TaskDefinitionRepository taskDefinitionRepository
	) {
		return new DefaultTaskExecutionService(
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
				taskSaveService, taskConfigurationProperties,
				composedTaskRunnerConfigurationProperties);
	}

	@Bean
	public TaskExecutionInfoService taskDefinitionRetriever(
			AppRegistryService registry,
			DataflowTaskExplorer taskExplorer,
			TaskDefinitionRepository taskDefinitionRepository,
			TaskConfigurationProperties taskConfigurationProperties,
			LauncherRepository launcherRepository,
			List<TaskPlatform> taskPlatforms,
			ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties
	) {
		return new DefaultTaskExecutionInfoService(
				new DataSourceProperties(),
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
	public TaskRepository taskRepository(DataSource dataSource) {
		TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean = new TaskExecutionDaoFactoryBean(dataSource, "TASK_");
		return new SimpleTaskRepository(taskExecutionDaoFactoryBean);
	}

	@Bean
	@Primary
	public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}


	@Bean
	public BatchDataSourceScriptDatabaseInitializer batchRepositoryInitializerForDefaultDBForServer(DataSource dataSource,
																									BatchProperties properties) {
		return new BatchDataSourceScriptDatabaseInitializer(dataSource, properties.getJdbc());
	}

	@Bean
	public RestControllerAdvice restControllerAdvice() {
		return new RestControllerAdvice();
	}

	@Bean
	@ConditionalOnMissingBean
	public AppRegistryService appRegistryService(AppRegistrationRepository appRegistrationRepository,
												 AuditRecordService auditRecordService) {
		return new DefaultAppRegistryService(appRegistrationRepository,
				new AppResourceCommon(new MavenProperties(), new DefaultResourceLoader()), auditRecordService);
	}


	@Bean
	public TaskPlatform taskPlatform() {
		Launcher launcher = new Launcher("default", "defaultType", null, null);
		List<Launcher> launchers = new ArrayList<>();
		launchers.add(launcher);
		return new TaskPlatform("testTaskPlatform", launchers);
	}

	@Bean
	public TaskLauncher taskLauncher() {
		return mock(TaskLauncher.class);
	}

	@Bean
	public SchedulerService schedulerService() {
		return new SchedulerService() {
			@Override
			public void schedule(String scheduleName, String taskDefinitionName, Map<String, String> taskProperties, List<String> commandLineArgs, String platformName) {

			}

			@Override
			public void schedule(String scheduleName, String taskDefinitionName, Map<String, String> taskProperties, List<String> commandLineArgs) {

			}

			@Override
			public void unschedule(String scheduleName, String platformName) {

			}

			@Override
			public void unschedule(String scheduleName) {

			}

			@Override
			public void unscheduleForTaskDefinition(String taskDefinitionName) {

			}

			@Override
			public List<ScheduleInfo> list(Pageable pageable, String taskDefinitionName, String platformName) {
				return null;
			}

			@Override
			public Page<ScheduleInfo> list(Pageable pageable, String platformName) {
				return null;
			}

			@Override
			public Page<ScheduleInfo> list(Pageable pageable) {
				return null;
			}

			@Override
			public List<ScheduleInfo> list(String taskDefinitionName, String platformName) {
				return null;
			}

			@Override
			public List<ScheduleInfo> list(String taskDefinitionName) {
				return null;
			}

			@Override
			public List<ScheduleInfo> listForPlatform(String platformName) {
				return null;
			}

			@Override
			public List<ScheduleInfo> list() {
				return null;
			}

			@Override
			public ScheduleInfo getSchedule(String scheduleName, String platformName) {
				return null;
			}

			@Override
			public ScheduleInfo getSchedule(String scheduleName) {
				return null;
			}
		};
	}

	@Bean
	public OAuth2TokenUtilsService oauth2TokenUtilsService() {
		return mock(OAuth2TokenUtilsService.class);
	}
}
