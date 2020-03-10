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

import java.util.Arrays;
import java.util.Map;

import org.springframework.cloud.dataflow.rest.client.SchedulerOperations;
import org.springframework.util.Assert;

/**
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

	public void schedule(Map<String, String> scheduleProperties, String... taskArgs) {
		Assert.isTrue(!isScheduled(), "Task already scheduled!");
		this.schedulerOperations.schedule(prefix, task.getTaskName(), scheduleProperties, Arrays.asList(taskArgs));
	}

	public void unschedule() {
		this.schedulerOperations.unschedule(getScheduleName());
	}

	public boolean isScheduled() {
		return this.schedulerOperations.list(this.task.getTaskName()).getContent().stream()
				.anyMatch(sr -> sr.getScheduleName().equals(this.getScheduleName()));
	}

	public Map<String, String> getScheduleProperties() {
		Assert.isTrue(isScheduled(), "Only scheduled task can have properties");
		return this.schedulerOperations.getSchedule(getScheduleName()).getScheduleProperties();
	}

	public Task getTask() {
		return this.task;
	}

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
