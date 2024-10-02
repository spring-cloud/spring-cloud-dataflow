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

package org.springframework.cloud.dataflow.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.rest.resource.TaskAppStatusResource;
import org.springframework.cloud.dataflow.server.service.TaskValidationService;
import org.springframework.cloud.dataflow.server.service.ValidationStatus;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link ValidationStatus}.
 *
 * @author Glenn Renfro
 *
 */
@RestController
@RequestMapping("/tasks/validation")
@ExposesResourceFor(TaskAppStatusResource.class)
public class TaskValidationController {

	private static final Logger logger = LoggerFactory.getLogger(TaskValidationController.class);

	/**
	 * The service that is responsible for validating tasks.
	 */
	private final TaskValidationService taskValidationService;

	/**
	 * Create a {@code TaskValidationController} that delegates to
	 * {@link TaskValidationService}.
	 *
	 * @param taskValidationService the task service to use
	 */
	public TaskValidationController(TaskValidationService taskValidationService) {
		Assert.notNull(taskValidationService, "TaskValidationService must not be null");
		this.taskValidationService = taskValidationService;
	}

	/**
	 * Return {@link TaskAppStatusResource} showing the validation status the apps in a task.
	 *
	 * @param name name of the task definition
	 * @return The status for the apps in a task definition.
	 */
	@GetMapping("/{name}")
	@ResponseStatus(HttpStatus.OK)
	public TaskAppStatusResource validate(
			@PathVariable String name) {
		ValidationStatus result = this.taskValidationService.validateTask(name);
		return new Assembler().toModel(result);
	}

	/**
	 * {@link org.springframework.hateoas.server.ResourceAssembler} implementation that converts
	 * {@link ValidationStatus}s to {@link TaskAppStatusResource}s.
	 */
	class Assembler extends RepresentationModelAssemblerSupport<ValidationStatus, TaskAppStatusResource> {

		public Assembler() {
			super(TaskValidationController.class, TaskAppStatusResource.class);
		}

		@Override
		public TaskAppStatusResource toModel(ValidationStatus entity) {
			return new TaskAppStatusResource(entity.getDefinitionName(), entity.getDefinitionDsl(), entity.getDescription(), entity.getAppsStatuses());
		}
	}
}
