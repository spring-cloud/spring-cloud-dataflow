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

package org.springframework.cloud.dataflow.server.job;

import javax.sql.DataSource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.util.Assert;

/**
 * Factory bean to create a Task Explorer.
 *
 * @author Glenn Renfro
 */
public class TaskExplorerFactoryBean implements FactoryBean<TaskExplorer> {

	private final DataSource dataSource;
	private TaskExplorer taskExplorer;
	private final String tablePrefix;
	public TaskExplorerFactoryBean(DataSource dataSource, String tablePrefix) {
		Assert.notNull(dataSource, "dataSource must not be null");
		this.dataSource = dataSource;
		this.tablePrefix = tablePrefix;
	}

	@Override
	public TaskExplorer getObject() throws Exception {
		if (taskExplorer == null) {
			taskExplorer = new SimpleTaskExplorer(new TaskExecutionDaoFactoryBean(dataSource, tablePrefix));
		}
		return taskExplorer;
	}

	@Override
	public Class<?> getObjectType() {
		return TaskExplorer.class;
	}

}
