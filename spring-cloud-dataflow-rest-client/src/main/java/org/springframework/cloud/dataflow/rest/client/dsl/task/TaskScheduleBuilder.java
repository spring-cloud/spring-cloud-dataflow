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
import org.springframework.cloud.dataflow.rest.resource.ScheduleInfoResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Christian Tzolov
 */
public class TaskScheduleBuilder {
	private final DataFlowOperations dataFlowOperations;
	private String name;
	private Task task;

	public TaskScheduleBuilder(DataFlowOperations dataFlowOperations) {
		this.dataFlowOperations = dataFlowOperations;
	}

	public TaskScheduleBuilder scheduleName(String name) {
		this.name = name;
		return this;
	}

	public TaskScheduleBuilder task(Task task) {
		this.task = task;
		return this;
	}

	/**
	 * Create new {@link TaskSchedule} instance based on the scheduler name and task.
	 * @return Returns new {@link TaskSchedule} instance.
	 */
	public TaskSchedule build() {
		Assert.notNull(this.task, "The schedule task must be set!");
		Assert.isTrue(StringUtils.hasText(this.name), "The schedule name must be set!");
		return new TaskSchedule(this.name, this.task, this.dataFlowOperations.schedulerOperations());
	}

	/**
	 * Retrieves an existing {@link TaskSchedule} by name.
	 * @param scheduleName schedule name to search for.
	 * @return Returns an existing {@link TaskSchedule} by name.
	 */
	public Optional<TaskSchedule> findByScheduleName(String scheduleName) {
		if (!isScheduled(scheduleName)) {
			return Optional.empty();
		}
		ScheduleInfoResource s = this.dataFlowOperations.schedulerOperations().getSchedule(scheduleName);
		if (s == null) {
			return Optional.empty();
		}

		Optional<Task> task = Task.builder(dataFlowOperations).findByName(s.getTaskDefinitionName());
		if (!task.isPresent()) {
			return Optional.empty();
		}
		return Optional.of(new TaskSchedule(scheduleName, task.get(), this.dataFlowOperations.schedulerOperations()));
	}

	private boolean isScheduled(String scheduleName) {
		return this.dataFlowOperations.schedulerOperations().list().getContent().stream()
				.anyMatch(sr -> sr.getScheduleName().equals(scheduleName));
	}

	/**
	 * Retrieve all schedules registered for a given Task.
	 * @param task Task to search scheduled for.
	 * @return Returns list of all schedules for the provided task.
	 */
	public List<TaskSchedule> list(Task task) {
		return this.dataFlowOperations.schedulerOperations().list(task.getTaskName()).getContent().stream()
				.map(s -> {
					String prefix = s.getScheduleName().replace("-scdf-" + task.getTaskName(), "");
					return new TaskSchedule(prefix, task, this.dataFlowOperations.schedulerOperations());
				}).collect(Collectors.toList());
	}

	/**
	 * @return Returns all registered task schedules.
	 */
	public List<TaskSchedule> list() {
		return this.dataFlowOperations.schedulerOperations().list().getContent().stream()
				.map(s -> {
					Task task = Task.builder(dataFlowOperations).findByName(s.getTaskDefinitionName()).get();
					String prefix = s.getScheduleName().replace("-scdf-" + task.getTaskName(), "");
					return new TaskSchedule(prefix, task, this.dataFlowOperations.schedulerOperations());
				}).collect(Collectors.toList());
	}
}
