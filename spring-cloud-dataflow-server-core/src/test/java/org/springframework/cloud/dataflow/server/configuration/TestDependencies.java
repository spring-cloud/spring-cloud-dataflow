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

package org.springframework.cloud.dataflow.server.configuration;

import static org.mockito.Mockito.mock;
import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.completion.CompletionConfiguration;
import org.springframework.cloud.dataflow.completion.StreamCompletionProvider;
import org.springframework.cloud.dataflow.completion.TaskCompletionProvider;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.controller.AppRegistryController;
import org.springframework.cloud.dataflow.server.controller.CompletionController;
import org.springframework.cloud.dataflow.server.controller.RestControllerAdvice;
import org.springframework.cloud.dataflow.server.controller.StreamDefinitionController;
import org.springframework.cloud.dataflow.server.controller.StreamDeploymentController;
import org.springframework.cloud.dataflow.server.controller.TaskDefinitionController;
import org.springframework.cloud.dataflow.server.controller.TaskDeploymentController;
import org.springframework.cloud.dataflow.server.controller.TaskExecutionController;
import org.springframework.cloud.dataflow.server.registry.DataFlowUriRegistryPopulator;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryDeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryStreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryTaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskService;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.registry.InMemoryUriRegistry;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.resource.registry.UriRegistryPopulator;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * @author Michael Minella
 * @author Mark Fisher
 * @author Gunnar Hillert
 */
@Configuration
@EnableSpringDataWebSupport
@EnableHypermediaSupport(type = HAL)
@Import(CompletionConfiguration.class)
@EnableWebMvc
@EnableConfigurationProperties(CommonApplicationProperties.class)
public class TestDependencies extends WebMvcConfigurationSupport {

	@Bean
	public RestControllerAdvice restControllerAdvice() {
		return new RestControllerAdvice();
	}

	@Bean
	public ResourceLoader resourceLoader() {
		MavenProperties mavenProperties = new MavenProperties();
		mavenProperties.setRemoteRepositories(new HashMap<>(Collections.singletonMap("springRepo",
				new MavenProperties.RemoteRepository("https://repo.spring.io/libs-snapshot"))));

		Map<String, ResourceLoader> resourceLoaders = new HashMap<>();
		resourceLoaders.put("maven", new MavenResourceLoader(mavenProperties));
		resourceLoaders.put("file", new FileSystemResourceLoader());

		DelegatingResourceLoader delegatingResourceLoader = new DelegatingResourceLoader(resourceLoaders);
		return delegatingResourceLoader;
	}

	@Bean
	public StreamDeploymentController streamDeploymentController(StreamDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository, AppRegistry registry,
			ApplicationConfigurationMetadataResolver metadataResolver,
			CommonApplicationProperties applicationProperties) {
		return new StreamDeploymentController(repository, deploymentIdRepository, registry, appDeployer(),
				metadataResolver, applicationProperties);
	}

	@Bean
	public StreamDefinitionController streamDefinitionController(StreamDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository, StreamDeploymentController deploymentController) {
		return new StreamDefinitionController(repository, deploymentIdRepository, deploymentController, appDeployer(),
				appRegistry());
	}

	@Bean
	public TaskDeploymentController taskController(ApplicationConfigurationMetadataResolver metadataResolver) {
		return new TaskDeploymentController(taskService(metadataResolver));
	}

	@Bean
	public MethodValidationPostProcessor methodValidationPostProcessor() {
		return new MethodValidationPostProcessor();
	}

	@Bean
	public CompletionController completionController(StreamCompletionProvider streamCompletionProvider, TaskCompletionProvider taskCompletionProvider) {
		return new CompletionController(streamCompletionProvider, taskCompletionProvider);
	}

	@Bean
	public AppRegistryController appRegistryController(AppRegistry registry, ApplicationConfigurationMetadataResolver metadataResolver) {
		return new AppRegistryController(registry, metadataResolver);
	}

	@Bean
	public TaskDefinitionController taskDefinitionController(TaskDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository) {
		return new TaskDefinitionController(repository, deploymentIdRepository, taskLauncher(), appRegistry());
	}

	public TaskExecutionController taskExecutionController(TaskExplorer explorer, TaskDefinitionRepository repository) {
		return new TaskExecutionController(explorer, repository);
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
		return new DataFlowUriRegistryPopulator(uriRegistry(), uriRegistryPopulator(), new String[] { "classpath:META-INF/test-apps.properties" });
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
	public TaskService taskService(ApplicationConfigurationMetadataResolver metadataResolver) {
		return new DefaultTaskService(new DataSourceProperties(), taskDefinitionRepository(), deploymentIdRepository(),
				uriRegistry(), resourceLoader(), taskLauncher(), metadataResolver);
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
	public DeploymentIdRepository deploymentIdRepository() {
		return new InMemoryDeploymentIdRepository();
	}
}
