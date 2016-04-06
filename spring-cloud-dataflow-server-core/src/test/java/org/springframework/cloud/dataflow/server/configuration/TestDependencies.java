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

package org.springframework.cloud.dataflow.server.configuration;

import static org.mockito.Mockito.mock;
import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.artifact.registry.AppRegistry;
import org.springframework.cloud.dataflow.completion.CompletionConfiguration;
import org.springframework.cloud.dataflow.server.controller.RestControllerAdvice;
import org.springframework.cloud.dataflow.server.controller.StreamDefinitionController;
import org.springframework.cloud.dataflow.server.controller.StreamDeploymentController;
import org.springframework.cloud.dataflow.server.controller.TaskDefinitionController;
import org.springframework.cloud.dataflow.server.controller.TaskDeploymentController;
import org.springframework.cloud.dataflow.server.controller.TaskExecutionController;
import org.springframework.cloud.dataflow.server.registry.DataFlowUriRegistryPopulator;
import org.springframework.cloud.dataflow.server.registry.DataFlowUriRegistryPopulatorProperties;
import org.springframework.cloud.dataflow.server.repository.AppDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryAppDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryStreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryTaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.registry.InMemoryUriRegistry;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.resource.registry.UriRegistryPopulator;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * @author Michael Minella
 * @author Mark Fisher
 */
@Configuration
@EnableSpringDataWebSupport
@EnableHypermediaSupport(type = HAL)
@Import(CompletionConfiguration.class)
@EnableWebMvc
public class TestDependencies extends WebMvcConfigurationSupport {

	private final MavenProperties mavenProperties = new MavenProperties();

	@Bean
	public RestControllerAdvice restControllerAdvice() {
		return new RestControllerAdvice();
	}

	@Bean
	public ResourceLoader resourceLoader() {
		return new MavenResourceLoader(mavenProperties);
	}

	@Bean
	public StreamDeploymentController streamDeploymentController(StreamDefinitionRepository repository,
			AppDeploymentRepository appDeploymentRepository, AppRegistry registry) {
		return new StreamDeploymentController(repository, appDeploymentRepository, registry, appDeployer());
	}

	@Bean
	public StreamDefinitionController streamDefinitionController(StreamDefinitionRepository repository,
			AppDeploymentRepository appDeploymentRepository, StreamDeploymentController deploymentController) {
		return new StreamDefinitionController(repository, appDeploymentRepository, deploymentController, appDeployer());
	}

	@Bean
	public TaskDeploymentController taskController(TaskDefinitionRepository repository, AppDeploymentRepository appDeploymentRepository,
			UriRegistry registry) {
		return new TaskDeploymentController(repository, appDeploymentRepository, registry, resourceLoader(), taskLauncher());
	}

	@Bean
	public TaskDefinitionController taskDefinitionController(TaskDefinitionRepository repository) {
		return new TaskDefinitionController(repository, taskLauncher());
	}

	public TaskExecutionController taskExecutionController(TaskExplorer explorer) {
		return new TaskExecutionController(explorer);
	}

	@Bean
	public UriRegistry uriRegistry() {
		return new InMemoryUriRegistry();
	}

	@Bean
	public AppRegistry appRegistry() {
		return new AppRegistry(uriRegistry(), resourceLoader());
	}

	@Bean
	public UriRegistryPopulator uriRegistryPopulator() {
		return new UriRegistryPopulator();
	}

	@Bean
	public DataFlowUriRegistryPopulator dataflowUriRegistryPopulator() {
		return new DataFlowUriRegistryPopulator(uriRegistry(), uriRegistryPopulator(),
				new DataFlowUriRegistryPopulatorProperties());
	}

	@Bean
	public AppDeployer appDeployer() {
		return mock(AppDeployer.class);
	}

	@Bean
	public TaskLauncher taskLauncher() {
		return mock(TaskLauncher.class);
	}

	@Bean
	public StreamDefinitionRepository streamDefinitionRepository() {
		return new InMemoryStreamDefinitionRepository();
	}

	@Bean
	public TaskDefinitionRepository taskDefinitionRepository() {
		return new InMemoryTaskDefinitionRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	public AppDeploymentRepository appDeploymentRepository() {
		return new InMemoryAppDeploymentRepository();
	}
}
