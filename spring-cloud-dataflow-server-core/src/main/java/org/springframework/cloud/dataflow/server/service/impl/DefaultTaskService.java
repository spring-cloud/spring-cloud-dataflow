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

package org.springframework.cloud.dataflow.server.service.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.h2.util.Task;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDefinition.TaskDefinitionBuilder;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentKey;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link TaskService} interface. Provide service
 * methods for {@link Task}s.
 *
 * Several properties in this class are annotated with {@link Value} annotations:
 *
 * <ul>
 *   <li>spring.datasource.url
 *   <li>spring.datasource.username
 *   <li>spring.datasource.password
 *   <li>spring.datasource.driverClassName
 * </ul>
 *
 * All four properties default to {@code null} if not set.
 *
 * @author Michael Minella
 * @author Marius Bogoevici
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Janne Valkealahti
 * @author Gunnar Hillert
 */
public class DefaultTaskService implements TaskService {

	private static final String DEFAULT_TASK_DATASOURCE_URL = "jdbc:h2:tcp://localhost:19092/mem:dataflow";

	private static final String DEFAULT_TASK_DATASOURCE_USER_NAME = "sa";

	private static final String DEFAULT_TASK_DATASOURCE_DRIVER_CLASS_NAME = "org.h2.Driver";

	@Value("${spring.datasource.url:#{null}}")
	private String dataSourceUrl;

	@Value("${spring.datasource.username:#{null}}")
	private String dataSourceUserName;

	@Value("${spring.datasource.password:#{null}}")
	private String dataSourcePassword;

	@Value("${spring.datasource.driverClassName:#{null}}")
	private String dataSourceDriverClassName;

	/**
	 * The repository this service will use for deployment IDs.
	 */
	private final DeploymentIdRepository deploymentIdRepository;

	private final TaskLauncher taskLauncher;

	/**
	 * The {@link UriRegistry} this service will use to look up task app URIs.
	 */
	private final UriRegistry registry;

	/**
	 * The {@link ResourceLoader} that will resolve URIs to {@link Resource}s.
	 */
	private final ResourceLoader resourceLoader;

	private final TaskDefinitionRepository repository;

	/**
	 * Initializes the {@link DefaultTaskService}.
	 *
	 * @param repository the {@link TaskDefinitionRepository} this service will use for task CRUD operations.
	 * @param deploymentIdRepository the repository this service will use for deployment IDs
	 * @param registry URI registry this service will use to look up app URIs.
	 * @param resourceLoader the {@link ResourceLoader} that will resolve URIs to {@link Resource}s.
	 * @param taskLauncher the launcher this service will use to launch task apps.
	 */
	public DefaultTaskService(TaskDefinitionRepository repository, DeploymentIdRepository deploymentIdRepository,
			UriRegistry registry, ResourceLoader resourceLoader, TaskLauncher taskLauncher) {
		Assert.notNull(repository, "TaskDefinitionRepository must not be null");
		Assert.notNull(deploymentIdRepository, "DeploymentIdRepository must not be null");
		Assert.notNull(registry, "UriRegistry must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Assert.notNull(taskLauncher, "TaskLauncher must not be null");
		this.repository = repository;
		this.deploymentIdRepository = deploymentIdRepository;
		this.registry = registry;
		this.taskLauncher = taskLauncher;
		this.resourceLoader = resourceLoader;
	}

	private TaskDefinition updateTaskProperties(TaskDefinition taskDefinition) {
		TaskDefinitionBuilder builder = TaskDefinitionBuilder.from(taskDefinition);
		builder.setProperty("spring.datasource.url",
				(StringUtils.hasText(dataSourceUrl)) ? dataSourceUrl :
						DEFAULT_TASK_DATASOURCE_URL);

		builder.setProperty("spring.datasource.username",
				(StringUtils.hasText(dataSourceUserName)) ? dataSourceUserName :
						DEFAULT_TASK_DATASOURCE_USER_NAME);

		if(StringUtils.hasText(dataSourcePassword)) {//password may be empty
			builder.setProperty("spring.datasource.password", dataSourcePassword);
		}

		builder.setProperty("spring.datasource.driverClassName",
				(StringUtils.hasText(dataSourceDriverClassName)) ? dataSourceDriverClassName :
						DEFAULT_TASK_DATASOURCE_DRIVER_CLASS_NAME);

		return builder.build();
	}

	@Override
	public void executeTask(String taskName, Map<String, String> runtimeProperties, List<String> runtimeParams) {
		Assert.hasText(taskName, "The provided taskName must not be null or empty.");
		Assert.notNull(runtimeProperties, "The provided runtimeProperties must not be null.");
		TaskDefinition taskDefinition = this.repository.findOne(taskName);
		if (taskDefinition == null) {
			throw new NoSuchTaskDefinitionException(taskName);
		}
		Map<String, String> deploymentProperties = new HashMap<>();
		taskDefinition = this.updateTaskProperties(taskDefinition);
		deploymentProperties.putAll(runtimeProperties);
		URI uri = this.registry.find(String.format("task.%s", taskDefinition.getRegisteredAppName()));
		Resource resource = this.resourceLoader.getResource(uri.toString());
		AppDeploymentRequest request = taskDefinition.createDeploymentRequest(resource, deploymentProperties, runtimeParams);
		String id = this.taskLauncher.launch(request);
		String deploymentKey = DeploymentKey.forTaskDefinition(taskDefinition);
		if (deploymentIdRepository.findOne(deploymentKey) == null) {
			this.deploymentIdRepository.save(deploymentKey, id);
		}
	}
}
