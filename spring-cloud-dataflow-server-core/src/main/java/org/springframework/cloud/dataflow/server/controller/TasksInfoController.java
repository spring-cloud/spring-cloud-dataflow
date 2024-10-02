/*
 * Copyright 2016-2021 the original author or authors.
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

import org.springframework.cloud.dataflow.rest.resource.TaskExecutionsInfoResource;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on
 * {@link TaskExecution}. This includes
 * obtaining task execution information from the task explorer.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @author David Turanski
 * @author Gunnar Hillert
 */
@RestController
@RequestMapping("/tasks/info")
public class TasksInfoController {

	private final TaskExecutionsAssembler taskExecutionsAssembler = new TaskExecutionsAssembler();

	private final TaskExecutionService taskExecutionService;

	/**
	 * Creates a {@code TaskExecutionController} that retrieves Task Executions information
	 *
	 * @param taskExecutionService used to launch tasks
	 */
	public TasksInfoController(TaskExecutionService taskExecutionService) {
		Assert.notNull(taskExecutionService, "taskExecutionService must not be null");
		this.taskExecutionService = taskExecutionService;
	}

	@GetMapping("executions")
	@ResponseStatus(HttpStatus.OK)
	public TaskExecutionsInfoResource getInfo(
			@RequestParam(required = false, defaultValue = "false") String completed,
			@RequestParam(required = false, defaultValue = "", name="name") String taskName,
			@RequestParam(required = false) Integer days
	) {
		return this.taskExecutionsAssembler.toModel(this.taskExecutionService.getAllTaskExecutionsCount(Boolean.parseBoolean(completed), taskName, days));
	}

	/**
	 * {@link org.springframework.hateoas.server.RepresentationModelAssembler} implementation to assembler TaskExecutionsResource.
	 */
	private static class TaskExecutionsAssembler extends RepresentationModelAssemblerSupport<Integer, TaskExecutionsInfoResource> {

		public TaskExecutionsAssembler() {
			super(TasksInfoController.class, TaskExecutionsInfoResource.class);
		}

		@Override
		public TaskExecutionsInfoResource toModel(Integer totalExecutions) {
			TaskExecutionsInfoResource taskExecutionsInfoResource = new TaskExecutionsInfoResource();
			taskExecutionsInfoResource.setTotalExecutions(totalExecutions);
			return createModelWithId(taskExecutionsInfoResource, totalExecutions);
		}

		@Override
		public TaskExecutionsInfoResource instantiateModel(Integer totalExecutions) {
			TaskExecutionsInfoResource taskExecutionsInfoResource = new TaskExecutionsInfoResource();
			taskExecutionsInfoResource.setTotalExecutions(totalExecutions);
			return taskExecutionsInfoResource;
		}
	}

}
