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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.cloud.dataflow.core.PlatformTaskExecutionInformation;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecutionRel;
import org.springframework.cloud.dataflow.rest.resource.CurrentTaskExecutionsResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.rest.util.TaskSanitizer;
import org.springframework.cloud.dataflow.server.controller.support.TaskExecutionControllerDeleteAction;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
 * Controller for operations on
 * {@link org.springframework.cloud.task.repository.TaskExecution}. This includes
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
@RequestMapping("/tasks/executions")
@ExposesResourceFor(TaskExecutionResource.class)
public class TaskExecutionController {

	private final Assembler taskAssembler = new Assembler();

	private final TaskExecutionService taskExecutionService;

	private final TaskExecutionInfoService taskExecutionInfoService;

	private final TaskDeleteService taskDeleteService;

	private final TaskExplorer explorer;

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final TaskSanitizer taskSanitizer = new TaskSanitizer();

	private static final List<String> allowedSorts = Arrays.asList("TASK_EXECUTION_ID", "START_TIME", "END_TIME",
			"TASK_NAME", "EXIT_CODE", "EXIT_MESSAGE", "ERROR_MESSAGE", "LAST_UPDATED", "EXTERNAL_EXECUTION_ID",
			"PARENT_EXECUTION_ID");

	/**
	 * Creates a {@code TaskExecutionController} that retrieves Task Execution information
	 * from a the {@link TaskExplorer}
	 *
	 * @param explorer the explorer this controller will use for retrieving task execution
	 *     information.
	 * @param taskExecutionService used to launch tasks
	 * @param taskDefinitionRepository the task definition repository
	 * @param taskExecutionInfoService the task execution information service
	 * @param taskDeleteService the task deletion service
	 */
	public TaskExecutionController(TaskExplorer explorer, TaskExecutionService taskExecutionService,
			TaskDefinitionRepository taskDefinitionRepository, TaskExecutionInfoService taskExecutionInfoService,
			TaskDeleteService taskDeleteService) {
		Assert.notNull(explorer, "explorer must not be null");
		Assert.notNull(taskExecutionService, "taskExecutionService must not be null");
		Assert.notNull(taskDefinitionRepository, "taskDefinitionRepository must not be null");
		Assert.notNull(taskExecutionInfoService, "taskDefinitionRetriever must not be null");
		Assert.notNull(taskDeleteService, "taskDeleteService must not be null");
		this.taskExecutionService = taskExecutionService;
		this.explorer = explorer;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskExecutionInfoService = taskExecutionInfoService;
		this.taskDeleteService = taskDeleteService;
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
	public PagedModel<TaskExecutionResource> list(Pageable pageable,
			PagedResourcesAssembler<TaskJobExecutionRel> assembler) {
		validatePageable(pageable);
		Page<TaskExecution> taskExecutions = this.explorer.findAll(pageable);
		Page<TaskJobExecutionRel> result = getPageableRelationships(taskExecutions, pageable);
		return assembler.toModel(result, this.taskAssembler);
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
	public PagedModel<TaskExecutionResource> retrieveTasksByName(@RequestParam("name") String taskName,
			Pageable pageable, PagedResourcesAssembler<TaskJobExecutionRel> assembler) {
		validatePageable(pageable);
		this.taskDefinitionRepository.findById(taskName)
				.orElseThrow(() -> new NoSuchTaskDefinitionException(taskName));
		Page<TaskExecution> taskExecutions = this.explorer.findTaskExecutionsByName(taskName, pageable);
		Page<TaskJobExecutionRel> result = getPageableRelationships(taskExecutions, pageable);
		return assembler.toModel(result, this.taskAssembler);
	}

	/**
	 * Request the launching of an existing task definition. The task definition will be created from a registered task application
	 * if `spring.cloud.dataflow.task.auto-create-task-definitions` is true.
	 * The name must be included in the path.
	 *
	 * @param taskName the name of the task to be executed (required)
	 * @param properties the runtime properties for the task, as a comma-delimited list of
	 *     key=value pairs
	 * @param arguments the runtime commandline arguments
	 * @return the taskExecutionId for the executed task
	 */
	@RequestMapping(value = "", method = RequestMethod.POST, params = "name")
	@ResponseStatus(HttpStatus.CREATED)
	public long launch(@RequestParam("name") String taskName,
			@RequestParam(required = false) String properties,
			@RequestParam(required = false) String arguments) {
		Map<String, String> propertiesToUse = DeploymentPropertiesUtils.parse(properties);
		List<String> argumentsToUse = DeploymentPropertiesUtils.parseArgumentList(arguments, " ");

		return this.taskExecutionService.executeTask(taskName, propertiesToUse, argumentsToUse);
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
		taskExecution = this.taskSanitizer.sanitizeTaskExecutionArguments(taskExecution);
		TaskManifest taskManifest = this.taskExecutionService.findTaskManifestById(id);
		taskManifest = this.taskSanitizer.sanitizeTaskManifest(taskManifest);
		TaskJobExecutionRel taskJobExecutionRel = new TaskJobExecutionRel(taskExecution,
				new ArrayList<>(this.explorer.getJobExecutionIdsByTaskExecutionId(taskExecution.getExecutionId())),
				taskManifest);
		return this.taskAssembler.toModel(taskJobExecutionRel);
	}

	@RequestMapping(value = "/current", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Collection<CurrentTaskExecutionsResource> getCurrentTaskExecutionsInfo() {
		List<PlatformTaskExecutionInformation> executionInformation = taskExecutionInfoService
				.findAllPlatformTaskExecutionInformation().getTaskExecutionInformation();
		List<CurrentTaskExecutionsResource> resources = new ArrayList<>();

		executionInformation.forEach(platformTaskExecutionInformation -> {
			CurrentTaskExecutionsResource currentTaskExecutionsResource =
			CurrentTaskExecutionsResource.fromTaskExecutionInformation(platformTaskExecutionInformation);
			resources.add(currentTaskExecutionsResource);
		});

		return resources;
	}

	/**
	 * Cleanup resources associated with one or more task executions, specified by id(s). The
	 * optional {@code actions} parameter can be used to not only clean up task execution resources,
	 * but can also trigger the deletion of task execution and job data in the persistence store.
	 *
	 * @param ids The id of the {@link TaskExecution}s to clean up
	 * @param actions Defaults to "CLEANUP" if not specified
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void cleanup(
			@PathVariable("id") Set<Long> ids,
			@RequestParam(defaultValue = "CLEANUP", name="action") TaskExecutionControllerDeleteAction[] actions) {

		final Set<TaskExecutionControllerDeleteAction> actionsAsSet = new HashSet<>(Arrays.asList(actions));

		this.taskDeleteService.cleanupExecutions(actionsAsSet, ids);
	}

	/**
	 * Stop a set of task executions.
	 *
	 * @param ids the ids of the {@link TaskExecution}s to stop
	 * @param platform the platform name
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public void stop(@PathVariable("id") Set<Long> ids,
	@RequestParam(defaultValue = "", name="platform") String platform) {
		this.taskExecutionService.stopTaskExecution(ids, platform);
	}

	private Page<TaskJobExecutionRel> getPageableRelationships(Page<TaskExecution> taskExecutions, Pageable pageable) {
		List<TaskJobExecutionRel> taskJobExecutionRels = new ArrayList<>();
		for (TaskExecution taskExecution : taskExecutions.getContent()) {
			TaskManifest taskManifest = this.taskExecutionService.findTaskManifestById(taskExecution.getExecutionId());
			taskManifest = this.taskSanitizer.sanitizeTaskManifest(taskManifest);
			List<Long> jobExecutionIds = new ArrayList<>(
					this.explorer.getJobExecutionIdsByTaskExecutionId(taskExecution.getExecutionId()));
			taskJobExecutionRels
					.add(new TaskJobExecutionRel(this.taskSanitizer.sanitizeTaskExecutionArguments(taskExecution),
							jobExecutionIds,
							taskManifest));
		}
		return new PageImpl<>(taskJobExecutionRels, pageable, taskExecutions.getTotalElements());
	}

	private static void validatePageable(Pageable pageable) {
		if (pageable != null) {
			Sort sort = pageable.getSort();
			if (sort != null) {
				for (Sort.Order order : sort) {
					String property = order.getProperty();
					if (property != null && !allowedSorts.contains(property.toUpperCase())) {
						throw new IllegalArgumentException("Sorting column " + order.getProperty() + " not allowed");
					}
				}
			}
		}
	}

	/**
	 * {@link org.springframework.hateoas.server.RepresentationModelAssembler} implementation that converts
	 * {@link TaskJobExecutionRel}s to {@link TaskExecutionResource}s.
	 */
	private static class Assembler extends RepresentationModelAssemblerSupport<TaskJobExecutionRel, TaskExecutionResource> {

		public Assembler() {
			super(TaskExecutionController.class, TaskExecutionResource.class);
		}

		@Override
		public TaskExecutionResource toModel(TaskJobExecutionRel taskJobExecutionRel) {
			return createModelWithId(taskJobExecutionRel.getTaskExecution().getExecutionId(), taskJobExecutionRel);
		}

		@Override
		public TaskExecutionResource instantiateModel(TaskJobExecutionRel taskJobExecutionRel) {
			return new TaskExecutionResource(taskJobExecutionRel);
		}
	}

}
