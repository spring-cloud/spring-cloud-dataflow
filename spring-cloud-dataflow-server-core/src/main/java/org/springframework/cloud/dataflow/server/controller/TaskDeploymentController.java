/*
 * Copyright 2015-2016 the original author or authors.
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

import org.springframework.cloud.dataflow.rest.resource.TaskDeploymentResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations for deployment operations.
 *
 * @author Michael Minella
 * @author Marius Bogoevici
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Janne Valkealahti
 * @author Gunnar Hillert
 */
@RestController
@RequestMapping("/tasks/deployments")
@ExposesResourceFor(TaskDeploymentResource.class)
public class TaskDeploymentController {

	private final TaskService taskService;

	/**
	 * Creates a {@code TaskDeploymentController} that delegates launching
	 * operations to the provided {@link TaskService}
	 * @param taskService Must not be null
	 */
	public TaskDeploymentController(TaskService taskService) {
		Assert.notNull(taskService, "TaskService must not be null");
		this.taskService = taskService;
	}

	/**
	 * Request the launching of an existing task definition.  The name must be
	 * included in the path.
	 *
	 * @param taskName the name of the existing task to be executed (required)
	 * @param properties the runtime properties for the task, as a comma-delimited list of
	 * 					 key=value pairs
	 * @param arguments the runtime commandline arguments
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void deploy(@PathVariable("name") String taskName, @RequestParam(required = false) String properties,
			@RequestParam(required = false) List<String> arguments) {
		this.taskService.executeTask(taskName, DeploymentPropertiesUtils.parse(properties), DeploymentPropertiesUtils.parseParams(arguments));
	}
}
