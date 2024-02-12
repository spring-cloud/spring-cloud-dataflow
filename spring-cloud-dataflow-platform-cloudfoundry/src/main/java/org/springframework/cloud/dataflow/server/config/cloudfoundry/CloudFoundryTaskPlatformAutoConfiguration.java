/*
 * Copyright 2018-2021 the original author or authors.
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

import java.util.Optional;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.server.config.CloudProfileProvider;
import org.springframework.cloud.dataflow.server.config.features.ConditionalOnTasksEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Creates TaskPlatform implementations to launch/schedule tasks on Cloud Foundry.
 * @author Mark Pollack
 * @author David Turanski
 */
@AutoConfiguration
@ConditionalOnTasksEnabled
@EnableConfigurationProperties(CloudFoundryPlatformProperties.class)
public class CloudFoundryTaskPlatformAutoConfiguration {

	@Bean
	public TaskPlatform cloudFoundryPlatform(CloudFoundryTaskPlatformFactory cloudFoundryTaskPlatformFactory,
		Environment environment) {
		TaskPlatform taskPlatform = cloudFoundryTaskPlatformFactory.createTaskPlatform();
		CloudProfileProvider cloudProfileProvider = new CloudFoundryCloudProfileProvider();
		if (cloudProfileProvider.isCloudPlatform(environment)) {
			taskPlatform.setPrimary(true);
		}
		return taskPlatform;
	}

	@Bean
	public CloudFoundryPlatformConnectionContextProvider connectionContextProvider(
			CloudFoundryPlatformProperties cloudFoundryPlatformProperties) {
		return new CloudFoundryPlatformConnectionContextProvider(cloudFoundryPlatformProperties);
	}

	@Bean
	CloudFoundryPlatformTokenProvider platformTokenProvider(
			CloudFoundryPlatformProperties cloudFoundryPlatformProperties) {
		return new CloudFoundryPlatformTokenProvider(cloudFoundryPlatformProperties);
	}

	@Bean
	public CloudFoundryPlatformClientProvider cloudFoundryClientProvider(
			CloudFoundryPlatformProperties cloudFoundryPlatformProperties,
			CloudFoundryPlatformTokenProvider platformTokenProvider,
			CloudFoundryPlatformConnectionContextProvider connectionContextProvider) {
		return new CloudFoundryPlatformClientProvider(
				cloudFoundryPlatformProperties, connectionContextProvider, platformTokenProvider);
	}

	@Bean
	public CloudFoundrySchedulerClientProvider schedulerClientProvider(
			CloudFoundryPlatformConnectionContextProvider connectionContextProvider,
			CloudFoundryPlatformTokenProvider platformTokenProvider,
			CloudFoundryPlatformProperties cloudFoundryPlatformProperties) {
		return new CloudFoundrySchedulerClientProvider(
				connectionContextProvider, platformTokenProvider, cloudFoundryPlatformProperties);
	}

	@Bean
	public CloudFoundryTaskPlatformFactory cloudFoundryTaskPlatformFactory(
			CloudFoundryPlatformProperties cloudFoundryPlatformProperties,
			CloudFoundryPlatformTokenProvider platformTokenProvider,
			CloudFoundryPlatformConnectionContextProvider connectionContextProvider,
			CloudFoundryPlatformClientProvider cloudFoundryClientProvider,
			Optional<CloudFoundrySchedulerClientProvider> cloudFoundrySchedulerClientProvider) {
		return CloudFoundryTaskPlatformFactory.builder()
				.platformProperties(cloudFoundryPlatformProperties)
				.platformTokenProvider(platformTokenProvider)
				.connectionContextProvider(connectionContextProvider)
				.cloudFoundryClientProvider(cloudFoundryClientProvider)
				.cloudFoundrySchedulerClientProvider(cloudFoundrySchedulerClientProvider)
				.build();
	}
}
