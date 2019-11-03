/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.cloud.skipper.server.autoconfigure;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import com.github.zafarkhaja.semver.Version;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppNameGenerator;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.util.RuntimeVersionUtils;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.deployer.cloudfoundry.CloudFoundryPlatformProperties;
import org.springframework.cloud.skipper.deployer.cloudfoundry.CloudFoundrySkipperServerConfiguration;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Platform;
import org.springframework.cloud.skipper.server.config.EnableSkipperServerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Donovan Muller
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
@Configuration
@ConditionalOnBean(EnableSkipperServerConfiguration.Marker.class)
@EnableConfigurationProperties(CloudFoundryPlatformProperties.class)
@Import(CloudFoundrySkipperServerConfiguration.class)
public class CloudFoundryPlatformAutoConfiguration {

	private static final Logger logger = LoggerFactory
			.getLogger(CloudFoundryPlatformAutoConfiguration.class);

	@Bean
	public Platform cloudFoundryPlatform(
			CloudFoundryPlatformProperties cloudFoundryPlatformProperties) {
		List<Deployer> deployers = cloudFoundryPlatformProperties.getAccounts().entrySet().stream().map(
				e -> createAndSaveCFAppDeployer(e.getKey(), e.getValue())
		).collect(Collectors.toList());
		return new Platform("Cloud Foundry", deployers);
	}

	private Deployer createAndSaveCFAppDeployer(String account,
			CloudFoundryPlatformProperties.CloudFoundryProperties cloudFoundryProperties) {
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
					.password(connectionProperties.getPassword())
					.loginHint(connectionProperties.getLoginHint())
					.build();
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
			ReactorDopplerClient dopplerClient = ReactorDopplerClient.builder()
					.connectionContext(connectionContext)
					.tokenProvider(tokenProvider)
					.build();
			CloudFoundryOperations cloudFoundryOperations = DefaultCloudFoundryOperations
					.builder().cloudFoundryClient(cloudFoundryClient)
					.organization(connectionProperties.getOrg())
					.dopplerClient(dopplerClient)
					.space(connectionProperties.getSpace()).build();
			CloudFoundryAppNameGenerator appNameGenerator = new CloudFoundryAppNameGenerator(
					deploymentProperties);
			appNameGenerator.afterPropertiesSet();
			CloudFoundryAppDeployer cfAppDeployer = new CloudFoundryAppDeployer(
					appNameGenerator, deploymentProperties, cloudFoundryOperations,
					runtimeEnvironmentInfo);
			Deployer deployer = new Deployer(account, "cloudfoundry", cfAppDeployer);
			deployer.setDescription(String.format("org = [%s], space = [%s], url = [%s]",
					connectionProperties.getOrg(), connectionProperties.getSpace(),
					connectionProperties.getUrl()));
			return deployer;
		}
		catch (Exception e) {
			logger.error("Cloud Foundry platform account [{}] could not be registered: {}",
					account, e.getMessage());
			throw new SkipperException(e.getMessage());
		}
	}
}
