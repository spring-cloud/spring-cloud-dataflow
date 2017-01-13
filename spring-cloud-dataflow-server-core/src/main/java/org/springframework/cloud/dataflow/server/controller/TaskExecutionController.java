/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecutionRel;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
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
 * Controller for operations on {@link org.springframework.cloud.task.repository.TaskExecution}.
 * This includes obtaining task execution information from the task explorer.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/tasks/executions")
@ExposesResourceFor(TaskExecutionResource.class)
public class TaskExecutionController {

	private final Assembler taskAssembler = new Assembler();

	private final TaskExplorer explorer;

	private final TaskDefinitionRepository taskDefinitionRepository;

	/**
	 * Creates a {@code TaskExecutionController} that retrieves Task Execution information
	 * from a the {@link TaskExplorer}
	 *
	 * @param explorer the explorer this controller will use for retrieving
	 *                   task execution information.
	 * @param taskDefinitionRepository the task definition repository
	 */
	public TaskExecutionController(TaskExplorer explorer, TaskDefinitionRepository taskDefinitionRepository) {
		Assert.notNull(explorer, "explorer must not be null");
		Assert.notNull(taskDefinitionRepository, "task definition repository must not be null");
		this.explorer = explorer;
		this.taskDefinitionRepository = taskDefinitionRepository;
	}

	/**
	 * Return a page-able list of {@link TaskExecutionResource} defined tasks.
	 *
	 * @param pageable  page-able collection of {@code TaskExecution}s.
	 * @param assembler for the {@link TaskExecution}s
	 * @return a list of task executions
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<TaskExecutionResource> list(Pageable pageable,
			PagedResourcesAssembler<TaskJobExecutionRel> assembler) {
		Page<TaskExecution> taskExecutions = explorer.findAll(pageable);
		Page<TaskJobExecutionRel> result = getPageableRelationships(taskExecutions, pageable);
		return assembler.toResource(result, taskAssembler);
	}

	/**
	 * Retrieve all task executions with the task name specified
	 *
	 * @param taskName name of the task
	 * @param pageable  page-able collection of {@code TaskExecution}s.
	 * @param assembler for the {@link TaskExecution}s
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, params = "name")
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<TaskExecutionResource> retrieveTasksByName(
			@RequestParam("name") String taskName, Pageable pageable,
				PagedResourcesAssembler<TaskJobExecutionRel> assembler) {
		if (this.taskDefinitionRepository.findOne(taskName) == null) {
			throw new NoSuchTaskDefinitionException(taskName);
		}
		Page<TaskExecution> taskExecutions = explorer.findTaskExecutionsByName(taskName, pageable);
		Page<TaskJobExecutionRel> result = getPageableRelationships(taskExecutions, pageable);
		return assembler.toResource(result, taskAssembler);
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
		if(taskExecution == null){
			throw new NoSuchTaskExecutionException(id);
		}
		TaskJobExecutionRel taskJobExecutionRel = new TaskJobExecutionRel(taskExecution,
				new ArrayList<>(explorer.getJobExecutionIdsByTaskExecutionId(
						taskExecution.getExecutionId())));
		return taskAssembler.toResource(taskJobExecutionRel);
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts {@link TaskJobExecutionRel}s to {@link TaskExecutionResource}s.
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

	private Page<TaskJobExecutionRel> getPageableRelationships(Page<TaskExecution> taskExecutions, Pageable pageable){
		List<TaskJobExecutionRel> taskJobExecutionRels = new ArrayList<>();
		for(TaskExecution taskExecution: taskExecutions.getContent()) {
			taskJobExecutionRels.add( new TaskJobExecutionRel(taskExecution,
					new ArrayList<>(explorer.getJobExecutionIdsByTaskExecutionId(
							taskExecution.getExecutionId()))));
		}
		return new PageImpl<>(taskJobExecutionRels,pageable,taskExecutions.getTotalElements());
	}
}
