/*
 * Copyright 2016-2017 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.analytics.metrics.AggregateCounterRepository;
import org.springframework.analytics.metrics.FieldValueCounterRepository;
import org.springframework.analytics.rest.controller.AggregateCounterController;
import org.springframework.analytics.rest.controller.CounterController;
import org.springframework.analytics.rest.controller.FieldValueCounterController;
import org.springframework.batch.admin.service.JobService;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.info.InfoProperties;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.dataflow.completion.CompletionConfiguration;
import org.springframework.cloud.dataflow.completion.StreamCompletionProvider;
import org.springframework.cloud.dataflow.completion.TaskCompletionProvider;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.registry.RdbmsUriRegistry;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.config.security.BasicAuthSecurityConfiguration.AuthorizationConfig;
import org.springframework.cloud.dataflow.server.controller.AboutController;
import org.springframework.cloud.dataflow.server.controller.AppRegistryController;
import org.springframework.cloud.dataflow.server.controller.CompletionController;
import org.springframework.cloud.dataflow.server.controller.FeaturesController;
import org.springframework.cloud.dataflow.server.controller.JobExecutionController;
import org.springframework.cloud.dataflow.server.controller.JobInstanceController;
import org.springframework.cloud.dataflow.server.controller.JobStepExecutionController;
import org.springframework.cloud.dataflow.server.controller.JobStepExecutionProgressController;
import org.springframework.cloud.dataflow.server.controller.MetricsController;
import org.springframework.cloud.dataflow.server.controller.RestControllerAdvice;
import org.springframework.cloud.dataflow.server.controller.RootController;
import org.springframework.cloud.dataflow.server.controller.RuntimeAppsController;
import org.springframework.cloud.dataflow.server.controller.RuntimeAppsController.AppInstanceController;
import org.springframework.cloud.dataflow.server.controller.StreamDefinitionController;
import org.springframework.cloud.dataflow.server.controller.StreamDeploymentController;
import org.springframework.cloud.dataflow.server.controller.TaskDefinitionController;
import org.springframework.cloud.dataflow.server.controller.TaskExecutionController;
import org.springframework.cloud.dataflow.server.controller.ToolsController;
import org.springframework.cloud.dataflow.server.controller.UiController;
import org.springframework.cloud.dataflow.server.controller.security.LoginController;
import org.springframework.cloud.dataflow.server.controller.security.SecurityController;
import org.springframework.cloud.dataflow.server.controller.support.MetricStore;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.hateoas.EntityLinks;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;

/**
 * Configuration for the Data Flow Server Controllers.
 *
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Andy Clement
 * @author Glenn Renfro
 */
@SuppressWarnings("all")
@Configuration
@Import(CompletionConfiguration.class)
@ConditionalOnBean({EnableDataFlowServerConfiguration.Marker.class, AppDeployer.class, TaskLauncher.class})
@EnableConfigurationProperties({ AuthorizationConfig.class, FeaturesProperties.class,
		VersionInfoProperties.class, MetricsProperties.class })
@ConditionalOnProperty(prefix = "dataflow.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableCircuitBreaker
public class DataFlowControllerAutoConfiguration {

	@Bean
	public UriRegistry uriRegistry(DataSource dataSource) {
		return new RdbmsUriRegistry(dataSource);
	}

	@Bean
	public AppRegistry appRegistry(UriRegistry uriRegistry, DelegatingResourceLoader resourceLoader) {
		return new AppRegistry(uriRegistry, resourceLoader);
	}

	@Bean
	public RootController rootController(EntityLinks entityLinks) {
		return new RootController(entityLinks);
	}

	@Bean
	public AppInstanceController appInstanceController(AppDeployer appDeployer) {
		return new AppInstanceController(appDeployer);
	}

	@Bean
	public MetricStore metricStore(MetricsProperties metricsProperties) {
		return new MetricStore(metricsProperties);
	}

	@Bean
	public MetricsController metricsController(MetricStore metricStore) {
		return new MetricsController(metricStore);
	}

	@Bean
	@ConditionalOnBean(StreamDefinitionRepository.class)
	public StreamDefinitionController streamDefinitionController(StreamDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository, StreamDeploymentController deploymentController,
			AppDeployer deployer, AppRegistry appRegistry) {
		return new StreamDefinitionController(repository, deploymentIdRepository, deploymentController, deployer,
				appRegistry);
	}

	@Bean
	@ConditionalOnBean(StreamDefinitionRepository.class)
	public StreamDeploymentController streamDeploymentController(StreamDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository, AppRegistry registry, AppDeployer deployer,
			ApplicationConfigurationMetadataResolver metadataResolver, CommonApplicationProperties appsProperties) {
		return new StreamDeploymentController(repository, deploymentIdRepository, registry, deployer, metadataResolver, appsProperties);
	}

	@Bean
	@ConditionalOnBean(StreamDefinitionRepository.class)
	public RuntimeAppsController runtimeAppsController(StreamDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository, AppDeployer appDeployer) {
		return new RuntimeAppsController(repository, deploymentIdRepository, appDeployer, runtimeAppsStatusFJPFB().getObject());
	}

	@Bean
	@ConditionalOnBean(StreamDefinitionRepository.class)
	@ConditionalOnMissingBean(name = "runtimeAppsStatusFJPFB")
	public ForkJoinPoolFactoryBean runtimeAppsStatusFJPFB() {
		ForkJoinPoolFactoryBean forkJoinPoolFactoryBean = new ForkJoinPoolFactoryBean();
		forkJoinPoolFactoryBean.setParallelism(8);
		return forkJoinPoolFactoryBean;
	}

	@Bean
	public MavenResourceLoader mavenResourceLoader(MavenProperties properties) {
		return new MavenResourceLoader(properties);
	}

	@Bean
	@ConditionalOnMissingBean(DelegatingResourceLoader.class)
	public DelegatingResourceLoader delegatingResourceLoader(MavenResourceLoader mavenResourceLoader) {
		Map<String, ResourceLoader> loaders = new HashMap<>();
		loaders.put("maven", mavenResourceLoader);
		return new DelegatingResourceLoader(loaders);
	}

	@Bean
	@ConditionalOnBean(TaskDefinitionRepository.class)
	public TaskDefinitionController taskDefinitionController(
			TaskDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository,
			TaskLauncher taskLauncher, AppRegistry appRegistry, TaskService taskService) {
		return new TaskDefinitionController(repository, deploymentIdRepository,
				taskLauncher, appRegistry, taskService);
	}

	@Bean
	@ConditionalOnBean(TaskDefinitionRepository.class)
	public TaskExecutionController taskExecutionController(TaskExplorer explorer, TaskService taskService,
		TaskDefinitionRepository taskDefinitionRepository) {
		return new TaskExecutionController(explorer, taskService, taskDefinitionRepository);
	}

	@Bean
	@ConditionalOnBean(TaskDefinitionRepository.class)
	public JobExecutionController jobExecutionController(TaskJobService repository) {
		return new JobExecutionController(repository);
	}

	@Bean
	@ConditionalOnBean(TaskDefinitionRepository.class)
	public JobStepExecutionController jobStepExecutionController(JobService service) {
		return new JobStepExecutionController(service);
	}

	@Bean
	@ConditionalOnBean(TaskDefinitionRepository.class)
	public JobStepExecutionProgressController jobStepExecutionProgressController(JobService service) {
		return new JobStepExecutionProgressController(service);
	}

	@Bean
	@ConditionalOnBean(TaskDefinitionRepository.class)
	public JobInstanceController jobInstanceController(TaskJobService repository) {
		return new JobInstanceController(repository);
	}

	@Bean
	@ConditionalOnBean(MetricRepository.class)
	public CounterController counterController(MetricRepository metricRepository) {
		return new CounterController(metricRepository);
	}

	@Bean
	@ConditionalOnBean(FieldValueCounterRepository.class)
	public FieldValueCounterController fieldValueCounterController(FieldValueCounterRepository repository) {
		return new FieldValueCounterController(repository);
	}

	@Bean
	@ConditionalOnBean(AggregateCounterRepository.class)
	public AggregateCounterController aggregateCounterController(AggregateCounterRepository repository) {
		return new AggregateCounterController(repository);
	}

	@Bean
	public CompletionController completionController(StreamCompletionProvider completionProvider, TaskCompletionProvider taskCompletionProvider) {
		return new CompletionController(completionProvider, taskCompletionProvider);
	}

	@Bean
	public ToolsController toolsController() {
		return new ToolsController();
	}

	@Bean
	@ConditionalOnMissingBean(name = "appRegistryFJPFB")
	public ForkJoinPoolFactoryBean appRegistryFJPFB() {
		ForkJoinPoolFactoryBean forkJoinPoolFactoryBean = new ForkJoinPoolFactoryBean();
		forkJoinPoolFactoryBean.setParallelism(4);
		return forkJoinPoolFactoryBean;
	}

	@Bean
	public AppRegistryController appRegistryController(AppRegistry appRegistry, ApplicationConfigurationMetadataResolver metadataResolver) {
		return new AppRegistryController(appRegistry, metadataResolver, appRegistryFJPFB().getObject());
	}

	@Bean
	public SecurityController securityController(SecurityProperties securityProperties, AuthorizationConfig authorizationConfig) {
		return new SecurityController(securityProperties, authorizationConfig);
	}

	@Bean
	@ConditionalOnProperty("security.basic.enabled")
	public LoginController loginController() {
		return new LoginController();
	}

	@Bean
	public FeaturesController featuresController(FeaturesProperties featuresProperties) {
		return new FeaturesController(featuresProperties);
	}

	@Bean
	public AboutController aboutController(
			AppDeployer appDeployer,
			TaskLauncher taskLauncher,
			FeaturesProperties featuresProperties,
			VersionInfoProperties versionInfoProperties,
			SecurityProperties securityProperties,
			AuthorizationConfig authorizationConfig) {
		return new AboutController(appDeployer, taskLauncher, featuresProperties, versionInfoProperties, securityProperties, authorizationConfig);
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
