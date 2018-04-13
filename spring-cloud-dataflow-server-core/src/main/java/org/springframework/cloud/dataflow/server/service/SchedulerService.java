/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service;


import java.util.List;
import java.util.Map;

import org.springframework.cloud.scheduler.spi.core.ScheduleInfo;
import org.springframework.cloud.scheduler.spi.core.ScheduleRequest;
import org.springframework.data.domain.Pageable;

/**
 * Provides Scheduler related services.
 *
 * @author Glenn Renfro
 */
public interface SchedulerService {

	/**
	 * Registers the {@link ScheduleRequest} to be executed based on the
	 * cron expression provided.
	 *
	 * @param scheduleName A name to be associated with the schedule.
	 * @param taskDefinitionName the name of the
	 * {@link org.springframework.cloud.dataflow.core.TaskDefinition} to be scheduled.
	 * @param taskProperties  properties required for scheduling or launching a task.
	 * @param commandLineArgs the commands line args to be used when launching the task.
	 */
	void schedule(String scheduleName, String taskDefinitionName,
			Map<String, String> taskProperties, List<String> commandLineArgs);

	/**
	 *  Deletes a schedule that has been created.
	 *
	 * @param scheduleName the name of the schedule to be removed.
	 */
	void unschedule(String scheduleName);

	/**
	 * List all of the Schedules associated with the provided AppDefinition.
	 *
	 * @param pageable Establish the pagination setup for the result set.
	 * @param taskDefinitionName to retrieve Schedules for a specified taskDefinitionName.
	 * @return A List of Schedules configured for the provided taskDefinitionName.
	 */
	List<ScheduleInfo> list(Pageable pageable, String taskDefinitionName) ;

	/**
	 * List all of the schedules registered with the system.
	 *
	 * @param pageable Establish the pagination setup for the result set.
	 * @return A List of Schedules for the given system.
	 */
	List<ScheduleInfo> list(Pageable pageable);
}
