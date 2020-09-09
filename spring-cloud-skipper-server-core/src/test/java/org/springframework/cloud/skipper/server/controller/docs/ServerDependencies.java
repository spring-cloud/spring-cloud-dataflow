/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.cloud.skipper.server.controller.docs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.cloud.common.security.AuthorizationProperties;
import org.springframework.cloud.common.security.support.SecurityStateBean;
import org.springframework.cloud.deployer.autoconfigure.ResourceLoadingAutoConfiguration;
import org.springframework.cloud.deployer.resource.docker.DockerResourceLoader;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifestReader;
import org.springframework.cloud.skipper.io.DefaultPackageReader;
import org.springframework.cloud.skipper.io.DefaultPackageWriter;
import org.springframework.cloud.skipper.io.PackageReader;
import org.springframework.cloud.skipper.io.PackageWriter;
import org.springframework.cloud.skipper.server.config.LocalPlatformProperties;
import org.springframework.cloud.skipper.server.config.SkipperServerConfiguration;
import org.springframework.cloud.skipper.server.config.SkipperServerPlatformConfiguration;
import org.springframework.cloud.skipper.server.config.SkipperServerProperties;
import org.springframework.cloud.skipper.server.controller.AboutController;
import org.springframework.cloud.skipper.server.controller.PackageController;
import org.springframework.cloud.skipper.server.controller.ReleaseController;
import org.springframework.cloud.skipper.server.controller.RootController;
import org.springframework.cloud.skipper.server.controller.SkipperErrorAttributes;
import org.springframework.cloud.skipper.server.controller.VersionInfoProperties;
import org.springframework.cloud.skipper.server.deployer.AppDeploymentRequestFactory;
import org.springframework.cloud.skipper.server.deployer.DefaultReleaseManager;
import org.springframework.cloud.skipper.server.deployer.DefaultReleaseManagerFactory;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalyzer;
import org.springframework.cloud.skipper.server.deployer.ReleaseManager;
import org.springframework.cloud.skipper.server.deployer.ReleaseManagerFactory;
import org.springframework.cloud.skipper.server.deployer.strategies.DefaultUpgradeStrategyFactory;
import org.springframework.cloud.skipper.server.deployer.strategies.DeleteStep;
import org.springframework.cloud.skipper.server.deployer.strategies.DeployAppStep;
import org.springframework.cloud.skipper.server.deployer.strategies.HandleHealthCheckStep;
import org.springframework.cloud.skipper.server.deployer.strategies.HealthCheckProperties;
import org.springframework.cloud.skipper.server.deployer.strategies.HealthCheckStep;
import org.springframework.cloud.skipper.server.deployer.strategies.SimpleRedBlackUpgradeStrategy;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategy;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategyFactory;
import org.springframework.cloud.skipper.server.index.PackageMetadataResourceProcessor;
import org.springframework.cloud.skipper.server.index.PackageSummaryResourceProcessor;
import org.springframework.cloud.skipper.server.index.SkipperLinksResourceProcessor;
import org.springframework.cloud.skipper.server.repository.jpa.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.jpa.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.cloud.skipper.server.repository.jpa.RepositoryRepository;
import org.springframework.cloud.skipper.server.repository.map.DeployerRepository;
import org.springframework.cloud.skipper.server.service.PackageMetadataService;
import org.springframework.cloud.skipper.server.service.PackageService;
import org.springframework.cloud.skipper.server.service.ReleaseReportService;
import org.springframework.cloud.skipper.server.service.ReleaseService;
import org.springframework.cloud.skipper.server.service.ReleaseStateUpdateService;
import org.springframework.cloud.skipper.server.service.RepositoryInitializationService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.mockito.Mockito.mock;

/**
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@EnableConfigurationProperties({ SkipperServerProperties.class, VersionInfoProperties.class,
		LocalPlatformProperties.class, HealthCheckProperties.class })
@EntityScan({ "org.springframework.cloud.skipper.domain",
		"org.springframework.cloud.skipper.server.domain" })
@EnableAsync
@ImportAutoConfiguration(classes = { JacksonAutoConfiguration.class, EmbeddedDataSourceConfiguration.class,
		HibernateJpaAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class,
		ErrorMvcAutoConfiguration.class, StateMachineJpaRepositoriesAutoConfiguration.class,
		SkipperServerPlatformConfiguration.class, ResourceLoadingAutoConfiguration.class })
@Import(RepositoryConfiguration.class)
@WebAppConfiguration
public class ServerDependencies implements AsyncConfigurer {

	private final Logger logger = LoggerFactory.getLogger(SkipperServerConfiguration.class);

	@Bean
	public ErrorAttributes errorAttributes() {
		// override boot's DefaultErrorAttributes
		return new SkipperErrorAttributes();
	}

	@Bean
	public PackageSummaryResourceProcessor packageSummaryResourceProcessor() {
		return new PackageSummaryResourceProcessor();
	}

	@Bean
	public PackageMetadataResourceProcessor packageMetadataResourceProcessor() {
		return new PackageMetadataResourceProcessor();
	}

	@Bean
	public SkipperLinksResourceProcessor skipperControllerResourceProcessor() {
		return new SkipperLinksResourceProcessor();
	}

	@Bean
	public SkipperStateMachineService skipperStateMachineService() {
		return mock(SkipperStateMachineService.class);
	}

	@Bean
	public ReleaseController releaseController(ReleaseService releaseService,
			SkipperStateMachineService skipperStateMachineService) {
		return new ReleaseController(releaseService, skipperStateMachineService);
	}

	@Bean
	public PackageController packageController(PackageService packageService,
			PackageMetadataService packageMetadataService, SkipperStateMachineService skipperStateMachineService) {
		return new PackageController(packageService, packageMetadataService, skipperStateMachineService);
	}

	@Bean
	public AboutController aboutController(VersionInfoProperties versionInfoProperties) {
		return new AboutController(versionInfoProperties);
	}

	@Bean
	public RootController rootController() {
		return new RootController();
	}

	// Services

	@Bean
	public PackageMetadataService packageMetadataService(RepositoryRepository repositoryRepository,
			PackageMetadataRepository packageMetadataRepository,
			ReleaseRepository releaseRepository) {
		return new PackageMetadataService(repositoryRepository,
				packageMetadataRepository,
				releaseRepository);
	}

	@Bean
	public PackageService packageService() {
		return mock(PackageService.class);
	}

	@Bean
	public DelegatingResourceLoader delegatingResourceLoader(MavenProperties mavenProperties) {
		DockerResourceLoader dockerLoader = new DockerResourceLoader();
		MavenResourceLoader mavenResourceLoader = new MavenResourceLoader(mavenProperties);
		Map<String, ResourceLoader> loaders = new HashMap<>();
		loaders.put("docker", dockerLoader);
		loaders.put("maven", mavenResourceLoader);
		return new DelegatingResourceLoader(loaders);
	}

	@Bean
	public ReleaseReportService releaseReportService() {
		return mock(ReleaseReportService.class);
	}

	@Bean
	public ReleaseManagerFactory releaseManagerFactory(List<ReleaseManager> managers) {
		return new DefaultReleaseManagerFactory(managers);
	}

	@Bean
	public UpgradeStrategyFactory upgradeStrategyFactory(List<UpgradeStrategy> strategies) {
		return new DefaultUpgradeStrategyFactory(strategies);
	}

	@Bean
	public ReleaseService releaseService() {
		return mock(ReleaseService.class);
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.cloud.skipper.server", name = "enableReleaseStateUpdateService", matchIfMissing = true)
	public ReleaseStateUpdateService releaseStateUpdateService(ReleaseManagerFactory releaseManagerFactory,
			ReleaseRepository releaseRepository) {
		return new ReleaseStateUpdateService(releaseManagerFactory, releaseRepository);
	}

	@Bean
	public RepositoryInitializationService repositoryInitializationService(RepositoryRepository repositoryRepository,
			PackageMetadataRepository packageMetadataRepository,
			PackageMetadataService packageMetadataService,
			SkipperServerProperties skipperServerProperties) {
		return new RepositoryInitializationService(repositoryRepository, packageMetadataRepository,
				packageMetadataService, skipperServerProperties);
	}

	// Deployer Package

	@Bean
	public DefaultReleaseManager defaultReleaseManager(ReleaseRepository releaseRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			DeployerRepository deployerRepository,
			ReleaseAnalyzer releaseAnalyzer,
			AppDeploymentRequestFactory appDeploymentRequestFactory,
			SpringCloudDeployerApplicationManifestReader applicationManifestReader) {
		return new DefaultReleaseManager(releaseRepository, appDeployerDataRepository, deployerRepository,
				releaseAnalyzer, appDeploymentRequestFactory, applicationManifestReader);
	}

	@Bean
	public SpringCloudDeployerApplicationManifestReader applicationSpecReader() {
		return new SpringCloudDeployerApplicationManifestReader();
	}

	@Bean
	public DeleteStep deleteStep(ReleaseRepository releaseRepository, DeployerRepository deployerRepository) {
		return new DeleteStep(releaseRepository, deployerRepository);
	}

	@Bean
	public UpgradeStrategy updateStrategy(HealthCheckStep healthCheckStep,
			HandleHealthCheckStep healthCheckAndDeleteStep,
			DeployAppStep deployAppStep) {
		return new SimpleRedBlackUpgradeStrategy(healthCheckStep, healthCheckAndDeleteStep,
				deployAppStep);
	}

	@Bean
	public HealthCheckStep healthCheckStep(AppDeployerDataRepository appDeployerDataRepository,
			DeployerRepository deployerRepository,
			SpringCloudDeployerApplicationManifestReader applicationManifestReader) {
		return new HealthCheckStep(appDeployerDataRepository, deployerRepository,
				applicationManifestReader/* , cfApplicationManifestReader, cfManifestApplicationDeployer */);
	}

	@Bean
	public DeployAppStep DeployAppStep() {
		return mock(DeployAppStep.class);
	}

	@Bean
	public HandleHealthCheckStep healthCheckAndDeleteStep(ReleaseRepository releaseRepository,
			AppDeployerDataRepository appDeployerDataRepository, DeleteStep deleteStep) {
		return new HandleHealthCheckStep(releaseRepository, appDeployerDataRepository, deleteStep);
	}

	@Bean(name = SkipperServerConfiguration.SKIPPER_EXECUTOR)
	@Override
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setCorePoolSize(5);
		threadPoolTaskExecutor.setMaxPoolSize(10);
		threadPoolTaskExecutor.setThreadNamePrefix("StateUpdate-");
		return threadPoolTaskExecutor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (throwable, method, objects) -> logger.error("Exception thrown in @Async Method " + method.getName(),
				throwable);
	}

	@Bean
	public ReleaseAnalyzer releaseAnalysisService(
			SpringCloudDeployerApplicationManifestReader applicationManifestReader,
			DelegatingResourceLoader delegatingResourceLoader) {
		return new ReleaseAnalyzer(applicationManifestReader, delegatingResourceLoader);
	}

	@Bean
	public AppDeploymentRequestFactory appDeploymentRequestFactory(DelegatingResourceLoader delegatingResourceLoader) {
		return new AppDeploymentRequestFactory(delegatingResourceLoader);
	}

	@Bean
	public PackageReader packageReader() {
		return new DefaultPackageReader();
	}

	@Bean
	public PackageWriter packageWriter() {
		return new DefaultPackageWriter();
	}

	@Bean
	public SecurityStateBean securityStateBean() {
		return new SecurityStateBean();
	}

	@Bean
	@ConfigurationProperties(prefix = "spring.cloud.skipper.security.authorization")
	public AuthorizationProperties authorizationProperties() {
		return new AuthorizationProperties();
	}
}
