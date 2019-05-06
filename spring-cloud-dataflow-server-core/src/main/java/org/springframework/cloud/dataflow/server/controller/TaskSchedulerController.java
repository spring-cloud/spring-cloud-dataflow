/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.rest.resource.ScheduleInfoResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.repository.NoSuchScheduleException;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.scheduler.spi.core.ScheduleInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link ScheduleInfo}. This includes CRUD operations.
 *
 * @author Glenn Renfro
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/tasks/schedules")
@ExposesResourceFor(ScheduleInfoResource.class)
public class TaskSchedulerController {

	private final SchedulerService schedulerService;
	private final Assembler taskAssembler = new Assembler();

	/**
	 * Create a {@code TaskSchedulerController} that delegates
	 * <ul>
	 * <li>task scheduling operations via {@link SchedulerService}</li>
	 * </ul>
	 *
	 * @param schedulerService the scheduler service to use to delegate task
	 *                            scheduler operations.
	 */
	public TaskSchedulerController(SchedulerService schedulerService) {
		Assert.notNull(schedulerService, "schedulerService must not be null");
		this.schedulerService = schedulerService;
	}

	/**
	 * Return a page-able list of {@link ScheduleInfo}s.
	 *
	 * @param assembler assembler for the {@link ScheduleInfo}
	 * @param pageable {@link Pageable} to be used
	 * @return a list of Schedules
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<ScheduleInfoResource> list(Pageable pageable,
			PagedResourcesAssembler<ScheduleInfo> assembler) {
		List<ScheduleInfo> result = this.schedulerService.list();
		return assembler.toResource(new PageImpl<>(result, pageable, result.size()), taskAssembler);
	}

	/**
	 * Return a {@link ScheduleInfo} for a specific Schedule.
	 *
	 * @param scheduleName assembler for the {@link ScheduleInfo}
	 * @return a {@link ScheduleInfoResource} instance for the scheduleName specified.
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public ScheduleInfoResource getSchedule(@PathVariable("name") String scheduleName) {
		ScheduleInfo schedule = this.schedulerService.getSchedule(scheduleName);
		if (schedule == null) {
			throw new NoSuchScheduleException(String.format("Schedule [%s] doesn't exist" , scheduleName));
		}
		return taskAssembler.toResource(schedule);
	}

	/**
	 * Return a page-able list of {@link ScheduleInfo}s for a specific
	 * {@link org.springframework.cloud.dataflow.core.TaskDefinition} name.
	 *
	 * @param taskDefinitionName name of the taskDefinition to search.
	 * @param assembler assembler for the {@link ScheduleInfo}.
	 * @return a list of Schedules.
	 */
	@RequestMapping("/instances/{taskDefinitionName}")
	public PagedResources<ScheduleInfoResource> filteredList(@PathVariable String taskDefinitionName,
			PagedResourcesAssembler<ScheduleInfo> assembler) {
		List<ScheduleInfo> result = this.schedulerService.list(taskDefinitionName);
		int resultSize = result.size();
		Pageable pageable = PageRequest.of(0,
				(resultSize == 0) ? resultSize = 1 : resultSize); //handle empty result set
		Page<ScheduleInfo> page = new PageImpl<>(result, pageable,
				result.size());
		return assembler.toResource(page, taskAssembler);
	}

	/**
	 * Create a schedule for an existing
	 * {@link org.springframework.cloud.dataflow.core.TaskDefinition} name.
	 *
	 * @param scheduleName the name of the schedule being created.
	 * @param taskDefinitionName the name of the existing task to be executed (required)
	 * @param properties the runtime properties for the task, as a comma-delimited list of
	 * key=value pairs
	 * @param arguments the runtime commandline arguments
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void save(@RequestParam("scheduleName") String scheduleName,
			@RequestParam("taskDefinitionName") String taskDefinitionName,
			@RequestParam String properties,
			@RequestParam(required = false) String arguments) {
		Map<String, String> propertiesToUse = DeploymentPropertiesUtils.parse(properties);
		List<String> argumentsToUse = DeploymentPropertiesUtils.parseParamList(arguments, " ");
		schedulerService.schedule(scheduleName, taskDefinitionName, propertiesToUse, argumentsToUse);
	}

	/**
	 * Unschedule the schedule from the Scheduler.
	 *
	 * @param scheduleName name of the schedule to be deleted
	 */
	@RequestMapping(value = "/{scheduleName}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void unschedule(@PathVariable("scheduleName") String scheduleName) {
		schedulerService.unschedule(scheduleName);
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation that converts
	 * {@link ScheduleInfo}s to {@link ScheduleInfoResource}s.
	 */
	class Assembler extends ResourceAssemblerSupport<ScheduleInfo, ScheduleInfoResource> {

		public Assembler() {
			super(TaskSchedulerController.class, ScheduleInfoResource.class);
		}

		@Override
		public ScheduleInfoResource toResource(ScheduleInfo scheduleInfo) {
			return createResourceWithId(scheduleInfo.getScheduleName(), scheduleInfo);
		}

		@Override
		public ScheduleInfoResource instantiateResource(ScheduleInfo scheduleInfo) {
			return new ScheduleInfoResource(scheduleInfo.getScheduleName(),
					scheduleInfo.getTaskDefinitionName(), scheduleInfo.getScheduleProperties());
		}
	}
}
