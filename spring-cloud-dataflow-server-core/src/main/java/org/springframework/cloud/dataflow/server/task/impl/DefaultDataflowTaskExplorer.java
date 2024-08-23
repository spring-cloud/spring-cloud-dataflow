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
package org.springframework.cloud.dataflow.server.task.impl;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.ThinTaskExecution;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorer;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExecutionQueryDao;
import org.springframework.cloud.dataflow.server.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.server.task.TaskDeploymentReader;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDeployment;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * Implements DataflowTaskExplorer. This class will be responsible for retrieving task execution data for all schema targets.
 *
 * @author Corneil du Plessis
 */
public class DefaultDataflowTaskExplorer implements DataflowTaskExplorer {
	private final static Logger logger = LoggerFactory.getLogger(DefaultDataflowTaskExplorer.class);

	private final TaskExplorer taskExplorer;

	private final DataflowTaskExecutionQueryDao taskExecutionQueryDao;

	private final TaskDefinitionReader taskDefinitionReader;

	private final TaskDeploymentReader taskDeploymentReader;

	public DefaultDataflowTaskExplorer(
			DataSource dataSource,
			DataflowTaskExecutionQueryDao taskExecutionQueryDao,
			TaskDefinitionReader taskDefinitionReader,
			TaskDeploymentReader taskDeploymentReader
	) {
		this.taskExecutionQueryDao = taskExecutionQueryDao;
		this.taskDefinitionReader = taskDefinitionReader;
		this.taskDeploymentReader = taskDeploymentReader;
    	this.taskExplorer = new SimpleTaskExplorer(new TaskExecutionDaoFactoryBean(dataSource, "TASK_"));
	}

	@Override
	public TaskExecution getTaskExecution(long executionId) {
		return taskExplorer.getTaskExecution(executionId);
	}

	@Override
	public TaskExecution getTaskExecutionByExternalExecutionId(String externalExecutionId, String platform) {
		TaskDeployment deployment = taskDeploymentReader.getDeployment(externalExecutionId, platform);
		if (deployment != null) {
			return this.taskExecutionQueryDao.geTaskExecutionByExecutionId(externalExecutionId, deployment.getTaskDefinitionName());
		}
		return null;
	}

	@Override
	public List<TaskExecution> findChildTaskExecutions(long executionId) {
		return this.taskExecutionQueryDao.findChildTaskExecutions(executionId);
	}

	@Override
	public List<TaskExecution> findChildTaskExecutions(Collection<Long> parentIds) {
		return this.taskExecutionQueryDao.findChildTaskExecutions(parentIds);
	}

	@Override
	public Page<TaskExecution> findRunningTaskExecutions(String taskName, Pageable pageable) {
		Assert.notNull(taskExplorer, "Expected TaskExplorer");
		Page<TaskExecution> executions = taskExplorer.findRunningTaskExecutions(taskName, pageable);
		List<TaskExecution> taskExecutions = executions.getContent();
		return new PageImpl<>(taskExecutions, executions.getPageable(), executions.getTotalElements());
	}

	@Override
	public List<String> getTaskNames() {
		return taskExplorer.getTaskNames();
	}

	@Override
	public long getTaskExecutionCountByTaskName(String taskName) {
		return taskExplorer.getTaskExecutionCountByTaskName(taskName);
	}

	@Override
	public long getTaskExecutionCount() {
		return taskExplorer.getTaskExecutionCount();
	}

	@Override
	public long getRunningTaskExecutionCount() {
		return taskExplorer.getRunningTaskExecutionCount();
	}

	@Override
	public List<TaskExecution> findTaskExecutions(String taskName, boolean completed) {
		return this.taskExecutionQueryDao.findTaskExecutions(taskName, completed);
	}

	@Override
	public List<TaskExecution> findTaskExecutionsBeforeEndTime(String taskName, Date endTime) {
		return this.taskExecutionQueryDao.findTaskExecutionsBeforeEndTime(taskName, endTime);
	}

	@Override
	public Page<TaskExecution> findTaskExecutionsByName(String taskName, Pageable pageable) {

		Assert.notNull(taskExplorer, "Expected TaskExplorer");
		Page<TaskExecution> executions = taskExplorer.findTaskExecutionsByName(taskName, pageable);
		List<TaskExecution> taskExecutions = executions.getContent();
		return new PageImpl<>(taskExecutions, executions.getPageable(), executions.getTotalElements());
	}

	private String getPlatformName(String taskName) {
		String platformName = null;
		TaskDefinition taskDefinition = taskDefinitionReader.findTaskDefinition(taskName);
		if (taskDefinition != null) {
			TaskDeployment taskDeployment = taskDeploymentReader.findByDefinitionName(taskDefinition.getName());
			platformName = taskDeployment != null ? taskDeployment.getPlatformName() : null;
		} else {
			logger.warn("TaskDefinition not found for " + taskName);
		}
		return platformName;
	}

	@Override
	public Page<TaskExecution> findAll(Pageable pageable) {
		return taskExplorer.findAll(pageable);
	}

	@Override
	public Long getTaskExecutionIdByJobExecutionId(long jobExecutionId) {
		Assert.notNull(taskExplorer, "Expected TaskExplorer");
		return taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecutionId);
	}

	@Override
	public Set<Long> getJobExecutionIdsByTaskExecutionId(long taskExecutionId) {
		return taskExplorer.getJobExecutionIdsByTaskExecutionId(taskExecutionId);
	}

	@Override
	public List<TaskExecution> getLatestTaskExecutionsByTaskNames(String... taskNames) {
		List<TaskExecution> result = new ArrayList<>();
		for (String taskName : taskNames) {
			String platformName = getPlatformName(taskName);
			Assert.notNull(taskExplorer, "Expected TaskExplorer");
			List<TaskExecution> taskExecutions = taskExplorer.getLatestTaskExecutionsByTaskNames(taskNames);
			result.addAll(taskExecutions);
		}
		return result;
	}

	@Override
	public TaskExecution getLatestTaskExecutionForTaskName(String taskName) {
		Assert.notNull(taskExplorer, "Expected TaskExplorer");
		return taskExplorer.getLatestTaskExecutionForTaskName(taskName);
	}

	@Override
	public void populateCtrStatus(Collection<ThinTaskExecution> taskExecutions) {
		this.taskExecutionQueryDao.populateCtrStatus(taskExecutions);
	}
}
