/*
 * Copyright 2016 the original author or authors.
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

import javax.sql.DataSource;

import org.h2.tools.Server;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.service.SimpleJobServiceFactoryBean;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.boot.autoconfigure.batch.BatchDatabaseInitializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.dataflow.server.job.TaskExplorerFactoryBean;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.RdbmsTaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskJobService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskService;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * @author Thomas Risberg
 * @author Janne Valkealahti
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@ConditionalOnProperty(prefix = FeaturesProperties.FEATURES_PREFIX, name = FeaturesProperties.TASKS_ENABLED, matchIfMissing = true)
public class TaskConfiguration {

	@Bean
	public TaskExplorerFactoryBean taskExplorerFactoryBean(DataSource dataSource) {
		return new TaskExplorerFactoryBean(dataSource);
	}

	@Bean
	@ConditionalOnBean(TaskDefinitionRepository.class)
	public TaskService taskService(TaskDefinitionRepository repository, DeploymentIdRepository deploymentIdRepository,
			UriRegistry registry, DelegatingResourceLoader resourceLoader, TaskLauncher taskLauncher) {
		return new DefaultTaskService(repository, deploymentIdRepository, registry, resourceLoader, taskLauncher);
	}

	@Bean
	@ConditionalOnBean(TaskDefinitionRepository.class)
	public TaskJobService taskJobExecutionRepository(JobService service, TaskExplorer taskExplorer,
			TaskDefinitionRepository taskDefinitionRepository, TaskService taskService) {
		return new DefaultTaskJobService(service, taskExplorer, taskDefinitionRepository, taskService);
	}

	@Bean
	public SimpleJobServiceFactoryBean simpleJobServiceFactoryBean(DataSource dataSource,
			JobRepositoryFactoryBean repositoryFactoryBean) throws Exception {
		SimpleJobServiceFactoryBean factoryBean = new SimpleJobServiceFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setJobRepository(repositoryFactoryBean.getObject());
		factoryBean.setJobLocator(new MapJobRegistry());
		factoryBean.setJobLauncher(new SimpleJobLauncher());
		factoryBean.setDataSource(dataSource);
		return factoryBean;
	}

	@Bean
	public JobExplorerFactoryBean jobExplorerFactoryBean(DataSource dataSource) {
		JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
		jobExplorerFactoryBean.setDataSource(dataSource);
		return jobExplorerFactoryBean;
	}

	@Configuration
	@ConditionalOnExpression("#{'${spring.datasource.url:}'.startsWith('jdbc:h2:tcp://localhost:') && '${spring.datasource.url:}'.contains('/mem:')}")
	public static class H2ServerConfiguration {

		@Bean
		public JobRepositoryFactoryBean jobRepositoryFactoryBeanForServer(DataSource dataSource, Server server,
				DataSourceTransactionManager dataSourceTransactionManager) {
			JobRepositoryFactoryBean repositoryFactoryBean = new JobRepositoryFactoryBean();
			repositoryFactoryBean.setDataSource(dataSource);
			repositoryFactoryBean.setTransactionManager(dataSourceTransactionManager);
			return repositoryFactoryBean;
		}

		@Bean
		public BatchDatabaseInitializer batchRepositoryInitializerForDefaultDBForServer(DataSource dataSource,
				Server server) {
			return new BatchDatabaseInitializer();
		}

		@Bean
		public TaskRepositoryInitializer taskRepositoryInitializerForDefaultDB(DataSource dataSource, Server server) {
			TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();
			taskRepositoryInitializer.setDataSource(dataSource);
			return taskRepositoryInitializer;
		}

		@Bean
		@ConditionalOnMissingBean
		public TaskDefinitionRepository taskDefinitionRepository(DataSource dataSource, Server server) throws Exception {
			return new RdbmsTaskDefinitionRepository(dataSource);
		}
	}

	@Configuration
	@ConditionalOnExpression("#{!'${spring.datasource.url:}'.startsWith('jdbc:h2:tcp://localhost:') && !'${spring.datasource.url:}'.contains('/mem:')}")
	public static class NoH2ServerConfiguration {

		@Bean
		public JobRepositoryFactoryBean jobRepositoryFactoryBean(DataSource dataSource,
				DataSourceTransactionManager dataSourceTransactionManager) {
			JobRepositoryFactoryBean repositoryFactoryBean = new JobRepositoryFactoryBean();
			repositoryFactoryBean.setDataSource(dataSource);
			repositoryFactoryBean.setTransactionManager(dataSourceTransactionManager);
			return repositoryFactoryBean;
		}

		@Bean
		public BatchDatabaseInitializer batchRepositoryInitializerForDefaultDB(DataSource dataSource) {
			return new BatchDatabaseInitializer();
		}

		@Bean
		public TaskRepositoryInitializer taskRepositoryInitializerForDB(DataSource dataSource) {
			TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();
			taskRepositoryInitializer.setDataSource(dataSource);
			return taskRepositoryInitializer;
		}

		@Bean
		@ConditionalOnMissingBean
		public TaskDefinitionRepository taskDefinitionRepository(DataSource dataSource) throws Exception {
			return new RdbmsTaskDefinitionRepository(dataSource);
		}
	}
}
