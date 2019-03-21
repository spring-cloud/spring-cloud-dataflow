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

package org.springframework.cloud.dataflow.server.configuration;

import javax.sql.DataSource;

import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.repository.RdbmsTaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.support.DataflowRdbmsInitializer;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@EnableSpringDataWebSupport
public class TaskDependencies {

	@Bean
	public TaskRepositoryInitializer taskExecutionRepository(DataSource dataSource) {
		TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();
		taskRepositoryInitializer.setDataSource(dataSource);
		return taskRepositoryInitializer;
	}

	@Bean
	public FeaturesProperties featuresProperties() {
		return new FeaturesProperties();
	}

	@Bean
	public TaskExplorer taskExplorer(DataSource dataSource) {
		return new SimpleTaskExplorer(new TaskExecutionDaoFactoryBean(dataSource));
	}

	@Bean
	public TaskDefinitionRepository taskDefinitionRepository(DataSource dataSource) throws Exception {
		return new RdbmsTaskDefinitionRepository(dataSource);
	}

	@Bean
	public DataflowRdbmsInitializer definitionRepositoryInitializer(DataSource dataSource) {
		DataflowRdbmsInitializer definitionRepositoryInitializer = new DataflowRdbmsInitializer(featuresProperties());
		definitionRepositoryInitializer.setDataSource(dataSource);
		return definitionRepositoryInitializer;
	}
}
