/*
 * Copyright 2018-2021 the original author or authors.
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
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
	 * @param platform the name of the platform from which schedules will be retrieved.
	 * @param pageable {@link Pageable} to be used
	 * @return a list of Schedules
	 */
	@GetMapping("")
	@ResponseStatus(HttpStatus.OK)
	public PagedModel<ScheduleInfoResource> list(
			Pageable pageable,
			@RequestParam(required = false) String platform,
			PagedResourcesAssembler<ScheduleInfo> assembler
	) {
		List<ScheduleInfo> result = this.schedulerService.listForPlatform(platform);
		return assembler.toModel(new PageImpl<>(result, pageable, result.size()), taskAssembler);
	}

	/**
	 * Return a {@link ScheduleInfo} for a specific Schedule.
	 *
	 * @param scheduleName assembler for the {@link ScheduleInfo}
	 * @param platform the name of the platform from which the schedule will be retrieved.
	 * @return a {@link ScheduleInfoResource} instance for the scheduleName specified.
	 */
	@GetMapping("/{name}")
	@ResponseStatus(HttpStatus.OK)
	public ScheduleInfoResource getSchedule(
			@PathVariable("name") String scheduleName,
			@RequestParam(required = false) String platform
	) {
		ScheduleInfo schedule = this.schedulerService.getSchedule(scheduleName, platform);
		if (schedule == null) {
			throw new NoSuchScheduleException(String.format("Schedule [%s] doesn't exist" , scheduleName));
		}
		return taskAssembler.toModel(schedule);
	}

	/**
	 * Return a page-able list of {@link ScheduleInfo}s for a specific
	 * {@link org.springframework.cloud.dataflow.core.TaskDefinition} name.
	 *
	 * @param taskDefinitionName name of the taskDefinition to search.
	 * @param platform name of the platform from which the list is retrieved.
	 * @param assembler assembler for the {@link ScheduleInfo}.
	 * @return a list of Schedules.
	 */
	@RequestMapping("/instances/{taskDefinitionName}")
	public PagedModel<ScheduleInfoResource> filteredList(
			@PathVariable String taskDefinitionName,
			@RequestParam(required = false) String platform,
			PagedResourcesAssembler<ScheduleInfo> assembler
	) {
		List<ScheduleInfo> result = this.schedulerService.list(taskDefinitionName, platform);
		int resultSize = result.size();
		Pageable pageable = PageRequest.of(0,
				(resultSize == 0) ? resultSize = 1 : resultSize); //handle empty result set
		Page<ScheduleInfo> page = new PageImpl<>(result, pageable,
				result.size());
		return assembler.toModel(page, taskAssembler);
	}
	/**
	 * Remove schedules for a specific {@link org.springframework.cloud.dataflow.core.TaskDefinition} name .
	 *
	 * @param taskDefinitionName the name of the {@link org.springframework.cloud.dataflow.core.TaskDefinition}.
	 */
	@DeleteMapping("/instances/{taskDefinitionName}")
	@ResponseStatus(HttpStatus.OK)
	public void deleteSchedulesforDefinition(@PathVariable String taskDefinitionName) {
		this.schedulerService.unscheduleForTaskDefinition(taskDefinitionName);
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
	 * @param platform the name of the platform for which the schedule is created.
	 */
	@PostMapping("")
	@ResponseStatus(HttpStatus.CREATED)
	public void save(
			@RequestParam String scheduleName,
			@RequestParam String taskDefinitionName,
			@RequestParam String properties,
			@RequestParam(required = false) String arguments,
			@RequestParam(required = false) String platform
	) {
		Map<String, String> propertiesToUse = DeploymentPropertiesUtils.parse(properties);
		List<String> argumentsToUse = DeploymentPropertiesUtils.parseArgumentList(arguments, " ");
		this.schedulerService.schedule(scheduleName.strip(), taskDefinitionName,
				propertiesToUse, argumentsToUse, platform);
	}

	/**
	 * Unschedule the schedule from the Scheduler.
	 *
	 * @param scheduleName name of the schedule to be deleted
	 * @param platform name of the platform from which the schedule is deleted.
	 */
	@DeleteMapping("/{scheduleName}")
	@ResponseStatus(HttpStatus.OK)
	public void unschedule(
			@PathVariable String scheduleName,
			@RequestParam(required = false) String platform
	) {
		schedulerService.unschedule(scheduleName, platform);
	}

	/**
	 * {@link org.springframework.hateoas.server.RepresentationModelAssembler} implementation that converts
	 * {@link ScheduleInfo}s to {@link ScheduleInfoResource}s.
	 */
	class Assembler extends RepresentationModelAssemblerSupport<ScheduleInfo, ScheduleInfoResource> {

		public Assembler() {
			super(TaskSchedulerController.class, ScheduleInfoResource.class);
		}

		@Override
		public ScheduleInfoResource toModel(ScheduleInfo scheduleInfo) {
			return createModelWithId(scheduleInfo.getScheduleName(), scheduleInfo);
		}

		@Override
		public ScheduleInfoResource instantiateModel(ScheduleInfo scheduleInfo) {
			return new ScheduleInfoResource(scheduleInfo.getScheduleName(),
					scheduleInfo.getTaskDefinitionName(), scheduleInfo.getScheduleProperties());
		}
	}
}
