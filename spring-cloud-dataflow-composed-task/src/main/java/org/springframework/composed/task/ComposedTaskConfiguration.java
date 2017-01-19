/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.composed.task;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Glenn Renfro
 */
@Configuration
@EnableConfigurationProperties(ComposedTaskProperties.class)
public class ComposedTaskConfiguration {

	@Autowired
	private JobBuilderFactory jobs;

	@Autowired
	private StepBuilderFactory steps;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ComposedTaskProperties properties;

	@Bean
	public Job job() throws Exception {
		return getJob();
	}

	@Bean
	public TaskRepository taskRepository() {
		return new SimpleTaskRepository(new TaskExecutionDaoFactoryBean(dataSource));
	}

	@Bean
	public TaskExplorer taskExplorer() {
		return new SimpleTaskExplorer(new TaskExecutionDaoFactoryBean(dataSource));
	}

	@Bean
	public TaskOperations taskOperations() {
		TaskOperations taskOperations = null;
		try {
			DataFlowTemplate dataFlowTemplate = new DataFlowTemplate(new URI(properties.getDataFlowUri()));
			taskOperations = dataFlowTemplate.taskOperations();
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
		return taskOperations;
	}

	private StepExecutionListener composedTaskStepExecutionListener() {
		return new ComposedTaskStepExecutionListener(taskExplorer(), properties);
	}
	/**
	 * For now this method is will only accept a a single task name.
	 * Need to create the derived graph here by parsing the DSL.
	 * @return Returns the Job to be executed.
	 */
	private Job getJob() {
		FlowBuilder<Flow> flowBuilder = new FlowBuilder<Flow>(UUID.randomUUID().toString())
				.start(getStep(properties.getComposedTaskDSL(),null,null));
		return this.jobs.get(
				properties.getJobNamePrefix() + UUID.randomUUID().toString())
				.start(flowBuilder.end()).end().build();
	}

	private Step getStep(String taskName, Map<String, String> properties, List<String> arguments ) {
		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(new Date());
		taskExecution.setArguments(new ArrayList());
		taskExecution = taskRepository().createTaskExecution(taskExecution);
		TaskLauncherTasklet taskLauncherTasklet = new TaskLauncherTasklet(
				String.valueOf(taskExecution.getExecutionId()),
				taskOperations(), taskName, properties, arguments);

		return this.steps.get(UUID.randomUUID().toString())
				.tasklet(taskLauncherTasklet)
				.listener(composedTaskStepExecutionListener())
				.build();
	}

}
