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
package org.springframework.cloud.dataflow.integration.test.util.task.dsl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;

/**
 * @author Christian Tzolov
 */
public class Tasks {
	private final DataFlowOperations dataFlowOperations;

	public Tasks(DataFlowOperations dataFlowOperations) {
		this.dataFlowOperations = dataFlowOperations;
	}

	public TaskBuilder builder() {
		return new TaskBuilder(dataFlowOperations.taskOperations(), dataFlowOperations.jobOperations());
	}

	public List<Task> list() {
		return this.dataFlowOperations.taskOperations().list().getContent().stream()
				.map(td -> new Task(td.getName(), this.dataFlowOperations.taskOperations(), this.dataFlowOperations.jobOperations()))
				.collect(Collectors.toList());
	}
}
