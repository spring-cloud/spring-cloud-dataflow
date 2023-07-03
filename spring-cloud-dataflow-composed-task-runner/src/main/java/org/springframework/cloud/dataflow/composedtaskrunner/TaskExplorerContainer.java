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

package org.springframework.cloud.dataflow.composedtaskrunner;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.task.repository.TaskExplorer;

/**
 * A container for the TaskExplorers for each Task by name.
 * @author Corneil du Plessis
 */
public class TaskExplorerContainer {
	private final static Logger logger = LoggerFactory.getLogger(TaskExplorerContainer.class);

	private final Map<String, TaskExplorer> taskExplorers;

	private final TaskExplorer defaultTaskExplorer;

	public TaskExplorerContainer(Map<String, TaskExplorer> taskExplorers, TaskExplorer defaultTaskExplorer) {
		this.taskExplorers = taskExplorers;
		this.defaultTaskExplorer = defaultTaskExplorer;

	}

	public TaskExplorer get(String name) {
		TaskExplorer result = taskExplorers.get(name);
		if (result == null) {
			result = taskExplorers.get(SchemaVersionTarget.defaultTarget().getName());
		}
		if(result == null) {
			logger.warn("Cannot find TaskExplorer for {}. Using default", name);
			result = defaultTaskExplorer;
		}
		return result;
	}
}
