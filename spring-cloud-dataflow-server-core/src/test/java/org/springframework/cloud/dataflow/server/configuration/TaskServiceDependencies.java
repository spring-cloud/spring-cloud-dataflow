/*
 * Copyright 2015-2018 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.repository.InMemoryDeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.RdbmsTaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.support.DataflowRdbmsInitializer;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskService;
import org.springframework.cloud.dataflow.server.service.impl.TaskConfigurationProperties;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 */
@EnableTransactionManagement
@Configuration
public class TaskServiceDependencies {

	@Autowired
	DataSourceProperties dataSourceProperties;

	@Bean
	public TaskRepositoryInitializer taskExecutionRepository(DataSource dataSource) {
		TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();
		taskRepositoryInitializer.setDataSource(dataSource);
		return taskRepositoryInitializer;
	}

	@Bean
	public TaskDefinitionRepository taskDefinitionRepository(DataSource dataSource) {
		return new RdbmsTaskDefinitionRepository(dataSource);
	}

	@Bean
	public TaskRepository taskRepository(TaskExecutionDaoFactoryBean daoFactoryBean) {
		return new SimpleTaskRepository(daoFactoryBean);
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
	public AppRegistry appRegistry() {
		return mock(AppRegistry.class);
	}

	@Bean
	public ResourceLoader resourceLoader() {
		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		when(resourceLoader.getResource(anyString())).thenReturn(mock(Resource.class));
		return resourceLoader;
	}

	@Bean
	TaskLauncher taskLauncher() {
		return 	mock(TaskLauncher.class);

	}

	@Bean
	ApplicationConfigurationMetadataResolver metadataResolver() {
		return mock(ApplicationConfigurationMetadataResolver.class);
	}

	@Bean
	public DataflowRdbmsInitializer definitionRepositoryInitializer(DataSource dataSource) {
		DataflowRdbmsInitializer definitionRepositoryInitializer = new DataflowRdbmsInitializer(featuresProperties());
		definitionRepositoryInitializer.setDataSource(dataSource);
		return definitionRepositoryInitializer;
	}

	@Bean
	public FeaturesProperties featuresProperties() {
		return new FeaturesProperties();
	}

	@Bean
	public DataSourceTransactionManager transactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	public DefaultTaskService defaultTaskService(TaskDefinitionRepository taskDefinitionRepository, TaskExplorer taskExplorer,
			TaskRepository taskExecutionRepository, AppRegistry appRegistry,
			ResourceLoader resourceLoader, TaskLauncher taskLauncher,
			ApplicationConfigurationMetadataResolver metadataResolver, CommonApplicationProperties commonApplicationProperties) {
		return new DefaultTaskService(dataSourceProperties, taskDefinitionRepository, taskExplorer,
				taskExecutionRepository, appRegistry, resourceLoader, taskLauncher, metadataResolver,
				new TaskConfigurationProperties(), new InMemoryDeploymentIdRepository(), null, commonApplicationProperties);

	}
}
