/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;

/**
 * FactoryBean that creates a Spring Batch Step that executes a configured
 * TaskLaunchTasklet.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
public class ComposedTaskRunnerStepFactory implements FactoryBean<Step> {

	private ComposedTaskProperties composedTaskProperties;

	private String taskName;

	private Map<String, String> taskSpecificProps = new HashMap<>();

	private List<String> arguments = new ArrayList<>();

	@Autowired
	private StepBuilderFactory steps;

	@Autowired
	private StepExecutionListener composedTaskStepExecutionListener;

	@Autowired
	private TaskOperations taskOperations;

	@Autowired
	private TaskConfigurer taskConfigurer;

	@Autowired
	private TaskProperties taskProperties;

	public ComposedTaskRunnerStepFactory(
			ComposedTaskProperties composedTaskProperties, String taskName) {
		Assert.notNull(composedTaskProperties,
				"composedTaskProperties must not be null");
		Assert.hasText(taskName, "taskName must not be empty nor null");

		this.composedTaskProperties = composedTaskProperties;
		this.taskName = taskName;
	}

	public void setTaskSpecificProps(Map<String, String> taskSpecificProps) {
		if(taskSpecificProps != null) {
			this.taskSpecificProps = taskSpecificProps;
		}
	}

	public void setArguments(List<String> arguments) {
		if(arguments != null) {
			this.arguments = arguments;
		}
	}

	@Override
	public Step getObject() throws Exception {
		org.springframework.cloud.dataflow.composedtaskrunner.TaskLauncherTasklet taskLauncherTasklet = new org.springframework.cloud.dataflow.composedtaskrunner.TaskLauncherTasklet(
				this.taskOperations, taskConfigurer.getTaskExplorer(),
				this.composedTaskProperties, this.taskName, taskProperties);

		taskLauncherTasklet.setArguments(this.arguments);
		taskLauncherTasklet.setProperties(this.taskSpecificProps);

		String stepName = this.taskName;

		return this.steps.get(stepName)
				.tasklet(taskLauncherTasklet)
				.transactionAttribute(getTransactionAttribute())
				.listener(this.composedTaskStepExecutionListener)
				.build();
	}

	/**
	 * Using the default transaction attribute for the job will cause the
	 * TaskLauncher not to see the latest state in the database but rather
	 * what is in its transaction.  By setting isolation to READ_COMMITTED
	 * The task launcher can see latest state of the db.  Since the changes
	 * to the task execution are done by the tasks.

	 * @return DefaultTransactionAttribute with isolation set to READ_COMMITTED.
	 */
	private TransactionAttribute getTransactionAttribute() {
		DefaultTransactionAttribute defaultTransactionAttribute =
				new DefaultTransactionAttribute();
		defaultTransactionAttribute.setIsolationLevel(
				Isolation.READ_COMMITTED.value());
		return defaultTransactionAttribute;
	}

	@Override
	public Class<?> getObjectType() {
		return Step.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
