/*
 * Copyright 2023-2024 the original author or authors.
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

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.core.database.support.MultiSchemaIncrementerFactory;
import org.springframework.cloud.dataflow.server.batch.AllInOneExecutionContextSerializer;
import org.springframework.cloud.dataflow.server.batch.JdbcSearchableJobExecutionDao;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.springframework.cloud.dataflow.server.batch.SimpleJobServiceFactoryBean;
import org.springframework.cloud.dataflow.server.repository.DataflowJobExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.DefaultTaskDefinitionReader;
import org.springframework.cloud.dataflow.server.repository.DefaultTaskDeploymentReader;
import org.springframework.cloud.dataflow.server.repository.JdbcDataflowJobExecutionDao;
import org.springframework.cloud.dataflow.server.repository.JdbcDataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.JdbcDataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExecutionQueryDao;
import org.springframework.cloud.dataflow.server.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.server.task.TaskDeploymentReader;
import org.springframework.cloud.dataflow.server.task.impl.DefaultDataFlowTaskExecutionQueryDao;
import org.springframework.cloud.task.batch.listener.support.JdbcTaskBatchDao;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.cloud.task.repository.support.DatabaseType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration for DAO Containers use for multiple schema targets.
 *
 * @author Corneil du Plessis
 */
@Configuration
public class DataFlowTaskConfiguration {

	@Bean
	public DataflowJobExecutionDao dataflowJobExecutionDao(DataSource dataSource) {
			return new JdbcDataflowJobExecutionDao(dataSource, "BATCH_");
	}

	@Bean
	public DataflowTaskExecutionDao dataflowTaskExecutionDao(DataSource dataSource,
																	  TaskProperties taskProperties) {
			TaskProperties properties = new TaskProperties();
			BeanUtils.copyProperties(taskProperties, properties);
			properties.setTablePrefix("TASK_");
			return new JdbcDataflowTaskExecutionDao(dataSource, properties);
	}

	@Bean
	public DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao(DataSource dataSource)
		throws SQLException {
		DataFieldMaxValueIncrementerFactory incrementerFactory = new MultiSchemaIncrementerFactory(dataSource);
		String databaseType;
		try {
			databaseType = DatabaseType.fromMetaData(dataSource).name();
		} catch (MetaDataAccessException e) {
			throw new IllegalStateException(e);
		}
			DataflowTaskExecutionMetadataDao dao = new JdbcDataflowTaskExecutionMetadataDao(
					dataSource,
					incrementerFactory.getIncrementer(databaseType, "TASK_EXECUTION_METADATA_SEQ"),
				"TASK_");
		return dao;
	}

	@Bean
	public TaskExecutionDao taskExecutionDao(DataSource dataSource) throws Exception{
		DataFieldMaxValueIncrementerFactory incrementerFactory = new MultiSchemaIncrementerFactory(dataSource);
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(dataSource);
		String databaseType;
		try {
			databaseType = DatabaseType.fromMetaData(dataSource).name();
		}
		catch (MetaDataAccessException e) {
			throw new IllegalStateException(e);
		}
		dao.setTaskIncrementer(incrementerFactory.getIncrementer(databaseType, "TASK_SEQ"));
		return dao;
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
	public JdbcSearchableJobExecutionDao jobExecutionDao(DataSource dataSource) {
		JdbcSearchableJobExecutionDao jdbcSearchableJobExecutionDao = new JdbcSearchableJobExecutionDao();
		jdbcSearchableJobExecutionDao.setDataSource(dataSource);
		try {
			jdbcSearchableJobExecutionDao.afterPropertiesSet();
		}
		catch (Throwable x) {
			throw new RuntimeException("Exception creating JdbcSearchableJobExecutionDao", x);
		}
		return jdbcSearchableJobExecutionDao;
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
	public JdbcTaskBatchDao taskBatchDao(DataSource dataSource) {
		return new JdbcTaskBatchDao(dataSource);
	}

	@Bean
	@ConditionalOnMissingBean
	public DataflowTaskExecutionQueryDao taskExecutionQueryDao(DataSource dataSource) {
		return new DefaultDataFlowTaskExecutionQueryDao(dataSource);
	}
}
