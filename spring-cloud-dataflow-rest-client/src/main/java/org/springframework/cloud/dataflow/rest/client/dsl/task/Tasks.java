/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.cloud.dataflow.rest.client.dsl.task;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;

/**
 * Helper utility to retrieve and introspect Tasks already defined in Data Flow.
 * @author Christian Tzolov
 */
public class Tasks {
	private final DataFlowOperations dataFlowOperations;

	/**
	 * Fluent API method to create a {@link Tasks} helper utility.
	 * @param dataFlowOperations {@link DataFlowOperations} Data Flow Rest client instance.
	 * @return A Tasks utility helper.
	 */
	public static Tasks of(DataFlowOperations dataFlowOperations) {
		return new Tasks(dataFlowOperations);
	}

	private Tasks(DataFlowOperations dataFlowOperations) {
		this.dataFlowOperations = dataFlowOperations;
	}

	/**
	 * @return All tasks defined currently in Data Flow.
	 */
	public List<Task> list() {
		return this.dataFlowOperations.taskOperations().list().getContent().stream()
				.map(td -> new Task(td.getName(), this.dataFlowOperations))
				.collect(Collectors.toList());
	}

	/**
	 * @param taskName existing Task name used to retrieve the task.
	 * @return Task instance identified by the taskName.
	 */
	public Optional<Task> get(String taskName) {
		return this.list().stream()
				.filter(task -> task.getTaskName().equalsIgnoreCase(taskName))
				.findFirst();
	}

	/**
	 * Destroys all Tasks currently defined currently in Data Flow.
	 */
	public void destroyAll() {
		this.dataFlowOperations.taskOperations().destroyAll();
	}
}
