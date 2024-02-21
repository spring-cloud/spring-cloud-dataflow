/*
 * Copyright 2023-2023 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.aggregate.task.TaskDeploymentReader;
import org.springframework.cloud.dataflow.core.database.support.MultiSchemaIncrementerFactory;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.batch.AllInOneExecutionContextSerializer;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.springframework.cloud.dataflow.server.batch.SimpleJobServiceFactoryBean;
import org.springframework.cloud.dataflow.server.repository.AggregateJobQueryDao;
import org.springframework.cloud.dataflow.server.repository.DataflowJobExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowJobExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDaoContainer;
import org.springframework.cloud.dataflow.server.repository.DefaultTaskDefinitionReader;
import org.springframework.cloud.dataflow.server.repository.DefaultTaskDeploymentReader;
import org.springframework.cloud.dataflow.server.repository.JdbcAggregateJobQueryDao;
import org.springframework.cloud.dataflow.server.repository.JdbcDataflowJobExecutionDao;
import org.springframework.cloud.dataflow.server.repository.JdbcDataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.JdbcDataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.JobExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.repository.TaskBatchDaoContainer;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.repository.support.SchemaUtilities;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.support.DatabaseType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.SQLException;

/**
 * Configuration for DAO Containers use for multiple schema targets.
 *
 * @author Corneil du Plessis
 */
@Configuration
public class AggregateDataFlowTaskConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(AggregateDataFlowTaskConfiguration.class);

	@Bean
	public DataflowJobExecutionDaoContainer dataflowJobExecutionDao(DataSource dataSource, SchemaService schemaService) {
		DataflowJobExecutionDaoContainer result = new DataflowJobExecutionDaoContainer();
		for (SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			DataflowJobExecutionDao dao = new JdbcDataflowJobExecutionDao(dataSource, target.getBatchPrefix());
			result.add(target.getName(), dao);
		}
		return result;
	}

	@Bean
	public DataflowTaskExecutionDaoContainer dataflowTaskExecutionDao(DataSource dataSource, SchemaService schemaService,
																	  TaskProperties taskProperties) {
		DataflowTaskExecutionDaoContainer result = new DataflowTaskExecutionDaoContainer();
		for (SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			TaskProperties properties = new TaskProperties();
			BeanUtils.copyProperties(taskProperties, properties);
			properties.setTablePrefix(target.getTaskPrefix());
			DataflowTaskExecutionDao dao = new JdbcDataflowTaskExecutionDao(dataSource, properties);
			result.add(target.getName(), dao);
		}
		return result;
	}

	@Bean
	public DataflowTaskExecutionMetadataDaoContainer dataflowTaskExecutionMetadataDao(DataSource dataSource,
																					  SchemaService schemaService)
		throws SQLException {
		DataFieldMaxValueIncrementerFactory incrementerFactory = new MultiSchemaIncrementerFactory(dataSource);
		String databaseType;
		try {
			databaseType = DatabaseType.fromMetaData(dataSource).name();
		} catch (MetaDataAccessException e) {
			throw new IllegalStateException(e);
		}
		DataflowTaskExecutionMetadataDaoContainer result = new DataflowTaskExecutionMetadataDaoContainer();
		for (SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			DataflowTaskExecutionMetadataDao dao = new JdbcDataflowTaskExecutionMetadataDao(
					dataSource,
					incrementerFactory.getIncrementer(databaseType,
							SchemaUtilities.getQuery("%PREFIX%EXECUTION_METADATA_SEQ", target.getTaskPrefix())
					),
					target.getTaskPrefix()
			);
			result.add(target.getName(), dao);
		}
		return result;
	}

	@Bean
	public TaskExecutionDaoContainer taskExecutionDaoContainer(DataSource dataSource, SchemaService schemaService) {
		return new TaskExecutionDaoContainer(dataSource, schemaService);
	}

	@Bean
	public JobRepository jobRepositoryContainer(DataSource dataSource,
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
	public JobService jobService(DataSource dataSource, PlatformTransactionManager platformTransactionManager,
								 JobRepository jobRepository, JobExplorer jobExplorer, Environment environment)
		throws Exception{
		SimpleJobServiceFactoryBean factoryBean = new SimpleJobServiceFactoryBean();
		factoryBean.setEnvironment(environment);
		factoryBean.setDataSource(dataSource);
		factoryBean.setTransactionManager(platformTransactionManager);
		factoryBean.setJobLauncher(new SimpleJobLauncher());
		factoryBean.setJobExplorer(jobExplorer);
		factoryBean.setJobRepository(jobRepository);
		factoryBean.setSerializer(new AllInOneExecutionContextSerializer());
		try {
			factoryBean.afterPropertiesSet();
		} catch (Throwable x) {
			throw new RuntimeException("Exception creating JobService", x);
		}
		return factoryBean.getObject();
	}

	@Bean
	public JobExecutionDaoContainer jobExecutionDaoContainer(DataSource dataSource, SchemaService schemaService) {
		return new JobExecutionDaoContainer(dataSource, schemaService);
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskDefinitionReader taskDefinitionReader(TaskDefinitionRepository repository) {
		return new DefaultTaskDefinitionReader(repository);
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskDeploymentReader taskDeploymentReader(TaskDeploymentRepository repository) {
		return new DefaultTaskDeploymentReader(repository);
	}

	@Bean
	public AggregateJobQueryDao aggregateJobQueryDao(DataSource dataSource, SchemaService schemaService,
			JobService jobService, Environment environment) throws Exception {
		return new JdbcAggregateJobQueryDao(dataSource, schemaService, jobService, environment);
	}

	@Bean
	public TaskBatchDaoContainer taskBatchDaoContainer(DataSource dataSource, SchemaService schemaService) {
		return new TaskBatchDaoContainer(dataSource, schemaService);
	}
}
