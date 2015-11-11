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

package org.springframework.cloud.dataflow.admin.configuration;

import static org.mockito.Mockito.mock;
import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;

import org.springframework.cloud.dataflow.admin.config.ArtifactRegistryPopulator;
import org.springframework.cloud.dataflow.admin.controller.RestControllerAdvice;
import org.springframework.cloud.dataflow.admin.controller.StreamController;
import org.springframework.cloud.dataflow.admin.controller.TaskController;
import org.springframework.cloud.dataflow.admin.repository.InMemoryStreamDefinitionRepository;
import org.springframework.cloud.dataflow.admin.repository.InMemoryTaskDefinitionRepository;
import org.springframework.cloud.dataflow.admin.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.admin.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistry;
import org.springframework.cloud.dataflow.artifact.registry.InMemoryArtifactRegistry;
import org.springframework.cloud.dataflow.completion.CompletionConfiguration;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * @author Michael Minella
 * @author Mark Fisher
 */
@Configuration
@EnableSpringDataWebSupport
@EnableHypermediaSupport(type = HAL)
@Import(CompletionConfiguration.class)
public class TestDependencies extends WebMvcConfigurationSupport {

	@Bean
	public RestControllerAdvice restControllerAdvice() {
		return new RestControllerAdvice();
	}

	@Bean
	public StreamController streamController(StreamDefinitionRepository repository, ArtifactRegistry registry) {
		return new StreamController(repository, registry, processModuleDeployer());
	}

	@Bean
	public TaskController taskController(TaskDefinitionRepository repository, ArtifactRegistry registry) {
		return new TaskController(repository, registry, taskModuleDeployer());
	}

	@Bean
	public ArtifactRegistry artifactRegistry() {
		return new InMemoryArtifactRegistry();
	}

	@Bean
	public ArtifactRegistryPopulator artifactRegistryPopulator() {
		return new ArtifactRegistryPopulator(artifactRegistry());
	}

	@Bean
	public ModuleDeployer processModuleDeployer() {
		return mock(ModuleDeployer.class);
	}

	@Bean
	public ModuleDeployer taskModuleDeployer() {
		return mock(ModuleDeployer.class);
	}

	@Bean
	public StreamDefinitionRepository streamDefinitionRepository() {
		return new InMemoryStreamDefinitionRepository();
	}

	@Bean
	public TaskDefinitionRepository taskDefinitionRepository() {
		return new InMemoryTaskDefinitionRepository();
	}
}
