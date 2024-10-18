/*
 * Copyright 2017-2022 the original author or authors.
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
import org.cloudfoundry.logcache.v1.LogCacheClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.logcache.v1.ReactorLogCacheClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.spi.app.ActuatorOperations;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.ApplicationLogAccessor;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryActuatorTemplate;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * @author Donovan Muller
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 * @author David Turanski
 */
@AutoConfiguration
@ConditionalOnBean(EnableSkipperServerConfiguration.Marker.class)
@EnableConfigurationProperties(CloudFoundryPlatformProperties.class)
@Import(CloudFoundrySkipperServerConfiguration.class)
public class CloudFoundryPlatformAutoConfiguration {

	private static final Logger logger = LoggerFactory
		.getLogger(CloudFoundryPlatformAutoConfiguration.class);

	@Bean
	public Platform cloudFoundryPlatform(
		CloudFoundryPlatformProperties cloudFoundryPlatformProperties,
		RestTemplate actuatorRestTemplate,
		ApplicationContext applicationContext
	) {
		List<Deployer> deployers = cloudFoundryPlatformProperties.getAccounts().entrySet().stream().map(
			e -> createAndSaveCFAppDeployer(e.getKey(), e.getValue(), actuatorRestTemplate, applicationContext)
		).collect(Collectors.toList());
		return new Platform("Cloud Foundry", deployers);
	}

	@Bean
	@ConditionalOnMissingBean
	RestTemplate actuatorRestTemplate() {
		return new RestTemplate();
	}

	private Deployer createAndSaveCFAppDeployer(
		String account,
		CloudFoundryPlatformProperties.CloudFoundryProperties cloudFoundryProperties,
		RestTemplate restTemplate,
		ApplicationContext applicationContext
	) {
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
			Builder tokenProviderBuilder = PasswordGrantTokenProvider.builder()
				.username(connectionProperties.getUsername())
				.password(connectionProperties.getPassword())
				.loginHint(connectionProperties.getLoginHint());
			if (StringUtils.hasText(connectionProperties.getClientId())) {
				tokenProviderBuilder.clientId(connectionProperties.getClientId());
			}
			if (StringUtils.hasText(connectionProperties.getClientSecret())) {
				tokenProviderBuilder.clientSecret(connectionProperties.getClientSecret());
			}
			TokenProvider tokenProvider = tokenProviderBuilder.build();

			CloudFoundryClient cloudFoundryClient = ReactorCloudFoundryClient.builder()
					.connectionContext(connectionContext)
					.tokenProvider(tokenProvider)
					.build();
			LogCacheClient logCacheClient = ReactorLogCacheClient.builder()
					.connectionContext(connectionContext)
					.tokenProvider(tokenProvider)
					.build();
			Version version = cloudFoundryClient.info()
				.get(GetInfoRequest.builder().build())
				.map(response -> Version.parse(response.getApiVersion()))
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
				appNameGenerator,
				deploymentProperties,
				cloudFoundryOperations,
				runtimeEnvironmentInfo,
				new ApplicationLogAccessor(logCacheClient));
			ActuatorOperations actuatorOperations = new CloudFoundryActuatorTemplate(
				restTemplate, cfAppDeployer, cloudFoundryProperties
				.getDeployment().getAppAdmin());
			Deployer deployer = new Deployer(account, "cloudfoundry", cfAppDeployer, actuatorOperations);
			deployer.setDescription("org = [%s], space = [%s], url = [%s]".formatted(
					connectionProperties.getOrg(), connectionProperties.getSpace(),
					connectionProperties.getUrl()));
			return deployer;
		} catch (Exception e) {
			logger.error("Cloud Foundry platform account [{}] could not be registered: {}",
				account, e.getMessage());
			throw new SkipperException(e.getMessage());
		}
	}
}
