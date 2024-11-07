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
import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.SchedulerOperations;
import org.springframework.util.Assert;

/**
 * Represents a Scheduled Task defined on DataFlow server. Tasks can be scheduled defined with the help of a fluent
 * style builder pattern or use it to retrieve and manage existing task schedules.
 *
 * For for instance to define a new task task schedule:
 * <pre>
 *     {@code
 *      TaskScheduleBuilder taskScheduleBuilder = TaskSchedule.builder(dataFlowOperations);
 *     	try (Task task = Task.builder(dataFlowOperations).name("myTask").definition("timestamp").build();
 *           TaskSchedule taskSchedule = taskScheduleBuilder.scheduleName("mySchedule").task(task).build()) {
 *
 *           taskSchedule.schedule("56 20 ? * *", Collections.emptyMap());
 *
 *           TaskSchedule retrievedSchedule = taskScheduleBuilder.findByScheduleName(taskSchedule.getScheduleName());
 *           taskSchedule.unschedule();
 *       }
 *     }
 * </pre>
 *
 * @author Christian Tzolov
 */
public class TaskSchedule implements AutoCloseable {

	public static final String CRON_EXPRESSION_KEY = "deployer.cron.expression";

	private final String scheduleName;

	private final SchedulerOperations schedulerOperations;
	private final Task task;

	TaskSchedule(String scheduleName, Task task, SchedulerOperations schedulerOperations) {
		this.scheduleName = scheduleName;
		this.task = task;
		this.schedulerOperations = schedulerOperations;
	}

	/**
	 * Fluent API method to create a {@link TaskScheduleBuilder}.
	 * @param dataFlowOperations {@link DataFlowOperations} Data Flow Rest client instance.
	 * @return A fluent style builder to create tasks.
	 */
	public static TaskScheduleBuilder builder(DataFlowOperations dataFlowOperations) {
		return new TaskScheduleBuilder(dataFlowOperations);
	}

	/**
	 * Schedule the task.
	 * @param scheduleProperties Scheduling properties to use.
	 * @param taskArgs  task arguments to pass on schedule run.
	 * @param cronExpression the cron expression used to schedule the task execution.
	 */
	public void schedule(String cronExpression, Map<String, String> scheduleProperties, String... taskArgs) {
		Assert.isTrue(!isScheduled(), "Task already scheduled!");
		Assert.hasText(cronExpression, "cronExpression must not be empty or null");
		Map<String, String> updatedProperties = new HashMap<>(scheduleProperties);
		updatedProperties.put(CRON_EXPRESSION_KEY, cronExpression);
		this.schedulerOperations.schedule(scheduleName, task.getTaskName(), updatedProperties, Arrays.asList(taskArgs));
	}

	/**
	 * Unschedule a previously scheduled task.
	 */
	public void unschedule() {
		this.schedulerOperations.unschedule(this.scheduleName);
	}

	/**
	 * @return Returns true if the task is being scheduled or false otherwise.
	 */
	public boolean isScheduled() {
		return this.schedulerOperations.list(this.task.getTaskName()).getContent().stream()
				.anyMatch(sr -> sr.getScheduleName().equals(this.scheduleName));
	}

	/**
	 * @return Returns the schedule properties.
	 */
	public Map<String, String> getScheduleProperties() {
		Assert.isTrue(isScheduled(), "Only scheduled task can have properties");
		return this.schedulerOperations.getSchedule(this.scheduleName).getScheduleProperties();
	}

	/**
	 * @return Get the scheduled task.
	 */
	public Task getTask() {
		return this.task;
	}

	public String getScheduleName() {
		return this.scheduleName;
	}

	@Override
	public void close() {
		if (isScheduled()) {
			unschedule();
		}
	}
}
