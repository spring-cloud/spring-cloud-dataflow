/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.h2.util.Task;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDefinition.TaskDefinitionBuilder;
import org.springframework.cloud.dataflow.server.controller.WhitelistProperties;
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
 * Default implementation of the {@link TaskService} interface. Provide service methods
 * for {@link Task}s.
 *
 * @author Michael Minella
 * @author Marius Bogoevici
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Janne Valkealahti
 * @author Gunnar Hillert
 */
public class DefaultTaskService implements TaskService {

	private final DataSourceProperties dataSourceProperties;

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

	private final WhitelistProperties whitelistProperties;

	/**
	 * Initializes the {@link DefaultTaskService}.
	 *
	 * @param dataSourceProperties the data source properties.
	 * @param repository the {@link TaskDefinitionRepository} this service will use for
	 * task CRUD operations.
	 * @param deploymentIdRepository the repository this service will use for deployment
	 * IDs.
	 * @param registry URI registry this service will use to look up app URIs.
	 * @param resourceLoader the {@link ResourceLoader} that will resolve URIs to
	 * {@link Resource}s.
	 * @param taskLauncher the launcher this service will use to launch task apps.
	 */
	public DefaultTaskService(DataSourceProperties dataSourceProperties,
			TaskDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository, UriRegistry registry,
			ResourceLoader resourceLoader, TaskLauncher taskLauncher,
			ApplicationConfigurationMetadataResolver metaDataResolver) {
		Assert.notNull(dataSourceProperties, "DataSourceProperties must not be null");
		Assert.notNull(repository, "TaskDefinitionRepository must not be null");
		Assert.notNull(deploymentIdRepository, "DeploymentIdRepository must not be null");
		Assert.notNull(registry, "UriRegistry must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Assert.notNull(taskLauncher, "TaskLauncher must not be null");
		Assert.notNull(metaDataResolver, "metaDataResolver must not be null");
		this.dataSourceProperties = dataSourceProperties;
		this.repository = repository;
		this.deploymentIdRepository = deploymentIdRepository;
		this.registry = registry;
		this.taskLauncher = taskLauncher;
		this.resourceLoader = resourceLoader;
		this.whitelistProperties = new WhitelistProperties(metaDataResolver);
	}

	private TaskDefinition updateTaskProperties(TaskDefinition taskDefinition) {
		TaskDefinitionBuilder builder = TaskDefinitionBuilder.from(taskDefinition);
		builder.setProperty("spring.datasource.url", dataSourceProperties.getUrl());
		builder.setProperty("spring.datasource.username",
				dataSourceProperties.getUsername());
		// password may be empty
		if (StringUtils.hasText(dataSourceProperties.getPassword())) {
			builder.setProperty("spring.datasource.password",
					dataSourceProperties.getPassword());
		}
		builder.setProperty("spring.datasource.driverClassName",
				dataSourceProperties.getDriverClassName());
		return builder.build();
	}

	@Override
	public void executeTask(String taskName, Map<String, String> runtimeProperties,
			List<String> runtimeParams) {
		Assert.hasText(taskName, "The provided taskName must not be null or empty.");
		Assert.notNull(runtimeProperties,
				"The provided runtimeProperties must not be null.");
		TaskDefinition taskDefinition = this.repository.findOne(taskName);
		if (taskDefinition == null) {
			throw new NoSuchTaskDefinitionException(taskName);
		}

		taskDefinition = this.updateTaskProperties(taskDefinition);

		Map<String, String> deploymentProperties = extractAppDeploymentProperties(taskDefinition, runtimeProperties);
		URI uri = this.registry.find(String.format("task.%s",
				taskDefinition.getRegisteredAppName()));
		Resource resource = this.resourceLoader.getResource(uri.toString());

		taskDefinition = qualifyProperties(taskDefinition, resource);

		AppDeploymentRequest request = taskDefinition.createDeploymentRequest(resource,
				deploymentProperties, runtimeParams);

		String id = this.taskLauncher.launch(request);
		if (!StringUtils.hasText(id)) {
			throw new IllegalStateException("Deployment ID is null for the task:"
					+ taskName);
		}
		String deploymentKey = DeploymentKey.forTaskDefinition(taskDefinition);
		if (deploymentIdRepository.findOne(deploymentKey) == null) {
			this.deploymentIdRepository.save(deploymentKey, id);
		}
	}

	private static Map<String, String> extractAppDeploymentProperties(TaskDefinition taskDefinition,
			Map<String, String> taskDeploymentProperties) {
		String appPrefix = String.format("app.%s.", taskDefinition.getRegisteredAppName());
		Map<String, String> deploymentProperties = new HashMap<>();
		for (Entry<String, String> entry : taskDeploymentProperties.entrySet()) {
			if (entry.getKey().startsWith(appPrefix)) {
				deploymentProperties.put(entry.getKey().substring(appPrefix.length()), entry.getValue());
			}
		}
		return deploymentProperties;
	}

	/**
	 * Return a copy of a given task definition where short form parameters have been expanded to their long form
	 * (amongst the whitelisted supported properties of the app) if applicable.
	 */
	/*default*/ TaskDefinition qualifyProperties(TaskDefinition original, Resource resource) {
		TaskDefinition.TaskDefinitionBuilder builder = TaskDefinition.TaskDefinitionBuilder.from(original);
		return builder.setProperties(whitelistProperties.qualifyProperties(original.getProperties(), resource)).build();
	}

}
