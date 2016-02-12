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

import org.springframework.batch.admin.service.JobService;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.app.resolver.ModuleResolver;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistry;
import org.springframework.cloud.dataflow.completion.StreamCompletionProvider;
import org.springframework.cloud.dataflow.server.controller.CompletionController;
import org.springframework.cloud.dataflow.server.controller.CounterController;
import org.springframework.cloud.dataflow.server.controller.FieldValueCounterController;
import org.springframework.cloud.dataflow.server.controller.JobExecutionController;
import org.springframework.cloud.dataflow.server.controller.JobInstanceController;
import org.springframework.cloud.dataflow.server.controller.JobStepExecutionController;
import org.springframework.cloud.dataflow.server.controller.LibraryController;
import org.springframework.cloud.dataflow.server.controller.ModuleController;
import org.springframework.cloud.dataflow.server.controller.RestControllerAdvice;
import org.springframework.cloud.dataflow.server.controller.RootController;
import org.springframework.cloud.dataflow.server.controller.RuntimeModulesController;
import org.springframework.cloud.dataflow.server.controller.RuntimeModulesController.AppInstanceController;
import org.springframework.cloud.dataflow.server.controller.SecurityController;
import org.springframework.cloud.dataflow.server.controller.StreamDefinitionController;
import org.springframework.cloud.dataflow.server.controller.StreamDeploymentController;
import org.springframework.cloud.dataflow.server.controller.TaskDefinitionController;
import org.springframework.cloud.dataflow.server.controller.TaskDeploymentController;
import org.springframework.cloud.dataflow.server.controller.TaskExecutionController;
import org.springframework.cloud.dataflow.server.controller.UiController;
import org.springframework.cloud.dataflow.server.job.TaskJobRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.stream.configuration.metadata.ModuleConfigurationMetadataResolver;
import org.springframework.cloud.stream.module.metrics.FieldValueCounterRepository;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.EntityLinks;

/**
 * Configuration for the Data Flow Server Controllers.
 *
 * @author Mark Fisher
 */
@Configuration
@ConditionalOnBean({ AppDeployer.class, TaskLauncher.class })
public class DataFlowControllerAutoConfiguration {

	@Bean
	public RootController rootController(EntityLinks entityLinks) {
		return new RootController(entityLinks);
	}

	@Bean
	public RuntimeModulesController runtimeModulesController(StreamDefinitionRepository repository,
			AppDeployer appDeployer) {
		return new RuntimeModulesController(repository, appDeployer);
	}

	@Bean
	public AppInstanceController appInstanceController(AppDeployer appDeployer) {
		return new AppInstanceController(appDeployer);
	}

	@Bean
	public StreamDefinitionController streamDefinitionController(StreamDefinitionRepository repository,
			StreamDeploymentController deploymentController, AppDeployer deployer) {
		return new StreamDefinitionController(repository, deploymentController, deployer);
	}

	@Bean
	public StreamDeploymentController streamDeploymentController(StreamDefinitionRepository repository,
			ArtifactRegistry registry, AppDeployer deployer, MavenProperties mavenProperties) {
		return new StreamDeploymentController(repository, registry, deployer, mavenProperties);
	}

	@Bean
	public TaskDefinitionController taskDefinitionController(TaskDefinitionRepository repository,
			TaskLauncher taskLauncher) {
		return new TaskDefinitionController(repository, taskLauncher);
	}

	@Bean
	public TaskDeploymentController taskDeploymentController(TaskDefinitionRepository repository,
			ArtifactRegistry registry, TaskLauncher taskLauncher, MavenProperties mavenProperties) {
		return new TaskDeploymentController(repository, registry, taskLauncher, mavenProperties);
	}

	@Bean
	public TaskExecutionController taskExecutionController(TaskExplorer explorer) {
		return new TaskExecutionController(explorer);
	}

	@Bean
	public JobExecutionController jobExecutionController(TaskJobRepository repository) {
		return new JobExecutionController(repository);
	}

	@Bean
	public JobStepExecutionController jobStepExecutionController(JobService service) {
		return new JobStepExecutionController(service);
	}

	@Bean
	JobInstanceController jobInstanceController(TaskJobRepository repository){
		return new JobInstanceController(repository);
	}

	@Bean
	public CounterController counterController(MetricRepository metricRepository) {
		return new CounterController(metricRepository);
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

	@Bean
	public MavenProperties mavenProperties() {
		return new MavenConfigurationProperties();
	}

	@ConfigurationProperties(prefix = "maven")
	static class MavenConfigurationProperties extends MavenProperties {
	}
}
