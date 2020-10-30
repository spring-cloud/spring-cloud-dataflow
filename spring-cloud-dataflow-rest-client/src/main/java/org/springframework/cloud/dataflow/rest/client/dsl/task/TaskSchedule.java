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

import java.util.Arrays;
import java.util.Map;

import org.springframework.cloud.dataflow.rest.client.SchedulerOperations;
import org.springframework.util.Assert;

/**
 * Represents a Scheduled Task defined on DataFlow server. Tasks can be scheduled defined with the help of a fluent
 * style builder pattern or use the {@link TaskSchedules} utility to retrieve and manage existing task schedules.
 *
 * For for instance to define a new task task schedule:
 * <pre>
 *     {@code
 *     	try (Task task = Task.builder(dataFlowOperations).name("myTask").definition("timestamp").create();
 *           TaskSchedule taskSchedule = TaskSchedule.builder(dataFlowOperations.schedulerOperations()).prefix("mySchedule").task(task).create()) {
 *
 *           taskSchedule.schedule(Collections.singletonMap(DEFAULT_SCDF_EXPRESSION_KEY, DEFAULT_CRON_EXPRESSION));
 *           TaskSchedule retrievedSchedule = schedules.findByScheduleName(taskSchedule.getScheduleName());
 *           taskSchedule.unschedule();
 *       }
 *     }
 * </pre>
 *
 * <pre>
 *     {@code
 *           taskSchedule.schedule(Collections.singletonMap(DEFAULT_SCDF_EXPRESSION_KEY, DEFAULT_CRON_EXPRESSION));
 *           TaskSchedule retrievedSchedule = TaskSchedules.of().findByScheduleName(taskSchedule.getScheduleName());
 *           taskSchedule.unschedule();
 *     }
 * </pre>

 * @author Christian Tzolov
 */
public class TaskSchedule implements AutoCloseable {
	private final String prefix;

	private final SchedulerOperations schedulerOperations;
	private final Task task;

	public TaskSchedule(String prefix, Task task, SchedulerOperations schedulerOperations) {
		this.prefix = prefix;
		this.task = task;
		this.schedulerOperations = schedulerOperations;
	}

	/**
	 * Fluent API method to create a {@link TaskScheduleBuilder}.
	 * @param schedulerOperations {@link SchedulerOperations} Data Flow Rest client instance.
	 * @return A fluent style builder to create tasks.
	 */
	public static TaskScheduleBuilder builder(SchedulerOperations schedulerOperations) {
		return new TaskScheduleBuilder(schedulerOperations);
	}

	/**
	 * Schedule the task.
	 * @param scheduleProperties Scheduling properties to use.
	 * @param taskArgs  task arguments to pass on schedule run.
	 */
	public void schedule(Map<String, String> scheduleProperties, String... taskArgs) {
		Assert.isTrue(!isScheduled(), "Task already scheduled!");
		this.schedulerOperations.schedule(prefix, task.getTaskName(), scheduleProperties, Arrays.asList(taskArgs));
	}

	/**
	 * Unschedule a previously scheduled task.
	 */
	public void unschedule() {
		this.schedulerOperations.unschedule(getScheduleName());
	}

	/**
	 * @return Returns true if the task is being scheduled or false otherwise.
	 */
	public boolean isScheduled() {
		return this.schedulerOperations.list(this.task.getTaskName()).getContent().stream()
				.anyMatch(sr -> sr.getScheduleName().equals(this.getScheduleName()));
	}

	/**
	 * @return Returns the schedule properties.
	 */
	public Map<String, String> getScheduleProperties() {
		Assert.isTrue(isScheduled(), "Only scheduled task can have properties");
		return this.schedulerOperations.getSchedule(getScheduleName()).getScheduleProperties();
	}

	/**
	 * @return Get the scheduled task.
	 */
	public Task getTask() {
		return this.task;
	}

	/**
	 * @return Get the schedule name.
	 */
	public String getScheduleName() {
		return String.format("%s-scdf-%s", this.prefix, this.task.getTaskName());
	}

	@Override
	public void close() {
		if (isScheduled()) {
			unschedule();
		}
	}
}
