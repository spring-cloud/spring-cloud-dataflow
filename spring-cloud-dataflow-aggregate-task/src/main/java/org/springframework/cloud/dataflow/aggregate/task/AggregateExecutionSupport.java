/*
 * Copyright 2023 the original author or authors.
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

import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.task.repository.TaskExecution;

/**
 * Allows users to retrieve Task execution and SchemaVersion information from either {@link TaskExecution} as well as
 * Task Name.
 * @author Corneil du Plessis
 */
public interface AggregateExecutionSupport {

	/**
	 * Retrieves the {@link AggregateTaskExecution} for the task execution and {@link TaskDefinitionReader} provided.
	 * @param execution A {@link TaskExecution} that contains the TaskName that will be used to find the {@link AggregateTaskExecution}.
	 * @param taskDefinitionReader {@link TaskDefinitionReader} that will be used to find the {@link SchemaVersionTarget} for the task execution.
	 * @return The {@link AggregateTaskExecution} containing the {@link SchemaVersionTarget} for the TaskExecution.
	 */
	AggregateTaskExecution from(TaskExecution execution, TaskDefinitionReader taskDefinitionReader, TaskDeploymentReader taskDeploymentReader);

	/**
	 * Retrieves the {@link SchemaVersionTarget} for the task name.
	 * @param taskName The name of the {@link org.springframework.cloud.dataflow.core.TaskDefinition} from which the {@link SchemaVersionTarget} will be retreived.
	 * @param taskDefinitionReader {@link TaskDefinitionReader} that will be used to find the {@link SchemaVersionTarget}
	 * @return The {@link SchemaVersionTarget} for the taskName specified.
	 */
	SchemaVersionTarget findSchemaVersionTarget(String taskName, TaskDefinitionReader taskDefinitionReader);
	SchemaVersionTarget findSchemaVersionTarget(String taskName, TaskDefinition taskDefinition);

	/**
	 * Retrieve the {@link AppRegistration} for the registeredName.
	 */
	AppRegistration findTaskAppRegistration(String registeredName);

	/**
	 * Return the {@link AggregateTaskExecution} for the {@link TaskExecution} and Schema Target name specified.
	 */
	AggregateTaskExecution from(TaskExecution execution, String schemaTarget, String platformName);
}
