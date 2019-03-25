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

import java.util.List;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.scheduler.spi.cloudfoundry.CloudFoundrySchedulerProperties;
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

	private static final org.slf4j.Logger logger = LoggerFactory
			.getLogger(CloudFoundryTaskPlatformAutoConfiguration.class);

	@Bean
	public TaskPlatform cloudFoundryPlatform(CloudFoundryLauncherFactory cloudFoundryTaskLauncherFactory) {
		List<Launcher> launchers = cloudFoundryTaskLauncherFactory.createLaunchers();
		return new TaskPlatform("Cloud Foundry", launchers);
	}

	@Bean
	public CloudFoundryLauncherFactory cloudFoundryTaskLauncherFactory(
			CloudFoundryPlatformProperties cloudFoundryPlatformProperties,
			Optional<CloudFoundrySchedulerProperties> schedulerProperties) {
		return new CloudFoundryLauncherFactory(cloudFoundryPlatformProperties, schedulerProperties, schedulesEnabled);
	}
}
