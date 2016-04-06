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

package org.springframework.cloud.dataflow.server.controller;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.cloud.dataflow.rest.resource.TaskDeploymentResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.repository.AppDeploymentKey;
import org.springframework.cloud.dataflow.server.repository.AppDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.hateoas.ExposesResourceFor;
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
 * Controller for operations for deployment operations.
 *
 * @author Michael Minella
 * @author Marius Bogoevici
 * @author Glenn Renfro
 * @author Mark Fisher
 */
@RestController
@RequestMapping("/tasks/deployments")
@ExposesResourceFor(TaskDeploymentResource.class)
public class TaskDeploymentController {

	private static final String DEFAULT_TASK_DATASOURCE_URL = "jdbc:h2:tcp://localhost:19092/mem:dataflow";

	private static final String DEFAULT_TASK_DATASOURCE_USER_NAME = "sa";

	private static final String DEFAULT_TASK_DATASOURCE_DRIVER_CLASS_NAME = "org.h2.Driver";


	private final TaskDefinitionRepository repository;

	/**
	 * The repository this controller will use for app deployment operations.
	 */
	private final AppDeploymentRepository appDeploymentRepository;

	private final TaskLauncher taskLauncher;

	@Value("${spring.datasource.url:#{null}}")
	private String dataSourceUrl;

	@Value("${spring.datasource.username:#{null}}")
	private String dataSourceUserName;

	@Value("${spring.datasource.password:#{null}}")
	private String dataSourcePassword;

	@Value("${spring.datasource.driverClassName:#{null}}")
	private String dataSourceDriverClassName;

	/**
	 * The {@link UriRegistry} this controller will use to look up task app URIs.
	 */
	private final UriRegistry registry;

	/**
	 * The {@link ResourceLoader} that will resolve URIs to {@link Resource}s.
	 */
	private final ResourceLoader resourceLoader;

	/**
	 * Creates a {@code TaskDeploymentController} that delegates launching
	 * operations to the provided {@link TaskLauncher}
	 * @param repository the repository this controller will use for task CRUD operations.
	 * @param appDeploymentRepository the repository this controller will use for app deployment operations
	 * @param registry URI registry this controller will use to look up app URIs.
	 * @param resourceLoader the {@link ResourceLoader} that will resolve URIs to {@link Resource}s.
	 * @param taskLauncher the launcher this controller will use to launch task apps.
	 */
	public TaskDeploymentController(TaskDefinitionRepository repository, AppDeploymentRepository appDeploymentRepository,
			UriRegistry registry, ResourceLoader resourceLoader, TaskLauncher taskLauncher) {
		Assert.notNull(repository, "repository must not be null");
		Assert.notNull(appDeploymentRepository, "appDeploymentRepository must not be null");
		Assert.notNull(registry, "registry must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Assert.notNull(taskLauncher, "TaskLauncher must not be null");
		this.repository = repository;
		this.appDeploymentRepository = appDeploymentRepository;
		this.registry = registry;
		this.resourceLoader = resourceLoader;
		this.taskLauncher = taskLauncher;
	}

	/**
	 * Request the launching of an existing task definition.  The name must be
	 * included in the path.
	 *
	 * @param name the name of the existing task to be executed (required)
	 * @param properties the runtime properties for the task, as a comma-delimited list of
	 * 					 key=value pairs
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void deploy(@PathVariable("name") String name, @RequestParam(required = false) String properties) {
		TaskDefinition taskDefinition = this.repository.findOne(name);
		if (taskDefinition == null) {
			throw new NoSuchTaskDefinitionException(name);
		}
		ModuleDefinition module = taskDefinition.getModuleDefinition();

		Map<String, String> deploymentProperties = new HashMap<>();
		module = updateTaskProperties(module, module.getName() );
		deploymentProperties.putAll(DeploymentPropertiesUtils.parse(properties));
		deploymentProperties.put(ModuleDeployer.GROUP_DEPLOYMENT_ID, taskDefinition.getName()
				+ "-" + System.currentTimeMillis());

		AppDefinition definition = new AppDefinition(module.getLabel(), module.getParameters());
		URI uri = this.registry.find(String.format("task.%s", module.getName()));
		Resource resource = this.resourceLoader.getResource(uri.toString());
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, deploymentProperties);
		String id = this.taskLauncher.launch(request);
		appDeploymentRepository.save(new AppDeploymentKey(taskDefinition, module), id);
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
