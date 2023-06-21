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
package org.springframework.cloud.dataflow.aggregate.task.impl;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.aggregate.task.AggregateTaskExplorer;
import org.springframework.cloud.dataflow.aggregate.task.DataflowTaskExecutionQueryDao;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * Implements CompositeTaskExplorer. This class will be responsible for retrieving task execution data for all schema targets.
 *
 * @author Corneil du Plessis
 */
public class DefaultAggregateTaskExplorer implements AggregateTaskExplorer {
	private final static Logger logger = LoggerFactory.getLogger(DefaultAggregateTaskExplorer.class);
	private final Map<String, TaskExplorer> taskExplorers;

	private final AggregateExecutionSupport aggregateExecutionSupport;

	private final DataflowTaskExecutionQueryDao taskExecutionQueryDao;

	private final TaskDefinitionReader taskDefinitionReader;

	public DefaultAggregateTaskExplorer(
			DataSource dataSource,
			DataflowTaskExecutionQueryDao taskExecutionQueryDao,
			SchemaService schemaService,
			AggregateExecutionSupport aggregateExecutionSupport,
			TaskDefinitionReader taskDefinitionReader) {
		this.taskExecutionQueryDao = taskExecutionQueryDao;
		this.aggregateExecutionSupport = aggregateExecutionSupport;
		this.taskDefinitionReader = taskDefinitionReader;
		Map<String, TaskExplorer> result = new HashMap<>();
		for (SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			TaskExplorer explorer = new SimpleTaskExplorer(new TaskExecutionDaoFactoryBean(dataSource, target.getTaskPrefix()));
			result.put(target.getName(), explorer);
		}
		taskExplorers = Collections.unmodifiableMap(result);
	}

	@Override
	public AggregateTaskExecution getTaskExecution(long executionId, String schemaTarget) {
		if(schemaTarget == null) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		TaskExplorer taskExplorer = taskExplorers.get(schemaTarget);
		Assert.notNull(taskExplorer, "Expected taskExplorer for " + schemaTarget);
		TaskExecution taskExecution = taskExplorer.getTaskExecution(executionId);
		return aggregateExecutionSupport.from(taskExecution, schemaTarget);
	}

	@Override
	public Page<AggregateTaskExecution> findRunningTaskExecutions(String taskName, Pageable pageable) {
		SchemaVersionTarget target = aggregateExecutionSupport.findSchemaVersionTarget(taskName, taskDefinitionReader);
		Assert.notNull(target, "Expected to find SchemaVersionTarget for " + taskName);
		TaskExplorer taskExplorer = taskExplorers.get(target.getName());
		Assert.notNull(taskExplorer, "Expected TaskExplorer for " + target.getName());
		Page<TaskExecution> executions = taskExplorer.findRunningTaskExecutions(taskName, pageable);
		List<AggregateTaskExecution> taskExecutions = executions.getContent()
				.stream()
				.map(execution -> aggregateExecutionSupport.from(execution, target.getName()))
				.collect(Collectors.toList());
		return new PageImpl<>(taskExecutions, executions.getPageable(), executions.getTotalElements());
	}

	@Override
	public List<String> getTaskNames() {
		List<String> result = new ArrayList<>();
		for (TaskExplorer explorer : taskExplorers.values()) {
			result.addAll(explorer.getTaskNames());
		}
		return result;
	}

	@Override
	public long getTaskExecutionCountByTaskName(String taskName) {
		long result = 0;
		for (TaskExplorer explorer : taskExplorers.values()) {
			result += explorer.getTaskExecutionCountByTaskName(taskName);
		}
		return result;
	}

	@Override
	public long getTaskExecutionCount() {
		long result = 0;
		for (TaskExplorer explorer : taskExplorers.values()) {
			result += explorer.getTaskExecutionCount();
		}
		return result;
	}

	@Override
	public long getRunningTaskExecutionCount() {
		long result = 0;
		for (TaskExplorer explorer : taskExplorers.values()) {
			result += explorer.getRunningTaskExecutionCount();
		}
		return result;
	}

	@Override
	public Page<AggregateTaskExecution> findTaskExecutionsByName(String taskName, Pageable pageable) {
		SchemaVersionTarget target = aggregateExecutionSupport.findSchemaVersionTarget(taskName, taskDefinitionReader);
		Assert.notNull(target, "Expected to find SchemaVersionTarget for " + taskName);
		TaskExplorer taskExplorer = taskExplorers.get(target.getName());
		Assert.notNull(taskExplorer, "Expected TaskExplorer for " + target.getName());
		Page<TaskExecution> executions = taskExplorer.findTaskExecutionsByName(taskName, pageable);
		List<AggregateTaskExecution> taskExecutions = executions.getContent()
				.stream()
				.map(execution -> aggregateExecutionSupport.from(execution, target.getName()))
				.collect(Collectors.toList());
		return new PageImpl<>(taskExecutions, executions.getPageable(), executions.getTotalElements());
	}

	@Override
	public Page<AggregateTaskExecution> findAll(Pageable pageable) {
		return taskExecutionQueryDao.findAll(pageable);
	}

	@Override
	public Long getTaskExecutionIdByJobExecutionId(long jobExecutionId, String schemaTarget) {
		if(schemaTarget == null) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		TaskExplorer taskExplorer = taskExplorers.get(schemaTarget);
		Assert.notNull(taskExplorer, "Expected TaskExplorer for " + schemaTarget);
		return taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecutionId);
	}

	@Override
	public Set<Long> getJobExecutionIdsByTaskExecutionId(long taskExecutionId, String schemaTarget) {
		if(schemaTarget == null) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		TaskExplorer taskExplorer = taskExplorers.get(schemaTarget);
		Assert.notNull(taskExplorer, "Expected TaskExplorer for " + schemaTarget);
		return taskExplorer.getJobExecutionIdsByTaskExecutionId(taskExecutionId);
	}

	@Override
	public List<AggregateTaskExecution> getLatestTaskExecutionsByTaskNames(String... taskNames) {
		List<AggregateTaskExecution> result = new ArrayList<>();
		for (String taskName : taskNames) {
			SchemaVersionTarget target = aggregateExecutionSupport.findSchemaVersionTarget(taskName, taskDefinitionReader);
			Assert.notNull(target, "Expected to find SchemaVersionTarget for " + taskName);
			TaskExplorer taskExplorer = taskExplorers.get(target.getName());
			Assert.notNull(taskExplorer, "Expected TaskExplorer for " + target.getName());
			List<AggregateTaskExecution> taskExecutions = taskExplorer.getLatestTaskExecutionsByTaskNames(taskNames)
					.stream()
					.map(execution -> aggregateExecutionSupport.from(execution, target.getName()))
					.collect(Collectors.toList());
			result.addAll(taskExecutions);
		}
		return result;
	}

	@Override
	public AggregateTaskExecution getLatestTaskExecutionForTaskName(String taskName) {
		SchemaVersionTarget target = aggregateExecutionSupport.findSchemaVersionTarget(taskName, taskDefinitionReader);
		Assert.notNull(target, "Expected to find SchemaVersionTarget for " + taskName);
		TaskExplorer taskExplorer = taskExplorers.get(target.getName());
		Assert.notNull(taskExplorer, "Expected TaskExplorer for " + target.getName());
		return aggregateExecutionSupport.from(taskExplorer.getLatestTaskExecutionForTaskName(taskName), target.getName());
	}
	@PostConstruct
	public void setup() {
		logger.info("created: org.springframework.cloud.dataflow.aggregate.task.impl.DefaultAggregateTaskExplorer");
	}
}
