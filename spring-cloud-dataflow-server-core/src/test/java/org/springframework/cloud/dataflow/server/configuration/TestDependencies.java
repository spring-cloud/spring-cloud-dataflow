/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.common.security.support.SecurityStateBean;
import org.springframework.cloud.dataflow.completion.CompletionConfiguration;
import org.springframework.cloud.dataflow.completion.StreamCompletionProvider;
import org.springframework.cloud.dataflow.completion.TaskCompletionProvider;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepository;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.registry.service.DefaultAppRegistryService;
import org.springframework.cloud.dataflow.server.ConditionalOnSkipperDisabled;
import org.springframework.cloud.dataflow.server.ConditionalOnSkipperEnabled;
import org.springframework.cloud.dataflow.server.config.MetricsProperties;
import org.springframework.cloud.dataflow.server.config.VersionInfoProperties;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.controller.AboutController;
import org.springframework.cloud.dataflow.server.controller.AppRegistryController;
import org.springframework.cloud.dataflow.server.controller.CompletionController;
import org.springframework.cloud.dataflow.server.controller.MetricsController;
import org.springframework.cloud.dataflow.server.controller.RestControllerAdvice;
import org.springframework.cloud.dataflow.server.controller.RuntimeAppInstanceController;
import org.springframework.cloud.dataflow.server.controller.RuntimeAppsController;
import org.springframework.cloud.dataflow.server.controller.SkipperAppRegistryController;
import org.springframework.cloud.dataflow.server.controller.SkipperStreamDeploymentController;
import org.springframework.cloud.dataflow.server.controller.StreamDefinitionController;
import org.springframework.cloud.dataflow.server.controller.StreamDeploymentController;
import org.springframework.cloud.dataflow.server.controller.TaskDefinitionController;
import org.springframework.cloud.dataflow.server.controller.TaskExecutionController;
import org.springframework.cloud.dataflow.server.controller.TaskSchedulerController;
import org.springframework.cloud.dataflow.server.controller.ToolsController;
import org.springframework.cloud.dataflow.server.controller.support.ApplicationsMetrics;
import org.springframework.cloud.dataflow.server.controller.support.ApplicationsMetrics.Application;
import org.springframework.cloud.dataflow.server.controller.support.ApplicationsMetrics.Instance;
import org.springframework.cloud.dataflow.server.controller.support.ApplicationsMetrics.Metric;
import org.springframework.cloud.dataflow.server.controller.support.MetricStore;
import org.springframework.cloud.dataflow.server.registry.DataFlowAppRegistryPopulator;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryDeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryStreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryStreamDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryTaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.SchedulerServiceProperties;
import org.springframework.cloud.dataflow.server.service.SkipperStreamService;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.impl.AppDeployerStreamService;
import org.springframework.cloud.dataflow.server.service.impl.AppDeploymentRequestCreator;
import org.springframework.cloud.dataflow.server.service.impl.DefaultSchedulerService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultSkipperStreamService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskService;
import org.springframework.cloud.dataflow.server.service.impl.TaskConfigurationProperties;
import org.springframework.cloud.dataflow.server.stream.AppDeployerStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.cloud.dataflow.server.stream.StreamDeployer;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.registry.InMemoryUriRegistry;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.scheduler.spi.core.CreateScheduleException;
import org.springframework.cloud.scheduler.spi.core.ScheduleInfo;
import org.springframework.cloud.scheduler.spi.core.ScheduleRequest;
import org.springframework.cloud.scheduler.spi.core.Scheduler;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Michael Minella
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
@Configuration
@EnableSpringDataWebSupport
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@Import(CompletionConfiguration.class)
@ImportAutoConfiguration({ HibernateJpaAutoConfiguration.class, EmbeddedDataSourceConfiguration.class })
@EnableWebMvc
@EnableConfigurationProperties({ CommonApplicationProperties.class,
		MetricsProperties.class,
		VersionInfoProperties.class })
@EntityScan({ "org.springframework.cloud.dataflow.registry.domain" })
@EnableJpaRepositories(basePackages = "org.springframework.cloud.dataflow.registry.repository")
@EnableTransactionManagement
public class TestDependencies extends WebMvcConfigurationSupport {

	@Bean
	public RestControllerAdvice restControllerAdvice() {
		return new RestControllerAdvice();
	}

	@Bean
	public MavenProperties mavenProperties() {
		MavenProperties mavenProperties = new MavenProperties();
		mavenProperties.setRemoteRepositories(new HashMap<>(Collections.singletonMap("springRepo",
				new MavenProperties.RemoteRepository("https://repo.spring.io/libs-snapshot"))));
		return mavenProperties;
	}

	@Bean
	public DelegatingResourceLoader resourceLoader(MavenProperties mavenProperties) {
		Map<String, ResourceLoader> resourceLoaders = new HashMap<>();
		resourceLoaders.put("maven", new MavenResourceLoader(mavenProperties));
		resourceLoaders.put("file", new FileSystemResourceLoader());

		DelegatingResourceLoader delegatingResourceLoader = new DelegatingResourceLoader(resourceLoaders);
		return delegatingResourceLoader;
	}

	@Bean
	@ConditionalOnSkipperDisabled
	public StreamDeploymentController streamDeploymentController(StreamDefinitionRepository repository,
			StreamService streamService) {
		return new StreamDeploymentController(repository, streamService);
	}

	@Bean
	@ConditionalOnSkipperEnabled
	public SkipperStreamDeploymentController updatableStreamDeploymentController(StreamDefinitionRepository repository,
			SkipperStreamService skipperStreamService) {
		return new SkipperStreamDeploymentController(repository, skipperStreamService);
	}

	@Bean
	public FeaturesProperties featuresProperties() {
		return new FeaturesProperties();
	}

	@Bean
	@ConditionalOnSkipperEnabled
	public SkipperStreamService skipperStreamService(StreamDefinitionRepository streamDefinitionRepository,
			SkipperStreamDeployer skipperStreamDeployer, AppDeploymentRequestCreator appDeploymentRequestCreator,
			AppRegistryCommon appRegistry) {
		return new DefaultSkipperStreamService(streamDefinitionRepository, skipperStreamDeployer,
				appDeploymentRequestCreator, appRegistry);
	}

	@Bean
	@ConditionalOnSkipperDisabled
	public StreamService simpleStreamService(StreamDefinitionRepository streamDefinitionRepository,
			AppDeployerStreamDeployer appDeployerStreamDeployer,
			AppDeploymentRequestCreator appDeploymentRequestCreator, AppRegistryCommon appRegistry) {
		return new AppDeployerStreamService(streamDefinitionRepository, appDeployerStreamDeployer,
				appDeploymentRequestCreator, appRegistry);
	}

	@Bean
	public AppDeploymentRequestCreator streamDeploymentPropertiesUtils(AppRegistryCommon appRegistryCommon,
			CommonApplicationProperties commonApplicationProperties,
			ApplicationConfigurationMetadataResolver applicationConfigurationMetadataResolver) {
		return new AppDeploymentRequestCreator(appRegistryCommon,
				commonApplicationProperties,
				applicationConfigurationMetadataResolver);
	}

	@Bean
	@ConditionalOnSkipperDisabled
	public AppDeployerStreamDeployer appDeployerStreamDeployer(AppDeployer appDeployer,
			DeploymentIdRepository deploymentIdRepository, StreamDefinitionRepository streamDefinitionRepository,
			StreamDeploymentRepository streamDeploymentRepository) {
		return new AppDeployerStreamDeployer(appDeployer, deploymentIdRepository, streamDefinitionRepository,
				streamDeploymentRepository, new ForkJoinPool(2));
	}

	@Bean
	@ConditionalOnSkipperEnabled
	public SkipperStreamDeployer skipperStreamDeployer(SkipperClient skipperClient,
			AppRegistryService appRegistryService,
			StreamDefinitionRepository streamDefinitionRepository) {
		return new SkipperStreamDeployer(skipperClient, streamDefinitionRepository, appRegistryService,
				new ForkJoinPool(2));
	}

	@Bean
	@ConditionalOnSkipperEnabled
	public SkipperClient skipperClient() {
		return mock(SkipperClient.class);
	}

	@Bean
	public StreamDefinitionController streamDefinitionController(StreamDefinitionRepository repository,
			StreamService streamService) {
		return new StreamDefinitionController(streamService);
	}

	@Bean
	public MethodValidationPostProcessor methodValidationPostProcessor() {
		return new MethodValidationPostProcessor();
	}

	@Bean
	public CompletionController completionController(StreamCompletionProvider streamCompletionProvider,
			TaskCompletionProvider taskCompletionProvider) {
		return new CompletionController(streamCompletionProvider, taskCompletionProvider);
	}

	@Bean
	public ToolsController toolsController() {
		return new ToolsController();
	}

	@Bean
	@ConditionalOnSkipperDisabled
	public AppRegistryController appRegistryController(AppRegistry registry,
			ApplicationConfigurationMetadataResolver metadataResolver) {
		return new AppRegistryController(registry, metadataResolver, new ForkJoinPool(2));
	}

	@Bean
	@ConditionalOnSkipperDisabled
	public AppRegistry appRegistry(UriRegistry uriRegistry, DelegatingResourceLoader resourceLoader) {
		return new AppRegistry(uriRegistry, resourceLoader);
	}

	@Bean
	@ConditionalOnSkipperEnabled
	public AppRegistryService appRegistryService(AppRegistrationRepository appRegistrationRepository,
			MavenProperties mavenProperties) {
		return new DefaultAppRegistryService(appRegistrationRepository, resourceLoader(mavenProperties), mavenProperties);
	}

	@Bean
	@ConditionalOnSkipperEnabled
	public SkipperAppRegistryController versionedAppRegistryController(
			StreamDefinitionRepository streamDefinitionRepository,
			StreamService streamService,
			AppRegistryService appRegistry,
			ApplicationConfigurationMetadataResolver metadataResolver, MavenProperties mavenProperties) {
		return new SkipperAppRegistryController(streamDefinitionRepository, streamService, appRegistry, metadataResolver,
				new ForkJoinPool(2), mavenProperties);
	}

	@Bean
	public MetricsController metricsController(MetricStore metricStore) {
		return new MetricsController(metricStore);
	}

	@Bean
	public RuntimeAppsController runtimeAppsController(MetricStore metricStore, StreamDeployer streamDeployer) {
		return new RuntimeAppsController(streamDeployer);
	}

	@Bean
	@ConditionalOnBean({ StreamDefinitionRepository.class, StreamDeploymentRepository.class })
	public RuntimeAppInstanceController appInstanceController(StreamDeployer streamDeployer) {
		return new RuntimeAppInstanceController(streamDeployer);
	}

	@Bean
	public MetricStore metricStore(MetricsProperties metricsProperties) {
		return new MetricStore(metricsProperties) {
			@Override
			public List<ApplicationsMetrics> getMetrics() {
				List<ApplicationsMetrics> metrics = new ArrayList<>();
				ApplicationsMetrics am = new ApplicationsMetrics();
				am.setName("ticktock1");
				List<Application> applications = new ArrayList<>();
				Application application = new Application();
				application.setName("time");
				List<Instance> instances = new ArrayList<>();
				Instance i = new Instance();
				List<ApplicationsMetrics.Metric> imetrics = new ArrayList<>();
				Metric imetric = new ApplicationsMetrics.Metric();
				imetric.setName("fake1");
				imetric.setValue(111);
				imetrics.add(imetric);
				i.setMetrics(imetrics);
				i.setGuid("34215");
				instances.add(i);
				application.setInstances(instances);

				List<Metric> aggregateMetrics = new ArrayList<>();
				Metric aggregateMetric = new ApplicationsMetrics.Metric();
				aggregateMetric.setName("rate");
				aggregateMetric.setValue("1000");
				aggregateMetrics.add(aggregateMetric);
				application.setAggregateMetrics(aggregateMetrics);

				applications.add(application);
				am.setApplications(applications);
				metrics.add(am);
				return metrics;
			}
		};
	}

	@Bean
	public TaskDefinitionController taskDefinitionController(TaskExplorer explorer, TaskDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository, ApplicationConfigurationMetadataResolver metadataResolver,
			AppRegistryCommon appRegistry, DelegatingResourceLoader delegatingResourceLoader,
			CommonApplicationProperties commonApplicationProperties) {
		return new TaskDefinitionController(explorer, repository,
				taskService(metadataResolver, taskRepository(), deploymentIdRepository, appRegistry, delegatingResourceLoader, commonApplicationProperties));
	}

	@Bean
	public TaskExecutionController taskExecutionController(TaskExplorer explorer,
			ApplicationConfigurationMetadataResolver metadataResolver, DeploymentIdRepository deploymentIdRepository,
			AppRegistryCommon appRegistry, DelegatingResourceLoader delegatingResourceLoader,
			CommonApplicationProperties commonApplicationProperties) {
		return new TaskExecutionController(explorer,
				taskService(metadataResolver, taskRepository(), deploymentIdRepository, appRegistry, delegatingResourceLoader, commonApplicationProperties),
				taskDefinitionRepository());
	}

	@Bean
	public TaskSchedulerController taskSchedulerController(
			SchedulerService schedulerService) {
		return new TaskSchedulerController(schedulerService);
	}

	@Bean
	public TaskRepository taskRepository() {
		return new SimpleTaskRepository(new TaskExecutionDaoFactoryBean());
	}

	@Bean
	@ConditionalOnSkipperDisabled
	public UriRegistry uriRegistry() {
		return new InMemoryUriRegistry();
	}

	@Bean
	@ConditionalOnSkipperDisabled
	public AppRegistry appRegistry(DelegatingResourceLoader resourceLoader) {
		return new AppRegistry(uriRegistry(), resourceLoader);
	}

	@Bean
	@ConditionalOnSkipperEnabled
	public AppRegistryService appRegistryService(AppRegistrationRepository appRegistrationRepository,
			DelegatingResourceLoader resourceLoader, MavenProperties mavenProperties) {
		return new DefaultAppRegistryService(appRegistrationRepository, resourceLoader, mavenProperties);
	}

	@Bean
	public DataFlowAppRegistryPopulator dataflowUriRegistryPopulator(AppRegistryCommon appRegistryCommon) {
		return new DataFlowAppRegistryPopulator(appRegistryCommon, "classpath:META-INF/test-apps.properties");
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
	public TaskExplorer taskExplorer() {
		return mock(TaskExplorer.class);
	}

	@Bean
	public TaskService taskService(ApplicationConfigurationMetadataResolver metadataResolver,
			TaskRepository taskExecutionRepository, DeploymentIdRepository deploymentIdRepository,
			AppRegistryCommon appRegistry, DelegatingResourceLoader delegatingResourceLoader,
			CommonApplicationProperties commonApplicationProperties) {
		return new DefaultTaskService(new DataSourceProperties(), taskDefinitionRepository(), taskExplorer(),
				taskExecutionRepository, appRegistry, delegatingResourceLoader, taskLauncher(), metadataResolver,
				new TaskConfigurationProperties(), deploymentIdRepository, null, commonApplicationProperties);
	}

	@Bean
	Scheduler scheduler() {
		return new SimpleTestScheduler();
	}

	@Bean
	public SchedulerService schedulerService(CommonApplicationProperties commonApplicationProperties,
			Scheduler scheduler, TaskDefinitionRepository taskDefinitionRepository,
			AppRegistryCommon registry, ResourceLoader resourceLoader,
			DataSourceProperties dataSourceProperties,
			ApplicationConfigurationMetadataResolver metaDataResolver) {
		return new DefaultSchedulerService(commonApplicationProperties,
				scheduler, taskDefinitionRepository,
				registry, resourceLoader,
				new TaskConfigurationProperties(),
				dataSourceProperties, null,
				metaDataResolver, new SchedulerServiceProperties());
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
	public StreamDeploymentRepository streamDeploymentRepository() {
		return new InMemoryStreamDeploymentRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	public DeploymentIdRepository deploymentIdRepository() {
		return new InMemoryDeploymentIdRepository();
	}


	@Bean
	public AboutController aboutController(VersionInfoProperties versionInfoProperties, FeaturesProperties featuresProperties) {
		StreamDeployer streamDeployer = mock(StreamDeployer.class);

		RuntimeEnvironmentInfo.Builder builder = new RuntimeEnvironmentInfo.Builder();
		if (!featuresProperties.isSkipperEnabled()) {
			RuntimeEnvironmentInfo appDeployerEnvInfo = builder.implementationName("testAppDepImplementationName").
					implementationVersion("testAppDepImplementationVersion").
					platformType("testAppDepPlatformType").
					platformApiVersion("testAppDepPlatformApiVersion").
					platformClientVersion("testAppDepPlatformClientVersion").spiClass(Class.class).
					platformHostVersion("testAppDepPlatformHostVersion").build();

			when(streamDeployer.environmentInfo()).thenReturn(appDeployerEnvInfo);
		}
		else {
			RuntimeEnvironmentInfo appDeployerEnvInfoSkipper = builder
					.implementationName("skipper server")
					.implementationVersion("1.0")
					.platformApiVersion("")
					.platformClientVersion("")
					.platformHostVersion("")
					.platformType("Skipper Managed")
					.spiClass(SkipperClient.class).build();

			when(streamDeployer.environmentInfo()).thenReturn(appDeployerEnvInfoSkipper);
		}

		TaskLauncher taskLauncher = mock(TaskLauncher.class);

		RuntimeEnvironmentInfo taskDeployerEnvInfo = new RuntimeEnvironmentInfo.Builder()
				.implementationName("testTaskDepImplementationName")
				.implementationVersion("testTaskDepImplementationVersion")
				.platformType("testTaskDepPlatformType")
				.platformApiVersion("testTaskDepPlatformApiVersion")
				.platformClientVersion("testTaskDepPlatformClientVersion")
				.spiClass(Class.class)
				.platformHostVersion("testTaskDepPlatformHostVersion").build();

		when(taskLauncher.environmentInfo()).thenReturn(taskDeployerEnvInfo);

		return new AboutController(streamDeployer, taskLauncher,
				featuresProperties, versionInfoProperties,
				mock(SecurityStateBean.class));
	}


	public static class SimpleTestScheduler implements Scheduler {
		List<ScheduleInfo> schedules = new ArrayList<>();

		@Override
		public void schedule(ScheduleRequest scheduleRequest) {
			ScheduleInfo schedule = new ScheduleInfo();
			schedule.setScheduleName(scheduleRequest.getScheduleName());
			schedule.setScheduleProperties(scheduleRequest.getSchedulerProperties());
			schedule.setTaskDefinitionName(scheduleRequest.getDefinition().getName());
			List<ScheduleInfo> scheduleInfos = schedules.stream().filter(s -> s.getScheduleName().
					equals(scheduleRequest.getScheduleName())).
					collect(Collectors.toList());
			if(scheduleInfos.size() > 0) {
				throw new CreateScheduleException(
						String.format("Schedule %s already exists",
								scheduleRequest.getScheduleName()), null);
			}
			schedules.add(schedule);

		}

		@Override
		public void unschedule(String scheduleName) {
			schedules = schedules.stream().filter(
					s -> !s.getScheduleName().equals(scheduleName)).
					collect(Collectors.toList());
		}

		@Override
		public List<ScheduleInfo> list(String taskDefinitionName) {
			return schedules.stream().filter(
					s -> s.getTaskDefinitionName().equals(taskDefinitionName)).
					collect(Collectors.toList());
		}

		@Override
		public List<ScheduleInfo> list() {
			return schedules;
		}

		public List<ScheduleInfo> getSchedules() {
			return schedules;
		}
	}
}
