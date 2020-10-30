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

import org.springframework.cloud.dataflow.rest.client.SchedulerOperations;

/**
 * @author Christian Tzolov
 */
public class TaskScheduleBuilder {
	private final SchedulerOperations schedulerOperations;
	private String name;
	private Task task;

	public TaskScheduleBuilder(SchedulerOperations schedulerOperations) {
		this.schedulerOperations = schedulerOperations;
	}

	public TaskScheduleBuilder prefix(String name) {
		this.name = name;
		return this;
	}

	public TaskScheduleBuilder task(Task task) {
		this.task = task;
		return this;
	}

	public TaskSchedule create() {
		return new TaskSchedule(this.name, this.task, this.schedulerOperations);
	}
}
