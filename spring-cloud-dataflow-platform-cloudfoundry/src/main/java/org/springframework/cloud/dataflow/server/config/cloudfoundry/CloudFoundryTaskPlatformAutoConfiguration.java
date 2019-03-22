/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.cloudfoundry;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.zafarkhaja.semver.Version;
import io.pivotal.reactor.scheduler.ReactorSchedulerClient;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundry2630AndLaterTaskLauncher;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppNameGenerator;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.util.RuntimeVersionUtils;
import org.springframework.cloud.scheduler.spi.cloudfoundry.CloudFoundryAppScheduler;
import org.springframework.cloud.scheduler.spi.cloudfoundry.CloudFoundrySchedulerProperties;
import org.springframework.cloud.scheduler.spi.core.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates TaskPlatform implementations to launch/schedule tasks on Cloud Foundry.
 * @author Mark Pollack
 */
@Configuration
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
@EnableConfigurationProperties(CloudFoundryPlatformProperties.class)
public class CloudFoundryTaskPlatformAutoConfiguration {

	@Value("${spring.cloud.dataflow.features.schedules-enabled:false}")
	private boolean schedulesEnabled;

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CloudFoundryTaskPlatformAutoConfiguration.class);

	@Bean
	public TaskPlatform cloudFoundryPlatform(
			CloudFoundryPlatformProperties cloudFoundryPlatformProperties,
			Optional<CloudFoundrySchedulerProperties> cloudFoundrySchedulerProperties) {
		List<Launcher> launchers = cloudFoundryPlatformProperties.getAccounts().entrySet().stream().map(
				e -> createAndSaveCFTaskLauncher(e.getKey(), e.getValue(), cloudFoundrySchedulerProperties)).collect(Collectors.toList());
		return new TaskPlatform("Cloud Foundry", launchers);
	}

	private Launcher createAndSaveCFTaskLauncher(String launcherName,
												 CloudFoundryPlatformProperties.CloudFoundryProperties cloudFoundryProperties,
												 Optional<CloudFoundrySchedulerProperties> cloudFoundrySchedulerProperties) {
		CloudFoundryDeploymentProperties deploymentProperties = cloudFoundryProperties
				.getDeployment();
		if (deploymentProperties == null) {
			// todo: use server level shared deployment properties
			deploymentProperties = new CloudFoundryDeploymentProperties();
		}
		CloudFoundryConnectionProperties connectionProperties = cloudFoundryProperties
				.getConnection();
		try {
			ConnectionContext connectionContext = DefaultConnectionContext.builder()
					.apiHost(connectionProperties.getUrl().getHost())
					.skipSslValidation(connectionProperties.isSkipSslValidation())
					.build();
			TokenProvider tokenProvider = PasswordGrantTokenProvider.builder()
					.username(connectionProperties.getUsername())
					.password(connectionProperties.getPassword()).build();
			CloudFoundryClient cloudFoundryClient = ReactorCloudFoundryClient.builder()
					.connectionContext(connectionContext).tokenProvider(tokenProvider)
					.build();
			Version version = cloudFoundryClient.info()
					.get(GetInfoRequest.builder().build())
					.map(response -> Version.valueOf(response.getApiVersion()))
					.doOnNext(versionInfo -> logger.info(
							"Connecting to Cloud Foundry with API Version {}",
							versionInfo))
					.timeout(Duration.ofSeconds(deploymentProperties.getApiTimeout()))
					.block();
			RuntimeEnvironmentInfo runtimeEnvironmentInfo = new RuntimeEnvironmentInfo.Builder()
					.implementationName(CloudFoundryAppDeployer.class.getSimpleName())
					.spiClass(AppDeployer.class)
					.implementationVersion(
							RuntimeVersionUtils.getVersion(CloudFoundryAppDeployer.class))
					.platformType("Cloud Foundry")
					.platformClientVersion(
							RuntimeVersionUtils.getVersion(cloudFoundryClient.getClass()))
					.platformApiVersion(version.toString()).platformHostVersion("unknown")
					.addPlatformSpecificInfo("API Endpoint",
							connectionProperties.getUrl().toString())
					.build();
			CloudFoundryOperations cloudFoundryOperations = DefaultCloudFoundryOperations
					.builder().cloudFoundryClient(cloudFoundryClient)
					.organization(connectionProperties.getOrg())
					.space(connectionProperties.getSpace()).build();
			CloudFoundryAppNameGenerator appNameGenerator = new CloudFoundryAppNameGenerator(
					cloudFoundryProperties.getDeployment());
			appNameGenerator.afterPropertiesSet();

			CloudFoundry2630AndLaterTaskLauncher cfTaskLauncher = new CloudFoundry2630AndLaterTaskLauncher(
					cloudFoundryClient, deploymentProperties, cloudFoundryOperations, runtimeEnvironmentInfo);

			Scheduler scheduler = null;
			if (schedulesEnabled) {
				CloudFoundrySchedulerProperties propsToUse =
						cloudFoundrySchedulerProperties.orElseGet(CloudFoundrySchedulerProperties::new);
				ReactorSchedulerClient reactorSchedulerClient = ReactorSchedulerClient.builder()
						.connectionContext(connectionContext)
						.tokenProvider(tokenProvider)
						.root(Mono.just(propsToUse.getSchedulerUrl()))
						.build();

				scheduler = new CloudFoundryAppScheduler(reactorSchedulerClient,
						cloudFoundryOperations,
						connectionProperties,
						cfTaskLauncher,
						propsToUse);
			}
			Launcher launcher = new Launcher(launcherName, "cloudfoundry", cfTaskLauncher, scheduler);
			launcher.setDescription(String.format("org = [%s], space = [%s], url = [%s]",
					connectionProperties.getOrg(), connectionProperties.getSpace(),
					connectionProperties.getUrl()));
			return launcher;
		}
		catch (Exception e) {
			logger.error("Cloud Foundry platform account [{}] could not be registered: {}",
					launcherName, e.getMessage());
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

}
