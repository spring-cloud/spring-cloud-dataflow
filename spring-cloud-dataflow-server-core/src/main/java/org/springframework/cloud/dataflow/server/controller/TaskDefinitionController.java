/*
 * Copyright 2016-2020 the original author or authors.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.cloud.dataflow.rest.util.TaskSanitizer;
import org.springframework.cloud.dataflow.server.controller.support.TaskExecutionAwareTaskDefinition;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskSaveService;
import org.springframework.cloud.dataflow.server.service.impl.TaskServiceUtils;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link TaskDefinition}. This includes CRUD operations.
 *
 * @author Michael Minella
 * @author Marius Bogoevici
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Daniel Serleg
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/tasks/definitions")
@ExposesResourceFor(TaskDefinitionResource.class)
public class TaskDefinitionController {

	private final TaskDefinitionRepository repository;

	private final TaskSaveService taskSaveService;

	private final TaskDeleteService taskDeleteService;

	private final TaskExplorer explorer;

	private final TaskExecutionService taskExecutionService;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	private final TaskSanitizer taskSanitizer = new TaskSanitizer();

	/**
	 * Creates a {@code TaskDefinitionController} that delegates
	 * <ul>
	 * <li>CRUD operations to the provided {@link TaskDefinitionRepository}</li>
	 * <li>task status checks to the provided {@link TaskLauncher}</li>
	 * </ul>
	 *
	 * @param taskExplorer used to look up TaskExecutions.
	 * @param repository the repository this controller will use for task CRUD operations.
	 * @param taskSaveService handles Task saving related operations.
	 * @param taskDeleteService handles Task deletion related operations.
	 * @param taskExecutionService handles Task execution related operations.
	 */
	public TaskDefinitionController(TaskExplorer taskExplorer, TaskDefinitionRepository repository,
			TaskSaveService taskSaveService, TaskDeleteService taskDeleteService,
			TaskExecutionService taskExecutionService) {
		Assert.notNull(taskExplorer, "taskExplorer must not be null");
		Assert.notNull(repository, "repository must not be null");
		Assert.notNull(taskSaveService, "taskSaveService must not be null");
		Assert.notNull(taskDeleteService, "taskDeleteService must not be null");
		Assert.notNull(taskExecutionService, "taskExecutionService must not be null");
		this.explorer = taskExplorer;
		this.repository = repository;
		this.taskSaveService = taskSaveService;
		this.taskDeleteService = taskDeleteService;
		this.taskExecutionService = taskExecutionService;
	}

	/**
	 * Register a task definition for future execution.
	 *
	 * @param name name the name of the task
	 * @param dsl DSL definition for the task
	 * @param description description of the task definition
	 * @return the task definition
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	public TaskDefinitionResource save(@RequestParam("name") String name, @RequestParam("definition") String dsl,
									   @RequestParam(value = "description", defaultValue = "") String description) {
		TaskDefinition taskDefinition = new TaskDefinition(name, dsl, description);
		taskSaveService.saveTaskDefinition(taskDefinition);
		return new Assembler().toModel(new TaskExecutionAwareTaskDefinition(taskDefinition));
	}

	/**
	 * Delete the task from the repository so that it can no longer be executed.
	 *
	 * @param name name of the task to be deleted
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void destroyTask(@PathVariable("name") String name, @RequestParam(required = false) Boolean cleanup) {
		boolean taskExecutionCleanup = (cleanup != null && cleanup) ? cleanup : false;
		this.taskDeleteService.deleteTaskDefinition(name, taskExecutionCleanup);
	}

	/**
	 * Delete all task from the repository.
	 *
	 */
	@RequestMapping(value = "", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void destroyAll() {
		taskDeleteService.deleteAll();
	}

	/**
	 * Return a page-able list of {@link TaskDefinitionResource} defined tasks.
	 *
	 * @param pageable page-able collection of {@code TaskDefinitionResource}
	 * @param search optional findByTaskNameContains parameter
	 * @param manifest optional manifest flag to indicate whether the latest task execution requires task manifest update
	 * @param assembler assembler for the {@link TaskDefinition}
	 * @return a list of task definitions
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedModel<TaskDefinitionResource> list(Pageable pageable, @RequestParam(required = false) String search,
			@RequestParam(required = false) boolean manifest,
			PagedResourcesAssembler<TaskExecutionAwareTaskDefinition> assembler) {

		final Page<TaskDefinition> taskDefinitions;
		if (search != null) {
			taskDefinitions = repository.findByTaskNameContains(search, pageable);
		}
		else {
			taskDefinitions = repository.findAll(pageable);
		}

		final java.util.HashMap<String, TaskDefinition> taskDefinitionMap = new java.util.HashMap<>();

		for (TaskDefinition taskDefinition : taskDefinitions) {
			taskDefinitionMap.put(taskDefinition.getName(), taskDefinition);
		}

		final List<TaskExecution> taskExecutions;

		if (!taskDefinitionMap.isEmpty()) {
			taskExecutions = this.explorer.getLatestTaskExecutionsByTaskNames(
					taskDefinitionMap.keySet().toArray(new String[taskDefinitionMap.size()]));
		}
		else {
			taskExecutions = null;
		}

		final Page<TaskExecutionAwareTaskDefinition> taskExecutionAwareTaskDefinitions = taskDefinitions
				.map(new TaskDefinitionConverter(taskExecutions));

		PagedModel<TaskDefinitionResource> taskDefinitionResources = assembler.toModel(taskExecutionAwareTaskDefinitions, new Assembler(manifest));
		// Classify the composed task elements by iterating through the task definitions that are part of this page.
		updateComposedTaskElement(taskDefinitionResources.getContent());
		return taskDefinitionResources;
	}


	private Collection<TaskDefinitionResource> updateComposedTaskElement(Collection<TaskDefinitionResource> taskDefinitionResources) {
		Map<String, TaskDefinitionResource> taskNameResources = new HashMap<>();
		for (TaskDefinitionResource taskDefinitionResource: taskDefinitionResources) {
			taskNameResources.put(taskDefinitionResource.getName(), taskDefinitionResource);
		}
		for (TaskDefinitionResource taskDefinition: taskDefinitionResources) {
			TaskParser taskParser = new TaskParser(taskDefinition.getName(), taskDefinition.getDslText(), true, true);
			TaskNode taskNode = taskParser.parse();
			if (taskNode.isComposed()) {
				taskNode.getTaskApps().forEach(task -> {
					if (taskNameResources.keySet().contains(task.getExecutableDSLName())) {
						taskNameResources.get(task.getExecutableDSLName()).setComposedTaskElement(true);
					}
				});
			}
		}
		return taskDefinitionResources;
	}

	/**
	 * Return a given task definition resource.
	 *
	 * @param name the name of an existing task definition (required)
	 * @return the task definition
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public TaskDefinitionResource display(@PathVariable("name") String name, @RequestParam(required = false, name = "manifest") boolean manifest) {
		TaskDefinition definition = this.repository.findById(name)
				.orElseThrow(() -> new NoSuchTaskDefinitionException(name));
		final TaskExecution taskExecution = this.explorer.getLatestTaskExecutionForTaskName(name);
		final Assembler taskAssembler = new Assembler(manifest);
		TaskDefinitionResource taskDefinitionResource;
		if (taskExecution != null) {
			taskDefinitionResource = taskAssembler.toModel(new TaskExecutionAwareTaskDefinition(definition, taskExecution));
		}
		else {
			taskDefinitionResource = taskAssembler.toModel(new TaskExecutionAwareTaskDefinition(definition));
		}
		// Identify if the task definition is a composed task element
		updateComposedTaskElement(taskDefinitionResource);
		return taskDefinitionResource;
	}


	private void updateComposedTaskElement(TaskDefinitionResource taskDefinitionResource) {
		if (taskDefinitionResource.getName().contains("-")) {
			String prefix = taskDefinitionResource.getName().split("-")[0];
			TaskDefinition taskDefinition = this.repository.findById(prefix).orElse(null);
			if (taskDefinition != null && TaskServiceUtils
					.isComposedTaskDefinition(taskDefinition.getDslText())) {
				taskDefinitionResource.setComposedTaskElement(true);
			}
		}
	}

	/**
	 * {@link org.springframework.hateoas.server.RepresentationModelAssembler} implementation that converts
	 * {@link TaskDefinition}s to {@link TaskDefinitionResource}s.
	 */
	class Assembler extends RepresentationModelAssemblerSupport<TaskExecutionAwareTaskDefinition, TaskDefinitionResource> {

		private boolean enableManifest;

		public Assembler() {
			super(TaskDefinitionController.class, TaskDefinitionResource.class);
		}

		public Assembler(boolean enableManifest) {
			super(TaskDefinitionController.class, TaskDefinitionResource.class);
			this.enableManifest = enableManifest;
		}

		TaskDefinitionResource updateTaskExecutionResource(TaskExecutionAwareTaskDefinition taskExecutionAwareTaskDefinition, TaskDefinitionResource taskDefinitionResource, boolean manifest) {
			TaskExecution taskExecution = taskExecutionAwareTaskDefinition.getLatestTaskExecution();
			TaskManifest taskManifest = taskExecutionService.findTaskManifestById(taskExecution.getExecutionId());
			taskManifest = taskSanitizer.sanitizeTaskManifest(taskManifest);
			TaskExecutionResource taskExecutionResource = (manifest) ? new TaskExecutionResource(taskExecution, taskManifest) : new TaskExecutionResource(taskExecution);
			taskDefinitionResource.setLastTaskExecution(taskExecutionResource);
			return taskDefinitionResource;
		}

		@Override
		public TaskDefinitionResource toModel(TaskExecutionAwareTaskDefinition taskExecutionAwareTaskDefinition) {
			return createModelWithId(taskExecutionAwareTaskDefinition.getTaskDefinition().getName(),
					taskExecutionAwareTaskDefinition);
		}

		@Override
		public TaskDefinitionResource instantiateModel(
				TaskExecutionAwareTaskDefinition taskExecutionAwareTaskDefinition) {
			boolean composed = TaskServiceUtils
					.isComposedTaskDefinition(taskExecutionAwareTaskDefinition.getTaskDefinition().getDslText());
			TaskDefinitionResource taskDefinitionResource = new TaskDefinitionResource(
					taskExecutionAwareTaskDefinition.getTaskDefinition().getName(),
					argumentSanitizer.sanitizeTaskDsl(taskExecutionAwareTaskDefinition.getTaskDefinition()),
					taskExecutionAwareTaskDefinition.getTaskDefinition().getDescription());
			taskDefinitionResource.setComposed(composed);
			if (taskExecutionAwareTaskDefinition.getLatestTaskExecution() != null) {
				updateTaskExecutionResource(taskExecutionAwareTaskDefinition, taskDefinitionResource,
						this.isEnableManifest());
			}
			return taskDefinitionResource;
		}

		/**
		 * Returns if the TaskExecution needs to be updated with the task manifest.
		 * @return the boolean value of to enable setting the manifest
		 */
		public boolean isEnableManifest() {
			return enableManifest;
		}

		/**
		 * Set the flag to indicate whether the task manifest needs to be updated for the TaskExecution.
		 * @param enableManifest the boolean value of to enable setting the manifest
		 */
		public void setEnableManifest(boolean enableManifest) {
			this.enableManifest = enableManifest;
		}
	}

	class TaskDefinitionConverter implements Function<TaskDefinition, TaskExecutionAwareTaskDefinition> {
		final Map<String, TaskExecution> taskExecutions;

		public TaskDefinitionConverter(List<TaskExecution> taskExecutions) {
			super();
			if (taskExecutions != null) {
				this.taskExecutions = new HashMap<>(taskExecutions.size());
				for (TaskExecution taskExecution : taskExecutions) {
					this.taskExecutions.put(taskExecution.getTaskName(), taskExecution);
				}
			}
			else {
				this.taskExecutions = null;
			}
		}

		@Override
		public TaskExecutionAwareTaskDefinition apply(TaskDefinition source) {
			TaskExecution lastTaskExecution = null;

			if (taskExecutions != null) {
				lastTaskExecution = taskExecutions.get(source.getName());
			}

			if (lastTaskExecution != null) {
				return new TaskExecutionAwareTaskDefinition(source, lastTaskExecution);
			}
			else {
				return new TaskExecutionAwareTaskDefinition(source);
			}
		}
	};
}
