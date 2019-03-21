/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.configuration;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cloud.scheduler.spi.core.CreateScheduleException;
import org.springframework.cloud.scheduler.spi.core.ScheduleInfo;
import org.springframework.cloud.scheduler.spi.core.ScheduleRequest;
import org.springframework.cloud.scheduler.spi.core.Scheduler;


/**
 * @author Mark Pollack
 */
public class SimpleTestScheduler implements Scheduler {

	public static final String INVALID_CRON_EXPRESSION = "BAD";
	List<ScheduleInfo> schedules = new ArrayList<>();

	@Override
	public void schedule(ScheduleRequest scheduleRequest) {
		ScheduleInfo schedule = new ScheduleInfo();
		schedule.setScheduleName(scheduleRequest.getScheduleName());
		schedule.setScheduleProperties(scheduleRequest.getSchedulerProperties());
		schedule.setTaskDefinitionName(scheduleRequest.getDefinition().getName());
		if (schedule.getScheduleProperties().containsKey("spring.cloud.scheduler.cron.expression") &&
				schedule.getScheduleProperties().get("spring.cloud.scheduler.cron.expression")
						.equals(INVALID_CRON_EXPRESSION)) {
			throw new CreateScheduleException("Invalid Cron Expression", new IllegalArgumentException());
		}
		List<ScheduleInfo> scheduleInfos = schedules.stream()
				.filter(s -> s.getScheduleName().equals(scheduleRequest.getScheduleName()))
				.collect(Collectors.toList());
		if (scheduleInfos.size() > 0) {
			throw new CreateScheduleException(
					String.format("Schedule %s already exists",
							scheduleRequest.getScheduleName()),
					null);
		}
		schedules.add(schedule);

	}

	@Override
	public void unschedule(String scheduleName) {
		schedules = schedules.stream().filter(
				s -> !s.getScheduleName().equals(scheduleName)).collect(Collectors.toList());
	}

	@Override
	public List<ScheduleInfo> list(String taskDefinitionName) {
		return schedules.stream().filter(
				s -> s.getTaskDefinitionName().equals(taskDefinitionName)).collect(Collectors.toList());
	}

	@Override
	public List<ScheduleInfo> list() {
		return schedules;
	}

	public List<ScheduleInfo> getSchedules() {
		return schedules;
	}
}
