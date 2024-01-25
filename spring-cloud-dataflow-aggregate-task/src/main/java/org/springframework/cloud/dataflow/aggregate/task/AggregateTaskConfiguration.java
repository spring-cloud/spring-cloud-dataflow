/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.cloud.dataflow.aggregate.task;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.aggregate.task.impl.AggregateDataFlowTaskExecutionQueryDao;
import org.springframework.cloud.dataflow.aggregate.task.impl.DefaultAggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.impl.DefaultAggregateTaskExplorer;
import org.springframework.cloud.dataflow.aggregate.task.impl.DefaultTaskRepositoryContainer;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.schema.service.SchemaServiceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.Assert;

/**
 * Configuration for aggregate task related components.
 *
 * @author Corneil du Plessis
 */
@Configuration
@Import(SchemaServiceConfiguration.class)
public class AggregateTaskConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(AggregateTaskConfiguration.class);


	@Bean
	public DataflowTaskExecutionQueryDao dataflowTaskExecutionQueryDao(
			DataSource dataSource,
			SchemaService schemaService
	) {
		return new AggregateDataFlowTaskExecutionQueryDao(dataSource, schemaService);
	}

	@Bean
	public AggregateExecutionSupport aggregateExecutionSupport(
			AppRegistryService registryService,
			SchemaService schemaService
	) {
		return new DefaultAggregateExecutionSupport(registryService, schemaService);
	}

	@Bean
	public TaskRepositoryContainer taskRepositoryContainer(
			DataSource dataSource,
			SchemaService schemaService
	) {
		return new DefaultTaskRepositoryContainer(dataSource, schemaService);
	}

	@Bean
	public AggregateTaskExplorer aggregateTaskExplorer(
			DataSource dataSource,
			DataflowTaskExecutionQueryDao taskExecutionQueryDao,
			SchemaService schemaService,
			AggregateExecutionSupport aggregateExecutionSupport,
			TaskDefinitionReader taskDefinitionReader,
			TaskDeploymentReader taskDeploymentReader
	) {
		Assert.notNull(dataSource, "dataSource required");
		Assert.notNull(taskExecutionQueryDao, "taskExecutionQueryDao required");
		Assert.notNull(schemaService, "schemaService required");
		Assert.notNull(aggregateExecutionSupport, "aggregateExecutionSupport required");
		Assert.notNull(taskDefinitionReader, "taskDefinitionReader required");
		Assert.notNull(taskDeploymentReader, "taskDeploymentReader required");
		return new DefaultAggregateTaskExplorer(dataSource,
				taskExecutionQueryDao,
				schemaService,
				aggregateExecutionSupport,
				taskDefinitionReader,
				taskDeploymentReader);
	}

}
