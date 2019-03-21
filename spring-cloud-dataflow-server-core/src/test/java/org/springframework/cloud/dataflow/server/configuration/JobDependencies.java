/*
 * Copyright 2016-2018 the original author or authors.
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

import javax.sql.DataSource;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.service.SimpleJobServiceFactoryBean;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.BootApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.dataflow.server.audit.repository.AuditRecordRepository;
import org.springframework.cloud.dataflow.server.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.server.audit.service.DefaultAuditRecordService;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.controller.JobExecutionController;
import org.springframework.cloud.dataflow.server.controller.JobInstanceController;
import org.springframework.cloud.dataflow.server.controller.JobStepExecutionController;
import org.springframework.cloud.dataflow.server.controller.JobStepExecutionProgressController;
import org.springframework.cloud.dataflow.server.controller.RestControllerAdvice;
import org.springframework.cloud.dataflow.server.controller.TaskExecutionController;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryDeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryTaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskExecutionCreationService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.TaskValidationService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionCreationService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskJobService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskService;
import org.springframework.cloud.dataflow.server.service.impl.TaskConfigurationProperties;
import org.springframework.cloud.dataflow.server.service.impl.validation.DefaultTaskValidationService;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.registry.InMemoryUriRegistry;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.batch.listener.support.JdbcTaskBatchDao;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
@Configuration
@EnableSpringDataWebSupport
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@ImportAutoConfiguration({ HibernateJpaAutoConfiguration.class, EmbeddedDataSourceConfiguration.class })
@EnableWebMvc
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {
	"org.springframework.cloud.dataflow.registry.repository",
	"org.springframework.cloud.dataflow.server.audit.repository"
})
@EnableJpaAuditing
@EntityScan({
	"org.springframework.cloud.dataflow.registry.domain",
	"org.springframework.cloud.dataflow.server.audit.domain"
})
@EnableConfigurationProperties({ DockerValidatorProperties.class, TaskConfigurationProperties.class})
public class JobDependencies {

	@Bean
	public AuditRecordService auditRecordService(AuditRecordRepository auditRecordRepository) {
		return new DefaultAuditRecordService(auditRecordRepository);
	}

	@Bean
	public TaskValidationService taskValidationService(AppRegistryCommon appRegistryCommon,
			DockerValidatorProperties dockerValidatorProperties, TaskDefinitionRepository taskDefinitionRepository,
			TaskConfigurationProperties taskConfigurationProperties) {
		return new DefaultTaskValidationService(appRegistryCommon, dockerValidatorProperties, taskDefinitionRepository,
				taskConfigurationProperties.getComposedTaskRunnerName());
	}

	@Bean
	public ApplicationConfigurationMetadataResolver metadataResolver() {
		return new BootApplicationConfigurationMetadataResolver();
	}

	@Bean
	public JobExecutionController jobExecutionController(TaskJobService repository) {
		return new JobExecutionController(repository);
	}

	@Bean
	public JobStepExecutionController jobStepExecutionController(JobService jobService) {
		return new JobStepExecutionController(jobService);
	}

	@Bean
	public JobStepExecutionProgressController jobStepExecutionProgressController(JobService jobService) {
		return new JobStepExecutionProgressController(jobService);
	}

	@Bean
	public JobInstanceController jobInstanceController(TaskJobService repository) {
		return new JobInstanceController(repository);
	}

	@Bean
	public TaskExecutionController taskExecutionController(TaskExplorer explorer, TaskService taskService,
			TaskDefinitionRepository taskDefinitionRepository) {
		return new TaskExecutionController(explorer, taskService, taskDefinitionRepository);
	}

	@Bean
	public TaskRepositoryInitializer taskExecutionRepository(DataSource dataSource) {
		TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();
		taskRepositoryInitializer.setDataSource(dataSource);
		return taskRepositoryInitializer;
	}

	@Bean
	public TaskJobService taskJobExecutionRepository(JobService jobService, TaskExplorer taskExplorer,
			TaskDefinitionRepository taskDefinitionRepository, TaskService taskService) {
		return new DefaultTaskJobService(jobService, taskExplorer, taskDefinitionRepository, taskService);
	}

	@Bean
	public TaskDefinitionRepository taskDefinitionRepository() {
		return new InMemoryTaskDefinitionRepository();
	}

	@Bean
	public TaskExecutionCreationService taskExecutionRepositoryService(TaskRepository taskRepository) {
		return new DefaultTaskExecutionCreationService(taskRepository);
	}

	@Bean
	public TaskService taskService(TaskDefinitionRepository repository, TaskExplorer explorer, AppRegistry registry,
			TaskLauncher taskLauncher, ApplicationConfigurationMetadataResolver metadataResolver,
			DeploymentIdRepository deploymentIdRepository, AuditRecordService auditRecordService,
			CommonApplicationProperties commonApplicationProperties, TaskValidationService taskValidationService,
			TaskExecutionCreationService taskExecutionCreationService) {
		return new DefaultTaskService(new DataSourceProperties(), repository, explorer, taskRepository(), registry,
				taskLauncher, metadataResolver, new TaskConfigurationProperties(), deploymentIdRepository,
				auditRecordService, null, commonApplicationProperties, taskValidationService,
				taskExecutionCreationService);
	}

	@Bean
	public TaskRepository taskRepository() {
		return new SimpleTaskRepository(new TaskExecutionDaoFactoryBean());
	}

	@Bean
	public TaskBatchDao taskBatchDao(DataSource dataSource) {
		return new JdbcTaskBatchDao(dataSource);
	}

	@Bean
	public SimpleJobServiceFactoryBean simpleJobServiceFactoryBean(DataSource dataSource,
			JobRepositoryFactoryBean repositoryFactoryBean) {
		SimpleJobServiceFactoryBean factoryBean = new SimpleJobServiceFactoryBean();
		factoryBean.setDataSource(dataSource);
		try {
			factoryBean.setJobRepository(repositoryFactoryBean.getObject());
			factoryBean.setJobLocator(new MapJobRegistry());
			factoryBean.setJobLauncher(new SimpleJobLauncher());
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return factoryBean;
	}

	@Bean
	public JobRepositoryFactoryBean jobRepositoryFactoryBeanForServer(DataSource dataSource,
			DataSourceTransactionManager dataSourceTransactionManager) {
		JobRepositoryFactoryBean repositoryFactoryBean = new JobRepositoryFactoryBean();
		repositoryFactoryBean.setDataSource(dataSource);
		repositoryFactoryBean.setTransactionManager(dataSourceTransactionManager);
		return repositoryFactoryBean;
	}

	@Bean
	public DataSourceTransactionManager transactionManagerForServer(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	public JobExplorerFactoryBean jobExplorerFactoryBeanForServer(DataSource dataSource) {
		JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
		jobExplorerFactoryBean.setDataSource(dataSource);
		return jobExplorerFactoryBean;
	}

	@Bean
	public BatchDatabaseInitializer batchRepositoryInitializerForDefaultDBForServer(DataSource dataSource,
			ResourceLoader resourceLoader, BatchProperties properties) {
		return new BatchDatabaseInitializer(dataSource, resourceLoader, properties);
	}

	@Bean
	public TaskExplorer taskExplorer(TaskExecutionDaoFactoryBean daoFactoryBean) {
		return new SimpleTaskExplorer(daoFactoryBean);
	}

	@Bean
	public TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean(DataSource dataSource) {
		return new TaskExecutionDaoFactoryBean(dataSource);
	}

	@Bean
	public RestControllerAdvice restControllerAdvice() {
		return new RestControllerAdvice();
	}

	@Bean
	public DeploymentIdRepository deploymentIdRepository() {
		return new InMemoryDeploymentIdRepository();
	}

	@Bean
	public UriRegistry uriRegistry() {
		return new InMemoryUriRegistry();
	}

	@Bean
	public AppRegistry appRegistry() {
		return new AppRegistry(uriRegistry(), new AppResourceCommon(new MavenProperties(), new DefaultResourceLoader()));
	}

	@Bean
	public TaskLauncher taskLauncher() {
		return mock(TaskLauncher.class);
	}
}
