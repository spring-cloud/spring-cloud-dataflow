/*
 * Copyright 2019-2020 the original author or authors.
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
import io.pivotal.scheduler.SchedulerClient;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.AbstractTaskPlatformFactory;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryPlatformSpecificInfo;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryTaskLauncher;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.deployer.spi.scheduler.cloudfoundry.CloudFoundryAppScheduler;
import org.springframework.cloud.deployer.spi.scheduler.cloudfoundry.CloudFoundrySchedulerProperties;
import org.springframework.cloud.deployer.spi.util.RuntimeVersionUtils;

/**
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 **/
public class CloudFoundryTaskPlatformFactory extends AbstractTaskPlatformFactory<CloudFoundryPlatformProperties> {

	private final static Logger logger = LoggerFactory.getLogger(CloudFoundryTaskPlatformFactory.class);

	private final CloudFoundryPlatformTokenProvider platformTokenProvider;

	private final CloudFoundryPlatformConnectionContextProvider connectionContextProvider;

	private final CloudFoundryPlatformClientProvider cloudFoundryClientProvider;

	private final Optional<CloudFoundrySchedulerClientProvider> cloudFoundrySchedulerClientProvider;


	private CloudFoundryTaskPlatformFactory(CloudFoundryPlatformProperties cloudFoundryPlatformProperties,
			CloudFoundryPlatformTokenProvider platformTokenProvider,
			CloudFoundryPlatformConnectionContextProvider connectionContextProvider,
			CloudFoundryPlatformClientProvider cloudFoundryClientProvider,
			Optional<CloudFoundrySchedulerClientProvider> cloudFoundrySchedulerClientProvider) {

		super(cloudFoundryPlatformProperties, CLOUDFOUNDRY_PLATFORM_TYPE);
		this.platformTokenProvider = platformTokenProvider;
		this.connectionContextProvider = connectionContextProvider;
		this.cloudFoundryClientProvider = cloudFoundryClientProvider;
		this.cloudFoundrySchedulerClientProvider = cloudFoundrySchedulerClientProvider;
	}

	@Override
	public Launcher createLauncher(String account) {
		ConnectionContext connectionContext = connectionContext(account);
		TokenProvider tokenProvider = tokenProvider(account);
		CloudFoundryClient cloudFoundryClient = cloudFoundryClient(account);
		CloudFoundryOperations cloudFoundryOperations = cloudFoundryOperations(cloudFoundryClient, account);
		CloudFoundryTaskLauncher taskLauncher = new CloudFoundryTaskLauncher(
				cloudFoundryClient,
				deploymentProperties(account),
				cloudFoundryOperations,
				runtimeEnvironmentInfo(cloudFoundryClient, account));
		Launcher launcher = new Launcher(account, CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher,
				scheduler(account, taskLauncher, cloudFoundryOperations));
		CloudFoundryConnectionProperties connectionProperties = connectionProperties(account);
		launcher.setDescription(String.format("org = [%s], space = [%s], url = [%s]",
				connectionProperties.getOrg(), connectionProperties.getSpace(),
				connectionProperties.getUrl()));
		return launcher;
	}

	private Scheduler scheduler(String account, CloudFoundryTaskLauncher taskLauncher,
			CloudFoundryOperations cloudFoundryOperations) {
		Scheduler scheduler = null;
		if (cloudFoundrySchedulerClientProvider.isPresent() && this.platformProperties.getAccounts().get(account).getScheduler() != null) {
			Optional<CloudFoundrySchedulerProperties> schedulerProperties = Optional.of(this.platformProperties.getAccounts().get(account).getScheduler());
			CloudFoundrySchedulerClientProvider cloudFoundrySchedulerClientProviderLocal = new CloudFoundrySchedulerClientProvider(
					connectionContextProvider, platformTokenProvider, schedulerProperties);
			SchedulerClient schedulerClient =
					cloudFoundrySchedulerClientProviderLocal.cloudFoundrySchedulerClient(account);
			scheduler = new CloudFoundryAppScheduler(
					schedulerClient,
					cloudFoundryOperations,
					connectionProperties(account),
					taskLauncher,
					schedulerProperties.get());
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
				.dopplerClient(ReactorDopplerClient.builder()
						.connectionContext(connectionContext(account))
						.tokenProvider(tokenProvider(account)).build())
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
		return new CloudFoundryPlatformSpecificInfo(new RuntimeEnvironmentInfo.Builder())
				.apiEndpoint(connectionProperties(account).getUrl().toString())
				.org(connectionProperties(account).getOrg())
				.space(connectionProperties(account).getSpace())
				.builder()
					.implementationName(CloudFoundryAppDeployer.class.getSimpleName())
					.spiClass(AppDeployer.class)
					.implementationVersion(
						RuntimeVersionUtils.getVersion(CloudFoundryAppDeployer.class))
					.platformType("Cloud Foundry")
					.platformClientVersion(
						RuntimeVersionUtils.getVersion(cloudFoundryClient.getClass()))
					.platformApiVersion(version(cloudFoundryClient, account).toString()).platformHostVersion("unknown")
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

		private Optional<CloudFoundrySchedulerClientProvider> cloudFoundrySchedulerClientProvider = Optional.empty();

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

		public Builder cloudFoundrySchedulerClientProvider(Optional<CloudFoundrySchedulerClientProvider>
			cloudFoundrySchedulerClientProvider) {
			this.cloudFoundrySchedulerClientProvider = cloudFoundrySchedulerClientProvider;
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
					cloudFoundrySchedulerClientProvider);
		}
	}
}
