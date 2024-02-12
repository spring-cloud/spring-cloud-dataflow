/*
 * Copyright 2018-2022 the original author or authors.
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

import reactor.core.publisher.Hooks;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Configuration class for customizing Cloud Foundry deployer.
 *
 * @author Eric Bottard
 * @author Corneil du Plessis
 */
@AutoConfiguration
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
public class CloudFoundryDataFlowServerConfiguration {
	private CloudFoundryServerConfigurationProperties cloudFoundryServerConfigurationProperties = new CloudFoundryServerConfigurationProperties();
	@Bean
	@ConfigurationProperties(prefix = CloudFoundryConnectionProperties.CLOUDFOUNDRY_PROPERTIES + ".task")
	public CloudFoundryDeploymentProperties taskDeploymentProperties() {
		return new CloudFoundryDeploymentProperties();
	}

	@Bean
	public CloudFoundryServerConfigurationProperties cloudFoundryServerConfigurationProperties() {
		return cloudFoundryServerConfigurationProperties;
	}

	// Replaced PostConstruct with handling ContextRefreshedEvent allows for the
	// resolution of beans to complete and to execute the code when configuration
	// is updated.
	@EventListener(ContextRefreshedEvent.class)
	public void handleContextRefreshedEvent() {
		if (this.cloudFoundryServerConfigurationProperties.isDebugReactor()) {
			Hooks.onOperatorDebug();
		}
	}
}
