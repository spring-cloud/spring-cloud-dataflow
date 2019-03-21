/*
 * Copyright 2016-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.rest.job.TaskJobExecutionRel;
import org.springframework.cloud.dataflow.rest.resource.CurrentTaskExecutionsResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.support.ArgumentSanitizer;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
 * Controller for operations on
 * {@link org.springframework.cloud.task.repository.TaskExecution}. This includes
 * obtaining task execution information from the task explorer.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @author David Turanski
 */
@RestController
@RequestMapping("/tasks/executions")
@ExposesResourceFor(TaskExecutionResource.class)
public class TaskExecutionController {

	private final Assembler taskAssembler = new Assembler();

	private final TaskService taskService;

	private final TaskExplorer explorer;

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	/**
	 * Creates a {@code TaskExecutionController} that retrieves Task Execution information
	 * from a the {@link TaskExplorer}
	 *
	 * @param explorer the explorer this controller will use for retrieving task execution
	 * information.
	 * @param taskService used to launch tasks
	 * @param taskDefinitionRepository the task definition repository
	 */
	public TaskExecutionController(TaskExplorer explorer, TaskService taskService,
			TaskDefinitionRepository taskDefinitionRepository) {
		Assert.notNull(explorer, "explorer must not be null");
		Assert.notNull(taskService, "taskService must not be null");
		Assert.notNull(taskDefinitionRepository, "taskDefinitionRepository must not be null");
		this.taskService = taskService;
		this.explorer = explorer;
		this.taskDefinitionRepository = taskDefinitionRepository;
	}

	/**
	 * Return a page-able list of {@link TaskExecutionResource} defined tasks.
	 *
	 * @param pageable page-able collection of {@code TaskExecution}s.
	 * @param assembler for the {@link TaskExecution}s
	 * @return a list of task executions
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<TaskExecutionResource> list(Pageable pageable,
			PagedResourcesAssembler<TaskJobExecutionRel> assembler) {
		Page<TaskExecution> taskExecutions = this.explorer.findAll(pageable);
		Page<TaskJobExecutionRel> result = getPageableRelationships(taskExecutions, pageable);
		return assembler.toResource(result, this.taskAssembler);
	}

	/**
	 * Retrieve all task executions with the task name specified
	 *
	 * @param taskName name of the task
	 * @param pageable page-able collection of {@code TaskExecution}s.
	 * @param assembler for the {@link TaskExecution}s
	 * @return the paged list of task executions
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, params = "name")
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<TaskExecutionResource> retrieveTasksByName(@RequestParam("name") String taskName,
			Pageable pageable, PagedResourcesAssembler<TaskJobExecutionRel> assembler) {
		if (this.taskDefinitionRepository.findOne(taskName) == null) {
			throw new NoSuchTaskDefinitionException(taskName);
		}
		Page<TaskExecution> taskExecutions = this.explorer.findTaskExecutionsByName(taskName, pageable);
		Page<TaskJobExecutionRel> result = getPageableRelationships(taskExecutions, pageable);
		return assembler.toResource(result, this.taskAssembler);
	}

	/**
	 * Request the launching of an existing task definition. The name must be included in
	 * the path.
	 *
	 * @param taskName the name of the existing task to be executed (required)
	 * @param properties the runtime properties for the task, as a comma-delimited list of
	 * key=value pairs
	 * @param arguments the runtime commandline arguments
	 * @return the taskExecutionId for the executed task
	 */
	@RequestMapping(value = "", method = RequestMethod.POST, params = "name")
	@ResponseStatus(HttpStatus.CREATED)
	public long launch(@RequestParam("name") String taskName,
			@RequestParam(required = false) String properties,
			@RequestParam(required = false) String arguments) {
		Map<String, String> propertiesToUse = DeploymentPropertiesUtils.parse(properties);
		DeploymentPropertiesUtils.validateDeploymentProperties(propertiesToUse);
		List<String> argumentsToUse = DeploymentPropertiesUtils.parseParamList(arguments, " ");
		return this.taskService.executeTask(taskName, propertiesToUse, argumentsToUse);
	}

	/**
	 * View the details of a single task execution, specified by id.
	 *
	 * @param id the id of the requested {@link TaskExecution}
	 * @return the {@link TaskExecution}
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public TaskExecutionResource view(@PathVariable("id") long id) {
		TaskExecution taskExecution = this.explorer.getTaskExecution(id);
		if (taskExecution == null) {
			throw new NoSuchTaskExecutionException(id);
		}
		taskExecution = sanitizePotentialSensitiveKeys(taskExecution);
		TaskJobExecutionRel taskJobExecutionRel = new TaskJobExecutionRel(taskExecution,
				new ArrayList<>(this.explorer.getJobExecutionIdsByTaskExecutionId(taskExecution.getExecutionId())));
		return this.taskAssembler.toResource(taskJobExecutionRel);
	}

	@RequestMapping(value = "/current", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public CurrentTaskExecutionsResource getCurrentTaskExecutionsInfo() {
		CurrentTaskExecutionsResource currentTaskExecutionsResource = new CurrentTaskExecutionsResource();
		currentTaskExecutionsResource.setRunningExecutionCount(explorer.getRunningTaskExecutionCount());
		currentTaskExecutionsResource.setMaximumTaskExecutions(taskService.getMaximumConcurrentTasks());
		return currentTaskExecutionsResource;
	}

	/**
	 * Cleanup resources associated with a single task execution, specified by id.
	 *
	 * @param id the id of the {@link TaskExecution} to clean up
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void cleanup(@PathVariable("id") long id) {
		TaskExecution taskExecution = this.explorer.getTaskExecution(id);
		if (taskExecution == null) {
			throw new NoSuchTaskExecutionException(id);
		}
		this.taskService.cleanupExecution(id);
	}

	private Page<TaskJobExecutionRel> getPageableRelationships(Page<TaskExecution> taskExecutions, Pageable pageable) {
		List<TaskJobExecutionRel> taskJobExecutionRels = new ArrayList<>();
		for (TaskExecution taskExecution : taskExecutions.getContent()) {
			taskJobExecutionRels
					.add(new TaskJobExecutionRel(sanitizePotentialSensitiveKeys(taskExecution), new ArrayList<>(
							this.explorer.getJobExecutionIdsByTaskExecutionId(taskExecution.getExecutionId()))));
		}
		return new PageImpl<>(taskJobExecutionRels, pageable, taskExecutions.getTotalElements());
	}

	private TaskExecution sanitizePotentialSensitiveKeys(TaskExecution taskExecution) {
		List<String> args = taskExecution.getArguments().stream()
				.map(argument -> (this.argumentSanitizer.sanitize(argument))).collect(Collectors.toList());
		taskExecution.setArguments(args);
		return taskExecution;
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation that converts
	 * {@link TaskJobExecutionRel}s to {@link TaskExecutionResource}s.
	 */
	private static class Assembler extends ResourceAssemblerSupport<TaskJobExecutionRel, TaskExecutionResource> {

		public Assembler() {
			super(TaskExecutionController.class, TaskExecutionResource.class);
		}

		@Override
		public TaskExecutionResource toResource(TaskJobExecutionRel taskJobExecutionRel) {
			return createResourceWithId(taskJobExecutionRel.getTaskExecution().getExecutionId(), taskJobExecutionRel);
		}

		@Override
		public TaskExecutionResource instantiateResource(TaskJobExecutionRel taskJobExecutionRel) {
			return new TaskExecutionResource(taskJobExecutionRel);
		}
	}

}
