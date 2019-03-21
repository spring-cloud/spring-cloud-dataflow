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
package org.springframework.cloud.dataflow.server.config.features;

import javax.sql.DataSource;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.service.SimpleJobServiceFactoryBean;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AbstractDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.job.TaskExplorerFactoryBean;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.RdbmsTaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskJobService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskService;
import org.springframework.cloud.dataflow.server.service.impl.TaskConfigurationProperties;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ResourceLoader;
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
 */
@Configuration
@ConditionalOnProperty(prefix = FeaturesProperties.FEATURES_PREFIX, name = FeaturesProperties.TASKS_ENABLED, matchIfMissing = true)
@EnableConfigurationProperties({ TaskConfigurationProperties.class, CommonApplicationProperties.class })
@EnableTransactionManagement
public class TaskConfiguration {

	@Autowired
	DataSourceProperties dataSourceProperties;

	@Value("${spring.cloud.dataflow.server.uri:}")
	private String dataflowServerUri;

	@Bean
	public TaskExplorerFactoryBean taskExplorerFactoryBean(DataSource dataSource) {
		return new TaskExplorerFactoryBean(dataSource);
	}

	@Bean
	public TaskRepository taskRepository(DataSource dataSource) {
		return new SimpleTaskRepository(new TaskExecutionDaoFactoryBean(dataSource));
	}

	@Bean
	@ConditionalOnBean(TaskDefinitionRepository.class)
	public TaskService taskService(TaskDefinitionRepository repository, TaskExplorer taskExplorer,
			TaskRepository taskExecutionRepository, AppRegistryCommon registry, DelegatingResourceLoader resourceLoader,
			TaskLauncher taskLauncher, ApplicationConfigurationMetadataResolver metadataResolver,
			TaskConfigurationProperties taskConfigurationProperties, DeploymentIdRepository deploymentIdRepository,
			CommonApplicationProperties commonApplicationProperties) {
		return new DefaultTaskService(dataSourceProperties, repository, taskExplorer, taskExecutionRepository, registry,
				resourceLoader, taskLauncher, metadataResolver, taskConfigurationProperties, deploymentIdRepository,
				this.dataflowServerUri, commonApplicationProperties);
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
	@ConditionalOnProperty(name = "spring.dataflow.embedded.database.enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnExpression("#{'${spring.datasource.url:}'.startsWith('jdbc:h2:tcp://localhost:')}")
	public static class H2ServerConfiguration {

		@Bean
		public JobRepositoryFactoryBean jobRepositoryFactoryBeanForServer(DataSource dataSource,
				PlatformTransactionManager dataSourceTransactionManager) {
			JobRepositoryFactoryBean repositoryFactoryBean = new JobRepositoryFactoryBean();
			repositoryFactoryBean.setDataSource(dataSource);
			repositoryFactoryBean.setTransactionManager(dataSourceTransactionManager);
			return repositoryFactoryBean;
		}

		@Bean
		public BatchDatabaseInitializer batchRepositoryInitializerForDefaultDBForServer(DataSource dataSource,
				ResourceLoader resourceLoader, BatchProperties properties) {
			return new BatchDatabaseInitializer(dataSource, resourceLoader, properties);
		}

		@Bean
		public TaskRepositoryInitializer taskRepositoryInitializerForDefaultDB(DataSource dataSource) {
			TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();
			taskRepositoryInitializer.setDataSource(dataSource);
			return taskRepositoryInitializer;
		}

		@Bean
		@ConditionalOnMissingBean
		public TaskDefinitionRepository taskDefinitionRepository(DataSource dataSource) {
			return new RdbmsTaskDefinitionRepository(dataSource);
		}
	}

	@Configuration
	@ConditionalOnExpression("#{!'${spring.datasource.url:}'.startsWith('jdbc:h2:tcp://localhost:') || "
			+ "('${spring.datasource.url:}'.startsWith('jdbc:h2:tcp://localhost:') &&"
			+ "'${spring.dataflow.embedded.database.enabled}'.equals('false'))}")
	public static class NoH2ServerConfiguration {

		@Bean
		public JobRepositoryFactoryBean jobRepositoryFactoryBean(DataSource dataSource,
				PlatformTransactionManager platformTransactionManager) {
			JobRepositoryFactoryBean repositoryFactoryBean = new JobRepositoryFactoryBean();
			repositoryFactoryBean.setDataSource(dataSource);
			repositoryFactoryBean.setTransactionManager(platformTransactionManager);
			return repositoryFactoryBean;
		}

		@Bean
		public BatchDatabaseInitializer batchRepositoryInitializerForDefaultDB(DataSource dataSource,
				ResourceLoader resourceLoader, BatchProperties properties) {
			return new BatchDatabaseInitializer(dataSource, resourceLoader, properties);
		}

		@Bean
		public TaskRepositoryInitializer taskRepositoryInitializerForDB(DataSource dataSource) {
			TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();
			taskRepositoryInitializer.setDataSource(dataSource);
			return taskRepositoryInitializer;
		}

		@Bean
		@DependsOn({ "batchRepositoryInitializerForDefaultDB", "taskRepositoryInitializerForDB" })
		public AbstractDatabaseInitializer batchTaskIndexesDatabaseInitializer(DataSource dataSource,
				ResourceLoader resourceLoader) {
			return new BatchTaskIndexesDatabaseInitializer(dataSource, resourceLoader);
		}

		@Bean
		@ConditionalOnMissingBean
		public TaskDefinitionRepository taskDefinitionRepository(DataSource dataSource) throws Exception {
			return new RdbmsTaskDefinitionRepository(dataSource);
		}
	}

}
