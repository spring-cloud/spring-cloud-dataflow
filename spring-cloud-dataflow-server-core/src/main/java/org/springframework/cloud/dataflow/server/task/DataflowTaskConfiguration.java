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
package org.springframework.cloud.dataflow.server.task;

import javax.sql.DataSource;

import org.springframework.cloud.dataflow.server.task.impl.DefaultDataflowTaskExplorer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * Configuration for aggregate task related components.
 *
 * @author Corneil du Plessis
 */
@Configuration
public class DataflowTaskConfiguration {

	@Bean
	public DataflowTaskExplorer aggregateTaskExplorer(
			DataSource dataSource,
			DataflowTaskExecutionQueryDao taskExecutionQueryDao,
			TaskDefinitionReader taskDefinitionReader,
			TaskDeploymentReader taskDeploymentReader
	) {
		Assert.notNull(dataSource, "dataSource required");
		Assert.notNull(taskExecutionQueryDao, "taskExecutionQueryDao required");
		Assert.notNull(taskDefinitionReader, "taskDefinitionReader required");
		Assert.notNull(taskDeploymentReader, "taskDeploymentReader required");
		return new DefaultDataflowTaskExplorer(dataSource,
				taskExecutionQueryDao,
				taskDefinitionReader,
				taskDeploymentReader);
	}

}
