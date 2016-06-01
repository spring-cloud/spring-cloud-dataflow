/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.repository;

import javax.sql.DataSource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

/**
 * A {@link FactoryBean} implementation that creates the appropriate
 * {@link TaskDefinitionRepositoryFactoryBean} based on the provided information.
 *
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public class TaskDefinitionRepositoryFactoryBean implements FactoryBean<TaskDefinitionRepository> {

	public static final String DEFAULT_TABLE_PREFIX = "TASK_";

	private DataSource dataSource;

	private TaskDefinitionRepository taskDefinitionRepository = null;

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	/**
	 * Default constructor will result in a Map based InMemoryTaskDefinitionRepository.  <b>This is only
	 * intended for testing purposes.</b>
	 */
	public TaskDefinitionRepositoryFactoryBean() {
	}

	/**
	 * {@link DataSource} to be used.
	 *
	 * @param dataSource {@link DataSource} to be used.
	 */
	public TaskDefinitionRepositoryFactoryBean(DataSource dataSource) {
		Assert.notNull(dataSource, "A DataSource is required");
		this.dataSource = dataSource;
	}

	@Override
	public TaskDefinitionRepository getObject() throws Exception {
		if(this.taskDefinitionRepository == null) {
			if (this.dataSource != null) {
				buildTaskDefinitionRepository(this.dataSource);
			}
			else {
				this.taskDefinitionRepository = new InMemoryTaskDefinitionRepository();
			}
		}

		return this.taskDefinitionRepository;
	}

	@Override
	public Class<?> getObjectType() {
		return TaskDefinitionRepositoryFactoryBean.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private void buildTaskDefinitionRepository(DataSource dataSource) {
		this.taskDefinitionRepository = new RdbmsTaskDefinitionRepository(dataSource);
	}
}
