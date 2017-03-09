/*
 * Copyright 2017 the original author or authors.
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

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.ComposedTaskNode;
import org.springframework.cloud.dataflow.core.dsl.ComposedTaskParser;
import org.springframework.cloud.dataflow.core.dsl.ComposedTaskValidationException;
import org.springframework.cloud.dataflow.core.dsl.ComposedTaskValidationProblem;
import org.springframework.cloud.dataflow.core.dsl.DSLMessage;
import org.springframework.cloud.dataflow.rest.resource.ComposedTaskResource;
import org.springframework.cloud.dataflow.server.config.features.ComposedTaskProperties;
import org.springframework.cloud.dataflow.server.controller.support.ComposedTaskValidator;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentKey;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Composed Task creation operations.
 *
 * @author Glenn Renfro
 */

@RestController
@RequestMapping("/tasks/composed-definitions")
@ExposesResourceFor(ComposedTaskResource.class)
public class ComposedTaskController {

	private final Assembler taskAssembler = new Assembler();

	private TaskDefinitionRepository repository;

	/**
	 * The repository this controller will use for deployment IDs.
	 */
	private final DeploymentIdRepository deploymentIdRepository;

	private TaskLauncher taskLauncher;


	/**
	 * The properties used to configure composed task definitions.
	 */
	private final ComposedTaskProperties composedTaskProperties;

	/**
	 * Creates a {@code TaskDefinitionController} that delegates
	 * <ul>
	 *     <li>CRUD operations to the provided {@link TaskDefinitionRepository}</li>
	 *     <li>task status checks to the provided {@link TaskLauncher}</li>
	 * </ul>
	 *
	 * @param repository the repository this controller will use for task CRUD operations.
	 * @param deploymentIdRepository the repository this controller will use for deployment IDs
	 * @param taskLauncher the TaskLauncher this controller will use to check task status.
	 * @param composedTaskProperties properties used to get composed task information.
	 */
	public ComposedTaskController(TaskDefinitionRepository repository, DeploymentIdRepository deploymentIdRepository,
			TaskLauncher taskLauncher,ComposedTaskProperties composedTaskProperties) {
		Assert.notNull(repository, "repository must not be null");
		Assert.notNull(deploymentIdRepository, "DeploymentIdRepository must not be null");
		Assert.notNull(taskLauncher, "taskLauncher must not be null");
		Assert.notNull(composedTaskProperties, "composedTaskProperties must not be null");
		this.repository = repository;
		this.deploymentIdRepository = deploymentIdRepository;
		this.taskLauncher = taskLauncher;
		this.composedTaskProperties = composedTaskProperties;
	}

	/**
	 * Register a composed task definition for future execution.
	 *
	 * @param name name of the composed task
	 * @param graph DSL definition for the composed task
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	public ComposedTaskResource saveComposedTask(@RequestParam("name") String name,
			@RequestParam("definition") String graph) {
		TaskDefinition taskDefinition = new TaskDefinition(name, graph, true);
		TaskDefinition composedTaskDefinition = new TaskDefinition(
				taskDefinition.getName(),getComposedArgs(taskDefinition.getDslText()));
		validateComposedTaskElementsExist(name, graph);
		repository.save(composedTaskDefinition);
		return taskAssembler.toResource(taskDefinition);
	}

	private void validateComposedTaskElementsExist(String name, String graph) {
		ComposedTaskValidator composedTaskValidator =
				new ComposedTaskValidator(repository);
		ComposedTaskNode composedTaskNode = new ComposedTaskParser().parse(name, graph);
		composedTaskNode.accept(composedTaskValidator);
		List<ComposedTaskValidationProblem> composedTaskValidationProblems =
				composedTaskValidator.getInvalidDefinitions()
						.values().stream()
						.map(pos -> new ComposedTaskValidationProblem(
								graph, pos, DSLMessage.CT_ELEMENT_IN_COMPOSED_DEFINITION_DOES_NOT_EXIST))
						.collect(Collectors.toList());
		if (!composedTaskValidationProblems.isEmpty()) {
			throw new ComposedTaskValidationException(
					composedTaskNode, composedTaskValidationProblems);
		}
	}

	private String getComposedArgs(String graph) {
		return String.format("%s --graph=\"%s\"",
				composedTaskProperties.getTaskName(), escapeQuotes(graph));
	}

	private String escapeQuotes(String graph) {
		return graph.replace("\"", "\\\"");
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts {@link TaskDefinition}s to {@link ComposedTaskResource}s.
	 */
	class Assembler extends ResourceAssemblerSupport<TaskDefinition, ComposedTaskResource> {

		public Assembler() {
			super(ComposedTaskController.class, ComposedTaskResource.class);
		}

		@Override
		public ComposedTaskResource toResource(TaskDefinition taskDefinition) {
			return createResourceWithId(taskDefinition.getName(), taskDefinition);
		}

		@Override
		public ComposedTaskResource instantiateResource(TaskDefinition taskDefinition) {
			String key = DeploymentKey.forTaskDefinition(taskDefinition);
			String id = deploymentIdRepository.findOne(key);
			TaskStatus status = null;
			if (id != null) {
				status = taskLauncher.status(id);
			}
			String state = (status != null) ? status.getState().name() : "unknown";
			ComposedTaskResource composedTaskResource = new ComposedTaskResource(
					taskDefinition.getName(),
					taskDefinition.getDslText());
			composedTaskResource.setStatus(state);
			return composedTaskResource;
		}
	}
}
