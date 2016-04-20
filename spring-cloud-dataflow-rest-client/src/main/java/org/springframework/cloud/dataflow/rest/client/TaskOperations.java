/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.hateoas.PagedResources;

/**
 * Interface defining operations available against tasks.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
public interface TaskOperations {

	/**
	 * List tasks known to the system.
	 */
	PagedResources<TaskDefinitionResource> list();

	/**
	 * Create a new task.
	 */
	TaskDefinitionResource create(String name, String definition);

	/**
	 * Launch an already created task.
	 */
	void launch(String name, Map<String, String> properties, List<String> arguments);

	/**
	 * Destroy an existing task.
	 */
	void destroy(String name);

	/**
	 * List task executions known to the system.
	 */
	PagedResources<TaskExecutionResource> executionList();

	/**
	 * List task executions known to the system filtered by task name.
	 * @param taskName of the executions.
	 */
	PagedResources<TaskExecutionResource> executionListByTaskName(String taskName);

	/**
	 * Return the {@link TaskExecutionResource} for the id specified.
	 *
	 * @param id identifier of the task execution
	 * @return {@link TaskExecutionResource}
	 */
	TaskExecutionResource view(long id);

}
