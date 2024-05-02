/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.info.BuildInfoContributor;
import org.springframework.boot.actuate.info.GitInfoContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.common.security.AuthorizationProperties;
import org.springframework.cloud.common.security.core.support.OAuth2AccessTokenProvidingClientHttpRequestInterceptor;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.common.security.support.SecurityStateBean;
import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.AggregateTaskExplorer;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.audit.repository.AuditRecordRepository;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.audit.service.DefaultAuditRecordService;
import org.springframework.cloud.dataflow.completion.CompletionConfiguration;
import org.springframework.cloud.dataflow.completion.StreamCompletionProvider;
import org.springframework.cloud.dataflow.completion.TaskCompletionProvider;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepository;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.registry.service.DefaultAppRegistryService;
import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.dataflow.server.TaskValidationController;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.config.features.ConditionalOnStreamsEnabled;
import org.springframework.cloud.dataflow.server.config.features.ConditionalOnTasksEnabled;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.controller.AboutController;
import org.springframework.cloud.dataflow.server.controller.AppRegistryController;
import org.springframework.cloud.dataflow.server.controller.AuditRecordController;
import org.springframework.cloud.dataflow.server.controller.CompletionController;
import org.springframework.cloud.dataflow.server.controller.JobExecutionController;
import org.springframework.cloud.dataflow.server.controller.JobExecutionThinController;
import org.springframework.cloud.dataflow.server.controller.JobInstanceController;
import org.springframework.cloud.dataflow.server.controller.JobStepExecutionController;
import org.springframework.cloud.dataflow.server.controller.JobStepExecutionProgressController;
import org.springframework.cloud.dataflow.server.controller.RestControllerAdvice;
import org.springframework.cloud.dataflow.server.controller.RootController;
import org.springframework.cloud.dataflow.server.controller.RuntimeAppInstanceController;
import org.springframework.cloud.dataflow.server.controller.RuntimeAppsController;
import org.springframework.cloud.dataflow.server.controller.RuntimeStreamsController;
import org.springframework.cloud.dataflow.server.controller.SchemaController;
import org.springframework.cloud.dataflow.server.controller.StreamDefinitionController;
import org.springframework.cloud.dataflow.server.controller.StreamDeploymentController;
import org.springframework.cloud.dataflow.server.controller.StreamLogsController;
import org.springframework.cloud.dataflow.server.controller.StreamValidationController;
import org.springframework.cloud.dataflow.server.controller.TaskCtrController;
import org.springframework.cloud.dataflow.server.controller.TaskDefinitionController;
import org.springframework.cloud.dataflow.server.controller.TaskExecutionController;
import org.springframework.cloud.dataflow.server.controller.TaskExecutionThinController;
import org.springframework.cloud.dataflow.server.controller.TaskLogsController;
import org.springframework.cloud.dataflow.server.controller.TaskPlatformController;
import org.springframework.cloud.dataflow.server.controller.TaskSchedulerController;
import org.springframework.cloud.dataflow.server.controller.TasksInfoController;
import org.springframework.cloud.dataflow.server.controller.ToolsController;
import org.springframework.cloud.dataflow.server.controller.UiController;
import org.springframework.cloud.dataflow.server.controller.assembler.AppRegistrationAssemblerProvider;
import org.springframework.cloud.dataflow.server.controller.assembler.DefaultAppRegistrationAssemblerProvider;
import org.springframework.cloud.dataflow.server.controller.assembler.DefaultStreamDefinitionAssemblerProvider;
import org.springframework.cloud.dataflow.server.controller.assembler.DefaultTaskDefinitionAssemblerProvider;
import org.springframework.cloud.dataflow.server.controller.assembler.StreamDefinitionAssemblerProvider;
import org.springframework.cloud.dataflow.server.controller.assembler.TaskDefinitionAssemblerProvider;
import org.springframework.cloud.dataflow.server.controller.security.SecurityController;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.JobServiceContainer;
import org.springframework.cloud.dataflow.server.service.LauncherService;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.SpringSecurityAuditorAware;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.service.StreamValidationService;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.service.TaskSaveService;
import org.springframework.cloud.dataflow.server.service.TaskValidationService;
import org.springframework.cloud.dataflow.server.service.impl.AppDeploymentRequestCreator;
import org.springframework.cloud.dataflow.server.service.impl.ComposedTaskRunnerConfigurationProperties;
import org.springframework.cloud.dataflow.server.service.impl.DefaultLauncherService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultStreamService;
import org.springframework.cloud.dataflow.server.service.impl.TaskConfigurationProperties;
import org.springframework.cloud.dataflow.server.service.impl.validation.DefaultStreamValidationService;
import org.springframework.cloud.dataflow.server.service.impl.validation.DefaultTaskValidationService;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.StreamDeployer;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.skipper.client.DefaultSkipperClient;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.client.SkipperClientProperties;
import org.springframework.cloud.skipper.client.SkipperClientResponseErrorHandler;
import org.springframework.cloud.skipper.client.util.HttpClientConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertyResolver;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.core.AnnotationLinkRelationProvider;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for the Data Flow Server Controllers.
 *
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Andy Clement
 * @author Glenn Renfro
 * @author Christian Tzolov
 * @author Corneil du Plessis
 */
@SuppressWarnings("all")
@Configuration
@Import(CompletionConfiguration.class)
@ConditionalOnBean({EnableDataFlowServerConfiguration.Marker.class})
@EnableConfigurationProperties({FeaturesProperties.class, VersionInfoProperties.class,
		DockerValidatorProperties.class, DataflowMetricsProperties.class})
@ConditionalOnProperty(prefix = "dataflow.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@EntityScan({
		"org.springframework.cloud.dataflow.core"
})
@EnableJpaRepositories(basePackages = {
		"org.springframework.cloud.dataflow.registry.repository",
		"org.springframework.cloud.dataflow.server.repository",
		"org.springframework.cloud.dataflow.audit.repository"
})
@EnableJpaAuditing
@EnableTransactionManagement
public class DataFlowControllerAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(DataFlowControllerAutoConfiguration.class);

	@Bean
	public RootController rootController(EntityLinks entityLinks) {
		return new RootController(entityLinks);
	}

	@Bean
	public CompletionController completionController(StreamCompletionProvider completionProvider,
													 TaskCompletionProvider taskCompletionProvider) {
		return new CompletionController(completionProvider, taskCompletionProvider);
	}

	@Bean
	public ToolsController toolsController() {
		return new ToolsController();
	}

	@Bean
	public AboutController aboutController(ObjectProvider<StreamDeployer> streamDeployer,
										   ObjectProvider<LauncherRepository> launcherRepository,
										   FeaturesProperties featuresProperties,
										   VersionInfoProperties versionInfoProperties,
										   SecurityStateBean securityStateBean,
										   DataflowMetricsProperties monitoringDashboardInfoProperties,
										   ObjectProvider<GitInfoContributor> gitInfoContributor,
										   ObjectProvider<BuildInfoContributor> buildInfoContributor) {
		return new AboutController(streamDeployer.getIfAvailable(), launcherRepository.getIfAvailable(),
				featuresProperties, versionInfoProperties, securityStateBean, monitoringDashboardInfoProperties,
				gitInfoContributor, buildInfoContributor);
	}

	@Bean
	public UiController uiController() {
		return new UiController();
	}

	@Bean
	public RestControllerAdvice restControllerAdvice() {
		return new RestControllerAdvice();
	}



	@Configuration
	public static class AppRegistryConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "appRegistryFJPFB")
		public ForkJoinPoolFactoryBean appRegistryFJPFB() {
			ForkJoinPoolFactoryBean forkJoinPoolFactoryBean = new ForkJoinPoolFactoryBean();
			forkJoinPoolFactoryBean.setParallelism(4);
			return forkJoinPoolFactoryBean;
		}

		@Bean
		public AppResourceCommon appResourceCommon(@Nullable MavenProperties mavenProperties,
												   DelegatingResourceLoader delegatingResourceLoader) {
			return new AppResourceCommon(mavenProperties, delegatingResourceLoader);
		}

		@Bean
		public AppRegistryService appRegistryService(AppRegistrationRepository appRegistrationRepository,
													 AppResourceCommon appResourceCommon, AuditRecordService auditRecordService) {
			return new DefaultAppRegistryService(appRegistrationRepository, appResourceCommon, auditRecordService);
		}

		@Bean
		@ConditionalOnMissingBean
		public AppRegistryController appRegistryController(
				Optional<StreamDefinitionRepository> streamDefinitionRepository,
				Optional<StreamService> streamService,
				AppRegistryService appRegistry, ApplicationConfigurationMetadataResolver metadataResolver,
				ForkJoinPool appRegistryFJPFB, StreamDefinitionService streamDefinitionService,
				AppRegistrationAssemblerProvider<? extends AppRegistrationResource> appRegistrationAssemblerProvider) {
			return new AppRegistryController(streamDefinitionRepository,
					streamService,
					appRegistry,
					metadataResolver, appRegistryFJPFB, streamDefinitionService, appRegistrationAssemblerProvider);
		}

		@Bean
		@ConditionalOnMissingBean
		public AppRegistrationAssemblerProvider appRegistryAssemblerProvider() {
			return new DefaultAppRegistrationAssemblerProvider();
		}
	}

	@Configuration
	@ConditionalOnTasksEnabled
	public static class TaskEnabledConfiguration {

		@Bean
		public SchemaController schemaController(SchemaService schemaService) {
			return new SchemaController(schemaService);
		}

		@Bean
		public TaskExecutionController taskExecutionController(
				AggregateTaskExplorer explorer,
				AggregateExecutionSupport aggregateExecutionSupport,
			   	TaskExecutionService taskExecutionService,
				TaskDefinitionRepository taskDefinitionRepository,
				TaskDefinitionReader taskDefinitionReader,
				TaskExecutionInfoService taskExecutionInfoService,
				TaskDeleteService taskDeleteService,
				TaskJobService taskJobService
		) {
			return new TaskExecutionController(explorer,
					aggregateExecutionSupport,
					taskExecutionService,
					taskDefinitionRepository,
					taskDefinitionReader,
					taskExecutionInfoService,
					taskDeleteService,
					taskJobService
			);
		}

		@Bean
		public TaskExecutionThinController taskExecutionThinController(AggregateTaskExplorer aggregateTaskExplorer, TaskJobService taskJobService) {
			return new TaskExecutionThinController(aggregateTaskExplorer, taskJobService);
		}

		@Bean
		public TaskPlatformController taskLauncherController(LauncherService launcherService) {
			return new TaskPlatformController(launcherService);
		}

		@Bean
		@ConditionalOnMissingBean
		public TaskDefinitionAssemblerProvider taskDefinitionAssemblerProvider(
				TaskExecutionService taskExecutionService,
				TaskJobService taskJobService,
				AggregateTaskExplorer taskExplorer,
				AggregateExecutionSupport aggregateExecutionSupport
		) {
			return new DefaultTaskDefinitionAssemblerProvider(taskExecutionService, taskJobService, taskExplorer, aggregateExecutionSupport);
		}

		@Bean
		public TaskDefinitionController taskDefinitionController(
				AggregateTaskExplorer taskExplorer,
				TaskDefinitionRepository repository,
				TaskSaveService taskSaveService,
																 TaskDeleteService taskDeleteService,
				TaskDefinitionAssemblerProvider<? extends TaskDefinitionResource> taskDefinitionAssemblerProvider
		) {
			return new TaskDefinitionController(taskExplorer, repository, taskSaveService, taskDeleteService,
					taskDefinitionAssemblerProvider);
		}

		@Bean
		public JobExecutionController jobExecutionController(TaskJobService repository) {
			return new JobExecutionController(repository);
		}

		@Bean
		public TasksInfoController taskExecutionsInfoController(TaskExecutionService taskExecutionService) {
			return new TasksInfoController(taskExecutionService);
		}

		@Bean
		public JobExecutionThinController jobExecutionThinController(TaskJobService repository) {
			return new JobExecutionThinController(repository);
		}

		@Bean
		public JobStepExecutionController jobStepExecutionController(JobServiceContainer jobServiceContainer) {
			return new JobStepExecutionController(jobServiceContainer);
		}

		@Bean
		public JobStepExecutionProgressController jobStepExecutionProgressController(JobServiceContainer jobServiceContainer, TaskJobService taskJobService) {
			return new JobStepExecutionProgressController(jobServiceContainer, taskJobService);
		}

		@Bean
		public JobInstanceController jobInstanceController(TaskJobService repository) {
			return new JobInstanceController(repository);
		}

		@Bean
		public TaskValidationService taskValidationService(AppRegistryService appRegistry,
														   DockerValidatorProperties dockerValidatorProperties,
														   TaskDefinitionRepository taskDefinitionRepository,
														   TaskConfigurationProperties taskConfigurationProperties) {
			return new DefaultTaskValidationService(appRegistry,
					dockerValidatorProperties,
					taskDefinitionRepository);
		}

		@Bean
		public TaskValidationController taskValidationController(TaskValidationService taskValidationService) {
			return new TaskValidationController(taskValidationService);
		}

		@Bean
		public TaskLogsController taskLogsController(TaskExecutionService taskExecutionService) {
			return new TaskLogsController(taskExecutionService);
		}

		@Bean
		public LauncherService launcherService(LauncherRepository launcherRepository) {
			return new DefaultLauncherService(launcherRepository);
		}

		@Bean
		public TaskCtrController tasksCtrController(ApplicationConfigurationMetadataResolver metadataResolver,
													TaskConfigurationProperties taskConfigurationProperties,
													ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties,
													AppResourceCommon appResourceCommon) {
			return new TaskCtrController(metadataResolver, taskConfigurationProperties,
					composedTaskRunnerConfigurationProperties, appResourceCommon);
		}

	}

	@Configuration
	@ConditionalOnStreamsEnabled
	@EnableConfigurationProperties(SkipperClientProperties.class)
	public static class StreamEnabledConfiguration {

		@Bean
		public StreamValidationService streamValidationService(AppRegistryService appRegistry,
															   DockerValidatorProperties dockerValidatorProperties,
															   StreamDefinitionRepository streamDefinitionRepository,
															   StreamDefinitionService streamDefinitionService) {
			return new DefaultStreamValidationService(appRegistry,
					dockerValidatorProperties,
					streamDefinitionRepository,
					streamDefinitionService);
		}

		@Bean
		public RuntimeAppInstanceController appInstanceController(StreamDeployer streamDeployer) {
			return new RuntimeAppInstanceController(streamDeployer);
		}

		@Bean
		@ConditionalOnMissingBean
		public StreamDefinitionAssemblerProvider streamDefinitionAssemblerProvider(
				StreamDefinitionService streamDefinitionService,
				StreamService streamService) {
			return new DefaultStreamDefinitionAssemblerProvider(streamDefinitionService, streamService);
		}

		@Bean
		@ConditionalOnMissingBean
		public StreamDefinitionController streamDefinitionController(StreamService streamService,
																	 StreamDefinitionService streamDefinitionService, AppRegistryService appRegistryService,
																	 StreamDefinitionAssemblerProvider<? extends StreamDefinitionResource> streamDefinitionAssemblerProvider,
																	 AppRegistrationAssemblerProvider<? extends AppRegistrationResource> appRegistrationAssemblerProvider) {
			return new StreamDefinitionController(streamService, streamDefinitionService, appRegistryService,
					streamDefinitionAssemblerProvider, appRegistrationAssemblerProvider);
		}

		@Bean
		public StreamValidationController streamValidationController(StreamService streamService) {
			return new StreamValidationController(streamService);
		}

		@Bean
		public RuntimeAppsController runtimeAppsController(StreamDeployer streamDeployer) {
			return new RuntimeAppsController(streamDeployer);
		}

		@Bean
		public RuntimeStreamsController runtimeStreamsControllerV2(StreamDeployer streamDeployer) {
			return new RuntimeStreamsController(streamDeployer);
		}

		@Bean
		public StreamLogsController streamLogsController(StreamDeployer streamDeployer) {
			return new StreamLogsController(streamDeployer);
		}

		@Bean
		@ConditionalOnMissingBean(name = "runtimeAppsStatusFJPFB")
		public ForkJoinPoolFactoryBean runtimeAppsStatusFJPFB() {
			ForkJoinPoolFactoryBean forkJoinPoolFactoryBean = new ForkJoinPoolFactoryBean();
			forkJoinPoolFactoryBean.setParallelism(8);
			return forkJoinPoolFactoryBean;
		}

		@Bean
		public StreamDeploymentController updatableStreamDeploymentController(
				StreamDefinitionRepository repository, StreamService streamService,
				StreamDefinitionService streamDefinitionService) {
			return new StreamDeploymentController(repository, streamService, streamDefinitionService);
		}

		@Bean
		public SkipperClient skipperClient(SkipperClientProperties properties,
										   RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper,
										   @Nullable OAuth2TokenUtilsService oauth2TokenUtilsService) {

			// TODO (Tzolov) review the manual Hal convertion configuration
			objectMapper.registerModule(new Jackson2HalModule());
			objectMapper.setHandlerInstantiator(new Jackson2HalModule.HalHandlerInstantiator(
					new AnnotationLinkRelationProvider(), CurieProvider.NONE, MessageResolver.DEFAULTS_ONLY));
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			RestTemplate restTemplate = restTemplateBuilder
					.errorHandler(new SkipperClientResponseErrorHandler(objectMapper))
					.interceptors(new OAuth2AccessTokenProvidingClientHttpRequestInterceptor(oauth2TokenUtilsService))
					.messageConverters(Arrays.asList(new StringHttpMessageConverter(),
							new MappingJackson2HttpMessageConverter(objectMapper)))
					.build();

			if (properties.isSkipSslValidation()) {
				restTemplate.setRequestFactory(HttpClientConfigurer.create()
						.targetHost(URI.create(properties.getServerUri()))
						.skipTlsCertificateVerification(true)
						.buildClientHttpRequestFactory());
				logger.warn("Skipper Client - Skip SSL Validation is Enabbled!");
			}

			return new DefaultSkipperClient(properties.getServerUri(), restTemplate);
		}

		@Bean
		public SkipperStreamDeployer skipperStreamDeployer(SkipperClient skipperClient,
														   StreamDefinitionRepository streamDefinitionRepository,
														   SkipperClientProperties skipperClientProperties,
														   AppRegistryService appRegistryService,
														   ForkJoinPool runtimeAppsStatusFJPFB,
														   StreamDefinitionService streamDefinitionService) {
			logger.info("Skipper URI [" + skipperClientProperties.getServerUri() + "]");
			return new SkipperStreamDeployer(skipperClient, streamDefinitionRepository, appRegistryService,
					runtimeAppsStatusFJPFB, streamDefinitionService);
		}

		@Bean
		public AppDeploymentRequestCreator streamDeploymentPropertiesUtils(AppRegistryService appRegistry,
																		   CommonApplicationProperties commonApplicationProperties,
																		   ApplicationConfigurationMetadataResolver applicationConfigurationMetadataResolver,
																		   StreamDefinitionService streamDefinitionService,
																		   PropertyResolver propertyResolver) {
			return new AppDeploymentRequestCreator(appRegistry, commonApplicationProperties,
					applicationConfigurationMetadataResolver, streamDefinitionService, propertyResolver);
		}

		@Bean
		public StreamService streamService(
				StreamDefinitionRepository streamDefinitionRepository,
				SkipperStreamDeployer skipperStreamDeployer, AppDeploymentRequestCreator appDeploymentRequestCreator,
				StreamValidationService streamValidationService,
				AuditRecordService auditRecordService, StreamDefinitionService streamDefinitionService) {
			return new DefaultStreamService(streamDefinitionRepository, skipperStreamDeployer,
					appDeploymentRequestCreator, streamValidationService, auditRecordService, streamDefinitionService);
		}
	}

	@Bean
	@ConditionalOnBean(SchedulerService.class)
	public TaskSchedulerController taskSchedulerController(SchedulerService schedulerService) {
		return new TaskSchedulerController(schedulerService);
	}

	@Configuration
	public static class AuditingConfiguration {
		@Bean
		public AuditRecordService auditRecordService(AuditRecordRepository auditRecordRepository,
													 ObjectMapper objectMapper) {
			return new DefaultAuditRecordService(auditRecordRepository);
		}

		@Bean
		@ConditionalOnBean(AuditRecordService.class) // TODO Redundant ??
		public AuditRecordController auditController(AuditRecordService auditRecordService) {
			return new AuditRecordController(auditRecordService);
		}
	}

	@Configuration
	public static class SecurityConfiguration {

		@Bean
		public SpringSecurityAuditorAware springSecurityAuditorAware(SecurityStateBean securityStateBean) {
			return new SpringSecurityAuditorAware(securityStateBean);
		}

		@Bean
		public SecurityStateBean securityStateBean() {
			return new SecurityStateBean();
		}

		@Bean
		public SecurityController securityController(SecurityStateBean securityStateBean) {
			return new SecurityController(securityStateBean);
		}

		@Bean
		@ConfigurationProperties(prefix = "spring.cloud.dataflow.security.authorization")
		public AuthorizationProperties authorizationProperties() {
			return new AuthorizationProperties();
		}

	}

}
