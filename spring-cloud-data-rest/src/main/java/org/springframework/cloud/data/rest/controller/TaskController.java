/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.cloud.data.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.data.core.TaskDefinition;
import org.springframework.cloud.data.rest.repository.TaskDefinitionRepository;
import org.springframework.cloud.data.rest.resource.TaskDefinitionResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Michael Minella
 */
@RestController
@RequestMapping("/tasks")
@ExposesResourceFor(TaskDefinitionResource.class)
public class TaskController {

	private final Assembler taskAssembler = new Assembler();

	@Autowired
	private TaskDefinitionRepository repository;

	@RequestMapping(value = "/", method = RequestMethod.POST)
	public void save(@RequestParam("name") String name,
			@RequestParam("definition") String dsl) {
		repository.save(TaskDefinition.getInstance(name, dsl));
	}

	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void destroyTask(@PathVariable("name") String name) {
		repository.delete(name);
	}

	@RequestMapping(value="/definitions", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<TaskDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<TaskDefinition> assembler) {
		return assembler.toResource(repository.findAll(pageable), taskAssembler);
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts {@link TaskDefinition}s to {@link TaskDefinitionResource}s.
	 */
	class Assembler extends ResourceAssemblerSupport<TaskDefinition, TaskDefinitionResource> {

		public Assembler() {
			super(TaskController.class, TaskDefinitionResource.class);
		}

		@Override
		public TaskDefinitionResource toResource(TaskDefinition taskDefinition) {
			return createResourceWithId(taskDefinition.getTaskName(), taskDefinition);
		}

		@Override
		public TaskDefinitionResource instantiateResource(TaskDefinition taskDefinition) {
			return new TaskDefinitionResource(taskDefinition.getTaskName(),
					taskDefinition.getTask());
		}
	}
}