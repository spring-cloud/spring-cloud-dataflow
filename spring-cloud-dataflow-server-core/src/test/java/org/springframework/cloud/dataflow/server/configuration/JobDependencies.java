/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.configuration;

import static org.mockito.Mockito.mock;
import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;

import javax.sql.DataSource;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.service.SimpleJobServiceFactoryBean;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchDatabaseInitializer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.BootApplicationConfigurationMetadataResolver;
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
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskJobService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskService;
import org.springframework.cloud.deployer.resource.registry.InMemoryUriRegistry;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.batch.listener.support.JdbcTaskBatchDao;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
@Configuration
@EnableSpringDataWebSupport
@EnableHypermediaSupport(type = HAL)
@EnableWebMvc
public class JobDependencies {

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
	public TaskExecutionController taskExecutionController(TaskExplorer explorer, TaskDefinitionRepository taskDefinitionRepository) {
		return new TaskExecutionController(explorer, taskDefinitionRepository);
	}

	@Bean
	public TaskRepositoryInitializer taskExecutionRepository(DataSource dataSource) {
		TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();
		taskRepositoryInitializer.setDataSource(dataSource);
		return taskRepositoryInitializer;
	}

	@Bean
	public TaskJobService taskJobExecutionRepository(JobService jobService,
			TaskExplorer taskExplorer, TaskDefinitionRepository taskDefinitionRepository, TaskService taskService){
		return new DefaultTaskJobService(jobService, taskExplorer, taskDefinitionRepository, taskService);
	}

	@Bean
	public TaskDefinitionRepository taskDefinitionRepository() {
		return new InMemoryTaskDefinitionRepository();
	}

	@Bean
	public TaskService taskService(TaskDefinitionRepository repository, DeploymentIdRepository deploymentIdRepository,
			UriRegistry registry, ResourceLoader resourceLoader, TaskLauncher taskLauncher,
			ApplicationConfigurationMetadataResolver metadataResolver) {
		return new DefaultTaskService(new DataSourceProperties(), repository, deploymentIdRepository,
				registry, resourceLoader, taskLauncher, metadataResolver);
	}

	@Bean
	public TaskBatchDao taskBatchDao(DataSource dataSource){
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
	public JobRepositoryFactoryBean jobRepositoryFactoryBeanForServer(DataSource dataSource, DataSourceTransactionManager dataSourceTransactionManager){
		JobRepositoryFactoryBean repositoryFactoryBean = new JobRepositoryFactoryBean();
		repositoryFactoryBean.setDataSource(dataSource);
		repositoryFactoryBean.setTransactionManager(dataSourceTransactionManager);
		return repositoryFactoryBean;
	}

	@Bean
	public DataSourceTransactionManager transactionManagerForServer(DataSource dataSource){
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	public JobExplorerFactoryBean jobExplorerFactoryBeanForServer(DataSource dataSource){
		JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
		jobExplorerFactoryBean.setDataSource(dataSource);
		return jobExplorerFactoryBean;
	}

	@Bean
	public BatchDatabaseInitializer batchRepositoryInitializerForDefaultDBForServer(DataSource dataSource) {
		return new BatchDatabaseInitializer();
	}

	@Bean
	public TaskExplorer taskExplorer(TaskExecutionDaoFactoryBean daoFactoryBean){
		return new SimpleTaskExplorer(daoFactoryBean);
	}

	@Bean
	public TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean(DataSource dataSource){
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
	public TaskLauncher taskLauncher() {
		return mock(TaskLauncher.class);
	}
}
