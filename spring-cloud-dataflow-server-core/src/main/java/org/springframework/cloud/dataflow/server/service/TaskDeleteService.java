/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service;

import java.util.Set;

import org.springframework.cloud.dataflow.server.controller.support.TaskExecutionControllerDeleteAction;

/**
 * Provides task deletion services.
 *
 * @author Daniel Serleg
 * @author Gunnar Hillert
 */
public interface TaskDeleteService {
	/**
	 * Cleanup the resources that resulted from running the task with the given execution id.
	 *
	 * @param id the execution id
	 */
	void cleanupExecution(long id);

	/**
	 *
	 * @param actionsAsSet
	 * @param ids
	 */
	void cleanupExecutions(Set<TaskExecutionControllerDeleteAction> actionsAsSet, Set<Long> ids);

	/**
	 * Delete one or more Task executions.
	 *
	 * @param id the execution id
	 */
	void deleteOneOrMoreTaskExecutions(Set<Long> ids);

	/**
	 * Destroy the task definition. If it is a Composed Task then the task definitions
	 * required for a ComposedTaskRunner task are also destroyed.
	 *
	 * @param name The name of the task.
	 */
	void deleteTaskDefinition(String name);

	/**
	 * Destroy all task definitions. If it is a Composed Task then the task definitions
	 * required for a ComposedTaskRunner tasks are also destroyed.
	 */
	void deleteAll();

}
