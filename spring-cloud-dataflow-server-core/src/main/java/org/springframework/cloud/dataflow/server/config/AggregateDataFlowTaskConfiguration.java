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

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.repository.AggregateJobQueryDao;
import org.springframework.cloud.dataflow.server.repository.DataflowJobExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowJobExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDaoContainer;
import org.springframework.cloud.dataflow.server.repository.DefaultTaskDefinitionReader;
import org.springframework.cloud.dataflow.server.repository.JdbcAggregateJobQueryDao;
import org.springframework.cloud.dataflow.server.repository.JdbcDataflowJobExecutionDao;
import org.springframework.cloud.dataflow.server.repository.JdbcDataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.JdbcDataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.JobExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.repository.JobRepositoryContainer;
import org.springframework.cloud.dataflow.server.repository.TaskBatchDaoContainer;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.repository.support.SchemaUtilities;
import org.springframework.cloud.dataflow.server.service.JobExplorerContainer;
import org.springframework.cloud.dataflow.server.service.JobServiceContainer;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.support.DatabaseType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

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
	public DataflowTaskExecutionDaoContainer dataflowTaskExecutionDao(DataSource dataSource, SchemaService schemaService, TaskProperties taskProperties) {
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
	public DataflowTaskExecutionMetadataDaoContainer dataflowTaskExecutionMetadataDao(DataSource dataSource, SchemaService schemaService) {
		DataFieldMaxValueIncrementerFactory incrementerFactory = new DefaultDataFieldMaxValueIncrementerFactory(dataSource);
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
	public JobRepositoryContainer jobRepositoryContainer(DataSource dataSource, PlatformTransactionManager platformTransactionManager, SchemaService schemaService) {
		return new JobRepositoryContainer(dataSource, platformTransactionManager, schemaService);
	}
	@Bean
	public JobExplorerContainer jobExplorerContainer(DataSource dataSource, SchemaService schemaService) {
		return new JobExplorerContainer(dataSource, schemaService);
	}
	@Bean
	public JobServiceContainer jobServiceContainer(DataSource dataSource, PlatformTransactionManager platformTransactionManager, SchemaService schemaService, JobRepositoryContainer jobRepositoryContainer, JobExplorerContainer jobExplorerContainer) {
		return new JobServiceContainer(dataSource, platformTransactionManager, schemaService, jobRepositoryContainer, jobExplorerContainer);
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
	public AggregateJobQueryDao aggregateJobQueryDao(DataSource dataSource, SchemaService schemaService) throws Exception {
		return new JdbcAggregateJobQueryDao(dataSource, schemaService);
	}
	@Bean
	public TaskBatchDaoContainer taskBatchDaoContainer(DataSource dataSource, SchemaService schemaService) {
		return new TaskBatchDaoContainer(dataSource, schemaService);
	}
	@PostConstruct
	public void setup() {
		logger.info("created: org.springframework.cloud.dataflow.server.config.AggregateDataFlowContainerConfiguration");
	}
}
