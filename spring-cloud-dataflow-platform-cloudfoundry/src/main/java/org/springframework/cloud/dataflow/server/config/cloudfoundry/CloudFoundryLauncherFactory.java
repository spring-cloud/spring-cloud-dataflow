/*
 * Copyright 2019 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.LauncherFactory;
import org.springframework.cloud.dataflow.server.config.cloudfoundry.CloudFoundryPlatformProperties.CloudFoundryProperties;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundry2630AndLaterTaskLauncher;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.util.RuntimeVersionUtils;
import org.springframework.cloud.scheduler.spi.cloudfoundry.CloudFoundryAppScheduler;
import org.springframework.cloud.scheduler.spi.cloudfoundry.CloudFoundrySchedulerProperties;
import org.springframework.cloud.scheduler.spi.core.Scheduler;

/**
 * @author David Turanski
 **/
public class CloudFoundryLauncherFactory implements LauncherFactory {

	private final Map<String, CloudFoundryProperties> accounts;

	private final boolean schedulerEnabled;

	private final static String PLATFORM_TYPE = "Cloud Foundry";

	private final static Logger logger = LoggerFactory.getLogger(CloudFoundryLauncherFactory.class);

	private final CloudFoundrySchedulerProperties schedulerProperties;

	public CloudFoundryLauncherFactory(CloudFoundryPlatformProperties cloudFoundryPlatformProperties,
			Optional<CloudFoundrySchedulerProperties> schedulerProperties,
			boolean schedulerEnabled) {
		this.accounts = cloudFoundryPlatformProperties.getAccounts();
		this.schedulerEnabled = schedulerEnabled;
		this.schedulerProperties = schedulerProperties.orElseGet(CloudFoundrySchedulerProperties::new);
	}

	@Override
	public List<Launcher> createLaunchers() {
		List<Launcher> launchers = new ArrayList<>();

		for (String key : keys()) {
			try {
				launchers.add(create(key));
			}
			catch (Exception e) {
				logger.error("Cloud Foundry platform account [{}] could not be registered: {}",
					key, e.getMessage());
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		return launchers;
	}

	private Launcher create(String key) {

		ConnectionContext connectionContext = connectionContext(key);
		TokenProvider tokenProvider = tokenProvider(key);
		CloudFoundryClient cloudFoundryClient = cloudFoundryClient(connectionContext, tokenProvider, key);
		CloudFoundryOperations cloudFoundryOperations = cloudFoundryOperations(cloudFoundryClient, key);
		CloudFoundry2630AndLaterTaskLauncher taskLauncher = new CloudFoundry2630AndLaterTaskLauncher(
				cloudFoundryClient,
				deploymentProperties(key),
				cloudFoundryOperations,
				runtimeEnvironmentInfo(cloudFoundryClient, key));
		return new Launcher(key, PLATFORM_TYPE, taskLauncher,
				scheduler(
						key,
						taskLauncher,
						cloudFoundryOperations,
						connectionContext,
						tokenProvider));
	}

	private Set<String> keys() {
		return accounts.keySet();
	}

	private Scheduler scheduler(String key, CloudFoundry2630AndLaterTaskLauncher taskLauncher,
			CloudFoundryOperations cloudFoundryOperations, ConnectionContext connectionContext,
			TokenProvider tokenProvider) {
		Scheduler scheduler = null;
		if (schedulerEnabled) {
			ReactorSchedulerClient reactorSchedulerClient = ReactorSchedulerClient.builder()
					.connectionContext(connectionContext)
					.tokenProvider(tokenProvider)
					.root(Mono.just(schedulerProperties.getSchedulerUrl()))
					.build();

			scheduler = new CloudFoundryAppScheduler(reactorSchedulerClient,
					cloudFoundryOperations,
					connectionProperties(key),
					taskLauncher,
					schedulerProperties);
		}
		return scheduler;
	}

	private ConnectionContext connectionContext(String key) {
		CloudFoundryConnectionProperties connectionProperties = connectionProperties(key);
		return DefaultConnectionContext.builder()
				.apiHost(connectionProperties.getUrl().getHost())
				.skipSslValidation(connectionProperties.isSkipSslValidation())
				.build();

	}

	private TokenProvider tokenProvider(String key) {
		CloudFoundryConnectionProperties connectionProperties = connectionProperties(key);
		return PasswordGrantTokenProvider.builder()
				.username(connectionProperties.getUsername())
				.password(connectionProperties.getPassword()).build();
	}

	private CloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext, TokenProvider tokenProvider,
			String key) {
		return ReactorCloudFoundryClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	private CloudFoundryConnectionProperties connectionProperties(String key) {
		return this.accounts.get(key).getConnection();
	}

	private CloudFoundryDeploymentProperties deploymentProperties(String key) {
		// todo: use server level shared deployment properties
		return this.accounts.get(key).getDeployment() == null ? new CloudFoundryDeploymentProperties()
				: this.accounts.get(key).getDeployment();
	}

	private CloudFoundryOperations cloudFoundryOperations(CloudFoundryClient cloudFoundryClient, String key) {
		return DefaultCloudFoundryOperations
				.builder().cloudFoundryClient(cloudFoundryClient)
				.organization(connectionProperties(key).getOrg())
				.space(connectionProperties(key).getSpace()).build();
	}

	private Version version(CloudFoundryClient cloudFoundryClient, String key) {
		return cloudFoundryClient.info()
				.get(GetInfoRequest.builder().build())
				.map(response -> Version.valueOf(response.getApiVersion()))
				.doOnNext(versionInfo -> logger.info(
						"Connecting to Cloud Foundry with API Version {}",
						versionInfo))
				.timeout(Duration.ofSeconds(deploymentProperties(key).getApiTimeout()))
				.block();
	}

	private RuntimeEnvironmentInfo runtimeEnvironmentInfo(CloudFoundryClient cloudFoundryClient, String key) {
		return new RuntimeEnvironmentInfo.Builder()
				.implementationName(CloudFoundryAppDeployer.class.getSimpleName())
				.spiClass(AppDeployer.class)
				.implementationVersion(
						RuntimeVersionUtils.getVersion(CloudFoundryAppDeployer.class))
				.platformType("Cloud Foundry")
				.platformClientVersion(
						RuntimeVersionUtils.getVersion(cloudFoundryClient.getClass()))
				.platformApiVersion(version(cloudFoundryClient, key).toString()).platformHostVersion("unknown")
				.addPlatformSpecificInfo("API Endpoint",
						connectionProperties(key).getUrl().toString())
				.build();
	}
}
