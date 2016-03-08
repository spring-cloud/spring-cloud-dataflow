/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config;

import java.util.Collection;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.cloud.dataflow.app.resolver.ModuleResolver;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistry;
import org.springframework.cloud.dataflow.completion.StreamCompletionProvider;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.cloud.dataflow.server.controller.CompletionController;
import org.springframework.cloud.dataflow.server.controller.CounterController;
import org.springframework.cloud.dataflow.server.controller.DeprecatedRootController;
import org.springframework.cloud.dataflow.server.controller.DeprecatedRuntimeModulesController;
import org.springframework.cloud.dataflow.server.controller.DeprecatedRuntimeModulesController.DeprecatedInstanceController;
import org.springframework.cloud.dataflow.server.controller.DeprecatedStreamDefinitionController;
import org.springframework.cloud.dataflow.server.controller.DeprecatedStreamDeploymentController;
import org.springframework.cloud.dataflow.server.controller.DeprecatedTaskDefinitionController;
import org.springframework.cloud.dataflow.server.controller.DeprecatedTaskDeploymentController;
import org.springframework.cloud.dataflow.server.controller.FieldValueCounterController;
import org.springframework.cloud.dataflow.server.controller.LibraryController;
import org.springframework.cloud.dataflow.server.controller.ModuleController;
import org.springframework.cloud.dataflow.server.controller.RestControllerAdvice;
import org.springframework.cloud.dataflow.server.controller.SecurityController;
import org.springframework.cloud.dataflow.server.controller.TaskExecutionController;
import org.springframework.cloud.dataflow.server.controller.UiController;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.stream.configuration.metadata.ModuleConfigurationMetadataResolver;
import org.springframework.cloud.stream.module.metrics.FieldValueCounterRepository;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.EntityLinks;

/**
 * Configuration for the Data Flow Server Controllers when the (deprecated)
 * ModuleDeployer SPI is still present.
 *
 * @author Mark Fisher
 */
@Configuration
@ConditionalOnBean(ModuleDeployer.class)
@Deprecated
public class DeprecatedDataFlowControllerAutoConfiguration {

	@Bean
	public DeprecatedRootController rootController(EntityLinks entityLinks) {
		return new DeprecatedRootController(entityLinks);
	}

	@Bean
	public DeprecatedRuntimeModulesController runtimeModulesController(Collection<ModuleDeployer> moduleDeployers) {
		return new DeprecatedRuntimeModulesController(moduleDeployers);
	}

	@Bean
	public DeprecatedInstanceController moduleInstanceController(Collection<ModuleDeployer> moduleDeployers) {
		return new DeprecatedInstanceController(moduleDeployers);
	}

	@Bean
	public DeprecatedStreamDefinitionController streamDefinitionController(StreamDefinitionRepository repository,
			DeprecatedStreamDeploymentController deploymentController, ModuleDeployer processModuleDeployer) {
		return new DeprecatedStreamDefinitionController(repository, deploymentController, processModuleDeployer);
	}

	@Bean
	public DeprecatedStreamDeploymentController streamDeploymentController(StreamDefinitionRepository repository,
			ArtifactRegistry registry, ModuleDeployer processModuleDeployer) {
		return new DeprecatedStreamDeploymentController(repository, registry, processModuleDeployer);
	}

	@Bean
	public DeprecatedTaskDefinitionController taskDefinitionController(TaskDefinitionRepository repository,
			ModuleDeployer taskModuleDeployer) {
		return new DeprecatedTaskDefinitionController(repository, taskModuleDeployer);
	}

	@Bean
	public DeprecatedTaskDeploymentController taskDeploymentController(TaskDefinitionRepository repository,
			ArtifactRegistry registry, ModuleDeployer taskModuleDeployer) {
		return new DeprecatedTaskDeploymentController(repository, registry, taskModuleDeployer);
	}

	@Bean
	public TaskExecutionController taskExecutionController(TaskExplorer explorer) {
		return new TaskExecutionController(explorer);
	}

	@Bean
	public CounterController counterController() {
		return new CounterController();
	}

	@Bean
	public CompletionController completionController(StreamCompletionProvider completionProvider) {
		return new CompletionController(completionProvider);
	}

	@Bean
	public FieldValueCounterController fieldValueCounterController(FieldValueCounterRepository repository) {
		return new FieldValueCounterController(repository);
	}

	@Bean
	public LibraryController libraryController(ArtifactRegistry registry) {
		return new LibraryController(registry);
	}

	@Bean
	public ModuleController moduleController(ArtifactRegistry registry, ModuleResolver moduleResolver,
			ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver) {
		return new ModuleController(registry, moduleResolver, moduleConfigurationMetadataResolver);
	}

	@Bean
	public SecurityController securityController(SecurityProperties securityProperties) {
		return new SecurityController(securityProperties);
	}

	@Bean
	public UiController uiController() {
		return new UiController();
	}

	@Bean
	public RestControllerAdvice restControllerAdvice() {
		return new RestControllerAdvice();
	}
}
