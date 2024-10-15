/*
 * Copyright 2016-2022 the original author or authors.
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
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.server.controller.assembler.TaskDefinitionAssemblerProvider;
import org.springframework.cloud.dataflow.server.controller.support.TaskExecutionAwareTaskDefinition;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskQueryParamException;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskSaveService;
import org.springframework.cloud.dataflow.server.service.impl.TaskServiceUtils;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelAssembler;
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
 * Controller for operations on {@link TaskDefinition}. This includes CRUD operations.
 *
 * @author Michael Minella
 * @author Marius Bogoevici
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Daniel Serleg
 * @author Ilayaperumal Gopinathan
 * @author Chris Bono
 */
@RestController
@RequestMapping("/tasks/definitions")
@ExposesResourceFor(TaskDefinitionResource.class)
public class TaskDefinitionController {

	private final TaskDefinitionRepository repository;

	private final TaskSaveService taskSaveService;

	private final TaskDeleteService taskDeleteService;

	private final DataflowTaskExplorer explorer;

	private final TaskDefinitionAssemblerProvider<? extends TaskDefinitionResource> taskDefinitionAssemblerProvider;

	/**
	 * Creates a {@code TaskDefinitionController} that delegates
	 * <ul>
	 * <li>CRUD operations to the provided {@link TaskDefinitionRepository}</li>
	 * <li>task status checks to the provided {@link TaskLauncher}</li>
	 * </ul>
	 *
	 * @param taskExplorer                    used to look up TaskExecutions.
	 * @param repository                      the repository this controller will use for task CRUD operations.
	 * @param taskSaveService                 handles Task saving related operations.
	 * @param taskDeleteService               handles Task deletion related operations.
	 * @param taskDefinitionAssemblerProvider the task definition assembler provider to use.
	 */
	public TaskDefinitionController(DataflowTaskExplorer taskExplorer, TaskDefinitionRepository repository,
									TaskSaveService taskSaveService, TaskDeleteService taskDeleteService,
									TaskDefinitionAssemblerProvider<? extends TaskDefinitionResource> taskDefinitionAssemblerProvider) {
		Assert.notNull(taskExplorer, "taskExplorer must not be null");
		Assert.notNull(repository, "repository must not be null");
		Assert.notNull(taskSaveService, "taskSaveService must not be null");
		Assert.notNull(taskDeleteService, "taskDeleteService must not be null");
		Assert.notNull(taskDefinitionAssemblerProvider, "taskDefinitionAssemblerProvider must not be null");
		this.explorer = taskExplorer;
		this.repository = repository;
		this.taskSaveService = taskSaveService;
		this.taskDeleteService = taskDeleteService;
		this.taskDefinitionAssemblerProvider = taskDefinitionAssemblerProvider;
	}

	/**
	 * Register a task definition for future execution.
	 *
	 * @param name        name the name of the task
	 * @param dsl         DSL definition for the task
	 * @param description description of the task definition
	 * @return the task definition
	 */
	@PostMapping("")
	public TaskDefinitionResource save(
			@RequestParam String name,
			@RequestParam("definition") String dsl,
			@RequestParam(defaultValue = "") String description
	) {
		TaskDefinition taskDefinition = new TaskDefinition(name, dsl, description);
		taskSaveService.saveTaskDefinition(taskDefinition);
		return this.taskDefinitionAssemblerProvider.getTaskDefinitionAssembler(false).toModel(new TaskExecutionAwareTaskDefinition(taskDefinition));
	}

	/**
	 * Delete the task from the repository so that it can no longer be executed.
	 *
	 * @param name    name of the task to be deleted
	 * @param cleanup optional cleanup indicator.
	 */
	@DeleteMapping("/{name}")
	@ResponseStatus(HttpStatus.OK)
	public void destroyTask(
			@PathVariable String name,
			@RequestParam(required = false) Boolean cleanup
	) {
		boolean taskExecutionCleanup = (cleanup != null && cleanup) ? cleanup : false;
		this.taskDeleteService.deleteTaskDefinition(name, taskExecutionCleanup);
	}

	/**
	 * Delete all task from the repository.
	 */
	@DeleteMapping("")
	@ResponseStatus(HttpStatus.OK)
	public void destroyAll() {
		taskDeleteService.deleteAll();
	}

	/**
	 * Return a page-able list of {@link TaskDefinitionResource} defined tasks.
	 *
	 * @param pageable    page-able collection of {@code TaskDefinitionResource}
	 * @param search      optional findByTaskNameContains parameter (Deprecated: please use taskName instead)
	 * @param taskName    optional findByTaskNameContains parameter
	 * @param dslText     optional findByDslText parameter
	 * @param description optional findByDescription parameter
	 * @param manifest    optional manifest flag to indicate whether the latest task execution requires task manifest update
	 * @param assembler   assembler for the {@link TaskDefinition}
	 * @return a list of task definitions
	 */
	@GetMapping("")
	@ResponseStatus(HttpStatus.OK)
	public PagedModel<? extends TaskDefinitionResource> list(
			Pageable pageable,
			@RequestParam(required = false) @Deprecated String search,
			@RequestParam(required = false) String taskName,
			@RequestParam(required = false) String description,
			@RequestParam(required = false) boolean manifest,
			@RequestParam(required = false) String dslText,
			PagedResourcesAssembler<TaskExecutionAwareTaskDefinition> assembler
	) {
		final Page<TaskDefinition> taskDefinitions;

		if (Stream.of(search, taskName, description, dslText).filter(Objects::nonNull).count() > 1L) {
			throw new TaskQueryParamException(new String[]{"taskName (or search)", "description", "dslText"});
		}

		if (taskName != null) {
			taskDefinitions = repository.findByTaskNameContains(taskName, pageable);
		} else if (search != null) {
			taskDefinitions = repository.findByTaskNameContains(search, pageable);
		} else if (description != null) {
			taskDefinitions = repository.findByDescriptionContains(description, pageable);
		} else if (dslText != null) {
			taskDefinitions = repository.findByDslTextContains(dslText, pageable);
		} else {
			taskDefinitions = repository.findAll(pageable);
		}

		final Map<String, TaskDefinition> taskDefinitionMap = taskDefinitions
				.stream()
				.collect(Collectors.toMap(TaskDefinition::getTaskName, Function.identity()));

		List<TaskExecution> taskExecutions = null;
		if (!taskDefinitionMap.isEmpty()) {
			taskExecutions = this.explorer.getLatestTaskExecutionsByTaskNames(taskDefinitionMap.keySet().toArray(new String[0]));
		}

		final Page<TaskExecutionAwareTaskDefinition> taskExecutionAwareTaskDefinitions = taskDefinitions
				.map(new TaskDefinitionConverter(taskExecutions));

		PagedModel<? extends TaskDefinitionResource> taskDefinitionResources = assembler.toModel(taskExecutionAwareTaskDefinitions,
				this.taskDefinitionAssemblerProvider.getTaskDefinitionAssembler(manifest));
		// Classify the composed task elements by iterating through the task definitions that are part of this page.
		updateComposedTaskElement(taskDefinitionResources.getContent(), taskDefinitions);
		return taskDefinitionResources;
	}


	private Collection<? extends TaskDefinitionResource> updateComposedTaskElement(Collection<? extends TaskDefinitionResource> taskDefinitionResources,
																				   Page<TaskDefinition> taskDefinitions) {
		Map<String, TaskDefinitionResource> taskNameResources = new HashMap<>();
		for (TaskDefinitionResource taskDefinitionResource : taskDefinitionResources) {
			taskNameResources.put(taskDefinitionResource.getName(), taskDefinitionResource);
		}
		for (TaskDefinition taskDefinition : taskDefinitions) {
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
	 * @param name     the name of an existing task definition (required)
	 * @param manifest indicator to include manifest in response.
	 * @return the task definition
	 */
	@GetMapping("/{name}")
	@ResponseStatus(HttpStatus.OK)
	public TaskDefinitionResource display(
			@PathVariable String name,
			@RequestParam(required = false) boolean manifest
	) {
		TaskDefinition definition = this.repository.findById(name)
				.orElseThrow(() -> new NoSuchTaskDefinitionException(name));
		final TaskExecution taskExecution = this.explorer.getLatestTaskExecutionForTaskName(name);
		final RepresentationModelAssembler<TaskExecutionAwareTaskDefinition, ? extends TaskDefinitionResource> taskAssembler =
				this.taskDefinitionAssemblerProvider.getTaskDefinitionAssembler(manifest);
		TaskDefinitionResource taskDefinitionResource;
		if (taskExecution != null) {
			taskDefinitionResource = taskAssembler.toModel(new TaskExecutionAwareTaskDefinition(definition, taskExecution));
		} else {
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

	class TaskDefinitionConverter implements Function<TaskDefinition, TaskExecutionAwareTaskDefinition> {
		final Map<String, TaskExecution> taskExecutions;

		public TaskDefinitionConverter(List<TaskExecution> taskExecutions) {
			super();
			if (taskExecutions != null) {
				this.taskExecutions = new HashMap<>(taskExecutions.size());
				for (TaskExecution taskExecution : taskExecutions) {
					this.taskExecutions.put(taskExecution.getTaskName(), taskExecution);
				}
			} else {
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
			} else {
				return new TaskExecutionAwareTaskDefinition(source);
			}
		}
	}

	;
}
