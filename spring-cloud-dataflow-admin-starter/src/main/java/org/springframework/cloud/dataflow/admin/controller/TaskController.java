/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.admin.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.dataflow.admin.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistration;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistry;
import org.springframework.cloud.dataflow.core.ArtifactCoordinates;
import org.springframework.cloud.dataflow.core.ArtifactType;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link TaskDefinition}.  This includes CRUD and deployment
 * operations.
 *
 * @author Michael Minella
 * @author Marius Bogoevici
 */
@RestController
@RequestMapping("/tasks")
@ExposesResourceFor(TaskDefinitionResource.class)
public class TaskController {

	private final String DEFAULT_TASK_DATASOURCE_URL = "jdbc:h2:tcp://localhost:9092/mem:dataflow";

	private final String DEFAULT_TASK_DATASOURCE_USER_NAME = "sa";

	private final String DEFAULT_TASK_DATASOURCE_DRIVER_CLASS_NAME = "org.h2.Driver";

	private final Assembler taskAssembler = new Assembler();

	@Autowired
	private TaskDefinitionRepository repository;

	@Autowired
	@Qualifier("taskModuleDeployer")
	private ModuleDeployer moduleDeployer;

	@Value("${spring.datasource.url:#{null}}")
	private String dataSourceUrl;

	@Value("${spring.datasource.username:#{null}}")
	private String dataSourceUserName;

	@Value("${spring.datasource.password:#{null}}")
	private String dataSourcePassword;

	@Value("${spring.datasource.driverClassName:#{null}}")
	private String dataSourceDriverClassName;

	/**
	 * The artifact registry this controller will use to look up modules.
	 */
	private final ArtifactRegistry registry;

	/**
	 * Creates a {@code TaskController} that delegates
	 * <ul>
	 *     <li>CRUD operations to the provided {@link TaskDefinitionRepository}</li>
	 *     <li>module coordinate retrieval to the provided {@link ArtifactRegistry}</li>
	 *     <li>deployment/launching operations to the provided {@link ModuleDeployer}</li>
	 * </ul>
	 *
	 * @param repository the repository this controller will use for task CRUD operations.
	 * @param registry artifact registry this controller will use to look up modules.
	 * @param deployer the deployer this controller will use to deploy/launch task modules.
	 */
	@Autowired
	public TaskController(TaskDefinitionRepository repository, ArtifactRegistry registry,
			@Qualifier("taskModuleDeployer") ModuleDeployer deployer) {
		Assert.notNull(repository, "repository must not be null");
		Assert.notNull(registry, "registry must not be null");
		Assert.notNull(deployer, "deployer must not be null");
		this.repository = repository;
		this.registry = registry;
		this.moduleDeployer = deployer;
	}

	/**
	 * Register a task for future deployment/execution.
	 *
	 * @param name the name of the task
	 * @param dsl DSL definition for the task
	 */
	@RequestMapping(value = "/definitions", method = RequestMethod.POST)
	public void save(@RequestParam("name") String name,
			@RequestParam("definition") String dsl) {
		TaskDefinition previous = repository.findOne(name);
		if (previous != null) {
			throw new ResourceAlreadyExistsException(name, "task definition");
		}
		repository.save(new TaskDefinition(name, dsl));
	}

	/**
	 * Delete the task from the repository so that it can no longer be executed.
	 *
	 * @param name name of the task to be deleted
	 */
	@RequestMapping(value = "/definitions/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void destroyTask(@PathVariable("name") String name) {
		findDefinitionOrFail(name);
		repository.delete(name);
	}

	private TaskDefinition findDefinitionOrFail(@PathVariable("name") String name) {
		TaskDefinition definition = repository.findOne(name);
		if (definition == null) {
			throw new ResourceNotFoundException(name, "task definition");
		}
		return definition;
	}

	/**
	 * Return a page-able list of {@link TaskDefinitionResource} defined tasks.
	 *
	 * @param pageable  page-able collection of {@code TaskDefinitionResource}.
	 * @param assembler assembler for the {@link TaskDefinition}
	 * @return a list of task definitions
	 */
	@RequestMapping(value="/definitions", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<TaskDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<TaskDefinition> assembler) {
		return assembler.toResource(repository.findAll(pageable), taskAssembler);
	}

	/**
	 * Request the launching of an existing task definition.  The name must be
	 * included in the path.
	 *
	 * @param name the name of the existing task to be executed (required)
	 * @param properties the runtime properties for the task, as a comma-delimited list of
	 * 					 key=value pairs
	 */
	@RequestMapping(value = "/deployments/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void deploy(@PathVariable("name") String name, @RequestParam(required = false) String properties) {
		TaskDefinition taskDefinition = this.findDefinitionOrFail(name);

		ModuleDefinition module = taskDefinition.getModuleDefinition();
		ArtifactRegistration registration = this.registry.find(module.getName(), ArtifactType.task);
		if (registration == null) {
			throw new IllegalArgumentException(String.format(
					"Module %s of type %s not found in registry", module.getName(), ArtifactType.task));
		}
		ArtifactCoordinates coordinates = registration.getCoordinates();

		Map<String, String> deploymentProperties = new HashMap<>();
		module = updateTaskProperties(module, module.getName() );
		deploymentProperties.putAll(DeploymentPropertiesUtils.parse(properties));
		deploymentProperties.put(ModuleDeployer.GROUP_DEPLOYMENT_ID, taskDefinition.getName()
				+ "-" + System.currentTimeMillis());

		this.moduleDeployer.deploy(
				new ModuleDeploymentRequest(module, coordinates, deploymentProperties));
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
			return createResourceWithId(taskDefinition.getName(), taskDefinition);
		}

		@Override
		public TaskDefinitionResource instantiateResource(TaskDefinition taskDefinition) {
			ModuleDeploymentId id =
					ModuleDeploymentId.fromModuleDefinition(taskDefinition.getModuleDefinition());
			TaskDefinitionResource taskDefinitionResource = new TaskDefinitionResource(taskDefinition.getName(),
					taskDefinition.getDslText());
			taskDefinitionResource.setStatus(moduleDeployer.status(id).getState().name());
			return taskDefinitionResource;
		}
	}

	private ModuleDefinition updateTaskProperties(ModuleDefinition moduleDefinition, String taskDefinitionName) {
		ModuleDefinition.Builder builder = ModuleDefinition.Builder.from(moduleDefinition);
		builder.setParameter("spring.datasource.url",
				(StringUtils.hasText(dataSourceUrl)) ? dataSourceUrl :
						DEFAULT_TASK_DATASOURCE_URL);

		builder.setParameter("spring.datasource.username",
				(StringUtils.hasText(dataSourceUserName)) ? dataSourceUserName :
						DEFAULT_TASK_DATASOURCE_USER_NAME);

		if(StringUtils.hasText(dataSourcePassword)) {//password may be empty
			builder.setParameter("spring.datasource.password", dataSourcePassword );
		}

		builder.setParameter("spring.datasource.driverClassName",
				(StringUtils.hasText(dataSourceDriverClassName)) ? dataSourceDriverClassName :
						DEFAULT_TASK_DATASOURCE_DRIVER_CLASS_NAME);

		return builder.build();
	}
}
