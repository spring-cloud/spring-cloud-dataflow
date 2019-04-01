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
import java.util.Optional;

import com.github.zafarkhaja.semver.Version;
import io.jsonwebtoken.lang.Assert;
import io.pivotal.reactor.scheduler.ReactorSchedulerClient;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.dataflow.core.AbstractTaskPlatformFactory;
import org.springframework.cloud.dataflow.core.Launcher;
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
public class CloudFoundryTaskPlatformFactory extends AbstractTaskPlatformFactory<CloudFoundryPlatformProperties> {

	private final boolean schedulerEnabled;

	private final static String PLATFORM_TYPE = "Cloud Foundry";

	private final static Logger logger = LoggerFactory.getLogger(CloudFoundryTaskPlatformFactory.class);

	private final CloudFoundrySchedulerProperties schedulerProperties;

	private final CloudFoundryPlatformTokenProvider platformTokenProvider;

	private final CloudFoundryPlatformConnectionContextProvider connectionContextProvider;

	private final CloudFoundryPlatformClientProvider cloudFoundryClientProvider;

	private CloudFoundryTaskPlatformFactory(CloudFoundryPlatformProperties cloudFoundryPlatformProperties,
			CloudFoundryPlatformTokenProvider platformTokenProvider,
			CloudFoundryPlatformConnectionContextProvider connectionContextProvider,
			CloudFoundryPlatformClientProvider cloudFoundryClientProvider,
			Optional<CloudFoundrySchedulerProperties> schedulerProperties,
			boolean schedulerEnabled) {
		super(cloudFoundryPlatformProperties, PLATFORM_TYPE);

		this.schedulerEnabled = schedulerEnabled;
		this.schedulerProperties = schedulerProperties.orElseGet(CloudFoundrySchedulerProperties::new);
		this.platformTokenProvider = platformTokenProvider;
		this.connectionContextProvider = connectionContextProvider;
		this.cloudFoundryClientProvider = cloudFoundryClientProvider;
	}

	@Override
	public Launcher createLauncher(String account) {
		ConnectionContext connectionContext = connectionContext(account);
		TokenProvider tokenProvider = tokenProvider(account);
		CloudFoundryClient cloudFoundryClient = cloudFoundryClient(account);
		CloudFoundryOperations cloudFoundryOperations = cloudFoundryOperations(cloudFoundryClient, account);
		CloudFoundry2630AndLaterTaskLauncher taskLauncher = new CloudFoundry2630AndLaterTaskLauncher(
				cloudFoundryClient,
				deploymentProperties(account),
				cloudFoundryOperations,
				runtimeEnvironmentInfo(cloudFoundryClient, account));
		Launcher launcher =  new Launcher(account, PLATFORM_TYPE, taskLauncher,
				scheduler(
						account,
						taskLauncher,
						cloudFoundryOperations,
						connectionContext,
						tokenProvider));
		CloudFoundryConnectionProperties connectionProperties = connectionProperties(account);
		launcher.setDescription(String.format("org = [%s], space = [%s], url = [%s]",
			connectionProperties.getOrg(), connectionProperties.getSpace(),
			connectionProperties.getUrl()));
		return launcher;
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

	private ConnectionContext connectionContext(String account) {
		return connectionContextProvider.connectionContext(account);
	}

	private TokenProvider tokenProvider(String account) {
		return platformTokenProvider.tokenProvider(account);
	}

	private CloudFoundryClient cloudFoundryClient(String account) {
		return cloudFoundryClientProvider.cloudFoundryClient(account);
	}

	private CloudFoundryConnectionProperties connectionProperties(String account) {
		return platformProperties.accountProperties(account).getConnection();
	}

	private CloudFoundryDeploymentProperties deploymentProperties(String account) {
		// todo: use server level shared deployment properties
		return platformProperties.accountProperties(account).getDeployment() == null
				? new CloudFoundryDeploymentProperties()
				: platformProperties.accountProperties(account).getDeployment();
	}

	private CloudFoundryOperations cloudFoundryOperations(CloudFoundryClient cloudFoundryClient, String account) {
		return DefaultCloudFoundryOperations
				.builder().cloudFoundryClient(cloudFoundryClient)
				.organization(connectionProperties(account).getOrg())
				.space(connectionProperties(account).getSpace()).build();
	}

	private Version version(CloudFoundryClient cloudFoundryClient, String account) {
		return cloudFoundryClient.info()
				.get(GetInfoRequest.builder().build())
				.map(response -> Version.valueOf(response.getApiVersion()))
				.doOnNext(versionInfo -> logger.info(
						"Connecting to Cloud Foundry with API Version {}",
						versionInfo))
				.timeout(Duration.ofSeconds(deploymentProperties(account).getApiTimeout()))
				.block();
	}

	private RuntimeEnvironmentInfo runtimeEnvironmentInfo(CloudFoundryClient cloudFoundryClient, String account) {
		return new RuntimeEnvironmentInfo.Builder()
				.implementationName(CloudFoundryAppDeployer.class.getSimpleName())
				.spiClass(AppDeployer.class)
				.implementationVersion(
						RuntimeVersionUtils.getVersion(CloudFoundryAppDeployer.class))
				.platformType("Cloud Foundry")
				.platformClientVersion(
						RuntimeVersionUtils.getVersion(cloudFoundryClient.getClass()))
				.platformApiVersion(version(cloudFoundryClient, account).toString()).platformHostVersion("unknown")
				.addPlatformSpecificInfo("API Endpoint",
						connectionProperties(account).getUrl().toString())
				.build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private CloudFoundryPlatformProperties platformProperties;

		private boolean schedulesEnabled;

		private Optional<CloudFoundrySchedulerProperties> schedulerProperties = Optional.empty();

		private CloudFoundryPlatformTokenProvider platformTokenProvider;

		private CloudFoundryPlatformConnectionContextProvider connectionContextProvider;

		private CloudFoundryPlatformClientProvider cloudFoundryClientProvider;

		public Builder platformProperties(CloudFoundryPlatformProperties platformProperties) {
			this.platformProperties = platformProperties;
			return this;
		}

		public Builder schedulesEnabled(boolean schedulesEnabled) {
			this.schedulesEnabled = schedulesEnabled;
			return this;
		}

		public Builder schedulerProperties(Optional<CloudFoundrySchedulerProperties> schedulerProperties) {
			this.schedulerProperties = schedulerProperties;
			return this;
		}

		public Builder platformTokenProvider(CloudFoundryPlatformTokenProvider platformTokenProvider) {
			this.platformTokenProvider = platformTokenProvider;
			return this;
		}

		public Builder connectionContextProvider(
			CloudFoundryPlatformConnectionContextProvider connectionContextProvider) {
			this.connectionContextProvider = connectionContextProvider;
			return this;
		}

		public Builder cloudFoundryClientProvider(
			CloudFoundryPlatformClientProvider cloudFoundryClientProvider) {
			this.cloudFoundryClientProvider = cloudFoundryClientProvider;
			return this;
		}

		public CloudFoundryTaskPlatformFactory build() {
			Assert.notNull(platformProperties, "'platformProperties' is required.");
			Assert.notNull(platformTokenProvider, "'platformTokenProvider' is required.");
			Assert.notNull(connectionContextProvider, "'connectionContextProvider' is required.");
			Assert.notNull(cloudFoundryClientProvider, "'cloudFoundryClientProvider' is required.");

			return new CloudFoundryTaskPlatformFactory(
				platformProperties,
				platformTokenProvider,
				connectionContextProvider,
				cloudFoundryClientProvider,
				schedulerProperties,
				schedulesEnabled);
		}
	}
}
