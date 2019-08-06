/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.rest.resource.ScheduleInfoResource;
import org.springframework.cloud.scheduler.spi.core.ScheduleInfo;
import org.springframework.hateoas.PagedModel;

/**
 * Interface defining operations available against schedules.
 *
 * @author Glenn Renfro
 */
public interface SchedulerOperations {

	/**
	 * Schedules the task specified with the taskDefinitionName with the
	 * platform specific scheduler.  To setup the task with the scheduler set
	 * the properties required to process the schedule within the taskProperties map.
	 * Each scheduler property should be prefixed with
	 * {@value org.springframework.cloud.scheduler.spi.core.SchedulerPropertyKeys#PREFIX}.
	 * The schedule expression (cron for example)
	 * are specified within the task properties using the
	 * {@value org.springframework.cloud.scheduler.spi.core.SchedulerPropertyKeys#CRON_EXPRESSION} as the key
	 * and the associated value would be the cron expression to be used.
	 *
	 * @param scheduleName A name to be associated with the schedule.
	 * @param taskDefinitionName the name of the
	 * {@link org.springframework.cloud.dataflow.core.TaskDefinition} to be scheduled.
	 * @param taskProperties  properties required for scheduling or launching a task.
	 * @param commandLineArgs the command line args to be used when launching the task.
	 */
	void schedule(String scheduleName, String taskDefinitionName,
			Map<String, String> taskProperties, List<String> commandLineArgs);

	/**
	 *  Unschedule a schedule that has been created.
	 *
	 * @param scheduleName the name of the schedule to be removed.
	 */
	void unschedule(String scheduleName);

	/**
	 * List all of the Schedules associated with the provided TaskDefinition.
	 *
	 * @param taskDefinitionName to retrieve Schedules for a specified taskDefinitionName.
	 * @return A List of Schedules configured for the provided taskDefinitionName.
	 */
	PagedModel<ScheduleInfoResource> list(String taskDefinitionName) ;

	/**
	 * List all of the schedules registered with the system.
	 *
	 * @return A List of Schedules for the given system.
	 */
	PagedModel<ScheduleInfoResource> list();

	/**
	 * Retrieves the {@link ScheduleInfo} for the specified ScheduleName.
	 *
	 * @param scheduleName the name of schedule to retrieve.
	 * @return {@link ScheduleInfo} for the scheduleName passed in.
	 */
	ScheduleInfoResource getSchedule(String scheduleName);}
